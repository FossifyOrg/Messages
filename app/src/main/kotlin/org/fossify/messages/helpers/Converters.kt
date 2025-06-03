package org.fossify.messages.helpers

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.fossify.commons.models.SimpleContact
import org.fossify.messages.models.Attachment
import org.fossify.messages.models.MessageAttachment

class Converters {
    private val gson = Gson()
    private val attachmentType = object : TypeToken<List<Attachment>>() {}.type
    private val simpleContactType = object : TypeToken<List<SimpleContact>>() {}.type
    private val messageAttachmentType = object : TypeToken<MessageAttachment?>() {}.type

    @TypeConverter
    fun jsonToAttachmentList(value: String?): ArrayList<Attachment>? {
        return gson.fromJson<ArrayList<Attachment>>(value, attachmentType)
    }

    @TypeConverter
    fun attachmentListToJson(list: ArrayList<Attachment>) = gson.toJson(list)

    @TypeConverter
    fun jsonToSimpleContactList(value: String?): ArrayList<SimpleContact>? {
        return gson.fromJson<ArrayList<SimpleContact>>(value, simpleContactType)
    }

    @TypeConverter
    fun simpleContactListToJson(list: ArrayList<SimpleContact>) = gson.toJson(list)

    @TypeConverter
    fun jsonToMessageAttachment(value: String): MessageAttachment? {
        return gson.fromJson<MessageAttachment>(value, messageAttachmentType)
    }

    @TypeConverter
    fun messageAttachmentToJson(messageAttachment: MessageAttachment?): String? {
        return gson.toJson(messageAttachment)
    }
}
