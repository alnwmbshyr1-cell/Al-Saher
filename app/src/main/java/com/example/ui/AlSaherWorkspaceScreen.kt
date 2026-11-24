package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.firebase.signOutUser
import com.example.model.AccountPreferences
import com.example.model.AudioTranscribeUiState
import com.example.model.ChatbotRole
import com.example.model.GeminiChatTier
import com.example.model.ImageGenUiState
import com.example.model.LiveVoiceUiState
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.WorkspaceDestination
import com.example.ui.components.AudioTranscribeDialog
import com.example.ui.components.CenterChatPane
import com.example.ui.components.ChatExportDialog
import com.example.ui.components.LeftSidebar
import com.example.ui.components.LiveVoiceDialog
import com.example.ui.components.RightStepsPanel
import com.example.ui.screens.AutomationsScreen
import com.example.ui.screens.BuilderStudioScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ConnectorsScreen
import com.example.ui.screens.DocsResearchStudioScreen
import com.example.ui.screens.ImageStudioScreen
import com.example.ui.screens.PricingDialog
import com.example.ui.screens.RemoteControlScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SlidesStudioScreen
import com.example.ui.screens.UsageDashboardScreen
import com.example.ui.screens.VideoStudioScreen
import com.example.util.ChatExportFormat
import com.example.util.ChatExportHelper
import com.example.util.ChatExportScope
import com.example.viewmodel.AlSaherViewModel
import kotlinx.coroutines.launch

