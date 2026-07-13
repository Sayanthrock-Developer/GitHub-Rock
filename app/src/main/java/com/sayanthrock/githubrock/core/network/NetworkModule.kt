package com.sayanthrock.githubrock.core.network

import com.sayanthrock.githubrock.BuildConfig
import com.sayanthrock.githubrock.core.security.TokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /**
     * Provides the JSON configuration used by the network layer.
     *
     * Unknown fields are ignored, explicit null values are omitted, and lenient parsing is enabled.
     *
     * @return The configured JSON serializer.
     */
    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
    }

    /**
     * Creates an HTTP logging interceptor with debug-dependent logging and sensitive header redaction.
     *
     * @return The configured HTTP logging interceptor.
     */
    @Provides
    @Singleton
    fun safeLogger(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
        redactHeader("Cookie")
        redactHeader("Set-Cookie")
    }

    /**
             * Creates an HTTP client configured for GitHub API requests.
             *
             * @param tokenStore Provides the access token used for authenticated requests.
             * @return An OkHttp client with GitHub API headers and optional bearer authentication.
             */
            @Provides
    @Singleton
    fun githubClient(tokenStore: TokenStore, logger: HttpLoggingInterceptor): OkHttpClient =
        baseClient(logger)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", BuildConfig.GITHUB_API_VERSION)
                    .apply {
                        tokenStore.accessToken()?.takeIf(String::isNotBlank)?.let {
                            header("Authorization", "Bearer $it")
                        }
                    }
                    .build()
                chain.proceed(request)
            }
            .build()

    /**
         * Provides the OkHttp client used for GitHub authentication requests.
         *
         * @param logger The HTTP logging interceptor added to the client.
         * @return A configured OkHttp client that requests JSON responses.
         */
        @Provides
    @Singleton
    @Named("authClient")
    fun authClient(logger: HttpLoggingInterceptor): OkHttpClient = baseClient(logger)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .build()
            )
        }
        .build()

    /**
     * Creates the HTTP client used for file downloads.
     *
     * @param logger The HTTP logging interceptor added to the client.
     * @return The configured download HTTP client.
     */
    @Provides
    @Singleton
    @Named("downloadClient")
    fun downloadClient(logger: HttpLoggingInterceptor): OkHttpClient = baseClient(logger).build()

    /**
         * Creates a Retrofit instance for GitHub API requests.
         *
         * @param json The JSON serializer used to convert request and response bodies.
         * @param client The HTTP client used for network requests.
         * @return A configured Retrofit instance targeting the GitHub API.
         */
        @Provides
    @Singleton
    @Named("githubRetrofit")
    fun githubRetrofit(json: Json, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    /**
         * Provides a Retrofit instance for GitHub authentication endpoints.
         *
         * @param json The JSON serializer used to convert request and response bodies.
         * @param client The OkHttp client used for authentication requests.
         * @return A configured Retrofit instance using GitHub's website base URL.
         */
        @Provides
    @Singleton
    @Named("authRetrofit")
    fun authRetrofit(json: Json, @Named("authClient") client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://github.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    /**
         * Creates the GitHub API service implementation.
         *
         * @param retrofit The Retrofit instance configured for the GitHub API.
         * @return The GitHub API service.
         */
        @Provides
    @Singleton
    fun githubApi(@Named("githubRetrofit") retrofit: Retrofit): GitHubApi =
        retrofit.create(GitHubApi::class.java)

    /**
         * Creates the GitHub authentication API implementation.
         *
         * @param retrofit The Retrofit instance configured for GitHub authentication.
         * @return The GitHub authentication API.
         */
        @Provides
    @Singleton
    fun authApi(@Named("authRetrofit") retrofit: Retrofit): GitHubAuthApi =
        retrofit.create(GitHubAuthApi::class.java)

    /**
             * Creates an OkHttp client builder with standard timeouts and request logging.
             *
             * @param logger The interceptor used to log requests.
             * @return A configured OkHttp client builder.
             */
            private fun baseClient(logger: HttpLoggingInterceptor): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .addInterceptor(logger)
}
