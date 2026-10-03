package com.example.editor

import com.example.model.EditorTab
import com.example.model.Project
import com.example.model.ProjectType
import com.example.runtime.PythonEngine
import java.io.File

object PreviewBundler {

    /**
     * Prepares a self-contained, offline HTML string for the WebView.
     * Inlines CSS stylesheets and JS scripts from the project folder (or active tabs)
     * so that edits are immediately reflected with zero WebView caching delays.
     * Also supports direct offline Python code execution & interactive terminal.
     */
    fun bundleHtml(
        project: Project,
        openTabs: List<EditorTab> = emptyList()
    ): String {
        val rootDir = File(project.rootDirPath)
        val indexHtmlFile = File(rootDir, "index.html")
        val mainPyFile = File(rootDir, "main.py")

        // If it's a Python project or main.py exists without index.html, run Python engine!
        if (project.type == ProjectType.PYTHON || (mainPyFile.exists() && !indexHtmlFile.exists())) {
            val pyCode = openTabs.find { it.file.name == "main.py" }?.content
                ?: if (mainPyFile.exists()) mainPyFile.readText(Charsets.UTF_8) else ""
            val result = PythonEngine.execute(pyCode)
            return generatePythonOutputHtml(project, pyCode, result.output, result.errors, result.executionTimeMs)
        }

        if (!indexHtmlFile.exists()) {
            return generateMissingHtmlFallback(project)
        }

        // Get index.html content (prefer unsaved tab if dirty)
        var htmlContent = openTabs.find { it.file.absolutePath == indexHtmlFile.absolutePath }?.content
            ?: indexHtmlFile.readText(Charsets.UTF_8)

        // Read all local files in project directory for inlining/resolution
        val localFiles = rootDir.listFiles() ?: emptyArray()
        val fileMap = mutableMapOf<String, String>()

        for (f in localFiles) {
            if (f.isFile && !f.name.startsWith(".")) {
                val tabContent = openTabs.find { it.file.absolutePath == f.absolutePath }?.content
                fileMap[f.name] = tabContent ?: f.readText(Charsets.UTF_8)
            }
        }

        // 1. Inline local CSS: <link rel="stylesheet" href="style.css">
        val cssLinkRegex = Regex("<link[^>]+rel=[\"']stylesheet[\"'][^>]*href=[\"']([^\"']+)[\"'][^>]*>", RegexOption.IGNORE_CASE)
        htmlContent = cssLinkRegex.replace(htmlContent) { match ->
            val href = match.groupValues[1]
            val fileName = href.substringAfterLast('/')
            val cssCode = fileMap[fileName]
            if (cssCode != null) {
                "<style id=\"__inline_$fileName\">\n/* inlined $fileName */\n$cssCode\n</style>"
            } else {
                match.value
            }
        }

        // 2. Inline local JS scripts: <script src="script.js"></script>
        val scriptSrcRegex = Regex("<script[^>]+src=[\"']([^\"']+)[\"'][^>]*>\\s*</script>", RegexOption.IGNORE_CASE)
        htmlContent = scriptSrcRegex.replace(htmlContent) { match ->
            val src = match.groupValues[1]
            val fileName = src.substringAfterLast('/')
            val jsCode = fileMap[fileName]
            if (jsCode != null) {
                "<script id=\"__inline_$fileName\">\n// inlined $fileName\n$jsCode\n</script>"
            } else {
                match.value
            }
        }

        // 3. Inject Offline Runtime Error Interceptor & Performance Diagnostics
        val runtimeScript = """
            <script id="__icarus_runtime_instrumentation">
            (function() {
                window.__icarus_errors = [];
                window.onerror = function(msg, url, line, col, error) {
                    var file = url ? url.substring(url.lastIndexOf('/') + 1) : 'script.js';
                    var errObj = { message: msg, file: file, line: line, col: col };
                    window.__icarus_errors.push(errObj);
                    console.error('[Runtime Error] ' + msg + ' (' + file + ':' + line + ')');
                    
                    var toast = document.getElementById('__icarus_err_toast');
                    if (!toast) {
                        toast = document.createElement('div');
                        toast.id = '__icarus_err_toast';
                        toast.style = 'position:fixed;bottom:12px;left:12px;right:12px;background:#2d0909;border:1px solid #f85149;color:#ffb4b4;padding:10px 14px;border-radius:8px;font-family:monospace;font-size:12px;z-index:9999999;box-shadow:0 8px 24px rgba(0,0,0,0.8);display:flex;justify-content:space-between;align-items:center;animation:fadeIn 0.2s ease;';
                        document.body.appendChild(toast);
                    }
                    toast.innerHTML = '<div style="display:flex;align-items:center;gap:8px;"><span style="color:#ff7b72;font-weight:bold;">⚠ Error:</span> <span>' + msg + '</span> <span style="color:#8b949e;font-size:10px;">(Line ' + line + ')</span></div><button onclick="this.parentElement.remove()" style="background:#f85149;border:none;color:#fff;padding:4px 8px;border-radius:4px;cursor:pointer;font-weight:bold;font-size:11px;">Dismiss</button>';
                    return false;
                };

                window.addEventListener('unhandledrejection', function(event) {
                    var reason = event.reason ? (event.reason.message || event.reason) : 'Promise Rejected';
                    console.error('[Unhandled Promise] ' + reason);
                });
            })();
            </script>
        """.trimIndent()

        htmlContent = if (htmlContent.contains("<head>", ignoreCase = true)) {
            htmlContent.replaceFirst("<head>", "<head>\n$runtimeScript", ignoreCase = true)
        } else if (htmlContent.contains("<html>", ignoreCase = true)) {
            htmlContent.replaceFirst("<html>", "<html>\n<head>$runtimeScript</head>", ignoreCase = true)
        } else {
            "$runtimeScript\n$htmlContent"
        }

        return htmlContent
    }

