package com.sayanthrock.githubrock.data.translation

import com.sayanthrock.githubrock.core.network.TranslationApi
import com.sayanthrock.githubrock.core.network.TranslationLanguage
import com.sayanthrock.githubrock.core.network.LibreTranslateRequest
import javax.inject.Inject
import javax.inject.Singleton

interface TranslationProvider {
    suspend fun languages(): List<TranslationLanguage>
    suspend fun translate(text: String, target: String, source: String = "auto"): String
}

@Singleton
class LibreTranslateProvider @Inject constructor(
    private val api: TranslationApi
) : TranslationProvider {
    override suspend fun languages(): List<TranslationLanguage> = api.languages()

    override suspend fun translate(text: String, target: String, source: String): String =
        api.translate(
            LibreTranslateRequest(
                q = text,
                source = source,
                target = target
            )
        ).translatedText
}
