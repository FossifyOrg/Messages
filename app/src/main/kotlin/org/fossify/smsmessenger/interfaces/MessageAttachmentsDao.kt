package org.fossify.smsmessenger.interfaces

import androidx.room.Dao
import androidx.room.Query
import org.fossify.smsmessenger.models.MessageAttachment

@Dao
interface MessageAttachmentsDao {
    @Query("SELECT * FROM message_attachments")
    fun getAll(): List<MessageAttachment>
}
