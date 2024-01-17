package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.commons.helpers.ensureBackgroundThread

abstract class SendStatusReceiver : BroadcastReceiver() {
    // Updates the status of the message in the internal database
    abstract fun updateAndroidDatabase(context: Context, intent: Intent, receiverResultCode: Int)

    // allows the implementer to update the status of the message in their database
    abstract fun updateAppDatabase(context: Context, intent: Intent, receiverResultCode: Int)

    override fun onReceive(context: Context, intent: Intent) {
        val resultCode = resultCode
        ensureBackgroundThread {
            updateAndroidDatabase(context, intent, resultCode)
            updateAppDatabase(context, intent, resultCode)
        }
    }

    companion object {
        const val SMS_SENT_ACTION = "org.fossify.org.fossify.messages.receiver.SMS_SENT"
        const val SMS_DELIVERED_ACTION = "org.fossify.org.fossify.messages.receiver.SMS_DELIVERED"

        // Defined by platform, but no constant provided. See docs for SmsManager.sendTextMessage.
        const val EXTRA_ERROR_CODE = "errorCode"
        const val EXTRA_SUB_ID = "subId"

        const val NO_ERROR_CODE = -1
    }
}
