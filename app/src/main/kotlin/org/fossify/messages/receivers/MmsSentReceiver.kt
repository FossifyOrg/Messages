package org.fossify.messages.receivers

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.Telephony
import android.widget.Toast
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.messages.R
import org.fossify.messages.extensions.deleteMessage
import org.fossify.messages.helpers.refreshConversations
import org.fossify.messages.helpers.refreshMessages
import java.io.File

/** Handles updating databases and states when a MMS message is sent. */
class MmsSentReceiver : SendStatusReceiver() {

    override fun updateAndroidDatabase(context: Context, intent: Intent, receiverResultCode: Int) {
        val uri = Uri.parse(intent.getStringExtra(EXTRA_CONTENT_URI))
        val originalResentMessageId = intent.getLongExtra(EXTRA_ORIGINAL_RESENT_MESSAGE_ID, -1L)
        val messageBox = if (receiverResultCode == Activity.RESULT_OK) {
            Telephony.Mms.MESSAGE_BOX_SENT
        } else {
            val msg = context.getString(R.string.unknown_error_occurred_sending_message, receiverResultCode)
            context.toast(msg = msg, length = Toast.LENGTH_LONG)
            Telephony.Mms.MESSAGE_BOX_FAILED
        }

        val values = ContentValues(1).apply {
            put(Telephony.Mms.MESSAGE_BOX, messageBox)
        }

        try {
            context.contentResolver.update(uri, values, null, null)
        } catch (e: SQLiteException) {
            context.showErrorToast(e)
        }

        // In case of resent message, delete original to prevent duplication
        if (originalResentMessageId != -1L) {
            context.deleteMessage(originalResentMessageId, true)
        }

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        if (filePath != null) {
            File(filePath).delete()
        }
    }

    override fun updateAppDatabase(context: Context, intent: Intent, receiverResultCode: Int) {
        refreshMessages()
        refreshConversations()
    }

    companion object {
        private const val EXTRA_CONTENT_URI = "content_uri"
        private const val EXTRA_FILE_PATH = "file_path"
        const val EXTRA_ORIGINAL_RESENT_MESSAGE_ID = "original_message_id"
    }
}