@Composable
fun AlSaherWorkspaceScreen(
    viewModel: AlSaherViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val agentMode by viewModel.agentMode.collectAsStateWithLifecycle()
    val activeTaskId by viewModel.activeTaskId.collectAsStateWithLifecycle()
    val streamingReply by viewModel.streamingReply.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val errorBanner by viewModel.errorBanner.collectAsStateWithLifecycle()
    val notifyOnComplete by viewModel.notifyOnComplete.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val showPricingSheet by viewModel.showPricingSheet.collectAsStateWithLifecycle()
    val chatTier by viewModel.chatTier.collectAsStateWithLifecycle()
    val chatbotRole by viewModel.chatbotRole.collectAsStateWithLifecycle()
    val searchGroundingEnabled by viewModel.searchGroundingEnabled.collectAsStateWithLifecycle()
    val mapsGroundingEnabled by viewModel.mapsGroundingEnabled.collectAsStateWithLifecycle()
    val imageStudioState by viewModel.imageStudioState.collectAsStateWithLifecycle()
    val liveVoiceState by viewModel.liveVoiceState.collectAsStateWithLifecycle()
    val audioTranscribeState by viewModel.audioTranscribeState.collectAsStateWithLifecycle()
    val accountPreferences by viewModel.accountPreferences.collectAsStateWithLifecycle()
    val isSavingProfile by viewModel.isSavingProfile.collectAsStateWithLifecycle()

    val userProfileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val tasksState by viewModel.tasks.collectAsStateWithLifecycle()
    val messagesState by viewModel.activeMessages.collectAsStateWithLifecycle()
    val projectsState by viewModel.projects.collectAsStateWithLifecycle()
    val automationsState by viewModel.automations.collectAsStateWithLifecycle()
    val connectorsState by viewModel.connectors.collectAsStateWithLifecycle()

    val videoTimeline by viewModel.videoTimeline.collectAsStateWithLifecycle()
    val slidesDeck by viewModel.slidesDeck.collectAsStateWithLifecycle()
    val wideResearchOutputs by viewModel.wideResearchOutputs.collectAsStateWithLifecycle()
    val studioGeneratedDoc by viewModel.studioGeneratedDoc.collectAsStateWithLifecycle()
    val remoteLogs by viewModel.remoteLogs.collectAsStateWithLifecycle()

    val userProfile = (userProfileState as? UiState.Success)?.data
    val tasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    val projects = (projectsState as? UiState.Success)?.data.orEmpty()
    val automations = (automationsState as? UiState.Success)?.data.orEmpty()
    val connectors = (connectorsState as? UiState.Success)?.data.orEmpty()

    val activeTask = remember(tasks, activeTaskId) {
        tasks.firstOrNull { it.id == activeTaskId }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage, errorBanner) {
        val msg = errorBanner ?: statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showExportDialog by remember { mutableStateOf(false) }
    var showLiveVoiceDialog by remember { mutableStateOf(false) }
    var showAudioTranscribeDialog by remember { mutableStateOf(false) }
    var pendingTranscriptToInsert by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var pendingExportScope by remember { mutableStateOf(ChatExportScope.ALL) }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ChatExportFormat.JSON.mimeType)
    ) { uri ->
        if (uri != null) {
            viewModel.exportChatHistoryToUri(
                appContext = context,
                uri = uri,
                format = ChatExportFormat.JSON,
                scope = pendingExportScope
            )
        }
    }

    val textExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ChatExportFormat.TEXT.mimeType)
    ) { uri ->
        if (uri != null) {
            viewModel.exportChatHistoryToUri(
                appContext = context,
                uri = uri,
                format = ChatExportFormat.TEXT,
                scope = pendingExportScope
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 760.dp
        val rightSurfaceWidth = if (maxWidth >= 1024.dp) 320.dp else 280.dp
        var showRightStepsPanel by remember(isWideScreen) { mutableStateOf(isWideScreen) }

        // BackHandler priority (last enabled wins): Drawer -> Compact Right Surface -> Secondary Destination
        BackHandler(enabled = destination != WorkspaceDestination.CHAT_AGENT) {
            viewModel.setDestination(WorkspaceDestination.CHAT_AGENT)
        }
        BackHandler(enabled = showRightStepsPanel && !isWideScreen) {
            showRightStepsPanel = false
        }
        BackHandler(enabled = drawerState.isOpen) {
            scope.launch { drawerState.close() }
        }

        // Keep outer positional container LTR so Left Drawer opens from the LEFT and Task Status Surface aligns RIGHT,
        // while every UI component inside renders in full Arabic RTL.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier
                            .width(316.dp)
                            .testTag("left_sidebar_drawer"),
                        drawerContainerColor = MaterialTheme.colorScheme.surface
                    ) {
                        LeftSidebar(
                            currentDestination = destination,
                            tasks = tasks,
                            activeTaskId = activeTaskId,
                            userProfile = userProfile,
                            isDarkTheme = isDarkTheme,
                            onSelectDestination = {
                                viewModel.setDestination(it)
                                scope.launch { drawerState.close() }
                            },
                            onNewTask = {
                                viewModel.startNewSession()
                                scope.launch { drawerState.close() }
                            },
                            onSelectTask = {
                                viewModel.selectTask(it)
                                scope.launch { drawerState.close() }
                            },
                            onDeleteTask = { viewModel.deleteTask(it) },
                            onOpenPricing = {
                                scope.launch { drawerState.close() }
                                viewModel.setShowPricingSheet(true)
                            },
                            onSetDarkTheme = { viewModel.setDarkTheme(it) },
                            onExportChatHistory = {
                                scope.launch { drawerState.close() }
                                showExportDialog = true
                            },
                            onSignOut = {
                                scope.launch { drawerState.close() }
                                signOutUser(credentialManager, scope)
                            }
                        )
                    }
                }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        snackbarHost = {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                SnackbarHost(snackbarHostState)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                            ) {
                                // Physical LTR Top Bar: Left Drawer button | Center Arabic RTL Title | Right Task Status button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("open_sidebar_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Menu,
                                            contentDescription = "فتح القائمة الجانبية اليسرى"
                                        )
                                    }

                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                text = "الساحر • ${destination.arabicTitle}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                onClick = { viewModel.setShowPricingSheet(true) },
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Bolt,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "${userProfile?.creditsBalance ?: 1000}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { showRightStepsPanel = !showRightStepsPanel },
                                        modifier = Modifier.testTag("open_steps_panel_button")
                                    ) {
                                        Box(contentAlignment = Alignment.TopEnd) {
                                            Icon(
                                                imageVector = Icons.Filled.Terminal,
                                                contentDescription = "عرض أو إخفاء تحديثات حالة المهمة",
                                                tint = if (showRightStepsPanel) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                            if (isBusy || activeTask != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isBusy) MaterialTheme.colorScheme.tertiary
                                                            else MaterialTheme.colorScheme.primary
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .navigationBarsPadding()
                        ) {
                            // Centered Column for Chat & Studio Content (RTL inside)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 860.dp)
                                        .fillMaxSize()
                                ) {
                                    CenterContentRouter(
                                        destination = destination,
                                        viewModel = viewModel,
                                        activeTask = activeTask,
                                        tasks = tasks,
                                        messages = messages,
                                        streamingReply = streamingReply,
                                        agentMode = agentMode,
                                        isBusy = isBusy,
                                        chatTier = chatTier,
                                        chatbotRole = chatbotRole,
                                        searchGroundingEnabled = searchGroundingEnabled,
                                        mapsGroundingEnabled = mapsGroundingEnabled,
                                        onOpenLiveVoice = { showLiveVoiceDialog = true },
                                        onOpenAudioTranscribe = {
                                            viewModel.resetAudioTranscribeState()
                                            showAudioTranscribeDialog = true
                                        },
                                        externalInputAppend = pendingTranscriptToInsert,
                                        onConsumeExternalInput = { pendingTranscriptToInsert = null },
                                        imageStudioState = imageStudioState,
                                        projects = projects,
                                        automations = automations,
                                        connectors = connectors,
                                        videoTimeline = videoTimeline,
                                        slidesDeck = slidesDeck,
                                        wideResearchOutputs = wideResearchOutputs,
                                        studioGeneratedDoc = studioGeneratedDoc,
                                        remoteLogs = remoteLogs,
                                        canExportChat = tasks.isNotEmpty(),
                                        onExportChat = { showExportDialog = true },
                                        userProfile = userProfile,
                                        accountPreferences = accountPreferences,
                                        isSavingProfile = isSavingProfile,
                                        onSignOut = { signOutUser(credentialManager, scope) },
                                        onOpenStepsSheet = if (!isWideScreen) {
                                            { showRightStepsPanel = true }
                                        } else null
                                    )
                                }
                            }

                            // Right-aligned Surface for Task Status Updates (docked on wide screens)
                            if (isWideScreen) {
                                AnimatedVisibility(
                                    visible = showRightStepsPanel,
                                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                                ) {
                                    Surface(
                                        tonalElevation = 4.dp,
                                        shadowElevation = 6.dp,
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                                        modifier = Modifier
                                            .width(rightSurfaceWidth)
                                            .fillMaxHeight()
                                            .testTag("right_task_status_surface")
                                    ) {
                                        RightStepsPanel(
                                            activeTask = activeTask,
                                            isBusy = isBusy,
                                            notifyOnComplete = notifyOnComplete,
                                            onToggleNotify = { viewModel.setNotifyOnComplete(it) },
                                            onPauseResume = { viewModel.togglePauseTask(it) },
                                            onStop = { viewModel.stopActiveAgent(it) },
                                            onEditAndRerun = { t, p -> viewModel.editAndRerunTask(t, p, context) },
                                            applySystemBarsPadding = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Compact screens: Scrim + Right-aligned Surface overlay for Task Status Updates
                    if (!isWideScreen) {
                        AnimatedVisibility(
                            visible = showRightStepsPanel,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.52f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { showRightStepsPanel = false }
                                    )
                            )
                        }

                        AnimatedVisibility(
                            visible = showRightStepsPanel,
                            enter = slideInHorizontally(initialOffsetX = { it }),
                            exit = slideOutHorizontally(targetOffsetX = { it }),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Surface(
                                tonalElevation = 8.dp,
                                shadowElevation = 12.dp,
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                                modifier = Modifier
                                    .width(310.dp)
                                    .fillMaxHeight()
                                    .testTag("right_task_status_surface")
                            ) {
                                RightStepsPanel(
                                    activeTask = activeTask,
                                    isBusy = isBusy,
                                    notifyOnComplete = notifyOnComplete,
                                    onToggleNotify = { viewModel.setNotifyOnComplete(it) },
                                    onPauseResume = { viewModel.togglePauseTask(it) },
                                    onStop = { viewModel.stopActiveAgent(it) },
                                    onEditAndRerun = { t, p -> viewModel.editAndRerunTask(t, p, context) },
                                    applySystemBarsPadding = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPricingSheet) {
        PricingDialog(
            currentPlan = userProfile?.plan ?: "FREE",
            creditsBalance = userProfile?.creditsBalance ?: 1000L,
            onSelectPlan = { viewModel.selectPricingPlan(it) },
            onDismiss = { viewModel.setShowPricingSheet(false) }
        )
    }

    if (showExportDialog) {
        ChatExportDialog(
            hasActiveConversation = activeTask != null,
            totalConversationsCount = tasks.size,
            onDismiss = { showExportDialog = false },
            onConfirmExport = { chosenScope, chosenFormat ->
                pendingExportScope = chosenScope
                showExportDialog = false
                val fileName = ChatExportHelper.buildSuggestedFileName(chosenFormat, chosenScope)
                when (chosenFormat) {
                    ChatExportFormat.JSON -> jsonExportLauncher.launch(fileName)
                    ChatExportFormat.TEXT -> textExportLauncher.launch(fileName)
                }
            }
        )
    }

    if (showLiveVoiceDialog) {
        LiveVoiceDialog(
            liveState = liveVoiceState,
            onStartLive = { viewModel.startLiveVoiceConversation() },
            onStopLive = { viewModel.stopLiveVoiceConversation() },
            onDismiss = {
                viewModel.stopLiveVoiceConversation()
                showLiveVoiceDialog = false
            }
        )
    }

    if (showAudioTranscribeDialog) {
        AudioTranscribeDialog(
            transcribeState = audioTranscribeState,
            onTranscribeAudio = { bytes -> viewModel.transcribeAudioRecording(bytes) },
            onInsertTranscript = { transcript ->
                pendingTranscriptToInsert = System.currentTimeMillis() to transcript
                viewModel.setDestination(WorkspaceDestination.CHAT_AGENT)
                viewModel.resetAudioTranscribeState()
            },
            onReset = { viewModel.resetAudioTranscribeState() },
            onDismiss = {
                viewModel.resetAudioTranscribeState()
                showAudioTranscribeDialog = false
            }
        )
    }
}

@Composable
private fun CenterContentRouter(
    destination: WorkspaceDestination,
    viewModel: AlSaherViewModel,
    activeTask: com.example.model.AgentTask?,
    tasks: List<com.example.model.AgentTask>,
    messages: List<com.example.model.TaskMessage>,
    streamingReply: String?,
    agentMode: com.example.model.AgentMode,
    isBusy: Boolean,
    chatTier: GeminiChatTier,
    chatbotRole: ChatbotRole,
    searchGroundingEnabled: Boolean,
    mapsGroundingEnabled: Boolean,
    onOpenLiveVoice: () -> Unit,
    onOpenAudioTranscribe: () -> Unit,
    externalInputAppend: Pair<Long, String>?,
    onConsumeExternalInput: () -> Unit,
    imageStudioState: ImageGenUiState,
    projects: List<com.example.model.StudioProject>,
    automations: List<com.example.model.AutomationRule>,
    connectors: List<com.example.model.ConnectorState>,
    videoTimeline: com.example.model.VideoTimelineModel?,
    slidesDeck: Pair<String, List<com.example.model.SlideCardModel>>?,
    wideResearchOutputs: List<com.example.model.WideResearchAgentOutput>,
    studioGeneratedDoc: Pair<String, String>?,
    remoteLogs: List<com.example.model.RemoteLogEntry>,
    canExportChat: Boolean,
    onExportChat: () -> Unit,
    userProfile: UserProfile?,
    accountPreferences: AccountPreferences,
    isSavingProfile: Boolean,
    onSignOut: () -> Unit,
    onOpenStepsSheet: (() -> Unit)?
) {
    val context = LocalContext.current
    when (destination) {
        WorkspaceDestination.CHAT_AGENT -> CenterChatPane(
            activeTask = activeTask,
            messages = messages,
            streamingReply = streamingReply,
            agentMode = agentMode,
            isBusy = isBusy,
            selectedTier = chatTier,
            selectedRole = chatbotRole,
            searchGroundingEnabled = searchGroundingEnabled,
            mapsGroundingEnabled = mapsGroundingEnabled,
            onSelectTier = { viewModel.setChatTier(it) },
            onSelectRole = { viewModel.setChatbotRole(it) },
            onToggleSearchGrounding = { viewModel.toggleSearchGrounding() },
            onToggleMapsGrounding = { viewModel.toggleMapsGrounding() },
            onOpenLiveVoice = onOpenLiveVoice,
            onOpenAudioTranscribe = onOpenAudioTranscribe,
            externalInputAppend = externalInputAppend,
            onConsumeExternalInput = onConsumeExternalInput,
            onModeChange = { viewModel.setAgentMode(it) },
            onSendPrompt = { viewModel.sendPrompt(it, context) },
            onPromoteToAgent = { viewModel.promoteChatToAgent(it, context) },
            onStopAgent = { viewModel.stopActiveAgent(activeTask) },
            onOpenStudio = { viewModel.setDestination(it) },
            canExportChat = canExportChat,
            onExportChat = onExportChat,
            onOpenStepsSheet = onOpenStepsSheet
        )
        WorkspaceDestination.DIRECT_AI_CHAT -> ChatScreen()
        WorkspaceDestination.IMAGE_STUDIO -> ImageStudioScreen(
            imageState = imageStudioState,
            savedProjects = projects,
            onGenerateOrEdit = { prompt, bmp, aspect -> viewModel.generateOrEditImage(prompt, bmp, aspect) }
        )
        WorkspaceDestination.VIDEO_STUDIO -> VideoStudioScreen(
            isBusy = isBusy,
            timeline = videoTimeline,
            savedProjects = projects,
            onGenerateVideo = { p, t, m, bmp -> viewModel.generateVideo(p, t, m, bmp) },
            onUpdateClip = { id, dur, cap -> viewModel.updateVideoClip(id, dur, cap) },
            onSwapMusic = { viewModel.swapVideoMusic(it) },
            onAddCustomClip = { viewModel.addCustomClipToTimeline(it) },
            onSaveVideoEdits = { viewModel.saveCurrentVideoEdits() },
            onLoadProject = { viewModel.loadProjectIntoStudio(it) }
        )
        WorkspaceDestination.SLIDES_STUDIO -> SlidesStudioScreen(
            isBusy = isBusy,
            slidesDeck = slidesDeck,
            savedProjects = projects,
            onGenerateSlides = { p, t -> viewModel.generateSlides(p, t) }
        )
        WorkspaceDestination.DOCS_RESEARCH -> DocsResearchStudioScreen(
            isBusy = isBusy,
            wideOutputs = wideResearchOutputs,
            docResult = studioGeneratedDoc,
            savedProjects = projects,
            onRunDocsOrResearch = { topic, sub, wide -> viewModel.runDocsOrResearch(topic, sub, wide) },
            onLoadProject = { viewModel.loadProjectIntoStudio(it) }
        )
        WorkspaceDestination.WEBSITE_BUILDER -> BuilderStudioScreen(
            studioType = "WEB",
            isBusy = isBusy,
            generatedDoc = studioGeneratedDoc,
            savedProjects = projects,
            onGenerate = { st, sub, p -> viewModel.generateBuilderProject(st, sub, p) },
            onLoadProject = { viewModel.loadProjectIntoStudio(it) }
        )
        WorkspaceDestination.APP_BUILDER -> BuilderStudioScreen(
            studioType = "APP",
            isBusy = isBusy,
            generatedDoc = studioGeneratedDoc,
            savedProjects = projects,
            onGenerate = { st, sub, p -> viewModel.generateBuilderProject(st, sub, p) },
            onLoadProject = { viewModel.loadProjectIntoStudio(it) }
        )
        WorkspaceDestination.GAME_MAKER -> BuilderStudioScreen(
            studioType = "GAME",
            isBusy = isBusy,
            generatedDoc = studioGeneratedDoc,
            savedProjects = projects,
            onGenerate = { st, sub, p -> viewModel.generateBuilderProject(st, sub, p) },
            onLoadProject = { viewModel.loadProjectIntoStudio(it) }
        )
        WorkspaceDestination.CONNECTORS -> ConnectorsScreen(
            connectors = connectors,
            onToggleConnector = { item, conn, label -> viewModel.setConnectorConnection(item, conn, label) }
        )
        WorkspaceDestination.REMOTE_CONTROL -> RemoteControlScreen(
            isBusy = isBusy,
            logs = remoteLogs,
            onExecuteCommand = { viewModel.executeRemoteCommand(it) }
        )
        WorkspaceDestination.AUTOMATIONS -> AutomationsScreen(
            automations = automations,
            onCreateAutomation = { t, type, det, act -> viewModel.createAutomation(t, type, det, act) },
            onToggleAutomation = { viewModel.toggleAutomation(it) },
            onTriggerNow = { viewModel.triggerAutomationNow(it, context) },
            onDeleteAutomation = { viewModel.deleteAutomation(it) }
        )
        WorkspaceDestination.USAGE_DASHBOARD -> UsageDashboardScreen(
            tasks = tasks,
            userProfile = userProfile,
            onOpenPricing = { viewModel.setShowPricingSheet(true) }
        )
        WorkspaceDestination.SETTINGS -> SettingsScreen(
            userProfile = userProfile,
            preferences = accountPreferences,
            isSavingProfile = isSavingProfile,
            canExportHistory = canExportChat,
            onSaveProfile = { name, email -> viewModel.updateUserProfile(name, email) },
            onSetDarkTheme = { viewModel.setDarkTheme(it) },
            onSetNotifyOnComplete = { viewModel.setNotifyOnComplete(it) },
            onSetDefaultAgentMode = { viewModel.setDefaultAgentModePreference(it) },
            onSetDefaultChatTier = { viewModel.setChatTier(it) },
            onSetDefaultChatbotRole = { viewModel.setChatbotRole(it) },
            onSetDefaultSearchGrounding = { viewModel.setDefaultSearchGrounding(it) },
            onOpenPricing = { viewModel.setShowPricingSheet(true) },
            onExportHistory = onExportChat,
            onSignOut = onSignOut
        )
    }
}
