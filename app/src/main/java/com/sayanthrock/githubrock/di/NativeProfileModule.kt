package com.sayanthrock.githubrock.di

import com.sayanthrock.githubrock.core.network.GitHubProfileApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NativeProfileModule {
    @Provides
    @Singleton
    fun nativeProfileApi(retrofit: Retrofit): GitHubProfileApi =
        retrofit.create(GitHubProfileApi::class.java)
}
