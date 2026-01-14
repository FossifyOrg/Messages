package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import org.fossify.commons.extensions.notificationManager
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.extensions.conversationsDB
import org.fossify.messages.extensions.markThreadMessagesRead
import org.fossify.messages.helpers.NotificationHelper
import org.fossify.messages.helpers.refreshConversations

class CopyCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.ACTION_COPY_CODE) {
            val code = intent.getStringExtra(NotificationHelper.EXTRA_CODE)
            val threadId = intent.getLongExtra(NotificationHelper.EXTRA_THREAD_ID, 0L)

            // 复制验证码到剪贴板
            if (!code.isNullOrEmpty()) {
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("verification_code", code)
                clipboardManager.setPrimaryClip(clipData)
            }

            // 标记为已读（与 MarkAsReadReceiver 相同的逻辑）
            if (threadId != 0L) {
                context.notificationManager.cancel(threadId.hashCode())
                ensureBackgroundThread {
                    context.markThreadMessagesRead(threadId)
                    context.conversationsDB.markRead(threadId)
                    refreshConversations()
                }
            }
        }
    }
}

