package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.model.AutomationRule
import com.example.model.ConnectorCatalogItem
import com.example.model.ConnectorState
import com.example.model.RemoteLogEntry
import com.example.viewmodel.OFFICIAL_CONNECTORS

@Composable
fun ConnectorsScreen(
    connectors: List<ConnectorState>,
    onToggleConnector: (ConnectorCatalogItem, Boolean, String) -> Unit
) {
    var editingItem by remember { mutableStateOf<ConnectorCatalogItem?>(null) }
    var accountInput by remember { mutableStateOf("") }

    val stateByKey = remember(connectors) { connectors.associateBy { it.connectorKey } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("الموصلات والتكاملات الرسمية", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "اربط حساباتك مع الساحر ليعمل الوكيل مباشرة عبر البريد والملفات والتقويم وأدوات الفريق.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OFFICIAL_CONNECTORS.forEach { catalog ->
                val state = stateByKey[catalog.key]
                val isConnected = state?.connected == true
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isConnected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(catalog.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = catalog.arabicCategory,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = catalog.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isConnected && !state?.accountLabel.isNullOrBlank()) {
                                Text(
                                    text = "متصل: ${state?.accountLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        if (isConnected) {
                            OutlinedButton(
                                onClick = { onToggleConnector(catalog, false, "") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("disconnect_${catalog.key}")
                            ) {
                                Text("فصل")
                            }
                        } else {
                            Button(
                                onClick = {
                                    editingItem = catalog
                                    accountInput = "user@${catalog.key}.com"
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("connect_${catalog.key}")
                            ) {
                                Text("ربط")
                            }
                        }
                    }
                }
            }
        }
    }

    editingItem?.let { item ->
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { editingItem = null },
                title = { Text("ربط ${item.name}") },
                text = {
                    OutlinedTextField(
                        value = accountInput,
                        onValueChange = { accountInput = it },
                        label = { Text("معرّف الحساب أو مساحة العمل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onToggleConnector(item, true, accountInput.ifBlank { item.name })
                            editingItem = null
                        }
                    ) { Text("تأكيد الربط") }
                },
                dismissButton = {
                    TextButton(onClick = { editingItem = null }) { Text("إلغاء") }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemoteControlScreen(
    isBusy: Boolean,
    logs: List<RemoteLogEntry>,
    onExecuteCommand: (String) -> Unit
) {
    var command by rememberSaveable { mutableStateOf("") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("التحكم عن بُعد واستخدام الحاسوب", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "تحكم في حاسوبك أو الحاسوب السحابي مباشرة من هاتفك: ابحث عن الملفات، شغّل التطبيقات، وأتمّ المهام وشاهد التنفيذ حياً.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Virtual Desktop Live Monitor
            Surface(
                color = Color(0xFF060A13),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Text("جلسة سطح المكتب السحابي — بث حي", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                        Icon(Icons.Filled.Terminal, contentDescription = null, tint = Color(0xFF22D3EE), modifier = Modifier.size(18.dp))
                    }

                    logs.takeLast(6).forEach { entry ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF111827))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "[${entry.timestamp}] $ ${entry.command}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF22D3EE)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = entry.output,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(
                    onClick = { onExecuteCommand("ابحث عن ملفات التقرير المالي 2026 على سطح المكتب") },
                    label = { Text("البحث عن ملف") },
                    leadingIcon = { Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = { onExecuteCommand("افتح المتصفح وتحقق من لوحة تحكم المتجر") },
                    label = { Text("تشغيل تطبيق") },
                    leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = { onExecuteCommand("رتّب مجلد التنزيلات وأنشئ نسخة احتياطية") },
                    label = { Text("إتمام مهمة سطح مكتب") },
                    leadingIcon = { Icon(Icons.Filled.Computer, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                label = { Text("أمر التحكم بالحاسوب") },
                placeholder = { Text("مثال: ابحث عن عرض المبيعات وافتحه للمراجعة") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("remote_command_input")
            )

            Button(
                onClick = {
                    val cmd = command.trim()
                    if (cmd.isNotEmpty()) {
                        command = ""
                        onExecuteCommand(cmd)
                    }
                },
                enabled = !isBusy && command.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("remote_execute_button")
            ) {
                if (isBusy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("تنفيذ على الحاسوب عن بُعد")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutomationsScreen(
    automations: List<AutomationRule>,
    onCreateAutomation: (String, String, String, String) -> Unit,
    onToggleAutomation: (AutomationRule) -> Unit,
    onTriggerNow: (AutomationRule) -> Unit,
    onDeleteAutomation: (String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var triggerDetail by rememberSaveable { mutableStateOf("كل صباح الساعة 9:00") }
    var actionPrompt by rememberSaveable { mutableStateOf("") }
    val triggers = listOf(
        "SCHEDULE" to "مهمة مجدولة",
        "NEW_EMAIL" to "بريد إلكتروني جديد",
        "CALENDAR_EVENT" to "حدث تقويم",
        "SLACK_MESSAGE" to "رسالة Slack",
        "NOTION_UPDATE" to "تحديث Notion"
    )
    var selectedTrigger by rememberSaveable { mutableStateOf("SCHEDULE") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("الأتمتة وسير العمل التلقائي", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "جدول المهام أو شغّل سير عمل تلقائياً عند وصول بريد جديد، حدث تقويم، رسالة Slack، أو تحديث Notion.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("إنشاء قاعدة أتمتة جديدة", style = MaterialTheme.typography.titleMedium)

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        triggers.forEach { (key, label) ->
                            FilterChip(
                                selected = selectedTrigger == key,
                                onClick = {
                                    selectedTrigger = key
                                    triggerDetail = when (key) {
                                        "SCHEDULE" -> "يومياً الساعة 09:00 صباحاً"
                                        "NEW_EMAIL" -> "عند وصول رسالة جديدة في Gmail/Outlook"
                                        "CALENDAR_EVENT" -> "قبل بدء حدث تقويم بـ 15 دقيقة"
                                        "SLACK_MESSAGE" -> "عند إشارة جديدة في قناة Slack"
                                        else -> "عند تعديل صفحة في Notion"
                                    }
                                },
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("اسم الأتمتة") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("automation_title_input")
                    )
                    OutlinedTextField(
                        value = triggerDetail,
                        onValueChange = { triggerDetail = it },
                        label = { Text("تفاصيل المُشغّل / الجدول") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = actionPrompt,
                        onValueChange = { actionPrompt = it },
                        label = { Text("الإجراء الذي ينفذه الوكيل") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("automation_action_input")
                    )

                    Button(
                        onClick = {
                            onCreateAutomation(title, selectedTrigger, triggerDetail, actionPrompt)
                            title = ""
                            actionPrompt = ""
                        },
                        enabled = title.isNotBlank() && actionPrompt.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("create_automation_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("حفظ وتفعيل الأتمتة")
                    }
                }
            }

            if (automations.isNotEmpty()) {
                Text("القواعد المفعلة (${automations.size})", style = MaterialTheme.typography.titleMedium)
                automations.forEach { rule ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rule.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(rule.triggerDetail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Switch(
                                    checked = rule.active,
                                    onCheckedChange = { onToggleAutomation(rule) }
                                )
                            }
                            Text(rule.actionPrompt, style = MaterialTheme.typography.bodySmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "عدد مرات التشغيل: ${rule.runsCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { onTriggerNow(rule) }) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("تشغيل الآن")
                                    }
                                    IconButton(onClick = { onDeleteAutomation(rule.id) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف الأتمتة", tint = MaterialTheme.colorScheme.error)
                                    }
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
fun PricingDialog(
    currentPlan: String,
    creditsBalance: Long,
    onSelectPlan: (String) -> Unit,
    onDismiss: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text("باقات الساحر والرصيد")
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "رصيدك الحالي: $creditsBalance نقطة",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    PricingTierCard(
                        title = "الباقة المجانية",
                        price = "0$ / شهرياً",
                        perks = "• 1000 رصيد ترحيبي عند التسجيل\n• دردشة ذكية مجانية غير محدودة\n• تجربة الوكيل العام والاستوديوهات",
                        selected = currentPlan == "FREE",
                        onClick = { onSelectPlan("FREE") }
                    )

                    PricingTierCard(
                        title = "باقة Pro الاحترافية",
                        price = "20$ / شهرياً",
                        perks = "• 5,000 رصيد شهرياً\n• أولوية تشغيل الحاسوب السحابي\n• توليد فيديو وشرائح والبحث الموسع بدون قيود",
                        selected = currentPlan == "PRO",
                        highlighted = true,
                        onClick = { onSelectPlan("PRO") }
                    )

                    PricingTierCard(
                        title = "باقة Team للفرق",
                        price = "49$ / شهرياً",
                        perks = "• 15,000 رصيد شهرياً\n• كل الموصلات والأتمتة المتقدمة\n• تحكم عن بُعد متعدد الجلسات",
                        selected = currentPlan == "TEAM",
                        onClick = { onSelectPlan("TEAM") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("إغلاق") }
            }
        )
    }
}

@Composable
private fun PricingTierCard(
    title: String,
    price: String,
    perks: String,
    selected: Boolean,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (highlighted) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected || highlighted) 1.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else if (highlighted) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(price, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            }
            Text(perks, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (selected) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                    Text("الباقة الحالية", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}
