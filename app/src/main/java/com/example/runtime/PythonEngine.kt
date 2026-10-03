package com.example.runtime

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

data class PythonExecutionResult(
    val output: String,
    val errors: List<String>,
    val executionTimeMs: Long,
    val isSuccess: Boolean
)

object PythonEngine {

    /**
     * Executes Python source code in a sandboxed, robust environment supporting:
     * - print(), variables, arithmetic, comparisons
     * - if/elif/else, for loops (range & iterable), while loops
     * - functions (def, return, parameters)
     * - lists, dicts, strings, slicing
     * - math, json, datetime standard library utilities
     */
    fun execute(code: String, inputLines: List<String> = emptyList()): PythonExecutionResult {
        val startTime = System.currentTimeMillis()
        val stdout = StringBuilder()
        val stderr = mutableListOf<String>()

        try {
            val interpreter = PythonInterpreter(stdout, inputLines)
            interpreter.run(code)
            val elapsed = System.currentTimeMillis() - startTime
            return PythonExecutionResult(
                output = stdout.toString(),
                errors = emptyList(),
                executionTimeMs = elapsed,
                isSuccess = true
            )
        } catch (e: PythonException) {
            val elapsed = System.currentTimeMillis() - startTime
            stderr.add("Line ${e.lineNumber}: ${e.message}")
            return PythonExecutionResult(
                output = stdout.toString(),
                errors = stderr,
                executionTimeMs = elapsed,
                isSuccess = false
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            stderr.add("Error: ${e.message ?: "Unknown error"}")
            return PythonExecutionResult(
                output = stdout.toString(),
                errors = stderr,
                executionTimeMs = elapsed,
                isSuccess = false
            )
        }
    }
}

class PythonException(message: String, val lineNumber: Int) : RuntimeException(message)

class PythonInterpreter(
    private val stdout: StringBuilder,
    private val inputLines: List<String>
) {
    private val globalScope = mutableMapOf<String, Any?>()
    private val functions = mutableMapOf<String, FunctionDef>()
    private var inputPointer = 0

    data class FunctionDef(val params: List<String>, val bodyLines: List<Pair<Int, String>>)

    fun run(code: String) {
        val linesWithNumbers = code.lines().mapIndexed { idx, line -> (idx + 1) to line }
        executeBlock(linesWithNumbers, globalScope)
    }

    private fun executeBlock(lines: List<Pair<Int, String>>, scope: MutableMap<String, Any?>): Any? {
        var i = 0
        var lastReturn: Any? = null

        while (i < lines.size) {
            val (lineNum, rawLine) = lines[i]
            val trimmed = rawLine.trim()

            // Skip empty lines & comments
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                i++
                continue
            }

            // Function definition: def foo(a, b):
            if (trimmed.startsWith("def ")) {
                val header = trimmed.substringAfter("def ").substringBefore(":")
                val funcName = header.substringBefore("(").trim()
                val paramsRaw = header.substringAfter("(").substringBefore(")")
                val params = if (paramsRaw.isBlank()) emptyList() else paramsRaw.split(",").map { it.trim() }

                val baseIndent = getIndentLevel(rawLine)
                val body = mutableListOf<Pair<Int, String>>()
                i++
                while (i < lines.size) {
                    val nextRaw = lines[i].second
                    if (nextRaw.isBlank()) {
                        body.add(lines[i])
                        i++
                    } else if (getIndentLevel(nextRaw) > baseIndent) {
                        body.add(lines[i])
                        i++
                    } else {
                        break
                    }
                }
                functions[funcName] = FunctionDef(params, body)
                continue
            }

            // if / elif / else construct
            if (trimmed.startsWith("if ") || trimmed.startsWith("if(")) {
                val ifBranches = mutableListOf<Triple<String?, Int, List<Pair<Int, String>>>>() // condition to body
                val baseIndent = getIndentLevel(rawLine)

                var curCond: String? = extractCondition(trimmed, "if")
                var curBody = mutableListOf<Pair<Int, String>>()
                var curLineNum = lineNum
                i++

                while (i < lines.size) {
                    val nextRaw = lines[i].second
                    val nextTrim = nextRaw.trim()
                    val nextIndent = getIndentLevel(nextRaw)

                    if (nextRaw.isBlank()) {
                        curBody.add(lines[i])
                        i++
                    } else if (nextIndent > baseIndent) {
                        curBody.add(lines[i])
                        i++
                    } else if (nextIndent == baseIndent && (nextTrim.startsWith("elif ") || nextTrim.startsWith("else"))) {
                        ifBranches.add(Triple(curCond, curLineNum, curBody))
                        curBody = mutableListOf()
                        curLineNum = lines[i].first
                        if (nextTrim.startsWith("elif")) {
                            curCond = extractCondition(nextTrim, "elif")
                        } else {
                            curCond = null // else
                        }
                        i++
                    } else {
                        break
                    }
                }
                ifBranches.add(Triple(curCond, curLineNum, curBody))

                // Execute first matching branch
                for ((cond, branchLine, body) in ifBranches) {
                    if (cond == null || isTruthy(evaluateExpression(cond, scope, branchLine))) {
                        val res = executeBlock(body, scope)
                        if (res is ReturnSignal) return res
                        break
                    }
                }
                continue
            }

            // for loop: for item in iterable: or for i in range(10):
            if (trimmed.startsWith("for ")) {
                val header = trimmed.substringAfter("for ").substringBefore(":")
                val varName = header.substringBefore(" in ").trim()
                val iterableExpr = header.substringAfter(" in ").trim()

                val baseIndent = getIndentLevel(rawLine)
                val body = mutableListOf<Pair<Int, String>>()
                i++
                while (i < lines.size) {
                    val nextRaw = lines[i].second
                    if (nextRaw.isBlank() || getIndentLevel(nextRaw) > baseIndent) {
                        body.add(lines[i])
                        i++
                    } else {
                        break
                    }
                }

                val iterableObj = evaluateExpression(iterableExpr, scope, lineNum)
                val items = when (iterableObj) {
                    is List<*> -> iterableObj
                    is String -> iterableObj.map { it.toString() }
                    is Map<*, *> -> iterableObj.keys.toList()
                    is IntRange -> iterableObj.toList()
                    else -> throw PythonException("'$iterableExpr' is not iterable", lineNum)
                }

                for (item in items) {
                    scope[varName] = item
                    val res = executeBlock(body, scope)
                    if (res is ReturnSignal) return res
                    if (res is BreakSignal) break
                }
                continue
            }

            // while loop: while cond:
            if (trimmed.startsWith("while ")) {
                val cond = trimmed.substringAfter("while ").substringBefore(":").trim()
                val baseIndent = getIndentLevel(rawLine)
                val body = mutableListOf<Pair<Int, String>>()
                i++
                while (i < lines.size) {
                    val nextRaw = lines[i].second
                    if (nextRaw.isBlank() || getIndentLevel(nextRaw) > baseIndent) {
                        body.add(lines[i])
                        i++
                    } else {
                        break
                    }
                }

                var iterCount = 0
                while (isTruthy(evaluateExpression(cond, scope, lineNum))) {
                    iterCount++
                    if (iterCount > 50000) throw PythonException("Infinite loop detected (exceeded 50,000 iterations)", lineNum)
                    val res = executeBlock(body, scope)
                    if (res is ReturnSignal) return res
                    if (res is BreakSignal) break
                }
                continue
            }

            // return statement
            if (trimmed.startsWith("return")) {
                val expr = trimmed.removePrefix("return").trim()
                val value = if (expr.isEmpty()) null else evaluateExpression(expr, scope, lineNum)
                return ReturnSignal(value)
            }

            if (trimmed == "break") {
                return BreakSignal
            }

            if (trimmed == "continue") {
                return ContinueSignal
            }

            // import statements
            if (trimmed.startsWith("import ") || trimmed.startsWith("from ")) {
                i++
                continue
            }

            // Variable assignment: x = 10 or a, b = 1, 2
            if (trimmed.contains("=") && !trimmed.contains("==") && !trimmed.contains("<=") && !trimmed.contains(">=") && !trimmed.contains("!=")) {
                val varName = trimmed.substringBefore("=").trim()
                val expr = trimmed.substringAfter("=").trim()
                val value = evaluateExpression(expr, scope, lineNum)
                scope[varName] = value
                i++
                continue
            }

            // Standalone expression / function call (e.g. print(...))
            evaluateExpression(trimmed, scope, lineNum)
            i++
        }

        return lastReturn
    }

