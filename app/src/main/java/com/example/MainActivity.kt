package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.AlSaherAiService
import com.example.data.AlSaherRepository
import com.example.data.ThemePreferences
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.ui.AlSaherWorkspaceScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.AlSaherTheme
import com.example.viewmodel.AlSaherViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FIX: App Check يجب ان لا يطفي التطبيق ابداً
        try {
            configureAppCheck(this, intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // نكمل بدون App Check، التطبيق يفتح عادي
        }

        enableEdgeToEdge()

        setContent {
            AlSaherAppRoot()
        }
    }
}

@Composable
private fun AlSaherAppRoot() {
    val context = LocalContext.current.applicationContext
    val themePreferences = remember(context) { ThemePreferences(context) }
    val persistedDarkTheme by themePreferences.isDarkThemeFlow.collectAsStateWithLifecycle(initialValue = true)

    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AlSaherTheme(darkTheme = persistedDarkTheme) {
            AuthScreen(
                onAuthSuccess = { /* authStateFlow updates automatically */ }
            )
        }
    } else {
        val uid = user.uid
        key(uid) {
            val viewModel: AlSaherViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = try {
                            app.getString(R.string.firestore_database_id)
                        } catch (e: Exception) {
                            "ai-studio-android-alsaher-f3f4cb4f-aa39-400b-81c2-91303e09ca50"
                        }
                        // FIX: لو فشل الاتصال بقاعدة البيانات المخصصة، ارجع للافتراضية ولا تطفي
                        val db = try {
                            FirebaseFirestore.getInstance(databaseId)
                        } catch (e: Exception) {
                            FirebaseFirestore.getInstance()
                        }
                        AlSaherViewModel(
                            repository = AlSaherRepository(db),
                            aiService = AlSaherAiService(),
                            currentUserId = uid,
                            userDisplayName = user.displayName ?: "مستخدم الساحر",
                            userEmail = user.email ?: "",
                            themePreferences = themePreferences,
                            initialDarkTheme = persistedDarkTheme
                        )
                    }
                }
            )
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            AlSaherTheme(darkTheme = isDarkTheme) {
                AlSaherWorkspaceScreen(viewModel = viewModel)
            }
        }
    }
}
