package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.FicSwipeViewModel
import com.example.ui.viewmodel.ReaderFont
import com.example.ui.viewmodel.ReaderTheme
import kotlin.math.absoluteValue

data class ParagraphOffset(
    val index: Int,
    val text: String,
    val startOffset: Int,
    val endOffset: Int
)

@Composable
fun ReaderScreen(
    viewModel: FicSwipeViewModel,
    modifier: Modifier = Modifier
) {
    val activeStory by viewModel.activeStory.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val totalChapters by viewModel.activeChapters.collectAsState()
    val currentIndex by viewModel.currentChapterIndex.collectAsState()

    // Preferences & formatting parameters
    val readingTheme by viewModel.readerTheme.collectAsState()
    val readingFont by viewModel.readerFont.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val lineSpacing by viewModel.lineSpacing.collectAsState()

    // TTS state
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()
    val pitch by viewModel.playbackPitch.collectAsState()
    val highlightStart by viewModel.highlightStart.collectAsState()
    val highlightEnd by viewModel.highlightEnd.collectAsState()

    var showConfigPanel by remember { mutableStateOf(false) }
    var touchDragOffset by remember { mutableStateOf(0f) }

    // Map theme selections to Hex colors
    val themeBgColor by animateColorAsState(targetValue = getThemeColors(readingTheme).bg)
    val themeTextColor by animateColorAsState(targetValue = getThemeColors(readingTheme).text)
    val themeAccentColor = getThemeColors(readingTheme).accent
    val themeHighlightColor = getThemeColors(readingTheme).highlight

    if (activeStory == null || currentChapter == null) {
        ReaderEmptyState(onNavigate = { viewModel.selectTab("library") })
        return
    }

    val chapterContent = currentChapter?.content ?: ""

    // Parse the story into visual paragraphs with exact start/end positions for TTS bounds searching
    val paragraphs = remember(chapterContent) {
        val list = mutableListOf<ParagraphOffset>()
        var accumulatedOffset = 0
        val rawParts = chapterContent.split("\n")
        for (part in rawParts) {
            val partWithNewline = part + "\n"
            if (part.trim().isNotEmpty()) {
                list.add(
                    ParagraphOffset(
                        index = list.size,
                        text = part,
                        startOffset = accumulatedOffset,
                        endOffset = accumulatedOffset + part.length
                    )
                )
            }
            accumulatedOffset += partWithNewline.length
        }
        list
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeBgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. Reader Navigation Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.selectTab("library") }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Volver a la Biblioteca",
                        tint = themeTextColor
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = activeStory?.title ?: "",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = themeTextColor,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Capítulo $currentIndex de ${totalChapters.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = themeTextColor.copy(alpha = 0.7f)
                    )
                }

                IconButton(onClick = { showConfigPanel = !showConfigPanel }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configurar Fuente",
                        tint = if (showConfigPanel) themeAccentColor else themeTextColor
                    )
                }
            }

            Divider(color = themeTextColor.copy(alpha = 0.15f))

            // 2. Main Swipable Book Contents (horizontal drag advances chapter; LazyColumn handles premium scrolling)
            val lazyListState = rememberLazyListState()

            // Find paragraph containing highlight
            val activeParagraphIndex = remember(highlightStart, paragraphs) {
                if (highlightStart == -1) -1
                else paragraphs.indexOfFirst { highlightStart in it.startOffset..it.endOffset }
            }

            // Auto Scroll active paragraph to keep spoken content visible
            LaunchedEffect(activeParagraphIndex) {
                if (activeParagraphIndex != -1) {
                    lazyListState.animateScrollToItem(activeParagraphIndex + 1) // +1 for the title item
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                touchDragOffset += dragAmount.x
                            },
                            onDragEnd = {
                                val threshold = 150f
                                if (touchDragOffset > threshold) {
                                    // Swiped right -> previous chapter
                                    viewModel.prevChapter()
                                } else if (touchDragOffset < -threshold) {
                                    // Swiped left -> next chapter
                                    viewModel.nextChapter()
                                }
                                touchDragOffset = 0f
                            }
                        )
                    }
                    .padding(horizontal = 20.dp)
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header title block of chapter
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp)) {
                            Text(
                                text = currentChapter?.title ?: "",
                                style = getFontFamily(readingFont, isHeading = true).copy(
                                    fontSize = (fontSize + 4).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeTextColor
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = themeTextColor.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Dynamic paragraphs listing
                    itemsIndexed(paragraphs) { index, paragraph ->
                        val isHighlighted = highlightStart in paragraph.startOffset..paragraph.endOffset
                        
                        val relStart = if (isHighlighted) {
                            (highlightStart - paragraph.startOffset).coerceIn(0, paragraph.text.length)
                        } else 0
                        val relEnd = if (isHighlighted) {
                            (highlightEnd - paragraph.startOffset).coerceIn(0, paragraph.text.length)
                        } else 0

                        val paragraphStyledText = remember(paragraph.text, isHighlighted, relStart, relEnd, themeTextColor) {
                            if (isHighlighted && relStart != relEnd) {
                                buildAnnotatedString {
                                    append(paragraph.text.substring(0, relStart))
                                    withStyle(
                                        style = SpanStyle(
                                            background = themeHighlightColor,
                                            color = getThemeColors(readingTheme).highlightText,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    ) {
                                        append(paragraph.text.substring(relStart, relEnd))
                                    }
                                    append(paragraph.text.substring(relEnd))
                                }
                            } else {
                                AnnotatedString(paragraph.text)
                            }
                        }

                        // Display paragraph with premium Material typography and generous negative space
                        Text(
                            text = paragraphStyledText,
                            style = getFontFamily(readingFont, isHeading = false).copy(
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * lineSpacing).sp,
                                color = themeTextColor,
                                letterSpacing = getFontLetterSpacing(readingFont)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isHighlighted) themeTextColor.copy(alpha = 0.05f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    // Premium: Click any paragraph to start speaking from there!
                                    viewModel.speakFromParagraph(paragraph.startOffset)
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                        )
                    }

                    // Bottom empty spacing for floating dashboard clearance
                    item {
                        Spacer(modifier = Modifier.height(115.dp))
                    }
                }
            }

            // 3. Floating Customizable Typography Panel Drawer
            AnimatedVisibility(visible = showConfigPanel) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = themeTextColor.copy(alpha = 0.08f)
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Fuentes Personalizables 🔠",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = themeTextColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Theme switchers (Cream, Sepia, Twilight, OLED circles)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Aspecto", style = MaterialTheme.typography.bodySmall, color = themeTextColor.copy(alpha = 0.7f))
                            Row {
                                ReaderTheme.values().forEach { theme ->
                                    val isSelected = readingTheme == theme
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(getThemeColors(theme).bg)
                                            .clickable { viewModel.setTheme(theme) }
                                            .padding(4.dp)
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                                    .background(getThemeColors(theme).accent.copy(alpha = 0.8f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Font selection row
                        Text("Familia Tipográfica", style = MaterialTheme.typography.bodySmall, color = themeTextColor.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ReaderFont.values().forEach { font ->
                                val isSelected = readingFont == font
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) themeTextColor.copy(alpha = 0.2f)
                                            else themeTextColor.copy(alpha = 0.05f)
                                        )
                                        .clickable { viewModel.setFont(font) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = font.displayName,
                                        style = getFontFamily(font, isHeading = false).copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = themeTextColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Font size and Line Heights adjustments
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.FormatSize, contentDescription = null, tint = themeTextColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tamaño (${fontSize.toInt()}sp)", style = MaterialTheme.typography.bodySmall, color = themeTextColor)
                            }
                            Row {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(themeTextColor.copy(alpha = 0.1f))
                                        .clickable { viewModel.changeFontSize(-1.5f) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) { Text("A-", color = themeTextColor, fontWeight = FontWeight.Bold) }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(themeTextColor.copy(alpha = 0.1f))
                                        .clickable { viewModel.changeFontSize(1.5f) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) { Text("A+", color = themeTextColor, fontWeight = FontWeight.Bold) }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Interlineado", style = MaterialTheme.typography.bodySmall, color = themeTextColor.copy(alpha = 0.7f))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(1.2f, 1.5f, 1.8f, 2.2f).forEach { spacing ->
                                    val isMatch = lineSpacing == spacing
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isMatch) themeTextColor.copy(alpha = 0.25f)
                                                else themeTextColor.copy(alpha = 0.08f)
                                            )
                                            .clickable { viewModel.setLineSpacing(spacing) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("${spacing}x", color = themeTextColor, fontSize = 11.sp, fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Integrated TTS Audiobook Pinned Console Drawer
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                colors = CardDefaults.cardColors(containerColor = themeTextColor.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Prev Chapter button
                        IconButton(onClick = { viewModel.prevChapter() }) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Capítulo anterior", tint = themeTextColor)
                        }

                        // Play/Pause central TTS audio player
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.toggleSpeech() },
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(themeAccentColor, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Escuchar Fanfic",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // TTS Speech Wave Animator
                            Column {
                                Text(
                                    text = if (isSpeaking) "Narrando obra..." else "Audio lector listo",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = themeTextColor
                                )
                                if (isSpeaking) {
                                    SpeechWaveAnimation(themeTextColor)
                                } else {
                                    Text(
                                        text = "Haz clic en Play para reproducir o pulsa un párrafo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = themeTextColor.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        // Next Chapter button
                        IconButton(onClick = { viewModel.nextChapter() }) {
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Siguiente capítulo", tint = themeTextColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // TTS speed multipliers sliders (0.5x to 3.0x)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = "Velocidad de reproducción", tint = themeTextColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Velocidad: ${"%.1f".format(speed)}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = themeTextColor,
                            modifier = Modifier.width(100.dp)
                        )
                        Slider(
                            value = speed,
                            onValueChange = { viewModel.setSpeed(it) },
                            valueRange = 0.5f..3.0f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = themeAccentColor,
                                activeTrackColor = themeAccentColor.copy(alpha = 0.6f),
                                inactiveTrackColor = themeTextColor.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpeechWaveAnimation(accentColor: Color) {
    val transition = rememberInfiniteTransition()
    
    // Animate four bars to simulate real speech waveforms in the bottom bar
    val scale1 by transition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(450), repeatMode = RepeatMode.Reverse)
    )
    val scale2 by transition.animateFloat(
        initialValue = 0.4f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(animation = tween(300), repeatMode = RepeatMode.Reverse)
    )
    val scale3 by transition.animateFloat(
        initialValue = 0.1f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse)
    )

    Row(
        modifier = Modifier
            .height(14.dp)
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(width = 3.dp, height = (12 * scale1).dp).background(accentColor, RoundedCornerShape(2.dp)))
        Box(modifier = Modifier.size(width = 3.dp, height = (12 * scale2).dp).background(accentColor, RoundedCornerShape(2.dp)))
        Box(modifier = Modifier.size(width = 3.dp, height = (12 * scale3).dp).background(accentColor, RoundedCornerShape(2.dp)))
        Box(modifier = Modifier.size(width = 3.dp, height = (12 * scale2).dp).background(accentColor, RoundedCornerShape(2.dp)))
    }
}

@Composable
fun ReaderEmptyState(onNavigate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.LibraryBooks,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ningún fic abierto",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Para comenzar a leer con fuentes personalizables, abre cualquier fanfiction desde tu Biblioteca Offline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNavigate,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Ir a Biblioteca")
        }
    }
}

// Helpers for mapped custom themes
data class ReaderThemeColor(
    val bg: Color,
    val text: Color,
    val accent: Color,
    val highlight: Color,
    val highlightText: Color
)

fun getThemeColors(theme: ReaderTheme): ReaderThemeColor {
    return when (theme) {
        ReaderTheme.CREAM -> ReaderThemeColor(
            bg = Color(0xFFFBF7F0),
            text = Color(0xFF2B2118),
            accent = Color(0xFFB45309), // Warm amber accent
            highlight = Color(0xFFFDE047), // Soft highlight yellow
            highlightText = Color(0xFF1E293B)
        )
        ReaderTheme.SEPIA -> ReaderThemeColor(
            bg = Color(0xFFF4ECD8),
            text = Color(0xFF433017),
            accent = Color(0xFF8B4513), // Sepia brown
            highlight = Color(0xFFF59E0B), // Sepia gold highlight
            highlightText = Color(0xFFFFFBEB)
        )
        ReaderTheme.SLATE -> ReaderThemeColor(
            bg = Color(0xFF1E293B),
            text = Color(0xFFF1F5F9),
            accent = Color(0xFF38BDF8), // Ocean Sky accent
            highlight = Color(0xFF0284C7).copy(alpha = 0.6f),
            highlightText = Color(0xFFFFFFFF)
        )
        ReaderTheme.TWILIGHT -> ReaderThemeColor(
            bg = Color(0xFF110C24),
            text = Color(0xFFE2DDF0),
            accent = Color(0xFFA855F7), // Twilight violet
            highlight = Color(0xFFE9D5FF).copy(alpha = 0.35f),
            highlightText = Color(0xFFFDF4FF)
        )
        ReaderTheme.OLED -> ReaderThemeColor(
            bg = Color(0xFF000000),
            text = Color(0xFFE2E8F0),
            accent = Color(0xFF10B981), // OLED Green neon accent
            highlight = Color(0xFF065F46),
            highlightText = Color(0xFFD1FAE5)
        )
    }
}

// Map custom styles with fallback weights and lines settings
private fun getFontFamily(font: ReaderFont, isHeading: Boolean): TextStyle {
    return when (font) {
        ReaderFont.SANS -> TextStyle(fontFamily = FontFamily.SansSerif)
        ReaderFont.SERIF -> TextStyle(fontFamily = FontFamily.Serif)
        ReaderFont.MONO -> TextStyle(fontFamily = FontFamily.Monospace)
        ReaderFont.ROUNDED -> TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium
        )
        ReaderFont.DYSLEXIC -> TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun getFontLetterSpacing(font: ReaderFont): androidx.compose.ui.unit.TextUnit {
    return when (font) {
        ReaderFont.DYSLEXIC -> 1.8.sp // Highlighted readability gap
        ReaderFont.ROUNDED -> 0.6.sp
        else -> 0.2.sp
    }
}
