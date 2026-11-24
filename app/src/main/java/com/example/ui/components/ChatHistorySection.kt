package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.model.AgentMode
import com.example.model.AgentTask
import com.example.model.TaskStatus
import com.example.model.WorkspaceDestination
import com.example.model.parseEnumSafe
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val COLLAPSED_HISTORY_LIMIT = 5

enum class ChatHistoryFilter(val arabicLabel: String) {
    ALL("الكل"),
    AGENT("الوكيل"),
    CHAT("الدردشة")
}

fun filterChatHistory(
    tasks: List<AgentTask>,
    query: String,
    filter: ChatHistoryFilter
): List<AgentTask> {
    val normalizedQuery = query.trim().lowercase()
    return tasks.filter { task ->
        val matchesFilter = when (filter) {
            ChatHistoryFilter.ALL -> true
            ChatHistoryFilter.AGENT -> !task.mode.equals(AgentMode.CHAT.name, ignoreCase = true)
            ChatHistoryFilter.CHAT -> task.mode.equals(AgentMode.CHAT.name, ignoreCase = true)
        }
        val matchesQuery = normalizedQuery.isEmpty() ||
            task.title.lowercase().contains(normalizedQuery) ||
            task.prompt.lowercase().contains(normalizedQuery)
        matchesFilter && matchesQuery
    }
}

private fun formatHistoryTimestamp(date: Date?, nowMillis: Long = System.currentTimeMillis()): String {
    if (date == null) return "الآن"
    val locale = Locale.forLanguageTag("ar")
    val targetCal = Calendar.getInstance().apply { time = date }
    val todayCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val yesterdayCal = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }

    val timeFmt = SimpleDateFormat("HH:mm", locale)
    return when {
        targetCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            targetCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) ->
            "اليوم • ${timeFmt.format(date)}"

        targetCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
            targetCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR) ->
            "أمس • ${timeFmt.format(date)}"

        else -> SimpleDateFormat("d MMM", locale).format(date)
    }
}

@Composable
fun ChatHistorySection(
    tasks: List<AgentTask>,
    activeTaskId: String?,
    currentDestination: WorkspaceDestination,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onExportHistory: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(ChatHistoryFilter.ALL) }
    var showAll by rememberSaveable { mutableStateOf(false) }

    val filteredTasks = remember(tasks, searchQuery, selectedFilter) {
        filterChatHistory(tasks, searchQuery, selectedFilter)
    }
    val visibleTasks = if (showAll) filteredTasks else filteredTasks.take(COLLAPSED_HISTORY_LIMIT)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .testTag("chat_history_section"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row with title + total count pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "سجل المحادثات",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (tasks.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (onExportHistory != null) {
                            IconButton(
                                onClick = onExportHistory,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("export_chat_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FileDownload,
                                    contentDescription = "تصدير سجل المحادثات",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "${tasks.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (tasks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "لا توجد محادثات سابقة بعد. ابدأ محادثة أو مهمة جديدة لتظهر هنا.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Search box for quick access to previous conversations
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "ابحث في المحادثات السابقة...",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "بحث",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("clear_chat_history_search")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "مسح البحث",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_history_search_input")
                )

                // Mode filter chips (All / Agent / Free Chat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChatHistoryFilter.entries.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            onClick = { selectedFilter = filter },
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    },
                                    shape = RoundedCornerShape(50)
                                )
                                .testTag("history_filter_${filter.name.lowercase()}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = filter.arabicLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (filteredTasks.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "لا توجد محادثات مطابقة لبحثك",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedFilter = ChatHistoryFilter.ALL
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("عرض كل المحادثات", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                } else {
                    visibleTasks.forEach { task ->
                        val isSelected = task.id == activeTaskId &&
                            currentDestination == WorkspaceDestination.CHAT_AGENT
                        ChatHistoryItemRow(
                            task = task,
                            selected = isSelected,
                            onSelect = { onSelectConversation(task.id) },
                            onDelete = { onDeleteConversation(task.id) }
                        )
                    }

                    AnimatedVisibility(visible = filteredTasks.size > COLLAPSED_HISTORY_LIMIT) {
                        TextButton(
                            onClick = { showAll = !showAll },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("chat_history_expand_button")
                        ) {
                            Icon(
                                imageVector = if (showAll) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = if (showAll) {
                                    "إخفاء المحادثات الأقدم"
                                } else {
                                    "عرض جميع المحادثات (${filteredTasks.size})"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatHistoryItemRow(
    task: AgentTask,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val isFreeChat = task.mode.equals(AgentMode.CHAT.name, ignoreCase = true)
    val status = parseEnumSafe(task.status, TaskStatus.COMPLETED)
    val statusDotColor = when (status) {
        TaskStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
        TaskStatus.RUNNING, TaskStatus.PLANNING -> MaterialTheme.colorScheme.primary
        TaskStatus.PAUSED -> MaterialTheme.colorScheme.secondary
        TaskStatus.STOPPED, TaskStatus.ERROR -> MaterialTheme.colorScheme.error
    }
    val timeLabel = remember(task.id, task.createdAt) {
        formatHistoryTimestamp(task.createdAt?.toDate())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
            .border(
                width = 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                },
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onSelect)
            .padding(start = 10.dp, end = 2.dp, top = 6.dp, bottom = 6.dp)
            .testTag("chat_history_item_${task.id}"),
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
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isFreeChat) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFreeChat) {
                        Icons.AutoMirrored.Filled.Chat
                    } else {
                        Icons.Filled.AutoAwesome
                    },
                    contentDescription = if (isFreeChat) "محادثة مجانية" else "مهمة وكيل",
                    tint = if (isFreeChat) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = task.title.ifBlank { "محادثة جديدة" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Text(
                        text = if (isFreeChat) "دردشة" else status.arabicLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(48.dp)
                .testTag("delete_history_item_${task.id}")
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "حذف المحادثة",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
