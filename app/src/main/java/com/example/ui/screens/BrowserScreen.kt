package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.viewmodel.FicSwipeViewModel
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    viewModel: FicSwipeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    val browserUrl by viewModel.browserUrl.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val activeStory by viewModel.activeStory.collectAsState()

    var textInputUrl by remember(browserUrl) { mutableStateOf(browserUrl) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var pageLoadingProgress by remember { mutableStateOf(100) }
    var isStoryPage by remember { mutableStateOf(false) }

    // Intercept standard fanfiction story patterns
    fun checkIsStoryPage(url: String?) {
        isStoryPage = url != null && url.contains("/s/") && url.contains("fanfiction.net", ignoreCase = true)
    }

    // Parse the storyId and chapter number from a given Fanfiction url
    fun parseUrlMeta(url: String): Pair<String, Int> {
        return try {
            val sPattern = Regex("/s/(\\d+)")
            val chPattern = Regex("/s/\\d+/(\\d+)")
            val storyId = sPattern.find(url)?.groupValues?.get(1) ?: "temp_${System.currentTimeMillis()}"
            val chIndex = chPattern.find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 1
            Pair(storyId, chIndex)
        } catch (e: Exception) {
            Pair("temp_${System.currentTimeMillis()}", 1)
        }
    }

    // Evaluates scraping Javascript and sends resulting data back via JS Interface
    fun triggerTtsExtraction() {
        val currentUrl = webViewInstance?.url ?: return
        if (!currentUrl.contains("fanfiction.net", ignoreCase = true)) {
            Toast.makeText(context, "Esta función solo opera en enlaces de FanFiction.net", Toast.LENGTH_SHORT).show()
            return
        }

        val (storyId, chapterNum) = parseUrlMeta(currentUrl)

        val scraperJs = """
            (function() {
                var title = "";
                var author = "";
                var chapterTitle = "";
                var content = "";

                try {
                    var profileTop = document.getElementById("profile_top") || document.querySelector(".storytop") || document.querySelector("#profile_top");
                    if (profileTop) {
                        var bTags = profileTop.getElementsByTagName("b");
                        if (bTags.length > 0) title = bTags[0].innerText;

                        var aTags = profileTop.getElementsByTagName("a");
                        for (var i = 0; i < aTags.length; i++) {
                            if (aTags[i].href && aTags[i].href.indexOf("/u/") !== -1) {
                                author = aTags[i].innerText;
                                break;
                            }
                        }
                    }
                    if (!title) {
                        var tEl = document.querySelector(".ititle") || document.querySelector("#profile_top b") || document.querySelector("h9 b");
                        if (tEl) title = tEl.innerText;
                    }
                    if (!author) {
                        var aEl = document.querySelector("a[href^='/u/']") || document.querySelector("#profile_top a");
                        if (aEl) author = aEl.innerText;
                    }

                    if (!title) title = document.title;
                    if (!author) author = "Autor FanFiction";

                    var chapSelect = document.getElementById("chap_select");
                    if (chapSelect) {
                        chapterTitle = chapSelect.options[chapSelect.selectedIndex].text;
                    } else {
                        var selects = document.getElementsByTagName("select");
                        if (selects.length > 0) {
                            var sel = null;
                            for (var s=0; s<selects.length; s++) {
                                if (selects[s].id === "chap_select" || selects[s].name === "chapter") {
                                    sel = selects[s];
                                    break;
                                }
                            }
                            if (sel && sel.selectedIndex >= 0) {
                                chapterTitle = sel.options[sel.selectedIndex].text;
                            }
                        }
                    }
                    if (!chapterTitle) {
                        chapterTitle = "Capítulo " + $chapterNum;
                    }

                    var storyDiv = document.getElementById("storytext");
                    if (storyDiv) {
                        content = storyDiv.innerText;
                    } else {
                        var alt = document.querySelector(".storytextp") || document.querySelector(".storytext") || document.querySelector("#storycontent");
                        if (alt) content = alt.innerText;
                    }
                } catch(err) {
                    content = "Error de extracción: " + err.message;
                }

                window.AndroidScraper.onContentExtracted(title, author, chapterTitle, content);
            })()
        """.trimIndent()

        Toast.makeText(context, "🪄 Analizando y convirtiendo texto a audio...", Toast.LENGTH_SHORT).show()
        webViewInstance?.evaluateJavascript("javascript:$scraperJs", null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Topic address bar with paste and control features
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            if (webViewInstance?.canGoBack() == true) {
                                webViewInstance?.goBack()
                            }
                        },
                        enabled = webViewInstance?.canGoBack() == true
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }

                    IconButton(
                        onClick = {
                            if (webViewInstance?.canGoForward() == true) {
                                webViewInstance?.goForward()
                            }
                        },
                        enabled = webViewInstance?.canGoForward() == true
                    ) {
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Siguiente")
                    }

                    IconButton(
                        onClick = {
                            webViewInstance?.reload()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Recargar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Fusión Web 🌐",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    // Clipboard Paste to trigger instant navigation
                    IconButton(
                        onClick = {
                            val clipboardText = clipboardManager.getText()?.text
                            if (!clipboardText.isNullOrBlank()) {
                                textInputUrl = clipboardText
                                viewModel.setBrowserUrl(clipboardText)
                                webViewInstance?.loadUrl(clipboardText)
                                Toast.makeText(context, "Enlace pegado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Pegar URL", tint = MaterialTheme.colorScheme.secondary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = textInputUrl,
                    onValueChange = { textInputUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://www.fanfiction.net/s/...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        var url = textInputUrl.trim()
                        if (url.isNotEmpty()) {
                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                url = "https://$url"
                            }
                            textInputUrl = url
                            viewModel.setBrowserUrl(url)
                            webViewInstance?.loadUrl(url)
                        }
                    }),
                    trailingIcon = {
                        if (textInputUrl.isNotEmpty()) {
                            IconButton(onClick = { textInputUrl = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    maxLines = 1,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Web Page Loading Progress indicator line
        if (pageLoadingProgress < 100) {
            LinearProgressIndicator(
                progress = pageLoadingProgress / 100f,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        }

        // WebView Holder
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (url != null) {
                                    textInputUrl = url
                                    viewModel.setBrowserUrl(url)
                                    checkIsStoryPage(url)
                                }
                            }

                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                if (url != null) {
                                    textInputUrl = url
                                    checkIsStoryPage(url)
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                pageLoadingProgress = newProgress
                            }
                        }

                        settings.apply {
                            javaScriptEnabled = true
                            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
                            supportZoom()
                            builtInZoomControls = true
                            displayZoomControls = false
                            domStorageEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }

                        // Add the Scraper javascript bridge interface
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onContentExtracted(title: String, author: String, chapterTitle: String, content: String) {
                                scope.launch {
                                    val (storyId, chapterIndex) = parseUrlMeta(url ?: "")
                                    viewModel.saveScrapedChapterAndPlay(
                                        scrapedTitle = title,
                                        scrapedAuthor = author,
                                        scrapedChapterTitle = chapterTitle,
                                        scrapedContent = content,
                                        storyId = storyId,
                                        chapterNum = chapterIndex
                                    )
                                }
                            }
                        }, "AndroidScraper")

                        webViewInstance = this
                        loadUrl(browserUrl)
                    }
                },
                update = { webView ->
                    // Navigate only if VM URL is changed from outside components
                    if (webView.url != browserUrl) {
                        webView.loadUrl(browserUrl)
                        checkIsStoryPage(browserUrl)
                    }
                }
            )

            // Dynamic Hover Conversion Banner when on a story page
            if (isStoryPage) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "¡Fic Detectado! Convierte esta página en Audio",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { triggerTtsExtraction() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Headphones, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Convertir Texto a Audio en Segundo Plano")
                            }
                        }
                    }
                }
            }
        }

        // Background Speak Controller Dashboard - displays persistent controls when audio is playing
        if (isSpeaking || activeStory != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Estado De Audio",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = activeStory?.title ?: "Narración en Segundo Plano",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = if (isSpeaking) "Reproduciendo Audio..." else "En pausa ⏸️",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { viewModel.toggleSpeech() }) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isSpeaking) "Pausar" else "Reproducir",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = { viewModel.selectTab("reader") }) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = "Abrir Formato Completo",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}
