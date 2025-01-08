package org.fossify.messages.helpers

import android.content.Intent
import android.net.Uri
import com.google.android.mms.ContentType
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

// Base on https://cs.android.com/android/platform/superproject/main/+/main:packages/apps/Messaging/src/com/android/messaging/ui/conversation/LaunchConversationActivity.java
object SmsIntentParser {
    private const val SCHEME_SMS = "sms"
    private const val SCHEME_SMSTO = "smsto"
    private const val SCHEME_MMS = "mms"
    private const val SCHEME_MMSTO = "mmsto"
    private val SMS_MMS_SCHEMES = setOf(SCHEME_SMS, SCHEME_SMSTO, SCHEME_MMS, SCHEME_MMSTO)

    private const val MAX_RECIPIENT_LENGTH = 100
    private const val SMS_BODY = "sms_body"
    private const val ADDRESS = "address"

    fun parse(intent: Intent): Pair<String, String>? {
        val action = intent.action
        if (action != Intent.ACTION_SENDTO && action != Intent.ACTION_VIEW) {
            // Unsupported intent action
            return null
        }

        val recipients = parseRecipients(intent)
        val body = extractBodyFromIntent(intent)
        return body.orEmpty() to recipients
    }

    private fun parseRecipients(intent: Intent): String {
        val uriRecipients = parseRecipientsFromUri(intent.data)
        val extraAddress = intent.getStringExtra(ADDRESS)
        val extraEmail = intent.getStringExtra(Intent.EXTRA_EMAIL)

        val recipients = when {
            !extraAddress.isNullOrEmpty() -> arrayOf(extraAddress)
            !extraEmail.isNullOrEmpty() -> arrayOf(extraEmail)
            else -> uriRecipients.orEmpty()
        }

        return recipients
            .filter { it.length < MAX_RECIPIENT_LENGTH }
            .joinToString(";")
    }

    private fun parseRecipientsFromUri(uri: Uri?): Array<String>? {
        if (uri == null || uri.scheme !in SMS_MMS_SCHEMES) return null
        val schemeSpecificPart = uri.schemeSpecificPart.split("?").firstOrNull() ?: return null
        return schemeSpecificPart.replace(';', ',').split(",").toTypedArray()
    }

    private fun extractBodyFromIntent(intent: Intent): String? {
        val uriBody = extractBodyFromUri(intent.data)
        val smsBody = intent.getStringExtra(SMS_BODY)
        val extraText = if (ContentType.TEXT_PLAIN == intent.type) {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else {
            // Invalid URL, probably
            null
        }

        return smsBody ?: uriBody ?: extraText
    }

    private fun extractBodyFromUri(uri: Uri?): String? {
        if (uri == null) return null
        val query = uri.query ?: return null
        val bodyParam = query.split("&").firstOrNull { it.startsWith("body=") } ?: return null
        return try {
            URLDecoder.decode(bodyParam.removePrefix("body="), "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            null
        }
    }
}
