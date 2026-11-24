package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AccountPreferences
import com.example.model.AgentMode
import com.example.model.ChatbotRole
import com.example.model.GeminiChatTier
import com.example.model.parseEnumSafe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.alSaherSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "al_saher_settings"
)

class ThemePreferences(private val context: Context) {

    private val isDarkThemeKey = booleanPreferencesKey("is_dark_theme")
    private val notifyOnCompleteKey = booleanPreferencesKey("notify_on_complete")
    private val defaultAgentModeKey = stringPreferencesKey("default_agent_mode")
    private val defaultChatTierKey = stringPreferencesKey("default_chat_tier")
    private val defaultChatbotRoleKey = stringPreferencesKey("default_chatbot_role")
    private val defaultSearchGroundingKey = booleanPreferencesKey("default_search_grounding")

    private val safePrefsFlow: Flow<Preferences> = context.alSaherSettingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val isDarkThemeFlow: Flow<Boolean> = safePrefsFlow.map { preferences ->
        preferences[isDarkThemeKey] ?: true
    }

    val accountPreferencesFlow: Flow<AccountPreferences> = safePrefsFlow.map { preferences ->
        AccountPreferences(
            isDarkTheme = preferences[isDarkThemeKey] ?: true,
            notifyOnComplete = preferences[notifyOnCompleteKey] ?: true,
            defaultAgentMode = parseEnumSafe(preferences[defaultAgentModeKey], AgentMode.AGENT),
            defaultChatTier = parseEnumSafe(preferences[defaultChatTierKey], GeminiChatTier.GENERAL),
            defaultChatbotRole = parseEnumSafe(
                preferences[defaultChatbotRoleKey],
                ChatbotRole.GENERAL_ASSISTANT
            ),
            defaultSearchGrounding = preferences[defaultSearchGroundingKey] ?: true
        )
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.alSaherSettingsDataStore.edit { preferences ->
            preferences[isDarkThemeKey] = isDark
        }
    }

    suspend fun setNotifyOnComplete(enabled: Boolean) {
        context.alSaherSettingsDataStore.edit { preferences ->
            preferences[notifyOnCompleteKey] = enabled
        }
    }

    suspend fun setDefaultAgentMode(mode: AgentMode) {
        context.alSaherSettingsDataStore.edit { preferences ->
            preferences[defaultAgentModeKey] = mode.name
        }
    }

    suspend fun setDefaultChatTier(tier: GeminiChatTier) {
        context.alSaherSettingsDataStore.edit { preferences ->
            preferences[defaultChatTierKey] = tier.name
        }
    }

    suspend fun setDefaultChatbotRole(role: ChatbotRole) {
        context.alSaherSettingsDataStore.edit { preferences ->
            preferences[defaultChatbotRoleKey] = role.name
        }
    }

    suspend fun setDefaultSearchGrounding(enabled: Boolean) {
        context.alSaherSettingsDataStore.edit { preferences ->
            preferences[defaultSearchGroundingKey] = enabled
        }
    }
}
