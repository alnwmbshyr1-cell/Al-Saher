package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AgentTask
import com.example.model.UserProfile
import com.example.ui.components.calculateCreditProgress
import com.example.util.UsageAnalyticsHelper
import com.example.util.UsageChartPoint
import com.example.util.UsageDashboardSummary
import com.example.util.UsageTimeframe

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UsageDashboardScreen(
    tasks: List<AgentTask>,
    userProfile: UserProfile?,
    onOpenPricing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = remember(tasks, userProfile) {
        UsageAnalyticsHelper.buildSummary(tasks = tasks, userProfile = userProfile)
    }

    UsageDashboardContent(
        summary = summary,
        onOpenPricing = onOpenPricing,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UsageDashboardContent(
    summary: UsageDashboardSummary,
    onOpenPricing: () -> Unit,
    modifier: Modifier = Modifier
) {
    var timeframe by rememberSaveable { mutableStateOf(UsageTimeframe.DAILY) }
    val activeSeries = if (timeframe == UsageTimeframe.DAILY) {
        summary.dailySeries
    } else {
        summary.monthlySeries
    }
    var selectedIndex by rememberSaveable(timeframe) {
        mutableIntStateOf((activeSeries.size - 1).coerceAtLeast(0))
    }
    val safeSelectedIndex = selectedIndex.coerceIn(0, (activeSeries.size - 1).coerceAtLeast(0))
    val selectedPoint = activeSeries.getOrNull(safeSelectedIndex)
    val creditProgress = calculateCreditProgress(summary.remainingCredits, summary.creditQuota)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 780.dp)
                    .fillMaxSize()
                    .testTag("usage_dashboard_screen"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BarChart,
                                contentDescription = stringResource(R.string.usage_dashboard_icon_desc),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.usage_dashboard_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.usage_dashboard_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // KPI Cards
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        maxItemsInEachRow = 3
                    ) {
                        SummaryMetricCard(
                            icon = Icons.Filled.Bolt,
                            title = "الرصيد المتبقي",
                            value = "${summary.remainingCredits}",
                            subtitle = "من أصل ${summary.creditQuota} نقطة",
                            modifier = Modifier.weight(1f, fill = true).widthIn(min = 180.dp)
                        )
                        SummaryMetricCard(
                            icon = Icons.Filled.Insights,
                            title = "إجمالي الاستهلاك",
                            value = "${summary.totalCreditsUsed}",
                            subtitle = "عبر ${summary.totalTasksCount} مهمة مسجّلة",
                            modifier = Modifier.weight(1f, fill = true).widthIn(min = 180.dp)
                        )
                        SummaryMetricCard(
                            icon = Icons.Filled.CalendarToday,
                            title = "أعلى يوم استخداماً",
                            value = "${summary.peakDayCredits}",
                            subtitle = "بتاريخ ${summary.peakDayLabel}",
                            modifier = Modifier.weight(1f, fill = true).widthIn(min = 180.dp)
                        )
                    }
                }

                // Quota Progress Bar
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(18.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "استهلاك حصة الباقة الحالية",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${(creditProgress * 100).toInt()}% متبقٍ",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { creditProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                            OutlinedButton(
                                onClick = onOpenPricing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("usage_upgrade_button")
                            ) {
                                Text("عرض باقات الرصيد والترقية")
                            }
                        }
                    }
                }

                // Daily & Monthly Native Compose Chart Card
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(20.dp)
                            )
                            .testTag("usage_chart_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (timeframe == UsageTimeframe.DAILY) {
                                        "مخطط الاستهلاك اليومي"
                                    } else {
                                        "مخطط الاستهلاك الشهري"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = timeframe == UsageTimeframe.DAILY,
                                    onClick = { timeframe = UsageTimeframe.DAILY },
                                    label = { Text(UsageTimeframe.DAILY.arabicLabel) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("usage_timeframe_daily")
                                )
                                FilterChip(
                                    selected = timeframe == UsageTimeframe.MONTHLY,
                                    onClick = { timeframe = UsageTimeframe.MONTHLY },
                                    label = { Text(UsageTimeframe.MONTHLY.arabicLabel) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("usage_timeframe_monthly")
                                )
                            }

                            if (selectedPoint != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "الفترة: ${selectedPoint.label}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "${selectedPoint.creditsUsed} نقطة • ${selectedPoint.taskCount} مهام",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            InteractiveUsageBarTrendChart(
                                points = activeSeries,
                                selectedIndex = safeSelectedIndex,
                                onSelectPoint = { selectedIndex = it }
                            )

                            if (activeSeries.all { it.creditsUsed == 0L }) {
                                Text(
                                    text = "لم تُسجّل نقاط استهلاك في هذه الفترة بعد — سيظهر المنحنى تلقائياً عند تنفيذ المهام.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Category Breakdown Card
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(20.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "توزيع الاستهلاك حسب نوع الخدمة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (summary.categoryBreakdown.isEmpty()) {
                                Text(
                                    text = "لا توجد مهام مسجلة لعرض توزيع الخدمات حتى الآن.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                summary.categoryBreakdown.forEachIndexed { idx, slice ->
                                    if (idx > 0) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = slice.category.arabicLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${slice.creditsUsed} نقطة (${slice.taskCount} مهام)",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { slice.shareFraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(7.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = MaterialTheme.colorScheme.secondary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
            RoundedCornerShape(16.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InteractiveUsageBarTrendChart(
    points: List<UsageChartPoint>,
    selectedIndex: Int,
    onSelectPoint: (Int) -> Unit
) {
    if (points.isEmpty()) return

    val maxCredits = points.maxOfOrNull { it.creditsUsed }?.coerceAtLeast(10L) ?: 10L
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val trendColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 450),
        label = "usage_chart_anim"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp, bottom = 6.dp)
            ) {
                val count = points.size
                val slotWidth = size.width / count.toFloat()
                val barWidth = (slotWidth * 0.44f).coerceIn(10f, 52f)
                val chartHeight = size.height

                // Grid lines
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                for (step in 0..3) {
                    val y = chartHeight * (step / 3f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5f,
                        pathEffect = dashEffect
                    )
                }

                val trendPath = Path()
                val pointCenters = mutableListOf<Offset>()

                points.forEachIndexed { idx, point ->
                    val ratio = (point.creditsUsed.toFloat() / maxCredits.toFloat()).coerceIn(0f, 1f)
                    val barHeight = (chartHeight * ratio * animatedProgress).coerceAtLeast(
                        if (point.creditsUsed > 0L) 8f else 3f
                    )
                    val centerX = slotWidth * idx + slotWidth / 2f
                    val topY = chartHeight - barHeight
                    val isSelected = idx == selectedIndex

                    drawRoundRect(
                        color = if (isSelected) secondaryColor else primaryColor.copy(alpha = 0.72f),
                        topLeft = Offset(centerX - barWidth / 2f, topY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    val pt = Offset(centerX, topY)
                    pointCenters.add(pt)
                    if (idx == 0) {
                        trendPath.moveTo(pt.x, pt.y)
                    } else {
                        trendPath.lineTo(pt.x, pt.y)
                    }
                }

                drawPath(
                    path = trendPath,
                    color = trendColor,
                    style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                pointCenters.forEachIndexed { idx, pt ->
                    val isSelected = idx == selectedIndex
                    drawCircle(
                        color = if (isSelected) secondaryColor else trendColor,
                        radius = if (isSelected) 8f else 5f,
                        center = pt
                    )
                    drawCircle(
                        color = surfaceColor,
                        radius = if (isSelected) 4f else 2.5f,
                        center = pt
                    )
                }
            }

            // Accessible clickable columns overlay matching bar slots
            Row(modifier = Modifier.fillMaxSize()) {
                points.forEachIndexed { idx, point ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelectPoint(idx) }
                            .testTag("usage_bar_$idx")
                    )
                }
            }
        }

        // X-axis clickable labels (>= 48.dp touch target)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { idx, point ->
                val isSelected = idx == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectPoint(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = point.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1
                    )
                }
            }
        }
    }
}
