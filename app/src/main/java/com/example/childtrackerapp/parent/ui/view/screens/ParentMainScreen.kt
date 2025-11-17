package com.example.childtrackerapp.parent.ui.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.rememberNavController
import com.example.childtrackerapp.Athu.viewmodel.AuthViewModel
import com.example.childtrackerapp.parent.ui.view.nav.ParentNavGraph
import com.example.childtrackerapp.parent.ui.viewmodel.ParentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentMainScreen(authViewModel: AuthViewModel,
                     parentViewModel: ParentViewModel
) {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf("location") }

    val items = listOf(
        BottomNavItem("location", "Vị trí", android.R.drawable.ic_menu_mylocation),
        BottomNavItem("list", "Danh sách", android.R.drawable.ic_menu_agenda),
        BottomNavItem("account", "Tài khoản", com.example.childtrackerapp.R.drawable.ic_account),
        BottomNavItem("schedule", "Lịch trình", android.R.drawable.ic_menu_my_calendar),
        BottomNavItem("menu", "Menu", android.R.drawable.ic_menu_more)
    )

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
                items.forEach { item ->
                    NavigationBarItem(
                        selected = selectedTab == item.route,
                        onClick = {
                            selectedTab = item.route
                            navController.navigate(item.route)
                        },
                        icon = { Icon(painterResource(id = item.icon), contentDescription = item.title) },
                        label = { Text(item.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        ParentNavGraph(
            navController = navController,
            viewModel = authViewModel,
            parentModel = parentViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

data class BottomNavItem(val route: String, val title: String, val icon: Int)
