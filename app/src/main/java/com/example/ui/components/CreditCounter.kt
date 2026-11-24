package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

const val FREE_SIGNUP_CREDITS_QUOTA = 1000L

fun calculateCreditQuota(plan: String?, creditsBalance: Long): Long {
    val baseQuota = when (plan?.uppercase()) {
        "PRO" -> 5000L
        "TEAM" -> 15000L
        else -> FREE_SIGNUP_CREDITS_QUOTA
    }
    return maxOf(baseQuota, creditsBalance.coerceAtLeast(0L))
}

fun calculateCreditProgress(creditsBalance: Long, totalQuota: Long): Float {
    if (totalQuota <= 0L) return 0f
    return (creditsBalance.coerceAtLeast(0L).toFloat() / totalQuota.toFloat()).coerceIn(0f, 1f)
}

@Composable
fun CreditCounter(
    creditsBalance: Long,
    plan: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeBalance = creditsBalance.coerceAtLeast(0L)
    val totalQuota = calculateCreditQuota(plan, safeBalance)
    val targetProgress = calculateCreditProgress(safeBalance, totalQuota)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(),
        label = "credit_counter_progress"
    )

    val isFreePlan = plan.isNullOrBlank() || plan.equals("FREE", ignoreCase = true)
    val planBadgeText = when (plan?.uppercase()) {
        "PRO" -> "Pro"
        "TEAM" -> "Team"
        else -> "مجاني"
    }

    val indicatorColor = when {
        safeBalance <= 150L -> MaterialTheme.colorScheme.error
        safeBalance <= 350L -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                )
                .testTag("credit_counter_card")
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = "رصيد الائتمان",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "رصيد الائتمان",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$safeBalance / $totalQuota ائتمان",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("credit_counter_value")
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = if (isFreePlan) "$planBadgeText • ترقية" else planBadgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = indicatorColor,
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isFreePlan) {
                            "متبقي $safeBalance من أصل $FREE_SIGNUP_CREDITS_QUOTA ائتمان مجاني"
                        } else {
                            "متاح للاستخدام في الوكيل والاستوديوهات"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
