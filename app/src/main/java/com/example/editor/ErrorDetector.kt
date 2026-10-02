package com.example.editor

enum class DiagnosticSeverity {
    ERROR,
    WARNING,
    INFO
}

data class QuickFix(
    val title: String,
    val applyFix: (String) -> String
)

data class CodeDiagnostic(
    val line: Int,
    val column: Int,
    val message: String,
    val severity: DiagnosticSeverity = DiagnosticSeverity.ERROR,
    val quickFix: QuickFix? = null
)

object ErrorDetector {

    fun detectErrors(code: String, extension: String): List<CodeDiagnostic> {
        val diagnostics = mutableListOf<CodeDiagnostic>()
        if (code.isBlank()) return diagnostics

        // 1. Bracket Matching Check (for all languages)
        checkBrackets(code, diagnostics)

        // 2. Unclosed string literals
        checkStrings(code, diagnostics)

        // 3. Language-specific checks
        when (extension.lowercase()) {
            "html", "htm" -> checkHtml(code, diagnostics)
            "css" -> checkCss(code, diagnostics)
            "js", "javascript" -> checkJs(code, diagnostics)
            "py", "python" -> checkPython(code, diagnostics)
        }

        return diagnostics.sortedBy { it.line }
    }

    private fun checkBrackets(code: String, diagnostics: MutableList<CodeDiagnostic>) {
        val stack = ArrayDeque<Pair<Char, Int>>() // char to index
        var inSingleQuote = false
        var inDoubleQuote = false
        var inBacktick = false
        var inComment = false

        val lines = code.split("\n")

        for (i in code.indices) {
            val c = code[i]
            val prev = if (i > 0) code[i - 1] else ' '

            // Simple string & comment skips
            if (c == '"' && prev != '\\' && !inSingleQuote && !inBacktick && !inComment) inDoubleQuote = !inDoubleQuote
            if (c == '\'' && prev != '\\' && !inDoubleQuote && !inBacktick && !inComment) inSingleQuote = !inSingleQuote
            if (c == '`' && prev != '\\' && !inSingleQuote && !inDoubleQuote) inBacktick = !inBacktick

            if (inSingleQuote || inDoubleQuote || inBacktick) continue

            when (c) {
                '(', '{', '[' -> stack.addLast(c to i)
                ')', '}', ']' -> {
                    if (stack.isEmpty()) {
                        val (line, col) = getLineAndCol(code, i)
                        diagnostics.add(
                            CodeDiagnostic(
                                line = line,
                                column = col,
                                message = "Unexpected closing '$c' with no matching opening bracket",
                                severity = DiagnosticSeverity.ERROR
                            )
                        )
                    } else {
                        val (topChar, _) = stack.last()
                        val isMatch = (topChar == '(' && c == ')') ||
                                (topChar == '{' && c == '}') ||
                                (topChar == '[' && c == ']')
                        if (isMatch) {
                            stack.removeLast()
                        } else {
                            val (line, col) = getLineAndCol(code, i)
                            val expected = when (topChar) {
                                '(' -> ')'
                                '{' -> '}'
                                else -> ']'
                            }
                            diagnostics.add(
                                CodeDiagnostic(
                                    line = line,
                                    column = col,
                                    message = "Mismatched bracket '$c'. Expected '$expected'",
                                    severity = DiagnosticSeverity.ERROR
                                )
                            )
                        }
                    }
                }
            }
        }

        while (stack.isNotEmpty()) {
            val (unclosed, index) = stack.removeLast()
            val (line, col) = getLineAndCol(code, index)
            val closer = when (unclosed) {
                '(' -> ')'
                '{' -> '}'
                else -> ']'
            }
            diagnostics.add(
                CodeDiagnostic(
                    line = line,
                    column = col,
                    message = "Unclosed bracket '$unclosed'. Missing '$closer'",
                    severity = DiagnosticSeverity.ERROR,
                    quickFix = QuickFix("Insert '$closer'") { currentText ->
                        currentText + "\n" + closer
                    }
                )
            )
        }
    }

