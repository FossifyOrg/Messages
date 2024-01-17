package org.fossify.messages.models

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = BackupSerializer::class)
sealed class MessagesBackup {
    @SerialName("backupType")
    abstract val backupType: BackupType
}

object BackupSerializer :
    JsonContentPolymorphicSerializer<MessagesBackup>(MessagesBackup::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out MessagesBackup> {
        return when (element.jsonObject["backupType"]?.jsonPrimitive?.content) {
            "sms" -> SmsBackup.serializer()
            "mms" -> MmsBackup.serializer()
            else -> throw SerializationException("ERROR: No Serializer found. Serialization failed.")
        }
    }
}
