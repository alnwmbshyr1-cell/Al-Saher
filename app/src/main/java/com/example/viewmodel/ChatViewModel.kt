package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "ChatViewModel"
private const val CHAT_TIMEOUT_MS = 60_000L

enum class ChatAgentModelOption(val modelName: String, val arabicLabel: String) {
    FLASH("gemini-3.8-flash", "Gemini 3.8 Flash"),
    FLASH_LITE("gemini-3.5-flash-lite", "سريع (3.5 Lite)"),
    ARGON("gemini-4-argon", "عميق (4 Argon)")
}

data class AgentChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val attachedImage: Bitmap? = null,
    val groundingSources: List<String> = emptyList(),
    val timestampMillis: Long = System.currentTimeMillis()
)

sealed interface ChatTurnUiState {
    data object Idle : ChatTurnUiState
    data class Streaming(val partialText: String) : ChatTurnUiState
    data class Error(val message: String) : ChatTurnUiState
}

data class ChatScreenUiState(
    val messages: List<AgentChatMessage> = emptyList(),
    val turnState: ChatTurnUiState = ChatTurnUiState.Idle,
    val selectedModel: ChatAgentModelOption = ChatAgentModelOption.FLASH,
    val googleSearchEnabled: Boolean = false,
    val pendingAttachment: Bitmap? = null
)

class ChatViewModel : ViewModel() {

    private val agentSystemInstruction: Content by lazy {
        content {
            text(
                "أنت وكيل الساحر الذكي. قدّم إجابات دقيقة ومباشرة ومنظمة باللغة العربية الواضحة، " +
                    "وساعد المستخدم في التحليل، البرمجة، الكتابة، والتخطيط العملي."
            )
        }
    }

