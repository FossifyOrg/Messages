package org.fossify.messages.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drafts")
data class Draft(
    @ColumnInfo(name = "thread_id") @PrimaryKey val threadId: Long,
    @ColumnInfo(name = "body") val body: String,
    @ColumnInfo(name = "date") val date: Long,
)
