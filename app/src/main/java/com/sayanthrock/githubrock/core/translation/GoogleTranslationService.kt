package com.sayanthrock.githubrock.core.translation

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.languageid.LanguageIdentification
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
        /**
         * Languages supported by Google ML Kit on-device translation.
         * Keep this list aligned with TranslateLanguage.fromLanguageTag().
         */
        val supportedLanguages = listOf(
            TranslationLanguage("af", "Afrikaans"),
            TranslationLanguage("ar", "Arabic"),
            TranslationLanguage("be", "Belarusian"),
            TranslationLanguage("bn", "Bengali"),
            TranslationLanguage("bg", "Bulgarian"),
            TranslationLanguage("ca", "Catalan"),
            TranslationLanguage("zh", "Chinese"),
            TranslationLanguage("hr", "Croatian"),
            TranslationLanguage("cs", "Czech"),
            TranslationLanguage("da", "Danish"),
            TranslationLanguage("nl", "Dutch"),
            TranslationLanguage("en", "English"),
            TranslationLanguage("et", "Estonian"),
            TranslationLanguage("fi", "Finnish"),
            TranslationLanguage("fr", "French"),
            TranslationLanguage("de", "German"),
            TranslationLanguage("el", "Greek"),
            TranslationLanguage("gu", "Gujarati"),
            TranslationLanguage("he", "Hebrew"),
            TranslationLanguage("hi", "Hindi"),
            TranslationLanguage("hu", "Hungarian"),
            TranslationLanguage("id", "Indonesian"),
            TranslationLanguage("ga", "Irish"),
            TranslationLanguage("it", "Italian"),
            TranslationLanguage("ja", "Japanese"),
            TranslationLanguage("kn", "Kannada"),
            TranslationLanguage("ko", "Korean"),
            TranslationLanguage("lv", "Latvian"),
            TranslationLanguage("lt", "Lithuanian"),
            TranslationLanguage("mk", "Macedonian"),
            TranslationLanguage("ms", "Malay"),
            TranslationLanguage("mr", "Marathi"),
            TranslationLanguage("no", "Norwegian"),
            TranslationLanguage("fa", "Persian"),
            TranslationLanguage("pl", "Polish"),
            TranslationLanguage("pt", "Portuguese"),
            TranslationLanguage("ro", "Romanian"),
            TranslationLanguage("ru", "Russian"),
            TranslationLanguage("sk", "Slovak"),
            TranslationLanguage("sl", "Slovenian"),
            TranslationLanguage("es", "Spanish"),
            TranslationLanguage("sw", "Swahili"),
            TranslationLanguage("sv", "Swedish"),
            TranslationLanguage("tl", "Tagalog"),
            TranslationLanguage("ta", "Tamil"),
            TranslationLanguage("te", "Telugu"),
            TranslationLanguage("th", "Thai"),
            TranslationLanguage("tr", "Turkish"),
            TranslationLanguage("uk", "Ukrainian"),
            TranslationLanguage("ur", "Urdu"),
            TranslationLanguage("vi", "Vietnamese"),
            TranslationLanguage("cy", "Welsh")
    }
}
