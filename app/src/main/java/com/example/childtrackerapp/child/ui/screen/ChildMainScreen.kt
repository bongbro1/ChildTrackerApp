package com.example.childtrackerapp.child.ui.screen


import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.childtrackerapp.Athu.viewmodel.AuthViewModel
import com.example.childtrackerapp.admin.MainActivity
import com.example.childtrackerapp.chatHelper.data.ChatViewModel
import com.example.childtrackerapp.child.ui.nav.ChildNavGraph
import com.example.childtrackerapp.child.viewmodel.ChildViewModel
import com.example.childtrackerapp.parent.ui.view.BottomNavItem


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildMainScreen(
    authViewModel: AuthViewModel,
    childViewModel: ChildViewModel
) {

    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf("location") }

    val bottomItems = listOf(
        BottomNavItem("location", "Vị trí", android.R.drawable.ic_menu_mylocation),
        BottomNavItem("list", "Danh sách", android.R.drawable.ic_menu_agenda),
        BottomNavItem("account", "Tài khoản", com.example.childtrackerapp.R.drawable.ic_account),
        BottomNavItem("schedule", "Lịch trình", android.R.drawable.ic_menu_my_calendar),
        BottomNavItem("logout", "Logout", android.R.drawable.ic_lock_power_off)
    )

    LaunchedEffect(Unit) {
        authViewModel.loadUserFromFirebase()
    }

    val chatViewModel: ChatViewModel = viewModel()
    val context = LocalContext.current

    // Current child user
    val currentUser = authViewModel.currentUser.collectAsState().value
    val childId = currentUser?.uid ?: ""

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            childViewModel.startSharing(context)
        }
    }

    // 🔔 Realtime notification subscriber
    LaunchedEffect(childId) {
        if (childId.isNotEmpty()) {
            chatViewModel.subscribeRealtimeMessagesWithNotification(childId) { newMsg ->
                if (newMsg.sender != childId) {
                    Toast.makeText(context, "📩 Tin nhắn mới: ${newMsg.text}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theo dõi con") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                bottomItems.forEach { item ->
                    NavigationBarItem(
                        selected = selectedTab == item.route,
                        onClick = {
                            selectedTab = item.route

                            if (item.route == "schedule") {
                                val intent = Intent(context, MainActivity::class.java)
                                context.startActivity(intent)
                            } else {
                                navController.navigate(item.route) {
                                    popUpTo("location") { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = { Icon(painterResource(id = item.icon), contentDescription = item.title) },
                        label = { Text(item.title) }
                    )

                }
            }
        }
    ) { innerPadding ->
        ChildNavGraph(
            navController = navController,
            viewModel = authViewModel,
            childModel = childViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}



data class BottomNavItem(val route: String, val title: String, val icon: Int)
