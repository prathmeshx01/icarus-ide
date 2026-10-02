package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsManager
import com.example.ui.theme.AppEditorTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by settingsManager.settings.collectAsState()
    val activeTheme = settings.theme

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = activeTheme.titleBarBg,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text = "Settings",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            )
        },
        containerColor = activeTheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Section 1: Themes
            item {
                SettingsSectionHeader(title = "COLOR THEME", icon = Icons.Default.Palette, theme = activeTheme)
            }

            items(AppEditorTheme.values().size) { index ->
                val theme = AppEditorTheme.values()[index]
                val isSelected = theme == activeTheme

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsManager.setTheme(theme) },
                    colors = CardDefaults.cardColors(containerColor = theme.sidebarBg),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) theme.accentColor else theme.border
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = theme.displayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = theme.description,
                                    fontSize = 11.sp,
                                    color = theme.textSecondary,
                                    lineHeight = 15.sp
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(theme.accentColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Palette Swatches Preview
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PaletteCircle(color = theme.background, label = "Bg")
                            PaletteCircle(color = theme.accentColor, label = "Accent")
                            PaletteCircle(color = theme.keywordColor, label = "Keyword")
                            PaletteCircle(color = theme.stringColor, label = "String")
                            PaletteCircle(color = theme.functionColor, label = "Fn")
                        }
                    }
                }
            }

            // Section 2: Editor Preferences
            item {
                Spacer(modifier = Modifier.height(6.dp))
                SettingsSectionHeader(title = "EDITOR PREFERENCES", icon = Icons.Default.Tune, theme = activeTheme)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = activeTheme.sidebarBg),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, activeTheme.border)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Font Size Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Editor Font Size",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "${settings.fontSizeSp.roundToInt()} sp",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeTheme.accentColor
                            )
                        }

                        Slider(
                            value = settings.fontSizeSp,
                            onValueChange = { settingsManager.setFontSize(it) },
                            valueRange = 11f..20f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = activeTheme.accentColor,
                                activeTrackColor = activeTheme.accentColor,
                                inactiveTrackColor = activeTheme.border
                            )
                        )

                        // Live sample preview
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            color = activeTheme.background,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, activeTheme.border)
                        ) {
                            Text(
                                text = "fun helloWorld() {\n    println(\"Sample Code Preview\")\n}",
                                fontSize = settings.fontSizeSp.sp,
                                fontFamily = FontFamily.Monospace,
                                color = activeTheme.textPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tab Size
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Tab Size", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("Number of spaces per indentation", fontSize = 11.sp, color = activeTheme.textSecondary)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TabSizeButton(
                                    label = "2 Spaces",
                                    isSelected = settings.tabSize == 2,
                                    theme = activeTheme,
                                    onClick = { settingsManager.setTabSize(2) }
                                )
                                TabSizeButton(
                                    label = "4 Spaces",
                                    isSelected = settings.tabSize == 4,
                                    theme = activeTheme,
                                    onClick = { settingsManager.setTabSize(4) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Word Wrap Switch
                        SettingsToggleRow(
                            title = "Word Wrap",
                            subtitle = "Wrap long code lines to prevent horizontal scrolling",
                            checked = settings.wordWrap,
                            theme = activeTheme,
                            onCheckedChange = { settingsManager.setWordWrap(it) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Line Numbers Switch
                        SettingsToggleRow(
                            title = "Line Numbers",
                            subtitle = "Display gutter with line indices on the left",
                            checked = settings.showLineNumbers,
                            theme = activeTheme,
                            onCheckedChange = { settingsManager.setShowLineNumbers(it) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Auto-Closing Brackets
                        SettingsToggleRow(
                            title = "Auto-Close Brackets",
                            subtitle = "Automatically insert closing brackets and quotes",
                            checked = settings.autoCloseBrackets,
                            theme = activeTheme,
                            onCheckedChange = { settingsManager.setAutoCloseBrackets(it) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Format on Save
                        SettingsToggleRow(
                            title = "Format on Save",
                            subtitle = "Automatically clean and re-indent code upon saving",
                            checked = settings.formatOnSave,
                            theme = activeTheme,
                            onCheckedChange = { settingsManager.setFormatOnSave(it) }
                        )
                    }
                }
            }

            // Section 3: About
            item {
                Spacer(modifier = Modifier.height(6.dp))
                SettingsSectionHeader(title = "ABOUT", icon = Icons.Default.Info, theme = activeTheme)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = activeTheme.sidebarBg),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, activeTheme.border)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ICARUS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "v0.1",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = activeTheme.accentColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Engineered for code craft. Syntax analysis, live preview, and minimal distraction.",
                            fontSize = 12.sp,
                            color = activeTheme.textSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    theme: AppEditorTheme,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = theme.accentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = theme.accentColor
        )
    }
}

@Composable
fun PaletteCircle(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, CircleShape)
                .border(1.dp, Color(0xFF555555), CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color(0xFFAAAAAA))
    }
}

@Composable
fun TabSizeButton(
    label: String,
    isSelected: Boolean,
    theme: AppEditorTheme,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (isSelected) theme.accentColor else Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) theme.accentColor else theme.border)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else theme.textSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    theme: AppEditorTheme,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(subtitle, fontSize = 11.sp, color = theme.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = theme.accentColor,
                uncheckedThumbColor = Color(0xFF888888),
                uncheckedTrackColor = Color(0xFF333333)
            )
        )
    }
}
