package com.sayanthrock.githubrock.core.network

import com.sayanthrock.githubrock.BuildConfig
import com.sayanthrock.githubrock.core.security.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request = original.newBuilder()
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", BuildConfig.GITHUB_API_VERSION)
            .apply {
                tokenStore.read()?.accessToken?.takeIf(String::isNotBlank)?.let {
                    header("Authorization", "Bearer $it")
                }
            }
            .build()
        return chain.proceed(request)
    }
}

