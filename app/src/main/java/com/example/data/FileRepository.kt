package com.example.data

import com.example.model.FileNode
import com.example.model.ProjectFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileRepository {

    suspend fun listFiles(projectDir: File): List<ProjectFile> = withContext(Dispatchers.IO) {
        if (!projectDir.exists() || !projectDir.isDirectory) return@withContext emptyList()
        val files = projectDir.listFiles() ?: return@withContext emptyList()
        files
            .filter { !it.name.startsWith(".") }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            .map { file ->
                ProjectFile(
                    name = file.name,
                    relativePath = file.relativeTo(projectDir).path,
                    extension = file.extension.lowercase(),
                    isDirectory = file.isDirectory,
                    absolutePath = file.absolutePath,
                    sizeBytes = if (file.isFile) file.length() else 0L
                )
            }
    }

    suspend fun loadFileTree(projectDir: File, rootDir: File = projectDir, level: Int = 0): List<FileNode> = withContext(Dispatchers.IO) {
        if (!projectDir.exists() || !projectDir.isDirectory) return@withContext emptyList()
        val entries = projectDir.listFiles { file -> !file.name.startsWith(".") } ?: return@withContext emptyList()

        entries
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            .map { file ->
                val children = if (file.isDirectory) {
                    loadFileTree(file, rootDir, level + 1)
                } else {
                    emptyList()
                }

                FileNode(
                    name = file.name,
                    relativePath = file.relativeTo(rootDir).path,
                    extension = file.extension.lowercase(),
                    isDirectory = file.isDirectory,
                    absolutePath = file.absolutePath,
                    sizeBytes = if (file.isFile) file.length() else 0L,
                    children = children,
                    level = level
                )
            }
    }

    suspend fun readFile(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                Result.failure(NoSuchFileException(file, reason = "File does not exist"))
            } else {
                Result.success(file.readText(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveFile(file: File, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFile(parentDir: File, name: String, initialContent: String = ""): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                val targetFile = File(parentDir, name)
                if (targetFile.exists()) {
                    return@withContext Result.failure(IllegalStateException("File already exists: $name"))
                }
                targetFile.parentFile?.mkdirs()
                targetFile.writeText(initialContent, Charsets.UTF_8)
                Result.success(targetFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createFolder(parentDir: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val targetDir = File(parentDir, name)
            if (targetDir.exists()) {
                return@withContext Result.failure(IllegalStateException("Folder already exists: $name"))
            }
            val created = targetDir.mkdirs()
            if (created || targetDir.isDirectory) {
                Result.success(targetDir)
            } else {
                Result.failure(IllegalStateException("Failed to create folder: $name"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameFile(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val destination = File(file.parentFile, newName)
            if (destination.exists()) {
                return@withContext Result.failure(IllegalStateException("Target name already exists: $newName"))
            }
            val success = file.renameTo(destination)
            if (success) Result.success(destination) else Result.failure(IllegalStateException("Could not rename file"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val success = if (file.isDirectory) file.deleteRecursively() else file.delete()
            if (success) Result.success(Unit) else Result.failure(IllegalStateException("Could not delete file"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
