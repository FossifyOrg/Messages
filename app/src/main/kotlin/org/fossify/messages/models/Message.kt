package org.fossify.messages.models

import android.provider.Telephony
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.fossify.commons.models.SimpleContact
import org.fossify.messages.helpers.THREAD_RECEIVED_MESSAGE
import org.fossify.messages.helpers.THREAD_SENT_MESSAGE
import org.fossify.messages.helpers.generateStableId

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "body") val body: String,
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "status") val status: Int,
    @ColumnInfo(name = "participants") val participants: ArrayList<SimpleContact>,
    @ColumnInfo(name = "date") val date: Int,
    @ColumnInfo(name = "read") val read: Boolean,
    @ColumnInfo(name = "thread_id") val threadId: Long,
    @ColumnInfo(name = "is_mms") val isMMS: Boolean,
    @ColumnInfo(name = "attachment") val attachment: MessageAttachment?,
    @ColumnInfo(name = "sender_phone_number") val senderPhoneNumber: String,
    @ColumnInfo(name = "sender_name") var senderName: String,
    @ColumnInfo(name = "sender_photo_uri") val senderPhotoUri: String,
    @ColumnInfo(name = "subscription_id") var subscriptionId: Int,
    @ColumnInfo(name = "is_scheduled") var isScheduled: Boolean = false
) : ThreadItem() {

    fun isReceivedMessage() = type == Telephony.Sms.MESSAGE_TYPE_INBOX

    fun millis() = date * 1000L

    fun getSender(): SimpleContact? =
        participants.firstOrNull { it.doesHavePhoneNumber(senderPhoneNumber) }
            ?: participants.firstOrNull { it.name == senderName }
            ?: participants.firstOrNull()

    fun getStableId(): Long {
        val providerBit = if (isMMS) 1L else 0L
        val key = (id shl 1) or providerBit
        val type = if (isReceivedMessage()) THREAD_RECEIVED_MESSAGE else THREAD_SENT_MESSAGE
        return generateStableId(type, key)
    }

    fun getSelectionKey(): Int {
        return (id xor (id ushr Int.SIZE_BITS)).toInt()
    }

    companion object {
        fun areItemsTheSame(old: Message, new: Message): Boolean {
            return old.id == new.id
        }

        fun areContentsTheSame(old: Message, new: Message): Boolean {
            return old.body == new.body &&
                old.threadId == new.threadId &&
                old.date == new.date &&
                old.isMMS == new.isMMS &&
                old.attachment == new.attachment &&
                old.senderPhoneNumber == new.senderPhoneNumber &&
                old.senderName == new.senderName &&
                old.senderPhotoUri == new.senderPhotoUri &&
                old.isScheduled == new.isScheduled
        }
    }
}
