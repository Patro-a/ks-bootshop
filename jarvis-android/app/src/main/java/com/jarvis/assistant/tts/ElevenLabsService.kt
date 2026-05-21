package com.jarvis.assistant.tts

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jarvis.assistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ElevenLabsService {

    companion object {
        // Popular ElevenLabs voice IDs — swap freely in settings
        const val VOICE_ADAM    = "pNInz6obpgDQGcFmaJgB"  // deep, authoritative
        const val VOICE_ANTONI  = "ErXwobaYiN019PkySvjV"  // warm, friendly
        const val VOICE_RACHEL  = "21m00Tcm4TlvDq8ikWAM"  // clear, professional
    }

    var selectedVoiceId: String = VOICE_ADAM

    private val gson = Gson()
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private var player: MediaPlayer? = null

    /**
     * Fetches audio from ElevenLabs, saves it temporarily, and plays it.
     * Suspends until playback is complete or an error occurs.
     */
    suspend fun speak(text: String, cacheDir: File): Result<Unit> {
        return try {
            val audioBytes = withContext(Dispatchers.IO) { fetchAudio(text) }
            val tmpFile = withContext(Dispatchers.IO) {
                File(cacheDir, "jarvis_${System.currentTimeMillis()}.mp3")
                    .also { it.writeBytes(audioBytes) }
            }
            withContext(Dispatchers.Main) { playAndAwait(tmpFile) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchAudio(text: String): ByteArray {
        val body = gson.toJson(
            TtsRequest(
                text = text,
                modelId = "eleven_monolingual_v1",
                voiceSettings = VoiceSettings(stability = 0.50f, similarityBoost = 0.75f)
            )
        ).toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.elevenlabs.io/v1/text-to-speech/$selectedVoiceId")
            .addHeader("xi-api-key", BuildConfig.ELEVENLABS_API_KEY)
            .addHeader("Accept", "audio/mpeg")
            .post(body)
            .build()

        val response = http.newCall(request).execute()
        if (!response.isSuccessful) {
            error("ElevenLabs ${response.code}: ${response.body?.string()}")
        }
        return response.body?.bytes() ?: error("Empty audio response from ElevenLabs")
    }

    private suspend fun playAndAwait(file: File) = suspendCancellableCoroutine<Unit> { cont ->
        stopPlaying()
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .build()
            )
            setOnCompletionListener {
                file.delete()
                cont.resume(Unit)
            }
            setOnErrorListener { _, what, extra ->
                cont.resumeWithException(Exception("MediaPlayer error $what/$extra"))
                true
            }
            setDataSource(file.absolutePath)
            prepare()
            start()
        }
        cont.invokeOnCancellation { stopPlaying() }
    }

    fun stopPlaying() {
        player?.let { mp -> if (mp.isPlaying) mp.stop(); mp.release() }
        player = null
    }

    fun destroy() = stopPlaying()

    // ── API data classes ──────────────────────────────────────────────────────

    data class TtsRequest(
        val text: String,
        @SerializedName("model_id") val modelId: String,
        @SerializedName("voice_settings") val voiceSettings: VoiceSettings
    )

    data class VoiceSettings(
        val stability: Float,
        @SerializedName("similarity_boost") val similarityBoost: Float
    )
}
