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
        val organization = tokenStore.activeOrganization()
            ?.trim()
            ?.removePrefix("@")
            ?.takeIf { it.matches(Regex("^[A-Za-z0-9_.-]+$")) }

        val scopedUrl = if (organization != null && original.url.encodedPath == "/user/repos") {
            original.url.newBuilder()
                .encodedPath("/orgs/$organization/repos")
                .build()
        } else {
            original.url
        }

        val request = original.newBuilder()
            .url(scopedUrl)
            // Preserve endpoint-specific media types. DownloadWorker requests
            // application/octet-stream; replacing it with GitHub's JSON media
            // type can make binary download endpoints return metadata instead.
            .apply {
                if (original.header("Accept") == null) {
                    header("Accept", "application/vnd.github+json")
                }
            }
            .header("X-GitHub-API-Version", BuildConfig.GITHUB_API_VERSION)
            .apply {
                tokenStore.read()?.accessToken?.takeIf(String::isNotBlank)?.let {
                    header("Authorization", "Bearer $it")
                }
            }
            .build()
        return chain.proceed(request)
    }
}