    // Lazy default model instance per Firebase AI Logic best practices
    private val defaultModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = ChatAgentModelOption.FLASH.modelName,
            systemInstruction = agentSystemInstruction
        )
    }

    private val configuredModels = mutableMapOf<Pair<ChatAgentModelOption, Boolean>, GenerativeModel>()

    private val _uiState = MutableStateFlow(ChatScreenUiState())
    val uiState: StateFlow<ChatScreenUiState> = _uiState.asStateFlow()

    private var activeStreamJob: Job? = null
    private var lastAttemptPrompt: String? = null
    private var lastAttemptImage: Bitmap? = null

    private fun resolveModel(
        option: ChatAgentModelOption,
        enableGoogleSearch: Boolean
    ): GenerativeModel {
        if (option == ChatAgentModelOption.FLASH && !enableGoogleSearch) {
            return defaultModel
        }
        return configuredModels.getOrPut(option to enableGoogleSearch) {
            val tools = if (enableGoogleSearch) listOf(Tool.googleSearch()) else emptyList()
            Firebase.ai.generativeModel(
                modelName = option.modelName,
                systemInstruction = agentSystemInstruction,
                tools = tools
            )
        }
    }

    fun selectModel(option: ChatAgentModelOption) {
        _uiState.update { it.copy(selectedModel = option) }
    }

    fun toggleGoogleSearch() {
        _uiState.update { it.copy(googleSearchEnabled = !it.googleSearchEnabled) }
    }

    fun setPendingAttachment(bitmap: Bitmap?) {
        _uiState.update { it.copy(pendingAttachment = bitmap) }
    }

    fun clearError() {
        _uiState.update { state ->
            if (state.turnState is ChatTurnUiState.Error) {
                state.copy(turnState = ChatTurnUiState.Idle)
            } else {
                state
            }
        }
    }

    fun stopGeneration() {
        activeStreamJob?.cancel()
        activeStreamJob = null
        val partial = (_uiState.value.turnState as? ChatTurnUiState.Streaming)?.partialText.orEmpty().trim()
        _uiState.update { state ->
            val updatedMessages = if (partial.isNotEmpty()) {
                state.messages + AgentChatMessage(
                    id = "agent_${System.currentTimeMillis()}_${state.messages.size}",
                    isUser = false,
                    text = partial
                )
            } else {
                state.messages
            }
            state.copy(messages = updatedMessages, turnState = ChatTurnUiState.Idle)
        }
    }

    fun clearConversation() {
        activeStreamJob?.cancel()
        activeStreamJob = null
        lastAttemptPrompt = null
        lastAttemptImage = null
        _uiState.update {
            it.copy(
                messages = emptyList(),
                turnState = ChatTurnUiState.Idle,
                pendingAttachment = null
            )
        }
    }

    fun sendMessage(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        val attachment = _uiState.value.pendingAttachment
        if (prompt.isEmpty() && attachment == null) return
        if (_uiState.value.turnState is ChatTurnUiState.Streaming) return

        val effectivePrompt = prompt.ifEmpty { "حلّل هذه الصورة بالتفصيل." }
        val priorHistory = _uiState.value.messages
        val userMessage = AgentChatMessage(
            id = "user_${System.currentTimeMillis()}_${priorHistory.size}",
            isUser = true,
            text = effectivePrompt,
            attachedImage = attachment
        )

        lastAttemptPrompt = effectivePrompt
        lastAttemptImage = attachment

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                pendingAttachment = null,
                turnState = ChatTurnUiState.Streaming("")
            )
        }

        streamAgentReply(
            prompt = effectivePrompt,
            attachment = attachment,
            historyMessages = priorHistory
        )
    }

    fun retryLastMessage() {
        val retryPrompt = lastAttemptPrompt ?: return
        val retryImage = lastAttemptImage
        if (_uiState.value.turnState is ChatTurnUiState.Streaming) return

        val historyWithoutFailedTurn = _uiState.value.messages.dropLastWhile { it.isUser }
        val userMessage = AgentChatMessage(
            id = "user_retry_${System.currentTimeMillis()}",
            isUser = true,
            text = retryPrompt,
            attachedImage = retryImage
        )
        _uiState.update {
            it.copy(
                messages = historyWithoutFailedTurn + userMessage,
                turnState = ChatTurnUiState.Streaming("")
            )
        }
        streamAgentReply(
            prompt = retryPrompt,
            attachment = retryImage,
            historyMessages = historyWithoutFailedTurn
        )
    }

    private fun streamAgentReply(
        prompt: String,
        attachment: Bitmap?,
        historyMessages: List<AgentChatMessage>
    ) {
        activeStreamJob?.cancel()
        activeStreamJob = viewModelScope.launch {
            val stateSnapshot = _uiState.value
            val model = resolveModel(
                option = stateSnapshot.selectedModel,
                enableGoogleSearch = stateSnapshot.googleSearchEnabled
            )
            val chatHistory = buildGeminiChatHistory(historyMessages)
            val chat = model.startChat(history = chatHistory)
            val accumulatedText = StringBuilder()
            val sources = linkedSetOf<String>()

            try {
                withTimeout(CHAT_TIMEOUT_MS) {
                    val userContent = content("user") {
                        if (attachment != null) {
                            image(attachment)
                        }
                        text(prompt)
                    }
                    chat.sendMessageStream(userContent).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        if (chunkText.isNotEmpty()) {
                            accumulatedText.append(chunkText)
                            _uiState.update {
                                it.copy(turnState = ChatTurnUiState.Streaming(accumulatedText.toString()))
                            }
                        }
                        chunk.candidates.firstOrNull()?.groundingMetadata?.groundingChunks?.forEach { gChunk ->
                            val web = gChunk.web
                            if (web != null) {
                                val title = web.title?.takeIf { it.isNotBlank() } ?: web.domain.orEmpty()
                                val uri = web.uri.orEmpty()
                                if (title.isNotBlank() && uri.isNotBlank()) {
                                    sources.add("$title ($uri)")
                                }
                            }
                        }
                    }
                }

                val finalReply = accumulatedText.toString().trim()
                if (finalReply.isEmpty()) {
                    _uiState.update {
                        it.copy(turnState = ChatTurnUiState.Error("أعاد النموذج استجابة فارغة. حاول مجدداً."))
                    }
                } else {
                    val assistantMessage = AgentChatMessage(
                        id = "agent_${System.currentTimeMillis()}_${_uiState.value.messages.size}",
                        isUser = false,
                        text = finalReply,
                        groundingSources = sources.take(4)
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            turnState = ChatTurnUiState.Idle
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Firebase AI chat stream timed out", e)
                _uiState.update {
                    it.copy(
                        turnState = ChatTurnUiState.Error(
                            "انتهت مهلة الاستجابة (60 ثانية). يرجى المحاولة مرة أخرى."
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI chat stream failed", e)
                _uiState.update {
                    it.copy(
                        turnState = ChatTurnUiState.Error(
                            e.localizedMessage ?: "تعذر الاتصال بوكيل Firebase AI."
                        )
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeStreamJob?.cancel()
    }
}

internal fun buildGeminiChatHistory(messages: List<AgentChatMessage>): List<Content> {
    val merged = mutableListOf<Pair<String, String>>()
    messages.takeLast(14).forEach { msg ->
        val clean = msg.text.trim()
        if (clean.isEmpty()) return@forEach
        val role = if (msg.isUser) "user" else "model"
        if (merged.isEmpty() && role != "user") return@forEach
        if (merged.lastOrNull()?.first == role) {
            val prev = merged.removeAt(merged.lastIndex)
            merged.add(role to "${prev.second}\n$clean")
        } else {
            merged.add(role to clean)
        }
    }
    if (merged.lastOrNull()?.first == "user") {
        merged.removeAt(merged.lastIndex)
    }
    return merged.map { (role, text) ->
        content(role) { text(text) }
    }
}
