package com.dark.launcher.di

import android.content.Context
import androidx.room.Room
import com.dark.launcher.data.db.DarkDatabase
import com.dark.launcher.data.db.FitnessDao
import com.dark.launcher.data.db.MIGRATION_1_2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DarkDatabase =
        Room.databaseBuilder(context, DarkDatabase::class.java, "dark.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideFitnessDao(db: DarkDatabase): FitnessDao = db.fitnessDao()
}
