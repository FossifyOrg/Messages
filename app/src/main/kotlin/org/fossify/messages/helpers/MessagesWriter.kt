package org.fossify.messages.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.Telephony.Mms
import android.provider.Telephony.Sms
import android.util.Base64
import com.google.android.mms.pdu_alt.PduHeaders
import com.klinker.android.send_message.Utils
import org.fossify.commons.extensions.getLongValue
import org.fossify.commons.extensions.queryCursor
import org.fossify.commons.helpers.isRPlus
import org.fossify.messages.extensions.updateLastConversationMessage
import org.fossify.messages.models.MmsAddress
import org.fossify.messages.models.MmsBackup
import org.fossify.messages.models.MmsPart
import org.fossify.messages.models.SmsBackup

class MessagesWriter(private val context: Context) {
    private val INVALID_ID = -1L
    private val contentResolver = context.contentResolver
    private val modifiedThreadIds = mutableSetOf<Long>()

    fun writeSmsMessage(smsBackup: SmsBackup) {
        val contentValues = smsBackup.toContentValues()
        val threadId = Utils.getOrCreateThreadId(context, smsBackup.address)
        contentValues.put(Sms.THREAD_ID, threadId)
        if (!smsExist(smsBackup)) {
            modifiedThreadIds.add(threadId)
            contentResolver.insert(Sms.CONTENT_URI, contentValues)
        }
    }

    private fun smsExist(smsBackup: SmsBackup): Boolean {
        val uri = Sms.CONTENT_URI
        val projection = arrayOf(Sms._ID)
        val selection = "${Sms.DATE} = ? AND ${Sms.ADDRESS} = ? AND ${Sms.TYPE} = ?"
        val selectionArgs = arrayOf(
            smsBackup.date.toString(), smsBackup.address, smsBackup.type.toString()
        )

        var exists = false
        context.queryCursor(uri, projection, selection, selectionArgs) {
            exists = it.count > 0
        }
        return exists
    }

    fun writeMmsMessage(mmsBackup: MmsBackup) {
        // 1. write mms msg, get the msg_id, check if mms exists before writing
        // 2. write parts - parts depend on the msg id, check if part exist before writing, write data if it is a non-text part
        // 3. write the addresses, address depends on msg id too, check if address exist before writing
        val contentValues = mmsBackup.toContentValues()
        val threadId = getMmsThreadId(mmsBackup)
        if (threadId != INVALID_ID) {
            contentValues.put(Mms.THREAD_ID, threadId)
            if (!mmsExist(mmsBackup)) {
                modifiedThreadIds.add(threadId)
                contentResolver.insert(Mms.CONTENT_URI, contentValues)
            }
            val messageId = getMmsId(mmsBackup)
            if (messageId != INVALID_ID) {
                mmsBackup.parts.forEach { writeMmsPart(it, messageId) }
                mmsBackup.addresses.forEach { writeMmsAddress(it, messageId) }
            }
        }
    }

    private fun getMmsThreadId(mmsBackup: MmsBackup): Long {
        val address = when (mmsBackup.messageBox) {
            Mms.MESSAGE_BOX_INBOX -> mmsBackup.addresses.firstOrNull { it.type == PduHeaders.FROM }?.address
            else -> mmsBackup.addresses.firstOrNull { it.type == PduHeaders.TO }?.address
        }
        return if (!address.isNullOrEmpty()) {
            Utils.getOrCreateThreadId(context, address)
        } else {
            INVALID_ID
        }
    }

    private fun getMmsId(mmsBackup: MmsBackup): Long {
        val threadId = getMmsThreadId(mmsBackup)
        val uri = Mms.CONTENT_URI
        val projection = arrayOf(Mms._ID)
        val selection = "${Mms.DATE} = ? AND ${Mms.DATE_SENT} = ? AND ${Mms.THREAD_ID} = ? AND ${Mms.MESSAGE_BOX} = ?"
        val selectionArgs = arrayOf(
            mmsBackup.date.toString(),
            mmsBackup.dateSent.toString(),
            threadId.toString(),
            mmsBackup.messageBox.toString()
        )
        var id = INVALID_ID
        context.queryCursor(uri, projection, selection, selectionArgs) {
            id = it.getLongValue(Mms._ID)
        }

        return id
    }

