package org.fossify.messages.messaging

import android.telephony.SmsManager
import com.klinker.android.send_message.Settings

private var smsManagerInstance: SmsManager? = null
private var associatedSubId: Int = -1

@Suppress("DEPRECATION")
fun getSmsManager(subId: Int): SmsManager {
    if (smsManagerInstance == null || subId != associatedSubId) {
        smsManagerInstance = if (subId != Settings.DEFAULT_SUBSCRIPTION_ID) {
            try {
                smsManagerInstance = SmsManager.getSmsManagerForSubscriptionId(subId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            smsManagerInstance ?: SmsManager.getDefault()
        } else {
            SmsManager.getDefault()
        }
        associatedSubId = subId
    }
    return smsManagerInstance!!
}
