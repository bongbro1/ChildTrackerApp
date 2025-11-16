package com.example.childtrackerapp.parent.ui.view.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.childtrackerapp.Athu.viewmodel.AuthViewModel
import com.example.childtrackerapp.chatHelper.ui.screen.ChatScreen
import com.example.childtrackerapp.parent.ui.view.LocationScreen
import com.example.childtrackerapp.parent.ui.view.screens.*
import com.example.childtrackerapp.parent.ui.viewmodel.ParentViewModel

@Composable
fun ParentNavGraph(
    navController: NavHostController,
    viewModel: AuthViewModel,
    parentModel: ParentViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = "location", modifier = modifier) {

        composable("location") {
            LocationScreen(parentModel)
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

