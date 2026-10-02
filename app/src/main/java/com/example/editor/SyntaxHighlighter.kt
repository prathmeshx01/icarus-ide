package com.example.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.AppEditorTheme
import java.util.regex.Pattern

object SyntaxHighlighter {

    // Precompiled Regex Patterns for High-Performance 60fps Real-Time Colorization
    private val KOTLIN_COMMENTS = Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/")
    private val KOTLIN_CONTROL = Pattern.compile("\\b(if|else|when|for|while|do|return|break|continue|try|catch|finally|throw|in|!in|is|!is|as|as\\?)\\b")
    private val KOTLIN_DECLARATIONS = Pattern.compile("\\b(fun|val|var|class|interface|object|enum|sealed|annotation|data|override|abstract|open|final|private|protected|public|internal|lateinit|by|lazy|suspend|tailrec|operator|infix|inline|external|package|import|this|super|constructor|init|companion|typealias)\\b")
    private val KOTLIN_ANNOTATIONS = Pattern.compile("@[a-zA-Z0-9_]+")
    private val KOTLIN_TYPES = Pattern.compile("\\b(String|Int|Boolean|Float|Double|Long|Byte|Short|Char|Unit|Any|Nothing|List|MutableList|Map|MutableMap|Set|Array|Modifier|State|Flow|StateFlow|SharedFlow|CoroutineScope)\\b")
    private val KOTLIN_FUNCTIONS = Pattern.compile("([a-zA-Z0-9_]+)(?=\\s*\\()")
    private val KOTLIN_STRINGS = Pattern.compile("\"\"\"[\\s\\S]*?\"\"\"|\"(?:\\\\.|[^\"\\\\])*\"|'[^']*'")
    private val KOTLIN_NUMBERS = Pattern.compile("\\b(0x[0-9a-fA-F]+|0b[01]+|\\d+(\\.\\d+)?([fFL])?)\\b")

    private val HTML_COMMENTS = Pattern.compile("<!--[\\s\\S]*?-->")
    private val HTML_DOCTYPE = Pattern.compile("<!DOCTYPE[\\s\\S]*?>", Pattern.CASE_INSENSITIVE)
    private val HTML_TAGS = Pattern.compile("</?[a-zA-Z0-9\\-]+")
    private val HTML_ATTRIBUTES = Pattern.compile("\\s+([a-zA-Z0-9\\-]+)(?=\\s*=)")
    private val HTML_STRINGS = Pattern.compile("\"[^\"]*\"|'[^']*'")
    private val HTML_ENTITIES = Pattern.compile("&[a-zA-Z0-9#]+;")

