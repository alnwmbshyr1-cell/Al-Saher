package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AlSaherAiService
import com.example.data.AlSaherRepository
import com.example.data.ThemePreferences
import com.example.model.AgentMode
import com.example.model.AgentTask
import com.example.model.AudioTranscribeUiState
import com.example.model.AutomationRule
import com.example.model.ChatbotRole
import com.example.model.ConnectorCatalogItem
import com.example.model.ConnectorState
import com.example.model.GeminiChatTier
import com.example.model.ImageAspectOption
import com.example.model.ImageGenUiState
import com.example.model.LiveTranscriptLine
import com.example.model.LiveVoiceUiState
import com.example.model.RemoteLogEntry
import com.example.model.SlideCardModel
import com.example.model.StudioProject
import com.example.model.TaskMessage
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.VideoTimelineClip
import com.example.model.VideoTimelineModel
import com.example.model.WideResearchAgentOutput
import com.example.model.WorkspaceDestination
import com.example.model.parseEnumSafe
import com.example.util.ChatExportFormat
import com.example.util.ChatExportHelper
import com.example.util.ChatExportScope
import com.example.util.ExportableConversation
import com.example.util.NotificationAndExportHelper
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.liveAudioConversationConfig
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "AlSaherVM"
private const val STOP_TIMEOUT_MS = 5000L

val OFFICIAL_CONNECTORS = listOf(
    ConnectorCatalogItem("gmail", "Gmail", "البريد الإلكتروني", "قراءة الرسائل الواردة وتلخيصها وإرسال الردود التلقائية"),
    ConnectorCatalogItem("drive", "Google Drive", "التخزين السحابي", "الوصول إلى المستندات والملفات ومشاركتها مع الوكيل"),
    ConnectorCatalogItem("calendar", "Google Calendar", "المواعيد", "جدولة الاجتماعات وتفعيل الأتمتة عند الأحداث الجديدة"),
    ConnectorCatalogItem("outlook", "Outlook", "البريد والعمل", "مزامنة بريد العمل والتقويم المؤسسي"),
    ConnectorCatalogItem("slack", "Slack", "التواصل الفوري", "متابعة القنوات وإرسال الملخصات والتنبيهات للفرق"),
    ConnectorCatalogItem("notion", "Notion", "قواعد المعرفة", "قراءة الصفحات وتحديث المهام عند تعديل مساحات العمل"),
    ConnectorCatalogItem("github", "GitHub", "البرمجة والتطوير", "إدارة المستودعات ومراجعة الأكواد ونشر المشاريع"),
    ConnectorCatalogItem("shopify", "Shopify", "التجارة الإلكترونية", "إدارة المنتجات والطلبات وتحليلات المتجر"),
    ConnectorCatalogItem("canva", "Canva", "التصميم", "تنسيق القوالب البصرية وإعداد المواد الإعلانية"),
    ConnectorCatalogItem("hubspot", "HubSpot", "إدارة العملاء", "تتبع العملاء المحتملين وتحديث الصفقات والتقارير")
)

