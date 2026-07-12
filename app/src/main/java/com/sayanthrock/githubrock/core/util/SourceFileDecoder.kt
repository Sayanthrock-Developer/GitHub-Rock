package com.sayanthrock.githubrock.core.util

import com.sayanthrock.githubrock.core.model.ContentEntry
import java.util.Base64

object SourceFileDecoder {
    fun decode(entry: ContentEntry): String {
        val content = entry.content.orEmpty()
        if (!entry.encoding.equals("base64", ignoreCase = true)) return content
        return Base64.getMimeDecoder().decode(content).toString(Charsets.UTF_8)
    }
}
