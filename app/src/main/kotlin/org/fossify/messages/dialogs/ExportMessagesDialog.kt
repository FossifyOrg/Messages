package org.fossify.messages.dialogs

import android.annotation.SuppressLint
import android.net.Uri
import android.provider.DocumentsContract
import androidx.appcompat.app.AlertDialog
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getCurrentFormattedDateTime
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.isAValidFilename
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.value
import org.fossify.commons.helpers.MEDIUM_ALPHA
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
import org.fossify.messages.activities.SimpleActivity
import org.fossify.messages.databinding.DialogExportMessagesBinding
import org.fossify.messages.extensions.config
import org.fossify.messages.helpers.MessagesReader

class ExportMessagesDialog(
    private val activity: SimpleActivity,
    private val callback: (fileName: String) -> Unit,
) {
    private val config = activity.config
    private var dialog: AlertDialog? = null

    @SuppressLint("SetTextI18n")
    private val binding = DialogExportMessagesBinding.inflate(activity.layoutInflater).apply {
        exportSmsCheckbox.isChecked = config.exportSms
        exportMmsCheckbox.isChecked = config.exportMms
        exportMessagesFilename.setText(
            "${activity.getString(R.string.messages)}_${activity.getCurrentFormattedDateTime()}"
        )
    }

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(
                    view = binding.root,
                    dialog = this,
                    titleId = R.string.export_messages
                ) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        config.exportSms = binding.exportSmsCheckbox.isChecked
                        config.exportMms = binding.exportMmsCheckbox.isChecked
                        val filename = binding.exportMessagesFilename.value
                        when {
                            filename.isEmpty() -> activity.toast(org.fossify.commons.R.string.empty_name)
                            filename.isAValidFilename() -> callback(filename)
                            else -> activity.toast(org.fossify.commons.R.string.invalid_name)
                        }
                    }
                }
            }
    }

    fun exportMessages(uri: Uri) {
        dialog!!.apply {
            setCanceledOnTouchOutside(false)
            arrayOf(
                binding.exportMmsCheckbox,
                binding.exportSmsCheckbox,
                getButton(AlertDialog.BUTTON_POSITIVE),
                getButton(AlertDialog.BUTTON_NEGATIVE)
            ).forEach {
                it.isEnabled = false
                it.alpha = MEDIUM_ALPHA
            }

            binding.exportProgress.setIndicatorColor(activity.getProperPrimaryColor())
            binding.exportProgress.post {
                binding.exportProgress.show()
            }
            export(uri)
        }
    }

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    private fun export(uri: Uri) {
        ensureBackgroundThread {
            var success = false
            try {
                MessagesReader(activity).getMessagesToExport(
                    getSms = config.exportSms,
                    getMms = config.exportMms
                ) { messagesToExport ->
                    if (messagesToExport.isEmpty()) {
                        activity.toast(org.fossify.commons.R.string.no_entries_for_exporting)
                        dismiss()
                        return@getMessagesToExport
                    }
                    val json = Json { encodeDefaults = true }
                    activity.contentResolver.openOutputStream(uri)!!.buffered()
                        .use { outputStream ->
                            json.encodeToStream(messagesToExport, outputStream)
                        }
                    success = true
                    activity.toast(org.fossify.commons.R.string.exporting_successful)
                }
            } catch (e: Throwable) {
                activity.showErrorToast(e.toString())
            } finally {
                if (!success) {
                    // delete the file to avoid leaving behind an empty/corrupt file
                    try {
                        DocumentsContract.deleteDocument(activity.contentResolver, uri)
                    } catch (_: Exception) {
                        // ignored because we don't want to show two error messages
                    }
                }

                dismiss()
            }
        }
    }

    private fun dismiss() = dialog?.dismiss()
}
