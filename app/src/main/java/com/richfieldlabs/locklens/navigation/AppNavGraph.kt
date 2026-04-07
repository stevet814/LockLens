package com.richfieldlabs.locklens.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.richfieldlabs.locklens.auth.LockScreen
import com.richfieldlabs.locklens.camera.CameraScreen
import com.richfieldlabs.locklens.settings.SettingsScreen
import com.richfieldlabs.locklens.vault.AlbumScreen
import com.richfieldlabs.locklens.vault.PhotoDetailScreen
import com.richfieldlabs.locklens.vault.VaultScreen

sealed class Screen(val route: String) {
    data object Lock : Screen("lock")
    data object Vault : Screen("vault?decoy={decoy}") {
        fun createRoute(decoy: Boolean) = "vault?decoy=$decoy"
    }
    data object Camera : Screen("camera")
    data object PhotoDetail : Screen("photo_detail/{photoId}") {
        fun createRoute(photoId: Long) = "photo_detail/$photoId"
    }
    data object Album : Screen("album/{albumId}") {
        fun createRoute(albumId: Long) = "album/$albumId"
    }
    data object Settings : Screen("settings")
}

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Lock.route,
        modifier = modifier,
    ) {
        composable(Screen.Lock.route) {
            LockScreen(
                onUnlocked = { decoy ->
                    navController.navigate(Screen.Vault.createRoute(decoy)) {
                        launchSingleTop = true
                    }
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                },
            )
        }

        composable(
            route = Screen.Vault.route,
            arguments = listOf(
                navArgument("decoy") {
                    defaultValue = false
                    type = NavType.BoolType
                },
            ),
        ) { backStackEntry ->
            val decoyMode = backStackEntry.arguments?.getBoolean("decoy") ?: false
            VaultScreen(
                decoyMode = decoyMode,
                onOpenCamera = { navController.navigate(Screen.Camera.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenPhoto = { photoId ->
                    navController.navigate(Screen.PhotoDetail.createRoute(photoId))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Camera.route) {
            CameraScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.PhotoDetail.route,
            arguments = listOf(navArgument("photoId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong("photoId") ?: 0L
            PhotoDetailScreen(
                photoId = photoId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Album.route,
            arguments = listOf(navArgument("albumId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getLong("albumId") ?: 0L
            AlbumScreen(
                albumId = albumId,
                onBack = { navController.popBackStack() },
                onOpenPhoto = { photoId ->
                    navController.navigate(Screen.PhotoDetail.createRoute(photoId))
                },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

