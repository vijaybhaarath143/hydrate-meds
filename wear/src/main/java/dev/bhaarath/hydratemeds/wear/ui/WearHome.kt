package dev.bhaarath.hydratemeds.wear.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bhaarath.hydratemeds.shared.R as SharedR
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import dev.bhaarath.hydratemeds.wear.WearHomeViewModel
import dev.bhaarath.hydratemeds.wear.WearUiState
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

private val WearBackground = Color(0xFF041416)
private val Aqua = Color(0xFF7BE0CD)
private val DeepAqua = Color(0xFF063D3A)
private val SoftSurface = Color(0xFF102629)
private val Pill = Color(0xFFE8D56F)
private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

private val InterFontFamily = FontFamily(
    Font(SharedR.font.inter_variable, weight = FontWeight.Light),
    Font(SharedR.font.inter_variable, weight = FontWeight.Normal),
    Font(SharedR.font.inter_variable, weight = FontWeight.Medium),
    Font(SharedR.font.inter_variable, weight = FontWeight.SemiBold),
    Font(SharedR.font.inter_variable, weight = FontWeight.Bold),
    Font(SharedR.font.inter_variable, weight = FontWeight.ExtraBold),
)

private val BaseTypography = Typography()

private val InterTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.ExtraBold),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.ExtraBold),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.ExtraBold),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium),
)

@Composable
fun WearHome(viewModel: WearHomeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var splashTrigger by remember { mutableIntStateOf(0) }
    MaterialTheme(typography = InterTypography) {
        Surface(color = WearBackground, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = state.activeEvent != null,
                    label = "active",
                ) { isActive ->
                    if (isActive) {
                        ActiveReminderScreen(
                            state = state,
                            onDone = {
                                if (state.activeEvent?.type == ReminderType.Water) {
                                    splashTrigger += 1
                                }
                                viewModel.acknowledgeActive()
                            },
                        )
                    } else {
                        WearProgressScreen(state, viewModel::logWater)
                    }
                }
                WatchSplashOverlay(trigger = splashTrigger)
            }
        }
    }
}