    private fun generatePythonOutputHtml(
        project: Project,
        code: String,
        stdout: String,
        errors: List<String>,
        elapsedMs: Long
    ): String {
        val escapedOut = stdout.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").ifBlank { "Program executed with no output." }
        val errHtml = if (errors.isNotEmpty()) {
            "<div class=\"error-box\">" + errors.joinToString("<br>") { it.replace("<", "&lt;").replace(">", "&gt;") } + "</div>"
        } else ""

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                body {
                  background-color: #0b0d13;
                  color: #e6edf3;
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace;
                  margin: 0;
                  padding: 16px;
                  box-sizing: border-box;
                  min-height: 100vh;
                  display: flex;
                  flex-direction: column;
                }
                .header {
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                  padding-bottom: 12px;
                  border-bottom: 1px solid #21262d;
                  margin-bottom: 14px;
                }
                .badge {
                  background: rgba(255, 216, 102, 0.15);
                  color: #ffd866;
                  border: 1px solid rgba(255, 216, 102, 0.3);
                  padding: 4px 10px;
                  border-radius: 9999px;
                  font-size: 11px;
                  font-weight: bold;
                  font-family: monospace;
                  display: flex;
                  align-items: center;
                  gap: 6px;
                }
                .dot { width: 7px; height: 7px; background: #3fb950; border-radius: 50%; }
                .metrics { font-size: 11px; color: #8b949e; font-family: monospace; }
                .terminal-card {
                  background: #161b22;
                  border: 1px solid #30363d;
                  border-radius: 8px;
                  padding: 14px;
                  flex: 1;
                  display: flex;
                  flex-direction: column;
                  box-shadow: 0 8px 24px rgba(0,0,0,0.5);
                }
                .terminal-title {
                  font-size: 11px;
                  color: #8b949e;
                  font-family: monospace;
                  margin-bottom: 8px;
                  font-weight: bold;
                  display: flex;
                  justify-content: space-between;
                }
                .terminal-body {
                  background: #0d1117;
                  border: 1px solid #21262d;
                  border-radius: 6px;
                  padding: 12px;
                  color: #a5d6ff;
                  font-family: "Courier New", Courier, monospace;
                  font-size: 12.5px;
                  line-height: 1.6;
                  white-space: pre-wrap;
                  word-break: break-all;
                  flex: 1;
                  overflow-y: auto;
                }
                .error-box {
                  background: #2d0909;
                  border: 1px solid #f85149;
                  color: #ffb4b4;
                  padding: 10px 12px;
                  border-radius: 6px;
                  font-family: monospace;
                  font-size: 12px;
                  margin-bottom: 12px;
                }
                .footer {
                  margin-top: 12px;
                  text-align: center;
                  font-size: 11px;
                  color: #8b949e;
                }
              </style>
            </head>
            <body>
              <div class="header">
                <div class="badge"><span class="dot"></span> PYTHON 3 RUNTIME</div>
                <div class="metrics">${elapsedMs}ms &bull; main.py</div>
              </div>
              
              $errHtml

              <div class="terminal-card">
                <div class="terminal-title">
                  <span>STANDARD OUTPUT (STDOUT)</span>
                  <span>EXIT: 0</span>
                </div>
                <div class="terminal-body">$escapedOut</div>
              </div>

              <div class="footer">
                ICARUS Python Local Engine &bull; Pure Offline Execution
              </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun generateMissingHtmlFallback(project: Project): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                body {
                  background-color: #0d1117;
                  color: #c9d1d9;
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace;
                  display: flex;
                  flex-direction: column;
                  align-items: center;
                  justify-content: center;
                  height: 100vh;
                  margin: 0;
                  padding: 1.5rem;
                  box-sizing: border-box;
                  text-align: center;
                }
                .card {
                  background: #161b22;
                  border: 1px solid #30363d;
                  border-radius: 12px;
                  padding: 2rem;
                  max-width: 380px;
                  box-shadow: 0 10px 30px rgba(0,0,0,0.5);
                }
                h1 { color: #58a6ff; font-size: 1.4rem; margin-top: 0; margin-bottom: 0.5rem; }
                p { color: #8b949e; font-size: 0.9rem; line-height: 1.5; margin-bottom: 1.5rem; }
                .pill {
                  background: #21262d;
                  color: #79c0ff;
                  font-family: monospace;
                  padding: 0.5rem 1rem;
                  border-radius: 6px;
                  border: 1px solid #30363d;
                  display: inline-block;
                  font-size: 0.85rem;
                }
              </style>
            </head>
            <body>
              <div class="card">
                <h1>index.html Not Found</h1>
                <p>The workspace <strong>${project.name}</strong> does not have an entry point file to render.</p>
                <div class="pill">Create index.html in files</div>
              </div>
            </body>
            </html>
        """.trimIndent()
    }
}
