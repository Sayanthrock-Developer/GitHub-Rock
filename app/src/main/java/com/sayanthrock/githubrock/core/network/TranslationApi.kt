package com.sayanthrock.githubrock.core.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@Serializable
data class LibreTranslateRequest(
    val q: String,
    val source: String = "auto",
    val target: String,
    val format: String = "text",
    val api_key: String? = null
)

@Serializable
data class LibreTranslateResponse(
    val translatedText: String
)

@Serializable
data class TranslationLanguage(
    val code: String,
    val name: String,
    val targets: List<String> = emptyList()
)

interface TranslationApi {
    @POST("translate")
    suspend fun translate(@Body request: LibreTranslateRequest): LibreTranslateResponse

    @GET("languages")
    suspend fun languages(): List<TranslationLanguage>
}
