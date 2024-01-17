package org.fossify.messages.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "archived_conversations",
    indices = [(Index(value = ["thread_id"], unique = true))]
)
data class ArchivedConversation(
    @PrimaryKey @ColumnInfo(name = "thread_id") var threadId: Long,
    @ColumnInfo(name = "deleted_ts") var deletedTs: Long
)
