package org.fossify.messages.models

import android.content.ContentValues
import android.provider.Telephony
import androidx.core.content.contentValuesOf
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class MmsBackup(
    @SerializedName("creator")
    val creator: String?,
    @SerializedName("ct_t")
    val contentType: String?,
    @SerializedName("d_rpt")
    val deliveryReport: Int,
    @SerializedName("date")
    val date: Long,
    @SerializedName("date_sent")
    val dateSent: Long,
    @SerializedName("locked")
    val locked: Int,
    @SerializedName("m_type")
    val messageType: Int,
    @SerializedName("msg_box")
    val messageBox: Int,
    @SerializedName("read")
    val read: Int,
    @SerializedName("rr")
    val readReport: Int,
    @SerializedName("seen")
    val seen: Int,
    @SerializedName("text_only")
    val textOnly: Int,
    @SerializedName("st")
    val status: String?,
    @SerializedName("sub")
    val subject: String?,
    @SerializedName("sub_cs")
    val subjectCharSet: String?,
    @SerializedName("sub_id")
    val subscriptionId: Long,
    @SerializedName("tr_id")
    val transactionId: String?,
    @SerializedName("addresses")
    val addresses: List<MmsAddress>,
    @SerializedName("parts")
    val parts: List<MmsPart>,

    override val backupType: BackupType = BackupType.MMS,
) : MessagesBackup() {

    fun toContentValues(): ContentValues {
        return contentValuesOf(
            Telephony.Mms.TRANSACTION_ID to transactionId,
            Telephony.Mms.SUBSCRIPTION_ID to subscriptionId,
            Telephony.Mms.SUBJECT to subject,
            Telephony.Mms.DATE to date,
            Telephony.Mms.DATE_SENT to dateSent,
            Telephony.Mms.LOCKED to locked,
            Telephony.Mms.READ to read,
            Telephony.Mms.STATUS to status,
            Telephony.Mms.SUBJECT_CHARSET to subjectCharSet,
            Telephony.Mms.SEEN to seen,
            Telephony.Mms.MESSAGE_TYPE to messageType,
            Telephony.Mms.MESSAGE_BOX to messageBox,
            Telephony.Mms.DELIVERY_REPORT to deliveryReport,
            Telephony.Mms.READ_REPORT to readReport,
            Telephony.Mms.CONTENT_TYPE to contentType,
            Telephony.Mms.TEXT_ONLY to textOnly,
        )
    }
}
