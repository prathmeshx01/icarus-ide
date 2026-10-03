package com.example.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object AutoCloser {

    private val VOID_HTML_TAGS = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input",
        "link", "meta", "param", "source", "track", "wbr", "!doctype"
    )

    /**
     * Automatically completes closing tags (e.g. <html> -> <html></html>) and closing brackets/quotes.
     */
    fun handleTextChange(
        oldValue: TextFieldValue,
        newValue: TextFieldValue,
        extension: String,
        autoCloseBrackets: Boolean = true
    ): TextFieldValue {
        if (!autoCloseBrackets) return newValue

        val oldText = oldValue.text
        val newText = newValue.text

        // Single character insertion check
        if (newText.length != oldText.length + 1) return newValue

        val cursor = newValue.selection.min
        if (cursor <= 0 || cursor > newText.length) return newValue

        val insertedChar = newText[cursor - 1]

        // 1. Tag auto-closing when '>' is typed in HTML/JSX/TSX/Vue/XML files
        if (insertedChar == '>') {
            val isMarkup = extension.lowercase() in listOf("html", "htm", "xml", "svg", "vue", "jsx", "tsx", "php")
            if (isMarkup) {
                val tagOpenIdx = newText.lastIndexOf('<', (cursor - 2).coerceAtLeast(0))
                if (tagOpenIdx >= 0) {
                    val candidate = newText.substring(tagOpenIdx + 1, cursor - 1).trim()
                    if (candidate.isNotEmpty() && !candidate.startsWith("/") && !candidate.startsWith("!") && !candidate.startsWith("?")) {
                        // Extract tag name (up to whitespace or attribute)
                        val tagName = candidate.split(Regex("\\s+"))[0].lowercase()
                        if (tagName.isNotEmpty() && tagName !in VOID_HTML_TAGS) {
                            val closingTag = "</$tagName>"
                            val updatedText = newText.substring(0, cursor) + closingTag + newText.substring(cursor)
                            return TextFieldValue(updatedText, TextRange(cursor))
                        }
                    }
                }
            }
        }

        // 2. Bracket and quote auto-closing
        val pairCloser = when (insertedChar) {
            '{' -> '}'
            '(' -> ')'
            '[' -> ']'
            '"' -> '"'
            '\'' -> '\''
            '`' -> '`'
            else -> null
        }

        if (pairCloser != null) {
            // Don't auto-close quotes if followed immediately by alphanumeric characters
            if ((insertedChar == '"' || insertedChar == '\'' || insertedChar == '`') && cursor < newText.length && newText[cursor].isLetterOrDigit()) {
                return newValue
            }

            val updatedText = newText.substring(0, cursor) + pairCloser + newText.substring(cursor)
            return TextFieldValue(updatedText, TextRange(cursor))
        }

        return newValue
    }
}
