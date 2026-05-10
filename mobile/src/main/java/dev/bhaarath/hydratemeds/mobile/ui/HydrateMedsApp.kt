package dev.bhaarath.hydratemeds.mobile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bhaarath.hydratemeds.mobile.ui.theme.HydrateMedsTheme
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import dev.bhaarath.hydratemeds.shared.model.TimelineState
import dev.bhaarath.hydratemeds.shared.schedule.HydrateMedsScheduleConfig
import java.time.LocalDate

@Composable
fun HydrateMedsApp(
    debugWaterConfirmationTrigger: Int = 0,
    viewModel: MobileHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HydrateMedsTheme {
        var tab by remember { mutableIntStateOf(0) }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    listOf("Today", "History", "Stats").forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Text(label.take(1), fontWeight = FontWeight.ExtraBold) },
                            label = { Text(label) },
                        )
                    }
                }
            },
        ) { padding ->
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(180)) togetherWith
                        fadeOut(animationSpec = tween(120))
                },
                label = "tab",
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) { currentTab ->
                when (currentTab) {
                    0 -> TodayScreen(
                        state = state,
                        debugWaterConfirmationTrigger = debugWaterConfirmationTrigger,
                        onAcknowledgeActive = viewModel::acknowledgeActive,
                        onTogglePause = viewModel::togglePause,
                    )
                    1 -> HistoryScreen(state, viewModel::selectDay)
                    else -> StatsScreen(state)
                }
            }
        }
    }
}

@Composable
private fun TodayScreen(
    state: MobileUiState,
    debugWaterConfirmationTrigger: Int,
    onAcknowledgeActive: () -> Unit,
    onTogglePause: (ReminderType) -> Unit,
) {
    var splashTrigger by remember { mutableIntStateOf(0) }
    val acknowledgeActive: (TimelineItem) -> Unit = { item ->
        if (item.event.type == ReminderType.Water) {
            splashTrigger += 1
        }
        onAcknowledgeActive()
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Spacer(Modifier.height(10.dp)) }
            item {
                HeaderCard(
                    state = state,
                    onAcknowledgeActive = acknowledgeActive,
                    onTogglePause = onTogglePause,
                )
            }
            items(state.timeline, key = { it.event.id.value }) { item ->
                TimelineRow(item)
            }
            item { Spacer(Modifier.height(88.dp)) }
        }

        WaterSplashOverlay(trigger = splashTrigger + debugWaterConfirmationTrigger)
    }
}

@Composable
private fun HeaderCard(
    state: MobileUiState,
    onAcknowledgeActive: (TimelineItem) -> Unit,
    onTogglePause: (ReminderType) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Hi Mini,",
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Hydrate & Meds",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        state.selectedDay.format(compactDateFormatter),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    )
                }
                DailyWaterGlass(
                    done = state.waterDone,
                    total = state.waterTarget,
                    active = state.activeReminder?.event?.type == ReminderType.Water,
                )
            }

            if (state.activeReminder != null) {
                ActiveReminderBanner(state.activeReminder, onAcknowledgeActive)
            }

            PauseControls(
                pausedTypes = state.pausedTypes,
                onTogglePause = onTogglePause,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusPill("AM", state.timeline.any {
                    it.event.type == ReminderType.MorningMedicine && it.log != null
                })
                StatusPill("PM", state.timeline.any {
                    it.event.type == ReminderType.EveningMedicine && it.log != null
                })
            }
        }
    }
}

