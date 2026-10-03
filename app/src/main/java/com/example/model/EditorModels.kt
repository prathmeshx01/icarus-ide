package com.example.model

enum class ProjectType(
    val displayName: String,
    val badge: String,
    val description: String,
    val isExecutableV01: Boolean
) {
    WEB("HTML5 Starter", "HTML5", "Clean HTML5, CSS & JS responsive boilerplate", true),
    REACT("React 18 Starter", "REACT", "React 18 with Babel JSX transformer & component state", true),
    VUE("Vue 3 Starter", "VUE", "Vue 3 reactive starter with template compiler", true),
    TAILWIND("Tailwind CSS", "TAILWIND", "HTML5 layout styled with Tailwind CSS utility classes", true),
    CANVAS("Canvas 2D Graphics", "CANVAS", "HTML5 Canvas 2D render loop and animation scaffold", true),
    PORTFOLIO("Portfolio Landing", "PORTFOLIO", "Developer portfolio with project showcases & themes", true),
    PYTHON("Python Script", "PYTHON", "Python 3 entry script with argument handling & utils", true),
    KOTLIN("Kotlin Script", "KOTLIN", "Kotlin JVM entry with collection algorithms & main()", true),
    BLANK("Custom Multi-File", "CUSTOM", "Pick custom language files (HTML, CSS, JS, Python, README)", true)
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
