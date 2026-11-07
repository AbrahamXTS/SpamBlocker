package net.abrahamespinosa.spam_blocker

import android.app.Application
import androidx.room.Room
import net.abrahamespinosa.spam_blocker.data.MIGRATION_1_2
import net.abrahamespinosa.spam_blocker.data.SpamBlockerDatabase

class SpamBlockerApplication : Application() {
    val database: SpamBlockerDatabase by lazy {
        Room.databaseBuilder(this, SpamBlockerDatabase::class.java, "spam-blocker.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}