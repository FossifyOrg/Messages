package org.fossify.messages.helpers

import android.app.Activity
import android.net.Uri
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
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
        } catch (e: Exception) {
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

    arrayOf(vcardPhoto, vcardTitle, vcardSubtitle, viewContactDetails).forEach {
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

            arrayOf(vcardPhoto, vcardTitle).forEach {
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

private fun getIconResourceForMimeType(mimeType: String) = when {
    mimeType.isAudioMimeType() -> R.drawable.ic_vector_audio_file
    mimeType.isCalendarMimeType() -> R.drawable.ic_calendar_month_vector
    mimeType.isPdfMimeType() -> R.drawable.ic_vector_pdf
    mimeType.isZipMimeType() -> R.drawable.ic_vector_folder_zip
    else -> R.drawable.ic_document_vector
}
