package com.richfieldlabs.locklens.vault

import android.app.Activity
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richfieldlabs.locklens.billing.BillingManager
import com.richfieldlabs.locklens.crypto.CryptoManager
import com.richfieldlabs.locklens.data.db.IntruderDao
import com.richfieldlabs.locklens.data.model.IntruderEvent
import com.richfieldlabs.locklens.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

data class IntruderLogUiState(
    val events: List<IntruderEvent> = emptyList(),
    val isProUnlocked: Boolean = false,
)

@HiltViewModel
class IntruderLogViewModel @Inject constructor(
    intruderDao: IntruderDao,
    private val cryptoManager: CryptoManager,
    authRepository: AuthRepository,
    private val billingManager: BillingManager,
) : ViewModel() {

    val uiState = combine(
        intruderDao.observeAll(),
        authRepository.authPreferences,
    ) { events, prefs ->
        IntruderLogUiState(
            events = events,
            isProUnlocked = prefs.isProUnlocked,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = IntruderLogUiState(),
        )

    fun formatTimestamp(epochMillis: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        return sdf.format(Date(epochMillis))
    }

    suspend fun decryptSelfie(event: IntruderEvent): ImageBitmap? {
        val path = event.encryptedSelfieFilePath ?: return null
        val ivStr = event.selfieIv ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val iv = Base64.decode(ivStr, Base64.NO_WRAP)
                val bytes = cryptoManager.decrypt(File(path), iv)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        billingManager.launchPurchaseFlow(activity)
    }
}
