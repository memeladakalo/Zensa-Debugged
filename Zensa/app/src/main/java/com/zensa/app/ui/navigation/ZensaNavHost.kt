package com.zensa.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zensa.app.di.ServiceLocator
import com.zensa.app.ui.screens.achievements.AchievementsScreen
import com.zensa.app.ui.screens.assignments.AssignmentDetailScreen
import com.zensa.app.ui.screens.assignments.AssignmentFormScreen
import com.zensa.app.ui.screens.assignments.AssignmentsScreen
import com.zensa.app.ui.screens.dashboard.DashboardScreen
import com.zensa.app.ui.screens.login.LoginScreen
import com.zensa.app.ui.screens.notes.NoteFormScreen
import com.zensa.app.ui.screens.notes.NotesScreen
import com.zensa.app.ui.screens.register.RegisterScreen
import com.zensa.app.ui.screens.settings.SettingsScreen
import com.zensa.app.ui.screens.splash.SplashScreen
import com.zensa.app.ui.screens.timer.ZenTimerScreen

@Composable
fun ZensaNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {

        // ---------------- Splash ----------------
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateNext = {
                    val destination = if (ServiceLocator.authRepository.isLoggedIn())
                        Routes.DASHBOARD else Routes.LOGIN
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // ---------------- Login ----------------
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        // ---------------- Register ----------------
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ---------------- Dashboard ----------------
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                },
                onOpenAssignments = { navController.navigate(Routes.ASSIGNMENTS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenTimer = { navController.navigate(Routes.TIMER) },
                onOpenNotes = { navController.navigate(Routes.NOTES) },
                onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                onOpenAssignmentDetail = { localId ->
                    navController.navigate(Routes.assignmentDetail(localId))
                }
            )
        }

        // ---------------- Assignments list ----------------
        composable(Routes.ASSIGNMENTS) {
            AssignmentsScreen(
                onBack = { navController.popBackStack() },
                onAddAssignment = { navController.navigate(Routes.ASSIGNMENT_FORM) },
                onOpenAssignment = { localId ->
                    navController.navigate(Routes.assignmentDetail(localId))
                }
            )
        }

        // ---------------- Assignment form (CREATE) ----------------
        composable(Routes.ASSIGNMENT_FORM) {
            AssignmentFormScreen(
                assignmentLocalId = null,
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // ---------------- Assignment form (EDIT) ----------------
        composable(
            route = Routes.ASSIGNMENT_FORM_EDIT,
            arguments = listOf(
                navArgument(Routes.ARG_ASSIGNMENT_LOCAL_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val localId = backStackEntry.arguments
                ?.getString(Routes.ARG_ASSIGNMENT_LOCAL_ID) ?: return@composable
            AssignmentFormScreen(
                assignmentLocalId = localId,
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // ---------------- Assignment details ----------------
        composable(
            route = Routes.ASSIGNMENT_DETAIL,
            arguments = listOf(
                navArgument(Routes.ARG_ASSIGNMENT_LOCAL_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val localId = backStackEntry.arguments
                ?.getString(Routes.ARG_ASSIGNMENT_LOCAL_ID) ?: return@composable
            AssignmentDetailScreen(
                assignmentLocalId = localId,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.assignmentFormEdit(id)) },
                onDeleted = { navController.popBackStack() }
            )
        }

        // ---------------- Notes list ----------------
        composable(Routes.NOTES) {
            NotesScreen(
                onBack = { navController.popBackStack() },
                onAddNote = { navController.navigate(Routes.NOTE_FORM) },
                onOpenNote = { localId ->
                    navController.navigate(Routes.noteFormEdit(localId))
                }
            )
        }

        // ---------------- Note form (CREATE) ----------------
        composable(Routes.NOTE_FORM) {
            NoteFormScreen(
                noteLocalId = null,
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() }
            )
        }

        // ---------------- Note form (EDIT) ----------------
        composable(
            route = Routes.NOTE_FORM_EDIT,
            arguments = listOf(
                navArgument(Routes.ARG_ASSIGNMENT_LOCAL_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val localId = backStackEntry.arguments
                ?.getString(Routes.ARG_ASSIGNMENT_LOCAL_ID) ?: return@composable
            NoteFormScreen(
                noteLocalId = localId,
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() }
            )
        }

        // ---------------- Settings ----------------
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                }
            )
        }

        // ---------------- Zen Timer ----------------
        composable(Routes.TIMER) {
            ZenTimerScreen(onBack = { navController.popBackStack() })
        }

        // ---------------- Achievements ----------------
        composable(Routes.ACHIEVEMENTS) {
            AchievementsScreen(onBack = { navController.popBackStack() })
        }
    }
}
