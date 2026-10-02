package com.example.ui.workspace

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.ConsoleLogEntry
import com.example.model.Project
import com.example.ui.theme.ZincAccent
import com.example.ui.theme.ZincBackground
import com.example.ui.theme.ZincBorder
import com.example.ui.theme.ZincGreen
import com.example.ui.theme.ZincRed
import com.example.ui.theme.ZincSurface
import com.example.ui.theme.ZincSurfaceElevated
import com.example.ui.theme.ZincTextMuted
import com.example.ui.theme.ZincTextPrimary
import com.example.ui.theme.ZincTextSecondary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPreviewPane(
    project: Project,
    consoleLogs: List<ConsoleLogEntry>,
    onConsoleLog: (level: String, message: String, sourceId: String?, lineNumber: Int?) -> Unit,
    onClearLogs: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showConsole by remember { mutableStateOf(false) }
    var reloadTrigger by remember { mutableStateOf(0) }

    val errorCount = remember(consoleLogs) {
        consoleLogs.count { it.level.equals("ERROR", ignoreCase = true) }
    }

    val latestError = remember(consoleLogs) {
        consoleLogs.lastOrNull { it.level.equals("ERROR", ignoreCase = true) }
    }

    var isErrorBannerDismissed by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = ZincBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Preview Header Bar
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ZincSurface,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onClose, modifier = Modifier.testTag("preview_close_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Editor")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (errorCount > 0) ZincRed else ZincGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Preview: ${project.name}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (errorCount > 0) "Offline Preview • $errorCount Error(s)" else "Offline Preview • Healthy",
                                fontSize = 11.sp,
                                color = if (errorCount > 0) ZincRed else ZincTextSecondary
                            )
                        }
                    }
                },
                actions = {
                    // Reload Page
                    IconButton(
                        onClick = {
                            isErrorBannerDismissed = false
                            reloadTrigger++
                            webViewRef?.reload()
                        },
                        modifier = Modifier.testTag("preview_reload_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload Page", tint = Color.White)
                    }

                    // Console Drawer Toggle with Badge
                    IconButton(
                        onClick = { showConsole = !showConsole },
                        modifier = Modifier.testTag("preview_console_toggle_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (consoleLogs.isNotEmpty()) {
                                    val badgeColor = if (errorCount > 0) ZincRed else ZincAccent
                                    Badge(containerColor = badgeColor, contentColor = Color.White) {
                                        Text("${consoleLogs.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Terminal,
                                contentDescription = "Console Logs",
                                tint = if (showConsole) ZincAccent else (if (errorCount > 0) ZincRed else Color.White)
                            )
                        }
                    }
                }
            )

            // Content Area: WebView & Optional Bottom Console Pane & In-Preview Error Overlay
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // The Native Android WebView running 100% offline code
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("local_webview_pane"),
                    factory = { context ->
                        WebView(context).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = true
                                allowContentAccess = true
                                allowFileAccessFromFileURLs = true
                                allowUniversalAccessFromFileURLs = true
                                cacheMode = WebSettings.LOAD_NO_CACHE
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    val url = request?.url?.toString() ?: ""
                                    val desc = error?.description?.toString() ?: "Resource load failed"
                                    onConsoleLog("ERROR", "Offline Asset Error: $desc ($url)", null, null)
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                    consoleMessage?.let { msg ->
                                        val level = when (msg.messageLevel()) {
                                            ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                                            ConsoleMessage.MessageLevel.WARNING -> "WARN"
                                            ConsoleMessage.MessageLevel.LOG -> "LOG"
                                            else -> "INFO"
                                        }
                                        onConsoleLog(level, msg.message(), msg.sourceId(), msg.lineNumber())
                                    }
                                    return true
                                }
                            }

                            loadOfflineProject(this, project, onConsoleLog)
                            webViewRef = this
                        }
                    },
                    update = { webView ->
                        if (reloadTrigger > 0) {
                            loadOfflineProject(webView, project, onConsoleLog)
                        }
                    }
                )

                // In-App Offline Runtime Error Banner (when an error occurs in the code)
                if (latestError != null && !isErrorBannerDismissed && !showConsole) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(12.dp),
                        color = Color(0xFF2E0808),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ZincRed)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = ZincRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Runtime Error Detected",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = latestError.message,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFB4B4),
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 2
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { showConsole = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ZincRed),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Inspect", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = { isErrorBannerDismissed = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Slide-up Developer Console Drawer
                if (showConsole) {
                    ConsoleLogPanel(
                        logs = consoleLogs,
                        onClear = onClearLogs,
                        onClose = { showConsole = false },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(0.45f)
                    )
                }
            }
        }
    }
}

