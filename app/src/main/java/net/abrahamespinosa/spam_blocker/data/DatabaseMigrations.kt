package net.abrahamespinosa.spam_blocker.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE inbox_messages ADD COLUMN providerId INTEGER")
        database.execSQL("ALTER TABLE inbox_messages ADD COLUMN threadId INTEGER")
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_inbox_messages_providerId " +
                "ON inbox_messages(providerId)",
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS sync_state (key TEXT NOT NULL, value TEXT NOT NULL, " +
                "PRIMARY KEY(key))",
        )
    }
}