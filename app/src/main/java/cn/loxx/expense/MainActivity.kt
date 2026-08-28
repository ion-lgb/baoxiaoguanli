package cn.loxx.expense

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentActivity
import cn.loxx.expense.security.AppLock
import cn.loxx.expense.security.AppLockScreen
import cn.loxx.expense.security.AppLockSession
import cn.loxx.expense.ui.navigation.ExpenseNavHost
import cn.loxx.expense.ui.theme.ExpenseTheme

class MainActivity : FragmentActivity() {
    private val locked = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        locked.value = shouldLockNow()
        setContent {
            ExpenseTheme {
                if (locked.value) {
                    AppLockScreen(onUnlocked = { locked.value = false })
                } else {
                    ExpenseNavHost()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        locked.value = shouldLockNow()
    }

    override fun onStop() {
        super.onStop()
        AppLockSession.lastBackgroundedAt = System.currentTimeMillis()
    }

    private fun shouldLockNow(): Boolean {
        val settings = (application as ExpenseApp).container.settingsRepository
        return AppLock.shouldLock(
            enabled = settings.appLockEnabled,
            unlockedThisSession = AppLockSession.unlocked,
            lastBackgroundedAt = AppLockSession.lastBackgroundedAt,
            now = System.currentTimeMillis(),
        )
    }
}
