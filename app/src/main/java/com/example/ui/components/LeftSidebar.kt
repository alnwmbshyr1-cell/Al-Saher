package com.example.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.model.AgentTask
import com.example.model.UserProfile
import com.example.model.WorkspaceDestination

private data class DrawerToolSpec(
    val destination: WorkspaceDestination,
    val title: String,
    val subtitle: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val badgeText: String? = null
)

private val workspaceTools = listOf(
    DrawerToolSpec(
        destination = WorkspaceDestination.CHAT_AGENT,
        title = "الوكيل العام والدردشة",
        subtitle = "تخطيط وتنفيذ المهام أو محادثة فورية",
        selectedIcon = Icons.AutoMirrored.Filled.Chat,
        unselectedIcon = Icons.AutoMirrored.Outlined.Chat,
        testTag = "nav_chat_agent"
    ),
    DrawerToolSpec(
        destination = WorkspaceDestination.DIRECT_AI_CHAT,
        title = "محادثة Firebase AI",
        subtitle = "محادثة متعددة الوسائط مباشرة مع Gemini",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Filled.AutoAwesome,
        testTag = "nav_direct_ai_chat",
        badgeText = "AI"
    )
)

private val creativeStudioTools = listOf(
    DrawerToolSpec(
        destination = WorkspaceDestination.IMAGE_STUDIO,
        title = "استوديو الصور",
        subtitle = "إنشاء وتعديل الصور بالذكاء الاصطناعي",
        selectedIcon = Icons.Filled.Image,
        unselectedIcon = Icons.Outlined.Image,
        testTag = "nav_image_studio"
    ),
    DrawerToolSpec(
        destination = WorkspaceDestination.VIDEO_STUDIO,
        title = "توليد وتحرير الفيديو",
        subtitle = "نصوص وصور إلى مشروع فيديو متعدد المسارات",
        selectedIcon = Icons.Filled.Movie,
        unselectedIcon = Icons.Outlined.Movie,
        testTag = "nav_video_studio"
    ),
    DrawerToolSpec(
        destination = WorkspaceDestination.SLIDES_STUDIO,
        title = "العروض التقديمية",
        subtitle = "تصميم شرائح احترافية وتصدير PDF",
        selectedIcon = Icons.Filled.Slideshow,
        unselectedIcon = Icons.Outlined.Slideshow,
        testTag = "nav_slides_studio"
    ),
    DrawerToolSpec(
        destination = WorkspaceDestination.DOCS_RESEARCH,
        title = "المستندات والبحث الموسع",
        subtitle = "تقارير، جداول بيانات وبحث وكلاء متوازٍ",
        selectedIcon = Icons.Filled.TravelExplore,
        unselectedIcon = Icons.Outlined.TravelExplore,
        testTag = "nav_docs_research"
    ),
    DrawerToolSpec(
        destination = WorkspaceDestination.WEBSITE_BUILDER,
        title = "منشئ المواقع",
        subtitle = "صفحات هبوط، متاجر وتطبيقات ويب كاملة",
        selectedIcon = Icons.Filled.Language,
        unselectedIcon = Icons.Outlined.Language,
        testTag = "nav_website_builder"
    ),
    DrawerToolSpec(
        destination = WorkspaceDestination.APP_BUILDER,
        title = "منشئ التطبيقات",
        subtitle = "تطبيقات iOS وAndroid بواجهة وخلفية",
        selectedIcon = Icons.Filled.PhoneAndroid,
        unselectedIcon = Icons.Outlined.PhoneAndroid,
        testTag = "nav_app_builder"
    ),
    DrawerToolSpec(
        destination = WorkspaceDestination.GAME_MAKER,
        title = "صانع الألعاب",
        subtitle = "ألعاب تفاعلية قابلة للعب بالرسوم والقواعد",
        selectedIcon = Icons.Filled.SportsEsports,
        unselectedIcon = Icons.Outlined.SportsEsports,
        testTag = "nav_game_maker"
    )
)

private val cloudAutomationTools = listOf(
    DrawerToolSpec(
        destination = WorkspaceDestination.CONNECTORS,
        title = "الموصلات والتكاملات",
        subtitle = "Gmail وDrive وSlack وNotion وGitHub",
        selectedIcon = Icons.Filled.Extension,
        unselectedIcon = Icons.Outlined.Extension,
        testTag = "nav_connectors",
        badgeText = "10"
    ),
    DrawerToolSpec(
        destination = WorkspaceDestination.REMOTE_CONTROL,
        title = "التحكم عن بُعد",
        subtitle = "إدارة وتشغيل الحاسوب السحابي مباشرة",
        selectedIcon = Icons.Filled.DesktopWindows,
        unselectedIcon = Icons.Outlined.DesktopWindows,
        testTag = "nav_remote_control"
    ),
    DrawerToolSpec(
        destination = WorkspaceDestination.AUTOMATIONS,
        title = "الأتمتة وسير العمل",
        subtitle = "جدولة المهام والمحفزات التلقائية",
        selectedIcon = Icons.Filled.AutoMode,
        unselectedIcon = Icons.Outlined.AutoMode,
        testTag = "nav_automations"
    )
)

