package com.example.data

import android.content.Context
import com.example.model.Project
import com.example.model.ProjectType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID

class ProjectRepository(private val context: Context) {

    private val projectsRoot: File
        get() = File(context.filesDir, "projects").apply { mkdirs() }

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    suspend fun refreshProjects() = withContext(Dispatchers.IO) {
        val root = projectsRoot
        val projectDirs = root.listFiles { file -> file.isDirectory } ?: emptyArray()
        val loaded = mutableListOf<Project>()

        for (dir in projectDirs) {
            val metaFile = File(dir, ".icarus_meta.json")
            if (metaFile.exists()) {
                try {
                    val json = JSONObject(metaFile.readText())
                    val id = json.optString("id", dir.name)
                    val name = json.optString("name", dir.name)
                    val typeStr = json.optString("type", ProjectType.WEB.name)
                    val type = try { ProjectType.valueOf(typeStr) } catch (_: Exception) { ProjectType.WEB }
                    val createdAt = json.optLong("createdAt", dir.lastModified())
                    val updatedAt = json.optLong("updatedAt", dir.lastModified())
                    val count = dir.listFiles { f -> !f.name.startsWith(".") }?.size ?: 0

                    loaded.add(
                        Project(
                            id = id,
                            name = name,
                            type = type,
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            rootDirPath = dir.absolutePath,
                            fileCount = count
                        )
                    )
                } catch (e: Exception) {
                    // Skip or fallback corrupted meta
                }
            }
        }

        val sorted = loaded.sortedByDescending { it.updatedAt }
        _projects.value = sorted
        sorted
    }

    suspend fun ensureInitialProjectIfEmpty(): Project? = withContext(Dispatchers.IO) {
        val current = refreshProjects()
        if (current.isEmpty()) {
            createProject("Canvas Particle Simulation", ProjectType.CANVAS)
        } else {
            null
        }
    }

    suspend fun createProject(name: String, type: ProjectType): Project = withContext(Dispatchers.IO) {
        val sanitizedName = name.trim().ifBlank { "Untitled Project" }
        val id = "proj_" + UUID.randomUUID().toString().take(8)
        val dir = File(projectsRoot, id).apply { mkdirs() }

        val now = System.currentTimeMillis()
        val metaJson = JSONObject().apply {
            put("id", id)
            put("name", sanitizedName)
            put("type", type.name)
            put("createdAt", now)
            put("updatedAt", now)
        }
        File(dir, ".icarus_meta.json").writeText(metaJson.toString())

        val fileCount: Int
        when (type) {
            ProjectType.WEB -> {
                File(dir, "index.html").writeText(ProjectTemplates.DEFAULT_HTML)
                File(dir, "style.css").writeText(ProjectTemplates.DEFAULT_CSS)
                File(dir, "script.js").writeText(ProjectTemplates.DEFAULT_JS)
                fileCount = 3
            }
            ProjectType.CANVAS -> {
                File(dir, "index.html").writeText(ProjectTemplates.CANVAS_HTML)
                File(dir, "script.js").writeText(ProjectTemplates.CANVAS_JS)
                fileCount = 2
            }
            ProjectType.PORTFOLIO -> {
                File(dir, "index.html").writeText(ProjectTemplates.PORTFOLIO_HTML)
                File(dir, "style.css").writeText(ProjectTemplates.PORTFOLIO_CSS)
                File(dir, "script.js").writeText(ProjectTemplates.PORTFOLIO_JS)
                fileCount = 3
            }
            ProjectType.KOTLIN -> {
                File(dir, "main.kt").writeText(ProjectTemplates.KOTLIN_CODE)
                fileCount = 1
            }
            ProjectType.PYTHON -> {
                File(dir, "main.py").writeText(ProjectTemplates.PYTHON_CODE)
                fileCount = 1
            }
            ProjectType.BLANK -> {
                File(dir, "index.html").writeText(ProjectTemplates.BLANK_HTML)
                fileCount = 1
            }
        }

        val project = Project(
            id = id,
            name = sanitizedName,
            type = type,
            createdAt = now,
            updatedAt = now,
            rootDirPath = dir.absolutePath,
            fileCount = fileCount
        )

        refreshProjects()
        project
    }

    suspend fun deleteProject(projectId: String): Boolean = withContext(Dispatchers.IO) {
        val dir = File(projectsRoot, projectId)
        val success = if (dir.exists()) dir.deleteRecursively() else false
        refreshProjects()
        success
    }

    suspend fun touchProject(project: Project) = withContext(Dispatchers.IO) {
        val dir = File(project.rootDirPath)
        val metaFile = File(dir, ".icarus_meta.json")
        if (metaFile.exists()) {
            try {
                val json = JSONObject(metaFile.readText())
                json.put("updatedAt", System.currentTimeMillis())
                metaFile.writeText(json.toString())
            } catch (_: Exception) {}
        }
        refreshProjects()
    }
}