    private fun checkStrings(code: String, diagnostics: MutableList<CodeDiagnostic>) {
        val lines = code.split("\n")
        for ((lineIdx, lineText) in lines.withIndex()) {
            // Check odd quotes on a single line (ignoring escaped ones)
            val dQuotes = lineText.countQuotes('"')
            val sQuotes = lineText.countQuotes('\'')

            if (dQuotes % 2 != 0) {
                diagnostics.add(
                    CodeDiagnostic(
                        line = lineIdx + 1,
                        column = lineText.length,
                        message = "Unterminated string literal (missing double quote)",
                        severity = DiagnosticSeverity.ERROR,
                        quickFix = QuickFix("Close double quote") { cur ->
                            val l = cur.split("\n").toMutableList()
                            if (lineIdx in l.indices) {
                                l[lineIdx] = l[lineIdx] + "\""
                            }
                            l.joinToString("\n")
                        }
                    )
                )
            }
            if (sQuotes % 2 != 0) {
                diagnostics.add(
                    CodeDiagnostic(
                        line = lineIdx + 1,
                        column = lineText.length,
                        message = "Unterminated string literal (missing single quote)",
                        severity = DiagnosticSeverity.ERROR,
                        quickFix = QuickFix("Close single quote") { cur ->
                            val l = cur.split("\n").toMutableList()
                            if (lineIdx in l.indices) {
                                l[lineIdx] = l[lineIdx] + "'"
                            }
                            l.joinToString("\n")
                        }
                    )
                )
            }
        }
    }

    private fun checkHtml(code: String, diagnostics: MutableList<CodeDiagnostic>) {
        val voidElements = setOf("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr", "!doctype")
        val tagPattern = java.util.regex.Pattern.compile("<(/?[a-zA-Z0-9\\-]+)([^>]*)>")
        val matcher = tagPattern.matcher(code)

        val openTags = ArrayDeque<Pair<String, Int>>() // tag name to index

        while (matcher.find()) {
            val rawTag = matcher.group(1) ?: continue
            val index = matcher.start()

            if (rawTag.startsWith("/")) {
                val closeName = rawTag.substring(1).lowercase()
                if (openTags.isEmpty()) {
                    val (line, col) = getLineAndCol(code, index)
                    diagnostics.add(
                        CodeDiagnostic(
                            line = line,
                            column = col,
                            message = "Closing tag </$closeName> has no matching opening tag",
                            severity = DiagnosticSeverity.WARNING
                        )
                    )
                } else {
                    val (lastOpen, openIdx) = openTags.removeLast()
                    if (lastOpen != closeName) {
                        val (line, col) = getLineAndCol(code, index)
                        diagnostics.add(
                            CodeDiagnostic(
                                line = line,
                                column = col,
                                message = "Mismatched HTML tag: expected </$lastOpen> but found </$closeName>",
                                severity = DiagnosticSeverity.ERROR
                            )
                        )
                    }
                }
            } else {
                val openName = rawTag.lowercase()
                val selfClosing = matcher.group(2)?.trim()?.endsWith("/") == true
                if (!selfClosing && openName !in voidElements) {
                    openTags.addLast(openName to index)
                }
            }
        }

        while (openTags.isNotEmpty()) {
            val (unclosedTag, idx) = openTags.removeLast()
            val (line, col) = getLineAndCol(code, idx)
            diagnostics.add(
                CodeDiagnostic(
                    line = line,
                    column = col,
                    message = "Unclosed HTML tag <$unclosedTag>. Missing </$unclosedTag>",
                    severity = DiagnosticSeverity.WARNING,
                    quickFix = QuickFix("Close with </$unclosedTag>") { cur ->
                        cur + "\n</$unclosedTag>"
                    }
                )
            )
        }
    }

