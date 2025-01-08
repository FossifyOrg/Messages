package org.fossify.messages.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.fossify.messages.models.Draft

@Dao
interface DraftsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(draft: Draft): Long

    @Query("SELECT * FROM drafts")
    fun getAll(): List<Draft>

    @Query("SELECT * FROM drafts WHERE thread_id = :threadId")
    fun getDraftById(threadId: Long): Draft?

    @Query("DELETE FROM drafts WHERE thread_id = :threadId")
    fun delete(threadId: Long): Int
}
