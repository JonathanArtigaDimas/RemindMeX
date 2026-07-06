package com.example.remindme

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Reminder::class, Note::class, Tag::class, NoteTagCrossRef::class, SharedNoteEntity::class, FavoriteGroup::class, Notebook::class, SharedNotebookEntity::class], 
    version = 20, // Incrementado tras añadir customInterval a Reminder
    exportSchema = false
)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun noteDao(): NoteDao
    abstract fun sharedNoteDao(): SharedNoteDao
    abstract fun favoriteGroupDao(): FavoriteGroupDao
    abstract fun notebookDao(): NotebookDao
    abstract fun sharedNotebookDao(): SharedNotebookDao

    companion object {
        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        fun getDatabase(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminder_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
