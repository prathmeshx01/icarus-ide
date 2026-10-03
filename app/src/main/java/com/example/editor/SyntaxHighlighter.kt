package com.example.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppEditorTheme
import java.util.regex.Pattern

object SyntaxHighlighter {

    // Precompiled high-performance regular expressions for real-time 60fps token colorization
    private val KOTLIN_COMMENTS = Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/")
    private val KOTLIN_CONTROL = Pattern.compile("\\b(if|else|when|for|while|do|return|break|continue|try|catch|finally|throw|in|!in|is|!is|as|as\\?)\\b")
    private val KOTLIN_DECLARATIONS = Pattern.compile("\\b(fun|val|var|class|interface|object|enum|sealed|annotation|data|override|abstract|open|final|private|protected|public|internal|lateinit|by|lazy|suspend|tailrec|operator|infix|inline|external|package|import|this|super|constructor|init|companion|typealias)\\b")
    private val KOTLIN_ANNOTATIONS = Pattern.compile("@[a-zA-Z0-9_]+")
    private val KOTLIN_TYPES = Pattern.compile("\\b(String|Int|Boolean|Float|Double|Long|Byte|Short|Char|Unit|Any|Nothing|List|MutableList|Map|MutableMap|Set|Array|Modifier|State|Flow|StateFlow|SharedFlow|CoroutineScope|Composable)\\b")
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
    private val JS_CONTROL = Pattern.compile("\\b(if|else|for|while|do|switch|case|break|continue|return|try|catch|finally|throw|import|export|from|as|default|async|await|yield)\\b")
    private val JS_DECLARATIONS = Pattern.compile("\\b(const|let|var|function|class|extends|new|this|typeof|instanceof|void|delete)\\b")
    private val JS_BUILTINS = Pattern.compile("\\b(console|document|window|Math|JSON|Array|Object|Promise|setTimeout|setInterval|addEventListener|fetch|null|undefined|true|false|NaN|process)\\b")
    private val JS_FUNCTIONS = Pattern.compile("([a-zA-Z0-9_\\$]+)(?=\\s*\\()")
    private val JS_OBJECT_KEYS = Pattern.compile("([a-zA-Z0-9_\\$]+)(?=\\s*:)")
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
    private val PY_FUNCTIONS = Pattern.compile("([a-zA-Z0-9_]+)(?=\\s*\\()")
    private val PY_STRINGS = Pattern.compile("\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"(?:\\\\.|[^\"\\\\])*\"|'[^']*'")
    private val PY_NUMBERS = Pattern.compile("\\b\\d+(\\.\\d+)?\\b")

    fun highlight(code: String, extension: String, theme: AppEditorTheme = AppEditorTheme.OBSIDIAN_DARK): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")
        return when (extension.lowercase()) {
            "kt", "kts", "kotlin" -> highlightKotlin(code, theme)
            "html", "htm" -> highlightHtml(code, theme)
            "js", "javascript", "ts", "typescript", "jsx", "tsx" -> highlightJs(code, theme)
            "css" -> highlightCss(code, theme)
            "py", "python" -> highlightPython(code, theme)
            else -> {
                val b = AnnotatedString.Builder(code)
                b.addStyle(SpanStyle(color = theme.textPrimary, letterSpacing = 0.4.sp), 0, code.length)
                b.toAnnotatedString()
            }
        }
    }

    private fun highlightKotlin(code: String, theme: AppEditorTheme): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        builder.addStyle(SpanStyle(color = theme.textPrimary, letterSpacing = 0.4.sp), 0, code.length)

        // Control flow keywords: if, return, else, when
        applyPattern(builder, code, KOTLIN_CONTROL, theme.controlKeywordColor, isBold = true)

        // Declarations: fun, val, var, class, import
        applyPattern(builder, code, KOTLIN_DECLARATIONS, theme.keywordColor, isBold = true)

        // Annotations: @Composable
        applyPattern(builder, code, KOTLIN_ANNOTATIONS, theme.functionColor)

        // Types: String, Int, List
        applyPattern(builder, code, KOTLIN_TYPES, theme.attributeColor)

        // Function invocations
        val funcMatcher = KOTLIN_FUNCTIONS.matcher(code)
        while (funcMatcher.find()) {
            val name = funcMatcher.group(1)
            if (name !in listOf("if", "when", "for", "while", "catch")) {
                builder.addStyle(SpanStyle(color = theme.functionColor), funcMatcher.start(1), funcMatcher.end(1))
            }
        }

        // Strings
        applyPattern(builder, code, KOTLIN_STRINGS, theme.stringColor)

        // Numbers
        applyPattern(builder, code, KOTLIN_NUMBERS, theme.numberColor)

        // Comments (applied last to override tokens within comments)
        applyPattern(builder, code, KOTLIN_COMMENTS, theme.commentColor, isItalic = true)

        return builder.toAnnotatedString()
    }