    private fun mmsExist(mmsBackup: MmsBackup): Boolean {
        return getMmsId(mmsBackup) != INVALID_ID
    }

    @SuppressLint("NewApi")
    private fun mmsAddressExist(mmsAddress: MmsAddress, messageId: Long): Boolean {
        val addressUri = if (isRPlus()) {
            Mms.Addr.getAddrUriForMessage(messageId.toString())
        } else {
            Uri.parse("content://mms/$messageId/addr")
        }

        val projection = arrayOf(Mms.Addr._ID)
        val selection = "${Mms.Addr.TYPE} = ? AND ${Mms.Addr.ADDRESS} = ? AND ${Mms.Addr.MSG_ID} = ?"
        val selectionArgs = arrayOf(
            mmsAddress.type.toString(),
            mmsAddress.address,
            messageId.toString()
        )
        var exists = false
        context.queryCursor(addressUri, projection, selection, selectionArgs) {
            exists = it.count > 0
        }
        return exists
    }

    @SuppressLint("NewApi")
    private fun writeMmsAddress(mmsAddress: MmsAddress, messageId: Long) {
        if (!mmsAddressExist(mmsAddress, messageId)) {
            val addressUri = if (isRPlus()) {
                Mms.Addr.getAddrUriForMessage(messageId.toString())
            } else {
                Uri.parse("content://mms/$messageId/addr")
            }

            val contentValues = mmsAddress.toContentValues()
            contentValues.put(Mms.Addr.MSG_ID, messageId)
            contentResolver.insert(addressUri, contentValues)
        }
    }

    @SuppressLint("NewApi")
    private fun writeMmsPart(mmsPart: MmsPart, messageId: Long) {
        if (!mmsPartExist(mmsPart, messageId)) {
            val uri = Uri.parse("content://mms/${messageId}/part")
            val contentValues = mmsPart.toContentValues()
            contentValues.put(Mms.Part.MSG_ID, messageId)
            val partUri = contentResolver.insert(uri, contentValues)
            try {
                if (partUri != null) {
                    if (mmsPart.isNonText()) {
                        contentResolver.openOutputStream(partUri).use {
                            val arr = Base64.decode(mmsPart.data, Base64.DEFAULT)
                            it!!.write(arr)
                        }
                    }
                }
            } catch (e: Exception) {

            }
        }
    }

    @SuppressLint("NewApi")
    private fun mmsPartExist(mmsPart: MmsPart, messageId: Long): Boolean {
        val uri = Uri.parse("content://mms/${messageId}/part")
        val projection = arrayOf(Mms.Part._ID)
        val selection = "${Mms.Part.CONTENT_LOCATION} = ? AND ${Mms.Part.CONTENT_TYPE} = ? AND ${Mms.Part.MSG_ID} = ? AND ${Mms.Part.CONTENT_ID} = ?"
        val selectionArgs = arrayOf(
            mmsPart.contentLocation.toString(),
            mmsPart.contentType,
            messageId.toString(),
            mmsPart.contentId.toString()
        )

        var exists = false
        context.queryCursor(uri, projection, selection, selectionArgs) {
            exists = it.count > 0
        }
        return exists
    }

    /** Fixes the timestamps of all conversations modified by previous writes. */
    fun fixConversationDates() {
        // This method should be called after messages are written, to set the correct conversation
        // timestamps.
        //
        // It is necessary because when we insert a message, Android's Telephony provider sets the
        // conversation date to the current time rather than the time of the message that was
        // inserted. Source code reference:
        // https://android.googlesource.com/platform/packages/providers/TelephonyProvider/+/android14-release/src/com/android/providers/telephony/MmsSmsDatabaseHelper.java#134
        context.updateLastConversationMessage(modifiedThreadIds)
    }
}
