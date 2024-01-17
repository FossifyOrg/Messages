package org.fossify.messages.interfaces

import androidx.room.*
import org.fossify.messages.models.Message
import org.fossify.messages.models.RecycleBinMessage

@Dao
interface MessagesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecycleBinEntry(recycleBinMessage: RecycleBinMessage)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertOrIgnore(message: Message): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessages(vararg message: Message)

    @Query("SELECT * FROM messages")
    fun getAll(): List<Message>

    @Query("SELECT messages.* FROM messages LEFT OUTER JOIN recycle_bin_messages ON messages.id = recycle_bin_messages.id WHERE recycle_bin_messages.id IS NOT NULL")
    fun getAllRecycleBinMessages(): List<Message>

    @Query("SELECT messages.* FROM messages LEFT OUTER JOIN recycle_bin_messages ON messages.id = recycle_bin_messages.id WHERE recycle_bin_messages.id IS NOT NULL AND recycle_bin_messages.deleted_ts < :timestamp")
    fun getOldRecycleBinMessages(timestamp: Long): List<Message>

    @Query("SELECT * FROM messages WHERE thread_id = :threadId")
    fun getThreadMessages(threadId: Long): List<Message>

    @Query("SELECT messages.* FROM messages LEFT OUTER JOIN recycle_bin_messages ON messages.id = recycle_bin_messages.id WHERE recycle_bin_messages.id IS NULL AND thread_id = :threadId")
    fun getNonRecycledThreadMessages(threadId: Long): List<Message>

    @Query("SELECT messages.* FROM messages LEFT OUTER JOIN recycle_bin_messages ON messages.id = recycle_bin_messages.id WHERE recycle_bin_messages.id IS NOT NULL AND thread_id = :threadId")
    fun getThreadMessagesFromRecycleBin(threadId: Long): List<Message>

    @Query("SELECT messages.* FROM messages LEFT OUTER JOIN recycle_bin_messages ON messages.id = recycle_bin_messages.id WHERE recycle_bin_messages.id IS NULL AND thread_id = :threadId AND is_scheduled = 1")
    fun getScheduledThreadMessages(threadId: Long): List<Message>

    @Query("SELECT * FROM messages WHERE thread_id = :threadId AND id = :messageId AND is_scheduled = 1")
    fun getScheduledMessageWithId(threadId: Long, messageId: Long): Message

    @Query("SELECT COUNT(*) FROM recycle_bin_messages")
    fun getArchivedCount(): Int

    @Query("SELECT * FROM messages WHERE body LIKE :text")
    fun getMessagesWithText(text: String): List<Message>

    @Query("UPDATE messages SET read = 1 WHERE id = :id")
    fun markRead(id: Long)

    @Query("UPDATE messages SET read = 1 WHERE thread_id = :threadId")
    fun markThreadRead(threadId: Long)

    @Query("UPDATE messages SET type = :type WHERE id = :id")
    fun updateType(id: Long, type: Int): Int

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    fun updateStatus(id: Long, status: Int): Int

    @Transaction
    fun delete(id: Long) {
        deleteFromMessages(id)
        deleteFromRecycleBin(id)
    }

    @Query("DELETE FROM messages WHERE id = :id")
    fun deleteFromMessages(id: Long)

    @Query("DELETE FROM recycle_bin_messages WHERE id = :id")
    fun deleteFromRecycleBin(id: Long)

    @Transaction
    fun deleteThreadMessages(threadId: Long) {
        deleteThreadMessagesFromRecycleBin(threadId)
        deleteAllThreadMessages(threadId)
    }

    @Query("DELETE FROM messages WHERE thread_id = :threadId")
    fun deleteAllThreadMessages(threadId: Long)

    @Query("DELETE FROM recycle_bin_messages WHERE id IN (SELECT id FROM messages WHERE thread_id = :threadId)")
    fun deleteThreadMessagesFromRecycleBin(threadId: Long)

    @Query("DELETE FROM messages")
    fun deleteAll()
}
