package com.safeguard.app.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * AI-Powered Voice Command Recognition
 * Supports multiple languages and natural language processing
 */
class VoiceCommandAI(private val context: Context) {

    companion object {
        private const val TAG = "VoiceCommandAI"
        
        // Emergency trigger phrases in multiple languages
        private val EMERGENCY_PHRASES = mapOf(
            "en" to listOf("help", "help me", "emergency", "sos", "save me", "call police", "i need help", "danger"),
            "es" to listOf("ayuda", "ayúdame", "emergencia", "socorro", "auxilio", "llama a la policía"),
            "hi" to listOf("bachao", "madad", "help", "police bulao", "emergency"),
            "fr" to listOf("aide", "aidez-moi", "urgence", "au secours", "police"),
            "de" to listOf("hilfe", "hilf mir", "notfall", "polizei"),
            "pt" to listOf("ajuda", "socorro", "emergência", "me ajuda"),
            "ar" to listOf("مساعدة", "النجدة", "طوارئ"),
            "zh" to listOf("救命", "帮助", "紧急"),
            "ja" to listOf("助けて", "たすけて", "緊急"),
            "ko" to listOf("도와주세요", "살려주세요", "긴급")
        )

        // Cancel phrases
        private val CANCEL_PHRASES = mapOf(
            "en" to listOf("cancel", "stop", "never mind", "false alarm", "i'm okay", "i'm fine"),
            "es" to listOf("cancelar", "parar", "estoy bien"),
            "hi" to listOf("cancel", "band karo", "theek hu"),
            "fr" to listOf("annuler", "arrêter", "ça va"),
            "de" to listOf("abbrechen", "stopp", "alles gut")
        )

        // Safe word customization
        private val DEFAULT_SAFE_WORD = "pineapple"
    }

    data class VoiceCommand(
        val type: CommandType,
        val confidence: Float,
        val rawText: String,
        val language: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    enum class CommandType {
        TRIGGER_SOS,
        CANCEL_SOS,
        SAFE_WORD,
        CALL_CONTACT,
        SHARE_LOCATION,
        UNKNOWN
    }

    enum class ListeningState {
        IDLE,
        LISTENING,
        PROCESSING,
        ERROR
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var customSafeWord: String = DEFAULT_SAFE_WORD

    private val _listeningState = MutableStateFlow(ListeningState.IDLE)
    val listeningState: StateFlow<ListeningState> = _listeningState.asStateFlow()

    private val _lastCommand = MutableStateFlow<VoiceCommand?>(null)
    val lastCommand: StateFlow<VoiceCommand?> = _lastCommand.asStateFlow()

    private var onCommandDetected: ((VoiceCommand) -> Unit)? = null

    /**
     * Initialize voice recognition
     */
    fun initialize() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition not available on this device")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
        }
        Log.d(TAG, "Voice Command AI initialized")
    }

    /**
     * Set custom safe word for discreet SOS trigger
     */
    fun setSafeWord(word: String) {
        customSafeWord = word.lowercase().trim()
        Log.d(TAG, "Safe word updated")
    }

    /**
     * Set callback for command detection
     */
    fun setOnCommandDetected(callback: (VoiceCommand) -> Unit) {
        onCommandDetected = callback
    }

