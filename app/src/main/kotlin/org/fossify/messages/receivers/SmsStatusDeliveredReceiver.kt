package org.fossify.messages.receivers

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony.Sms
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.extensions.messagesDB
import org.fossify.messages.extensions.messagingUtils
import org.fossify.messages.helpers.refreshMessages

/** Handles updating databases and states when a sent SMS message is delivered. */
class SmsStatusDeliveredReceiver : SendStatusReceiver() {

    private var status: Int = Sms.Sent.STATUS_NONE

    override fun updateAndroidDatabase(context: Context, intent: Intent, receiverResultCode: Int) {
        val messageUri: Uri? = intent.data
        val smsMessage = context.messagingUtils.getSmsMessageFromDeliveryReport(intent) ?: return

        try {
            val format = intent.getStringExtra("format")
            status = smsMessage.status
            // Simple matching up CDMA status with GSM status.
            if ("3gpp2" == format) {
                val errorClass = status shr 24 and 0x03
                val statusCode = status shr 16 and 0x3f
                status = when (errorClass) {
                    0 -> {
                        if (statusCode == 0x02 /*STATUS_DELIVERED*/) {
                            Sms.STATUS_COMPLETE
                        } else {
                            Sms.STATUS_PENDING
                        }
                    }

                    2 -> {
                        // TODO: Need to check whether SC still trying to deliver the SMS to destination and will send the report again?
                        Sms.STATUS_PENDING
                    }

                    3 -> {
                        Sms.STATUS_FAILED
                    }

                    else -> {
                        Sms.STATUS_PENDING
                    }
                }
            }
        } catch (e: NullPointerException) {
            // Sometimes, SmsMessage.mWrappedSmsMessage is null causing NPE when we access
            // the methods on it although the SmsMessage itself is not null.
            return
        }

        updateSmsStatusAndDateSent(context, messageUri, System.currentTimeMillis())
    }

    private fun updateSmsStatusAndDateSent(context: Context, messageUri: Uri?, timeSentInMillis: Long = -1L) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            if (status != Sms.Sent.STATUS_NONE) {
                put(Sms.Sent.STATUS, status)
            }
            put(Sms.Sent.DATE_SENT, timeSentInMillis)
        }

        if (messageUri != null) {
            resolver.update(messageUri, values, null, null)
        } else {
            // mark latest sms as delivered, need to check if this is still necessary (or reliable)
            val cursor = resolver.query(Sms.Sent.CONTENT_URI, null, null, null, "date desc")
            cursor?.use {
                if (cursor.moveToFirst()) {
                    @SuppressLint("Range")
                    val id = cursor.getString(cursor.getColumnIndex(Sms.Sent._ID))
                    val selection = "${Sms._ID} = ?"
                    val selectionArgs = arrayOf(id.toString())
                    resolver.update(Sms.Sent.CONTENT_URI, values, selection, selectionArgs)
                }
            }
        }
    }

    override fun updateAppDatabase(context: Context, intent: Intent, receiverResultCode: Int) {
        val messageUri: Uri? = intent.data
        if (messageUri != null) {
            val messageId = messageUri.lastPathSegment?.toLong() ?: 0L
            ensureBackgroundThread {
                if (status != Sms.Sent.STATUS_NONE) {
                    context.messagesDB.updateStatus(messageId, status)
                }
                refreshMessages()
            }
        }
    }
}
