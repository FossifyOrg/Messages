package org.fossify.smsmessenger.messaging

import android.content.Context
import android.telephony.SmsMessage
import android.widget.Toast.LENGTH_LONG
import com.klinker.android.send_message.Settings
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.smsmessenger.R
import org.fossify.smsmessenger.extensions.config
import org.fossify.smsmessenger.extensions.messagingUtils
import org.fossify.smsmessenger.messaging.SmsException.Companion.EMPTY_DESTINATION_ADDRESS
import org.fossify.smsmessenger.messaging.SmsException.Companion.ERROR_PERSISTING_MESSAGE
import org.fossify.smsmessenger.messaging.SmsException.Companion.ERROR_SENDING_MESSAGE
import org.fossify.smsmessenger.models.Attachment

@Deprecated("TODO: Move/rewrite messaging config code into the app.")
fun Context.getSendMessageSettings(): Settings {
    val settings = Settings()
    settings.useSystemSending = true
    settings.deliveryReports = config.enableDeliveryReports
    settings.sendLongAsMms = config.sendLongMessageMMS
    settings.sendLongAsMmsAfter = 1
    settings.group = config.sendGroupMessageMMS
    return settings
}

fun Context.isLongMmsMessage(text: String, settings: Settings = getSendMessageSettings()): Boolean {
    val data = SmsMessage.calculateLength(text, false)
    val numPages = data.first()
    return numPages > settings.sendLongAsMmsAfter && settings.sendLongAsMms
}

/** Sends the message using the in-app SmsManager API wrappers if it's an SMS or using android-smsmms for MMS. */
fun Context.sendMessageCompat(text: String, addresses: List<String>, subId: Int?, attachments: List<Attachment>, messageId: Long? = null) {
    val settings = getSendMessageSettings()
    if (subId != null) {
        settings.subscriptionId = subId
    }

    val messagingUtils = messagingUtils
    val isMms = attachments.isNotEmpty() || isLongMmsMessage(text, settings) || addresses.size > 1 && settings.group
    if (isMms) {
        // we send all MMS attachments separately to reduces the chances of hitting provider MMS limit.
        if (attachments.isNotEmpty()) {
            val lastIndex = attachments.lastIndex
            if (attachments.size > 1) {
                for (i in 0 until lastIndex) {
                    val attachment = attachments[i]
                    messagingUtils.sendMmsMessage("", addresses, attachment, settings, messageId)
                }
            }

            val lastAttachment = attachments[lastIndex]
            messagingUtils.sendMmsMessage(text, addresses, lastAttachment, settings, messageId)
        } else {
            messagingUtils.sendMmsMessage(text, addresses, null, settings, messageId)
        }
    } else {
        try {
            messagingUtils.sendSmsMessage(text, addresses.toSet(), settings.subscriptionId, settings.deliveryReports, messageId)
        } catch (e: SmsException) {
            when (e.errorCode) {
                EMPTY_DESTINATION_ADDRESS -> toast(id = R.string.empty_destination_address, length = LENGTH_LONG)
                ERROR_PERSISTING_MESSAGE -> toast(id = R.string.unable_to_save_message, length = LENGTH_LONG)
                ERROR_SENDING_MESSAGE -> toast(
                    msg = getString(R.string.unknown_error_occurred_sending_message, e.errorCode),
                    length = LENGTH_LONG
                )
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }
}

/**
 * Check if a given "address" is a short code.
 * There's not much info available on these special numbers, even the wikipedia page (https://en.wikipedia.org/wiki/Short_code)
 * contains outdated information regarding max number of digits. The exact parameters for short codes can vary by country and by carrier.
 *
 * This function simply returns true if the [address] contains at least one letter.
 */
fun isShortCodeWithLetters(address: String): Boolean {
    return address.any { it.isLetter() }
}
