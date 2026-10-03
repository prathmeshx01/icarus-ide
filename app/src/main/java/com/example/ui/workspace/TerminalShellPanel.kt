package com.example.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Project
import com.example.ui.theme.AppEditorTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ShellOutput(
    val line: String,
    val isCommand: Boolean = false,
    val isError: Boolean = false
)

@Composable
fun TerminalShellPanel(
    project: Project,
    theme: AppEditorTheme,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val history = remember {
        mutableStateListOf(
            ShellOutput("ICARUS Developer Shell [Version 0.1 - pRoot / Linux Ready]"),
            ShellOutput("Type 'help' for available commands or 'clear' to reset terminal."),
            ShellOutput("Workspace: ${project.name} (${project.rootDirPath})")
        )
    }

    var inputCommand by remember { mutableStateOf("") }

    fun executeCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        history.add(ShellOutput("icarus@android:~$ $trimmed", isCommand = true))

        val parts = trimmed.split("\\s+".toRegex())
        val command = parts[0].lowercase()
        val args = parts.drop(1)

        val projectDir = File(project.rootDirPath)

        when (command) {
            "help" -> {
                history.add(ShellOutput("Available commands:"))
                history.add(ShellOutput("  ls               - List files in current project"))
                history.add(ShellOutput("  pwd              - Print working directory"))
                history.add(ShellOutput("  cat <file>       - Output contents of a file"))
                history.add(ShellOutput("  echo <text>      - Print text to stdout"))
                history.add(ShellOutput("  python <file.py> - Execute Python script in offline runtime"))
                history.add(ShellOutput("  date             - Display current date and time"))
                history.add(ShellOutput("  clear            - Clear terminal screen"))
                history.add(ShellOutput("  proot --info     - Check Alpine / Linux container status"))
            }
            "python", "python3" -> {
                if (args.isEmpty()) {
                    history.add(ShellOutput("Python 3.11.0 (Icarus Local Micro-Runtime, Oct 2026)"))
                    history.add(ShellOutput("Usage: python <file.py>"))
                } else {
                    val target = File(projectDir, args[0])
                    if (!target.exists()) {
                        history.add(ShellOutput("python: can't open file '${args[0]}': [Errno 2] No such file or directory", isError = true))
                    } else {
                        val pyCode = target.readText(Charsets.UTF_8)
                        val res = com.example.runtime.PythonEngine.execute(pyCode)
                        if (res.output.isNotBlank()) {
                            res.output.lines().filter { it.isNotBlank() }.forEach { history.add(ShellOutput(it)) }
                        }
                        res.errors.forEach { history.add(ShellOutput(it, isError = true)) }
                    }
                }
            }
            "clear" -> {
                history.clear()
            }
            "pwd" -> {
                history.add(ShellOutput(projectDir.absolutePath))
            }
            "ls" -> {
                val files = projectDir.listFiles { f -> !f.name.startsWith(".") }
                if (files.isNullOrEmpty()) {
                    history.add(ShellOutput("(empty directory)"))
                } else {
                    val list = files.joinToString("  ") { if (it.isDirectory) "${it.name}/" else it.name }
                    history.add(ShellOutput(list))
                }
            }
            "cat" -> {
                if (args.isEmpty()) {
                    history.add(ShellOutput("cat: missing operand", isError = true))
                } else {
                    val target = File(projectDir, args[0])
                    if (!target.exists()) {
                        history.add(ShellOutput("cat: ${args[0]}: No such file", isError = true))
                    } else if (target.isDirectory) {
                        history.add(ShellOutput("cat: ${args[0]}: Is a directory", isError = true))
                    } else {
                        val content = target.readText(Charsets.UTF_8).take(1000)
                        history.add(ShellOutput(content))
                    }
                }
            }
            "echo" -> {
                history.add(ShellOutput(args.joinToString(" ")))
            }
            "date" -> {
                val sdf = SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US)
                history.add(ShellOutput(sdf.format(Date())))
            }
            "proot" -> {
                history.add(ShellOutput("[pRoot Container Engine Subsystem]"))
                history.add(ShellOutput("Architecture: Ready for Milestone 10 Alpine Linux rootfs."))
                history.add(ShellOutput("Host OS: Android Linux Kernel (sandboxed)"))
            }
            else -> {
                history.add(ShellOutput("bash: $command: command not found", isError = true))
            }
        }

        inputCommand = ""
    }

    Surface(
        modifier = modifier,
        color = Color(0xFF0F1012),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.border)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            // Terminal Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = null,
                        tint = theme.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TERMINAL: icarus@android",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "bash 5.2 (pRoot Ready)",
                        fontSize = 11.sp,
                        color = theme.textSecondary
                    )
                }

                Row {
                    IconButton(onClick = { history.clear() }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Clear",
                            tint = theme.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = theme.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Output lines
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF0A0B0D), RoundedCornerShape(4.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(history) { item ->
                    Text(
                        text = item.line,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            item.isError -> Color(0xFFF85149)
                            item.isCommand -> theme.accentColor
                            else -> Color(0xFFCCCCCC)
                        },
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Command input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16171A), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "icarus@android:~$ ",
                    color = theme.accentColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                BasicTextField(
                    value = inputCommand,
                    onValueChange = { inputCommand = it },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(theme.accentColor),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { executeCommand(inputCommand) }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("terminal_input_field")
                )
            }
        }
    }
}
