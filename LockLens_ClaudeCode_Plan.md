# LockLens — Claude Code Build Plan

**App:** LockLens – Private Photo Vault
**Package:** `com.richfieldlabs.locklens`
**Tagline:** *The camera that forgets where you were.*
**Play Store Title:** `LockLens – Private Photo Vault` (30 chars)
**Target SDK:** 35 (Android 15) | **Min SDK:** 26 (Android 8.0)
**Language:** Kotlin | **UI:** Jetpack Compose + Material 3
**Infra cost:** $0/month forever

---

## What This App Does

LockLens is two things in one seamless workflow:

1. **Secure Camera** — Takes photos with all EXIF metadata stripped at capture time (GPS, device model, timestamp). No location leak. No fingerprinting. Photos are clean the moment they're shot.
2. **Encrypted Vault** — Stores photos in an AES-256-GCM encrypted gallery on-device. Biometric lock. Decoy PIN (wrong PIN shows fake empty vault). Break-in selfie on failed unlock attempts.

No internet permission. No account. No cloud. One-time $4.99 Pro unlock.

---

## Monetization

| Tier | Price | Gate |
|---|---|---|
| Free | $0 | Camera (metadata-free) + vault up to 100 photos |
| Pro | $4.99 one-time | Unlimited vault + video support + decoy PIN + break-in selfie + secure share |

Use **Google Play Billing Library 7** for the IAP. One SKU: `locklens_pro_lifetime`.

---

## Project Structure

```
locklens/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/richfieldlabs/locklens/
│   │   │   ├── LockLensApp.kt               # Application class, Hilt entry point
│   │   │   ├── MainActivity.kt              # Single activity, Compose nav host
│   │   │   ├── camera/
│   │   │   │   ├── CameraScreen.kt          # CameraX viewfinder + capture UI
│   │   │   │   ├── CameraViewModel.kt
│   │   │   │   └── ExifStripper.kt          # Strips metadata post-capture
│   │   │   ├── vault/
│   │   │   │   ├── VaultScreen.kt           # Encrypted gallery grid
│   │   │   │   ├── VaultViewModel.kt
│   │   │   │   ├── PhotoDetailScreen.kt     # Full-screen decrypted view
│   │   │   │   └── AlbumScreen.kt           # Album/folder view
│   │   │   ├── auth/
│   │   │   │   ├── LockScreen.kt            # Biometric + PIN entry
│   │   │   │   ├── AuthViewModel.kt
│   │   │   │   ├── DecoyVault.kt            # Fake vault shown on decoy PIN
│   │   │   │   └── IntruderDetector.kt      # Break-in selfie logic
│   │   │   ├── crypto/
│   │   │   │   ├── CryptoManager.kt         # AES-256-GCM encrypt/decrypt
│   │   │   │   └── KeystoreHelper.kt        # Android Keystore key management
│   │   │   ├── data/
│   │   │   │   ├── db/
│   │   │   │   │   ├── AppDatabase.kt       # Room database
│   │   │   │   │   ├── PhotoDao.kt
│   │   │   │   │   ├── AlbumDao.kt
│   │   │   │   │   └── IntruderDao.kt
│   │   │   │   ├── model/
│   │   │   │   │   ├── Photo.kt             # Room entity
│   │   │   │   │   ├── Album.kt             # Room entity
│   │   │   │   │   └── IntruderEvent.kt     # Room entity
│   │   │   │   └── repository/
│   │   │   │       ├── PhotoRepository.kt
│   │   │   │       └── AuthRepository.kt
│   │   │   ├── billing/
│   │   │   │   ├── BillingManager.kt        # Play Billing wrapper
│   │   │   │   └── ProGate.kt              # Composable for Pro upgrade prompts
│   │   │   ├── settings/
│   │   │   │   ├── SettingsScreen.kt
│   │   │   │   └── SettingsViewModel.kt
│   │   │   ├── ui/
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Color.kt             # Richfield Labs brand colors
│   │   │   │   │   ├── Theme.kt
│   │   │   │   │   └── Type.kt
│   │   │   │   └── components/
│   │   │   │       ├── PhotoThumbnail.kt    # Encrypted thumbnail composable
│   │   │   │       ├── ProBadge.kt
│   │   │   │       └── EmptyState.kt
│   │   │   └── navigation/
│   │   │       └── AppNavGraph.kt           # Compose Navigation routes
│   │   └── res/
│   │       ├── drawable/                    # App icon, vector assets
│   │       └── values/                      # strings.xml, no sensitive strings
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/libs.versions.toml                # Version catalog
```

