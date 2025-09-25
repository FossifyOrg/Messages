package org.fossify.messages.models

import androidx.core.net.toUri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "attachments", indices = [(Index(value = ["message_id"], unique = true))])
data class Attachment(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "message_id") var messageId: Long,
    @ColumnInfo(name = "uri_string") var uriString: String,
    @ColumnInfo(name = "mimetype") var mimetype: String,
    @ColumnInfo(name = "width") var width: Int,
    @ColumnInfo(name = "height") var height: Int,
    @ColumnInfo(name = "filename") var filename: String
) {

    fun getUri() = uriString.toUri()
}
