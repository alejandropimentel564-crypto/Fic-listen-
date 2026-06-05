package com.example.ui.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

data class SpeechChunk(
    val text: String,
    val startOffset: Int,
    val endOffset: Int
)

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    private var onFinishedCallback: (() -> Unit)? = null
    private var onWordHighlightCallback: ((startCharPos: Int, endCharPos: Int) -> Unit)? = null

    // Multi-chunk reading engine tracking
    private val speechChunks = mutableListOf<SpeechChunk>()
    private var currentChunkIndex = 0
    private var currentText = ""
    private var activeSpeed = 1.0f
    private var activePitch = 1.0f

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val systemLocale = Locale.getDefault()
            val result = tts?.setLanguage(systemLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale("es", "ES"))
            }
            _isInitialized.value = true
            setupProgressListener()
        } else {
            Log.e("TtsManager", "Initialization of TextToSpeech failed.")
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId?.startsWith("FIC_SPEECH_CHUNK_") == true) {
                    playNextChunk()
                } else {
                    _isSpeaking.value = false
                    onFinishedCallback?.invoke()
                }
            }

            @Deprecated("Deprecated")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                Log.e("TtsManager", "Error vocalizing chunk: $utteranceId")
                if (utteranceId?.startsWith("FIC_SPEECH_CHUNK_") == true) {
                    playNextChunk() // recover gracefully
                }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                // Get the active sentence chunk
                val chunk = speechChunks.getOrNull(currentChunkIndex)
                if (chunk != null) {
                    // Offset highlight index to absolute position in chapter
                    onWordHighlightCallback?.invoke(chunk.startOffset + start, chunk.startOffset + end)
                } else {
                    onWordHighlightCallback?.invoke(start, end)
                }
            }
        })
    }

    fun speak(
        text: String,
        speed: Float,
        pitch: Float,
        onWordHighlight: ((Int, Int) -> Unit)? = null,
        onFinished: () -> Unit
    ) {
        if (!_isInitialized.value) return

        onFinishedCallback = onFinished
        onWordHighlightCallback = onWordHighlight

        activeSpeed = speed
        activePitch = pitch

        tts?.setSpeechRate(speed)
        tts?.setPitch(pitch)

        // Resets chunks if text actually changed, otherwise we preserve our position (perfect resuming!)
        if (text != currentText || speechChunks.isEmpty()) {
            currentText = text
            speechChunks.clear()
            speechChunks.addAll(splitIntoSpeechChunks(text))
            currentChunkIndex = 0
        }

        stopSpeechEngineOnly()

        if (speechChunks.isNotEmpty() && currentChunkIndex in speechChunks.indices) {
            _isSpeaking.value = true
            speakChunk(currentChunkIndex)
        } else {
            _isSpeaking.value = false
            currentChunkIndex = 0
            onFinishedCallback?.invoke()
        }
    }

    private fun speakChunk(index: Int) {
        val chunk = speechChunks.getOrNull(index) ?: return
        val utteranceId = "FIC_SPEECH_CHUNK_${index}_${System.currentTimeMillis()}"
        handler.post {
            tts?.speak(chunk.text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    private fun playNextChunk() {
        if (currentChunkIndex + 1 < speechChunks.size) {
            currentChunkIndex++
            speakChunk(currentChunkIndex)
        } else {
            _isSpeaking.value = false
            currentChunkIndex = 0
            currentText = ""
            speechChunks.clear()
            handler.post {
                onFinishedCallback?.invoke()
            }
        }
    }

    fun seekToOffset(targetOffset: Int) {
        val foundIndex = speechChunks.indexOfFirst { targetOffset in it.startOffset..it.endOffset }
        if (foundIndex != -1) {
            currentChunkIndex = foundIndex
            if (_isSpeaking.value) {
                stopSpeechEngineOnly()
                _isSpeaking.value = true
                speakChunk(currentChunkIndex)
            } else {
                // Instantly update highlights to represent chosen paragraph area
                val chunk = speechChunks[currentChunkIndex]
                onWordHighlightCallback?.invoke(chunk.startOffset, chunk.startOffset + 2)
            }
        }
    }

    private fun stopSpeechEngineOnly() {
        tts?.stop()
    }

    fun pause() {
        stopSpeechEngineOnly()
        _isSpeaking.value = false
    }

    fun stop() {
        stopSpeechEngineOnly()
        _isSpeaking.value = false
        currentText = ""
        speechChunks.clear()
        currentChunkIndex = 0
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }

    // Punctuated sentence splitter suited for fanfictions text parsing
    private fun splitIntoSpeechChunks(text: String): List<SpeechChunk> {
        val chunks = mutableListOf<SpeechChunk>()
        if (text.isEmpty()) return chunks

        val delimiters = charArrayOf('.', '?', '!', '\n', ';', ':')
        var tempStart = 0
        var i = 0
        val len = text.length

        while (i < len) {
            val c = text[i]
            // Push chunk if we find a sentence delimiter or we exceed 250 characters for clean speaking
            if (c in delimiters || (i - tempStart) >= 250) {
                var tempEnd = i + 1
                // Catch ellipses like "..." or multi punct "?" or "?!" safely together
                while (tempEnd < len && text[tempEnd] in delimiters) {
                    tempEnd++
                }

                val chunkStr = text.substring(tempStart, tempEnd).trim()
                if (chunkStr.isNotEmpty()) {
                    chunks.add(SpeechChunk(text = chunkStr, startOffset = tempStart, endOffset = tempEnd))
                }
                tempStart = tempEnd
                i = tempEnd - 1
            }
            i++
        }

        if (tempStart < len) {
            val remaining = text.substring(tempStart, len).trim()
            if (remaining.isNotEmpty()) {
                chunks.add(SpeechChunk(text = remaining, startOffset = tempStart, endOffset = len))
            }
        }

        return chunks
    }
}
