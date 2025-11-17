package com.example.childtrackerapp.schedule.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.childtrackerapp.schedule.ui.add.AddScheduleScreen
import com.example.childtrackerapp.schedule.ui.daily.DailyScreen
import com.example.childtrackerapp.schedule.ui.edit.EditScheduleScreen
import com.example.childtrackerapp.schedule.ui.weekly.WeeklyScreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = "daily"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Daily Screen - hỗ trợ nhận date từ weekly screen
        composable(
            route = "daily?date={date}",
            arguments = listOf(
                navArgument("date") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val selectedDate = backStackEntry.arguments?.getString("date")

            DailyScreen(
                initialDate = selectedDate, // Truyền ngày được chọn từ weekly
                onNavigateToAdd = {
                    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    navController.navigate("add_schedule/$today")
                },
                onNavigateToEdit = { scheduleId ->
                    navController.navigate("edit_schedule/$scheduleId")
                },
                onNavigateToWeekly = {
                    navController.navigate("weekly")
                }
            )
        }

        // Weekly Screen
        composable("weekly") {
            WeeklyScreen(
                onNavigateToDaily = { date ->
                    // Navigate to daily screen with selected date
                    navController.navigate("daily?date=$date") {
                        popUpTo("daily") { inclusive = true }
                    }
                },
                onNavigateToAdd = { date ->
                    navController.navigate("add_schedule/$date")
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Add Schedule Screen
        composable(
            route = "add_schedule/{date}",
            arguments = listOf(
                navArgument("date") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            AddScheduleScreen(
                selectedDate = date,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Edit Schedule Screen
        composable(
            route = "edit_schedule/{scheduleId}",
            arguments = listOf(
                navArgument("scheduleId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val scheduleId = backStackEntry.arguments?.getString("scheduleId") ?: ""
            EditScheduleScreen(
                scheduleId = scheduleId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}