    private fun checkCss(code: String, diagnostics: MutableList<CodeDiagnostic>) {
        val lines = code.split("\n")
        var inRule = false
        for ((lineIdx, lineText) in lines.withIndex()) {
            val trimmed = lineText.trim()
            if (trimmed.contains("{")) inRule = true
            if (trimmed.contains("}")) inRule = false

            // Inside a CSS rule, check for properties missing semicolon
            if (inRule && trimmed.contains(":") && !trimmed.endsWith(";") && !trimmed.endsWith("{") && !trimmed.endsWith(",")) {
                diagnostics.add(
                    CodeDiagnostic(
                        line = lineIdx + 1,
                        column = lineText.length,
                        message = "CSS declaration is missing a trailing semicolon ';'",
                        severity = DiagnosticSeverity.WARNING,
                        quickFix = QuickFix("Add semicolon ';'") { cur ->
                            val l = cur.split("\n").toMutableList()
                            if (lineIdx in l.indices) {
                                l[lineIdx] = l[lineIdx] + ";"
                            }
                            l.joinToString("\n")
                        }
                    )
                )
            }
        }
    }

    private fun checkJs(code: String, diagnostics: MutableList<CodeDiagnostic>) {
        val lines = code.split("\n")
        for ((lineIdx, lineText) in lines.withIndex()) {
            val trimmed = lineText.trim()
            // Check for loose '=' instead of '==' in if statements: e.g., if (a = 5)
            if (trimmed.startsWith("if") && trimmed.contains("=") && !trimmed.contains("==") && !trimmed.contains("===") && !trimmed.contains("<=") && !trimmed.contains(">=")) {
                diagnostics.add(
                    CodeDiagnostic(
                        line = lineIdx + 1,
                        column = lineText.indexOf('='),
                        message = "Assignment '=' inside conditional statement. Did you mean '==='?",
                        severity = DiagnosticSeverity.WARNING
                    )
                )
            }
        }
    }

    private fun checkPython(code: String, diagnostics: MutableList<CodeDiagnostic>) {
        val lines = code.split("\n")
        for ((lineIdx, lineText) in lines.withIndex()) {
            val trimmed = lineText.trim()
            // Check missing colon on control statements
            val isControl = trimmed.startsWith("def ") || trimmed.startsWith("class ") ||
                    trimmed.startsWith("if ") || trimmed.startsWith("elif ") ||
                    trimmed.startsWith("else") || trimmed.startsWith("for ") ||
                    trimmed.startsWith("while ") || trimmed.startsWith("try") ||
                    trimmed.startsWith("except") || trimmed.startsWith("with ")

            if (isControl && !trimmed.endsWith(":") && !trimmed.contains("#")) {
                diagnostics.add(
                    CodeDiagnostic(
                        line = lineIdx + 1,
                        column = lineText.length,
                        message = "Missing colon ':' at the end of statement",
                        severity = DiagnosticSeverity.ERROR,
                        quickFix = QuickFix("Add ':' at end") { cur ->
                            val l = cur.split("\n").toMutableList()
                            if (lineIdx in l.indices) {
                                l[lineIdx] = l[lineIdx] + ":"
                            }
                            l.joinToString("\n")
                        }
                    )
                )
            }
        }
    }

    private fun getLineAndCol(text: String, charIndex: Int): Pair<Int, Int> {
        var line = 1
        var col = 1
        val safeIndex = charIndex.coerceIn(0, text.length)
        for (i in 0 until safeIndex) {
            if (text[i] == '\n') {
                line++
                col = 1
            } else {
                col++
            }
        }
        return line to col
    }

    private fun String.countQuotes(q: Char): Int {
        var count = 0
        var escaped = false
        for (ch in this) {
            if (ch == '\\') {
                escaped = !escaped
            } else if (ch == q && !escaped) {
                count++
                escaped = false
            } else {
                escaped = false
            }
        }
        return count
    }
}
