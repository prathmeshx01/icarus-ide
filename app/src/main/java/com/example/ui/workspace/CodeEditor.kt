package com.example.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.editor.CodeDiagnostic
import com.example.editor.CodeFoldingDetector
import com.example.editor.FoldingBlock
import com.example.editor.SyntaxHighlighter
import com.example.ui.theme.AppEditorTheme

@Composable
fun CodeEditor(
    editorValue: TextFieldValue,
    extension: String,
    theme: AppEditorTheme,
    fontSizeSp: Float,
    showLineNumbers: Boolean,
    wordWrap: Boolean,
    autoCloseBrackets: Boolean,
    diagnostics: List<CodeDiagnostic>,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    isSearchOpen: Boolean,
    searchQuery: String,
    replaceQuery: String,
    searchMatchCount: Int,
    onValueChange: (TextFieldValue) -> Unit,
    onInsertSymbol: (String, Int) -> Unit,
    onIndentLine: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onReplaceQueryChange: (String) -> Unit,
    onFindNext: (Boolean) -> Unit,
    onReplaceAll: (Boolean) -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lines = remember(editorValue.text) {
        editorValue.text.split("\n")
    }
    val lineCount = maxOf(1, lines.size)

    val errorLines = remember(diagnostics) {
        diagnostics.map { it.line }.toSet()
    }

    // Detect collapsible blocks
    val foldingBlocks = remember(editorValue.text, extension) {
        CodeFoldingDetector.detectFoldingBlocks(editorValue.text, extension)
    }
    val startLineToBlock = remember(foldingBlocks) {
        foldingBlocks.associateBy { it.startLine }
    }

    var foldedStartLines by remember { mutableStateOf(setOf<Int>()) }

    val hiddenLines = remember(foldedStartLines, foldingBlocks) {
        val hidden = mutableSetOf<Int>()
        for (block in foldingBlocks) {
            if (block.startLine in foldedStartLines) {
                for (l in (block.startLine + 1)..block.endLine) {
                    hidden.add(l)
                }
            }
        }
        hidden
    }

    // Determine current line
    val currentLine = remember(editorValue.selection, editorValue.text) {
        val sel = editorValue.selection.min.coerceIn(0, editorValue.text.length)
        editorValue.text.substring(0, sel).count { it == '\n' } + 1
    }

    // Visual Transformation with Syntax Highlighting
    val visualTransformation = remember(extension, theme, foldedStartLines, foldingBlocks, editorValue.text) {
        VisualTransformation { original ->
            if (foldedStartLines.isEmpty()) {
                val highlighted = SyntaxHighlighter.highlight(original.text, extension, theme)
                TransformedText(highlighted, OffsetMapping.Identity)
            } else {
                createFoldedTransformedText(original.text, extension, theme, foldingBlocks, foldedStartLines)
            }
        }
    }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val calculatedLineHeight = (fontSizeSp * 1.58f).sp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        // Find & Replace Overlay Drawer
        if (isSearchOpen) {
            SearchReplaceBar(
                searchQuery = searchQuery,
                replaceQuery = replaceQuery,
                matchCount = searchMatchCount,
                theme = theme,
                onSearchQueryChange = onSearchQueryChange,
                onReplaceQueryChange = onReplaceQueryChange,
                onFindNext = onFindNext,
                onReplaceAll = onReplaceAll,
                onClose = onCloseSearch
            )
        }

        // Main Code Canvas Area with Line Numbers Gutter
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (showLineNumbers) {
                val gutterWidth = remember(lineCount) {
                    if (lineCount > 999) 58.dp else if (lineCount > 99) 48.dp else 40.dp
                }

                Box(
                    modifier = Modifier
                        .width(gutterWidth)
                        .fillMaxHeight()
                        .background(theme.gutterBg)
                        .border(0.5.dp, theme.border)
                        .verticalScroll(verticalScrollState)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        for (i in 1..lineCount) {
                            if (i in hiddenLines) continue

                            val hasError = i in errorLines
                            val isCurrent = i == currentLine
                            val foldingBlock = startLineToBlock[i]
                            val isFolded = i in foldedStartLines

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(calculatedLineHeight.value.dp)
                                    .background(if (isCurrent) Color(0xFF1E2025) else Color.Transparent)
                            ) {
                                // Git modification indicator
                                Box(
                                    modifier = Modifier
                                        .width(2.5.dp)
                                        .fillMaxHeight()
                                        .background(if (i % 3 == 0) Color(0xFF3FB950) else Color.Transparent)
                                )

                                Spacer(modifier = Modifier.width(2.dp))

                                // Code folding icon
                                if (foldingBlock != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                foldedStartLines = if (isFolded) {
                                                    foldedStartLines - i
                                                } else {
                                                    foldedStartLines + i
                                                }
                                            }
                                            .testTag("fold_toggle_line_$i"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isFolded) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isFolded) "Expand line $i" else "Collapse line $i",
                                            tint = if (isFolded) theme.accentColor else theme.textSecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(14.dp))
                                }

                                if (hasError) {
                                    Box(
                                        modifier = Modifier
                                            .size(3.dp)
                                            .background(Color(0xFFF85149), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                }

                                Text(
                                    text = i.toString(),
                                    color = when {
                                        hasError -> Color(0xFFF85149)
                                        isCurrent -> Color.White
                                        else -> theme.textSecondary.copy(alpha = 0.5f)
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = fontSizeSp.sp,
                                    lineHeight = calculatedLineHeight,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Editable Monospaced Code Text Canvas
            val codeBoxModifier = if (wordWrap) {
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(verticalScrollState)
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            } else {
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState)
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            }

            Box(modifier = codeBoxModifier) {
                BasicTextField(
                    value = editorValue,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSizeSp.sp,
                        letterSpacing = 0.4.sp,
                        lineHeight = calculatedLineHeight,
                        color = theme.textPrimary
                    ),
                    visualTransformation = visualTransformation,
                    cursorBrush = SolidColor(theme.accentColor),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Default,
                        autoCorrectEnabled = false
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("code_editor_field")
                )
            }
        }

        // Clean, Compact Customizable Symbol Toolbar (Flush at bottom)
        SymbolToolbar(
            theme = theme,
            canUndo = canUndo,
            canRedo = canRedo,
            onUndo = onUndo,
            onRedo = onRedo,
            onInsertSymbol = onInsertSymbol,
            onIndent = onIndentLine
        )
    }
}