@Composable
fun LeftSidebar(
    currentDestination: WorkspaceDestination,
    tasks: List<AgentTask>,
    activeTaskId: String?,
    userProfile: UserProfile?,
    isDarkTheme: Boolean,
    onSelectDestination: (WorkspaceDestination) -> Unit,
    onNewTask: () -> Unit,
    onSelectTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onOpenPricing: () -> Unit,
    onSetDarkTheme: (Boolean) -> Unit,
    onExportChatHistory: () -> Unit = {},
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Brand Header + Theme Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "شعار الساحر",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "الساحر",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Al Saher • استوديو الأدوات",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            CreditCounter(
                creditsBalance = userProfile?.creditsBalance ?: FREE_SIGNUP_CREDITS_QUOTA,
                plan = userProfile?.plan,
                onClick = onOpenPricing
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onNewTask,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("new_task_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "مهمة جديدة", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("مهمة أو محادثة جديدة", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SidebarSectionLabel("مساحة العمل")
                workspaceTools.forEach { tool ->
                    DrawerToolRow(
                        tool = tool,
                        selected = currentDestination == tool.destination,
                        onClick = { onSelectDestination(tool.destination) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                SidebarSectionLabel("أدوات الإبداع والبناء")
                creativeStudioTools.forEach { tool ->
                    DrawerToolRow(
                        tool = tool,
                        selected = currentDestination == tool.destination,
                        onClick = { onSelectDestination(tool.destination) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                SidebarSectionLabel("الحاسوب السحابي والأتمتة")
                cloudAutomationTools.forEach { tool ->
                    DrawerToolRow(
                        tool = tool,
                        selected = currentDestination == tool.destination,
                        onClick = { onSelectDestination(tool.destination) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                SidebarSectionLabel("إعدادات التطبيق")
                ApplicationSettingsThemeCard(
                    isDarkTheme = isDarkTheme,
                    onSetDarkTheme = onSetDarkTheme
                )

                Spacer(modifier = Modifier.height(10.dp))
                ChatHistorySection(
                    tasks = tasks,
                    activeTaskId = activeTaskId,
                    currentDestination = currentDestination,
                    onSelectConversation = onSelectTask,
                    onDeleteConversation = onDeleteTask,
                    onExportHistory = onExportChatHistory
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Compact Plan & Upgrade Row
            val planLabel = when (userProfile?.plan) {
                "PRO" -> "باقة Pro (20$/شهر)"
                "TEAM" -> "باقة Team (49$/شهر)"
                else -> "الباقة المجانية"
            }

            Surface(
                onClick = onOpenPricing,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("pricing_card_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = planLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        text = "الباقات والترقية",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Account & Sign Out row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = userProfile?.displayName ?: "مستخدم الساحر",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "تسجيل الخروج",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun DrawerToolRow(
    tool: DrawerToolSpec,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedContainerColor = MaterialTheme.colorScheme.surface,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        icon = {
            Icon(
                imageVector = if (selected) tool.selectedIcon else tool.unselectedIcon,
                contentDescription = tool.title,
                modifier = Modifier.size(21.dp)
            )
        },
        label = {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tool.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        badge = tool.badgeText?.let { badge ->
            {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(tool.testTag)
    )
}

@Composable
private fun ApplicationSettingsThemeCard(
    isDarkTheme: Boolean,
    onSetDarkTheme: (Boolean) -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            tonalElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(16.dp)
                )
                .testTag("application_settings_theme_card")
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "إعدادات المظهر",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "الوضع الليلي والنهاري",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDarkTheme) "الوضع الليلي مُفعّل" else "الوضع النهاري مُفعّل",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onSetDarkTheme,
                        thumbContent = {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = if (isDarkTheme) "الوضع الليلي" else "الوضع النهاري",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier.testTag("theme_toggle_button")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeModePill(
                        label = "ليلي",
                        icon = Icons.Filled.DarkMode,
                        selected = isDarkTheme,
                        testTag = "theme_dark_option",
                        onClick = { onSetDarkTheme(true) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeModePill(
                        label = "نهاري",
                        icon = Icons.Filled.LightMode,
                        selected = !isDarkTheme,
                        testTag = "theme_light_option",
                        onClick = { onSetDarkTheme(false) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModePill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier
            .heightIn(min = 48.dp)
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
