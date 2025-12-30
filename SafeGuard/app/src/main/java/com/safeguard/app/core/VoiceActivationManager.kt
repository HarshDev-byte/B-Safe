package com.safeguard.app.core

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Voice Activation Manager - Enables hands-free SOS triggering
 * Supports multiple languages and custom wake words
 */
class VoiceActivationManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _detectedCommand = MutableStateFlow<VoiceCommand?>(null)
    val detectedCommand: StateFlow<VoiceCommand?> = _detectedCommand.asStateFlow()

    // Configurable trigger phrases in multiple languages
    private val triggerPhrases = mapOf(
        "en" to listOf("help me", "emergency", "call for help", "i need help", "save me", "danger"),
        "es" to listOf("ayuda", "emergencia", "socorro", "auxilio", "necesito ayuda"),
        "fr" to listOf("au secours", "aide moi", "urgence", "à l'aide"),
        "de" to listOf("hilfe", "notfall", "hilf mir", "rettung"),
        "hi" to listOf("bachao", "madad", "help", "emergency"),
        "zh" to listOf("救命", "帮助", "紧急"),
        "ar" to listOf("ساعدني", "نجدة", "طوارئ"),
        "pt" to listOf("socorro", "ajuda", "emergência"),
        "ja" to listOf("助けて", "緊急", "たすけて"),
        "ko" to listOf("도와주세요", "긴급", "살려주세요")
    )

    // Cancel phrases
    private val cancelPhrases = mapOf(
        "en" to listOf("cancel", "stop", "i'm okay", "i'm safe", "false alarm", "nevermind"),
        "es" to listOf("cancelar", "parar", "estoy bien", "falsa alarma"),
        "fr" to listOf("annuler", "arrêter", "je vais bien", "fausse alerte"),
        "de" to listOf("abbrechen", "stopp", "mir geht es gut", "falscher alarm"),
        "hi" to listOf("cancel", "ruko", "main theek hoon"),
        "zh" to listOf("取消", "停止", "我没事"),
        "ar" to listOf("إلغاء", "توقف", "أنا بخير"),
        "pt" to listOf("cancelar", "parar", "estou bem"),
        "ja" to listOf("キャンセル", "止めて", "大丈夫"),
        "ko" to listOf("취소", "멈춰", "괜찮아요")
    )

    sealed class VoiceState {
        object Idle : VoiceState()
        object Listening : VoiceState()
        object Processing : VoiceState()
        data class Error(val message: String) : VoiceState()
        object NotAvailable : VoiceState()
    }

    sealed class VoiceCommand {
        object TriggerSOS : VoiceCommand()
        object CancelSOS : VoiceCommand()
        data class Custom(val phrase: String) : VoiceCommand()
    }

    fun initialize(): Boolean {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceState.value = VoiceState.NotAvailable
            return false
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            _voiceState.value = VoiceState.Error("Microphone permission required")
            return false
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(createRecognitionListener())
        return true
    }

    fun startListening(languageCode: String = Locale.getDefault().language) {
        if (isListening || speechRecognizer == null) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            _voiceState.value = VoiceState.Listening
        } catch (e: Exception) {
            _voiceState.value = VoiceState.Error(e.message ?: "Failed to start listening")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        _voiceState.value = VoiceState.Idle
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _voiceState.value = VoiceState.Listening
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _voiceState.value = VoiceState.Processing
        }

        override fun onError(error: Int) {
            isListening = false
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                else -> "Unknown error"
            }
            
            // Auto-restart on timeout errors for continuous listening
            if (error == SpeechRecognizer.ERROR_NO_MATCH || 
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                _voiceState.value = VoiceState.Idle
            } else {
                _voiceState.value = VoiceState.Error(errorMessage)
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            processResults(matches)
            _voiceState.value = VoiceState.Idle
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            // Check partial results for faster response
            matches?.forEach { phrase ->
                if (isTriggerPhrase(phrase.lowercase())) {
                    _detectedCommand.value = VoiceCommand.TriggerSOS
                    stopListening()
                    return
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun processResults(matches: List<String>?) {
        matches?.forEach { phrase ->
            val lowerPhrase = phrase.lowercase()
            
            when {
                isTriggerPhrase(lowerPhrase) -> {
                    _detectedCommand.value = VoiceCommand.TriggerSOS
                    return
                }
                isCancelPhrase(lowerPhrase) -> {
                    _detectedCommand.value = VoiceCommand.CancelSOS
                    return
                }
            }
        }
    }

    private fun isTriggerPhrase(phrase: String): Boolean {
        return triggerPhrases.values.flatten().any { trigger ->
            phrase.contains(trigger, ignoreCase = true)
        }
    }

    private fun isCancelPhrase(phrase: String): Boolean {
        return cancelPhrases.values.flatten().any { cancel ->
            phrase.contains(cancel, ignoreCase = true)
        }
    }

    fun addCustomTriggerPhrase(languageCode: String, phrase: String) {
        // Allow users to add custom trigger phrases
        val existing = triggerPhrases[languageCode]?.toMutableList() ?: mutableListOf()
        existing.add(phrase.lowercase())
    }

    fun clearDetectedCommand() {
        _detectedCommand.value = null
    }

    fun cleanup() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
