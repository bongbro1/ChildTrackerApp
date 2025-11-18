package com.example.childtrackerapp.parent.ui.view.nav

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.childtrackerapp.Athu.ui.LoginActivity
import com.example.childtrackerapp.Athu.viewmodel.AuthViewModel
import com.example.childtrackerapp.admin.MainActivity
import com.example.childtrackerapp.chatHelper.ui.screen.ChatScreen
import com.example.childtrackerapp.parent.ui.screen.AllowedAppsScreen
import com.example.childtrackerapp.parent.ui.view.LocationScreen
import com.example.childtrackerapp.parent.ui.view.screens.*
import com.example.childtrackerapp.parent.ui.viewmodel.AllowedAppsViewModel
import com.example.childtrackerapp.parent.ui.viewmodel.ParentViewModel

@Composable
fun ParentNavGraph(
    navController: NavHostController,
    viewModel: AuthViewModel,
    parentModel: ParentViewModel,
    allowedAppsViewModel: AllowedAppsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = "location",
        modifier = modifier
    ) {

        // LOCATION
        composable("location") {
            LocationScreen(parentModel)
        }

        // ALLOWED APPS (giữ nguyên)
        composable("allowed_apps") {
            AllowedAppsScreen(
                uiState = allowedAppsViewModel.uiState.collectAsState().value,
                onToggle = { pkg, allowed ->
                    allowedAppsViewModel.setAppAllowed(pkg, allowed)
                },
                onBack = { navController.popBackStack() },
                onAddWebsite = { childId, website ->
                    allowedAppsViewModel.addBlockedWebsite(childId, website)
                },
                onRemoveWebsite = { childId, website ->
                    allowedAppsViewModel.removeBlockedWebsite(childId, website)
                },
                onChildSelected = { childId ->
                    allowedAppsViewModel.onChildSelected(childId)
                }
            )
        }

        // LOGOUT
        composable("logout") {
            LogOutScreen(parentModel)
        }

        // SCHEDULE (chuyển sang Activity admin)
        composable("schedule") {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
        }

        // ACCOUNT
        composable("account") {
            ParentAccountScreen(
                viewModel = viewModel,
                navController = navController
            )
        }

        // CHAT
        composable(
            route = "chat/{childId}/{childName}",
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("childName") { type = NavType.StringType }
            )
        ) { backStack ->

            val childId = backStack.arguments!!.getString("childId")!!
            val childName = backStack.arguments!!.getString("childName")!!

            ChatScreen(
                childId = childId,
                childName = childName
            )
        }
    }
}
