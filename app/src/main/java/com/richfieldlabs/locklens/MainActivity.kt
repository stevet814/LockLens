package com.richfieldlabs.locklens

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.richfieldlabs.locklens.navigation.AppNavGraph
import com.richfieldlabs.locklens.navigation.Screen
import com.richfieldlabs.locklens.ui.theme.LockLensTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LockLensTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    
                    // Observe app lifecycle for auto-lock
                    val lifecycleObserver = remember {
                        LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_STOP) {
                                // App went to background
                                navController.navigate(Screen.Lock.route) {
                                    // Clear backstack so user must unlock again
                                    popUpTo(0)
                                    launchSingleTop = true
                                }
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
