package com.sayanthrock.githubrock.core.util

import com.sayanthrock.githubrock.core.model.ContentEntry
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.Base64

object SourceFileDecoder {
    fun decode(entry: ContentEntry): String = decodeStrictText(entry)

    fun decodeStrictText(entry: ContentEntry): String {
        val content = requireNotNull(entry.content) {
            "The file content is unavailable and cannot be opened as text"
        }
        val text = if (entry.encoding.equals("base64", ignoreCase = true)) {
            val bytes = Base64.getMimeDecoder().decode(content)
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } else {
            content
        }
        require(text.none(::isBinaryControlCharacter)) {
            "The file contains binary control characters and cannot be opened as text"
        }
        return text
    }

    private fun isBinaryControlCharacter(character: Char): Boolean =
        character == '\u0000' ||
            character.code in 1..8 ||
            character.code in 11..12 ||
            character.code in 14..31 ||
            character.code == 127
}
