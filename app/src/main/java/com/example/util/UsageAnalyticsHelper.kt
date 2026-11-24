package com.example.util

import com.example.model.AgentTask
import com.example.model.TaskCategory
import com.example.model.UserProfile
import com.example.model.parseEnumSafe
import com.example.ui.components.FREE_SIGNUP_CREDITS_QUOTA
import com.example.ui.components.calculateCreditQuota
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class UsageTimeframe(val arabicLabel: String) {
    DAILY("يومي (7 أيام)"),
    MONTHLY("شهري (6 أشهر)")
}

data class UsageChartPoint(
    val key: String,
    val label: String,
    val creditsUsed: Long,
    val taskCount: Int
)

data class CategoryUsageSlice(
    val category: TaskCategory,
    val creditsUsed: Long,
    val taskCount: Int,
    val shareFraction: Float
)

data class UsageDashboardSummary(
    val totalCreditsUsed: Long,
    val remainingCredits: Long,
    val creditQuota: Long,
    val totalTasksCount: Int,
    val peakDayLabel: String,
    val peakDayCredits: Long,
    val dailySeries: List<UsageChartPoint>,
    val monthlySeries: List<UsageChartPoint>,
    val categoryBreakdown: List<CategoryUsageSlice>
)

object UsageAnalyticsHelper {

    fun buildSummary(
        tasks: List<AgentTask>,
        userProfile: UserProfile?,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): UsageDashboardSummary {
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            this.timeZone = timeZone
        }
        val dayLabelFormat = SimpleDateFormat("d/M", Locale.US).apply {
            this.timeZone = timeZone
        }
        val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US).apply {
            this.timeZone = timeZone
        }
        val monthLabelFormat = SimpleDateFormat("M/yy", Locale.US).apply {
            this.timeZone = timeZone
        }

        val dailyBuckets = linkedMapOf<String, Pair<String, MutableList<Long>>>()
        val dayCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -6)
        }
        repeat(7) {
            val d = dayCal.time
            dailyBuckets[dayKeyFormat.format(d)] = dayLabelFormat.format(d) to mutableListOf()
            dayCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val monthlyBuckets = linkedMapOf<String, Pair<String, MutableList<Long>>>()
        val monthCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -5)
        }
        repeat(6) {
            val d = monthCal.time
            monthlyBuckets[monthKeyFormat.format(d)] = monthLabelFormat.format(d) to mutableListOf()
            monthCal.add(Calendar.MONTH, 1)
        }

        val categoryCredits = linkedMapOf<TaskCategory, Pair<Long, Int>>()
        var totalUsed = 0L

        tasks.forEach { task ->
            val used = task.creditsUsed.coerceAtLeast(0L)
            totalUsed += used

            val timestampMillis = task.createdAt?.toDate()?.time
                ?: task.updatedAt?.toDate()?.time
                ?: nowMillis
            val date = Date(timestampMillis)

            val dKey = dayKeyFormat.format(date)
            dailyBuckets[dKey]?.second?.add(used)

            val mKey = monthKeyFormat.format(date)
            monthlyBuckets[mKey]?.second?.add(used)

            val cat = parseEnumSafe(task.category, TaskCategory.GENERAL)
            val prev = categoryCredits[cat] ?: (0L to 0)
            categoryCredits[cat] = (prev.first + used) to (prev.second + 1)
        }

        val dailySeries = dailyBuckets.map { (key, entry) ->
            UsageChartPoint(
                key = key,
                label = entry.first,
                creditsUsed = entry.second.sum(),
                taskCount = entry.second.size
            )
        }

        val monthlySeries = monthlyBuckets.map { (key, entry) ->
            UsageChartPoint(
                key = key,
                label = entry.first,
                creditsUsed = entry.second.sum(),
                taskCount = entry.second.size
            )
        }

        val peakPoint = dailySeries.maxByOrNull { it.creditsUsed }
        val denominator = totalUsed.coerceAtLeast(1L).toFloat()
        val breakdown = categoryCredits.entries
            .map { (category, pair) ->
                val fraction = if (totalUsed > 0L) {
                    (pair.first.toFloat() / denominator).coerceIn(0f, 1f)
                } else {
                    (pair.second.toFloat() / tasks.size.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                }
                CategoryUsageSlice(
                    category = category,
                    creditsUsed = pair.first,
                    taskCount = pair.second,
                    shareFraction = fraction
                )
            }
            .sortedWith(compareByDescending<CategoryUsageSlice> { it.creditsUsed }.thenByDescending { it.taskCount })

        val balance = userProfile?.creditsBalance ?: FREE_SIGNUP_CREDITS_QUOTA
        val quota = calculateCreditQuota(userProfile?.plan, balance)

        return UsageDashboardSummary(
            totalCreditsUsed = totalUsed,
            remainingCredits = balance,
            creditQuota = quota,
            totalTasksCount = tasks.size,
            peakDayLabel = peakPoint?.label ?: "-",
            peakDayCredits = peakPoint?.creditsUsed ?: 0L,
            dailySeries = dailySeries,
            monthlySeries = monthlySeries,
            categoryBreakdown = breakdown
        )
    }
}
