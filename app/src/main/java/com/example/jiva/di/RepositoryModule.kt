package com.example.jiva.di

import com.example.jiva.data.repository.AuthRepository
import com.example.jiva.data.repository.DummyAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for repositories
 * When you connect your SQL database, simply replace DummyAuthRepository
 * with your actual database implementation
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        dummyAuthRepository: DummyAuthRepository
    ): AuthRepository
    
    // When you implement SQL database, replace the above with:
    // @Binds
    // @Singleton
    // abstract fun bindAuthRepository(
    //     sqlAuthRepository: SqlAuthRepository
    // ): AuthRepository
}
