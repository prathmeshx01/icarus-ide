package com.example.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppEditorTheme

data class SymbolAction(
    val label: String,
    val insertText: String,
    val cursorOffset: Int = insertText.length
)

enum class SymbolCategory(val displayName: String, val badge: String) {
    CORE("Core Symbols", "SYM"),
    HTML_WEB("HTML / Web Tags", "HTML"),
    OPERATORS("Operators & Logic", "OPS"),
    SNIPPETS("Quick Snippets", "SNIP")
}

@Composable
fun SymbolToolbar(
    theme: AppEditorTheme,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onInsertSymbol: (text: String, cursorOffset: Int) -> Unit,
    onIndent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var selectedCategory by remember { mutableStateOf(SymbolCategory.CORE) }
    var menuExpanded by remember { mutableStateOf(false) }

    val coreSymbols = remember {
        listOf(
            SymbolAction("{ }", "{}", 1),
            SymbolAction("( )", "()", 1),
            SymbolAction("[ ]", "[]", 1),
            SymbolAction("< >", "<>", 1),
            SymbolAction("\"", "\"\"", 1),
            SymbolAction("'", "''", 1),
            SymbolAction("`", "``", 1),
            SymbolAction(";", ";", 1),
            SymbolAction(":", ":", 1),
            SymbolAction("=", " = ", 3),
            SymbolAction("/", "/", 1),
            SymbolAction("\\", "\\", 1),
            SymbolAction(".", ".", 1),
            SymbolAction(",", ", ", 2),
            SymbolAction("!", "!", 1),
            SymbolAction("?", "?", 1),
            SymbolAction("$", "$", 1),
            SymbolAction("#", "#", 1),
            SymbolAction("@", "@", 1),
            SymbolAction("_", "_", 1),
            SymbolAction("-", "-", 1),
            SymbolAction("+", " + ", 3)
        )
    }

    val htmlSymbols = remember {
        listOf(
            SymbolAction("< >", "<>", 1),
            SymbolAction("</>", "</>", 2),
            SymbolAction("div", "<div></div>", 5),
            SymbolAction("span", "<span></span>", 6),
            SymbolAction("class", " class=\"\"", 8),
            SymbolAction("id", " id=\"\"", 5),
            SymbolAction("src", " src=\"\"", 6),
            SymbolAction("href", " href=\"\"", 7),
            SymbolAction("style", " style=\"\"", 8),
            SymbolAction("btn", "<button></button>", 8),
            SymbolAction("p", "<p></p>", 3),
            SymbolAction("h1", "<h1></h1>", 4),
            SymbolAction("img", "<img src=\"\" alt=\"\">", 10),
            SymbolAction("<!-- -->", "<!--  -->", 5)
        )
    }

    val operatorSymbols = remember {
        listOf(
            SymbolAction("===", " === ", 5),
            SymbolAction("!==", " !== ", 5),
            SymbolAction("==", " == ", 4),
            SymbolAction("!=", " != ", 4),
            SymbolAction("&&", " && ", 4),
            SymbolAction("||", " || ", 4),
            SymbolAction("=>", " => ", 4),
            SymbolAction("->", " -> ", 4),
            SymbolAction("+=", " += ", 4),
            SymbolAction("-=", " -= ", 4),
            SymbolAction("*=", " *= ", 4),
            SymbolAction("/=", " /= ", 4),
            SymbolAction("++", "++", 2),
            SymbolAction("--", "--", 2),
            SymbolAction("<=", " <= ", 4),
            SymbolAction(">=", " >= ", 4)
        )
    }

    val snippetSymbols = remember {
        listOf(
            SymbolAction("log()", "console.log();", 12),
            SymbolAction("fn()", "function () {\n  \n}", 10),
            SymbolAction("const", "const  = ;", 6),
            SymbolAction("let", "let  = ;", 4),
            SymbolAction("return", "return ;", 7),
            SymbolAction("if ()", "if () {\n  \n}", 4),
            SymbolAction("import", "import {  } from \"\";", 9),
            SymbolAction("export", "export const ", 13),
            SymbolAction("async", "async ", 6),
            SymbolAction("await", "await ", 6)
        )
    }

    val currentActions = when (selectedCategory) {
        SymbolCategory.CORE -> coreSymbols
        SymbolCategory.HTML_WEB -> htmlSymbols
        SymbolCategory.OPERATORS -> operatorSymbols
        SymbolCategory.SNIPPETS -> snippetSymbols
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        color = theme.sidebarBg,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, theme.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Switcher Dropdown Button (Changes what is in the bar!)
            Box {
                Surface(
                    modifier = Modifier
                        .clickable { menuExpanded = true }
                        .padding(end = 4.dp),
                    color = theme.accentColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCategory.badge,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.accentColor
                        )
                    }
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(theme.sidebarBg)
                ) {
                    SymbolCategory.values().forEach { cat ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = cat.badge,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedCategory == cat) theme.accentColor else theme.textSecondary,
                                        modifier = Modifier
                                            .background(
                                                if (selectedCategory == cat) theme.accentColor.copy(alpha = 0.2f) else Color(0x14FFFFFF),
                                                RoundedCornerShape(3.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = cat.displayName,
                                        color = if (selectedCategory == cat) Color.White else theme.textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            },
                            onClick = {
                                selectedCategory = cat
                                menuExpanded = false
                            }
                        )
                    }
                }
            }

            // Undo & Redo Quick Actions
            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 28.dp)
                    .background(if (canUndo) Color(0x1AFFFFFF) else Color.Transparent, RoundedCornerShape(3.dp))
                    .border(0.5.dp, if (canUndo) theme.border else Color.Transparent, RoundedCornerShape(3.dp))
                    .clickable(enabled = canUndo, onClick = onUndo)
                    .testTag("symbol_btn_undo"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) Color.White else Color(0xFF555555),
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(3.dp))

            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 28.dp)
                    .background(if (canRedo) Color(0x1AFFFFFF) else Color.Transparent, RoundedCornerShape(3.dp))
                    .border(0.5.dp, if (canRedo) theme.border else Color.Transparent, RoundedCornerShape(3.dp))
                    .clickable(enabled = canRedo, onClick = onRedo)
                    .testTag("symbol_btn_redo"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) Color.White else Color(0xFF555555),
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(3.dp))

            // Tab (2 spaces)
            SymbolChip(
                label = "Tab",
                isHighlighted = true,
                theme = theme,
                onClick = { onInsertSymbol("  ", 2) }
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Horizontally Scrollable Customizable Symbol Row
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                currentActions.forEach { action ->
                    SymbolChip(
                        label = action.label,
                        isHighlighted = action.label in listOf("{ }", "( )", "[ ]", "< >", "log()"),
                        theme = theme,
                        onClick = { onInsertSymbol(action.insertText, action.cursorOffset) }
                    )
                }
            }
        }
    }
}

@Composable
fun SymbolChip(
    label: String,
    isHighlighted: Boolean,
    theme: AppEditorTheme,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .background(if (isHighlighted) Color(0x22FFFFFF) else Color(0x12FFFFFF), RoundedCornerShape(3.dp))
            .border(0.5.dp, if (isHighlighted) theme.accentColor.copy(alpha = 0.5f) else theme.border, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.5.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlighted) theme.accentColor else Color(0xFFE0E0E0)
        )
    }
}
