package org.fossify.messages.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.value
import org.fossify.messages.databinding.DialogAddBlockedKeywordBinding
import org.fossify.messages.extensions.config

class AddBlockedKeywordDialog(val activity: BaseSimpleActivity, private val originalKeyword: String? = null, val callback: () -> Unit) {
    init {
        val binding = DialogAddBlockedKeywordBinding.inflate(activity.layoutInflater).apply {
            if (originalKeyword != null) {
                addBlockedKeywordEdittext.setText(originalKeyword)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    alertDialog.showKeyboard(binding.addBlockedKeywordEdittext)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newBlockedKeyword = binding.addBlockedKeywordEdittext.value
                        if (originalKeyword != null && newBlockedKeyword != originalKeyword) {
                            activity.config.removeBlockedKeyword(originalKeyword)
                        }

                        if (newBlockedKeyword.isNotEmpty()) {
                            activity.config.addBlockedKeyword(newBlockedKeyword)
                        }

                        callback()
                        alertDialog.dismiss()
                    }
                }
            }
    }
}
