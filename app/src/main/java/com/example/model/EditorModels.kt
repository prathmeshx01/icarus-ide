package com.example.model

enum class ProjectType(
    val displayName: String,
    val badge: String,
    val description: String,
    val isExecutableV01: Boolean
) {
    WEB("Web Application", "HTML5", "HTML5, CSS & JS reactive starter with DOM events", true),
    CANVAS("Canvas Physics Simulation", "CANVAS", "HTML5 Canvas particle wave with touch & pointer dynamics", true),
    PORTFOLIO("Portfolio / Landing Page", "PORTFOLIO", "Modern dark developer portfolio with project showcases", true),
    KOTLIN("Kotlin Algorithm", "KOTLIN", "Data classes, collection filtering, and sorting routines", false),
    PYTHON("Python Script & Data", "PYTHON", "JSON data pipeline with record parsing and CLI entry", false),
    BLANK("Blank Scratchpad", "EMPTY", "Minimal clean canvas to start writing code from scratch", true)
}

data class Project(
    val id: String,
    val name: String,
    val type: ProjectType,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val rootDirPath: String,
    val fileCount: Int = 0,
    val isExternalStorage: Boolean = false
)

data class ProjectFile(
    val name: String,
    val relativePath: String,
    val extension: String,
    val isDirectory: Boolean,
    val absolutePath: String,
    val sizeBytes: Long = 0L,
    val lastModified: Long = System.currentTimeMillis()
)

data class FileNode(
    val name: String,
    val relativePath: String,
    val extension: String,
    val isDirectory: Boolean,
    val absolutePath: String,
    val sizeBytes: Long = 0L,
    val lastModified: Long = System.currentTimeMillis(),
    val children: List<FileNode> = emptyList(),
    val level: Int = 0
)

data class EditorTab(
    val file: ProjectFile,
    val content: String,
    val isDirty: Boolean = false,
    val lastSavedContent: String = content
)

data class GitDiffMarker(
    val line: Int,
    val type: GitDiffType
)

enum class GitDiffType {
    ADDED,
    MODIFIED,
    DELETED
}

data class CodeFoldingRegion(
    val startLine: Int,
    val endLine: Int,
    val isFolded: Boolean = false
)

data class CommandItem(
    val id: String,
    val title: String,
    val category: String,
    val shortcut: String? = null,
    val action: () -> Unit
)

data class CommandPaletteState(
    val isOpen: Boolean = false,
    val searchQuery: String = "",
    val mode: CommandPaletteMode = CommandPaletteMode.COMMANDS
)

enum class CommandPaletteMode {
    COMMANDS,
    FILES
}

data class ConsoleLogEntry(
    val level: String, // LOG, INFO, WARN, ERROR
    val message: String,
    val sourceId: String? = null,
    val lineNumber: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)
