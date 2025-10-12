package org.fossify.messages.adapters

import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.*
import org.fossify.messages.R
import org.fossify.messages.activities.VCardViewerActivity
import org.fossify.messages.databinding.ItemAttachmentDocumentPreviewBinding
import org.fossify.messages.databinding.ItemAttachmentMediaPreviewBinding
import org.fossify.messages.databinding.ItemAttachmentVcardPreviewBinding
import org.fossify.messages.extensions.*
import org.fossify.messages.helpers.*
import org.fossify.messages.models.AttachmentSelection

class AttachmentsAdapter(
    val activity: BaseSimpleActivity,
    val recyclerView: RecyclerView,
    val onAttachmentsRemoved: () -> Unit,
    val onReady: (() -> Unit)
) : ListAdapter<AttachmentSelection, AttachmentsAdapter.AttachmentsViewHolder>(AttachmentDiffCallback()) {

    private val config = activity.config
    private val resources = activity.resources
    private val primaryColor = activity.getProperPrimaryColor()
    private val imageCompressor by lazy { ImageCompressor(activity) }

    val attachments = mutableListOf<AttachmentSelection>()

    override fun getItemViewType(position: Int): Int {
        return getItem(position).viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttachmentsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = when (viewType) {
            ATTACHMENT_DOCUMENT -> ItemAttachmentDocumentPreviewBinding.inflate(inflater, parent, false)
            ATTACHMENT_VCARD -> ItemAttachmentVcardPreviewBinding.inflate(inflater, parent, false)
            ATTACHMENT_MEDIA -> ItemAttachmentMediaPreviewBinding.inflate(inflater, parent, false)
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }

        return AttachmentsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttachmentsViewHolder, position: Int) {
        val attachment = getItem(position)
        holder.bindView { binding, _ ->
            when (attachment.viewType) {
                ATTACHMENT_DOCUMENT -> {
                    (binding as ItemAttachmentDocumentPreviewBinding).setupDocumentPreview(
                        uri = attachment.uri,
                        title = attachment.filename,
                        mimeType = attachment.mimetype,
                        onClick = { activity.launchViewIntent(attachment.uri, attachment.mimetype, attachment.filename) },
                        onRemoveButtonClicked = { removeAttachment(attachment) }
                    )
                }

                ATTACHMENT_VCARD -> {
                    (binding as ItemAttachmentVcardPreviewBinding).setupVCardPreview(
                        activity = activity,
                        uri = attachment.uri,
                        onClick = {
                            val intent = Intent(activity, VCardViewerActivity::class.java).also {
                                it.putExtra(EXTRA_VCARD_URI, attachment.uri)
                            }
                            activity.startActivity(intent)
                        },
                        onRemoveButtonClicked = { removeAttachment(attachment) }
                    )
                }

                ATTACHMENT_MEDIA -> setupMediaPreview(
                    binding = binding as ItemAttachmentMediaPreviewBinding,
                    attachment = attachment
                )
            }
        }
    }

    fun clear() {
        attachments.clear()
        submitList(emptyList())
        recyclerView.onGlobalLayout {
            onAttachmentsRemoved()
        }
    }

    fun addAttachment(attachment: AttachmentSelection) {
        attachments.removeAll { AttachmentSelection.areItemsTheSame(it, attachment) }
        attachments.add(attachment)
        submitList(attachments.toList())
    }

    private fun removeAttachment(attachment: AttachmentSelection) {
        attachments.removeAll { AttachmentSelection.areItemsTheSame(it, attachment) }
        if (attachments.isEmpty()) {
            clear()
        } else {
            submitList(attachments.toList())
        }
    }

    private fun setupMediaPreview(binding: ItemAttachmentMediaPreviewBinding, attachment: AttachmentSelection) {
        binding.apply {
            mediaAttachmentHolder.background.applyColorFilter(primaryColor.darkenColor())
            mediaAttachmentHolder.setOnClickListener {
                activity.launchViewIntent(attachment.uri, attachment.mimetype, attachment.filename)
            }

            removeAttachmentButtonHolder.removeAttachmentButton.apply {
                beVisible()
                background.applyColorFilter(primaryColor)
                setOnClickListener {
                    removeAttachment(attachment)
                }
            }

            val compressImage = attachment.mimetype.isImageMimeType() && !attachment.mimetype.isGifMimeType()
            if (compressImage && attachment.isPending && config.mmsFileSizeLimit != FILE_SIZE_NONE) {
                thumbnail.beGone()
                compressionProgress.beVisible()

                imageCompressor.compressImage(attachment.uri, config.mmsFileSizeLimit) { compressedUri ->
                    activity.runOnUiThread {
                        when (compressedUri) {
                            attachment.uri -> {
                                attachments.find { it.uri == attachment.uri }?.isPending = false
                                loadMediaPreview(this, attachment)
                            }

                            null -> {
                                activity.toast(R.string.compress_error)
                                removeAttachment(attachment)
                            }

                            else -> {
                                attachments.remove(attachment)
                                addAttachment(attachment.copy(uri = compressedUri, isPending = false))
                            }
                        }
                        onReady()
                    }
                }
            } else {
                if (attachment.isPending) {
                    attachments.find { it.uri == attachment.uri }?.isPending = false
                    onReady()
                }
                loadMediaPreview(this, attachment)
            }
        }
    }

    private fun loadMediaPreview(binding: ItemAttachmentMediaPreviewBinding, attachment: AttachmentSelection) {
        val roundedCornersRadius = resources.getDimension(org.fossify.commons.R.dimen.activity_margin).toInt()
        val size = resources.getDimension(R.dimen.attachment_preview_size).toInt()

        val options = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .transform(CenterCrop(), RoundedCorners(roundedCornersRadius))

        Glide.with(binding.thumbnail)
            .load(attachment.uri)
            .transition(DrawableTransitionOptions.withCrossFade())
            .override(size, size)
            .apply(options)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                    removeAttachment(attachment)
                    activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                    return false
                }

                override fun onResourceReady(dr: Drawable, a: Any, t: Target<Drawable>, d: DataSource, i: Boolean): Boolean {
                    binding.thumbnail.beVisible()
                    binding.playIcon.beVisibleIf(attachment.mimetype.isVideoMimeType())
                    binding.compressionProgress.beGone()
                    return false
                }
            })
            .into(binding.thumbnail)
    }

    inner class AttachmentsViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindView(callback: (binding: ViewBinding, adapterPosition: Int) -> Unit) {
            callback(binding, adapterPosition)
        }
    }
}

private class AttachmentDiffCallback : DiffUtil.ItemCallback<AttachmentSelection>() {
    override fun areItemsTheSame(oldItem: AttachmentSelection, newItem: AttachmentSelection): Boolean {
        return AttachmentSelection.areItemsTheSame(oldItem, newItem)
    }

    override fun areContentsTheSame(oldItem: AttachmentSelection, newItem: AttachmentSelection): Boolean {
        return AttachmentSelection.areContentsTheSame(oldItem, newItem)
    }
}