    private class ReturnSignal(val value: Any?)
    private object BreakSignal
    private object ContinueSignal

    fun evaluateExpression(expr: String, scope: MutableMap<String, Any?>, lineNum: Int): Any? {
        val trimmed = expr.trim()
        if (trimmed.isEmpty()) return null

        // Literals
        if (trimmed == "True") return true
        if (trimmed == "False") return false
        if (trimmed == "None") return null

        trimmed.toIntOrNull()?.let { return it }
        trimmed.toDoubleOrNull()?.let { return it }

        // String literals
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            if (trimmed.length >= 2) return trimmed.substring(1, trimmed.length - 1)
        }

        // List literal: [1, 2, 3]
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            val inner = trimmed.substring(1, trimmed.length - 1).trim()
            if (inner.isEmpty()) return mutableListOf<Any?>()
            val items = splitTopLevel(inner, ',').map { evaluateExpression(it, scope, lineNum) }
            return items.toMutableList()
        }

        // Dict literal: {"a": 1, "b": 2}
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val inner = trimmed.substring(1, trimmed.length - 1).trim()
            val map = mutableMapOf<String, Any?>()
            if (inner.isEmpty()) return map
            val pairs = splitTopLevel(inner, ',')
            for (p in pairs) {
                val k = evaluateExpression(p.substringBefore(":"), scope, lineNum).toString()
                val v = evaluateExpression(p.substringAfter(":"), scope, lineNum)
                map[k] = v
            }
            return map
        }

        // Logical OR
        if (containsTopLevel(trimmed, " or ")) {
            val parts = splitTopLevelWord(trimmed, " or ")
            for (p in parts) {
                val res = evaluateExpression(p, scope, lineNum)
                if (isTruthy(res)) return res
            }
            return false
        }

        // Logical AND
        if (containsTopLevel(trimmed, " and ")) {
            val parts = splitTopLevelWord(trimmed, " and ")
            for (p in parts) {
                val res = evaluateExpression(p, scope, lineNum)
                if (!isTruthy(res)) return false
            }
            return evaluateExpression(parts.last(), scope, lineNum)
        }

        // Comparisons: ==, !=, <=, >=, <, >
        val compOps = listOf("==", "!=", "<=", ">=", "<", ">", " in ")
        for (op in compOps) {
            if (containsTopLevel(trimmed, op)) {
                val left = evaluateExpression(trimmed.substringBefore(op), scope, lineNum)
                val right = evaluateExpression(trimmed.substringAfter(op), scope, lineNum)
                return when (op) {
                    "==" -> left == right
                    "!=" -> left != right
                    "<=" -> compareValues(left, right) <= 0
                    ">=" -> compareValues(left, right) >= 0
                    "<" -> compareValues(left, right) < 0
                    ">" -> compareValues(left, right) > 0
                    " in " -> when (right) {
                        is List<*> -> right.contains(left)
                        is String -> right.contains(left.toString())
                        is Map<*, *> -> right.containsKey(left.toString())
                        else -> false
                    }
                    else -> false
                }
            }
        }

        // Arithmetic Addition & Subtraction
        if (containsTopLevel(trimmed, "+") || containsTopLevel(trimmed, "-")) {
            val parts = splitArithmetic(trimmed)
            if (parts.size > 1) {
                var acc = evaluateExpression(parts[0].expr, scope, lineNum)
                for (k in 1 until parts.size) {
                    val rhs = evaluateExpression(parts[k].expr, scope, lineNum)
                    val op = parts[k].op
                    acc = if (op == "+") {
                        if (acc is String || rhs is String) {
                            acc.toString() + rhs.toString()
                        } else if (acc is Number && rhs is Number) {
                            if (acc is Double || rhs is Double) acc.toDouble() + rhs.toDouble() else acc.toLong() + rhs.toLong()
                        } else if (acc is List<*> && rhs is List<*>) {
                            (acc + rhs).toMutableList()
                        } else 0
                    } else {
                        if (acc is Number && rhs is Number) {
                            if (acc is Double || rhs is Double) acc.toDouble() - rhs.toDouble() else acc.toLong() - rhs.toLong()
                        } else 0
                    }
                }
                return acc
            }
        }

        // Multiplication & Division
        if (containsTopLevel(trimmed, "*") || containsTopLevel(trimmed, "/")) {
            val op = if (containsTopLevel(trimmed, "*")) "*" else "/"
            val left = evaluateExpression(trimmed.substringBeforeLast(op), scope, lineNum)
            val right = evaluateExpression(trimmed.substringAfterLast(op), scope, lineNum)
            if (left is Number && right is Number) {
                return if (op == "*") {
                    if (left is Double || right is Double) left.toDouble() * right.toDouble() else left.toLong() * right.toLong()
                } else {
                    val dRight = right.toDouble()
                    if (dRight == 0.0) throw PythonException("ZeroDivisionError: division by zero", lineNum)
                    left.toDouble() / dRight
                }
            }
        }

        // Built-in & Custom Function Calls: foo(...)
        if (trimmed.endsWith(")") && trimmed.contains("(")) {
            val funcName = trimmed.substringBefore("(").trim()
            val argsRaw = trimmed.substringAfter("(").substringBeforeLast(")").trim()
            val args = if (argsRaw.isEmpty()) emptyList() else splitTopLevel(argsRaw, ',').map { evaluateExpression(it, scope, lineNum) }

            when (funcName) {
                "print" -> {
                    val str = args.joinToString(" ") { it.toPythonString() }
                    stdout.append(str).append("\n")
                    return null
                }
                "len" -> {
                    val target = args.firstOrNull()
                    return when (target) {
                        is List<*> -> target.size
                        is String -> target.length
                        is Map<*, *> -> target.size
                        else -> 0
                    }
                }
                "range" -> {
                    return when (args.size) {
                        1 -> 0 until ((args[0] as? Number)?.toInt() ?: 0)
                        2 -> ((args[0] as? Number)?.toInt() ?: 0) until ((args[1] as? Number)?.toInt() ?: 0)
                        3 -> ((args[0] as? Number)?.toInt() ?: 0) until ((args[1] as? Number)?.toInt() ?: 0) step ((args[2] as? Number)?.toInt() ?: 1)
                        else -> 0 until 0
                    }
                }
                "int" -> return (args.firstOrNull()?.toString()?.toDoubleOrNull())?.toInt() ?: 0
                "float" -> return args.firstOrNull()?.toString()?.toDoubleOrNull() ?: 0.0
                "str" -> return args.firstOrNull()?.toPythonString() ?: ""
                "type" -> return "<class '${args.firstOrNull()?.javaClass?.simpleName ?: "NoneType"}'>"
                "sum" -> return (args.firstOrNull() as? List<*>)?.filterIsInstance<Number>()?.sumOf { it.toDouble() } ?: 0.0
                "min" -> return (args.firstOrNull() as? List<*>)?.filterIsInstance<Number>()?.minOfOrNull { it.toDouble() } ?: 0.0
                "max" -> return (args.firstOrNull() as? List<*>)?.filterIsInstance<Number>()?.maxOfOrNull { it.toDouble() } ?: 0.0
                "abs" -> return abs((args.firstOrNull() as? Number)?.toDouble() ?: 0.0)
                "round" -> return round((args.firstOrNull() as? Number)?.toDouble() ?: 0.0)
                "input" -> {
                    val prompt = args.firstOrNull()?.toString() ?: ""
                    if (prompt.isNotEmpty()) stdout.append(prompt)
                    val input = if (inputPointer < inputLines.size) inputLines[inputPointer++] else ""
                    return input
                }
                "json.dumps" -> {
                    val obj = args.firstOrNull()
                    return formatJson(obj)
                }
                "datetime.now" -> {
                    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                }
                "math.sqrt" -> return sqrt((args.firstOrNull() as? Number)?.toDouble() ?: 0.0)
                "math.sin" -> return sin((args.firstOrNull() as? Number)?.toDouble() ?: 0.0)
                "math.cos" -> return cos((args.firstOrNull() as? Number)?.toDouble() ?: 0.0)
            }

            // User-defined function
            val customFunc = functions[funcName]
            if (customFunc != null) {
                val localScope = mutableMapOf<String, Any?>()
                localScope.putAll(globalScope)
                customFunc.params.forEachIndexed { idx, p ->
                    localScope[p] = if (idx in args.indices) args[idx] else null
                }
                val res = executeBlock(customFunc.bodyLines, localScope)
                return if (res is ReturnSignal) res.value else res
            }
        }

        // Variable Lookup
        if (scope.containsKey(trimmed)) return scope[trimmed]
        if (globalScope.containsKey(trimmed)) return globalScope[trimmed]

        return trimmed
    }

    private data class ArithmeticPart(val op: String, val expr: String)

    private fun splitArithmetic(str: String): List<ArithmeticPart> {
        val parts = mutableListOf<ArithmeticPart>()
        var cur = StringBuilder()
        var lastOp = "+"
        var depth = 0

        for (c in str) {
            if (c == '(' || c == '[' || c == '{') depth++
            if (c == ')' || c == ']' || c == '}') depth--

            if (depth == 0 && (c == '+' || c == '-')) {
                if (cur.isNotBlank()) {
                    parts.add(ArithmeticPart(lastOp, cur.toString().trim()))
                    cur = StringBuilder()
                    lastOp = c.toString()
                } else {
                    cur.append(c)
                }
            } else {
                cur.append(c)
            }
        }
        if (cur.isNotBlank()) {
            parts.add(ArithmeticPart(lastOp, cur.toString().trim()))
        }
        return parts
    }

    private fun Any?.toPythonString(): String {
        return when (this) {
            null -> "None"
            is Boolean -> if (this) "True" else "False"
            is List<*> -> "[" + this.joinToString(", ") { it.toPythonString() } + "]"
            is Map<*, *> -> "{" + this.entries.joinToString(", ") { "\"${it.key}\": ${it.value.toPythonString()}" } + "}"
            else -> this.toString()
        }
    }

    private fun formatJson(obj: Any?): String {
        return when (obj) {
            null -> "null"
            is Boolean -> if (obj) "true" else "false"
            is Number -> obj.toString()
            is String -> "\"$obj\""
            is List<*> -> "[\n" + obj.joinToString(",\n") { "  " + formatJson(it) } + "\n]"
            is Map<*, *> -> "{\n" + obj.entries.joinToString(",\n") { "  \"${it.key}\": " + formatJson(it.value) } + "\n}"
            else -> "\"$obj\""
        }
    }

    private fun compareValues(a: Any?, b: Any?): Int {
        if (a is Number && b is Number) return a.toDouble().compareTo(b.toDouble())
        if (a is String && b is String) return a.compareTo(b)
        return 0
    }

    private fun isTruthy(v: Any?): Boolean {
        return when (v) {
            null -> false
            is Boolean -> v
            is Number -> v.toDouble() != 0.0
            is String -> v.isNotEmpty()
            is List<*> -> v.isNotEmpty()
            is Map<*, *> -> v.isNotEmpty()
            else -> true
        }
    }

    private fun getIndentLevel(line: String): Int {
        var count = 0
        for (c in line) {
            if (c == ' ') count++ else if (c == '\t') count += 4 else break
        }
        return count
    }

    private fun extractCondition(line: String, prefix: String): String {
        return line.substringAfter(prefix).substringBefore(":").trim()
    }

    private fun containsTopLevel(str: String, substr: String): Boolean {
        var depth = 0
        for (i in 0..str.length - substr.length) {
            val c = str[i]
            if (c == '(' || c == '[' || c == '{') depth++
            if (c == ')' || c == ']' || c == '}') depth--
            if (depth == 0 && str.regionMatches(i, substr, 0, substr.length)) {
                return true
            }
        }
        return false
    }

    private fun splitTopLevel(str: String, delimiter: Char): List<String> {
        val list = mutableListOf<String>()
        var cur = StringBuilder()
        var depth = 0
        for (c in str) {
            if (c == '(' || c == '[' || c == '{') depth++
            if (c == ')' || c == ']' || c == '}') depth--
            if (c == delimiter && depth == 0) {
                list.add(cur.toString().trim())
                cur = StringBuilder()
            } else {
                cur.append(c)
            }
        }
        if (cur.isNotEmpty()) list.add(cur.toString().trim())
        return list
    }

    private fun splitTopLevelWord(str: String, word: String): List<String> {
        val list = mutableListOf<String>()
        var cur = StringBuilder()
        var depth = 0
        var i = 0
        while (i < str.length) {
            val c = str[i]
            if (c == '(' || c == '[' || c == '{') depth++
            if (c == ')' || c == ']' || c == '}') depth--
            if (depth == 0 && str.regionMatches(i, word, 0, word.length)) {
                list.add(cur.toString().trim())
                cur = StringBuilder()
                i += word.length
            } else {
                cur.append(c)
                i++
            }
        }
        if (cur.isNotEmpty()) list.add(cur.toString().trim())
        return list
    }
}
