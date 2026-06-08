package org.fossify.messages.helpers

import android.app.Activity
import android.net.Uri
import android.view.View
import android.widget.SeekBar
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
import org.fossify.messages.databinding.ItemAttachmentAudioBinding
import org.fossify.messages.databinding.ItemAttachmentAudioPreviewBinding
import org.fossify.messages.databinding.ItemAttachmentDocumentBinding
import org.fossify.messages.databinding.ItemAttachmentDocumentPreviewBinding
import org.fossify.messages.databinding.ItemAttachmentVcardBinding
import org.fossify.messages.databinding.ItemAttachmentVcardPreviewBinding
import org.fossify.messages.extensions.*

fun ItemAttachmentDocumentPreviewBinding.setupDocumentPreview(
    uri: Uri,
    title: String,
    mimeType: String,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onRemoveButtonClicked: (() -> Unit)? = null
) {
    documentAttachmentHolder.setupDocumentPreview(uri, title, mimeType, onClick, onLongClick)
    removeAttachmentButtonHolder.removeAttachmentButton.apply {
        beVisible()
        background.applyColorFilter(context.getProperPrimaryColor())
        if (onRemoveButtonClicked != null) {
            setOnClickListener {
                onRemoveButtonClicked.invoke()
            }
        }
    }
}

fun ItemAttachmentDocumentBinding.setupDocumentPreview(
    uri: Uri,
    title: String,
    mimeType: String,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val context = root.context
    if (title.isNotEmpty()) {
        filename.text = title
    }

    ensureBackgroundThread {
        try {
            val size = context.getFileSizeFromUri(uri)
            root.post {
                fileSize.beVisible()
                fileSize.text = size.formatSize()
            }
        } catch (_: Exception) {
            root.post {
                fileSize.beGone()
            }
        }
    }

    val textColor = context.getProperTextColor()
    val primaryColor = context.getProperPrimaryColor()

    filename.setTextColor(textColor)
    fileSize.setTextColor(textColor)

    icon.setImageResource(getIconResourceForMimeType(mimeType))
    icon.background.setTint(primaryColor)
    root.background.applyColorFilter(primaryColor.darkenColor())

    root.setOnClickListener {
        onClick?.invoke()
    }

    root.setOnLongClickListener {
        onLongClick?.invoke()
        true
    }
}

fun ItemAttachmentAudioPreviewBinding.setupAudioPreview(
    uri: Uri,
    onRemoveButtonClicked: (() -> Unit)? = null,
) {
    audioAttachmentHolder.setupAudio(uri)
    removeAttachmentButtonHolder.removeAttachmentButton.apply {
        beVisible()
        background.applyColorFilter(context.getProperPrimaryColor())
        if (onRemoveButtonClicked != null) {
            setOnClickListener {
                onRemoveButtonClicked.invoke()
            }
        }
    }
}

fun ItemAttachmentAudioBinding.setupAudio(
    uri: Uri,
) {
    val context = root.context

    val textColor = context.getProperTextColor()
    val primaryColor = context.getProperPrimaryColor()
    var playIcon = org.fossify.commons.R.drawable.ic_play_vector
    var pauseIcon = org.fossify.commons.R.drawable.ic_pause_vector

    duration.setTextColor(textColor)
    playButton.apply {
        background.setTint(primaryColor)
        contentDescription = context.getString(R.string.play)
    }
    progressBar.progressDrawable.setTint(primaryColor)
    progressBar.thumb.setTint(primaryColor)

    root.background.applyColorFilter(primaryColor.darkenColor())

    AudioPlayerManager.getDurationMs(uri, context) { durationMs ->
        progressBar.max = durationMs.toInt()
        duration.text = formatMs(durationMs.toInt())
    }

    val audioListener = object : AudioPlayerManager.AudioPlayerListener {
        override fun onPlaybackStateChanged(isPlaying: Boolean) {
            playButton.apply {
                setImageResource(if (isPlaying) pauseIcon else playIcon)
                contentDescription = context.getString(if (isPlaying) R.string.pause else R.string.play)
            }
        }

        override fun onProgressUpdated(positionMs: Int) {
            progressBar.progress = positionMs
            duration.text = formatMs(positionMs)
        }

        override fun onPlaybackCompleted() {
            playButton.apply {
                setImageResource(playIcon)
                contentDescription = context.getString(R.string.play)
            }
            progressBar.progress = 0
        }

        override fun onPlaybackError() {
            playButton.apply {
                setImageResource(playIcon)
                contentDescription = context.getString(R.string.play)
            }
        }
    }

    playButton.setOnClickListener {
        AudioPlayerManager.togglePlay(uri, progressBar.progress, context, audioListener)
    }

    root.setOnClickListener {
        AudioPlayerManager.togglePlay(uri, progressBar.progress, context, audioListener)
    }

    progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser && AudioPlayerManager.isActive(audioListener)) {
                AudioPlayerManager.seekTo(progress)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            // Not required for this implementation
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            if (AudioPlayerManager.isActive(audioListener)) {
                AudioPlayerManager.seekTo(progressBar.progress)
            }
        }
    })

    progressBar.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        val minBarWidth = context.resources.getDimensionPixelSize(R.dimen.audio_seekbar_min_width)
        progressBar.beVisibleIf(progressBar.width >= minBarWidth)
    }

    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            AudioPlayerManager.attachListener(uri, audioListener)
        }
        override fun onViewDetachedFromWindow(v: View) {
            AudioPlayerManager.detachListener(audioListener)
        }
    })

    AudioPlayerManager.attachListener(uri, audioListener)
}

