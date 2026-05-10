package dev.bhaarath.hydratemeds.shared.runtime

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dev.bhaarath.hydratemeds.shared.R
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import dev.bhaarath.hydratemeds.shared.schedule.HydrateMedsScheduleConfig
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object ReminderVoiceAnnouncer {
    fun speak(context: Context, payload: ReminderPayload) {
        if (!HydrateMedsScheduleConfig.spokenAlertsEnabled) return

        val appContext = context.applicationContext
        val prompt = HydrateMedsScheduleConfig.spokenPrompt(payload.reminderType)
        thread(name = "HydrateMedsVoice", isDaemon = false) {
            val playedCustomClip = customClipFor(payload.reminderType)
                ?.let { clip -> playRawClip(appContext, clip) }
                ?: false
            if (!playedCustomClip) {
                speakBlocking(appContext, prompt)
            }
        }
    }

    private fun customClipFor(type: ReminderType): Int? =
        when (type) {
            ReminderType.Water -> R.raw.drink_water_appa
            ReminderType.MorningMedicine,
            ReminderType.EveningMedicine -> R.raw.medicine_reminder
        }

    private fun playRawClip(context: Context, rawResId: Int): Boolean {
        val completed = CountDownLatch(1)
        var failed = false
        val player = MediaPlayer()
        return try {
            context.resources.openRawResourceFd(rawResId).use { asset ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    player.setAudioAttributes(alarmSpeechAttributes())
                }
                player.setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
                player.setVolume(1f, 1f)
                player.setOnCompletionListener { completed.countDown() }
                player.setOnErrorListener { _, _, _ ->
                    failed = true
                    completed.countDown()
                    true
                }
                player.prepare()
                val playbackTimeoutMillis = playbackTimeoutMillis(player)
                player.start()
                completed.await(playbackTimeoutMillis, TimeUnit.MILLISECONDS)
                !failed
            }
        } catch (_: Exception) {
            false
        } finally {
            player.release()
        }
    }

    private fun playbackTimeoutMillis(player: MediaPlayer): Long {
        val durationWithPadding = player.duration.takeIf { it > 0 }?.plus(3_000) ?: 45_000
        return durationWithPadding.coerceAtLeast(15_000).toLong()
    }

    private fun speakBlocking(context: Context, prompt: String) {
        val initLatch = CountDownLatch(1)
        var initStatus = TextToSpeech.ERROR
        var tts: TextToSpeech? = null

        tts = TextToSpeech(context) { status ->
            initStatus = status
            initLatch.countDown()
        }

        try {
            if (!initLatch.await(3, TimeUnit.SECONDS) || initStatus != TextToSpeech.SUCCESS) return
            val readyEngine = tts ?: return
            configure(readyEngine)

            val utteranceLatch = CountDownLatch(1)
            val utteranceId = "hydrate-meds-${System.currentTimeMillis()}"
            readyEngine.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(utteranceId: String?) {
                        utteranceLatch.countDown()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        utteranceLatch.countDown()
                    }
                },
            )

            speakNow(readyEngine, prompt, utteranceId)
            utteranceLatch.await(6, TimeUnit.SECONDS)
        } finally {
            tts?.shutdown()
        }
    }

    private fun configure(tts: TextToSpeech) {
        tts.language = Locale.getDefault()
        tts.setSpeechRate(0.94f)
        tts.setPitch(1.02f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.setAudioAttributes(alarmSpeechAttributes())
        }
    }

    private fun alarmSpeechAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

    private fun speakNow(tts: TextToSpeech, prompt: String, utteranceId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(prompt, TextToSpeech.QUEUE_FLUSH, Bundle(), utteranceId)
        } else {
            @Suppress("DEPRECATION")
            tts.speak(prompt, TextToSpeech.QUEUE_FLUSH, null)
        }
    }
}
