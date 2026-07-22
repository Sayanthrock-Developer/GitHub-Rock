package com.sayanthrock.githubrock.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.sayanthrock.githubrock.core.network.GitHubProfileApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NativeProfileModule {
    @Provides
    @Singleton
    fun nativeProfileApi(json: Json, client: OkHttpClient): GitHubProfileApi = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GitHubProfileApi::class.java)
}