---

## Gradle Dependencies

Add to `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.0.0"
agp = "8.5.0"
compose-bom = "2024.09.00"
hilt = "2.52"
room = "2.6.1"
camerax = "1.3.4"
billing = "7.0.0"
biometric = "1.2.0-alpha05"
navigation-compose = "2.8.0"
lifecycle = "2.8.4"
coroutines = "1.8.1"
datastore = "1.1.1"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.9.1" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }

# Hilt DI
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# CameraX
camera-core = { group = "androidx.camera", name = "camera-core", version.ref = "camerax" }
camera-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "camerax" }
camera-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "camerax" }
camera-view = { group = "androidx.camera", name = "camera-view", version.ref = "camerax" }

# Biometric
biometric = { group = "androidx.biometric", name = "biometric", version.ref = "biometric" }

# Billing
billing = { group = "com.android.billingclient", name = "billing", version.ref = "billing" }
billing-ktx = { group = "com.android.billingclient", name = "billing-ktx", version.ref = "billing" }

# Lifecycle
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }

# Coroutines
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# DataStore (settings/PIN storage)
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Coil (display thumbnails - decrypted in memory only)
coil-compose = { group = "io.coil-kt", name = "coil-compose", version = "2.7.0" }
```

---

## AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- CAMERA only — no INTERNET, no READ/WRITE_EXTERNAL_STORAGE, no CONTACTS -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.USE_BIOMETRIC" />
    <uses-permission android:name="android.permission.USE_FINGERPRINT" />
    <uses-permission android:name="com.android.vending.BILLING" />
    <!-- POST_NOTIFICATIONS for break-in selfie alerts (Android 13+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- Explicitly declare NO internet permission — this is a selling point -->
    <!-- DO NOT ADD: android.permission.INTERNET -->

    <application
        android:name=".LockLensApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.LockLens"
        android:networkSecurityConfig="@xml/network_security_config">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- FileProvider for secure share -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>
</manifest>
```

**res/xml/network_security_config.xml** — blocks all network traffic as defense-in-depth:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <!-- No trusted anchors = no outbound connections even if permission added later -->
        </trust-anchors>
    </base-config>
</network-security-config>
```

---

## Data Models

### `Photo.kt`
```kotlin
@Entity(tableName = "photos", foreignKeys = [
    ForeignKey(entity = Album::class, parentColumns = ["id"],
        childColumns = ["albumId"], onDelete = ForeignKey.SET_NULL)
])
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val albumId: Long?,
    val encryptedFilePath: String,      // Path to AES-256 encrypted .enc file
    val encryptedThumbPath: String,     // Path to encrypted thumbnail .enc file
    val iv: String,                      // Base64 IV used for this file's encryption
    val mimeType: String,                // "image/jpeg" or "video/mp4"
    val originalWidth: Int,
    val originalHeight: Int,
    val capturedAt: Long,                // epoch millis — from app, NOT from EXIF
    val addedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val label: String = ""
)
```

### `Album.kt`
```kotlin
@Entity(tableName = "albums")
data class Album(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverPhotoId: Long?,
    val createdAt: Long = System.currentTimeMillis(),
    val photoCount: Int = 0
)
```

### `IntruderEvent.kt`
```kotlin
@Entity(tableName = "intruder_events")
data class IntruderEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val encryptedSelfieFilePath: String?,   // Selfie taken on failed unlock
    val attemptedPin: String,               // Obfuscated/hashed
    val attemptedAt: Long = System.currentTimeMillis()
)
```

---

## CryptoManager.kt

All encryption/decryption goes through this class. Use Android Keystore for key storage — the key never exists in app memory as plaintext.

```kotlin
@Singleton
class CryptoManager @Inject constructor() {

    companion object {
        private const val KEY_ALIAS = "locklens_vault_key"
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun getOrCreateKey(): SecretKey {
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, "AndroidKeyStore")
        keyGenerator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(PADDING)
                .setUserAuthenticationRequired(false) // auth at app level via PIN/biometric
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGenerator.generateKey()
    }

    fun encrypt(inputStream: InputStream, outputFile: File): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv // Save this — needed for decryption

        CipherOutputStream(FileOutputStream(outputFile), cipher).use { cos ->
            inputStream.copyTo(cos)
        }
        return iv
    }

    fun decrypt(encryptedFile: File, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)

        return CipherInputStream(FileInputStream(encryptedFile), cipher).use { cis ->
            cis.readBytes()
        }
    }
}
```

---

## ExifStripper.kt

Strip ALL EXIF at capture time before the image ever touches disk as plaintext.

```kotlin
object ExifStripper {

    private val TAGS_TO_STRIP = listOf(
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_SERIAL_NUMBER,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_BODY_SERIAL_NUMBER,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_CAMERA_OWNER_NAME,
    )

    /**
     * Strip all identifying EXIF tags from a JPEG file in-place.
     * Preserves only: orientation, image dimensions, color space.
     * Call this BEFORE encrypting.
     */
    fun strip(file: File) {
        val exif = ExifInterface(file.absolutePath)
        TAGS_TO_STRIP.forEach { tag -> exif.setAttribute(tag, null) }
        exif.saveAttributes()
    }

    /**
     * Verify no GPS data remains. Use in debug builds only.
     */
    fun hasGpsData(file: File): Boolean {
        val exif = ExifInterface(file.absolutePath)
        return exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null
    }
}
```

---

## CameraScreen.kt — Key Logic

```kotlin
@Composable
fun CameraScreen(
    onPhotoSaved: (Uri) -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    // ... CameraX preview setup ...

    fun capturePhoto() {
        val photoFile = File(
            context.cacheDir,  // Cache dir — temp file before encryption
            "capture_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // 1. Strip EXIF immediately
                    ExifStripper.strip(photoFile)

                    // 2. Encrypt and save to vault (delete temp file after)
                    viewModel.saveToVault(photoFile)
                }
                override fun onError(exception: ImageCaptureException) { /* handle */ }
            }
        )
    }
}
```

**CameraViewModel.kt** — handles the encrypt-save-delete pipeline:
```kotlin
fun saveToVault(tempFile: File) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            // Encrypt to internal storage
            val encryptedFile = File(vaultDir, "${UUID.randomUUID()}.enc")
            val iv = cryptoManager.encrypt(tempFile.inputStream(), encryptedFile)

            // Generate + encrypt thumbnail
            val thumb = generateThumbnail(tempFile)
            val thumbFile = File(thumbDir, "${UUID.randomUUID()}.enc")
            val thumbIv = cryptoManager.encrypt(thumb.inputStream(), thumbFile)

            // Save metadata to Room
            val photo = Photo(
                albumId = currentAlbumId,
                encryptedFilePath = encryptedFile.absolutePath,
                encryptedThumbPath = thumbFile.absolutePath,
                iv = Base64.encodeToString(iv, Base64.DEFAULT),
                mimeType = "image/jpeg",
                originalWidth = /* from bitmap */ 0,
                originalHeight = 0,
                capturedAt = System.currentTimeMillis()  // App time, not EXIF
            )
            photoRepository.insert(photo)
        } finally {
            tempFile.delete()  // Always delete plaintext temp file
        }
    }
}
```

---

## Auth / Lock Screen

### Two-PIN System (Real vs Decoy)

Store two hashed PINs in `DataStore<Preferences>` (NOT Room — doesn't need encryption):
- `PREF_REAL_PIN_HASH` — SHA-256 hash of the real PIN. Shows the full vault.
- `PREF_DECOY_PIN_HASH` — SHA-256 hash of the decoy PIN. Shows an empty (or fake-populated) vault.

```kotlin
// In AuthRepository.kt
suspend fun checkPin(entered: String): PinResult {
    val hash = sha256(entered)
    return when (hash) {
        dataStore.data.first()[PREF_REAL_PIN_HASH] -> PinResult.REAL
        dataStore.data.first()[PREF_DECOY_PIN_HASH] -> PinResult.DECOY
        else -> PinResult.WRONG
    }
}

enum class PinResult { REAL, DECOY, WRONG }
```

### Biometric Auth

```kotlin
fun authenticateWithBiometric(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onFallback: () -> Unit
) {
    val biometricManager = BiometricManager.from(activity)
    if (biometricManager.canAuthenticate(BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
        onFallback(); return
    }

    val prompt = BiometricPrompt(activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: AuthenticationResult) = onSuccess()
            override fun onAuthenticationFailed() { /* show wrong attempt counter */ }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFallback()
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock LockLens")
        .setSubtitle("Use biometric to open your vault")
        .setNegativeButtonText("Use PIN")
        .build()

    prompt.authenticate(promptInfo)
}
```

### Break-In Selfie (Pro feature)

After N failed PIN attempts, silently open front camera, capture photo, strip EXIF, encrypt, save to `intruder_events` table, show notification (if granted).

```kotlin
// In IntruderDetector.kt
class IntruderDetector @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val intruderDao: IntruderDao
) {
    companion object {
        const val FAILED_ATTEMPTS_THRESHOLD = 3
    }

    suspend fun captureIntruder(context: Context, attemptedPin: String) {
        // Use ImageCapture with front camera (LENS_FACING_FRONT)
        // Strip EXIF, encrypt, save to intruder_events
        // Show local notification: "Failed unlock attempt recorded"
    }
}
```

---

## Navigation Routes

```kotlin
// AppNavGraph.kt
sealed class Screen(val route: String) {
    object Lock : Screen("lock")
    object Vault : Screen("vault")
    object Camera : Screen("camera")
    object PhotoDetail : Screen("photo_detail/{photoId}") {
        fun createRoute(photoId: Long) = "photo_detail/$photoId"
    }
    object Album : Screen("album/{albumId}") {
        fun createRoute(albumId: Long) = "album/$albumId"
    }
    object Intruders : Screen("intruders")  // Pro only
    object Settings : Screen("settings")
}
```

App always starts at `Screen.Lock`. Successful auth navigates to `Screen.Vault` (real) or `Screen.Vault?decoy=true`.

---

## Vault Screen — Thumbnail Display

Thumbnails are stored encrypted. Decrypt in-memory for display, never write decrypted bytes to disk:

```kotlin
// In PhotoThumbnail.kt composable
val bitmap by produceState<Bitmap?>(initialValue = null, key1 = photo.id) {
    value = withContext(Dispatchers.IO) {
        val iv = Base64.decode(photo.iv, Base64.DEFAULT)
        val decryptedBytes = cryptoManager.decrypt(File(photo.encryptedThumbPath), iv)
        BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
    }
}

bitmap?.let { bmp ->
    Image(
        painter = rememberAsyncImagePainter(bmp),
        contentDescription = null,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
    )
}
```

**Important:** Do NOT use Coil's disk cache for encrypted images. Set `diskCachePolicy = CachePolicy.DISABLED` on any Coil request for vault images. Memory cache only, and clear it when the vault locks.

---

## Free vs Pro Gates

```kotlin
// ProGate.kt — reusable composable
@Composable
fun ProGate(
    isProUnlocked: Boolean,
    featureName: String,
    onUpgradeClick: () -> Unit,
    content: @Composable () -> Unit
) {
    if (isProUnlocked) {
        content()
    } else {
        ProUpgradeCard(
            featureName = featureName,
            onUpgradeClick = onUpgradeClick
        )
    }
}
```

Track Pro state in `DataStore<Preferences>` AND verify with Play Billing on each launch. Don't trust local state alone.

---

## Brand Colors

```kotlin
// ui/theme/Color.kt — matches Richfield Labs brand exactly
val Background    = Color(0xFF05090A)
val TealPrimary   = Color(0xFF00BFA5)
val TealSecondary = Color(0xFF00D4B8)
val TealTertiary  = Color(0xFF00ACC8)
val Foreground    = Color(0xFFF1F5F9)
val SurfaceCard   = Color(0xFF0D1517)
val ErrorRed      = Color(0xFFFF6B6B)
```

---

## Build Phases

### Phase 1 — Scaffold + Crypto Foundation (Days 1–3)
- [ ] Create Android project: Kotlin, Compose, min SDK 26, no internet permission
- [ ] Add all Gradle dependencies from version catalog above
- [ ] Implement `CryptoManager.kt` with Android Keystore AES-256-GCM
- [ ] Write unit tests for encrypt → decrypt round trip
- [ ] Implement `AppDatabase.kt`, `Photo`, `Album`, `IntruderEvent` entities and DAOs
- [ ] Implement `LockLensApp.kt` with Hilt setup
- [ ] Implement `AppNavGraph.kt` with all routes defined
- [ ] Apply Richfield Labs brand colors in `Theme.kt`

### Phase 2 — Auth System (Days 4–5)
- [ ] Implement `LockScreen.kt` — PIN entry with Material 3 number pad UI
- [ ] Implement `AuthRepository.kt` — real PIN vs decoy PIN hash check via DataStore
- [ ] Wire biometric auth with `BiometricPrompt`
- [ ] Implement `DecoyVault.kt` — empty vault shown on decoy PIN
- [ ] App always launches to `LockScreen`; vault unlocks only after successful auth
- [ ] Auto-lock when app goes to background (use `ProcessLifecycleOwner`)

### Phase 3 — Camera (Days 6–8)
- [ ] Implement `CameraScreen.kt` with CameraX `PreviewView` and `ImageCapture`
- [ ] Implement `ExifStripper.kt` — strip all tags listed above
- [ ] Implement `CameraViewModel.kt` — strip → encrypt → save to vault → delete temp file pipeline
- [ ] Generate encrypted thumbnail (256×256 JPEG) alongside full-res
- [ ] Request `CAMERA` permission with graceful fallback
- [ ] Debug assertion: `ExifStripper.hasGpsData()` returns false after strip

### Phase 4 — Vault UI (Days 9–11)
- [ ] Implement `VaultScreen.kt` — `LazyVerticalGrid` of photo thumbnails
- [ ] Implement `PhotoThumbnail.kt` — decrypt-in-memory-only, no disk cache
- [ ] Implement `PhotoDetailScreen.kt` — full-screen decrypted view with pinch-to-zoom
- [ ] Import from gallery: let user select photos from device, strip EXIF, encrypt, move to vault
- [ ] Delete from vault: wipe encrypted file + thumbnail + DB row
- [ ] Album creation and assignment (Pro: unlimited, Free: one default album)

### Phase 5 — Pro Features (Days 12–14)
- [ ] Implement `BillingManager.kt` with Google Play Billing Library 7
- [ ] One SKU: `locklens_pro_lifetime` — one-time purchase
- [ ] Implement `ProGate.kt` composable for upgrade prompts
- [ ] Pro: Decoy PIN setup (second PIN that shows empty vault)
- [ ] Pro: `IntruderDetector.kt` — front camera selfie on failed attempts, encrypted + saved
- [ ] Pro: Video support in vault
- [ ] Pro: Secure share via Android share sheet (FileProvider, temp decrypted file, delete after share)
- [ ] Pro: Unlimited photo count (Free capped at 100)

### Phase 6 — Settings + Polish (Days 15–16)
- [ ] `SettingsScreen.kt`: change PIN, change decoy PIN, toggle auto-lock timeout, view intruder log
- [ ] Auto-lock timeout: 30 seconds / 1 min / 5 mins / immediate (stored in DataStore)
- [ ] On failed unlock: increment counter, show remaining attempts, trigger intruder selfie at threshold
- [ ] `EmptyState.kt` — shown in decoy vault and when real vault is empty
- [ ] APK size optimization — target under 15MB

### Phase 7 — QA + Launch Prep (Days 17–18)
- [ ] Test on physical device: pixel phone + one Samsung (different cameras, different biometric implementations)
- [ ] Verify: no internet calls in network traffic (use Android's Network Inspector)
- [ ] Verify: no EXIF GPS data in captured photos (`ExifStripper.hasGpsData()` debug check)
- [ ] Verify: encrypted files in app internal storage are not readable without the app
- [ ] Verify: uninstall deletes all encrypted files (no orphans on device)
- [ ] Confirm no `INTERNET` permission in merged manifest (`gradlew :app:processDebugManifest`)
- [ ] Store listing copy, 8 screenshots, privacy policy (host at `richfieldlabs.github.io/locklens-privacy`)
- [ ] Submit to Play Store open testing

---

## Security Checklist

Before shipping, verify all of these:

- [ ] `android.permission.INTERNET` does NOT appear in merged manifest
- [ ] No `android:allowBackup="true"` — set to `false` (prevents adb backup of encrypted files)
- [ ] `CryptoManager` uses `AndroidKeyStore` provider, not `"BC"` (Bouncy Castle)
- [ ] IV is unique per file (never reuse an IV with the same key — GCM is catastrophically broken if IV reused)
- [ ] Temp plaintext capture file is deleted in a `finally` block — even if encryption throws
- [ ] Decrypted bitmap is never written to disk — only lives in memory while displayed
- [ ] Coil disk cache disabled for vault images
- [ ] Intruder selfie strips EXIF before encrypting (ironic if the intruder selfie leaks YOUR location)
- [ ] PIN stored as SHA-256 hash, never plaintext
- [ ] Biometric auth uses `BIOMETRIC_STRONG` not `BIOMETRIC_WEAK`

---

## File Storage Layout

All app data lives in `context.filesDir` — private to the app, auto-deleted on uninstall:

```
/data/data/com.richfieldlabs.locklens/files/
├── vault/
│   ├── photos/
│   │   ├── {uuid}.enc         ← encrypted full-res JPEG
│   │   └── ...
│   ├── thumbs/
│   │   ├── {uuid}.enc         ← encrypted 256x256 JPEG thumbnail
│   │   └── ...
│   └── videos/                ← Pro only
│       └── {uuid}.enc
├── intruders/
│   └── {uuid}.enc             ← encrypted intruder selfies
└── locklens.db                ← Room database (SQLite)
```

**Do NOT use `externalFilesDir` or `getExternalStorageDirectory()`** — external storage is readable by other apps with storage permission on older Android versions.

---

## Store Listing

**Title:** `LockLens – Private Photo Vault`
**Short description:** `Private camera + encrypted vault. No metadata. No cloud. No account.`

**Full description:**
```
LockLens is the only app that does both: strips your photo's location data before it's saved,
then locks everything in an AES-256 encrypted vault only you can open.

Every photo you take with Android embeds your GPS location, device model, and timestamp.
Share that photo and you've shared your home address.

Every photo vault app in the top results has been flagged for spyware or aggressive data collection.
LockLens was built to fix both problems at once.

SECURE CAMERA
• Strips ALL EXIF metadata at capture time — no GPS, no device info, no timestamp
• Photos are clean the moment they're shot, before they ever touch storage

ENCRYPTED VAULT
• AES-256-GCM encryption — military-grade, Android Keystore backed
• Biometric lock — fingerprint or face
• Decoy PIN — wrong PIN shows a fake empty vault (Pro)
• Break-in selfie — front camera fires on failed unlock attempts (Pro)

ZERO PERMISSIONS (besides camera)
• No internet permission — verify it yourself in any permission checker
• No contacts, no location, no microphone
• Your photos never leave your phone

SIMPLE PRICING
• Core camera + vault: free forever (up to 100 photos)
• Pro unlock: $4.99 once — unlimited vault, video, decoy PIN, break-in selfie, secure share
• No subscription. No recurring charge. Pay once.

BUILT BY RICHFIELD LABS
From the makers of OffScript and ScanVault. We build apps where your data stays yours.
```

**Keywords:** private photo vault, encrypted gallery Android, photo vault no ads, Keepsafe alternative, secure camera no metadata, hide photos encrypted, EXIF remover camera, private gallery Android

---

## Cross-Promotion with OffScript + ScanVault

After launch, add a small "Also by Richfield Labs" section in each app's Play Store listing linking to the others. Inside LockLens, a dismissible settings card: "Scan documents privately with ScanVault."

The three apps serve the same user: someone who takes privacy seriously and doesn't want their data in the cloud.
