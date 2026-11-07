package net.abrahamespinosa.spam_blocker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "keywords", indices = [Index(value = ["word"], unique = true)])
data class KeywordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
)

@Entity(
    tableName = "inbox_messages",
    indices = [Index(value = ["providerId"], unique = true)],
)
data class InboxMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,
    val body: String,
    val receivedAt: Long,
    val providerId: Long? = null,
    val threadId: Long? = null,
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Entity(tableName = "quarantine_items")
data class QuarantineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val address: String,
    val body: String?,
    val receivedAt: Long,
)