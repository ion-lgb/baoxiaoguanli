package cn.loxx.expense.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF6D5EF2)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFFE4DFFF)
private val OnPrimaryContainer = Color(0xFF1A0066)
private val Secondary = Color(0xFF5E5D72)
private val BackgroundLight = Color(0xFFFCF8FF)
private val BackgroundDark = Color(0xFF14131B)

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    background = BackgroundLight,
    surface = BackgroundLight,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC8BFFF),
    onPrimary = Color(0xFF2E159B),
    primaryContainer = Color(0xFF4534B6),
    onPrimaryContainer = Color(0xFFE4DFFF),
    secondary = Color(0xFFC6C5DD),
    background = BackgroundDark,
    surface = BackgroundDark,
)

@Composable
fun ExpenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
