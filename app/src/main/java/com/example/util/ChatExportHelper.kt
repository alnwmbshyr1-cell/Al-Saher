package com.example.util

import android.content.Context
import android.net.Uri
import com.example.model.AgentMode
import com.example.model.AgentTask
import com.example.model.TaskMessage
import com.example.model.TaskStatus
import com.example.model.parseEnumSafe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class ChatExportFormat(
    val extension: String,
    val mimeType: String,
    val arabicLabel: String,
    val arabicDescription: String
) {
    JSON(
        extension = "json",
        mimeType = "application/json",
        arabicLabel = "ملف JSON (.json)",
        arabicDescription = "بيانات منظمة كاملة مناسبة للنسخ الاحتياطي والتحليل"
    ),
    TEXT(
        extension = "txt",
        mimeType = "text/plain",
        arabicLabel = "ملف نصي (.txt)",
        arabicDescription = "نص منسّق واضح لسهولة القراءة والمشاركة"
    )
}

enum class ChatExportScope(val arabicLabel: String) {
    CURRENT("المحادثة الحالية فقط"),
    ALL("جميع المحادثات في السجل")
}

data class ExportableConversation(
    val task: AgentTask,
    val messages: List<TaskMessage>
)

object ChatExportHelper {

    fun formatIsoTimestamp(date: Date?): String {
        if (date == null) return ""
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return fmt.format(date)
    }

    fun formatReadableTimestamp(date: Date?): String {
        if (date == null) return "غير محدد"
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return fmt.format(date)
    }

    fun buildSuggestedFileName(
        format: ChatExportFormat,
        scope: ChatExportScope,
        now: Date = Date()
    ): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(now)
        val prefix = if (scope == ChatExportScope.CURRENT) "alsaher_chat" else "alsaher_history"
        return "${prefix}_$stamp.${format.extension}"
    }

    private fun jsonString(value: String): String = buildString(value.length + 2) {
        append('"')
        for (ch in value) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch.code < 0x20) {
                    append("\\u%04x".format(ch.code))
                } else {
                    append(ch)
                }
            }
        }
        append('"')
    }

    fun formatConversationsAsJson(
        conversations: List<ExportableConversation>,
        exportedAtIso: String = formatIsoTimestamp(Date())
    ): String = buildString {
        appendLine("{")
        appendLine("  \"app\": ${jsonString("Al Saher - الساحر")},")
        appendLine("  \"exportedAt\": ${jsonString(exportedAtIso)},")
        appendLine("  \"conversationCount\": ${conversations.size},")
        append("  \"conversations\": [")
        if (conversations.isEmpty()) {
            appendLine("]")
        } else {
            appendLine()
            conversations.forEachIndexed { idx, item ->
                val task = item.task
                appendLine("    {")
                appendLine("      \"id\": ${jsonString(task.id)},")
                appendLine("      \"title\": ${jsonString(task.title.ifBlank { "محادثة جديدة" })},")
                appendLine("      \"mode\": ${jsonString(task.mode)},")
                appendLine("      \"category\": ${jsonString(task.category)},")
                appendLine("      \"status\": ${jsonString(task.status)},")
                appendLine("      \"prompt\": ${jsonString(task.prompt)},")
                appendLine("      \"summary\": ${jsonString(task.summary)},")
                appendLine("      \"createdAt\": ${jsonString(formatIsoTimestamp(task.createdAt?.toDate()))},")
                appendLine("      \"messageCount\": ${item.messages.size},")
                append("      \"messages\": [")
                if (item.messages.isEmpty()) {
                    appendLine("]")
                } else {
                    appendLine()
                    item.messages.forEachIndexed { mIdx, msg ->
                        appendLine("        {")
                        appendLine("          \"id\": ${jsonString(msg.id)},")
                        appendLine("          \"role\": ${jsonString(msg.role)},")
                        appendLine("          \"mode\": ${jsonString(msg.mode)},")
                        appendLine("          \"content\": ${jsonString(msg.content)},")
                        appendLine("          \"createdAt\": ${jsonString(formatIsoTimestamp(msg.createdAt?.toDate()))}")
                        append("        }")
                        if (mIdx < item.messages.lastIndex) append(",")
                        appendLine()
                    }
                    appendLine("      ]")
                }
                append("    }")
                if (idx < conversations.lastIndex) append(",")
                appendLine()
            }
            appendLine("  ]")
        }
        append("}")
    }

    fun formatConversationsAsText(
        conversations: List<ExportableConversation>,
        exportedAtIso: String = formatIsoTimestamp(Date())
    ): String = buildString {
        appendLine("==================================================")
        appendLine("سجل محادثات تطبيق الساحر (Al Saher)")
        appendLine("تاريخ التصدير: $exportedAtIso")
        appendLine("عدد المحادثات: ${conversations.size}")
        appendLine("==================================================")
        appendLine()

        conversations.forEachIndexed { index, item ->
            val task = item.task
            val modeLabel = parseEnumSafe(task.mode, AgentMode.AGENT).arabicLabel
            val statusLabel = parseEnumSafe(task.status, TaskStatus.COMPLETED).arabicLabel
            val dateLabel = formatReadableTimestamp(task.createdAt?.toDate())

            appendLine("[${index + 1}] ${task.title.ifBlank { "محادثة جديدة" }}")
            appendLine("الوضع: $modeLabel | الحالة: $statusLabel | التاريخ: $dateLabel")
            appendLine("--------------------------------------------------")

            if (item.messages.isEmpty()) {
                if (task.prompt.isNotBlank()) {
                    appendLine("أنت:")
                    appendLine(task.prompt)
                    appendLine()
                }
                if (task.summary.isNotBlank()) {
                    appendLine("الساحر:")
                    appendLine(task.summary)
                    appendLine()
                }
            } else {
                item.messages.forEach { msg ->
                    val speaker = if (msg.role.equals("USER", ignoreCase = true)) "أنت" else "الساحر"
                    val msgTime = formatReadableTimestamp(msg.createdAt?.toDate())
                    appendLine("$speaker ($msgTime):")
                    appendLine(msg.content)
                    appendLine()
                }
            }

            appendLine("==================================================")
            appendLine()
        }
    }.trimEnd() + "\n"

    fun writeTextToUri(context: Context, uri: Uri, content: String): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            } ?: error("تعذر فتح مسار الحفظ المحدد")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
