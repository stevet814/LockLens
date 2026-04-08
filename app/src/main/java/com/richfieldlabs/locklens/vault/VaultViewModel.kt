package com.richfieldlabs.locklens.vault

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richfieldlabs.locklens.camera.ExifStripper
import com.richfieldlabs.locklens.crypto.CryptoManager
import com.richfieldlabs.locklens.data.model.Album
import com.richfieldlabs.locklens.data.model.Photo
import com.richfieldlabs.locklens.data.repository.AlbumRepository
import com.richfieldlabs.locklens.data.repository.AuthRepository
import com.richfieldlabs.locklens.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VaultUiState(
    val photos: List<Photo> = emptyList(),
    val albums: List<Album> = emptyList(),
    val isProUnlocked: Boolean = false,
    val isImporting: Boolean = false,
    val importError: String? = null,
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val albumRepository: AlbumRepository,
    private val cryptoManager: CryptoManager,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private data class ImportState(val isImporting: Boolean = false, val error: String? = null)

    private val importState = MutableStateFlow(ImportState())

    val uiState = combine(
        photoRepository.observePhotos(),
        albumRepository.observeAll(),
        authRepository.authPreferences,
        importState,
    ) { photos, albums, prefs, import ->
        VaultUiState(
            photos = photos,
            albums = albums,
            isProUnlocked = prefs.isProUnlocked,
            isImporting = import.isImporting,
            importError = import.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VaultUiState(),
    )

    /** Called from PhotoThumbnail's produceState block — runs on IO dispatcher. */
    fun decryptThumbnail(photo: Photo): ByteArray {
        val iv = Base64.decode(photo.thumbIv, Base64.NO_WRAP)
        return cryptoManager.decrypt(File(photo.encryptedThumbPath), iv)
    }

    fun deletePhoto(photo: Photo) {
        viewModelScope.launch(Dispatchers.IO) {
            File(photo.encryptedFilePath).delete()
            File(photo.encryptedThumbPath).delete()
            photoRepository.delete(photo)
        }
    }

    fun importPhotos(uris: List<Uri>) {
        if (importState.value.isImporting) return
        viewModelScope.launch {
            importState.update { it.copy(isImporting = true, error = null) }
            val prefs = authRepository.authPreferences.first()
            val limit = if (prefs.isProUnlocked) Int.MAX_VALUE else 100

            withContext(Dispatchers.IO) {
                var imported = 0
                var skipped = 0
                val currentCount = photoRepository.count()

                for (uri in uris) {
                    if (currentCount + imported >= limit) {
                        skipped++
                        continue
                    }
                    val tempFile = File(context.cacheDir, "import_${UUID.randomUUID()}.tmp")
                    try {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            tempFile.outputStream().use { input.copyTo(it) }
                        }
                        ExifStripper.strip(tempFile)

                        val photoDir = File(context.filesDir, "vault/photos").apply { mkdirs() }
                        val thumbDir = File(context.filesDir, "vault/thumbs").apply { mkdirs() }
                        val encPhoto = File(photoDir, "${UUID.randomUUID()}.enc")
                        val encThumb = File(thumbDir, "${UUID.randomUUID()}.enc")

                        val photoIv = tempFile.inputStream().use { cryptoManager.encrypt(it, encPhoto) }
                        val thumbBytes = generateThumbnailBytes(tempFile)
                        val thumbIv = ByteArrayInputStream(thumbBytes).use { cryptoManager.encrypt(it, encThumb) }

                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(tempFile.absolutePath, bounds)

                        photoRepository.insert(
                            Photo(
                                albumId = null,
                                encryptedFilePath = encPhoto.absolutePath,
                                encryptedThumbPath = encThumb.absolutePath,
                                iv = Base64.encodeToString(photoIv, Base64.NO_WRAP),
                                thumbIv = Base64.encodeToString(thumbIv, Base64.NO_WRAP),
                                mimeType = context.contentResolver.getType(uri) ?: "image/jpeg",
                                originalWidth = bounds.outWidth.coerceAtLeast(0),
                                originalHeight = bounds.outHeight.coerceAtLeast(0),
                                capturedAt = System.currentTimeMillis(),
                            ),
                        )
                        imported++
                    } catch (_: Exception) {
                        // skip unreadable files
                    } finally {
                        tempFile.delete()
                    }
                }

                val error = if (skipped > 0)
                    "$imported imported, $skipped skipped — vault limit reached. Upgrade to Pro for unlimited."
                else
                    null
                importState.update { it.copy(isImporting = false, error = error) }
            }
        }
    }

    fun consumeImportError() {
        importState.update { it.copy(error = null) }
    }

    fun createAlbum(name: String) {
        viewModelScope.launch {
            albumRepository.insert(Album(name = name.trim()))
        }
    }

    private fun generateThumbnailBytes(sourceFile: File): ByteArray {
        val source = BitmapFactory.decodeFile(sourceFile.absolutePath)
            ?: error("Unable to decode image for thumbnail.")
        val scaled = Bitmap.createScaledBitmap(source, 256, 256, true)
        return ByteArrayOutputStream().also { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
            source.recycle()
            scaled.recycle()
        }.toByteArray()
    }
}
