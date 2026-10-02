package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.FileRepository
import com.example.data.ProjectRepository
import com.example.data.SettingsManager
import com.example.editor.CodeFoldingDetector
import com.example.editor.DiagnosticSeverity
import com.example.editor.ErrorDetector
import com.example.editor.SyntaxHighlighter
import com.example.model.ProjectType
import com.example.ui.theme.AppEditorTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `appName is ICARUS`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ICARUS", appName)
    }

    @Test
    fun `create and load offline website project with tree`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val projectRepo = ProjectRepository(context)
        val fileRepo = FileRepository()

        val project = projectRepo.createProject("Test Website", ProjectType.WEB)
        assertNotNull(project)
        assertEquals("Test Website", project.name)
        assertEquals(ProjectType.WEB, project.type)

        val rootDir = File(project.rootDirPath)
        val tree = fileRepo.loadFileTree(rootDir)
        assertTrue(tree.any { it.name == "index.html" })
        assertTrue(tree.any { it.name == "style.css" })
        assertTrue(tree.any { it.name == "script.js" })

        // Create a subfolder and file inside it
        val componentsDir = fileRepo.createFolder(rootDir, "components").getOrNull()
        assertNotNull(componentsDir)
        val buttonFile = fileRepo.createFile(componentsDir!!, "button.js", "// button").getOrNull()
        assertNotNull(buttonFile)

        val updatedTree = fileRepo.loadFileTree(rootDir)
        val folderNode = updatedTree.find { it.name == "components" && it.isDirectory }
        assertNotNull(folderNode)
        assertTrue(folderNode!!.children.any { it.name == "button.js" })
    }

    @Test
    fun `create canvas physics project from starter template`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val projectRepo = ProjectRepository(context)
        val fileRepo = FileRepository()

        val project = projectRepo.createProject("Wave Simulation", ProjectType.CANVAS)
        assertNotNull(project)
        assertEquals(ProjectType.CANVAS, project.type)

        val files = fileRepo.listFiles(File(project.rootDirPath))
        assertTrue(files.any { it.name == "index.html" })
        assertTrue(files.any { it.name == "script.js" })
    }

    @Test
    fun `code folding detector detects braces and html blocks`() {
        val sampleCode = """
            fun calculate() {
                val x = 10
                val y = 20
            }
        """.trimIndent()

        val blocks = CodeFoldingDetector.detectFoldingBlocks(sampleCode, "kt")
        assertEquals(1, blocks.size)
        assertEquals(1, blocks[0].startLine)
        assertEquals(4, blocks[0].endLine)
    }

    @Test
    fun `error detector catches mismatched brackets and unclosed tags`() {
        val badHtml = "<div><h1>Title</h2></div>"
        val diagnostics = ErrorDetector.detectErrors(badHtml, "html")
        assertTrue(diagnostics.any { it.severity == DiagnosticSeverity.ERROR })

        val badJs = "function foo( { console.log('hello'); }"
        val jsDiagnostics = ErrorDetector.detectErrors(badJs, "js")
        assertTrue(jsDiagnostics.any { it.severity == DiagnosticSeverity.ERROR })
    }

    @Test
    fun `syntax highlighter supports Kotlin colorization`() {
        val kotlinCode = """
            package com.example
            @Composable
            fun UserProfile(name: String, count: Int = 42) {
                val greeting = "Welcome to ${'$'}name"
                if (count > 0) {
                    println(greeting)
                }
            }
        """.trimIndent()
        val highlighted = SyntaxHighlighter.highlight(kotlinCode, "kt", AppEditorTheme.OBSIDIAN_DARK)
        assertEquals(kotlinCode, highlighted.text)
        assertTrue(highlighted.spanStyles.isNotEmpty())
    }

    @Test
    fun `syntax highlighter supports HTML colorization`() {
        val htmlCode = "<!DOCTYPE html><div class=\"card\"><h1>Title</h1><!-- comment --></div>"
        val highlighted = SyntaxHighlighter.highlight(htmlCode, "html", AppEditorTheme.OBSIDIAN_DARK)
        assertEquals(htmlCode, highlighted.text)
        assertTrue(highlighted.spanStyles.isNotEmpty())
    }

    @Test
    fun `syntax highlighter supports JavaScript colorization`() {
        val jsCode = """
            const button = document.getElementById("btn");
            button.addEventListener("click", async () => {
                console.log("Clicked", 123);
            });
        """.trimIndent()
        val highlighted = SyntaxHighlighter.highlight(jsCode, "js", AppEditorTheme.OBSIDIAN_DARK)
        assertEquals(jsCode, highlighted.text)
        assertTrue(highlighted.spanStyles.isNotEmpty())
    }

    @Test
    fun `settings manager persists and updates theme`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settingsManager = SettingsManager(context)

        settingsManager.setTheme(AppEditorTheme.MONOKAI_PRO)
        assertEquals(AppEditorTheme.MONOKAI_PRO, settingsManager.settings.value.theme)

        settingsManager.setFontSize(16f)
        assertEquals(16f, settingsManager.settings.value.fontSizeSp, 0.01f)
    }
}