/**
 * Visual transformation collapsing folded code regions.
 */
private fun createFoldedTransformedText(
    fullText: String,
    extension: String,
    theme: AppEditorTheme,
    blocks: List<FoldingBlock>,
    foldedStarts: Set<Int>
): TransformedText {
    val lines = fullText.split("\n")
    val lineOffsets = mutableListOf<Int>()
    var currentOffset = 0
    for (line in lines) {
        lineOffsets.add(currentOffset)
        currentOffset += line.length + 1
    }

    val activeBlocks = blocks.filter { it.startLine in foldedStarts }.sortedBy { it.startLine }

    val transformedBuilder = StringBuilder()
    val originalToTransformed = mutableListOf<Int>()
    val transformedToOriginal = mutableListOf<Int>()

    var origIdx = 0

    for (block in activeBlocks) {
        val startLineIdx = block.startLine - 1
        val endLineIdx = block.endLine - 1

        val foldStartOffset = if (startLineIdx in lineOffsets.indices) {
            val lineStart = lineOffsets[startLineIdx]
            val lineContent = lines[startLineIdx]
            lineStart + lineContent.length
        } else origIdx

        val foldEndOffset = if (endLineIdx in lineOffsets.indices) {
            val endLineStart = lineOffsets[endLineIdx]
            val endLineContent = lines[endLineIdx]
            minOf(fullText.length, endLineStart + endLineContent.length)
        } else origIdx

        if (foldStartOffset > origIdx) {
            val chunk = fullText.substring(origIdx, foldStartOffset)
            val tStart = transformedBuilder.length
            transformedBuilder.append(chunk)
            for (k in chunk.indices) {
                originalToTransformed.add(tStart + k)
                transformedToOriginal.add(origIdx + k)
            }
            origIdx = foldStartOffset
        }

        if (foldEndOffset > origIdx) {
            val placeholder = " ${block.previewLabel}"
            val pStart = transformedBuilder.length
            transformedBuilder.append(placeholder)

            for (k in origIdx until foldEndOffset) {
                originalToTransformed.add(pStart)
            }
            for (k in placeholder.indices) {
                transformedToOriginal.add(origIdx)
            }
            origIdx = foldEndOffset
        }
    }

    if (origIdx < fullText.length) {
        val chunk = fullText.substring(origIdx)
        val tStart = transformedBuilder.length
        transformedBuilder.append(chunk)
        for (k in chunk.indices) {
            originalToTransformed.add(tStart + k)
            transformedToOriginal.add(origIdx + k)
        }
    }

    val transformedString = transformedBuilder.toString()
    val highlighted = SyntaxHighlighter.highlight(transformedString, extension, theme)

    val offsetMapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            if (originalToTransformed.isEmpty()) return 0
            val safe = offset.coerceIn(0, originalToTransformed.size - 1)
            return originalToTransformed[safe]
        }

        override fun transformedToOriginal(offset: Int): Int {
            if (transformedToOriginal.isEmpty()) return 0
            val safe = offset.coerceIn(0, transformedToOriginal.size - 1)
            return transformedToOriginal[safe]
        }
    }

    return TransformedText(highlighted, offsetMapping)
}

@Composable
fun SearchReplaceBar(
    searchQuery: String,
    replaceQuery: String,
    matchCount: Int,
    theme: AppEditorTheme,
    onSearchQueryChange: (String) -> Unit,
    onReplaceQueryChange: (String) -> Unit,
    onFindNext: (Boolean) -> Unit,
    onReplaceAll: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRegex by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = theme.sidebarBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.border)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Find...", fontSize = 12.sp, color = theme.textSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = theme.border,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    modifier = Modifier.clickable { isRegex = !isRegex },
                    color = if (isRegex) theme.accentColor else Color(0x1AFFFFFF),
                    shape = RoundedCornerShape(3.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.border)
                ) {
                    Text(
                        text = ".*",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRegex) Color.White else theme.textSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "$matchCount",
                    fontSize = 11.sp,
                    color = theme.textSecondary
                )

                IconButton(onClick = { onFindNext(isRegex) }, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Find Next",
                        tint = theme.accentColor,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close Find",
                        tint = theme.textSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = onReplaceQueryChange,
                    placeholder = { Text("Replace...", fontSize = 12.sp, color = theme.textSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = theme.border,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = { onReplaceAll(isRegex) },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Replace All", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}