@Composable
private fun WearProgressScreen(
    state: WearUiState,
    onLogWater: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val waterComplete = state.waterDone >= state.waterTarget
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        state.nextEvent?.let {
            val nextKind = if (it.type == ReminderType.Water) "WATER" else "MED"
            Text(
                "NEXT $nextKind",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                it.localTime.format(timeFormatter).uppercase(),
                color = Aqua,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        WatchDailyGlass(
            done = state.waterDone,
            target = state.waterTarget,
            canLog = !waterComplete,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onLogWater()
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MedicinePill(isMorning = true, done = state.morningDone)
            MedicinePill(isMorning = false, done = state.eveningDone)
        }
    }
}

@Composable
private fun ActiveReminderScreen(
    state: WearUiState,
    onDone: () -> Unit,
) {
    val event = state.activeEvent ?: return
    val haptics = LocalHapticFeedback.current
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (event.type == ReminderType.Water) {
            WaterGlass(
                fillFraction = 0.9f,
                active = true,
                modifier = Modifier
                    .size(width = 68.dp, height = 80.dp)
                    .scale(pulse),
            )
        } else {
            ReminderIcon(event.type, Modifier.scale(pulse))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            event.title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onDone()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Aqua, contentColor = Color(0xFF002B28)),
            shape = RoundedCornerShape(24.dp),
        ) {
            Text("Done", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WatchDailyGlass(
    done: Int,
    target: Int,
    canLog: Boolean,
    onClick: () -> Unit,
) {
    val remaining = (target - done).coerceAtLeast(0)
    val targetFill = if (target == 0) 0f else remaining / target.toFloat()
    val fill by animateFloatAsState(
        targetValue = targetFill.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 90f),
        label = "watch-glass-fill",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 70.dp, height = 80.dp)
            .clickable(enabled = canLog, onClick = onClick),
    ) {
        WaterGlass(
            fillFraction = fill,
            active = false,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            "$done/$target",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun WaterGlass(
    fillFraction: Float,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        drawTumbler(
            fillFraction = fillFraction.coerceIn(0f, 1f),
            waterColor = Aqua,
            glassFill = Color.White.copy(alpha = 0.08f),
            outline = Color.White.copy(alpha = 0.72f),
            active = active,
        )
    }
}

@Composable
private fun WatchSplashOverlay(trigger: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            visible = true
            delay(780)
            visible = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = if (visible) 100 else 260),
        label = "watch-splash-alpha",
    )
    val fill by animateFloatAsState(
        targetValue = if (visible) 0.18f else 0.92f,
        animationSpec = tween(durationMillis = 560),
        label = "watch-splash-fill",
    )

    if (alpha > 0.01f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WearBackground.copy(alpha = 0.72f * alpha)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = DeepAqua.copy(alpha = alpha),
                contentColor = Color.White,
                shape = CircleShape,
            ) {
                Column(
                    modifier = Modifier
                        .size(142.dp)
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Canvas(Modifier.size(width = 58.dp, height = 70.dp)) {
                        drawTumbler(
                            fillFraction = fill,
                            waterColor = Aqua,
                            glassFill = Color.White.copy(alpha = 0.08f),
                            outline = Color.White.copy(alpha = 0.82f),
                            active = true,
                            splash = alpha,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Done", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

private fun DrawScope.drawTumbler(
    fillFraction: Float,
    waterColor: Color,
    glassFill: Color,
    outline: Color,
    active: Boolean,
    splash: Float = 0f,
) {
    val w = size.width
    val h = size.height
    val topY = h * 0.1f
    val bottomY = h * 0.9f
    val topInset = w * 0.18f
    val bottomInset = w * 0.28f
    val leftTop = topInset
    val rightTop = w - topInset
    val leftBottom = bottomInset
    val rightBottom = w - bottomInset

    fun tFor(y: Float) = ((y - topY) / (bottomY - topY)).coerceIn(0f, 1f)
    fun leftAt(y: Float): Float {
        val t = tFor(y)
        return leftTop + (leftBottom - leftTop) * t
    }
    fun rightAt(y: Float): Float {
        val t = tFor(y)
        return rightTop + (rightBottom - rightTop) * t
    }

    if (active) {
        drawCircle(
            color = waterColor.copy(alpha = 0.16f),
            radius = size.minDimension * 0.58f,
            center = Offset(w / 2f, h * 0.52f),
        )
    }

    val shell = Path().apply {
        moveTo(leftTop, topY)
        lineTo(rightTop, topY)
        lineTo(rightBottom, bottomY)
        quadraticBezierTo(w / 2f, bottomY + h * 0.05f, leftBottom, bottomY)
        close()
    }
    drawPath(shell, glassFill)

    if (fillFraction > 0.02f) {
        val fillTop = bottomY - (bottomY - topY) * fillFraction
        val wave = h * 0.02f * (if (active) 1.45f else 1f)
        val water = Path().apply {
            moveTo(leftAt(fillTop), fillTop)
            quadraticBezierTo(w * 0.38f, fillTop - wave, w * 0.5f, fillTop)
            quadraticBezierTo(w * 0.63f, fillTop + wave, rightAt(fillTop), fillTop)
            lineTo(rightBottom, bottomY)
            quadraticBezierTo(w / 2f, bottomY + h * 0.035f, leftBottom, bottomY)
            close()
        }
        drawPath(water, waterColor.copy(alpha = 0.92f))
        drawCircle(Color.White.copy(alpha = 0.42f), radius = w * 0.025f, center = Offset(w * 0.58f, fillTop + h * 0.25f))
    }

    if (splash > 0f) {
        val center = Offset(w / 2f, topY - h * 0.02f)
        repeat(5) { index ->
            val x = center.x + (index - 2) * w * 0.11f
            val y = center.y - kotlin.math.abs(index - 2) * h * 0.02f - splash * h * 0.12f
            drawCircle(waterColor.copy(alpha = 0.75f * splash), radius = w * 0.022f, center = Offset(x, y))
        }
    }

    drawPath(shell, outline, style = Stroke(width = w * 0.05f, cap = StrokeCap.Round))
    drawRoundRect(
        color = outline.copy(alpha = 0.88f),
        topLeft = Offset(leftTop - w * 0.04f, topY - h * 0.035f),
        size = Size(rightTop - leftTop + w * 0.08f, h * 0.055f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
        style = Stroke(width = w * 0.035f),
    )
    drawLine(
        color = Color.White.copy(alpha = 0.32f),
        start = Offset(leftTop + w * 0.08f, topY + h * 0.13f),
        end = Offset(leftBottom + w * 0.04f, bottomY - h * 0.12f),
        strokeWidth = w * 0.018f,
        cap = StrokeCap.Round,
    )
}

@Composable
private fun MedicinePill(isMorning: Boolean, done: Boolean) {
    val background = if (done) Pill else SoftSurface
    val foreground = if (done) Color(0xFF2F2A00) else Color.White.copy(alpha = 0.76f)
    Surface(
        color = background,
        contentColor = foreground,
        shape = RoundedCornerShape(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 26.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                if (isMorning) {
                    drawSun(foreground)
                } else {
                    drawMoon(foreground, background)
                }
            }
        }
    }
}

private fun DrawScope.drawSun(color: Color) {
    val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension * 0.2f
    drawCircle(color, radius = radius, center = center)
    repeat(8) { index ->
        val angle = Math.toRadians((index * 45).toDouble())
        val inner = radius * 1.45f
        val outer = radius * 2.25f
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * inner,
                y = center.y + kotlin.math.sin(angle).toFloat() * inner,
            ),
            end = androidx.compose.ui.geometry.Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * outer,
                y = center.y + kotlin.math.sin(angle).toFloat() * outer,
            ),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawMoon(color: Color, cutout: Color) {
    val center = androidx.compose.ui.geometry.Offset(size.width * 0.48f, size.height * 0.5f)
    val radius = size.minDimension * 0.42f
    drawCircle(color, radius = radius, center = center)
    drawCircle(
        color = cutout,
        radius = radius * 0.82f,
        center = androidx.compose.ui.geometry.Offset(
            x = center.x + radius * 0.46f,
            y = center.y - radius * 0.16f,
        ),
    )
}

@Composable
private fun ReminderIcon(type: ReminderType, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(82.dp)
            .clip(CircleShape)
            .background(if (type == ReminderType.Water) DeepAqua else Color(0xFF3B3320)),
    ) {
        Canvas(Modifier.size(52.dp)) {
            if (type == ReminderType.Water) {
                drawCircle(Aqua, radius = size.minDimension * 0.42f)
                drawCircle(Color.White.copy(alpha = 0.45f), radius = size.minDimension * 0.18f)
            } else {
                drawRoundRect(Pill, cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f))
                drawLine(
                    Color(0xFF3B3320),
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.5f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height),
                    strokeWidth = 4.dp.toPx(),
                )
            }
        }
    }
}
