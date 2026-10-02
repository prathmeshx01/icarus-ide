package com.example.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Css
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.model.FileNode
import com.example.model.Project
import com.example.model.ProjectFile
import com.example.ui.theme.AppEditorTheme

@Composable
fun ProjectExplorerDrawer(
    project: Project,
    fileTree: List<FileNode>,
    activeFilePath: String?,
    theme: AppEditorTheme,
    onFileSelected: (ProjectFile) -> Unit,
    onCreateFile: (parentDir: String, fileName: String) -> Unit,
    onCreateFolder: (parentDir: String, folderName: String) -> Unit,
    onRenameItem: (node: FileNode, newName: String) -> Unit,
    onDeleteItem: (node: FileNode) -> Unit,
    onRefresh: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    var targetFolderForNewItem by remember { mutableStateOf<FileNode?>(null) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var itemToRename by remember { mutableStateOf<FileNode?>(null) }
    var itemToDelete by remember { mutableStateOf<FileNode?>(null) }

    fun toggleFolder(node: FileNode) {
        expandedFolders = if (node.absolutePath in expandedFolders) {
            expandedFolders - node.absolutePath
        } else {
            expandedFolders + node.absolutePath
        }
    }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(290.dp),
        color = theme.sidebarBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.border)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // VS Code Style Explorer Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.sidebarBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXPLORER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = theme.textSecondary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // New File at project root
                    IconButton(
                        onClick = {
                            targetFolderForNewItem = null
                            showNewFileDialog = true
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New File",
                            tint = Color(0xFFCCCCCC),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // New Folder at project root
                    IconButton(
                        onClick = {
                            targetFolderForNewItem = null
                            showNewFolderDialog = true
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.CreateNewFolder,
                            contentDescription = "New Folder",
                            tint = Color(0xFFCCCCCC),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Collapse All Folders
                    IconButton(
                        onClick = { expandedFolders = emptySet() },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.UnfoldLess,
                            contentDescription = "Collapse All",
                            tint = Color(0xFFCCCCCC),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Refresh
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFFCCCCCC),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Project Root Folder Banner (expandable section header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x15FFFFFF))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = theme.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = project.name.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }

            // Recursive Tree LazyColumn
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp)
            ) {
                // Flatten the hierarchical tree based on expanded folders
                val visibleNodes = flattenTree(fileTree, expandedFolders)
                items(visibleNodes, key = { it.absolutePath }) { node ->
                    FileTreeNodeRow(
                        node = node,
                        isActive = node.absolutePath == activeFilePath,
                        isExpanded = node.absolutePath in expandedFolders,
                        theme = theme,
                        onNodeClick = {
                            if (node.isDirectory) {
                                toggleFolder(node)
                            } else {
                                onFileSelected(
                                    ProjectFile(
                                        name = node.name,
                                        relativePath = node.relativePath,
                                        extension = node.extension,
                                        isDirectory = false,
                                        absolutePath = node.absolutePath,
                                        sizeBytes = node.sizeBytes
                                    )
                                )
                            }
                        },
                        onNewFileInFolder = {
                            targetFolderForNewItem = node
                            showNewFileDialog = true
                        },
                        onNewFolderInFolder = {
                            targetFolderForNewItem = node
                            showNewFolderDialog = true
                        },
                        onRename = { itemToRename = node },
                        onDelete = { itemToDelete = node }
                    )
                }
            }

            // Bottom Status / Quick Info
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = theme.titleBarBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Workspace Files",
                        fontSize = 10.sp,
                        color = theme.textSecondary
                    )
                    Text(
                        text = "Local Storage",
                        fontSize = 10.sp,
                        color = theme.accentColor
                    )
                }
            }
        }
    }

    // Modal: Create File
    if (showNewFileDialog) {
        var newFileName by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        val targetPath = targetFolderForNewItem?.absolutePath ?: project.rootDirPath

        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            containerColor = theme.sidebarBg,
            title = {
                Text(
                    text = if (targetFolderForNewItem != null) "New File in /${targetFolderForNewItem!!.name}" else "New File in Root",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "File name with extension (e.g., app.js, style.css, Main.kt)",
                        fontSize = 12.sp,
                        color = theme.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = {
                            newFileName = it
                            errorMsg = null
                        },
                        singleLine = true,
                        placeholder = { Text("new_file.html", color = Color(0xFF666666)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.accentColor,
                            unfocusedBorderColor = theme.border,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        isError = errorMsg != null,
                        supportingText = errorMsg?.let { { Text(it, color = Color(0xFFF14C4C)) } },
                        modifier = Modifier.fillMaxWidth().testTag("new_file_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newFileName.trim()
                        if (clean.isBlank()) {
                            errorMsg = "File name cannot be empty"
                        } else {
                            onCreateFile(targetPath, clean)
                            showNewFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.testTag("confirm_create_file_btn")
                ) {
                    Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancel", color = theme.textSecondary)
                }
            }
        )
    }

    // Modal: Create Folder
    if (showNewFolderDialog) {
        var newFolderName by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        val targetPath = targetFolderForNewItem?.absolutePath ?: project.rootDirPath

        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            containerColor = theme.sidebarBg,
            title = {
                Text(
                    text = if (targetFolderForNewItem != null) "New Folder in /${targetFolderForNewItem!!.name}" else "New Folder in Root",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Folder name (e.g. css, components, utils)",
                        fontSize = 12.sp,
                        color = theme.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = {
                            newFolderName = it
                            errorMsg = null
                        },
                        singleLine = true,
                        placeholder = { Text("components", color = Color(0xFF666666)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.accentColor,
                            unfocusedBorderColor = theme.border,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        isError = errorMsg != null,
                        supportingText = errorMsg?.let { { Text(it, color = Color(0xFFF14C4C)) } },
                        modifier = Modifier.fillMaxWidth().testTag("new_folder_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newFolderName.trim()
                        if (clean.isBlank()) {
                            errorMsg = "Folder name cannot be empty"
                        } else {
                            onCreateFolder(targetPath, clean)
                            showNewFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.testTag("confirm_create_folder_btn")
                ) {
                    Text("Create Folder", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel", color = theme.textSecondary)
                }
            }
        )
    }

    // Modal: Rename
    itemToRename?.let { node ->
        var renameText by remember { mutableStateOf(node.name) }
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            containerColor = theme.sidebarBg,
            title = { Text("Rename ${if (node.isDirectory) "Folder" else "File"}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = theme.border,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank() && renameText != node.name) {
                            onRenameItem(node, renameText.trim())
                        }
                        itemToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Rename", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRename = null }) {
                    Text("Cancel", color = theme.textSecondary)
                }
            }
        )
    }

    // Modal: Delete
    itemToDelete?.let { node ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = theme.sidebarBg,
            title = { Text("Delete ${if (node.isDirectory) "Folder" else "File"}?", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = if (node.isDirectory) "Permanently delete folder '${node.name}' and all its contents?" else "Permanently delete '${node.name}'?",
                    color = Color(0xFFCCCCCC)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteItem(node)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF14C4C)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = theme.textSecondary)
                }
            }
        )
    }
}

private fun flattenTree(nodes: List<FileNode>, expandedFolders: Set<String>): List<FileNode> {
    val result = mutableListOf<FileNode>()
    for (node in nodes) {
        result.add(node)
        if (node.isDirectory && node.absolutePath in expandedFolders) {
            result.addAll(flattenTree(node.children, expandedFolders))
        }
    }
    return result
}

@Composable
fun FileTreeNodeRow(
    node: FileNode,
    isActive: Boolean,
    isExpanded: Boolean,
    theme: AppEditorTheme,
    onNodeClick: () -> Unit,
    onNewFileInFolder: () -> Unit,
    onNewFolderInFolder: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val iconColor = when {
        node.isDirectory -> Color(0xFFDCDCAA) // Folder yellow
        node.extension in listOf("html", "htm") -> Color(0xFFE44D26)
        node.extension == "css" -> Color(0xFF569CD6)
        node.extension in listOf("js", "javascript") -> Color(0xFFF7DF1E)
        node.extension in listOf("kt", "kts") -> Color(0xFFB125EA)
        node.extension in listOf("py", "python") -> Color(0xFF4EC9B0)
        else -> Color(0xFFCCCCCC)
    }

    val icon = when {
        node.isDirectory && isExpanded -> Icons.Default.FolderOpen
        node.isDirectory && !isExpanded -> Icons.Default.Folder
        node.extension in listOf("html", "htm") -> Icons.Default.Code
        node.extension == "css" -> Icons.Default.Css
        node.extension in listOf("js", "javascript") -> Icons.Default.Javascript
        node.extension in listOf("kt", "kts") -> Icons.Default.Code
        node.extension in listOf("py", "python") -> Icons.Default.Terminal
        else -> Icons.Default.Description
    }

    val indentDp = (node.level * 14 + 10).dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onNodeClick)
            .background(if (isActive) Color(0xFF37373D) else Color.Transparent)
            .padding(start = indentDp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expand/Collapse Chevron for directories
        if (node.isDirectory) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = theme.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }

        // File/Folder icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = node.name,
            color = if (isActive) Color.White else Color(0xFFCCCCCC),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (node.isDirectory || isActive) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(15.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(theme.sidebarBg)
            ) {
                if (node.isDirectory) {
                    DropdownMenuItem(
                        text = { Text("New File in Folder", color = Color.White, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = theme.accentColor) },
                        onClick = {
                            menuExpanded = false
                            onNewFileInFolder()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New Subfolder", color = Color.White, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = theme.accentColor) },
                        onClick = {
                            menuExpanded = false
                            onNewFolderInFolder()
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Rename", color = Color.White, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = theme.accentColor) },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color(0xFFF14C4C), fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFF14C4C)) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}
