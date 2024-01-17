package org.fossify.messages.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.messages.databinding.DialogDeleteConfirmationBinding

class DeleteConfirmationDialog(
    private val activity: Activity,
    private val message: String,
    private val showSkipRecycleBinOption: Boolean,
    private val callback: (skipRecycleBin: Boolean) -> Unit
) {

    private var dialog: AlertDialog? = null
    val binding = DialogDeleteConfirmationBinding.inflate(activity.layoutInflater)

    init {
        binding.deleteRememberTitle.text = message
        binding.skipTheRecycleBinCheckbox.beGoneIf(!showSkipRecycleBinOption)
        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.yes) { _, _ -> dialogConfirmed() }
            .setNegativeButton(org.fossify.commons.R.string.no, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun dialogConfirmed() {
        dialog?.dismiss()
        callback(binding.skipTheRecycleBinCheckbox.isChecked)
    }
}
