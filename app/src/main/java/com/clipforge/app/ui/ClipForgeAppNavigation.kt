package com.clipforge.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.clipforge.app.ui.screens.ClipPreviewScreen
import com.clipforge.app.ui.screens.HomeScreen
import com.clipforge.app.ui.screens.ImportScreen
import com.clipforge.app.ui.screens.ProjectScreen
import com.clipforge.app.ui.screens.SettingsScreen

@Composable
fun ClipForgeAppNavigation(viewModel: ClipForgeViewModel = viewModel()) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onOpenProject = { id -> navController.navigate("project/$id") },
                onSettings = { navController.navigate("settings") },
            )
        }
        composable("settings") { SettingsScreen(viewModel, onBack = { navController.popBackStack() }) }
        composable("project/{projectId}", arguments = listOf(navArgument("projectId") { type = NavType.StringType })) { entry ->
            val projectId = entry.arguments?.getString("projectId") ?: return@composable
            ProjectScreen(
                projectId = projectId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onImport = { navController.navigate("import/$projectId") },
                onPreview = { id -> navController.navigate("preview/$id") },
            )
        }
        composable("import/{projectId}", arguments = listOf(navArgument("projectId") { type = NavType.StringType })) { entry ->
            val projectId = entry.arguments?.getString("projectId") ?: return@composable
            ImportScreen(projectId, viewModel, onBack = { navController.popBackStack() })
        }
        composable("preview/{clipId}", arguments = listOf(navArgument("clipId") { type = NavType.StringType })) { entry ->
            val clipId = entry.arguments?.getString("clipId") ?: return@composable
            ClipPreviewScreen(clipId, viewModel, onBack = { navController.popBackStack() })
        }
    }
}
