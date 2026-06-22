package com.cunffy.launcher.di

import android.content.Context
import androidx.room.Room
import com.cunffy.launcher.data.db.LauncherDatabase
import com.cunffy.launcher.data.db.dao.CustomizationDao
import com.cunffy.launcher.data.db.dao.HomeLayoutDao
import com.cunffy.launcher.data.db.dao.NotesDao
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
    fun provideDatabase(@ApplicationContext context: Context): LauncherDatabase =
        Room.databaseBuilder(context, LauncherDatabase::class.java, "launcher.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCustomizationDao(db: LauncherDatabase): CustomizationDao = db.customizationDao()

    @Provides
    fun provideHomeLayoutDao(db: LauncherDatabase): HomeLayoutDao = db.homeLayoutDao()

    @Provides
    fun provideNotesDao(db: LauncherDatabase): NotesDao = db.notesDao()
}
