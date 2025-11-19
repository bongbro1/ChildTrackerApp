package com.example.childtrackerapp.child.ui.nav

import android.util.Log
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
import com.example.childtrackerapp.child.ui.components.ChildLocationScreen

import com.example.childtrackerapp.child.viewmodel.ChildViewModel
import com.example.childtrackerapp.parent.ui.view.screens.LogOutScreen


@Composable
fun ChildNavGraph(
    navController: NavHostController,
    viewModel: AuthViewModel,
    childModel: ChildViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = "location", modifier = modifier) {

        // LOCATION SCREEN
        composable("location") {
            val child = viewModel.currentUser.collectAsState().value
            ChildLocationScreen(
                childModel = childModel,

                   onOpenChat = {
                    if (child != null) {
                        Log.d("NAV_CHAT", "Navigating to chat/${child.uid}/${child.name}")
                        navController.navigate("chat/${child.uid}/${child.name}")
                    } else {
                        Log.e("NAV_CHAT", "Child is NULL -> Không thể navigate")
                    }
                }

            )
        }

        // CHAT SCREEN (MÀN RIÊNG — KHÔNG GỘP)
        composable(
            route = "chat/{childId}/{childName}",
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("childName") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val childId = backStackEntry.arguments!!.getString("childId")!!
            val childName = backStackEntry.arguments!!.getString("childName")!!

            ChatScreen(
                childId = childId,
                childName = childName
            )
        }


        composable("logout") {
            LogOutScreen(childModel)
        }
    }
}


