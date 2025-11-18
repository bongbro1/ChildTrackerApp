package com.example.childtrackerapp.parent.ui.view.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.childtrackerapp.Athu.viewmodel.AuthViewModel
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
    NavHost(navController = navController, startDestination = "location", modifier = modifier) {

        composable("location") {
            LocationScreen(parentModel)
        }
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

        composable("account") {
            ParentAccountScreen(
                viewModel = viewModel,
                navController = navController
            )
        }

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

