package com.richfieldlabs.locklens.debug

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.Base64
import com.richfieldlabs.locklens.auth.AuthFallbackMode
import com.richfieldlabs.locklens.auth.PinSecurity
import com.richfieldlabs.locklens.crypto.CryptoManager
import com.richfieldlabs.locklens.data.db.IntruderDao
import com.richfieldlabs.locklens.data.model.IntruderEvent
import com.richfieldlabs.locklens.data.model.Photo
import com.richfieldlabs.locklens.data.repository.AuthRepository
import com.richfieldlabs.locklens.data.repository.PhotoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class DebugSeedManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager,
    private val photoRepository: PhotoRepository,
    private val authRepository: AuthRepository,
    private val intruderDao: IntruderDao,
) {

    suspend fun seed() = withContext(Dispatchers.IO) {
        // Set up auth state: APP_PIN mode, PIN = "123456"
        authRepository.setFallbackMode(AuthFallbackMode.APP_PIN)
        authRepository.setRealPin("123456")
        authRepository.markVaultInitialized()
        authRepository.setProUnlocked(true)

        // Seed photos if vault is empty
        if (photoRepository.count() == 0) {
            seedPhotos()
            seedIntruderEvents()
        }
    }

    private suspend fun seedPhotos() {
        val photoDir = File(context.filesDir, "vault/photos").apply { mkdirs() }
        val thumbDir = File(context.filesDir, "vault/thumbs").apply { mkdirs() }

        val photoMetas = listOf(
            Triple("Sunset hike", 0, System.currentTimeMillis() - 86_400_000L * 30),
            Triple("Beach day", 1, System.currentTimeMillis() - 86_400_000L * 25),
            Triple("Mountain trail", 2, System.currentTimeMillis() - 86_400_000L * 20),
            Triple("City lights", 3, System.currentTimeMillis() - 86_400_000L * 14),
            Triple("Forest walk", 4, System.currentTimeMillis() - 86_400_000L * 10),
            Triple("Lake mirror", 5, System.currentTimeMillis() - 86_400_000L * 7),
            Triple("Desert dunes", 6, System.currentTimeMillis() - 86_400_000L * 5),
            Triple("Waterfall", 7, System.currentTimeMillis() - 86_400_000L * 3),
            Triple("Cliff view", 0, System.currentTimeMillis() - 86_400_000L * 2),
            Triple("Valley mist", 2, System.currentTimeMillis() - 86_400_000L),
            Triple("Sunrise peak", 1, System.currentTimeMillis() - 3_600_000L),
            Triple("Night sky", 3, System.currentTimeMillis()),
        )

        for ((label, style, capturedAt) in photoMetas) {
            val fullBitmap = generateSceneBitmap(style, 1080, 810)
            val thumbBitmap = generateSceneBitmap(style, 256, 256)

            val fullBytes = bitmapToJpeg(fullBitmap)
            val thumbBytes = bitmapToJpeg(thumbBitmap)

            val encPhotFile = File(photoDir, "${UUID.randomUUID()}.enc")
            val encThumbFile = File(thumbDir, "${UUID.randomUUID()}.enc")

            val iv = ByteArrayInputStream(fullBytes).use { cryptoManager.encrypt(it, encPhotFile) }
            val thumbIv = ByteArrayInputStream(thumbBytes).use { cryptoManager.encrypt(it, encThumbFile) }

            photoRepository.insert(
                Photo(
                    albumId = null,
                    encryptedFilePath = encPhotFile.absolutePath,
                    encryptedThumbPath = encThumbFile.absolutePath,
                    iv = Base64.encodeToString(iv, Base64.NO_WRAP),
                    thumbIv = Base64.encodeToString(thumbIv, Base64.NO_WRAP),
                    mimeType = "image/jpeg",
                    originalWidth = 1080,
                    originalHeight = 810,
                    capturedAt = capturedAt,
                    label = label,
                )
            )
        }
    }

    private suspend fun seedIntruderEvents() {
        val intruderDir = File(context.filesDir, "intruders").apply { mkdirs() }

        // 3 failed attempts: two with selfies, one without
        val attempts = listOf(
            Pair(true, System.currentTimeMillis() - 86_400_000L * 2),
            Pair(true, System.currentTimeMillis() - 3_600_000L),
            Pair(false, System.currentTimeMillis() - 600_000L),
        )

        for ((hasSelfie, attemptedAt) in attempts) {
            val (encPath, ivStr) = if (hasSelfie) {
                val selfie = generateSelfiePlaceholder()
                val selfieBytes = bitmapToJpeg(selfie)
                val encFile = File(intruderDir, "${UUID.randomUUID()}.enc")
                val iv = ByteArrayInputStream(selfieBytes).use { cryptoManager.encrypt(it, encFile) }
                Pair(encFile.absolutePath, Base64.encodeToString(iv, Base64.NO_WRAP))
            } else {
                Pair(null, null)
            }

            intruderDao.insert(
                IntruderEvent(
                    encryptedSelfieFilePath = encPath,
                    selfieIv = ivStr,
                    attemptedPin = PinSecurity.hash("0000"),
                    attemptedAt = attemptedAt,
                )
            )
        }
    }

    // --- Scene generators ---

    private fun generateSceneBitmap(style: Int, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        when (style % 8) {
            0 -> drawSunset(canvas, w, h)
            1 -> drawOcean(canvas, w, h)
            2 -> drawForest(canvas, w, h)
            3 -> drawCityNight(canvas, w, h)
            4 -> drawDesert(canvas, w, h)
            5 -> drawLake(canvas, w, h)
            6 -> drawMountains(canvas, w, h)
            else -> drawAurora(canvas, w, h)
        }
        return bmp
    }

    private fun drawSunset(canvas: Canvas, w: Int, h: Int) {
        val skyPaint = gradientPaint(
            0f, 0f, 0f, h * 0.65f,
            Color.parseColor("#FF6B35"), Color.parseColor("#8B1A1A"),
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.65f, skyPaint)
        val groundPaint = gradientPaint(
            0f, h * 0.65f, 0f, h.toFloat(),
            Color.parseColor("#2D1B00"), Color.parseColor("#1A0F00"),
        )
        canvas.drawRect(0f, h * 0.65f, w.toFloat(), h.toFloat(), groundPaint)
        // Horizon glow
        val glowPaint = gradientPaint(
            0f, h * 0.55f, 0f, h * 0.75f,
            Color.parseColor("#FFAA00"), Color.parseColor("#FF6B3500"),
        )
        canvas.drawRect(0f, h * 0.55f, w.toFloat(), h * 0.75f, glowPaint)
        // Sun
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD700") }
        canvas.drawCircle(w / 2f, h * 0.63f, w * 0.06f, sunPaint)
    }

    private fun drawOcean(canvas: Canvas, w: Int, h: Int) {
        val skyPaint = gradientPaint(
            0f, 0f, 0f, h * 0.5f,
            Color.parseColor("#87CEEB"), Color.parseColor("#4682B4"),
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.5f, skyPaint)
        val oceanPaint = gradientPaint(
            0f, h * 0.5f, 0f, h.toFloat(),
            Color.parseColor("#006994"), Color.parseColor("#001a2e"),
        )
        canvas.drawRect(0f, h * 0.5f, w.toFloat(), h.toFloat(), oceanPaint)
        // Horizon sparkle
        val sparklePaint = Paint().apply { color = Color.parseColor("#FFFFFF44") }
        for (i in 0..6) {
            canvas.drawRect(
                w * 0.05f + i * w * 0.14f, h * 0.495f,
                w * 0.10f + i * w * 0.14f, h * 0.505f, sparklePaint,
            )
        }
    }

    private fun drawForest(canvas: Canvas, w: Int, h: Int) {
        val skyPaint = gradientPaint(
            0f, 0f, 0f, h * 0.3f,
            Color.parseColor("#87CEEB"), Color.parseColor("#E0F0FF"),
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.3f, skyPaint)
        val forestPaint = gradientPaint(
            0f, h * 0.3f, 0f, h.toFloat(),
            Color.parseColor("#228B22"), Color.parseColor("#004400"),
        )
        canvas.drawRect(0f, h * 0.3f, w.toFloat(), h.toFloat(), forestPaint)
        // Trees
        val treePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#004D00") }
        for (i in 0..5) {
            val cx = w * 0.08f + i * w * 0.18f
            canvas.drawCircle(cx, h * 0.45f, w * 0.09f, treePaint)
            canvas.drawCircle(cx + w * 0.09f, h * 0.55f, w * 0.07f, treePaint)
        }
        // Path
        val pathPaint = gradientPaint(
            w * 0.45f, h * 0.4f, w * 0.5f, h.toFloat(),
            Color.parseColor("#8B6914"), Color.parseColor("#A0855A"),
        )
        val path = android.graphics.Path().apply {
            moveTo(w * 0.44f, h.toFloat())
            lineTo(w * 0.56f, h.toFloat())
            lineTo(w * 0.52f, h * 0.4f)
            lineTo(w * 0.48f, h * 0.4f)
            close()
        }
        canvas.drawPath(path, pathPaint)
    }

    private fun drawCityNight(canvas: Canvas, w: Int, h: Int) {
        val skyPaint = gradientPaint(
            0f, 0f, 0f, h.toFloat(),
            Color.parseColor("#0D0D2B"), Color.parseColor("#1a0a2e"),
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), skyPaint)
        // Stars
        val starPaint = Paint().apply { color = Color.WHITE }
        val rng = java.util.Random(42L)
        repeat(80) {
            canvas.drawCircle(rng.nextFloat() * w, rng.nextFloat() * h * 0.6f, 1.5f, starPaint)
        }
        // Buildings
        val buildingColors = listOf("#1C1C3A", "#141428", "#1A1A35")
        val buildingPaint = Paint()
        val windowPaint = Paint().apply { color = Color.parseColor("#FFD700BB") }
        for (i in 0..7) {
            buildingPaint.color = Color.parseColor(buildingColors[i % 3])
            val bx = i * w * 0.13f
            val bh = h * (0.25f + rng.nextFloat() * 0.25f)
            canvas.drawRect(bx, h - bh, bx + w * 0.11f, h.toFloat(), buildingPaint)
            // Windows
            var wy = h - bh + 10f
            while (wy < h - 10f) {
                var wx = bx + 5f
                while (wx < bx + w * 0.09f) {
                    if (rng.nextFloat() > 0.3f) {
                        canvas.drawRect(wx, wy, wx + w * 0.02f, wy + h * 0.025f, windowPaint)
                    }
                    wx += w * 0.03f
                }
                wy += h * 0.045f
            }
        }
        // Moon
        val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFFACD") }
        canvas.drawCircle(w * 0.8f, h * 0.15f, w * 0.05f, moonPaint)
    }

    private fun drawDesert(canvas: Canvas, w: Int, h: Int) {
        val skyPaint = gradientPaint(
            0f, 0f, 0f, h * 0.6f,
            Color.parseColor("#FFD59E"), Color.parseColor("#FF8C42"),
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.6f, skyPaint)
        val sandPaint = gradientPaint(
            0f, h * 0.5f, 0f, h.toFloat(),
            Color.parseColor("#D4A456"), Color.parseColor("#8B6914"),
        )
        // Dune curves
        val dunePath = android.graphics.Path().apply {
            moveTo(0f, h * 0.65f)
            cubicTo(w * 0.25f, h * 0.50f, w * 0.5f, h * 0.70f, w * 0.75f, h * 0.55f)
            cubicTo(w * 0.88f, h * 0.48f, w.toFloat(), h * 0.60f, w.toFloat(), h * 0.65f)
            lineTo(w.toFloat(), h.toFloat())
            lineTo(0f, h.toFloat())
            close()
        }
        canvas.drawPath(dunePath, sandPaint)
        // Sun
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFEC8B") }
        canvas.drawCircle(w * 0.15f, h * 0.2f, w * 0.07f, sunPaint)
    }

    private fun drawLake(canvas: Canvas, w: Int, h: Int) {
        val skyPaint = gradientPaint(
            0f, 0f, 0f, h * 0.5f,
            Color.parseColor("#E8F4FD"), Color.parseColor("#B0D4F1"),
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.5f, skyPaint)
        val waterPaint = gradientPaint(
            0f, h * 0.5f, 0f, h.toFloat(),
            Color.parseColor("#A8D8EA"), Color.parseColor("#2C7873"),
        )
        canvas.drawRect(0f, h * 0.5f, w.toFloat(), h.toFloat(), waterPaint)
        // Mountain reflections
        val mtnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6B8F71") }
        val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4A6F5480") }
        val mtnPath = android.graphics.Path().apply {
            moveTo(0f, h * 0.5f)
            lineTo(w * 0.2f, h * 0.22f)
            lineTo(w * 0.4f, h * 0.5f)
            lineTo(w * 0.6f, h * 0.18f)
            lineTo(w * 0.8f, h * 0.44f)
            lineTo(w.toFloat(), h * 0.28f)
            lineTo(w.toFloat(), h * 0.5f)
            close()
        }
        canvas.drawPath(mtnPath, mtnPaint)
        // Mirror reflection (flip)
        canvas.save()
        canvas.scale(1f, -1f, 0f, h * 0.5f)
        canvas.drawPath(mtnPath, refPaint)
        canvas.restore()
    }

    private fun drawMountains(canvas: Canvas, w: Int, h: Int) {
        val skyPaint = gradientPaint(
            0f, 0f, 0f, h * 0.55f,
            Color.parseColor("#3A7BD5"), Color.parseColor("#00D2FF"),
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.55f, skyPaint)
        val snowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val rockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6D6D6D") }
        val darkRockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3E3E3E") }
        val grassPaint = gradientPaint(
            0f, h * 0.75f, 0f, h.toFloat(),
            Color.parseColor("#4A7C59"), Color.parseColor("#2D4A35"),
        )
        // Back mountains
        drawTriangle(canvas, w * 0.1f, h * 0.55f, w * 0.4f, h * 0.2f, w * 0.7f, h * 0.55f, rockPaint)
        drawTriangle(canvas, w * 0.5f, h * 0.55f, w * 0.75f, h * 0.15f, w.toFloat(), h * 0.55f, darkRockPaint)
        // Snow caps
        drawTriangle(canvas, w * 0.32f, h * 0.30f, w * 0.4f, h * 0.2f, w * 0.48f, h * 0.30f, snowPaint)
        drawTriangle(canvas, w * 0.69f, h * 0.25f, w * 0.75f, h * 0.15f, w * 0.81f, h * 0.25f, snowPaint)
        // Foreground grass
        canvas.drawRect(0f, h * 0.75f, w.toFloat(), h.toFloat(), grassPaint)
    }

    private fun drawAurora(canvas: Canvas, w: Int, h: Int) {
        val skyPaint = gradientPaint(
            0f, 0f, 0f, h.toFloat(),
            Color.parseColor("#0A0A1A"), Color.parseColor("#050510"),
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), skyPaint)
        // Aurora bands
        val auroraPaints = listOf(
            "#00FF8833" to Pair(0.1f, 0.45f),
            "#00CCFF44" to Pair(0.2f, 0.55f),
            "#8833FF33" to Pair(0.05f, 0.40f),
        )
        for ((colorHex, yRange) in auroraPaints) {
            val auroraPaint = gradientPaint(
                0f, h * yRange.first, 0f, h * yRange.second,
                Color.parseColor(colorHex), Color.TRANSPARENT,
            )
            canvas.drawRect(0f, h * yRange.first, w.toFloat(), h * yRange.second, auroraPaint)
        }
        // Stars
        val starPaint = Paint().apply { color = Color.WHITE }
        val rng = java.util.Random(7L)
        repeat(120) {
            val x = rng.nextFloat() * w
            val y = rng.nextFloat() * h * 0.7f
            canvas.drawCircle(x, y, 1f + rng.nextFloat(), starPaint)
        }
    }

    private fun generateSelfiePlaceholder(): Bitmap {
        val w = 480; val h = 640
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        // Dark background
        val bgPaint = gradientPaint(
            0f, 0f, 0f, h.toFloat(),
            Color.parseColor("#1A1A2E"), Color.parseColor("#16213E"),
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)
        // Silhouette: head + shoulders
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F3460") }
        // Shoulders
        canvas.drawOval(
            android.graphics.RectF(w * 0.1f, h * 0.62f, w * 0.9f, h * 1.1f),
            bodyPaint,
        )
        // Head circle
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w / 2f, h * 0.38f, w * 0.22f,
                Color.parseColor("#2E4057"), Color.parseColor("#1A2540"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(w / 2f, h * 0.38f, w * 0.22f, headPaint)
        // Flash glare on lens
        val glarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.88f, h * 0.08f, w * 0.06f,
                Color.parseColor("#FFFFFF88"), Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(w * 0.88f, h * 0.08f, w * 0.06f, glarePaint)
        return bmp
    }

    // --- Helpers ---

    private fun gradientPaint(x0: Float, y0: Float, x1: Float, y1: Float, c0: Int, c1: Int) =
        Paint().apply {
            shader = LinearGradient(x0, y0, x1, y1, c0, c1, Shader.TileMode.CLAMP)
        }

    private fun drawTriangle(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, paint: Paint) {
        val path = android.graphics.Path().apply {
            moveTo(x1, y1); lineTo(x2, y2); lineTo(x3, y3); close()
        }
        canvas.drawPath(path, paint)
    }

    private fun bitmapToJpeg(bitmap: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        return out.toByteArray()
    }
}
