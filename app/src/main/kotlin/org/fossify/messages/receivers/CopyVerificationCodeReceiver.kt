package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.fossify.messages.R
import org.fossify.messages.helpers.COPY_VERIFICATION_CODE
import org.fossify.messages.helpers.EXTRA_VERIFICATION_CODE

class CopyVerificationCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            COPY_VERIFICATION_CODE -> {
                val verificationCode = intent.getStringExtra(EXTRA_VERIFICATION_CODE)
                if (!verificationCode.isNullOrEmpty()) {
                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText("verification_code", verificationCode)
                    clipboardManager.setPrimaryClip(clipData)
                    Toast.makeText(context, R.string.verification_code_copied, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

