package org.fossify.messages.helpers

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import org.fossify.commons.helpers.ensureBackgroundThread

object AudioPlayerManager {
    private const val PROGRESS_INTERVAL_MS = 200L

    interface AudioPlayerListener {
        fun onPlaybackStateChanged(isPlaying: Boolean)
        fun onProgressUpdated(positionMs: Int)
        fun onPlaybackCompleted()
        fun onPlaybackError()
    }

    private var mediaPlayer: MediaPlayer? = null
    private var currentUri: Uri? = null
    private val listeners = mutableMapOf<AudioPlayerListener, Uri>()
    private val currentListeners get() = listeners.filterValues { it == currentUri }.keys
    private var audioManager: AudioManager? = null
    private var noisyReceiverContext: Application? = null
    private var isPreparing = false
    private var pendingSeekPositionMs = 0
    private val handler = Handler(Looper.getMainLooper())
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(audioAttributes)
        .setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener({ focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> pause()
            }
        }, handler)
        .build()
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) pause()
        }
    }

    private val progressRunnable = object : Runnable {
        override fun run() {
            val player = mediaPlayer ?: return
            if (isPreparing) return

            try {
                val position = player.currentPosition
                currentListeners.forEach { it.onProgressUpdated(position) }
                handler.postDelayed(this, PROGRESS_INTERVAL_MS)
            } catch (_: IllegalStateException) {
                playbackFailed()
            }
        }
    }

    fun togglePlay(uri: Uri, positionMs: Int, context: Context) {
        val appContext = context.applicationContext
        val player = mediaPlayer
        if (currentUri == uri && player != null) {
            if (isPreparing) {
                release()
            } else if (player.isPlaying) {
                pause()
            } else {
                startPlayback(appContext)
            }
            return
        }

        release()
        currentUri = uri
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        prepareAndPlay(uri, positionMs, appContext)
    }

    fun seekTo(uri: Uri, positionMs: Int) {
        if (currentUri != uri) return
        try {
            if (isPreparing) {
                pendingSeekPositionMs = positionMs
            } else {
                mediaPlayer?.seekTo(positionMs)
            }
            currentListeners.forEach { it.onProgressUpdated(positionMs) }
        } catch (_: IllegalStateException) {
            playbackFailed()
        }
    }

    fun getDurationMs(uri: Uri, context: Context, onResult: (Int) -> Unit) {
        val appContext = context.applicationContext
        ensureBackgroundThread {
            val duration = try {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(appContext, uri)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull() ?: 0
                } finally {
                    retriever.release()
                }
            } catch (_: Exception) {
                0
            }
            handler.post { onResult(duration) }
        }
    }

    fun release(uri: Uri? = null) {
        if (uri != null && currentUri != uri) return
        handler.removeCallbacks(progressRunnable)
        val player = mediaPlayer
        val activeListeners = currentListeners
        mediaPlayer = null
        currentUri = null
        isPreparing = false
        pendingSeekPositionMs = 0
        player?.release()
        noisyReceiverContext?.unregisterReceiver(noisyReceiver)
        noisyReceiverContext = null
        audioManager?.abandonAudioFocusRequest(focusRequest)
        audioManager = null
        activeListeners.forEach { it.onPlaybackStateChanged(false) }
    }

    fun detachListener(listener: AudioPlayerListener) {
        listeners.remove(listener)
        if (currentListeners.isEmpty()) handler.removeCallbacks(progressRunnable)
    }

    fun attachListener(uri: Uri, listener: AudioPlayerListener): Boolean {
        listeners[listener] = uri
        val player = mediaPlayer ?: return false
        if (currentUri != uri) return false
        return try {
            val playing = !isPreparing && player.isPlaying
            listener.onPlaybackStateChanged(playing)
            listener.onProgressUpdated(if (isPreparing) pendingSeekPositionMs else player.currentPosition)
            handler.removeCallbacks(progressRunnable)
            if (playing) handler.post(progressRunnable)
            true
        } catch (_: IllegalStateException) {
            playbackFailed()
            false
        }
    }

    private fun prepareAndPlay(uri: Uri, positionMs: Int, context: Context) {
        try {
            val player = MediaPlayer()
            mediaPlayer = player
            isPreparing = true
            pendingSeekPositionMs = positionMs
            player.apply {
                setAudioAttributes(audioAttributes)
                setOnPreparedListener {
                    isPreparing = false
                    it.seekTo(pendingSeekPositionMs)
                    currentListeners.forEach { listener -> listener.onProgressUpdated(pendingSeekPositionMs) }
                    startPlayback(context)
                }
                setOnCompletionListener {
                    val activeListeners = currentListeners
                    release()
                    activeListeners.forEach { listener -> listener.onPlaybackCompleted() }
                }
                setOnErrorListener { _, _, _ ->
                    playbackFailed()
                    true
                }
                setDataSource(context, uri)
                prepareAsync()
            }
        } catch (_: Exception) {
            playbackFailed()
        }
    }

    private fun startPlayback(context: Context) {
        if (audioManager?.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            playbackFailed()
            return
        }
        val appContext = context.applicationContext as Application
        ContextCompat.registerReceiver(
            appContext,
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        noisyReceiverContext = appContext
        try {
            mediaPlayer?.start()
            currentListeners.forEach { it.onPlaybackStateChanged(true) }
            handler.removeCallbacks(progressRunnable)
            if (currentListeners.isNotEmpty()) handler.post(progressRunnable)
        } catch (_: IllegalStateException) {
            playbackFailed()
        }
    }

    private fun pause() {
        if (isPreparing) {
            release()
            return
        }
        try {
            mediaPlayer?.pause()
            handler.removeCallbacks(progressRunnable)
            noisyReceiverContext?.unregisterReceiver(noisyReceiver)
            noisyReceiverContext = null
            audioManager?.abandonAudioFocusRequest(focusRequest)
            currentListeners.forEach { it.onPlaybackStateChanged(false) }
        } catch (_: IllegalStateException) {
            playbackFailed()
        }
    }

    private fun playbackFailed() {
        val activeListeners = currentListeners
        release()
        activeListeners.forEach { it.onPlaybackError() }
    }
}
