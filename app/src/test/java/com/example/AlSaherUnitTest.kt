package com.example

import android.speech.SpeechRecognizer
import com.example.model.AccountPreferences
import com.example.model.AgentMode
import com.example.model.AgentTask
import com.example.model.AudioTranscribeUiState
import com.example.model.ChatbotRole
import com.example.model.GeminiChatTier
import com.example.model.ImageAspectOption
import com.example.model.LiveTranscriptLine
import com.example.model.LiveVoiceUiState
import com.example.model.TaskMessage
import com.example.model.TaskStatus
import com.example.model.UserProfile
import com.example.model.WorkspaceDestination
import com.example.model.parseEnumSafe
import com.example.ui.components.ChatHistoryFilter
import com.example.ui.components.FREE_SIGNUP_CREDITS_QUOTA
import com.example.ui.components.calculateCreditProgress
import com.example.ui.components.calculateCreditQuota
import com.example.ui.components.filterChatHistory
import com.example.util.ChatExportFormat
import com.example.util.ChatExportHelper
import com.example.util.ChatExportScope
import com.example.util.ExportableConversation
import com.example.util.UsageAnalyticsHelper
import com.example.util.mapSpeechErrorToArabic
import com.example.util.mergeVoiceTranscript
import com.example.viewmodel.AgentChatMessage
import com.example.viewmodel.ChatAgentModelOption
import com.example.viewmodel.ChatScreenUiState
import com.example.viewmodel.ChatTurnUiState
import com.example.viewmodel.OFFICIAL_CONNECTORS
import com.example.viewmodel.buildGeminiChatHistory
import com.google.firebase.Timestamp
import java.util.Date
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlSaherUnitTest {
    @Test
    fun officialConnectorsContainsAllTenRequiredServices() {
        val keys = OFFICIAL_CONNECTORS.map { it.key }.toSet()
        val expected = setOf(
            "gmail", "drive", "calendar", "outlook", "slack",
            "notion", "github", "shopify", "canva", "hubspot"
        )
        assertEquals(expected, keys)
    }

    @Test
    fun userProfileDefaultsIncludeFreeTierAnd1000SignupCredits() {
        val profile = UserProfile()
        assertEquals("FREE", profile.plan)
        assertEquals(1000L, profile.creditsBalance)
    }

    @Test
    fun parseEnumSafeHandlesUnknownValuesGracefully() {
        assertEquals(TaskStatus.COMPLETED, parseEnumSafe("UNKNOWN_STATE", TaskStatus.COMPLETED))
        assertEquals(AgentMode.AGENT, parseEnumSafe("agent", AgentMode.CHAT))
        assertTrue(OFFICIAL_CONNECTORS.size == 10)
    }

    @Test
    fun creditCounterCalculationsRespectFree1000QuotaAndBounds() {
        assertEquals(1000L, FREE_SIGNUP_CREDITS_QUOTA)
        assertEquals(1000L, calculateCreditQuota("FREE", 750L))
        assertEquals(0.75f, calculateCreditProgress(750L, 1000L), 0.001f)
        assertEquals(0f, calculateCreditProgress(-25L, 1000L), 0.001f)
        assertEquals(1f, calculateCreditProgress(1200L, 1000L), 0.001f)
        assertEquals(5000L, calculateCreditQuota("PRO", 4500L))
    }

    @Test
    fun chatHistoryFilterSupportsModeAndSearchQuery() {
        val items = listOf(
            AgentTask(id = "1", title = "حملة تسويق", mode = "AGENT", prompt = "إطلاق منتج"),
            AgentTask(id = "2", title = "سؤال سريع عن البرمجة", mode = "CHAT", prompt = "كيف يعمل Kotlin"),
            AgentTask(id = "3", title = "ملخص تقرير", mode = "CHAT", prompt = "تلخيص المبيعات")
        )
        assertEquals(3, filterChatHistory(items, "", ChatHistoryFilter.ALL).size)
        assertEquals(1, filterChatHistory(items, "", ChatHistoryFilter.AGENT).size)
        assertEquals(2, filterChatHistory(items, "", ChatHistoryFilter.CHAT).size)
        val matched = filterChatHistory(items, "kotlin", ChatHistoryFilter.CHAT)
        assertEquals(listOf("2"), matched.map { it.id })
    }

    @Test
    fun voiceTranscriptMergingAndArabicErrorMappingWorkCorrectly() {
        assertEquals("أنشئ عرضاً تقديمياً", mergeVoiceTranscript("", " أنشئ عرضاً تقديمياً "))
        assertEquals("اكتب خطة عمل متكاملة", mergeVoiceTranscript("اكتب خطة", "عمل متكاملة"))
        assertEquals("اكتب خطة عمل", mergeVoiceTranscript("اكتب خطة ", "عمل"))
        assertEquals("نفس النص", mergeVoiceTranscript("نفس النص", "   "))
        assertTrue(mapSpeechErrorToArabic(SpeechRecognizer.ERROR_NO_MATCH).isNotBlank())
        assertTrue(mapSpeechErrorToArabic(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS).contains("الميكروفون"))
    }

    @Test
    fun geminiChatTiersImageStudioAndAspectOptionsMatchSupportedModels() {
        assertEquals("gemini-3.1-flash-lite", GeminiChatTier.FAST.modelName)
        assertEquals("gemini-4-hydrogen", GeminiChatTier.GENERAL.modelName)
        assertEquals("gemini-4-neon", GeminiChatTier.INTERACTIVE.modelName)
        assertEquals("gemini-4-carbon", GeminiChatTier.DEEP.modelName)
        assertTrue(ChatbotRole.entries.size == 4)
        assertTrue(ChatbotRole.entries.all { it.systemRoleInstruction.isNotBlank() })
        assertTrue(WorkspaceDestination.entries.contains(WorkspaceDestination.IMAGE_STUDIO))
        assertEquals(3, ImageAspectOption.entries.size)
    }

    @Test
    fun chatExportFormatsValidJsonAndReadableArabicText() {
        val conversation = ExportableConversation(
            task = AgentTask(
                id = "t1",
                title = "خطة إطلاق \"المنتج\"",
                mode = "CHAT",
                status = "COMPLETED",
                prompt = "اقترح خطة تسويق"
            ),
            messages = listOf(
                TaskMessage(id = "m1", role = "USER", content = "اقترح خطة تسويق", mode = "CHAT"),
                TaskMessage(id = "m2", role = "ASSISTANT", content = "إليك خطة من 3 مراحل:\nالمرحلة الأولى", mode = "CHAT")
            )
        )

        val jsonOutput = ChatExportHelper.formatConversationsAsJson(
            conversations = listOf(conversation),
            exportedAtIso = "2026-11-23T13:50:00Z"
        )
        assertTrue(jsonOutput.contains("\"conversationCount\": 1"))
        assertTrue(jsonOutput.contains("\"messageCount\": 2"))
        assertTrue(jsonOutput.contains("\"title\": \"خطة إطلاق \\\"المنتج\\\"\""))
        assertTrue(jsonOutput.contains("\"content\": \"إليك خطة من 3 مراحل:\\nالمرحلة الأولى\""))

        val textOutput = ChatExportHelper.formatConversationsAsText(
            conversations = listOf(conversation),
            exportedAtIso = "2026-11-23T13:50:00Z"
        )
        assertTrue(textOutput.contains("سجل محادثات تطبيق الساحر"))
        assertTrue(textOutput.contains("خطة إطلاق \"المنتج\""))
        assertTrue(textOutput.contains("أنت (غير محدد):"))
        assertTrue(textOutput.contains("الساحر (غير محدد):"))
        assertTrue(textOutput.contains("إليك خطة من 3 مراحل:\nالمرحلة الأولى"))

        val jsonName = ChatExportHelper.buildSuggestedFileName(ChatExportFormat.JSON, ChatExportScope.ALL)
        val txtName = ChatExportHelper.buildSuggestedFileName(ChatExportFormat.TEXT, ChatExportScope.CURRENT)
        assertTrue(jsonName.endsWith(".json") && jsonName.startsWith("alsaher_history_"))
        assertTrue(txtName.endsWith(".txt") && txtName.startsWith("alsaher_chat_"))
    }

    @Test
    fun liveVoiceAndAudioTranscribeUiStatesRepresentExpectedPayloads() {
        val transcript = LiveTranscriptLine(id = "l1", isUser = true, text = "مرحباً بالساحر")
        val liveActive: LiveVoiceUiState = LiveVoiceUiState.Active(
            statusText = "محادثة صوتية نشطة (gemini-3.8-live)",
            transcripts = listOf(transcript)
        )
        assertTrue(liveActive is LiveVoiceUiState.Active)
        assertEquals(1, (liveActive as LiveVoiceUiState.Active).transcripts.size)
        assertEquals("مرحباً بالساحر", liveActive.transcripts.first().text)

        val transcribeSuccess: AudioTranscribeUiState = AudioTranscribeUiState.Success("تم التفريغ الصوتي بدقة")
        assertTrue(transcribeSuccess is AudioTranscribeUiState.Success)
        assertEquals("تم التفريغ الصوتي بدقة", (transcribeSuccess as AudioTranscribeUiState.Success).transcript)
    }

    @Test
    fun chatScreenModelsAndHistoryBuilderProduceValidGeminiTurns() {
        assertEquals("gemini-3.8-flash", ChatAgentModelOption.FLASH.modelName)
        assertEquals("gemini-3.5-flash-lite", ChatAgentModelOption.FLASH_LITE.modelName)
        assertEquals("gemini-4-argon", ChatAgentModelOption.ARGON.modelName)
        assertTrue(WorkspaceDestination.entries.contains(WorkspaceDestination.DIRECT_AI_CHAT))

        val messages = listOf(
            AgentChatMessage(id = "0", isUser = false, text = "تجاهل رسالة نموذج أولية"),
            AgentChatMessage(id = "1", isUser = true, text = "مرحباً"),
            AgentChatMessage(id = "2", isUser = false, text = "أهلاً بك، كيف أساعدك؟"),
            AgentChatMessage(id = "3", isUser = true, text = "سؤال أخير معلق")
        )
        val history = buildGeminiChatHistory(messages)
        assertEquals(2, history.size)
        assertEquals("user", history[0].role)
        assertEquals("model", history[1].role)

        val state = ChatScreenUiState(
            messages = messages,
            turnState = ChatTurnUiState.Streaming("جاري الكتابة")
        )
        assertTrue(state.turnState is ChatTurnUiState.Streaming)
    }

    @Test
    fun accountPreferencesDefaultsAndSettingsDestinationAreConfigured() {
        val prefs = AccountPreferences()
        assertTrue(prefs.isDarkTheme)
        assertTrue(prefs.notifyOnComplete)
        assertEquals(AgentMode.AGENT, prefs.defaultAgentMode)
        assertEquals(GeminiChatTier.GENERAL, prefs.defaultChatTier)
        assertEquals(ChatbotRole.GENERAL_ASSISTANT, prefs.defaultChatbotRole)
        assertTrue(prefs.defaultSearchGrounding)
        assertTrue(WorkspaceDestination.entries.contains(WorkspaceDestination.SETTINGS))
        assertTrue(WorkspaceDestination.entries.contains(WorkspaceDestination.USAGE_DASHBOARD))
    }

    @Test
    fun usageAnalyticsHelperAggregatesDailyMonthlyAndCategoryUsage() {
        val utc = TimeZone.getTimeZone("UTC")
        val nowMillis = 1_795_520_000_000L // fixed UTC reference timestamp
        val twoDaysAgoMillis = nowMillis - (2L * 24L * 60L * 60L * 1000L)

        val tasks = listOf(
            AgentTask(
                id = "t1",
                category = "GENERAL",
                creditsUsed = 20L,
                createdAt = Timestamp(Date(nowMillis))
            ),
            AgentTask(
                id = "t2",
                category = "VIDEO",
                creditsUsed = 15L,
                createdAt = Timestamp(Date(nowMillis))
            ),
            AgentTask(
                id = "t3",
                category = "RESEARCH",
                creditsUsed = 15L,
                createdAt = Timestamp(Date(twoDaysAgoMillis))
            )
        )
        val profile = UserProfile(plan = "FREE", creditsBalance = 950L)

        val summary = UsageAnalyticsHelper.buildSummary(
            tasks = tasks,
            userProfile = profile,
            nowMillis = nowMillis,
            timeZone = utc
        )

        assertEquals(50L, summary.totalCreditsUsed)
        assertEquals(950L, summary.remainingCredits)
        assertEquals(1000L, summary.creditQuota)
        assertEquals(3, summary.totalTasksCount)
        assertEquals(7, summary.dailySeries.size)
        assertEquals(6, summary.monthlySeries.size)
        assertEquals(35L, summary.dailySeries.last().creditsUsed)
        assertEquals(15L, summary.dailySeries[4].creditsUsed)
        assertEquals(35L, summary.peakDayCredits)
        assertEquals(50L, summary.monthlySeries.last().creditsUsed)
        assertEquals(3, summary.categoryBreakdown.size)
    }
}
