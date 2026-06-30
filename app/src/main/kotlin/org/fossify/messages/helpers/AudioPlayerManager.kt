package org.fossify.messages.helpers

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper

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
    private var activeListener: AudioPlayerListener? = null
    private val handler = Handler(Looper.getMainLooper())
    private var durationMs = 0

    private val progressRunnable = object : Runnable {
        override fun run() {
            val mp = mediaPlayer ?: return

            try {
                val position = mp.currentPosition
                activeListener?.onProgressUpdated(position)
            } catch (_: Exception) {
            }

            handler.postDelayed(this, PROGRESS_INTERVAL_MS)
        }
    }

    fun togglePlay(uri: Uri, positionMs: Int, context: Context, listener: AudioPlayerListener) {
        val sameUri = currentUri == uri

        if (sameUri && isPlaying()) {
            try {
                mediaPlayer?.pause()
                activeListener?.onPlaybackStateChanged(false)
                handler.removeCallbacks(progressRunnable)
            } catch (_: Exception) {
            }
            return
        }

        val isPaused = mediaPlayer != null && currentUri != null && !isPlaying()

        if (sameUri && isPaused) {
            try {
                mediaPlayer?.start()
                activeListener?.onPlaybackStateChanged(true)
                handler.post(progressRunnable)
            } catch (_: Exception) {
            }
            return
        }

        activeListener?.onPlaybackStateChanged(false)
        stopAndRelease()
        currentUri = uri
        activeListener = listener
        prepareAndPlay(uri, positionMs, context)
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            val position = mediaPlayer?.currentPosition ?: positionMs
            activeListener?.onProgressUpdated(position)
        } catch (_: Exception) {
        }
    }

    fun getDurationMs(uri: Uri, context: Context, onResult: (Long) -> Unit) {
        Thread {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                retriever.release()
                val durationMs = durationStr?.toLongOrNull() ?: 0L
                handler.post { onResult(durationMs) }
            } catch (_: Exception) {
                handler.post { onResult(0L) }
            }
        }.start()
    }

    fun release() {
        stopAndRelease()
    }

    fun detachListener(listener: AudioPlayerListener) {
        if (activeListener === listener) {
            activeListener = null
            handler.removeCallbacks(progressRunnable)
        }
    }

    fun attachListener(uri: Uri, listener: AudioPlayerListener): Boolean {
        if (currentUri == uri && mediaPlayer != null) {
            activeListener = listener
            val playing = isPlaying()
            listener.onPlaybackStateChanged(playing)
            if (playing) {
                val position = try {
                    mediaPlayer?.currentPosition ?: 0
                } catch (_: Exception) {
                    0
                }
                listener.onProgressUpdated(position)
                handler.post(progressRunnable)
            }
            return true
        }
        return false
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }

    fun isActive(listener: AudioPlayerListener): Boolean = activeListener === listener

    private fun prepareAndPlay(uri: Uri, positionMs: Int, context: Context) {
        try {
            val mp = MediaPlayer().apply {
                setOnPreparedListener {
                    durationMs = it.duration
                    activeListener?.onProgressUpdated(positionMs)
                    it.seekTo(positionMs)
                    it.start()
                    activeListener?.onPlaybackStateChanged(true)
                    handler.post(progressRunnable)
                }
                setOnCompletionListener {
                    activeListener?.onPlaybackCompleted()
                    stopAndRelease()
                }
                setOnErrorListener { _, _, _ ->
                    activeListener?.onPlaybackError()
                    stopAndRelease()
                    true
                }
                setDataSource(context, uri)
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (_: Exception) {
            activeListener?.onPlaybackError()
            stopAndRelease()
        }
    }

    private fun stopAndRelease() {
        handler.removeCallbacks(progressRunnable)
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (_: Exception) {
        }
        mediaPlayer = null
        currentUri = null
        activeListener = null
        durationMs = 0
    }

}