    private fun highlightHtml(code: String, theme: AppEditorTheme): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        builder.addStyle(SpanStyle(color = theme.textPrimary, letterSpacing = 0.4.sp), 0, code.length)

        // Doctype
        applyPattern(builder, code, HTML_DOCTYPE, theme.controlKeywordColor, isBold = true)

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

        // Comments
        applyPattern(builder, code, HTML_COMMENTS, theme.commentColor, isItalic = true)

        return builder.toAnnotatedString()
    }

    private fun highlightJs(code: String, theme: AppEditorTheme): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        builder.addStyle(SpanStyle(color = theme.textPrimary, letterSpacing = 0.4.sp), 0, code.length)

        // Control Keywords (async, await, return, import, export, from)
        applyPattern(builder, code, JS_CONTROL, theme.controlKeywordColor, isBold = true)

        // Declarative Keywords (const, let, var, function)
        applyPattern(builder, code, JS_DECLARATIONS, theme.keywordColor, isBold = true)

        // Builtins & Global objects (console, document, window, fetch)
        applyPattern(builder, code, JS_BUILTINS, theme.attributeColor)

        // Object keys in literals (headers: { "Authorization": ... })
        applyPattern(builder, code, JS_OBJECT_KEYS, theme.attributeColor)

        // Function invocations (fetchUserData, log)
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

        // Comments
        applyPattern(builder, code, JS_COMMENTS, theme.commentColor, isItalic = true)

        return builder.toAnnotatedString()
    }

    private fun highlightCss(code: String, theme: AppEditorTheme): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        builder.addStyle(SpanStyle(color = theme.textPrimary, letterSpacing = 0.4.sp), 0, code.length)

        applyPattern(builder, code, CSS_SELECTORS, theme.functionColor, isBold = true)

        val propMatcher = CSS_PROPERTIES.matcher(code)
        while (propMatcher.find()) {
            builder.addStyle(SpanStyle(color = theme.attributeColor), propMatcher.start(1), propMatcher.end(1))
        }

        applyPattern(builder, code, CSS_NUMBERS, theme.numberColor)
        applyPattern(builder, code, CSS_COLORS, theme.stringColor)
        applyPattern(builder, code, CSS_STRINGS, theme.stringColor)
        applyPattern(builder, code, CSS_COMMENTS, theme.commentColor, isItalic = true)

        return builder.toAnnotatedString()
    }

    private fun highlightPython(code: String, theme: AppEditorTheme): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        builder.addStyle(SpanStyle(color = theme.textPrimary, letterSpacing = 0.4.sp), 0, code.length)

        applyPattern(builder, code, PY_CONTROL, theme.controlKeywordColor, isBold = true)
        applyPattern(builder, code, PY_TYPES, theme.keywordColor)

        val funcMatcher = PY_FUNCTIONS.matcher(code)
        while (funcMatcher.find()) {
            val name = funcMatcher.group(1)
            if (name !in listOf("if", "while", "for", "elif")) {
                builder.addStyle(SpanStyle(color = theme.functionColor), funcMatcher.start(1), funcMatcher.end(1))
            }
        }

        applyPattern(builder, code, PY_STRINGS, theme.stringColor)
        applyPattern(builder, code, PY_NUMBERS, theme.numberColor)
        applyPattern(builder, code, PY_COMMENTS, theme.commentColor, isItalic = true)

        return builder.toAnnotatedString()
    }

    private fun applyPattern(
        builder: AnnotatedString.Builder,
        text: String,
        pattern: Pattern,
        color: androidx.compose.ui.graphics.Color,
        isBold: Boolean = false,
        isItalic: Boolean = false
    ) {
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            builder.addStyle(
                SpanStyle(
                    color = color,
                    fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal
                ),
                matcher.start(),
                matcher.end()
            )
        }
    }
}
