package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import org.fossify.messages.helpers.CODE_TO_COPY

class CopyCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val code = intent.getStringExtra(CODE_TO_COPY) ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("code", code))
    }
}