/**
 * Loads project files into WebView with offline error boundary instrumentation.
 */
private fun loadOfflineProject(
    webView: WebView,
    project: Project,
    onConsoleLog: (String, String, String?, Int?) -> Unit
) {
    val projectDir = File(project.rootDirPath)
    val indexHtml = File(projectDir, "index.html")

    if (indexHtml.exists()) {
        var rawHtml = indexHtml.readText(Charsets.UTF_8)

        // Offline Error Instrumentation Script
        val offlineErrorCatchScript = """
            <script id="__icarus_error_catcher">
            window.onerror = function(msg, url, line, col, error) {
                var cleanUrl = url ? url.substring(url.lastIndexOf('/') + 1) : 'script.js';
                var formatted = msg + ' (' + cleanUrl + ':' + line + ')';
                console.error(formatted);
                return false;
            };
            window.addEventListener('unhandledrejection', function(event) {
                var reason = event.reason ? (event.reason.message || event.reason) : 'Unknown Promise Error';
                console.error('Unhandled Promise: ' + reason);
            });
            </script>
        """.trimIndent()

        // Inject script right inside <head> or at the top of <html>
        val instrumentedHtml = if (rawHtml.contains("<head>", ignoreCase = true)) {
            rawHtml.replaceFirst("<head>", "<head>\n$offlineErrorCatchScript", ignoreCase = true)
        } else if (rawHtml.contains("<html>", ignoreCase = true)) {
            rawHtml.replaceFirst("<html>", "<html>\n<head>$offlineErrorCatchScript</head>", ignoreCase = true)
        } else {
            "$offlineErrorCatchScript\n$rawHtml"
        }

        webView.loadDataWithBaseURL(
            "file://${projectDir.absolutePath}/",
            instrumentedHtml,
            "text/html",
            "UTF-8",
            null
        )
    } else {
        // Offline Fallback View
        val fallbackHtml = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <style>
                body { background: #0f1012; color: #fff; font-family: -apple-system, sans-serif; padding: 2rem; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; margin: 0; text-align: center; }
                h1 { color: #388bfd; font-size: 1.5rem; margin-bottom: 0.5rem; }
                p { color: #8b949e; font-size: 0.95rem; max-width: 320px; line-height: 1.5; }
                .code { background: #1a1d24; padding: 0.5rem 1rem; border-radius: 6px; font-family: monospace; color: #79c0ff; margin-top: 1rem; border: 1px solid #282c34; }
              </style>
            </head>
            <body>
              <h1>index.html Not Found</h1>
              <p>The offline web engine requires an entry point file to render your workspace.</p>
              <div class="code">Create index.html in the workspace root</div>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(
            "file://${projectDir.absolutePath}/",
            fallbackHtml,
            "text/html",
            "UTF-8",
            null
        )
        onConsoleLog("WARN", "index.html not found in ${project.name}", null, null)
    }
}

@Composable
fun ConsoleLogPanel(
    logs: List<ConsoleLogEntry>,
    onClear: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ZincSurfaceElevated,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ZincBorder)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            // Console Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = null,
                        tint = ZincAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "JavaScript Console",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${logs.size})",
                        fontSize = 11.sp,
                        color = ZincTextSecondary
                    )
                }

                Row {
                    IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Clear logs",
                            tint = ZincTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close console",
                            tint = ZincTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No console output. Call console.log() in script.js",
                        color = ZincTextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ZincBackground, RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { entry ->
                        val badgeColor = when (entry.level) {
                            "ERROR" -> ZincRed
                            "WARN" -> Color(0xFFD29922)
                            else -> ZincAccent
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = entry.level,
                                color = badgeColor,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.message,
                                    color = if (entry.level == "ERROR") Color(0xFFFFB4B4) else ZincTextPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (entry.lineNumber != null && entry.lineNumber > 0) {
                                    Text(
                                        text = "at line ${entry.lineNumber}",
                                        color = ZincTextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
