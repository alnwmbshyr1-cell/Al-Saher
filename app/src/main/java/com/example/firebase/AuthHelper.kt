package com.example.firebase

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    try {
        addAuthStateListener(listener)
    } catch (t: Throwable) {
        Log.e("Auth", "Failed to register AuthStateListener", t)
        trySend(null)
    }
    awaitClose {
        try {
            removeAuthStateListener(listener)
        } catch (t: Throwable) {
            Log.w("Auth", "Failed to remove AuthStateListener", t)
        }
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    try {
        val auth = try {
            Firebase.auth
        } catch (_: Throwable) {
            null
        }
        if (auth?.currentUser != null) {
            onAuthSuccess()
            return
        }
        if (credentialManager == null || auth == null) {
            onUnauthenticated()
            return
        }
        val clientId = resolveDefaultWebClientId(context)
        if (clientId.isEmpty()) {
            onUnauthenticated()
            return
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        scope.launch {
            try {
                val cred = withTimeoutOrNull(3500L) {
                    credentialManager.getCredential(context, request).credential
                }
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (_: Throwable) {
                onUnauthenticated()
            }
        }
    } catch (_: Throwable) {
        onUnauthenticated()
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val auth = try {
        Firebase.auth
    } catch (t: Throwable) {
        Log.e("Auth", "FirebaseAuth unavailable on sign-in", t)
        onAuthError("تعذر تهيئة خدمة المصادقة.")
        return
    }
    if (credentialManager == null) {
        onAuthError("خدمة Credential Manager غير متاحة على هذا الجهاز.")
        return
    }
    val clientId = resolveDefaultWebClientId(context)
    if (clientId.isEmpty()) {
        onAuthError("تعذر العثور على إعدادات تسجيل الدخول بحساب Google.")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    scope.launch {
        try {
            val activityContext = context as? Activity
            if (activityContext == null) {
                onAuthError("يتطلب تسجيل الدخول سياق Activity صالح")
                return@launch
            }
            val cred = credentialManager.getCredential(activityContext, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("نوع بيانات اعتماد غير متوقع")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (t: Throwable) {
            Log.e("Auth", "Google Sign-In failed", t)
            onAuthError(t.localizedMessage ?: "تعذر تسجيل الدخول عبر Google")
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager?,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager?.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Throwable) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                try {
                    Firebase.auth.signOut()
                } catch (t: Throwable) {
                    Log.w("Auth", "FirebaseAuth signOut warning: ${t.message}")
                }
                onSignOutComplete()
            }
        }
    }
}

@SuppressLint("DiscouragedApi")
private fun resolveDefaultWebClientId(context: Context): String {
    return try {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resId != 0) context.getString(resId).trim() else ""
    } catch (_: Throwable) {
        ""
    }
}
