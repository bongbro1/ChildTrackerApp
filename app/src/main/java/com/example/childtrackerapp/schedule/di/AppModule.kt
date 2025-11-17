package com.example.childtrackerapp.schedule.di

import com.example.childtrackerapp.data.repository.ScheduleRepository
import com.example.childtrackerapp.schedule.data.FirebaseDataSource
import com.example.childtrackerapp.schedule.data.ScheduleRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module để cung cấp dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseDataSource(): FirebaseDataSource {
        return FirebaseDataSource()
    }

    @Provides
    @Singleton
    fun provideScheduleRepository(
        firebaseDataSource: FirebaseDataSource
    ): ScheduleRepository {
        return ScheduleRepositoryImpl(firebaseDataSource)
    }
}