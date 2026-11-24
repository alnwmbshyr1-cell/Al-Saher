package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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

private const val MAIN_TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Safe App Check initialization that never crashes the app
        try {
            configureAppCheck(this, intent)
        } catch (t: Throwable) {
            Log.e(MAIN_TAG, "configureAppCheck failed safely", t)
        }

        try {
            enableEdgeToEdge()
        } catch (t: Throwable) {
            Log.w(MAIN_TAG, "enableEdgeToEdge warning", t)
        }

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

    val auth = remember {
        try {
            Firebase.auth
        } catch (t: Throwable) {
            Log.e(MAIN_TAG, "FirebaseAuth unavailable", t)
            null
        }
    }

    if (auth == null) {
        AlSaherTheme(darkTheme = persistedDarkTheme) {
            FirebaseUnavailableNotice()
        }
        return
    }

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
        val firestoreDb = remember(context) { resolveSafeFirestore(context) }
        if (firestoreDb == null) {
            AlSaherTheme(darkTheme = persistedDarkTheme) {
                FirebaseUnavailableNotice()
            }
            return
        }

        key(uid) {
            val viewModel: AlSaherViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val safeApp = try {
                            this[APPLICATION_KEY]
                        } catch (_: Throwable) {
                            null
                        }
                        val db = safeApp?.let { resolveSafeFirestore(it) } ?: firestoreDb
                        AlSaherViewModel(
                            repository = AlSaherRepository(db = db, auth = auth),
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

private fun resolveSafeFirestore(context: android.content.Context): FirebaseFirestore? {
    val databaseId = try {
        context.getString(R.string.firestore_database_id).trim()
    } catch (_: Throwable) {
        ""
    }
    return try {
        if (databaseId.isNotEmpty()) {
            FirebaseFirestore.getInstance(databaseId)
        } else {
            FirebaseFirestore.getInstance()
        }
    } catch (t: Throwable) {
        Log.w(MAIN_TAG, "Named Firestore instance failed, falling back to default instance", t)
        try {
            FirebaseFirestore.getInstance()
        } catch (fallbackError: Throwable) {
            Log.e(MAIN_TAG, "Default FirebaseFirestore.getInstance() failed", fallbackError)
            null
        }
    }
}

@Composable
private fun FirebaseUnavailableNotice() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "تعذر تهيئة خدمات Firebase",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "تحقق من ملف google-services.json وإعدادات Firebase ثم أعد فتح التطبيق.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
