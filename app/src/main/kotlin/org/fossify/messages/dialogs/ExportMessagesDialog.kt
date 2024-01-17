package org.fossify.messages.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.*
import org.fossify.messages.R
import org.fossify.messages.activities.SimpleActivity
import org.fossify.messages.databinding.DialogExportMessagesBinding
import org.fossify.messages.extensions.config

class ExportMessagesDialog(
    private val activity: SimpleActivity,
    private val callback: (fileName: String) -> Unit,
) {
    private val config = activity.config

    init {
        val binding = DialogExportMessagesBinding.inflate(activity.layoutInflater).apply {
            exportSmsCheckbox.isChecked = config.exportSms
            exportMmsCheckbox.isChecked = config.exportMms
            exportMessagesFilename.setText(
                activity.getString(R.string.messages) + "_" + activity.getCurrentFormattedDateTime()
            )
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.export_messages) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        config.exportSms = binding.exportSmsCheckbox.isChecked
                        config.exportMms = binding.exportMmsCheckbox.isChecked
                        val filename = binding.exportMessagesFilename.value
                        when {
                            filename.isEmpty() -> activity.toast(org.fossify.commons.R.string.empty_name)
                            filename.isAValidFilename() -> {
                                callback(filename)
                                alertDialog.dismiss()
                            }

                            else -> activity.toast(org.fossify.commons.R.string.invalid_name)
                        }
                    }
                }
            }
    }
}
