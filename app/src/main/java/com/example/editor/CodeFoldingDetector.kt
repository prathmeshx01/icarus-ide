package com.example.editor

data class FoldingBlock(
    val startLine: Int, // 1-based line index
    val endLine: Int,   // 1-based line index
    val previewLabel: String = "{ ... }"
)

object CodeFoldingDetector {

    /**
     * Detects collapsible code blocks (braces, HTML tags, and indentation blocks).
     * Returns a list of [FoldingBlock]s where startLine and endLine are 1-based.
     */
    fun detectFoldingBlocks(text: String, extension: String): List<FoldingBlock> {
        val lines = text.split("\n")
        if (lines.size < 2) return emptyList()

        return when (extension.lowercase()) {
            "py", "python" -> detectIndentationBlocks(lines)
            "html", "htm" -> detectBraceAndHtmlBlocks(lines)
            else -> detectBraceBlocks(lines)
        }
    }

    /**
     * Detects matching { and } across lines.
     */
    private fun detectBraceBlocks(lines: List<String>): List<FoldingBlock> {
        val blocks = mutableListOf<FoldingBlock>()
        val braceStack = mutableListOf<Int>() // stores 1-based line numbers of open '{'

        for (i in lines.indices) {
            val lineNum = i + 1
            val line = lines[i]

            // Count unescaped '{' and '}' outside strings
            var inSingle = false
            var inDouble = false
            var inComment = false

            var col = 0
            while (col < line.length) {
                val c = line[col]

                if (!inSingle && !inDouble && col + 1 < line.length && line[col] == '/' && line[col + 1] == '/') {
                    break // Rest of line is comment
                }

                if (c == '"' && (col == 0 || line[col - 1] != '\\')) {
                    if (!inSingle) inDouble = !inDouble
                } else if (c == '\'' && (col == 0 || line[col - 1] != '\\')) {
                    if (!inDouble) inSingle = !inSingle
                } else if (!inSingle && !inDouble) {
                    if (c == '{') {
                        braceStack.add(lineNum)
                    } else if (c == '}') {
                        if (braceStack.isNotEmpty()) {
                            val start = braceStack.removeAt(braceStack.lastIndex)
                            if (lineNum > start) {
                                blocks.add(FoldingBlock(startLine = start, endLine = lineNum, previewLabel = "{ ... }"))
                            }
                        }
                    }
                }
                col++
            }
        }

        return blocks.sortedBy { it.startLine }
    }

    /**
     * Detects HTML container tags (<div... > ... </div>, <ul...>, <section>, etc.)
     * as well as embedded CSS/JS braces.
     */
    private fun detectBraceAndHtmlBlocks(lines: List<String>): List<FoldingBlock> {
        val blocks = detectBraceBlocks(lines).toMutableList()
        val tagStack = mutableListOf<Pair<String, Int>>() // tag name to 1-based line

        val tagOpenRegex = Regex("<([a-zA-Z0-9\\-]+)(?:\\s+[^>]*)?(?<!/)>")
        val tagCloseRegex = Regex("</([a-zA-Z0-9\\-]+)>")

        val voidTags = setOf("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr")

        for (i in lines.indices) {
            val lineNum = i + 1
            val line = lines[i].trim()

            // Check opening tags
            for (match in tagOpenRegex.findAll(line)) {
                val tagName = match.groupValues[1].lowercase()
                if (tagName !in voidTags && !line.contains("</$tagName>")) {
                    tagStack.add(tagName to lineNum)
                }
            }

            // Check closing tags
            for (match in tagCloseRegex.findAll(line)) {
                val tagName = match.groupValues[1].lowercase()
                val lastIdx = tagStack.indexOfLast { it.first == tagName }
                if (lastIdx >= 0) {
                    val (tag, start) = tagStack.removeAt(lastIdx)
                    if (lineNum > start && blocks.none { it.startLine == start }) {
                        blocks.add(FoldingBlock(startLine = start, endLine = lineNum, previewLabel = "<$tag>...</$tag>"))
                    }
                }
            }
        }

        return blocks.sortedBy { it.startLine }
    }

    /**
     * Detects indentation-based blocks (for Python, YAML, etc.).
     */
    private fun detectIndentationBlocks(lines: List<String>): List<FoldingBlock> {
        val blocks = mutableListOf<FoldingBlock>()
        val indentStack = mutableListOf<Pair<Int, Int>>() // 1-based line to indent level

        for (i in lines.indices) {
            val lineNum = i + 1
            val line = lines[i]
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue

            val indent = line.takeWhile { it == ' ' }.length

            // If indentation decreased, close previous blocks
            while (indentStack.isNotEmpty() && indentStack.last().second >= indent) {
                val (start, prevIndent) = indentStack.removeAt(indentStack.lastIndex)
                if (lineNum - 1 > start) {
                    blocks.add(FoldingBlock(startLine = start, endLine = lineNum - 1, previewLabel = ": ..."))
                }
            }

            // If line ends with ':' it starts a block
            if (line.trimEnd().endsWith(":")) {
                indentStack.add(lineNum to indent)
            }
        }

        // Close any remaining open blocks
        while (indentStack.isNotEmpty()) {
            val (start, _) = indentStack.removeAt(indentStack.lastIndex)
            if (lines.size > start) {
                blocks.add(FoldingBlock(startLine = start, endLine = lines.size, previewLabel = ": ..."))
            }
        }

        return blocks.sortedBy { it.startLine }
    }
}
