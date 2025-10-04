package com.aerotech.agenticnotes

import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.UUID
// ... other imports
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// ...

// --- NEW: Define the migration from version 1 to 2 ---
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add the new columns to the existing table
        db.execSQL("ALTER TABLE notes_table ADD COLUMN isBinned INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE notes_table ADD COLUMN binnedTimestamp INTEGER")
    }
}




// A TypeConverter is needed to tell Room how to store a UUID, which it doesn't know by default.
class UUIDConverter {
    @TypeConverter
    fun fromUUID(uuid: UUID?): String? {
        return uuid?.toString()
    }

    @TypeConverter
    fun uuidFromString(string: String?): UUID? {
        // FIX: Change this line to use the correct UUID class
        return string?.let{ UUID.fromString(it) }
    }
}


@Database(entities = [Note::class], version = 1, exportSchema = false)
@TypeConverters(UUIDConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        // Volatile ensures that the value of INSTANCE is always up-to-date
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notes_database" // The file name for the database
                ).addMigrations(MIGRATION_1_2)
                    .allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }
    }
}