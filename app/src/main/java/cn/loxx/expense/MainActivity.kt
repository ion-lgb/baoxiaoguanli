package cn.loxx.expense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cn.loxx.expense.ui.navigation.ExpenseNavHost
import cn.loxx.expense.ui.theme.ExpenseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTheme {
                ExpenseNavHost()
            }
        }
    }
}
