package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.model.AgentTask
import com.example.model.TaskStatus
import com.example.model.parseEnumSafe

@Composable
fun RightStepsPanel(
    activeTask: AgentTask?,
    isBusy: Boolean,
    notifyOnComplete: Boolean,
    onToggleNotify: (Boolean) -> Unit,
    onPauseResume: (AgentTask) -> Unit,
    onStop: (AgentTask?) -> Unit,
    onEditAndRerun: (AgentTask, String) -> Unit,
    modifier: Modifier = Modifier,
    applySystemBarsPadding: Boolean = true
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editedPrompt by remember(activeTask?.id, activeTask?.prompt) {
        mutableStateOf(activeTask?.prompt ?: "")
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onToggleNotify(granted)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val baseModifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
        val paddedModifier = if (applySystemBarsPadding) {
            baseModifier
                .statusBarsPadding()
                .navigationBarsPadding()
        } else {
            baseModifier
        }
        Column(
            modifier = paddedModifier.padding(14.dp)
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
                    Icon(
                        imageVector = Icons.Filled.Terminal,
                        contentDescription = "خطوات الوكيل",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "خطوات تنفيذ المهمة",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val status = parseEnumSafe(activeTask?.status, TaskStatus.COMPLETED)
                Surface(
                    color = when (status) {
                        TaskStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                        TaskStatus.RUNNING, TaskStatus.PLANNING -> MaterialTheme.colorScheme.primaryContainer
                        TaskStatus.PAUSED -> MaterialTheme.colorScheme.secondaryContainer
                        TaskStatus.STOPPED, TaskStatus.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (activeTask == null) "جاهز" else status.arabicLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cloud Computer Background Execution Toggle
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "إشعار عند الاكتمال",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "العمل السحابي في الخلفية",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "أغلق التطبيق وسيصلك إشعار عند الإنجاز",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = notifyOnComplete,
                        onCheckedChange = { checked ->
                            if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onToggleNotify(checked)
                            }
                        },
                        modifier = Modifier.testTag("background_notify_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeTask == null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "الحاسوب السحابي جاهز",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "اكتب أي طلب في المحادثة ليقوم الساحر بتقسيمه إلى خطوات وتنفيذه خطوة بخطوة هنا.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val steps = activeTask.stepTitles.ifEmpty {
                        listOf("تحليل الطلب", "التنفيذ على الحاسوب السحابي", "تسليم النتيجة")
                    }
                    val total = steps.size.coerceAtLeast(1)
                    val completedCount = activeTask.currentStepIndex.toInt().coerceIn(0, total)
                    val progress = completedCount.toFloat() / total.toFloat()

                    LinearProgressIndicator(
                        progress = { progress },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )

                    Text(
                        text = "التقدم: $completedCount من $total خطوات",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    steps.forEachIndexed { idx, stepLabel ->
                        val isDone = idx < completedCount
                        val isCurrent = idx == completedCount &&
                            (activeTask.status == "RUNNING" || activeTask.status == "PLANNING")
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                when {
                                    isDone -> Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "خطوة مكتملة",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    isCurrent -> CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                    else -> Icon(
                                        imageVector = Icons.Filled.HourglassEmpty,
                                        contentDescription = "في الانتظار",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "خطوة ${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stepLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Control buttons: Stop, Pause/Resume, Edit Request at any time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isBusy || activeTask.status == "RUNNING" || activeTask.status == "PAUSED") {
                            OutlinedButton(
                                onClick = { onPauseResume(activeTask) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pause_resume_task_button")
                            ) {
                                Icon(
                                    imageVector = if (activeTask.status == "PAUSED") Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                    contentDescription = "إيقاف مؤقت",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    text = if (activeTask.status == "PAUSED") "استئناف" else "مؤقت",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Button(
                                onClick = { onStop(activeTask) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("stop_task_button")
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = "إيقاف", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("إيقاف", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                editedPrompt = activeTask.prompt
                                showEditDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("edit_task_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "تعديل الطلب", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("تعديل", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (activeTask.deliverableContent.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                                    RoundedCornerShape(14.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "المخرج الجاهز: ${activeTask.deliverableTitle}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = activeTask.deliverableContent,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && activeTask != null) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("تعديل الطلب وإعادة التنفيذ") },
                text = {
                    OutlinedTextField(
                        value = editedPrompt,
                        onValueChange = { editedPrompt = it },
                        label = { Text("الطلب المعدّل") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_task_prompt_input")
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showEditDialog = false
                            onEditAndRerun(activeTask, editedPrompt)
                        },
                        modifier = Modifier.testTag("confirm_edit_task_button")
                    ) {
                        Text("تحديث وتشغيل")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}
