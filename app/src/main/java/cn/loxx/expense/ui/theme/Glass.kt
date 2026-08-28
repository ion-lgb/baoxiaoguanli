package cn.loxx.expense.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.cos
import kotlin.math.sin

/** Glass shapes used across the app. */
object GlassShapes {
    val card: Shape = RoundedCornerShape(24.dp)
    val item: Shape = RoundedCornerShape(18.dp)
    val chip: Shape = RoundedCornerShape(12.dp)
}

/** Status accent colors resolved per theme. */
@Immutable
data class StatusColors(
    val ongoing: Color,
    val completed: Color,
    val reported: Color,
)

/** Fixed hue palette used for category breakdown charts. */
val CategoryPalette = listOf(
    Color(0xFF6D5EF2),
    Color(0xFF0EA5A6),
    Color(0xFFE86FA4),
    Color(0xFFE89A3C),
    Color(0xFF4B9FE8),
    Color(0xFF7CB342),
    Color(0xFFE05E5E),
    Color(0xFF9575CD),
)

fun categoryColor(index: Int): Color = CategoryPalette[index % CategoryPalette.size]

private val LightStatusColors = StatusColors(
    ongoing = Color(0xFF6D5EF2),
    completed = Color(0xFF2E9E63),
    reported = Color(0xFF0E9BA6),
)

private val DarkStatusColors = StatusColors(
    ongoing = Color(0xFFC8BFFF),
    completed = Color(0xFF8FD6AE),
    reported = Color(0xFF7FD8DE),
)

@Composable
fun rememberStatusColors(): StatusColors =
    if (isSystemInDarkTheme()) DarkStatusColors else LightStatusColors

/**
 * Provides the aurora background shared by every screen. Screens host their
 * content inside [GlassScaffold]; glass surfaces blur the aurora through
 * [LocalHazeState]. Null when the surface cannot blur (sheets, dialogs) —
 * those fall back to a translucent scrim.
 */
val LocalHazeState: ProvidableCompositionLocal<dev.chrisbanes.haze.HazeState?> =
    staticCompositionLocalOf { null }

@Composable
fun GlassScaffold(
    topBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable BoxScope.(PaddingValues) -> Unit,
) {
    val hazeState = rememberHazeState()
    androidx.compose.runtime.CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            AuroraBackground(
                modifier = Modifier
                    .matchParentSize()
                    .hazeSource(hazeState),
            )
            androidx.compose.material3.Scaffold(
                containerColor = Color.Transparent,
                topBar = topBar,
                floatingActionButton = floatingActionButton,
            ) { innerPadding ->
                Box(Modifier.fillMaxSize()) {
                    content(innerPadding)
                }
            }
        }
    }
}

/** Slowly drifting radial-gradient blobs behind the frosted glass surfaces. */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val base = if (dark) Color(0xFF0E0D16) else Color(0xFFF3F1FB)
    val blobs = if (dark) {
        listOf(
            Blob(anchorX = 0.15f, anchorY = 0.05f, radius = 0.75f, color = Color(0xFF6D5EF2).copy(alpha = 0.34f)),
            Blob(anchorX = 0.95f, anchorY = 0.55f, radius = 0.6f, color = Color(0xFF2BB5A0).copy(alpha = 0.20f)),
            Blob(anchorX = 0.35f, anchorY = 1.0f, radius = 0.65f, color = Color(0xFFB05EC8).copy(alpha = 0.16f)),
        )
    } else {
        listOf(
            Blob(anchorX = 0.1f, anchorY = 0.0f, radius = 0.75f, color = Color(0xFF6D5EF2).copy(alpha = 0.24f)),
            Blob(anchorX = 1.0f, anchorY = 0.45f, radius = 0.6f, color = Color(0xFF5EC8F2).copy(alpha = 0.22f)),
            Blob(anchorX = 0.3f, anchorY = 1.0f, radius = 0.65f, color = Color(0xFFF27FB2).copy(alpha = 0.18f)),
        )
    }

    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 36_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "aurora-phase",
    )

    Canvas(modifier) {
        drawRect(base)
        blobs.forEachIndexed { index, blob ->
            val wobble = phase + index * 2.1f
            val cx = size.width * (blob.anchorX + 0.05f * sin(wobble))
            val cy = size.height * (blob.anchorY + 0.04f * cos(wobble * 0.8f))
            val radius = size.minDimension * blob.radius
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob.color, Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }
    }
}

private data class Blob(
    val anchorX: Float,
    val anchorY: Float,
    val radius: Float,
    val color: Color,
)

@Composable
private fun glassStyle(): Pair<HazeStyle, Brush> {
    val dark = isSystemInDarkTheme()
    return if (dark) {
        HazeStyle(
            backgroundColor = Color(0xFF0E0D16),
            tints = listOf(HazeTint(Color(0xFF23203A).copy(alpha = 0.62f))),
            fallbackTint = HazeTint(Color(0xFF1D1A31).copy(alpha = 0.88f)),
            blurRadius = 28.dp,
            noiseFactor = 0f,
        ) to Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.02f)),
        )
    } else {
        HazeStyle(
            backgroundColor = Color(0xFFF3F1FB),
            tints = listOf(HazeTint(Color.White.copy(alpha = 0.52f))),
            fallbackTint = HazeTint(Color.White.copy(alpha = 0.78f)),
            blurRadius = 28.dp,
            noiseFactor = 0f,
        ) to Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.25f)),
        )
    }
}

/** Frost fill + specular border for an in-screen glass surface (blurs the aurora). */
@Composable
fun Modifier.glassEffect(shape: Shape = GlassShapes.card): Modifier {
    val hazeState = LocalHazeState.current
    val (style, border) = glassStyle()
    // clip first so the rectangular blur layer is masked to the rounded shape
    val base = if (hazeState != null) {
        Modifier
            .clip(shape)
            .hazeEffect(state = hazeState) { this.style = style }
    } else {
        Modifier.background(style.fallbackTint?.color ?: Color.White, shape)
    }
    return base
        .glassBorder(shape, border)
}

/** Frost fill for surfaces in overlay windows (sheets/menus) where backdrop blur is unavailable. */
@Composable
fun Modifier.glassScrim(shape: Shape = GlassShapes.card): Modifier {
    val (_, border) = glassStyle()
    val dark = isSystemInDarkTheme()
    val fill = if (dark) Color(0xFF1D1A31).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.9f)
    return background(fill, shape).glassBorder(shape, border)
}

private fun Modifier.glassBorder(shape: Shape, brush: Brush, width: Dp = 1.dp): Modifier =
    drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(outline, brush, style = Stroke(width.toPx()))
    }

/** Frosted card used on aurora-backed screens. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassShapes.card,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val container = modifier.glassEffect(shape)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = Color.Transparent,
            contentColor = contentColorFor(MaterialTheme.colorScheme.background),
            modifier = container,
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            shape = shape,
            color = Color.Transparent,
            contentColor = contentColorFor(MaterialTheme.colorScheme.background),
            modifier = container,
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/** Mixes [amount] of white/black to soften text colors on glass, per theme. */
@Composable
fun Color.glassAdjusted(): Color {
    val dark = isSystemInDarkTheme()
    return if (dark) lerp(this, Color.White, 0.1f) else this
}
