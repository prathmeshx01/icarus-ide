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
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.editor.PreviewBundler
import com.example.model.ConsoleLogEntry
import com.example.model.EditorTab
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

enum class PreviewViewport(val label: String, val widthDp: Int?) {
    RESPONSIVE("Responsive", null),
    MOBILE("Phone (375px)", 375),
    TABLET("Tablet (600px)", 600)
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPreviewPane(
    project: Project,
    openTabs: List<EditorTab> = emptyList(),
    consoleLogs: List<ConsoleLogEntry>,
    onConsoleLog: (level: String, message: String, sourceId: String?, lineNumber: Int?) -> Unit,
    onClearLogs: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showConsole by remember { mutableStateOf(false) }
    var reloadCount by remember { mutableStateOf(0) }
    var currentViewport by remember { mutableStateOf(PreviewViewport.RESPONSIVE) }

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
            // Enhanced Output Screen Top Bar
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (errorCount > 0) ZincRed else ZincGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = project.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "OFFLINE",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = ZincGreen,
                                    modifier = Modifier
                                        .background(ZincGreen.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = "file://${File(project.rootDirPath).name}/index.html",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ZincTextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    // Viewport Mode Switcher
                    IconButton(
                        onClick = {
                            currentViewport = when (currentViewport) {
                                PreviewViewport.RESPONSIVE -> PreviewViewport.MOBILE
                                PreviewViewport.MOBILE -> PreviewViewport.TABLET
                                PreviewViewport.TABLET -> PreviewViewport.RESPONSIVE
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = when (currentViewport) {
                                PreviewViewport.RESPONSIVE -> Icons.Default.Devices
                                PreviewViewport.MOBILE -> Icons.Default.PhoneAndroid
                                PreviewViewport.TABLET -> Icons.Default.Tablet
                            },
                            contentDescription = "Switch Viewport: ${currentViewport.label}",
                            tint = if (currentViewport == PreviewViewport.RESPONSIVE) ZincTextSecondary else ZincAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Reload Page
                    IconButton(
                        onClick = {
                            isErrorBannerDismissed = false
                            reloadCount++
                            webViewRef?.let { wv ->
                                loadFreshContent(wv, project, openTabs, onConsoleLog)
                            }
                        },
                        modifier = Modifier.size(36.dp).testTag("preview_reload_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload Page", tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    // Console Drawer Toggle with Badge
                    IconButton(
                        onClick = { showConsole = !showConsole },
                        modifier = Modifier.size(36.dp).testTag("preview_console_toggle_btn")
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
                                tint = if (showConsole) ZincAccent else (if (errorCount > 0) ZincRed else Color.White),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
            )

            // Content Area: Centered Frame (for Phone/Tablet) or Full-Width
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF090A0C)),
                contentAlignment = Alignment.TopCenter
            ) {
                val webViewModifier = if (currentViewport.widthDp != null) {
                    Modifier
                        .width(currentViewport.widthDp!!.dp)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                        .border(1.dp, ZincBorder, RoundedCornerShape(8.dp))
                } else {
                    Modifier.fillMaxSize()
                }

                AndroidView(
                    modifier = webViewModifier.testTag("local_webview_pane"),
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
                                    onConsoleLog("ERROR", "Asset Error: $desc ($url)", null, null)
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

                            loadFreshContent(this, project, openTabs, onConsoleLog)
                            webViewRef = this
                        }
                    },
                    update = { webView ->
                        loadFreshContent(webView, project, openTabs, onConsoleLog)
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

                // Slide-up Developer Console Drawer with REPL Runner
                if (showConsole) {
                    ConsoleLogPanel(
                        logs = consoleLogs,
                        onClear = onClearLogs,
                        onClose = { showConsole = false },
                        onExecuteJs = { code ->
                            webViewRef?.evaluateJavascript(code) { result ->
                                onConsoleLog("LOG", "> $code\n< $result", "REPL", 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f)
                    )
                }
            }
        }
    }
}

/**
 * Loads project files into WebView bundled with all local CSS/JS to guarantee instant updates.
 */
private fun loadFreshContent(
    webView: WebView,
    project: Project,
    openTabs: List<EditorTab>,
    onConsoleLog: (String, String, String?, Int?) -> Unit
) {
    val projectDir = File(project.rootDirPath)
    val bundledHtml = PreviewBundler.bundleHtml(project, openTabs)

    webView.loadDataWithBaseURL(
        "file://${projectDir.absolutePath}/",
        bundledHtml,
        "text/html",
        "UTF-8",
        null
    )
}

@Composable
fun ConsoleLogPanel(
    logs: List<ConsoleLogEntry>,
    onClear: () -> Unit,
    onClose: () -> Unit,
    onExecuteJs: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var filterLevel by remember { mutableStateOf("ALL") }
    var replInput by remember { mutableStateOf("") }

    val filteredLogs = remember(logs, filterLevel) {
        if (filterLevel == "ALL") logs
        else logs.filter { it.level.equals(filterLevel, ignoreCase = true) }
    }

    Surface(
        modifier = modifier,
        color = ZincSurfaceElevated,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ZincBorder)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            // Console Header with Filter Tabs
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
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("ALL", "ERROR", "WARN", "LOG").forEach { lvl ->
                        val isSelected = filterLevel == lvl
                        Surface(
                            modifier = Modifier
                                .clickable { filterLevel = lvl }
                                .padding(vertical = 2.dp),
                            color = if (isSelected) ZincAccent else Color(0x1AFFFFFF),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = lvl,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else ZincTextSecondary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = ZincTextSecondary, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = ZincTextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Log output list
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No console output. Run code or call console.log()",
                            color = ZincTextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ZincBackground, RoundedCornerShape(4.dp))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredLogs) { entry ->
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
                                    fontSize = 9.sp,
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
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (entry.lineNumber != null && entry.lineNumber > 0) {
                                        Text(
                                            text = "at line ${entry.lineNumber}",
                                            color = ZincTextMuted,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Action Snippet Bar (Matching Mockup)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SNIPS:", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = ZincTextMuted, fontWeight = FontWeight.Bold)
                listOf("npm test", "build", "git status", "lint", "clear").forEach { snippet ->
                    Surface(
                        modifier = Modifier
                            .clickable {
                                if (snippet == "clear") {
                                    onClear()
                                } else {
                                    replInput = snippet
                                }
                            }
                            .padding(vertical = 2.dp),
                        color = Color(0x1AFFFFFF),
                        shape = RoundedCornerShape(3.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, ZincBorder)
                    ) {
                        Text(
                            text = snippet,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ZincTextSecondary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // REPL Interactive Evaluator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replInput,
                    onValueChange = { replInput = it },
                    placeholder = { Text("eval: console.log(window.icarus)", fontSize = 11.sp, color = ZincTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZincAccent,
                        unfocusedBorderColor = ZincBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f).height(38.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {
                        val cmd = replInput.trim()
                        if (cmd.isNotBlank()) {
                            onExecuteJs(cmd)
                            replInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZincAccent),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("EXEC ↵", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
