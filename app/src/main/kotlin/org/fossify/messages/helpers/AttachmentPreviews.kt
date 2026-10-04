package org.fossify.messages.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.darkenColor
import org.fossify.commons.extensions.formatSize
import org.fossify.commons.extensions.getColorStateList
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
import org.fossify.messages.databinding.ItemAttachmentAudioBinding
import org.fossify.messages.databinding.ItemAttachmentAudioPreviewBinding
import org.fossify.messages.databinding.ItemAttachmentDocumentBinding
import org.fossify.messages.databinding.ItemAttachmentDocumentPreviewBinding
import org.fossify.messages.databinding.ItemAttachmentVcardBinding
import org.fossify.messages.databinding.ItemAttachmentVcardPreviewBinding
import org.fossify.messages.extensions.getFileSizeFromUri
import org.fossify.messages.extensions.isAudioMimeType
import org.fossify.messages.extensions.isCalendarMimeType
import org.fossify.messages.extensions.isPdfMimeType
import org.fossify.messages.extensions.isZipMimeType

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

@SuppressLint("ClickableViewAccessibility")
fun ItemAttachmentAudioBinding.setupAudio(
    uri: Uri,
    onSelect: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isSelecting: () -> Boolean = { false }
) {
    val previousListener = root.tag
    (previousListener as? AudioPlayerManager.AudioPlayerListener)?.let(AudioPlayerManager::detachListener)
    (previousListener as? View.OnAttachStateChangeListener)?.let(root::removeOnAttachStateChangeListener)

    val context = root.context
    val primaryColor = context.getProperPrimaryColor()
    var totalDurationMs = 0
    val playIcon = org.fossify.commons.R.drawable.ic_play_vector
    val pauseIcon = org.fossify.commons.R.drawable.ic_pause_vector

    duration.setTextColor(context.getProperTextColor())
    duration.text = formatMs(0)
    playButton.apply {
        setImageResource(playIcon)
        background.setTint(primaryColor)
        applyColorFilter(primaryColor.getContrastColor())
        contentDescription = context.getString(R.string.play)
    }
    progressBar.progress = 0
    progressBar.progressTintList = primaryColor.getColorStateList()
    progressBar.thumbTintList = primaryColor.getColorStateList()
    root.background.applyColorFilter(primaryColor.darkenColor())

    val audioListener = object : AudioPlayerManager.AudioPlayerListener, View.OnAttachStateChangeListener {
        override fun onPlaybackStateChanged(isPlaying: Boolean) {
            playButton.setImageResource(if (isPlaying) pauseIcon else playIcon)
            playButton.contentDescription = context.getString(if (isPlaying) R.string.pause else R.string.play)
        }

        override fun onProgressUpdated(positionMs: Int) {
            progressBar.progress = positionMs
            duration.text = formatMs(positionMs)
        }

        override fun onPlaybackCompleted() {
            progressBar.progress = 0
            duration.text = formatMs(totalDurationMs)
        }

        override fun onPlaybackError() {
            onPlaybackCompleted()
            context.toast(org.fossify.commons.R.string.unknown_error_occurred)
        }

        override fun onViewAttachedToWindow(view: View) {
            if (!AudioPlayerManager.attachListener(uri, this)) {
                onPlaybackStateChanged(false)
                onPlaybackCompleted()
            }
        }

        override fun onViewDetachedFromWindow(view: View) {
            AudioPlayerManager.detachListener(this)
        }
    }
    root.tag = audioListener
    root.addOnAttachStateChangeListener(audioListener)
    if (root.isAttachedToWindow) audioListener.onViewAttachedToWindow(root)

    AudioPlayerManager.getDurationMs(uri, context) { durationMs ->
        if (root.tag === audioListener) {
            totalDurationMs = durationMs
            progressBar.max = durationMs
            if (!root.isAttachedToWindow || !AudioPlayerManager.attachListener(uri, audioListener)) {
                duration.text = formatMs(durationMs)
            }
        }
    }

    root.setOnClickListener {
        if (isSelecting()) {
            onSelect?.invoke()
        } else {
            AudioPlayerManager.togglePlay(uri, progressBar.progress, context)
        }
    }
    playButton.setOnClickListener { root.performClick() }
    progressBar.setOnClickListener { root.performClick() }

    val longClickListener = onLongClick?.let { callback ->
        View.OnLongClickListener {
            callback()
            true
        }
    }
    root.setOnLongClickListener(longClickListener)
    playButton.setOnLongClickListener(longClickListener)
    progressBar.setOnLongClickListener(longClickListener)
    progressBar.setOnTouchListener { view, event ->
        if (isSelecting()) {
            if (event.action == MotionEvent.ACTION_UP) {
                view.performClick()
            }
            true
        } else {
            false
        }
    }
    progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                duration.text = formatMs(progress)
                AudioPlayerManager.seekTo(uri, progress)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    })
}

fun ItemAttachmentVcardPreviewBinding.setupVCardPreview(
    activity: Activity,
    uri: Uri,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onRemoveButtonClicked: (() -> Unit)? = null,
) {
    vcardProgress.beVisible()
    vcardAttachmentHolder.setupVCardPreview(
        activity = activity,
        uri = uri,
        attachment = true,
        onClick = onClick,
        onLongClick = onLongClick
    ) {
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
                vcardSubtitle.text =
                    context.resources.getQuantityString(R.plurals.and_other_contacts, quantity, quantity)
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
    mimeType.isZipMimeType() -> R.drawable.ic_vector_folder_zip
    else -> R.drawable.ic_document_vector
}
