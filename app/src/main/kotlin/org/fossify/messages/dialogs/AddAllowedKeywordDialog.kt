package org.fossify.messages.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.value
import org.fossify.messages.databinding.DialogAddAllowedKeywordBinding
import org.fossify.messages.extensions.config

class AddAllowedKeywordDialog(
    val activity: BaseSimpleActivity,
    private val originalKeyword: String? = null,
    val callback: () -> Unit
) {
    init {
        val binding = DialogAddAllowedKeywordBinding.inflate(activity.layoutInflater).apply {
            if (originalKeyword != null) {
                addAllowedKeywordEdittext.setText(originalKeyword)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    alertDialog.showKeyboard(binding.addAllowedKeywordEdittext)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newAllowedKeyword = binding.addAllowedKeywordEdittext.value
                        if (originalKeyword != null && newAllowedKeyword != originalKeyword) {
                            activity.config.removeAllowedKeyword(originalKeyword)
                        }

                        if (newAllowedKeyword.isNotEmpty()) {
                            activity.config.addAllowedKeyword(newAllowedKeyword)
                        }

                        callback()
                        alertDialog.dismiss()
                    }
                }
            }
    }
}
