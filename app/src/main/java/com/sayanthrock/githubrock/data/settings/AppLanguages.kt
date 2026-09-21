package com.sayanthrock.githubrock.data.settings

import java.util.Locale

data class AppLanguage(
    val tag: String,
    val displayName: String,
)

object AppLanguages {
    fun available(currentLocale: Locale = Locale.getDefault()): List<AppLanguage> {
        return Locale.getAvailableLocales()
            .asSequence()
            .filter { it.language.isNotBlank() }
            .map { locale ->
                val tag = locale.toLanguageTag()
                val displayName = locale.getDisplayName(currentLocale)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(currentLocale) else it.toString() }
                AppLanguage(tag, displayName)
            }
            .distinctBy { it.tag }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName })
            .toList()
    }
}