class AlSaherViewModel(
    private val repository: AlSaherRepository,
    private val aiService: AlSaherAiService,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String,
    private val themePreferences: ThemePreferences? = null
) : ViewModel() {

    private val _destination = MutableStateFlow(WorkspaceDestination.CHAT_AGENT)
    val destination: StateFlow<WorkspaceDestination> = _destination.asStateFlow()

    private val _agentMode = MutableStateFlow(AgentMode.AGENT)
    val agentMode: StateFlow<AgentMode> = _agentMode.asStateFlow()

    private val _activeTaskId = MutableStateFlow<String?>(null)
    val activeTaskId: StateFlow<String?> = _activeTaskId.asStateFlow()

    private val _streamingReply = MutableStateFlow<String?>(null)
    val streamingReply: StateFlow<String?> = _streamingReply.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    private val _notifyOnComplete = MutableStateFlow(true)
    val notifyOnComplete: StateFlow<Boolean> = _notifyOnComplete.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _showPricingSheet = MutableStateFlow(false)
    val showPricingSheet: StateFlow<Boolean> = _showPricingSheet.asStateFlow()

    private val _chatTier = MutableStateFlow(GeminiChatTier.GENERAL)
    val chatTier: StateFlow<GeminiChatTier> = _chatTier.asStateFlow()

    private val _chatbotRole = MutableStateFlow(ChatbotRole.GENERAL_ASSISTANT)
    val chatbotRole: StateFlow<ChatbotRole> = _chatbotRole.asStateFlow()

    private val _searchGroundingEnabled = MutableStateFlow(true)
    val searchGroundingEnabled: StateFlow<Boolean> = _searchGroundingEnabled.asStateFlow()

    private val _mapsGroundingEnabled = MutableStateFlow(false)
    val mapsGroundingEnabled: StateFlow<Boolean> = _mapsGroundingEnabled.asStateFlow()

    private val _imageStudioState = MutableStateFlow<ImageGenUiState>(ImageGenUiState.Idle)
    val imageStudioState: StateFlow<ImageGenUiState> = _imageStudioState.asStateFlow()

    private val _liveVoiceState = MutableStateFlow<LiveVoiceUiState>(LiveVoiceUiState.Idle)
    val liveVoiceState: StateFlow<LiveVoiceUiState> = _liveVoiceState.asStateFlow()

    private val _audioTranscribeState = MutableStateFlow<AudioTranscribeUiState>(AudioTranscribeUiState.Idle)
    val audioTranscribeState: StateFlow<AudioTranscribeUiState> = _audioTranscribeState.asStateFlow()

    private val _isExportingChat = MutableStateFlow(false)
    val isExportingChat: StateFlow<Boolean> = _isExportingChat.asStateFlow()

    // Studio state holders
    private val _activeVideoProjectId = MutableStateFlow<String?>(null)
    private val _videoTimeline = MutableStateFlow<VideoTimelineModel?>(null)
    val videoTimeline: StateFlow<VideoTimelineModel?> = _videoTimeline.asStateFlow()

    private val _slidesDeck = MutableStateFlow<Pair<String, List<SlideCardModel>>?>(null)
    val slidesDeck: StateFlow<Pair<String, List<SlideCardModel>>?> = _slidesDeck.asStateFlow()

    private val _wideResearchOutputs = MutableStateFlow<List<WideResearchAgentOutput>>(emptyList())
    val wideResearchOutputs: StateFlow<List<WideResearchAgentOutput>> = _wideResearchOutputs.asStateFlow()

    private val _studioGeneratedDoc = MutableStateFlow<Pair<String, String>?>(null)
    val studioGeneratedDoc: StateFlow<Pair<String, String>?> = _studioGeneratedDoc.asStateFlow()

    private val _remoteLogs = MutableStateFlow(
        listOf(
            RemoteLogEntry(
                timestamp = "00:00",
                command = "cloud-vm --status",
                output = "بيئة الحاسوب السحابي لـ الساحر جاهزة (Ubuntu 24.04 • 8 vCPU • 32GB RAM)",
                status = "متصل"
            )
        )
    )
    val remoteLogs: StateFlow<List<RemoteLogEntry>> = _remoteLogs.asStateFlow()

    private var activeAgentJob: Job? = null
    private var liveVoiceJob: Job? = null
    @OptIn(PublicPreviewAPI::class)
    private var activeLiveSession: LiveSession? = null
    private val _isPaused = MutableStateFlow(false)

    val userProfile: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = unwrapFirestoreError(error)
            if (fsErr == null && error is CancellationException) throw error
            emit(UiState.Error((fsErr ?: error).localizedMessage ?: "تعذر تحميل ملف الحساب"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val tasks: StateFlow<UiState<List<AgentTask>>> = repository.observeTasks(currentUserId)
        .map<List<AgentTask>, UiState<List<AgentTask>>> { list ->
            if (_activeTaskId.value == null && list.isNotEmpty()) {
                _activeTaskId.value = list.first().id
            }
            UiState.Success(list)
        }
        .catch { error ->
            val fsErr = unwrapFirestoreError(error)
            if (fsErr == null && error is CancellationException) throw error
            emit(UiState.Error((fsErr ?: error).localizedMessage ?: "تعذر تحميل المهام"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<UiState<List<TaskMessage>>> = _activeTaskId
        .flatMapLatest { taskId ->
            if (taskId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeTaskMessages(currentUserId, taskId)
                    .map<List<TaskMessage>, UiState<List<TaskMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val fsErr = unwrapFirestoreError(error)
                        if (fsErr == null && error is CancellationException) throw error
                        emit(UiState.Error((fsErr ?: error).localizedMessage ?: "تعذر تحميل الرسائل"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Success(emptyList()))

    val projects: StateFlow<UiState<List<StudioProject>>> = repository.observeProjects(currentUserId)
        .map<List<StudioProject>, UiState<List<StudioProject>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = unwrapFirestoreError(error)
            if (fsErr == null && error is CancellationException) throw error
            emit(UiState.Error((fsErr ?: error).localizedMessage ?: "تعذر تحميل المشاريع"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val automations: StateFlow<UiState<List<AutomationRule>>> = repository.observeAutomations(currentUserId)
        .map<List<AutomationRule>, UiState<List<AutomationRule>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = unwrapFirestoreError(error)
            if (fsErr == null && error is CancellationException) throw error
            emit(UiState.Error((fsErr ?: error).localizedMessage ?: "تعذر تحميل الأتمتة"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val connectors: StateFlow<UiState<List<ConnectorState>>> = repository.observeConnectors(currentUserId)
        .map<List<ConnectorState>, UiState<List<ConnectorState>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = unwrapFirestoreError(error)
            if (fsErr == null && error is CancellationException) throw error
            emit(UiState.Error((fsErr ?: error).localizedMessage ?: "تعذر تحميل الموصلات"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, userDisplayName, userEmail)
                .onFailure { e ->
                    Log.w(TAG, "Profile bootstrap failed", e)
                }
        }
        themePreferences?.let { prefs ->
            viewModelScope.launch {
                prefs.isDarkThemeFlow.collect { savedDark ->
                    _isDarkTheme.value = savedDark
                }
            }
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    fun setDestination(dest: WorkspaceDestination) {
        _destination.value = dest
    }

    fun setAgentMode(mode: AgentMode) {
        _agentMode.value = mode
    }

    fun selectTask(taskId: String?) {
        _activeTaskId.value = taskId
        _streamingReply.value = null
        val selected = (tasks.value as? UiState.Success)?.data?.firstOrNull { it.id == taskId }
        if (selected != null) {
            _agentMode.value = parseEnumSafe(selected.mode, AgentMode.AGENT)
        }
        _destination.value = WorkspaceDestination.CHAT_AGENT
    }

    fun startNewSession() {
        _activeTaskId.value = null
        _streamingReply.value = null
        _destination.value = WorkspaceDestination.CHAT_AGENT
    }

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        themePreferences?.let { prefs ->
            viewModelScope.launch {
                prefs.setDarkTheme(isDark)
            }
        }
    }

    fun toggleTheme() {
        setDarkTheme(!_isDarkTheme.value)
    }

    fun setNotifyOnComplete(enabled: Boolean) {
        _notifyOnComplete.value = enabled
    }

    fun setShowPricingSheet(show: Boolean) {
        _showPricingSheet.value = show
    }

    fun setChatTier(tier: GeminiChatTier) {
        _chatTier.value = tier
    }

    fun setChatbotRole(role: ChatbotRole) {
        _chatbotRole.value = role
    }

    fun toggleSearchGrounding() {
        _searchGroundingEnabled.value = !_searchGroundingEnabled.value
    }

    fun toggleMapsGrounding() {
        _mapsGroundingEnabled.value = !_mapsGroundingEnabled.value
    }

    @OptIn(PublicPreviewAPI::class)
    fun startLiveVoiceConversation() {
        liveVoiceJob?.cancel()
        liveVoiceJob = viewModelScope.launch {
            _liveVoiceState.value = LiveVoiceUiState.Connecting
            try {
                val session = aiService.connectLiveVoiceSession(_chatbotRole.value)
                activeLiveSession = session
                val transcriptLines = mutableListOf<LiveTranscriptLine>()
                _liveVoiceState.value = LiveVoiceUiState.Active(
                    statusText = "متصل عبر gemini-3.8-live — تكلّم الآن مباشرة",
                    transcripts = emptyList()
                )
                val config = liveAudioConversationConfig {
                    enableInterruptions = true
                    transcriptHandler = { inputTranscription, outputTranscription ->
                        val inText = inputTranscription?.text.orEmpty().trim()
                        val outText = outputTranscription?.text.orEmpty().trim()
                        var changed = false
                        if (inText.isNotEmpty()) {
                            appendOrMergeTranscript(transcriptLines, isUser = true, text = inText)
                            changed = true
                        }
                        if (outText.isNotEmpty()) {
                            appendOrMergeTranscript(transcriptLines, isUser = false, text = outText)
                            changed = true
                        }
                        if (changed) {
                            _liveVoiceState.value = LiveVoiceUiState.Active(
                                statusText = "محادثة صوتية نشطة (gemini-3.8-live)",
                                transcripts = transcriptLines.toList()
                            )
                        }
                    }
                    goAwayHandler = {
                        _liveVoiceState.value = LiveVoiceUiState.Active(
                            statusText = "تم إنهاء الجلسة من الخادم",
                            transcripts = transcriptLines.toList()
                        )
                    }
                }
                session.startAudioConversation(config)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Live API connection timed out", e)
                _liveVoiceState.value = LiveVoiceUiState.Error("انتهت مهلة الاتصال بالمحادثة الصوتية الحية.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Live voice conversation failed", e)
                _liveVoiceState.value = LiveVoiceUiState.Error(
                    e.localizedMessage ?: "تعذر بدء المحادثة الصوتية الحية عبر gemini-3.8-live."
                )
            }
        }
    }

    private fun appendOrMergeTranscript(
        list: MutableList<LiveTranscriptLine>,
        isUser: Boolean,
        text: String
    ) {
        val last = list.lastOrNull()
        if (last != null && last.isUser == isUser) {
            list[list.lastIndex] = last.copy(text = "${last.text} $text".trim())
        } else {
            list.add(
                LiveTranscriptLine(
                    id = "live_${list.size + 1}_${System.currentTimeMillis()}",
                    isUser = isUser,
                    text = text
                )
            )
        }
    }

    @OptIn(PublicPreviewAPI::class)
    fun stopLiveVoiceConversation() {
        liveVoiceJob?.cancel()
        liveVoiceJob = null
        val session = activeLiveSession
        activeLiveSession = null
        viewModelScope.launch {
            runCatching { session?.stopAudioConversation() }
            runCatching { session?.close() }
        }
        _liveVoiceState.value = LiveVoiceUiState.Idle
    }

    fun transcribeAudioRecording(audioBytes: ByteArray) {
        viewModelScope.launch {
            _audioTranscribeState.value = AudioTranscribeUiState.Transcribing
            try {
                val transcript = aiService.transcribeAudioBytes(audioBytes)
                _audioTranscribeState.value = AudioTranscribeUiState.Success(transcript)
            } catch (e: TimeoutCancellationException) {
                _audioTranscribeState.value = AudioTranscribeUiState.Error("انتهت مهلة التفريغ الصوتي.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Audio transcription failed", e)
                _audioTranscribeState.value = AudioTranscribeUiState.Error(
                    e.localizedMessage ?: "تعذر تفريغ التسجيل عبر gemini-3.5-transcribe."
                )
            }
        }
    }

    fun resetAudioTranscribeState() {
        _audioTranscribeState.value = AudioTranscribeUiState.Idle
    }

    @OptIn(PublicPreviewAPI::class)
    override fun onCleared() {
        super.onCleared()
        runCatching { activeLiveSession?.stopAudioConversation() }
        activeLiveSession = null
    }

    fun generateOrEditImage(prompt: String, inputBitmap: Bitmap?, aspectOption: ImageAspectOption) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) return
        viewModelScope.launch {
            _imageStudioState.value = ImageGenUiState.Loading
            _errorBanner.value = null
            try {
                val bitmap = aiService.generateOrEditImage(cleanPrompt, inputBitmap, aspectOption)
                _imageStudioState.value = ImageGenUiState.Success(bitmap = bitmap, prompt = cleanPrompt)
                repository.saveProject(
                    userId = currentUserId,
                    title = "صورة: ${cleanPrompt.take(45)}",
                    studioType = "IMAGE",
                    subType = aspectOption.arabicLabel,
                    prompt = cleanPrompt,
                    content = if (inputBitmap != null) "تعديل صورة باستخدام gemini-nano-banana-2.1" else "توليد صورة باستخدام gemini-nano-banana-2.1",
                    tracksSummary = aspectOption.arabicLabel,
                    status = "READY"
                )
                val credits = (userProfile.value as? UiState.Success)?.data?.creditsBalance ?: 1000L
                repository.deductCredits(currentUserId, credits, 10L)
                _statusMessage.value = "تم إنشاء الصورة بنجاح."
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Image generation timed out", e)
                _imageStudioState.value = ImageGenUiState.Error("انتهت مهلة توليد الصورة (60 ثانية). حاول مجدداً.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image generation failed", e)
                _imageStudioState.value = ImageGenUiState.Error(
                    e.localizedMessage ?: "تعذر إنشاء الصورة. تحقق من الاتصال وخطة المشروع."
                )
            }
        }
    }

    fun clearMessages() {
        _errorBanner.value = null
        _statusMessage.value = null
    }

    fun exportChatHistoryToUri(
        appContext: Context,
        uri: Uri,
        format: ChatExportFormat,
        scope: ChatExportScope
    ) {
        viewModelScope.launch {
            _isExportingChat.value = true
            _errorBanner.value = null
            try {
                val allTasks = (tasks.value as? UiState.Success)?.data.orEmpty()
                val targetTasks = when (scope) {
                    ChatExportScope.CURRENT -> {
                        val activeId = _activeTaskId.value
                        val currentTask = allTasks.firstOrNull { it.id == activeId }
                        if (currentTask != null) listOf(currentTask) else emptyList()
                    }
                    ChatExportScope.ALL -> allTasks
                }

                if (targetTasks.isEmpty()) {
                    _errorBanner.value = "لا توجد محادثات لتصديرها حالياً."
                    return@launch
                }

                val currentLoadedMessages = (activeMessages.value as? UiState.Success)?.data.orEmpty()
                val exportItems = coroutineScope {
                    targetTasks.map { task ->
                        async {
                            val msgs = if (task.id == _activeTaskId.value && currentLoadedMessages.isNotEmpty()) {
                                currentLoadedMessages
                            } else {
                                repository.getTaskMessagesOnce(currentUserId, task.id).getOrThrow()
                            }
                            ExportableConversation(task = task, messages = msgs)
                        }
                    }.awaitAll()
                }

                val serialized = withContext(Dispatchers.Default) {
                    when (format) {
                        ChatExportFormat.JSON -> ChatExportHelper.formatConversationsAsJson(exportItems)
                        ChatExportFormat.TEXT -> ChatExportHelper.formatConversationsAsText(exportItems)
                    }
                }

                withContext(Dispatchers.IO) {
                    ChatExportHelper.writeTextToUri(appContext, uri, serialized).getOrThrow()
                }

                _statusMessage.value = "تم تصدير ${exportItems.size} محادثة بنجاح بصيغة ${format.extension.uppercase()}."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat history export failed", e)
                _errorBanner.value = e.localizedMessage ?: "تعذر تصدير سجل المحادثات."
            } finally {
                _isExportingChat.value = false
            }
        }
    }

    fun sendPrompt(promptText: String, appContext: Context, forceMode: AgentMode? = null) {
        val clean = promptText.trim()
        if (clean.isBlank()) return
        val effectiveMode = forceMode ?: _agentMode.value
        _errorBanner.value = null

        if (effectiveMode == AgentMode.CHAT) {
            runFreeChat(clean)
        } else {
            runGeneralAgent(clean, appContext)
        }
    }

    private fun runFreeChat(prompt: String) {
        activeAgentJob?.cancel()
        activeAgentJob = viewModelScope.launch {
            _isBusy.value = true
            _streamingReply.value = ""
            try {
                var taskId = _activeTaskId.value
                if (taskId.isNullOrBlank()) {
                    val created = repository.createTask(
                        userId = currentUserId,
                        title = prompt.take(60),
                        mode = AgentMode.CHAT.name,
                        category = "GENERAL",
                        status = "COMPLETED",
                        prompt = prompt,
                        stepTitles = listOf("إجابة فورية عبر الدردشة المجانية"),
                        creditsUsed = 0L
                    )
                    taskId = created.getOrThrow()
                    _activeTaskId.value = taskId
                }

                val history = (activeMessages.value as? UiState.Success)?.data
                    ?.map { it.role to it.content } ?: emptyList()

                repository.addMessage(currentUserId, taskId, "USER", prompt, AgentMode.CHAT.name)

                var finalReply = ""
                aiService.streamFreeChat(
                    prompt = prompt,
                    historyContext = history,
                    tier = _chatTier.value,
                    role = _chatbotRole.value,
                    enableSearchGrounding = _searchGroundingEnabled.value,
                    enableMapsGrounding = _mapsGroundingEnabled.value
                ).collect { partial ->
                    finalReply = partial
                    _streamingReply.value = partial
                }

                if (finalReply.isNotBlank()) {
                    repository.addMessage(currentUserId, taskId, "ASSISTANT", finalReply, AgentMode.CHAT.name)
                }
                _streamingReply.value = null
            } catch (e: TimeoutCancellationException) {
                _errorBanner.value = "انتهت مهلة الرد السريع. حاول مرة أخرى."
                _streamingReply.value = null
            } catch (e: CancellationException) {
                _streamingReply.value = null
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Free chat failed", e)
                _errorBanner.value = e.localizedMessage ?: "تعذر الاتصال بنموذج الساحر."
                _streamingReply.value = null
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun promoteChatToAgent(prompt: String, appContext: Context) {
        _agentMode.value = AgentMode.AGENT
        runGeneralAgent(prompt, appContext)
    }

    private fun runGeneralAgent(prompt: String, appContext: Context) {
        val currentCredits = (userProfile.value as? UiState.Success)?.data?.creditsBalance ?: 1000L
        if (currentCredits < 10L) {
            _errorBanner.value = "رصيد النقاط غير كافٍ. يمكنك الترقية أو استخدام الدردشة المجانية."
            _showPricingSheet.value = true
            return
        }

        activeAgentJob?.cancel()
        _isPaused.value = false
        activeAgentJob = viewModelScope.launch {
            _isBusy.value = true
            _streamingReply.value = "جاري تحليل الطلب وتجهيز بيئة الحاسوب السحابي..."
            var createdTaskId: String? = _activeTaskId.value
            try {
                val initialSteps = listOf(
                    "فهم المتطلبات وتقسيم المهمة",
                    "تشغيل الحاسوب السحابي وجمع المعطيات",
                    "بناء المخرجات وتنسيق الملفات",
                    "المراجعة النهائية والتسليم"
                )
                val taskId = if (createdTaskId.isNullOrBlank()) {
                    val taskRes = repository.createTask(
                        userId = currentUserId,
                        title = prompt.take(65),
                        mode = AgentMode.AGENT.name,
                        category = "GENERAL",
                        status = "PLANNING",
                        prompt = prompt,
                        stepTitles = initialSteps,
                        creditsUsed = 15L
                    )
                    taskRes.getOrThrow().also {
                        createdTaskId = it
                        _activeTaskId.value = it
                    }
                } else {
                    repository.updateTaskProgress(currentUserId, createdTaskId!!, "PLANNING", 0L)
                    createdTaskId!!
                }
                repository.addMessage(currentUserId, taskId, "USER", prompt, AgentMode.AGENT.name)

                val connectedApps = (connectors.value as? UiState.Success)?.data
                    ?.filter { it.connected }?.map { it.displayName } ?: emptyList()

                repository.updateTaskProgress(currentUserId, taskId, "RUNNING", 1L)
                _streamingReply.value = "يقوم الوكيل بتنفيذ الخطوات على الحاسوب السحابي..."

                val plan = aiService.planAndExecuteAgentTask(prompt, connectedApps)

                // Step-by-step visible cloud execution with pause/stop support
                repository.updateTaskEdit(
                    userId = currentUserId,
                    taskId = taskId,
                    title = plan.title,
                    prompt = prompt,
                    mode = AgentMode.AGENT.name,
                    status = "RUNNING",
                    stepTitles = plan.steps,
                    currentStepIndex = 1L
                )

                for (stepIdx in 1..plan.steps.size) {
                    while (_isPaused.value) {
                        delay(300L)
                    }
                    repository.updateTaskProgress(currentUserId, taskId, "RUNNING", stepIdx.toLong())
                    delay(450L)
                }

                repository.completeTask(
                    userId = currentUserId,
                    taskId = taskId,
                    status = "COMPLETED",
                    summary = plan.summary,
                    stepTitles = plan.steps,
                    currentStepIndex = plan.steps.size.toLong(),
                    deliverableTitle = plan.deliverableTitle,
                    deliverableContent = plan.deliverableContent,
                    creditsUsed = 15L
                )

                val assistantText = buildString {
                    append("✅ **").append(plan.title).append("**\n\n")
                    append(plan.summary).append("\n\n")
                    append("📦 **المخرج الجاهز: ").append(plan.deliverableTitle).append("**\n")
                    append(plan.deliverableContent)
                }
                repository.addMessage(currentUserId, taskId, "ASSISTANT", assistantText, AgentMode.AGENT.name)
                repository.deductCredits(currentUserId, currentCredits, 15L)

                if (_notifyOnComplete.value) {
                    NotificationAndExportHelper.showAgentCompletionNotification(
                        context = appContext,
                        title = plan.title,
                        summary = plan.summary
                    )
                }
                _streamingReply.value = null
            } catch (e: TimeoutCancellationException) {
                createdTaskId?.let {
                    repository.updateTaskProgress(currentUserId, it, "ERROR", 0L)
                }
                _errorBanner.value = "انتهت مهلة تنفيذ الوكيل (60 ثانية). يمكنك إعادة المحاولة."
                _streamingReply.value = null
            } catch (e: CancellationException) {
                _streamingReply.value = null
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Agent execution failed", e)
                createdTaskId?.let {
                    repository.updateTaskProgress(currentUserId, it, "ERROR", 0L)
                }
                _errorBanner.value = e.localizedMessage ?: "تعذر إكمال مهمة الوكيل."
                _streamingReply.value = null
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun stopActiveAgent(task: AgentTask?) {
        activeAgentJob?.cancel()
        activeAgentJob = null
        _isBusy.value = false
        _isPaused.value = false
        _streamingReply.value = null
        if (task != null) {
            viewModelScope.launch {
                repository.updateTaskProgress(currentUserId, task.id, "STOPPED", task.currentStepIndex)
                _statusMessage.value = "تم إيقاف المهمة بناءً على طلبك."
            }
        }
    }

    fun togglePauseTask(task: AgentTask) {
        val nextPaused = !_isPaused.value
        _isPaused.value = nextPaused
        viewModelScope.launch {
            val nextStatus = if (nextPaused) "PAUSED" else "RUNNING"
            repository.updateTaskProgress(currentUserId, task.id, nextStatus, task.currentStepIndex)
        }
    }

    fun editAndRerunTask(task: AgentTask, updatedPrompt: String, appContext: Context) {
        if (updatedPrompt.isBlank()) return
        activeAgentJob?.cancel()
        viewModelScope.launch {
            repository.updateTaskEdit(
                userId = currentUserId,
                taskId = task.id,
                title = updatedPrompt.take(65),
                prompt = updatedPrompt,
                mode = AgentMode.AGENT.name,
                status = "PLANNING",
                stepTitles = task.stepTitles,
                currentStepIndex = 0L
            )
            _activeTaskId.value = task.id
            runGeneralAgent(updatedPrompt, appContext)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(currentUserId, taskId)
            if (_activeTaskId.value == taskId) {
                _activeTaskId.value = null
            }
        }
    }

    // --- VIDEO STUDIO ---
    fun generateVideo(prompt: String, videoType: String, creationMode: String, attachedBitmap: Bitmap?) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isBusy.value = true
            _errorBanner.value = null
            try {
                val model = aiService.generateVideoProject(prompt, videoType, creationMode, attachedBitmap)
                _videoTimeline.value = model
                val tracksDesc = model.clips.joinToString("\n") {
                    "• ${it.shotTitle} (${it.durationSeconds}ث): ${it.captionText}"
                }
                val savedId = repository.saveProject(
                    userId = currentUserId,
                    title = model.title,
                    studioType = "VIDEO",
                    subType = videoType,
                    prompt = prompt,
                    content = "التعليق الصوتي:\n${model.voiceoverScript}\n\nالموسيقى: ${model.musicTrack}\nالمؤثرات: ${model.sfxNotes}",
                    tracksSummary = tracksDesc,
                    status = "READY"
                )
                _activeVideoProjectId.value = savedId.getOrNull()
                val credits = (userProfile.value as? UiState.Success)?.data?.creditsBalance ?: 1000L
                repository.deductCredits(currentUserId, credits, 20L)
                _statusMessage.value = "تم توليد مشروع الفيديو القابل للتعديل وحفظه."
            } catch (e: TimeoutCancellationException) {
                _errorBanner.value = "انتهت مهلة توليد الفيديو."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Video generation failed", e)
                _errorBanner.value = e.localizedMessage ?: "تعذر توليد مشروع الفيديو."
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun saveCurrentVideoEdits() {
        val model = _videoTimeline.value ?: return
        val projectId = _activeVideoProjectId.value
        viewModelScope.launch {
            val tracksDesc = model.clips.joinToString("\n") {
                "• ${it.shotTitle} (${it.durationSeconds}ث): ${it.captionText}"
            }
            val content = "التعليق الصوتي:\n${model.voiceoverScript}\n\nالموسيقى: ${model.musicTrack}\nالمؤثرات: ${model.sfxNotes}"
            if (projectId.isNullOrBlank()) {
                repository.saveProject(
                    userId = currentUserId,
                    title = model.title,
                    studioType = "VIDEO",
                    subType = model.videoType,
                    prompt = model.title,
                    content = content,
                    tracksSummary = tracksDesc,
                    status = "READY"
                ).onSuccess { newId ->
                    _activeVideoProjectId.value = newId
                    _statusMessage.value = "تم حفظ تعديلات الفيديو في السحابة."
                }
            } else {
                repository.updateProject(
                    userId = currentUserId,
                    projectId = projectId,
                    title = model.title,
                    subType = model.videoType,
                    prompt = model.title,
                    content = content,
                    tracksSummary = tracksDesc,
                    status = "READY"
                ).onSuccess {
                    _statusMessage.value = "تم تحديث مشروع الفيديو بنجاح."
                }
            }
        }
    }

    fun loadProjectIntoStudio(project: StudioProject) {
        when (project.studioType) {
            "VIDEO" -> {
                _activeVideoProjectId.value = project.id
                val parsedClips = project.tracksSummary.lines().filter { it.isNotBlank() }.mapIndexed { idx, line ->
                    val clean = line.removePrefix("•").trim()
                    val title = clean.substringBefore("(").trim().ifBlank { "لقطة ${idx + 1}" }
                    val caption = clean.substringAfter(":", "").trim().ifBlank { clean }
                    VideoTimelineClip(
                        id = "clip_loaded_${idx + 1}",
                        shotTitle = title,
                        visualPrompt = project.prompt,
                        durationSeconds = 5,
                        captionText = caption
                    )
                }.ifEmpty {
                    listOf(
                        VideoTimelineClip("clip_1", "اللقطة الافتتاحية", project.prompt, 5, project.title)
                    )
                }
                _videoTimeline.value = VideoTimelineModel(
                    title = project.title,
                    videoType = project.subType,
                    voiceoverScript = project.content,
                    musicTrack = "إيقاع سينمائي حماسي",
                    sfxNotes = "انتقالات صوتية ناعمة",
                    clips = parsedClips
                )
            }
            "DOCS", "WEB", "APP", "GAME" -> {
                _studioGeneratedDoc.value = project.title to project.content
            }
        }
    }

    fun updateVideoClip(clipId: String, newDuration: Int, newCaption: String) {
        val current = _videoTimeline.value ?: return
        val updated = current.clips.map { clip ->
            if (clip.id == clipId) {
                clip.copy(durationSeconds = newDuration.coerceIn(2, 30), captionText = newCaption)
            } else clip
        }
        _videoTimeline.value = current.copy(clips = updated)
    }

    fun swapVideoMusic(newMusic: String) {
        val current = _videoTimeline.value ?: return
        _videoTimeline.value = current.copy(musicTrack = newMusic)
    }

    fun addCustomClipToTimeline(title: String) {
        val current = _videoTimeline.value ?: return
        val next = VideoTimelineClip(
            id = "clip_custom_${current.clips.size + 1}",
            shotTitle = title.ifBlank { "لقطة مضافة من المستخدم" },
            visualPrompt = "لقطة مرفوعة من المعرض",
            durationSeconds = 4,
            captionText = "مشهد مخصص"
        )
        _videoTimeline.value = current.copy(clips = current.clips + next)
    }

    // --- SLIDES STUDIO ---
    fun generateSlides(prompt: String, deckType: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isBusy.value = true
            _errorBanner.value = null
            try {
                val deck = aiService.generateSlideDeck(prompt, deckType)
                _slidesDeck.value = deck
                val content = deck.second.joinToString("\n\n") { s ->
                    "الشريحة ${s.number}: ${s.title}\n" + s.bullets.joinToString("\n") { "- $it" }
                }
                repository.saveProject(
                    userId = currentUserId,
                    title = deck.first,
                    studioType = "SLIDES",
                    subType = deckType,
                    prompt = prompt,
                    content = content,
                    tracksSummary = "${deck.second.size} شرائح جاهزة للتصدير PDF/PPT",
                    status = "READY"
                )
                _statusMessage.value = "تم إعداد العرض التقديمي بنجاح."
            } catch (e: TimeoutCancellationException) {
                _errorBanner.value = "انتهت مهلة توليد الشرائح."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorBanner.value = e.localizedMessage ?: "تعذر إنشاء العرض التقديمي."
            } finally {
                _isBusy.value = false
            }
        }
    }

    // --- DOCS & WIDE RESEARCH ---
    fun runDocsOrResearch(topic: String, subType: String, useWideResearch: Boolean) {
        if (topic.isBlank()) return
        viewModelScope.launch {
            _isBusy.value = true
            _errorBanner.value = null
            try {
                if (useWideResearch) {
                    val outputs = aiService.runWideResearch(topic)
                    _wideResearchOutputs.value = outputs
                    val combined = outputs.joinToString("\n\n---\n\n") {
                        "### ${it.agentName}\n${it.findings}"
                    }
                    _studioGeneratedDoc.value = "تقرير البحث الموسع: ${topic.take(40)}" to combined
                    repository.saveProject(
                        userId = currentUserId,
                        title = "بحث موسع: ${topic.take(50)}",
                        studioType = "DOCS",
                        subType = "Wide Research",
                        prompt = topic,
                        content = combined,
                        tracksSummary = "${outputs.size} وكلاء متوازين",
                        status = "READY"
                    )
                } else {
                    val res = aiService.generateStudioArtifact(
                        "أنت خبير مستندات وجداول وتقارير في منصة الساحر. النوع المطلوب: $subType.\n" +
                        "الموضوع/البيانات: $topic\nأخرج مستنداً عربياً منظماً مع جداول markdown وخلاصة تنفيذية."
                    )
                    _wideResearchOutputs.value = emptyList()
                    _studioGeneratedDoc.value = "$subType: ${topic.take(40)}" to res
                    repository.saveProject(
                        userId = currentUserId,
                        title = "$subType: ${topic.take(50)}",
                        studioType = "DOCS",
                        subType = subType,
                        prompt = topic,
                        content = res,
                        tracksSummary = subType,
                        status = "READY"
                    )
                }
            } catch (e: TimeoutCancellationException) {
                _errorBanner.value = "انتهت مهلة البحث أو تحليل المستند."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorBanner.value = e.localizedMessage ?: "تعذر إنشاء التقرير."
            } finally {
                _isBusy.value = false
            }
        }
    }

    // --- WEBSITE / APP / GAME BUILDER ---
    fun generateBuilderProject(studioType: String, subType: String, prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isBusy.value = true
            _errorBanner.value = null
            try {
                val roleInstruction = when (studioType) {
                    "WEB" -> "صمم موقع ويب متكامل ($subType) مع الصفحات، واجهة المستخدم، تسجيل الدخول، قاعدة البيانات، ونظام المدفوعات، وأرفق بنية HTML/CSS أساسية."
                    "APP" -> "صمم تطبيق موبايل متكامل لنظامي iOS و Android ($subType) يتضمن شاشات الواجهة الأمامية (Frontend)، نقاط API الخلفية (Backend)، ومخطط قاعدة البيانات."
                    else -> "صمم لعبة متكاملة ($subType) تتضمن قواعد اللعب، تصميم العالم، الأسلوب الفني، المؤثرات الصوتية، والمستويات."
                }
                val res = aiService.generateStudioArtifact("$roleInstruction\nالفكرة: $prompt\nاكتب الرد منظماً بالعربية.")
                val title = "${when (studioType) { "WEB" -> "موقع"; "APP" -> "تطبيق"; else -> "لعبة" }}: ${prompt.take(45)}"
                _studioGeneratedDoc.value = title to res
                repository.saveProject(
                    userId = currentUserId,
                    title = title,
                    studioType = studioType,
                    subType = subType,
                    prompt = prompt,
                    content = res,
                    tracksSummary = subType,
                    status = "READY"
                )
                _statusMessage.value = "تم بناء المشروع وحفظه في مشاريعك."
            } catch (e: TimeoutCancellationException) {
                _errorBanner.value = "انتهت مهلة البناء."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorBanner.value = e.localizedMessage ?: "تعذر بناء المشروع."
            } finally {
                _isBusy.value = false
            }
        }
    }

    // --- CONNECTORS ---
    fun setConnectorConnection(item: ConnectorCatalogItem, connected: Boolean, accountLabel: String) {
        viewModelScope.launch {
            repository.setConnectorState(currentUserId, item, connected, accountLabel)
                .onSuccess {
                    _statusMessage.value = if (connected) "تم ربط ${item.name}" else "تم فصل ${item.name}"
                }
                .onFailure { e ->
                    _errorBanner.value = e.localizedMessage ?: "تعذر تحديث الموصل"
                }
        }
    }

    // --- REMOTE COMPUTER USE ---
    fun executeRemoteCommand(commandText: String) {
        if (commandText.isBlank()) return
        viewModelScope.launch {
            _isBusy.value = true
            _errorBanner.value = null
            try {
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val output = aiService.generateStudioArtifact(
                    "أنت وحدة التحكم في الحاسوب السحابي عن بُعد لمنصة الساحر. المستخدم طلب من هاتفه تنفيذ: \"$commandText\".\n" +
                    "اشرح خطوات التنفيذ الفعلية على سطح المكتب أو الطرفية ومخرجاتها بوضوح وإيجاز بالعربية."
                )
                _remoteLogs.value = _remoteLogs.value + RemoteLogEntry(
                    timestamp = timeStr,
                    command = commandText,
                    output = output,
                    status = "مكتمل"
                )
            } catch (e: TimeoutCancellationException) {
                _errorBanner.value = "انتهت مهلة أمر الحاسوب السحابي."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorBanner.value = e.localizedMessage ?: "تعذر تنفيذ الأمر عن بُعد."
            } finally {
                _isBusy.value = false
            }
        }
    }

    // --- AUTOMATIONS ---
    fun createAutomation(title: String, triggerType: String, triggerDetail: String, actionPrompt: String) {
        if (title.isBlank() || actionPrompt.isBlank()) return
        viewModelScope.launch {
            repository.createAutomation(currentUserId, title, triggerType, triggerDetail, actionPrompt)
                .onSuccess { _statusMessage.value = "تم إنشاء الأتمتة بنجاح." }
                .onFailure { _errorBanner.value = it.localizedMessage ?: "تعذر حفظ الأتمتة" }
        }
    }

    fun toggleAutomation(rule: AutomationRule) {
        viewModelScope.launch {
            repository.toggleAutomation(currentUserId, rule.id, !rule.active)
        }
    }

    fun triggerAutomationNow(rule: AutomationRule, appContext: Context) {
        viewModelScope.launch {
            repository.incrementAutomationRun(currentUserId, rule.id, rule.runsCount + 1L)
            _destination.value = WorkspaceDestination.CHAT_AGENT
            runGeneralAgent("تنفيذ أتمتة (${rule.title} - ${rule.triggerDetail}): ${rule.actionPrompt}", appContext)
        }
    }

    fun deleteAutomation(ruleId: String) {
        viewModelScope.launch {
            repository.deleteAutomation(currentUserId, ruleId)
        }
    }

    // --- PRICING ---
    fun selectPricingPlan(plan: String) {
        viewModelScope.launch {
            val newBalance = when (plan) {
                "PRO" -> 5000L
                "TEAM" -> 15000L
                else -> 1000L
            }
            repository.upgradePlan(currentUserId, plan, newBalance)
                .onSuccess {
                    _statusMessage.value = when (plan) {
                        "PRO" -> "تمت الترقية إلى باقة Pro ($20/شهر) — 5000 رصيد!"
                        "TEAM" -> "تمت الترقية إلى باقة Team ($49/شهر) — 15000 رصيد!"
                        else -> "تم تفعيل الباقة المجانية (1000 رصيد)"
                    }
                    _showPricingSheet.value = false
                }
                .onFailure { _errorBanner.value = it.localizedMessage ?: "تعذر تحديث الباقة" }
        }
    }
}
