package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PresentToAll
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AgentMode
import com.example.model.AgentTask
import com.example.model.ChatbotRole
import com.example.model.GeminiChatTier
import com.example.model.TaskMessage
import com.example.model.WorkspaceDestination
import com.example.util.mergeVoiceTranscript
import com.example.util.rememberVoiceInputController

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CenterChatPane(
    activeTask: AgentTask?,
    messages: List<TaskMessage>,
    streamingReply: String?,
    agentMode: AgentMode,
    isBusy: Boolean,
    selectedTier: GeminiChatTier = GeminiChatTier.GENERAL,
    selectedRole: ChatbotRole = ChatbotRole.GENERAL_ASSISTANT,
    searchGroundingEnabled: Boolean = true,
    mapsGroundingEnabled: Boolean = false,
    onSelectTier: (GeminiChatTier) -> Unit = {},
    onSelectRole: (ChatbotRole) -> Unit = {},
    onToggleSearchGrounding: () -> Unit = {},
    onToggleMapsGrounding: () -> Unit = {},
    onOpenLiveVoice: () -> Unit = {},
    onOpenAudioTranscribe: () -> Unit = {},
    externalInputAppend: Pair<Long, String>? = null,
    onConsumeExternalInput: () -> Unit = {},
    onModeChange: (AgentMode) -> Unit,
    onSendPrompt: (String) -> Unit,
    onPromoteToAgent: (String) -> Unit,
    onStopAgent: () -> Unit,
    onOpenStudio: (WorkspaceDestination) -> Unit,
    canExportChat: Boolean = false,
    onExportChat: (() -> Unit)? = null,
    onOpenStepsSheet: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val voiceController = rememberVoiceInputController { spokenText ->
        inputText = mergeVoiceTranscript(inputText, spokenText)
    }

    LaunchedEffect(externalInputAppend) {
        val pending = externalInputAppend
        if (pending != null && pending.second.isNotBlank()) {
            inputText = mergeVoiceTranscript(inputText, pending.second)
            onConsumeExternalInput()
        }
    }

    LaunchedEffect(messages.size, streamingReply) {
        val total = messages.size + (if (streamingReply != null) 1 else 0)
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .testTag("center_chat_column")
            ) {
            // Mode Switcher Bar + Compact Steps Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        RoundedCornerShape(50)
                    )
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        ModePill(
                            label = "الوكيل العام (Agent)",
                            selected = agentMode == AgentMode.AGENT,
                            testTag = "mode_agent_tab",
                            onClick = { onModeChange(AgentMode.AGENT) }
                        )
                        ModePill(
                            label = "دردشة مجانية",
                            selected = agentMode == AgentMode.CHAT,
                            testTag = "mode_chat_tab",
                            onClick = { onModeChange(AgentMode.CHAT) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (canExportChat && onExportChat != null) {
                        IconButton(
                            onClick = onExportChat,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("chat_pane_export_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FileDownload,
                                contentDescription = "تصدير المحادثة",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (onOpenStepsSheet != null && activeTask != null) {
                        Surface(
                            onClick = onOpenStepsSheet,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("compact_steps_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayCircle,
                                    contentDescription = "عرض الخطوات",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "الخطوات ${activeTask.currentStepIndex}/${activeTask.stepTitles.size.coerceAtLeast(1)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            if (agentMode == AgentMode.CHAT) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GeminiChatTier.entries.forEach { tier ->
                        FilterChip(
                            selected = selectedTier == tier,
                            onClick = { onSelectTier(tier) },
                            label = {
                                Text(
                                    text = tier.arabicLabel,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("tier_${tier.name.lowercase()}")
                        )
                    }
                    FilterChip(
                        selected = searchGroundingEnabled,
                        onClick = onToggleSearchGrounding,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "تفعيل بحث Google",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "بحث Google",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("search_grounding_toggle")
                    )
                    FilterChip(
                        selected = mapsGroundingEnabled,
                        onClick = onToggleMapsGrounding,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Place,
                                contentDescription = "تفعيل خرائط Google",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "خرائط Google",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("maps_grounding_toggle")
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChatbotRole.entries.forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { onSelectRole(role) },
                            label = {
                                Text(
                                    text = role.arabicLabel,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("role_${role.name.lowercase()}")
                        )
                    }
                }
            }

            // Messages or Welcome Empty State
            if (messages.isEmpty() && streamingReply == null) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 680.dp)
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    RoundedCornerShape(22.dp)
                                )
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_alsaher_hero),
                                contentDescription = "مساحة الساحر الإبداعية",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = if (agentMode == AgentMode.AGENT)
                                        "مرحباً بك في الوكيل الذكي الساحر"
                                    else
                                        "الدردشة الذكية المجانية",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (agentMode == AgentMode.AGENT)
                                        "اكتب فكرتك وسأخططها، أنفذها على الحاسوب السحابي، وأسلّمك النتيجة."
                                    else
                                        "اسأل أي شيء واحصل على إجابات فورية مجانية، أو انتقل لوضع الوكيل للتنفيذ.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier.widthIn(max = 680.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "أوامر سريعة للاستخدام الفوري",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuickStarterChip(
                                    icon = Icons.Filled.RocketLaunch,
                                    text = "خطط ونفذ حملة إطلاق منتج تقني متكاملة",
                                    onClick = { onSendPrompt("خطط ونفذ حملة إطلاق منتج تقني متكاملة مع جدول نشر ونصوص إعلانية") }
                                )
                                QuickStarterChip(
                                    icon = Icons.Filled.TravelExplore,
                                    text = "بحث موسع عن سوق التجارة الإلكترونية في الخليج",
                                    onClick = { onSendPrompt("قم ببحث موسع شامل حول فرص التجارة الإلكترونية بالذكاء الاصطناعي في الخليج 2026") }
                                )
                                QuickStarterChip(
                                    icon = Icons.Filled.Search,
                                    text = "بحث حي ببيانات Google",
                                    onClick = {
                                        onModeChange(AgentMode.CHAT)
                                        if (!searchGroundingEnabled) {
                                            onToggleSearchGrounding()
                                        }
                                        onSendPrompt("ما أبرز المستجدات التقنية والذكاء الاصطناعي اليوم؟")
                                    }
                                )
                                QuickStarterChip(
                                    icon = Icons.Filled.Place,
                                    text = "استكشاف أماكن عبر خرائط Google",
                                    onClick = {
                                        onModeChange(AgentMode.CHAT)
                                        if (!mapsGroundingEnabled) {
                                            onToggleMapsGrounding()
                                        }
                                        onSendPrompt("اقترح أفضل الأماكن السياحية والمطاعم المميزة في الرياض مع الروابط")
                                    }
                                )
                                QuickStarterChip(
                                    icon = Icons.Filled.RecordVoiceOver,
                                    text = "محادثة صوتية حية (Live)",
                                    onClick = onOpenLiveVoice
                                )
                                QuickStarterChip(
                                    icon = Icons.Filled.Audiotrack,
                                    text = "تسجيل وتفريغ صوتي (3.5)",
                                    onClick = onOpenAudioTranscribe
                                )
                                QuickStarterChip(
                                    icon = Icons.Filled.Image,
                                    text = "إنشاء وتعديل الصور",
                                    onClick = { onOpenStudio(WorkspaceDestination.IMAGE_STUDIO) }
                                )
                                QuickStarterChip(
                                    icon = Icons.Filled.Movie,
                                    text = "استوديو الفيديو الذكي",
                                    onClick = { onOpenStudio(WorkspaceDestination.VIDEO_STUDIO) }
                                )
                                QuickStarterChip(
                                    icon = Icons.Filled.PresentToAll,
                                    text = "إنشاء عرض تقديمي PDF",
                                    onClick = { onOpenStudio(WorkspaceDestination.SLIDES_STUDIO) }
                                )
                                QuickStarterChip(
                                    icon = Icons.Filled.Web,
                                    text = "بناء موقع أو متجر إلكتروني",
                                    onClick = { onOpenStudio(WorkspaceDestination.WEBSITE_BUILDER) }
                                )
                                QuickStarterChip(
                                    icon = Icons.Filled.SportsEsports,
                                    text = "صانع الألعاب القابلة للعب",
                                    onClick = { onOpenStudio(WorkspaceDestination.GAME_MAKER) }
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(
                            message = msg,
                            showPromoteButton = msg.role == "USER" && msg.mode == "CHAT",
                            onPromoteToAgent = { onPromoteToAgent(msg.content) }
                        )
                    }
                    if (streamingReply != null) {
                        item(key = "streaming_bubble") {
                            StreamingAssistantBubble(text = streamingReply)
                        }
                    }
                }
            }

            // Composer Bar with Voice Dictation
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(visible = voiceController.isListening) {
                        val pulseTransition = rememberInfiniteTransition(label = "voice_pulse")
                        val pulseScale by pulseTransition.animateFloat(
                            initialValue = 0.92f,
                            targetValue = 1.15f + (voiceController.rmsDb / 40f),
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 550),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "voice_pulse_scale"
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 10.dp)
                                .testTag("voice_listening_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .scale(pulseScale)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.GraphicEq,
                                            contentDescription = "جاري الاستماع",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "جاري الاستماع... تكلّم الآن (${voiceController.selectedLanguage.shortLabel})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (voiceController.partialTranscript.isNotBlank()) {
                                            Text(
                                                text = voiceController.partialTranscript,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    TextButton(
                                        onClick = voiceController.onCycleLanguage,
                                        modifier = Modifier.testTag("voice_language_toggle")
                                    ) {
                                        Text(
                                            text = voiceController.selectedLanguage.shortLabel,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    TextButton(
                                        onClick = voiceController.onStopListening,
                                        modifier = Modifier.testTag("voice_stop_listening_button")
                                    ) {
                                        Text(
                                            text = "إنهاء",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = !voiceController.errorMessage.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 8.dp)
                                .testTag("voice_error_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = voiceController.errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                if (voiceController.permissionDenied) {
                                    TextButton(
                                        onClick = {
                                            val settingsIntent = Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", context.packageName, null)
                                            )
                                            runCatching { context.startActivity(settingsIntent) }
                                        }
                                    ) {
                                        Text("الإعدادات", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                IconButton(
                                    onClick = voiceController.onClearError,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "إغلاق التنبيه",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 12.dp, end = 12.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = onOpenLiveVoice,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.RecordVoiceOver,
                                    contentDescription = "فتح المحادثة الصوتية الحية",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "محادثة حية (Live)",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("open_live_voice_button")
                        )
                        AssistChip(
                            onClick = onOpenAudioTranscribe,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Audiotrack,
                                    contentDescription = "فتح أداة التفريغ الصوتي",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "تفريغ صوتي (3.5)",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("open_audio_transcribe_button")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = if (voiceController.isListening)
                                        "تكلّم الآن وسيُكتب كلامك هنا..."
                                    else if (agentMode == AgentMode.AGENT)
                                        "صف المهمة أو استخدم الميكروفون للتحدث..."
                                    else
                                        "اسأل الساحر كتابةً أو عبر الميكروفون..."
                                )
                            },
                            maxLines = 4,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field")
                        )

                        IconButton(
                            onClick = voiceController.onToggleListening,
                            enabled = !isBusy,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    if (voiceController.isListening) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (voiceController.isListening) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = CircleShape
                                )
                                .testTag("voice_input_button")
                        ) {
                            Icon(
                                imageVector = if (voiceController.isListening) {
                                    Icons.Filled.GraphicEq
                                } else {
                                    Icons.Filled.Mic
                                },
                                contentDescription = if (voiceController.isListening) {
                                    "إيقاف التسجيل الصوتي"
                                } else {
                                    "التحدث بالميكروفون"
                                },
                                tint = if (voiceController.isListening) {
                                    MaterialTheme.colorScheme.onError
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        if (isBusy) {
                            IconButton(
                                onClick = onStopAgent,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .testTag("composer_stop_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Stop,
                                    contentDescription = "إيقاف التنفيذ",
                                    tint = MaterialTheme.colorScheme.onError
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (voiceController.isListening) {
                                        voiceController.onStopListening()
                                    }
                                    val text = inputText.trim()
                                    if (text.isNotEmpty()) {
                                        inputText = ""
                                        onSendPrompt(text)
                                    }
                                },
                                enabled = inputText.isNotBlank(),
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .size(50.dp)
                                    .testTag("send_prompt_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "إرسال",
                                    modifier = Modifier.size(20.dp)
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

@Composable
private fun ModePill(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        shape = RoundedCornerShape(50),
        modifier = Modifier.testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun QuickStarterChip(icon: ImageVector, text: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        },
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun ChatBubble(
    message: TaskMessage,
    showPromoteButton: Boolean,
    onPromoteToAgent: () -> Unit
) {
    val isUser = message.role == "USER"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            modifier = Modifier
                .widthIn(max = 580.dp)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    RoundedCornerShape(18.dp)
                )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isUser) Icons.Filled.Bolt else Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isUser) "أنت" else "الساحر",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                SelectionContainer {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (showPromoteButton) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = onPromoteToAgent,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("promote_to_agent_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RocketLaunch,
                            contentDescription = "تنفيذ بوضع الوكيل",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = "نفّذ هذا الطلب بوضع الوكيل",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingAssistantBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .widthIn(max = 580.dp)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    RoundedCornerShape(18.dp)
                )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "الساحر يكتب وينفّذ...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
