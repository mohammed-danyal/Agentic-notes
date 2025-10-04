package com.aerotech.agenticnotes

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.UUID

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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}