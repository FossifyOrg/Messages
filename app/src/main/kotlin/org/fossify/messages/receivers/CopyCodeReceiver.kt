package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import org.fossify.messages.helpers.NotificationHelper

class CopyCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.ACTION_COPY_CODE) {
            val code = intent.getStringExtra(NotificationHelper.EXTRA_CODE)
            if (!code.isNullOrEmpty()) {
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("verification_code", code)
                clipboardManager.setPrimaryClip(clipData)
            }
        }
    }
}

