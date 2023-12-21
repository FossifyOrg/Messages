package org.fossify.smsmessenger.interfaces

import androidx.room.Dao
import androidx.room.Query
import org.fossify.smsmessenger.models.Attachment

@Dao
interface AttachmentsDao {
    @Query("SELECT * FROM attachments")
    fun getAll(): List<Attachment>
}
