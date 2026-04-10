package com.richfieldlabs.locklens

import android.Manifest
import com.richfieldlabs.locklens.BuildConfig
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.richfieldlabs.locklens.auth.LockTimeout
import com.richfieldlabs.locklens.billing.BillingManager
import com.richfieldlabs.locklens.data.repository.AuthRepository
import com.richfieldlabs.locklens.navigation.AppNavGraph
import com.richfieldlabs.locklens.navigation.Screen
import com.richfieldlabs.locklens.ui.theme.LockLensTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()

        billingManager.connect()

        setContent {
            LockLensTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    // Request POST_NOTIFICATIONS on Android 13+ (needed for intruder alerts)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val notifLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestPermission()
                        ) { /* user decision — notifications are non-critical */ }
                        LaunchedEffect(Unit) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    val lockTimeout by authRepository.authPreferences
                        .map { it.lockTimeout }
                        .collectAsStateWithLifecycle(initialValue = LockTimeout.IMMEDIATE)

                    // Timestamp when the app was last backgrounded (0 = not backgrounded)
                    var backgroundedAt by remember { mutableLongStateOf(0L) }

                    val lifecycleObserver = remember {
                        LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_STOP -> {
                                    backgroundedAt = System.currentTimeMillis()
                                }
                                Lifecycle.Event.ON_START -> {
                                    val at = backgroundedAt
                                    if (at > 0L) {
                                        backgroundedAt = 0L
                                        val elapsed = System.currentTimeMillis() - at
                                        if (elapsed >= lockTimeout.milliseconds) {
                                            navController.navigate(Screen.Lock.route) {
                                                popUpTo(0)
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }

                    DisposableEffect(Unit) {
                        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
                        onDispose {
                            ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
                        }
                    }

                    AppNavGraph(navController = navController)
                }
            }
        }
    }
}
