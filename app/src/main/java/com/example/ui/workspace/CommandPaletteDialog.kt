package com.example.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.model.CommandItem
import com.example.model.FileNode
import com.example.model.ProjectFile
import com.example.ui.theme.AppEditorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandPaletteDialog(
    theme: AppEditorTheme,
    commands: List<CommandItem>,
    files: List<ProjectFile>,
    onDismiss: () -> Unit,
    onFileSelected: (ProjectFile) -> Unit,
    initialQuery: String = ">",
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf(initialQuery) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val isFileMode = !query.startsWith(">")
    val cleanQuery = if (query.startsWith(">")) query.removePrefix(">").trim() else query.trim()

    val filteredCommands = remember(cleanQuery, commands, isFileMode) {
        if (isFileMode) emptyList()
        else if (cleanQuery.isBlank()) commands
        else commands.filter {
            it.title.contains(cleanQuery, ignoreCase = true) ||
            it.category.contains(cleanQuery, ignoreCase = true)
        }
    }

    val filteredFiles = remember(cleanQuery, files, isFileMode) {
        if (!isFileMode) emptyList()
        else if (cleanQuery.isBlank()) files
        else files.filter { it.name.contains(cleanQuery, ignoreCase = true) || it.relativePath.contains(cleanQuery, ignoreCase = true) }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f),
            color = theme.sidebarBg,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, theme.accentColor)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Command / File Search Input Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.titleBarBg)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isFileMode) Icons.Default.Description else Icons.Default.KeyboardCommandKey,
                        contentDescription = null,
                        tint = theme.accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                text = if (isFileMode) "Search files by name..." else "Type a command or '>' to filter...",
                                color = theme.textSecondary,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = theme.accentColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .testTag("command_palette_input")
                    )

                    // Mode switch badge
                    Surface(
                        modifier = Modifier.clickable {
                            query = if (query.startsWith(">")) "" else ">"
                        },
                        color = Color(0x22FFFFFF),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.border)
                    ) {
                        Text(
                            text = if (isFileMode) "Files" else "Commands",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Results List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp)
                ) {
                    if (isFileMode) {
                        if (filteredFiles.isEmpty()) {
                            item {
                                EmptyResultsMessage(message = "No matching files found in project")
                            }
                        } else {
                            items(filteredFiles, key = { it.absolutePath }) { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onFileSelected(file)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        tint = theme.accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = file.relativePath,
                                            color = theme.textSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        if (filteredCommands.isEmpty()) {
                            item {
                                EmptyResultsMessage(message = "No matching commands found")
                            }
                        } else {
                            items(filteredCommands, key = { it.id }) { cmd ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            cmd.action()
                                            onDismiss()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cmd.title,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = cmd.category,
                                            color = theme.textSecondary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    cmd.shortcut?.let { sc ->
                                        Text(
                                            text = sc,
                                            color = theme.textSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier
                                                .background(Color(0x22FFFFFF), RoundedCornerShape(3.dp))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Keyboard Tip / Footer
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = theme.titleBarBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Type '>' for commands or search filename directly", fontSize = 11.sp, color = theme.textSecondary)
                        Text("ESC to close", fontSize = 11.sp, color = theme.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyResultsMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, fontSize = 12.sp, color = Color(0xFF777777))
    }
}
