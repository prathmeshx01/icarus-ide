package com.example.editor

object CodeFormatter {

    fun format(code: String, extension: String): String {
        return when (extension.lowercase()) {
            "html", "htm" -> formatHtml(code)
            "css" -> formatCss(code)
            "js", "javascript" -> formatBraceLanguage(code)
            "py", "python" -> code.trimEnd() + "\n"
            else -> code
        }
    }

    private fun formatBraceLanguage(code: String): String {
        val lines = code.split("\n")
        val result = mutableListOf<String>()
        var indentLevel = 0

        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) {
                result.add("")
                continue
            }

            // If line starts with closing brace, decrease indent first
            if (trimmed.startsWith("}") || trimmed.startsWith("]") || trimmed.startsWith(")")) {
                indentLevel = maxOf(0, indentLevel - 1)
            }

            val indent = "  ".repeat(indentLevel)
            result.add(indent + trimmed)

            // If line ends with opening brace, increase indent for next lines
            val opens = trimmed.count { it == '{' || it == '[' || it == '(' }
            val closes = trimmed.count { it == '}' || it == ']' || it == ')' }
            val diff = opens - closes
            if (diff > 0) {
                indentLevel += diff
            }
        }

        return result.joinToString("\n")
    }

    private fun formatCss(code: String): String {
        return formatBraceLanguage(code)
    }

    private fun formatHtml(code: String): String {
        val voidElements = setOf("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr", "!doctype")
        val lines = code.split("\n")
        val result = mutableListOf<String>()
        var indentLevel = 0

        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) {
                result.add("")
                continue
            }

            val startsWithClosing = trimmed.startsWith("</")
            if (startsWithClosing) {
                indentLevel = maxOf(0, indentLevel - 1)
            }

            val indent = "  ".repeat(indentLevel)
            result.add(indent + trimmed)

            // Calculate change in tag balance on this line
            val openMatcher = java.util.regex.Pattern.compile("<([a-zA-Z0-9\\-]+)([^>]*)>").matcher(trimmed)
            var lineOpens = 0
            while (openMatcher.find()) {
                val tag = openMatcher.group(1).lowercase()
                val isSelf = openMatcher.group(2)?.trim()?.endsWith("/") == true
                if (!isSelf && tag !in voidElements) {
                    lineOpens++
                }
            }

            val closeMatcher = java.util.regex.Pattern.compile("</([a-zA-Z0-9\\-]+)>").matcher(trimmed)
            var lineCloses = 0
            while (closeMatcher.find()) {
                lineCloses++
            }

            val netChange = lineOpens - lineCloses
            if (netChange > 0 && !startsWithClosing) {
                indentLevel += netChange
            }
        }

        return result.joinToString("\n")
    }
}