    private val JS_COMMENTS = Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/")
    private val JS_CONTROL = Pattern.compile("\\b(if|else|for|while|do|switch|case|break|continue|return|try|catch|finally|throw|import|export|from|as|default|async|await)\\b")
    private val JS_DECLARATIONS = Pattern.compile("\\b(const|let|var|function|class|extends|new|this|typeof|instanceof|void|delete|yield)\\b")
    private val JS_BUILTINS = Pattern.compile("\\b(console|document|window|Math|JSON|Array|Object|Promise|setTimeout|setInterval|addEventListener|fetch|null|undefined|true|false|NaN)\\b")
    private val JS_FUNCTIONS = Pattern.compile("([a-zA-Z0-9_\\$]+)(?=\\s*\\()")
    private val JS_STRINGS = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|`(?:\\\\.|[^`\\\\])*`")
    private val JS_NUMBERS = Pattern.compile("\\b(0x[0-9a-fA-F]+|\\d+(\\.\\d+)?)\\b")

    private val CSS_COMMENTS = Pattern.compile("/\\*[\\s\\S]*?\\*/")
    private val CSS_SELECTORS = Pattern.compile("[.#]?[a-zA-Z0-9_\\-]+(?=\\s*\\{)")
    private val CSS_PROPERTIES = Pattern.compile("([a-zA-Z0-9\\-]+)(?=\\s*:)")
    private val CSS_NUMBERS = Pattern.compile("\\b\\d+(\\.\\d+)?(px|em|rem|%|vh|vw|s|ms|deg)?\\b")
    private val CSS_COLORS = Pattern.compile("#[0-9a-fA-F]{3,8}\\b")
    private val CSS_STRINGS = Pattern.compile("\"[^\"]*\"|'[^']*'")

    private val PY_COMMENTS = Pattern.compile("#.*")
    private val PY_CONTROL = Pattern.compile("\\b(def|class|if|elif|else|for|while|break|continue|return|yield|try|except|finally|raise|import|from|as|with|pass|lambda)\\b")
    private val PY_TYPES = Pattern.compile("\\b(None|True|False|self|int|float|str|bool|list|dict|set|tuple|print|len|range)\\b")
    private val PY_STRINGS = Pattern.compile("\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"(?:\\\\.|[^\"\\\\])*\"|'[^']*'")
    private val PY_NUMBERS = Pattern.compile("\\b\\d+(\\.\\d+)?\\b")

    fun highlight(code: String, extension: String, theme: AppEditorTheme = AppEditorTheme.OBSIDIAN_DARK): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")
        return when (extension.lowercase()) {
            "kt", "kts", "kotlin" -> highlightKotlin(code, theme)
            "html", "htm" -> highlightHtml(code, theme)
            "js", "javascript" -> highlightJs(code, theme)
            "css" -> highlightCss(code, theme)
            "py", "python" -> highlightPython(code, theme)
            else -> {
                val b = AnnotatedString.Builder(code)
                b.addStyle(SpanStyle(color = theme.textPrimary), 0, code.length)
                b.toAnnotatedString()
            }
        }
    }

    private fun highlightKotlin(code: String, theme: AppEditorTheme): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        builder.addStyle(SpanStyle(color = theme.textPrimary), 0, code.length)

        // 1. Comments
        applyPattern(builder, code, KOTLIN_COMMENTS, theme.commentColor)

        // 2. Control flow keywords
        applyPattern(builder, code, KOTLIN_CONTROL, theme.controlKeywordColor)

        // 3. Declarative & Modifier keywords
        applyPattern(builder, code, KOTLIN_DECLARATIONS, theme.keywordColor)

        // 4. Annotations: @Composable, @OptIn, etc.
        applyPattern(builder, code, KOTLIN_ANNOTATIONS, theme.functionColor)

        // 5. Types
        applyPattern(builder, code, KOTLIN_TYPES, theme.attributeColor)

        // 6. Function calls
        val funcMatcher = KOTLIN_FUNCTIONS.matcher(code)
        while (funcMatcher.find()) {
            val name = funcMatcher.group(1)
            if (name !in listOf("if", "when", "for", "while", "catch")) {
                builder.addStyle(SpanStyle(color = theme.functionColor), funcMatcher.start(1), funcMatcher.end(1))
            }
        }

        // 7. Strings & Character literals
        applyPattern(builder, code, KOTLIN_STRINGS, theme.stringColor)

        // 8. Numbers
        applyPattern(builder, code, KOTLIN_NUMBERS, theme.numberColor)

        return builder.toAnnotatedString()
    }

    private fun highlightHtml(code: String, theme: AppEditorTheme): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        builder.addStyle(SpanStyle(color = theme.textPrimary), 0, code.length)

        // Comments
        applyPattern(builder, code, HTML_COMMENTS, theme.commentColor)

        // Doctype
        applyPattern(builder, code, HTML_DOCTYPE, theme.controlKeywordColor)

        // Tags: <div or </div
        val tagMatcher = HTML_TAGS.matcher(code)
        while (tagMatcher.find()) {
            builder.addStyle(SpanStyle(color = theme.tagColor, fontWeight = FontWeight.SemiBold), tagMatcher.start(), tagMatcher.end())
        }

        // Attributes: class=, id=
        val attrMatcher = HTML_ATTRIBUTES.matcher(code)
        while (attrMatcher.find()) {
            builder.addStyle(SpanStyle(color = theme.attributeColor), attrMatcher.start(1), attrMatcher.end(1))
        }

        // Attribute strings
        applyPattern(builder, code, HTML_STRINGS, theme.stringColor)

        // Entity references: &bull;, &copy;
        applyPattern(builder, code, HTML_ENTITIES, theme.numberColor)

        return builder.toAnnotatedString()
    }

    private fun highlightJs(code: String, theme: AppEditorTheme): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        builder.addStyle(SpanStyle(color = theme.textPrimary), 0, code.length)

        // Comments
        applyPattern(builder, code, JS_COMMENTS, theme.commentColor)

        // Control Keywords
        applyPattern(builder, code, JS_CONTROL, theme.controlKeywordColor)

        // Declarative Keywords
        applyPattern(builder, code, JS_DECLARATIONS, theme.keywordColor)

        // Builtins & Global objects
        applyPattern(builder, code, JS_BUILTINS, theme.attributeColor)

        // Function invocations
        val funcMatcher = JS_FUNCTIONS.matcher(code)
        while (funcMatcher.find()) {
            val name = funcMatcher.group(1)
            if (name !in listOf("if", "for", "while", "switch", "catch")) {
                builder.addStyle(SpanStyle(color = theme.functionColor), funcMatcher.start(1), funcMatcher.end(1))
            }
        }

        // Strings
        applyPattern(builder, code, JS_STRINGS, theme.stringColor)

        // Numbers
        applyPattern(builder, code, JS_NUMBERS, theme.numberColor)

        return builder.toAnnotatedString()
    }

    private fun highlightCss(code: String, theme: AppEditorTheme): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        builder.addStyle(SpanStyle(color = theme.textPrimary), 0, code.length)

        applyPattern(builder, code, CSS_COMMENTS, theme.commentColor)
        applyPattern(builder, code, CSS_SELECTORS, theme.functionColor)

        val propMatcher = CSS_PROPERTIES.matcher(code)
        while (propMatcher.find()) {
            builder.addStyle(SpanStyle(color = theme.attributeColor), propMatcher.start(1), propMatcher.end(1))
        }

        applyPattern(builder, code, CSS_NUMBERS, theme.numberColor)
        applyPattern(builder, code, CSS_COLORS, theme.stringColor)
        applyPattern(builder, code, CSS_STRINGS, theme.stringColor)

        return builder.toAnnotatedString()
    }

    private fun highlightPython(code: String, theme: AppEditorTheme): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        builder.addStyle(SpanStyle(color = theme.textPrimary), 0, code.length)

        applyPattern(builder, code, PY_COMMENTS, theme.commentColor)
        applyPattern(builder, code, PY_CONTROL, theme.controlKeywordColor)
        applyPattern(builder, code, PY_TYPES, theme.keywordColor)
        applyPattern(builder, code, PY_STRINGS, theme.stringColor)
        applyPattern(builder, code, PY_NUMBERS, theme.numberColor)

        return builder.toAnnotatedString()
    }

    private fun applyPattern(builder: AnnotatedString.Builder, text: String, pattern: Pattern, color: androidx.compose.ui.graphics.Color) {
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            builder.addStyle(SpanStyle(color = color), matcher.start(), matcher.end())
        }
    }
}