    /**
     * Start listening for voice commands
     */
    fun startListening(language: String = "en") {
        if (_listeningState.value == ListeningState.LISTENING) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, getLocaleForLanguage(language))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
        }

        try {
            speechRecognizer?.startListening(intent)
            _listeningState.value = ListeningState.LISTENING
            Log.d(TAG, "Started listening for voice commands")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            _listeningState.value = ListeningState.ERROR
        }
    }

    /**
     * Stop listening
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        _listeningState.value = ListeningState.IDLE
    }

    /**
     * Process recognized text and detect commands
     */
    fun processText(text: String, language: String = "en"): VoiceCommand {
        val normalizedText = text.lowercase().trim()
        
        // Check for safe word first (highest priority)
        if (normalizedText.contains(customSafeWord)) {
            return VoiceCommand(
                type = CommandType.SAFE_WORD,
                confidence = 1.0f,
                rawText = text,
                language = language
            )
        }

        // Check for emergency phrases
        val emergencyPhrases = EMERGENCY_PHRASES[language] ?: EMERGENCY_PHRASES["en"]!!
        for (phrase in emergencyPhrases) {
            if (normalizedText.contains(phrase)) {
                val confidence = calculateConfidence(normalizedText, phrase)
                return VoiceCommand(
                    type = CommandType.TRIGGER_SOS,
                    confidence = confidence,
                    rawText = text,
                    language = language
                )
            }
        }

        // Check for cancel phrases
        val cancelPhrases = CANCEL_PHRASES[language] ?: CANCEL_PHRASES["en"]!!
        for (phrase in cancelPhrases) {
            if (normalizedText.contains(phrase)) {
                return VoiceCommand(
                    type = CommandType.CANCEL_SOS,
                    confidence = 0.9f,
                    rawText = text,
                    language = language
                )
            }
        }

        // Check for contact call commands
        if (normalizedText.contains("call") || normalizedText.contains("phone")) {
            return VoiceCommand(
                type = CommandType.CALL_CONTACT,
                confidence = 0.7f,
                rawText = text,
                language = language
            )
        }

        // Check for location sharing
        if (normalizedText.contains("location") || normalizedText.contains("where")) {
            return VoiceCommand(
                type = CommandType.SHARE_LOCATION,
                confidence = 0.7f,
                rawText = text,
                language = language
            )
        }

        return VoiceCommand(
            type = CommandType.UNKNOWN,
            confidence = 0f,
            rawText = text,
            language = language
        )
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _listeningState.value = ListeningState.PROCESSING
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Unknown error"
                }
                Log.e(TAG, "Recognition error: $errorMessage")
                _listeningState.value = ListeningState.ERROR
            }

            override fun onResults(results: Bundle?) {
                _listeningState.value = ListeningState.IDLE
                
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                
                if (!matches.isNullOrEmpty()) {
                    val bestMatch = matches[0]
                    val confidence = confidences?.getOrNull(0) ?: 0.5f
                    
                    Log.d(TAG, "Recognized: $bestMatch (confidence: $confidence)")
                    
                    val command = processText(bestMatch)
                    _lastCommand.value = command
                    
                    if (command.type != CommandType.UNKNOWN) {
                        onCommandDetected?.invoke(command)
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    // Check partial results for emergency phrases (faster response)
                    val command = processText(matches[0])
                    if (command.type == CommandType.TRIGGER_SOS && command.confidence > 0.8f) {
                        _lastCommand.value = command
                        onCommandDetected?.invoke(command)
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun calculateConfidence(text: String, phrase: String): Float {
        // Simple confidence calculation based on exact match vs partial
        return when {
            text == phrase -> 1.0f
            text.startsWith(phrase) || text.endsWith(phrase) -> 0.9f
            text.contains(phrase) -> 0.8f
            else -> 0.5f
        }
    }

    private fun getLocaleForLanguage(language: String): String {
        return when (language) {
            "en" -> "en-US"
            "es" -> "es-ES"
            "hi" -> "hi-IN"
            "fr" -> "fr-FR"
            "de" -> "de-DE"
            "pt" -> "pt-BR"
            "ar" -> "ar-SA"
            "zh" -> "zh-CN"
            "ja" -> "ja-JP"
            "ko" -> "ko-KR"
            else -> "en-US"
        }
    }

    /**
     * Get supported languages
     */
    fun getSupportedLanguages(): List<Pair<String, String>> {
        return listOf(
            "en" to "English",
            "es" to "Español",
            "hi" to "हिंदी",
            "fr" to "Français",
            "de" to "Deutsch",
            "pt" to "Português",
            "ar" to "العربية",
            "zh" to "中文",
            "ja" to "日本語",
            "ko" to "한국어"
        )
    }

    fun cleanup() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