fun ItemAttachmentVcardPreviewBinding.setupVCardPreview(
    activity: Activity,
    uri: Uri,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onRemoveButtonClicked: (() -> Unit)? = null,
) {
    vcardProgress.beVisible()
    vcardAttachmentHolder.setupVCardPreview(activity = activity, uri = uri, attachment = true, onClick = onClick, onLongClick = onLongClick) {
        vcardProgress.beGone()
        removeAttachmentButtonHolder.removeAttachmentButton.apply {
            beVisible()
            background.applyColorFilter(activity.getProperPrimaryColor())
            if (onRemoveButtonClicked != null) {
                setOnClickListener {
                    onRemoveButtonClicked.invoke()
                }
            }
        }
    }
}

fun ItemAttachmentVcardBinding.setupVCardPreview(
    activity: Activity,
    uri: Uri,
    attachment: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onVCardLoaded: (() -> Unit)? = null,
) {
    val context = root.context
    val textColor = activity.getProperTextColor()
    val primaryColor = activity.getProperPrimaryColor()

    root.background.applyColorFilter(primaryColor.darkenColor())
    vcardTitle.setTextColor(textColor)
    vcardSubtitle.setTextColor(textColor)

    arrayOf<View>(vcardPhoto, vcardTitle, vcardSubtitle, viewContactDetails).forEach {
        it.beGone()
    }

    parseVCardFromUri(activity, uri) { vCards ->
        activity.runOnUiThread {
            if (vCards.isEmpty()) {
                vcardTitle.beVisible()
                vcardTitle.text = context.getString(org.fossify.commons.R.string.unknown_error_occurred)
                return@runOnUiThread
            }

            val title = vCards.firstOrNull()?.parseNameFromVCard()
            val imageIcon = if (title != null) {
                SimpleContactsHelper(activity).getContactLetterIcon(title)
            } else {
                null
            }

            arrayOf<View>(vcardPhoto, vcardTitle).forEach {
                it.beVisible()
            }

            vcardPhoto.setImageBitmap(imageIcon)
            vcardTitle.text = title

            if (vCards.size > 1) {
                vcardSubtitle.beVisible()
                val quantity = vCards.size - 1
                vcardSubtitle.text = context.resources.getQuantityString(R.plurals.and_other_contacts, quantity, quantity)
            } else {
                vcardSubtitle.beGone()
            }

            if (attachment) {
                onVCardLoaded?.invoke()
            } else {
                viewContactDetails.setTextColor(primaryColor)
                viewContactDetails.beVisible()
            }

            vcardAttachmentHolder.setOnClickListener {
                onClick?.invoke()
            }
            vcardAttachmentHolder.setOnLongClickListener {
                onLongClick?.invoke()
                true
            }
        }
    }
}

private const val SECONDS_PER_MINUTE = 60

private fun formatMs(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return "%d:%02d".format(minutes, seconds)
}

private fun getIconResourceForMimeType(mimeType: String) = when {
    mimeType.isAudioMimeType() -> R.drawable.ic_vector_audio_file
    mimeType.isCalendarMimeType() -> R.drawable.ic_calendar_month_vector
    mimeType.isPdfMimeType() -> R.drawable.ic_vector_pdf
    mimeType.endsWith("zip", ignoreCase = true) -> R.drawable.ic_vector_folder_zip
    else -> R.drawable.ic_document_vector
}
