package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.util.ChatExportFormat
import com.example.util.ChatExportScope

@Composable
fun ChatExportDialog(
    hasActiveConversation: Boolean,
    totalConversationsCount: Int,
    onDismiss: () -> Unit,
    onConfirmExport: (ChatExportScope, ChatExportFormat) -> Unit
) {
    var selectedScope by rememberSaveable(hasActiveConversation) {
        mutableStateOf(
            if (hasActiveConversation) ChatExportScope.CURRENT else ChatExportScope.ALL
        )
    }
    var selectedFormat by rememberSaveable { mutableStateOf(ChatExportFormat.JSON) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Filled.FileDownload,
                    contentDescription = "تصدير المحادثات",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "تصدير سجل المحادثات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "نطاق التصدير",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (hasActiveConversation) {
                            ExportOptionRow(
                                title = ChatExportScope.CURRENT.arabicLabel,
                                subtitle = "تصدير رسائل المحادثة المفتوحة حالياً فقط",
                                selected = selectedScope == ChatExportScope.CURRENT,
                                testTag = "export_scope_current",
                                onClick = { selectedScope = ChatExportScope.CURRENT }
                            )
                        }
                        ExportOptionRow(
                            title = "${ChatExportScope.ALL.arabicLabel} ($totalConversationsCount)",
                            subtitle = "نسخة شاملة لجميع محادثاتك ومهامك المحفوظة",
                            selected = selectedScope == ChatExportScope.ALL,
                            testTag = "export_scope_all",
                            onClick = { selectedScope = ChatExportScope.ALL }
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "صيغة الملف",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChatExportFormat.entries.forEach { format ->
                            ExportOptionRow(
                                title = format.arabicLabel,
                                subtitle = format.arabicDescription,
                                selected = selectedFormat == format,
                                leadingIcon = if (format == ChatExportFormat.JSON) {
                                    Icons.Filled.Code
                                } else {
                                    Icons.Filled.Description
                                },
                                testTag = "export_format_${format.name.lowercase()}",
                                onClick = { selectedFormat = format }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onConfirmExport(selectedScope, selectedFormat) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("confirm_export_chat_button")
                ) {
                    Text("حفظ الملف")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("cancel_export_chat_button")
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun ExportOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    testTag: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .border(
                width = 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                },
                shape = RoundedCornerShape(14.dp)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
