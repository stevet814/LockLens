# ── Hilt ─────────────────────────────────────────────────────────────────────
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract *;
}

# ── Kotlin data classes used by Room / DataStore ──────────────────────────────
-keepclassmembers class com.richfieldlabs.locklens.data.model.** {
    *;
}
-keepclassmembers class com.richfieldlabs.locklens.data.repository.** {
    *;
}

# ── Kotlin coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Play Billing ──────────────────────────────────────────────────────────────
-keep class com.android.billingclient.** { *; }

# ── ExoPlayer / Media3 ────────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── CameraX ───────────────────────────────────────────────────────────────────
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ── Biometric ────────────────────────────────────────────────────────────────
-keep class androidx.biometric.** { *; }

# ── Enum entries (used by fromStorageValue helpers) ───────────────────────────
-keepclassmembers enum com.richfieldlabs.locklens.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Suppress warnings from optional dependencies ─────────────────────────────
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
