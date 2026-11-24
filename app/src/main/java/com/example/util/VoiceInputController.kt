package com.example.util

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

enum class VoiceLanguageOption(val languageTag: String, val shortLabel: String) {
    ARABIC("ar-SA", "العربية"),
    ENGLISH("en-US", "EN")
}

fun mergeVoiceTranscript(existingText: String, spokenText: String): String {
    val cleanSpoken = spokenText.trim()
    if (cleanSpoken.isEmpty()) return existingText
    if (existingText.isBlank()) return cleanSpoken
    val separator = if (existingText.last().isWhitespace()) "" else " "
    return "$existingText$separator$cleanSpoken"
}

fun mapSpeechErrorToArabic(errorCode: Int): String = when (errorCode) {
    SpeechRecognizer.ERROR_AUDIO -> "تعذر تسجيل الصوت من الميكروفون."
    SpeechRecognizer.ERROR_CLIENT -> "تم إلغاء التعرف الصوتي."
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "يرجى منح صلاحية الميكروفون للتحدث."
    SpeechRecognizer.ERROR_NETWORK,
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "تحقق من اتصال الإنترنت لإتمام التعرف على الصوت."
    SpeechRecognizer.ERROR_NO_MATCH -> "لم أتمكن من تمييز كلام واضح، حاول التحدث مجدداً."
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "خدمة الصوت مشغولة حالياً، حاول بعد لحظة."
    SpeechRecognizer.ERROR_SERVER -> "حدث خطأ في خادم التعرف على الصوت."
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "لم يُكتشف صوت، اضغط على الميكروفون وتكلّم مباشرة."
    else -> "تعذر التعرف على الصوت (رمز $errorCode)."
}

class VoiceInputController(
    val isListening: Boolean,
    val partialTranscript: String,
    val rmsDb: Float,
    val errorMessage: String?,
    val permissionDenied: Boolean,
    val selectedLanguage: VoiceLanguageOption,
    val onToggleListening: () -> Unit,
    val onStopListening: () -> Unit,
    val onCycleLanguage: () -> Unit,
    val onClearError: () -> Unit
)

@Composable
fun rememberVoiceInputController(
    onFinalTranscript: (String) -> Unit
): VoiceInputController {
    val context = LocalContext.current
    val latestOnFinalTranscript by rememberUpdatedState(onFinalTranscript)

    var isListening by remember { mutableStateOf(false) }
    var partialTranscript by remember { mutableStateOf("") }
    var rmsDb by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var permissionDenied by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(VoiceLanguageOption.ARABIC) }

    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        } else {
            null
        }
    }

    val buildSpeechIntent = remember(selectedLanguage) {
        {
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguage.languageTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, selectedLanguage.languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "تكلّم الآن للكتابة في مربع المحادثة")
            }
        }
    }

    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        partialTranscript = ""
        rmsDb = 0f
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognized = matches?.firstOrNull().orEmpty()
            if (recognized.isNotBlank()) {
                latestOnFinalTranscript(recognized)
                errorMessage = null
            } else {
                errorMessage = mapSpeechErrorToArabic(SpeechRecognizer.ERROR_NO_MATCH)
            }
        }
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                errorMessage = null
                partialTranscript = ""
            }

            override fun onBeginningOfSpeech() {
                isListening = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                rmsDb = rmsdB.coerceIn(0f, 12f)
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                rmsDb = 0f
            }

            override fun onError(error: Int) {
                isListening = false
                rmsDb = 0f
                val fallbackPartial = partialTranscript.trim()
                partialTranscript = ""
                if (error != SpeechRecognizer.ERROR_CLIENT) {
                    if (fallbackPartial.isNotEmpty() &&
                        (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                            error == SpeechRecognizer.ERROR_NO_MATCH)
                    ) {
                        latestOnFinalTranscript(fallbackPartial)
                    } else {
                        errorMessage = mapSpeechErrorToArabic(error)
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                rmsDb = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val finalText = matches?.firstOrNull()?.takeIf { it.isNotBlank() }
                    ?: partialTranscript
                partialTranscript = ""
                if (finalText.isNotBlank()) {
                    latestOnFinalTranscript(finalText)
                    errorMessage = null
                } else {
                    errorMessage = mapSpeechErrorToArabic(SpeechRecognizer.ERROR_NO_MATCH)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull().orEmpty()
                if (partial.isNotBlank()) {
                    partialTranscript = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
        speechRecognizer?.setRecognitionListener(listener)

        onDispose {
            runCatching {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            }
        }
    }

    val startRecognition: () -> Unit = {
        errorMessage = null
        permissionDenied = false
        val intent = buildSpeechIntent()
        if (speechRecognizer != null) {
            runCatching {
                speechRecognizer.cancel()
                speechRecognizer.startListening(intent)
                isListening = true
            }.onFailure {
                isListening = false
                errorMessage = "تعذر بدء التعرف الصوتي، يرجى المحاولة مجدداً."
            }
        } else {
            try {
                isListening = true
                systemSpeechLauncher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                isListening = false
                errorMessage = "خدمة التعرف الصوتي غير متوفرة على هذا الجهاز حالياً."
            }
        }
    }

    val stopRecognition: () -> Unit = {
        isListening = false
        rmsDb = 0f
        if (speechRecognizer != null) {
            runCatching { speechRecognizer.stopListening() }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDenied = false
            startRecognition()
        } else {
            permissionDenied = true
            isListening = false
            errorMessage = "تم رفض إذن الميكروفون. فعّل الإذن من الإعدادات لاستخدام الإملاء الصوتي."
        }
    }

    val toggleListening: () -> Unit = {
        if (isListening) {
            stopRecognition()
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                startRecognition()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    return VoiceInputController(
        isListening = isListening,
        partialTranscript = partialTranscript,
        rmsDb = rmsDb,
        errorMessage = errorMessage,
        permissionDenied = permissionDenied,
        selectedLanguage = selectedLanguage,
        onToggleListening = toggleListening,
        onStopListening = stopRecognition,
        onCycleLanguage = {
            selectedLanguage = if (selectedLanguage == VoiceLanguageOption.ARABIC) {
                VoiceLanguageOption.ENGLISH
            } else {
                VoiceLanguageOption.ARABIC
            }
            if (isListening) {
                stopRecognition()
            }
        },
        onClearError = {
            errorMessage = null
            permissionDenied = false
        }
    )
}
