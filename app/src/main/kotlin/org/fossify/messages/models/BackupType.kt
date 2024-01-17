package org.fossify.messages.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BackupType {
    @SerialName("sms")
    SMS,

    @SerialName("mms")
    MMS,
}
