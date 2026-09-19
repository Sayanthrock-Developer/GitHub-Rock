package com.sayanthrock.githubrock.core.translation

import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.languageid.LanguageIdentification
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class TranslationLanguage(
    val code: String,
    val label: String
)

@Singleton
class GoogleTranslationService @Inject constructor() {
    private val translators = ConcurrentHashMap<String, Translator>()
    private val languageIdentifier by lazy { LanguageIdentification.getClient() }

    suspend fun detectLanguage(text: String): String? {
        if (text.isBlank()) return null
        return runCatching {
            val code = awaitTask<String> { success, failure ->
                languageIdentifier.identifyLanguage(text)
                    .addOnSuccessListener(success)
                    .addOnFailureListener(failure)
            }
            code.takeUnless { it == "und" }
        }.getOrNull()
    }

    suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String? = null
    ): String {
        if (text.isBlank() || targetLanguage.isBlank()) return text
        val source = sourceLanguage ?: detectLanguage(text)
            ?: throw IllegalArgumentException("Google could not identify the source language.")
        if (source.equals(targetLanguage, ignoreCase = true)) return text

        val sourceCode = TranslateLanguage.fromLanguageTag(source)
            ?: throw IllegalArgumentException("Unsupported source language: $source")
        val targetCode = TranslateLanguage.fromLanguageTag(targetLanguage)
            ?: throw IllegalArgumentException("Unsupported target language: $targetLanguage")

        val key = "$sourceCode->$targetCode"
        val translator = translators.getOrPut(key) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(sourceCode)
                    .setTargetLanguage(targetCode)
                    .build()
            )
        }

        awaitTask<Unit> { success, failure ->
            translator.downloadModelIfNeeded()
                .addOnSuccessListener { success(Unit) }
                .addOnFailureListener(failure)
        }
        return awaitTask { success, failure ->
            translator.translate(text)
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }
    }

    fun close() {
        translators.values.forEach { it.close() }
        translators.clear()
        languageIdentifier.close()
    }

    private suspend fun <T> awaitTask(register: (success: (T) -> Unit, failure: (Exception) -> Unit) -> Unit): T =
        suspendCancellableCoroutine { continuation ->
            register(
                { value -> if (continuation.isActive) continuation.resume(value) },
                { error -> if (continuation.isActive) continuation.resumeWithException(error) }
            )
        }

    companion object {
        val supportedLanguages = listOf(
            TranslationLanguage("en", "English"),
            TranslationLanguage("ml", "Malayalam"),
            TranslationLanguage("hi", "Hindi"),
            TranslationLanguage("ta", "Tamil"),
            TranslationLanguage("te", "Telugu"),
            TranslationLanguage("kn", "Kannada"),
            TranslationLanguage("bn", "Bengali"),
            TranslationLanguage("mr", "Marathi"),
            TranslationLanguage("gu", "Gujarati"),
            TranslationLanguage("es", "Spanish"),
            TranslationLanguage("fr", "French"),
            TranslationLanguage("de", "German"),
            TranslationLanguage("pt", "Portuguese"),
            TranslationLanguage("ja", "Japanese"),
            TranslationLanguage("ko", "Korean"),
            TranslationLanguage("zh", "Chinese"),
            TranslationLanguage("ar", "Arabic"),
            TranslationLanguage("ru", "Russian")
        )
    }
}