@Composable
private fun PauseControls(
    pausedTypes: Set<ReminderType>,
    onTogglePause: (ReminderType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Quiet today",
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PauseChip(
                label = "Water",
                paused = ReminderType.Water in pausedTypes,
                onClick = { onTogglePause(ReminderType.Water) },
                modifier = Modifier.weight(1f),
            )
            PauseChip(
                label = "AM med",
                paused = ReminderType.MorningMedicine in pausedTypes,
                onClick = { onTogglePause(ReminderType.MorningMedicine) },
                modifier = Modifier.weight(1f),
            )
            PauseChip(
                label = "PM med",
                paused = ReminderType.EveningMedicine in pausedTypes,
                onClick = { onTogglePause(ReminderType.EveningMedicine) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PauseChip(
    label: String,
    paused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (paused) Color(0xFF263235) else Color.White.copy(alpha = 0.64f),
        animationSpec = tween(durationMillis = 120),
        label = "pause-chip",
    )
    Surface(
        color = color,
        contentColor = if (paused) Color(0xFFFFD1C8) else Color(0xFF16363A),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, fontWeight = FontWeight.Bold)
            Text(
                if (paused) "Paused" else "On",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (paused) Color(0xFFFFB4A8) else Color(0xFF0B6B5D),
            )
        }
    }
}

@Composable
private fun ActiveReminderBanner(item: TimelineItem, onDone: (TimelineItem) -> Unit) {
    val isWater = item.event.type == ReminderType.Water
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isWater) {
                WaterGlass(
                    fillFraction = 0.9f,
                    active = true,
                    modifier = Modifier.size(width = 46.dp, height = 58.dp),
                )
            } else {
                Text("Now", fontWeight = FontWeight.ExtraBold)
            }
            Column(Modifier.weight(1f)) {
                Text(item.event.title, fontWeight = FontWeight.Bold)
                Text(item.event.localTime.format(timeFormatter))
            }
            Button(onClick = { onDone(item) }, shape = RoundedCornerShape(14.dp)) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun DailyWaterGlass(
    done: Int,
    total: Int,
    active: Boolean,
) {
    val remaining = (total - done).coerceAtLeast(0)
    val targetFill = if (total == 0) 0f else (remaining / total.toFloat()).coerceIn(0f, 1f)
    val fill by animateFloatAsState(
        targetValue = targetFill,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "daily-glass-fill",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(width = 92.dp, height = 112.dp)) {
            WaterGlass(
                fillFraction = fill,
                active = active,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            "$remaining left",
            color = Color(0xFF06343A),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            "$done / $total cleared",
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
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
            waterColor = Color(0xFF1FB7A6),
            glassFill = Color.White.copy(alpha = 0.32f),
            outline = Color.White.copy(alpha = 0.82f),
            active = active,
        )
    }
}

@Composable
private fun WaterSplashOverlay(trigger: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            visible = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = if (visible) 120 else 320),
        label = "splash-alpha",
    )
    val fill by animateFloatAsState(
        targetValue = if (visible) 0.18f else 0.92f,
        animationSpec = tween(durationMillis = 680),
        label = "splash-fill",
    )
    val checkProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 620),
        label = "check-progress",
    )

    if (alpha > 0.01f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF021D20).copy(alpha = 0.44f * alpha)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = Color(0xFF073F43).copy(alpha = alpha),
                contentColor = Color.White,
                shape = RoundedCornerShape(32.dp),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .padding(horizontal = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(width = 156.dp, height = 150.dp)) {
                        Canvas(Modifier.size(width = 94.dp, height = 112.dp)) {
                            drawTumbler(
                                fillFraction = fill,
                                waterColor = Color(0xFF57E4D1),
                                glassFill = Color.White.copy(alpha = 0.16f),
                                outline = Color.White.copy(alpha = 0.9f),
                                active = true,
                                splash = alpha,
                            )
                        }
                        CelebrationCheckmark(
                            progress = checkProgress,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(62.dp),
                        )
                    }
                    Text(
                        "Good job",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Water logged. The glass level dropped.",
                        color = Color.White.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = { visible = false },
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CelebrationCheckmark(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val p = progress.coerceIn(0f, 1f)
        val strokeWidth = size.minDimension * 0.09f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = Color(0xFF7BE0CD).copy(alpha = 0.22f + 0.48f * p),
            radius = size.minDimension * 0.48f,
            center = center,
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.92f),
            radius = size.minDimension * 0.48f,
            center = center,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        val start = Offset(size.width * 0.28f, size.height * 0.52f)
        val middle = Offset(size.width * 0.44f, size.height * 0.67f)
        val end = Offset(size.width * 0.74f, size.height * 0.36f)
        val firstSegment = 0.42f
        if (p <= firstSegment) {
            val t = p / firstSegment
            drawLine(
                color = Color.White,
                start = start,
                end = Offset(
                    x = start.x + (middle.x - start.x) * t,
                    y = start.y + (middle.y - start.y) * t,
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        } else {
            drawLine(
                color = Color.White,
                start = start,
                end = middle,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            val t = (p - firstSegment) / (1f - firstSegment)
            drawLine(
                color = Color.White,
                start = middle,
                end = Offset(
                    x = middle.x + (end.x - middle.x) * t,
                    y = middle.y + (end.y - middle.y) * t,
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTumbler(
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
            radius = size.minDimension * 0.56f,
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
        val wave = h * 0.018f * (if (active) 1.6f else 1f)
        val water = Path().apply {
            moveTo(leftAt(fillTop), fillTop)
            quadraticBezierTo(w * 0.38f, fillTop - wave, w * 0.5f, fillTop)
            quadraticBezierTo(w * 0.63f, fillTop + wave, rightAt(fillTop), fillTop)
            lineTo(rightBottom, bottomY)
            quadraticBezierTo(w / 2f, bottomY + h * 0.035f, leftBottom, bottomY)
            close()
        }
        drawPath(water, waterColor.copy(alpha = 0.92f))
        drawCircle(waterColor.copy(alpha = 0.36f), radius = w * 0.035f, center = Offset(w * 0.42f, fillTop + h * 0.18f))
        drawCircle(Color.White.copy(alpha = 0.42f), radius = w * 0.022f, center = Offset(w * 0.58f, fillTop + h * 0.27f))
    }

    if (splash > 0f) {
        val center = Offset(w / 2f, topY - h * 0.02f)
        repeat(7) { index ->
            val x = center.x + (index - 3) * w * 0.09f
            val y = center.y - kotlin.math.abs(index - 3) * h * 0.018f - splash * h * 0.11f
            drawCircle(waterColor.copy(alpha = 0.7f * splash), radius = w * (0.018f + index % 2 * 0.005f), center = Offset(x, y))
        }
    }

    drawPath(shell, outline, style = Stroke(width = w * 0.045f, cap = StrokeCap.Round))
    drawRoundRect(
        color = outline.copy(alpha = 0.88f),
        topLeft = Offset(leftTop - w * 0.04f, topY - h * 0.035f),
        size = Size(rightTop - leftTop + w * 0.08f, h * 0.055f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
        style = Stroke(width = w * 0.035f),
    )
    drawLine(
        color = Color.White.copy(alpha = 0.44f),
        start = Offset(leftTop + w * 0.08f, topY + h * 0.12f),
        end = Offset(leftBottom + w * 0.04f, bottomY - h * 0.12f),
        strokeWidth = w * 0.018f,
        cap = StrokeCap.Round,
    )
}

@Composable
private fun StatusPill(label: String, done: Boolean) {
    val color by animateColorAsState(
        if (done) Color(0xFF0B6B5D) else Color.White.copy(alpha = 0.48f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pill",
    )
    Surface(
        color = color,
        contentColor = if (done) Color.White else Color(0xFF16363A),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp, 10.dp))
    }
}

@Composable
private fun TimelineRow(item: TimelineItem) {
    val indicator = when (item.state) {
        TimelineState.Upcoming -> Color(0xFF98A7A7)
        TimelineState.Active -> Color(0xFFCB4F38)
        TimelineState.Paused -> Color(0xFF8C6F00)
        TimelineState.DoneOnTime -> Color(0xFF0B8F74)
        TimelineState.DoneLate -> Color(0xFFD18B00)
        TimelineState.Missed -> Color(0xFF596366)
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(indicator),
            )
            Column(Modifier.weight(1f)) {
                Text(item.event.title, fontWeight = FontWeight.Bold)
                Text(
                    item.event.localTime.format(timeFormatter),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(item.state.label(), fontWeight = FontWeight.Bold, color = indicator)
                item.log?.let {
                    Text(
                        it.acknowledgedAt.atZone(java.time.ZoneId.systemDefault()).toLocalTime()
                            .format(timeFormatter),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    state: MobileUiState,
    onSelectDay: (LocalDate) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("History", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.week.forEach { day ->
                    DayChip(
                        day = day,
                        selected = day.date == state.selectedDay,
                        onClick = { onSelectDay(day.date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        items(state.timeline, key = { it.event.id.value }) { TimelineRow(it) }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun DayChip(
    day: DaySummary,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = day.completion.coerceIn(0f, 1f)
    val percent = (progress * 100).toInt()
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = if (selected) 0.dp else 1.dp,
        modifier = modifier
            .height(82.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                day.date.dayOfWeek.name.take(1),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "$percent%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                softWrap = false,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            Color.White.copy(alpha = 0.28f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        },
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        ),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsScreen(state: MobileUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Stats", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Current", "${state.stats.currentWaterStreak} days", Modifier.weight(1f))
                StatCard("Longest", "${state.stats.longestWaterStreak} days", Modifier.weight(1f))
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("30-day completion", fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.stats.month.forEach { day ->
                            val tone = day.waterCompleted / HydrateMedsScheduleConfig.dailyWaterTarget.toFloat()
                            Box(
                                Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0B8F74).copy(alpha = 0.18f + tone * 0.82f)),
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { if (state.waterTarget == 0) 0f else state.waterDone / state.waterTarget.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF0B8F74),
                    )
                }
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(24.dp), modifier = modifier) {
        Column(Modifier.padding(18.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private fun TimelineState.label(): String =
    when (this) {
        TimelineState.Upcoming -> "Upcoming"
        TimelineState.Active -> "Active"
        TimelineState.Paused -> "Paused"
        TimelineState.DoneOnTime -> "On time"
        TimelineState.DoneLate -> "Late"
        TimelineState.Missed -> "Missed"
    }
