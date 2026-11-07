package net.abrahamespinosa.spam_blocker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KeywordDao {
    @Query("SELECT * FROM keywords ORDER BY word COLLATE NOCASE")
    fun observeAll(): Flow<List<KeywordEntity>>

    @Query("SELECT word FROM keywords")
    suspend fun getWords(): List<String>

    @Query("INSERT OR IGNORE INTO keywords(word) VALUES (LOWER(:word))")
    suspend fun insert(word: String)

    @Delete
    suspend fun delete(keyword: KeywordEntity)
}

@Dao
interface InboxMessageDao {
    @Query("SELECT * FROM inbox_messages ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<InboxMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(message: InboxMessageEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM inbox_messages WHERE providerId = :providerId)")
    suspend fun existsByProviderId(providerId: Long): Boolean

    @Query("DELETE FROM inbox_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE inbox_messages SET providerId = :providerId WHERE id = :id")
    suspend fun updateProviderId(id: Long, providerId: Long)

    @Query("SELECT * FROM inbox_messages WHERE address = :address")
    suspend fun findByAddress(address: String): List<InboxMessageEntity>

    @Query("DELETE FROM inbox_messages WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}

@Dao
interface SyncStateDao {
    @Query("SELECT value FROM sync_state WHERE key = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(state: SyncStateEntity)
}

@Dao
interface QuarantineDao {
    @Query("SELECT * FROM quarantine_items ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<QuarantineItemEntity>>

    @Insert
    suspend fun insert(item: QuarantineItemEntity)

    @Delete
    suspend fun delete(item: QuarantineItemEntity)
    @Query("DELETE FROM quarantine_items WHERE type = :type")
    suspend fun deleteByType(type: String)

    @Query("DELETE FROM quarantine_items WHERE type = 'sms' AND address = :address")
    suspend fun deleteSmsConversation(address: String)
}