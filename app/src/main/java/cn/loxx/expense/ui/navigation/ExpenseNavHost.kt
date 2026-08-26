package cn.loxx.expense.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import cn.loxx.expense.ui.expense.AddEditExpenseScreen
import cn.loxx.expense.ui.expense.ReceiptViewScreen
import cn.loxx.expense.ui.home.HomeScreen
import cn.loxx.expense.ui.report.ReportPreviewScreen
import cn.loxx.expense.ui.settings.SettingsScreen
import cn.loxx.expense.ui.trip.TripDetailScreen

@Composable
fun ExpenseNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onTripClick = { tripId -> navController.navigate(TripDetailRoute(tripId)) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<TripDetailRoute> { entry ->
            val route = entry.toRoute<TripDetailRoute>()
            TripDetailScreen(
                tripId = route.tripId,
                onBack = { navController.popBackStack() },
                onAddExpense = { navController.navigate(AddEditExpenseRoute(route.tripId)) },
                onEditExpense = { expenseId ->
                    navController.navigate(AddEditExpenseRoute(route.tripId, expenseId))
                },
                onViewReceipt = { receiptId ->
                    navController.navigate(ReceiptViewRoute(receiptId))
                },
                onGenerateReport = { navController.navigate(ReportPreviewRoute(route.tripId)) },
            )
        }
        composable<AddEditExpenseRoute> { entry ->
            val route = entry.toRoute<AddEditExpenseRoute>()
            AddEditExpenseScreen(
                tripId = route.tripId,
                expenseId = route.expenseId,
                onBack = { navController.popBackStack() },
            )
        }
        composable<ReceiptViewRoute> { entry ->
            val route = entry.toRoute<ReceiptViewRoute>()
            ReceiptViewScreen(receiptId = route.receiptId, onBack = { navController.popBackStack() })
        }
        composable<ReportPreviewRoute> { entry ->
            val route = entry.toRoute<ReportPreviewRoute>()
            ReportPreviewScreen(tripId = route.tripId, onBack = { navController.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
