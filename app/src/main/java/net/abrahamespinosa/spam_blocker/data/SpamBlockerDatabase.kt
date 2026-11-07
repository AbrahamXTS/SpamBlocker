package net.abrahamespinosa.spam_blocker.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        KeywordEntity::class,
        InboxMessageEntity::class,
        QuarantineItemEntity::class,
        SyncStateEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class SpamBlockerDatabase : RoomDatabase() {
    abstract fun keywordDao(): KeywordDao
    abstract fun inboxMessageDao(): InboxMessageDao
    abstract fun quarantineDao(): QuarantineDao
    abstract fun syncStateDao(): SyncStateDao
}