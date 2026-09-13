package com.sayanthrock.githubrock.data.translation

import com.sayanthrock.githubrock.core.network.TranslationLanguage
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationRepository @Inject constructor(
    private val provider: TranslationProvider
) {
    private val cache = ConcurrentHashMap<CacheKey, String>()

    suspend fun languages(): List<TranslationLanguage> = provider.languages()

    suspend fun translate(text: String, target: String, source: String = "auto"): String {
        require(text.isNotBlank()) { "There is no content to translate." }
        require(target.isNotBlank()) { "Choose a target language." }
        val key = CacheKey(text, source, target)
        return cache[key] ?: provider.translate(text, target, source).also { translated ->
            if (translated.isNotBlank()) cache[key] = translated
        }
    }

    private data class CacheKey(val text: String, val source: String, val target: String)
}
