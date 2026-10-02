package com.example.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val cursorOffset: Int = insertText.length // Offset from insertion start
)

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

    val symbolActions = listOf(
        SymbolAction("{ }", "{}", 1),
        SymbolAction("( )", "()", 1),
        SymbolAction("[ ]", "[]", 1),
        SymbolAction("< >", "<>", 1),
        SymbolAction("\"", "\"\"", 1),
        SymbolAction("'", "''", 1),
        SymbolAction(";", ";", 1),
        SymbolAction("=", " = ", 3),
        SymbolAction("=>", " => ", 4),
        SymbolAction(":=", " := ", 4),
        SymbolAction("/", "/", 1),
        SymbolAction("\\", "\\", 1),
        SymbolAction(":", ":", 1),
        SymbolAction("!", "!", 1),
        SymbolAction("?", "?", 1),
        SymbolAction("&", " && ", 4),
        SymbolAction("|", " || ", 4),
        SymbolAction("#", "#", 1),
        SymbolAction(".", ".", 1),
        SymbolAction(",", ", ", 2),
        SymbolAction("`", "``", 1)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        color = theme.sidebarBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .horizontalScroll(scrollState)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Undo Button
            Box(
                modifier = Modifier
                    .size(width = 32.dp, height = 28.dp)
                    .background(if (canUndo) Color(0x1AFFFFFF) else Color.Transparent, RoundedCornerShape(3.dp))
                    .border(1.dp, if (canUndo) theme.border else Color.Transparent, RoundedCornerShape(3.dp))
                    .clickable(enabled = canUndo, onClick = onUndo)
                    .testTag("symbol_btn_undo"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) Color.White else Color(0xFF555555),
                    modifier = Modifier.size(15.dp)
                )
            }

            // Redo Button
            Box(
                modifier = Modifier
                    .size(width = 32.dp, height = 28.dp)
                    .background(if (canRedo) Color(0x1AFFFFFF) else Color.Transparent, RoundedCornerShape(3.dp))
                    .border(1.dp, if (canRedo) theme.border else Color.Transparent, RoundedCornerShape(3.dp))
                    .clickable(enabled = canRedo, onClick = onRedo)
                    .testTag("symbol_btn_redo"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) Color.White else Color(0xFF555555),
                    modifier = Modifier.size(15.dp)
                )
            }

            // Tab (Inserts 2 spaces)
            SymbolChip(
                label = "Tab",
                isHighlighted = true,
                theme = theme,
                onClick = { onInsertSymbol("  ", 2) }
            )

            // Indent line
            SymbolChip(
                label = "Indent",
                isHighlighted = false,
                theme = theme,
                onClick = onIndent
            )

            // Programming Symbols
            symbolActions.forEach { action ->
                SymbolChip(
                    label = action.label,
                    isHighlighted = action.label in listOf("{ }", "( )", "[ ]", "< >"),
                    theme = theme,
                    onClick = { onInsertSymbol(action.insertText, action.cursorOffset) }
                )
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
            .background(Color(0x14FFFFFF), RoundedCornerShape(3.dp))
            .border(1.dp, theme.border, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlighted) theme.accentColor else Color(0xFFCCCCCC)
        )
    }
}
