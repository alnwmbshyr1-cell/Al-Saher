package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class WorkspaceDestination(val arabicTitle: String) {
    CHAT_AGENT("الوكيل والدردشة"),
    DIRECT_AI_CHAT("محادثة Firebase AI"),
    IMAGE_STUDIO("استوديو الصور"),
    VIDEO_STUDIO("استوديو الفيديو"),
    SLIDES_STUDIO("العروض التقديمية"),
    DOCS_RESEARCH("المستندات والبحث الموسع"),
    WEBSITE_BUILDER("منشئ المواقع"),
    APP_BUILDER("منشئ التطبيقات"),
    GAME_MAKER("صانع الألعاب"),
    CONNECTORS("الموصلات"),
    REMOTE_CONTROL("التحكم عن بُعد"),
    AUTOMATIONS("الأتمتة"),
    USAGE_DASHBOARD("لوحة الاستهلاك"),
    SETTINGS("الإعدادات والحساب")
}

enum class GeminiChatTier(val modelName: String, val arabicLabel: String) {
    FAST("gemini-3.1-flash-lite", "سريع"),
    GENERAL("gemini-4-hydrogen", "عام"),
    INTERACTIVE("gemini-4-neon", "تفاعلي معقد"),
    DEEP("gemini-4-carbon", "استدلال عميق")
}

enum class ChatbotRole(val arabicLabel: String, val systemRoleInstruction: String) {
    GENERAL_ASSISTANT(
        arabicLabel = "مساعد عام",
        systemRoleInstruction = "أنت (الساحر)، مساعد ذكاء اصطناعي عام متكامل باللغة العربية الفصحى الواضحة والاحترافية. تجيب بدقة وتنظّم الحلول والأفكار بوضوح."
    ),
    CODING_ARCHITECT(
        arabicLabel = "مهندس برمجيات",
        systemRoleInstruction = "أنت مهندس برمجيات ومعماري أنظمة خبير ضمن منصة (الساحر). قدّم حلولاً برمجية عملية، نظيفة، وآمنة مع شرح تقني دقيق باللغة العربية."
    ),
    CREATIVE_WRITER(
        arabicLabel = "كاتب ومسوّق",
        systemRoleInstruction = "أنت كاتب محتوى إبداعي وخبير تسويق استراتيجي في منصة (الساحر). صغ نصوصاً جذابة ومقنعة ومرتبة باللغة العربية."
    ),
    RESEARCH_ANALYST(
        arabicLabel = "محلل وباحث",
        systemRoleInstruction = "أنت محلل بيانات وباحث استراتيجي في منصة (الساحر). اعتمد على الحقائق والأدلة والمقارنات المنظمة ووضّح المصادر عند توفرها."
    )
}

enum class AgentMode(val arabicLabel: String) {
    AGENT("الوكيل العام"),
    CHAT("دردشة مجانية")
}

enum class TaskCategory(val arabicLabel: String) {
    GENERAL("عام"),
    VIDEO("فيديو"),
    SLIDES("شرائح"),
    DOCS("مستندات"),
    WEB("موقع ويب"),
    APP("تطبيق"),
    GAME("لعبة"),
    RESEARCH("بحث موسع"),
    REMOTE("حاسوب سحابي")
}

enum class TaskStatus(val arabicLabel: String) {
    PLANNING("يخطط"),
    RUNNING("قيد التنفيذ"),
    PAUSED("متوقف مؤقتاً"),
    COMPLETED("مكتمل"),
    STOPPED("تم الإيقاف"),
    ERROR("خطأ")
}

enum class StudioType(val arabicLabel: String) {
    VIDEO("فيديو ذكي"),
    SLIDES("عروض تقديمية"),
    DOCS("مستندات وبحث"),
    WEB("موقع ويب"),
    APP("تطبيق جوال"),
    GAME("لعبة تفاعلية")
}

inline fun <reified T : Enum<T>> parseEnumSafe(raw: String?, default: T): T {
    if (raw.isNullOrBlank()) return default
    return enumValues<T>().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val plan: String = "FREE",
    val creditsBalance: Long = 1000L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AccountPreferences(
    val isDarkTheme: Boolean = true,
    val notifyOnComplete: Boolean = true,
    val defaultAgentMode: AgentMode = AgentMode.AGENT,
    val defaultChatTier: GeminiChatTier = GeminiChatTier.GENERAL,
    val defaultChatbotRole: ChatbotRole = ChatbotRole.GENERAL_ASSISTANT,
    val defaultSearchGrounding: Boolean = true
)

@Keep
@IgnoreExtraProperties
data class AgentTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val mode: String = "AGENT",
    val category: String = "GENERAL",
    val status: String = "PLANNING",
    val prompt: String = "",
    val summary: String = "",
    val stepTitles: List<String> = emptyList(),
    val currentStepIndex: Long = 0L,
    val deliverableTitle: String = "",
    val deliverableContent: String = "",
    val creditsUsed: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TaskMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "USER",
    val content: String = "",
    val mode: String = "AGENT",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudioProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val studioType: String = "VIDEO",
    val subType: String = "",
    val prompt: String = "",
    val content: String = "",
    val tracksSummary: String = "",
    val status: String = "READY",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AutomationRule(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val triggerType: String = "SCHEDULE",
    val triggerDetail: String = "",
    val actionPrompt: String = "",
    val active: Boolean = true,
    val runsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ConnectorState(
    @DocumentId val id: String = "",
    val userId: String = "",
    val connectorKey: String = "",
    val displayName: String = "",
    val connected: Boolean = false,
    val accountLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ConnectorCatalogItem(
    val key: String,
    val name: String,
    val arabicCategory: String,
    val description: String
)

data class VideoTimelineClip(
    val id: String,
    val shotTitle: String,
    val visualPrompt: String,
    val durationSeconds: Int,
    val captionText: String
)

data class VideoTimelineModel(
    val title: String,
    val videoType: String,
    val voiceoverScript: String,
    val musicTrack: String,
    val sfxNotes: String,
    val clips: List<VideoTimelineClip>
)

data class SlideCardModel(
    val number: Int,
    val title: String,
    val bullets: List<String>,
    val speakerNotes: String
)

data class WideResearchAgentOutput(
    val agentName: String,
    val focusArea: String,
    val findings: String
)

data class RemoteLogEntry(
    val timestamp: String,
    val command: String,
    val output: String,
    val status: String
)

enum class ImageAspectOption(val arabicLabel: String) {
    SQUARE_1X1("مربع 1:1"),
    LANDSCAPE_16X9("أفقي 16:9"),
    PORTRAIT_9X16("عمودي 9:16")
}

sealed interface ImageGenUiState {
    data object Idle : ImageGenUiState
    data object Loading : ImageGenUiState
    data class Success(val bitmap: android.graphics.Bitmap, val prompt: String) : ImageGenUiState
    data class Error(val message: String) : ImageGenUiState
}

data class LiveTranscriptLine(
    val id: String,
    val isUser: Boolean,
    val text: String
)

sealed interface LiveVoiceUiState {
    data object Idle : LiveVoiceUiState
    data object Connecting : LiveVoiceUiState
    data class Active(
        val statusText: String,
        val transcripts: List<LiveTranscriptLine>
    ) : LiveVoiceUiState
    data class Error(val message: String) : LiveVoiceUiState
}

sealed interface AudioTranscribeUiState {
    data object Idle : AudioTranscribeUiState
    data object Transcribing : AudioTranscribeUiState
    data class Success(val transcript: String) : AudioTranscribeUiState
    data class Error(val message: String) : AudioTranscribeUiState
}

