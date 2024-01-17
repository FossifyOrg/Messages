package org.fossify.messages.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recycle_bin_messages",
    indices = [(Index(value = ["id"], unique = true))]
)
data class RecycleBinMessage(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "deleted_ts") var deletedTS: Long
)
