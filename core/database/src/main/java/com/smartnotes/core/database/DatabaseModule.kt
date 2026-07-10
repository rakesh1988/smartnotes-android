package com.smartnotes.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartNotesDatabase {
        return Room.databaseBuilder(
            context,
            SmartNotesDatabase::class.java,
            "smartnotes.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(database: SmartNotesDatabase): NoteDao {
        return database.noteDao()
    }
}
