package com.sayanthrock.githubrock.core.util

import com.sayanthrock.githubrock.core.model.ContentEntry
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.Base64

object SourceFileDecoder {
    /**
 * Decodes a content entry into validated text.
 *
 * @param entry The content entry to decode.
 * @return The decoded text.
 */
fun decode(entry: ContentEntry): String = decodeStrictText(entry)

    /**
     * Decodes file content as strict text and rejects unavailable content or binary control characters.
     *
     * @param entry The file entry containing the content and its encoding.
     * @return The decoded and validated text.
     * @throws NullPointerException If the file content is unavailable.
     * @throws IllegalArgumentException If the content is invalid Base64 or contains binary control characters.
     */
    fun decodeStrictText(entry: ContentEntry): String {
        val content = requireNotNull(entry.content) {
            "The file content is unavailable and cannot be opened as text"
        }
        val text = if (entry.encoding.equals("base64", ignoreCase = true)) {
            val normalizedBase64 = content.replace("\r", "").replace("\n", "")
            val bytes = Base64.getDecoder().decode(normalizedBase64)
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
