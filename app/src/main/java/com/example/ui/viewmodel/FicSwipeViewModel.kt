package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.Chapter
import com.example.data.model.Story
import com.example.data.repository.CatalogItem
import com.example.data.repository.StoryRepository
import com.example.ui.tts.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class ReaderTheme(
    val displayName: String,
    val isDark: Boolean
) {
    CREAM("Cremoso", false),
    SEPIA("Sepia", false),
    SLATE("Pizarra", true),
    TWILIGHT("Crepúsculo", true),
    OLED("Negro Absoluto", true)
}

enum class ReaderFont(val displayName: String) {
    SANS("Sans Serif"),
    SERIF("Serif Clásica"),
    MONO("Monoespacio"),
    ROUNDED("Redondeada"),
    DYSLEXIC("Lectura Fácil")
}

class FicSwipeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StoryRepository
    private val ttsManager: TtsManager

    // Screen Tabs: "discover" (the Tinder swipe mode), "library" (offline fics), "reader" (immersive reading + tts), "browser" (integrated fusion web reader)
    private val _currentTab = MutableStateFlow("discover")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Integrated Web Navegador states
    private val _browserUrl = MutableStateFlow("https://www.fanfiction.net")
    val browserUrl: StateFlow<String> = _browserUrl.asStateFlow()

    // Offline stories list
    val offlineStories: StateFlow<List<Story>>

    // Tinder-swiping Discovery list
    private val _discoverQueue = MutableStateFlow<List<CatalogItem>>(emptyList())
    val discoverQueue: StateFlow<List<CatalogItem>> = _discoverQueue.asStateFlow()

    // For importing custom links
    var importUrl by mutableStateOf("")
    var isImporting by mutableStateOf(false)

    // Form states for manual pasting
    var manualTitle by mutableStateOf("")
    var manualAuthor by mutableStateOf("")
    var manualCategory by mutableStateOf("Anime")
    var manualContent by mutableStateOf("")
    var isShowingPasteForm by mutableStateOf(false)

    // Background Web Scraper States
    var isBackgroundScraping by mutableStateOf(false)
    var backgroundScrapingStatus by mutableStateOf("")
    var backgroundDownloadProgress by mutableStateOf(0f)

    // Reader Mode fields
    private val _activeStory = MutableStateFlow<Story?>(null)
    val activeStory: StateFlow<Story?> = _activeStory.asStateFlow()

    private val _activeChapters = MutableStateFlow<List<Chapter>>(emptyList())
    val activeChapters: StateFlow<List<Chapter>> = _activeChapters.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(1) // 1-based index
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    // Observable current chapter
    val currentChapter: StateFlow<Chapter?> = combine(
        _activeChapters,
        _currentChapterIndex
    ) { chapters, index ->
        chapters.find { it.chapterIndex == index }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Typography customization
    private val _readerTheme = MutableStateFlow(ReaderTheme.SEPIA)
    val readerTheme: StateFlow<ReaderTheme> = _readerTheme.asStateFlow()

    private val _readerFont = MutableStateFlow(ReaderFont.SERIF)
    val readerFont: StateFlow<ReaderFont> = _readerFont.asStateFlow()

    private val _fontSize = MutableStateFlow(18f) // in sp
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    private val _lineSpacing = MutableStateFlow(1.4f)
    val lineSpacing: StateFlow<Float> = _lineSpacing.asStateFlow()

    // TTS configurations
    private val _playbackSpeed = MutableStateFlow(1.3f) // Speed default slightly faster for fanfic audio specs
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _playbackPitch = MutableStateFlow(1.0f)
    val playbackPitch: StateFlow<Float> = _playbackPitch.asStateFlow()

    val isSpeaking: StateFlow<Boolean>

    private val _highlightStart = MutableStateFlow(-1)
    val highlightStart: StateFlow<Int> = _highlightStart.asStateFlow()

    private val _highlightEnd = MutableStateFlow(-1)
    val highlightEnd: StateFlow<Int> = _highlightEnd.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StoryRepository(database.storyDao())
        ttsManager = TtsManager(application)
        
        offlineStories = repository.allStories.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        isSpeaking = ttsManager.isSpeaking

        // Hydrate discover deck with repository presets
        _discoverQueue.value = repository.swipeCatalog
    }

    fun selectTab(tab: String) {
        _currentTab.value = tab
        if (tab != "reader" && tab != "browser") {
            // Stop TTS if navigating away from immersive reader AND background browser fusion completely
            ttsManager.stop()
            clearHighlight()
        }
    }

    fun setBrowserUrl(url: String) {
        _browserUrl.value = url
    }

    // --- Discover Swiping System ---
    fun swipeLeft() {
        val currentList = _discoverQueue.value
        if (currentList.isNotEmpty()) {
            // Drop top card
            _discoverQueue.value = currentList.drop(1)
            Toast.makeText(getApplication(), "Omitido ⏭️", Toast.LENGTH_SHORT).show()
        }
    }

    fun swipeRight(item: CatalogItem) {
        viewModelScope.launch {
            // Save to offline Room database
            val story = Story(
                id = item.id,
                title = item.title,
                author = item.author,
                synopsis = item.synopsis,
                category = item.category,
                wordCount = item.wordCount,
                chaptersCount = item.chaptersCount
            )
            val chapters = item.chapters.mapIndexed { index, content ->
                Chapter(
                    id = "${item.id}_${index + 1}",
                    storyId = item.id,
                    chapterIndex = index + 1,
                    title = "Capítulo ${index + 1}",
                    content = content
                )
            }
            repository.saveStoryAndChapters(story, chapters)
            
            // Highlight saved & cycle deck
            val currentList = _discoverQueue.value
            _discoverQueue.value = currentList.drop(1)

            Toast.makeText(
                getApplication(),
                "¡Descargado para Offline! 📥 ${item.title}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun resetDiscoverDeck() {
        _discoverQueue.value = repository.swipeCatalog
        Toast.makeText(getApplication(), "¡Catálogo de Descubrimiento reiniciado!", Toast.LENGTH_SHORT).show()
    }

    // --- Link Importing System ---
    private var scrapingWebView: WebView? = null
    private var scrapingStoryId = ""
    private var scrapingTotalChapters = 1
    private var scrapingCurrentIndex = 1
    private var scrapingStoryTitle = ""
    private var scrapingStoryAuthor = ""
    private var scrapingStorySynopsis = ""
    private var scrapingStoryCategory = "Extracción Web"
    private val scrapedChapters = mutableListOf<Chapter>()

    fun startBackgroundScraping(url: String) {
        val sPattern = Regex("/s/(\\d+)")
        val match = sPattern.find(url)
        val storyId = match?.groupValues?.get(1)
        if (storyId == null) {
            Toast.makeText(getApplication(), "Formato de enlace de FanFiction.net inválido", Toast.LENGTH_SHORT).show()
            return
        }

        scrapingStoryId = storyId
        scrapingTotalChapters = 1
        scrapingCurrentIndex = 1
        scrapingStoryTitle = ""
        scrapingStoryAuthor = ""
        scrapingStorySynopsis = ""
        scrapedChapters.clear()

        isBackgroundScraping = true
        backgroundScrapingStatus = "Iniciando descarga en segundo plano para el ID $storyId..."
        backgroundDownloadProgress = 0.05f

        Handler(Looper.getMainLooper()).post {
            try {
                if (scrapingWebView == null) {
                    scrapingWebView = WebView(getApplication())
                    scrapingWebView?.apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onChapterScraped(
                                title: String,
                                author: String,
                                synopsis: String,
                                chapterTitle: String,
                                content: String,
                                totalChapters: Int,
                                category: String
                            ) {
                                viewModelScope.launch {
                                    handleScrapedChapter(
                                        title,
                                        author,
                                        synopsis,
                                        chapterTitle,
                                        content,
                                        totalChapters,
                                        category
                                    )
                                }
                            }

                            @JavascriptInterface
                            fun onError(message: String) {
                                viewModelScope.launch {
                                    handleScrapeError(message)
                                }
                            }
                        }, "AndroidBackgroundScraper")
                    }
                }

                scrapingWebView?.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        injectBackgroundScraperJs()
                    }
                }

                // Load first chapter
                val firstChapterUrl = "https://www.fanfiction.net/s/$storyId/$scrapingCurrentIndex"
                scrapingWebView?.loadUrl(firstChapterUrl)
            } catch (e: Exception) {
                isBackgroundScraping = false
                backgroundScrapingStatus = "Error: " + e.message
                Toast.makeText(getApplication(), "No se pudo iniciar el extractor en segundo plano: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun injectBackgroundScraperJs() {
        val scraperJs = """
            (function() {
                try {
                    var title = "";
                    var author = "";
                    var synopsis = "";
                    var chapterTitle = "";
                    var content = "";
                    var category = "FanFiction";

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
                        
                        var synopsisEl = profileTop.querySelector(".xcontrast_txt.profile_top_synopsis") || profileTop.querySelector(".profile_top_synopsis");
                        if (synopsisEl) {
                            synopsis = synopsisEl.innerText;
                        }
                    }
                    
                    var pre_story_links = document.getElementById("pre_story_links") || document.querySelector("#pre_story_links");
                    if (pre_story_links) {
                        var aTags = pre_story_links.getElementsByTagName("a");
                        if (aTags.length > 0) {
                            category = aTags[aTags.length - 1].innerText;
                        }
                    }

                    if (!title) title = document.title;
                    if (!author) author = "Autor FanFiction";
                    if (!synopsis) synopsis = "Extraído automáticamente en segundo plano.";

                    var chapSelect = document.getElementById("chap_select");
                    var totalChapters = 1;
                    if (chapSelect) {
                        totalChapters = chapSelect.options.length;
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
                            if (sel) {
                                totalChapters = sel.options.length;
                                if (sel.selectedIndex >= 0) {
                                    chapterTitle = sel.options[sel.selectedIndex].text;
                                }
                            }
                        }
                    }
                    if (!chapterTitle) {
                        chapterTitle = "Capítulo " + $scrapingCurrentIndex;
                    }

                    var storyDiv = document.getElementById("storytext");
                    if (storyDiv) {
                        content = storyDiv.innerText;
                    } else {
                        var alt = document.querySelector(".storytextp") || document.querySelector(".storytext") || document.querySelector("#storycontent");
                        if (alt) content = alt.innerText;
                    }

                    if (!content || content.trim().length === 0) {
                        window.AndroidBackgroundScraper.onError("No se pudo extraer texto del capítulo. Podría haber un CAPTCHA.");
                    } else {
                        window.AndroidBackgroundScraper.onChapterScraped(title, author, synopsis, chapterTitle, content, totalChapters, category);
                    }
                } catch(err) {
                    window.AndroidBackgroundScraper.onError(err.message);
                }
            })()
        """.trimIndent()
        scrapingWebView?.evaluateJavascript("javascript:$scraperJs", null)
    }

    private fun handleScrapedChapter(
        title: String,
        author: String,
        synopsis: String,
        chapterTitle: String,
        content: String,
        totalChapters: Int,
        category: String
    ) {
        if (scrapingStoryTitle.isEmpty()) scrapingStoryTitle = title.trim()
        if (scrapingStoryAuthor.isEmpty()) scrapingStoryAuthor = author.trim()
        if (scrapingStorySynopsis.isEmpty()) scrapingStorySynopsis = synopsis.trim()
        scrapingStoryCategory = category.trim()
        scrapingTotalChapters = if (totalChapters > 0) totalChapters else 1

        val newChapter = Chapter(
            id = "${scrapingStoryId}_$scrapingCurrentIndex",
            storyId = scrapingStoryId,
            chapterIndex = scrapingCurrentIndex,
            title = chapterTitle.trim(),
            content = content.trim()
        )
        scrapedChapters.add(newChapter)

        backgroundDownloadProgress = (scrapingCurrentIndex.toFloat() / scrapingTotalChapters.toFloat()).coerceIn(0f, 1f)
        backgroundScrapingStatus = "Descargado Capítulo $scrapingCurrentIndex de $scrapingTotalChapters: ${chapterTitle.trim()}"

        if (scrapingCurrentIndex < scrapingTotalChapters) {
            // Load next chapter
            scrapingCurrentIndex++
            Handler(Looper.getMainLooper()).post {
                val nextUrl = "https://www.fanfiction.net/s/$scrapingStoryId/$scrapingCurrentIndex"
                scrapingWebView?.loadUrl(nextUrl)
            }
        } else {
            // Scraped all chapters successfully! Save to DB
            viewModelScope.launch {
                val totalWords = scrapedChapters.sumOf { it.content.split("\\s+".toRegex()).size }
                val story = Story(
                    id = scrapingStoryId,
                    title = scrapingStoryTitle.ifEmpty { "Fanfic Extraído ($scrapingStoryId)" },
                    author = scrapingStoryAuthor.ifEmpty { "Autor de FanFiction.net" },
                    synopsis = scrapingStorySynopsis.ifEmpty { "Fanfic extraído dinámicamente desde el Navegador Fusión offline." },
                    category = scrapingStoryCategory,
                    wordCount = totalWords,
                    chaptersCount = scrapingTotalChapters
                )

                repository.saveStoryAndChapters(story, scrapedChapters)
                isBackgroundScraping = false
                backgroundScrapingStatus = ""
                Toast.makeText(getApplication(), "¡Descarga en Segundo Plano Completada! 🎧 Guardado: ${story.title}", Toast.LENGTH_LONG).show()
                openStoryInReader(story)
            }
        }
    }

    private fun handleScrapeError(message: String) {
        isBackgroundScraping = false
        backgroundScrapingStatus = ""
        Toast.makeText(
            getApplication(),
            "Error en descarga en segundo plano: $message. Prueba abriendo el link en el Navegador Fusión.",
            Toast.LENGTH_LONG
        ).show()
    }

    fun importStoryFromUrl() {
        val url = importUrl.trim()
        if (url.isEmpty()) {
            Toast.makeText(getApplication(), "Por favor, introduce una URL válida", Toast.LENGTH_SHORT).show()
            return
        }

        if (url.contains("fanfiction.net", ignoreCase = true)) {
            importUrl = ""
            startBackgroundScraping(url)
            return
        }

        isImporting = true
        viewModelScope.launch {
            try {
                // Simulate web fetching and robust format parsing
                val (story, chapters) = repository.simulateDownloadFromUrl(url)
                repository.saveStoryAndChapters(story, chapters)
                
                importUrl = ""
                isImporting = false
                
                Toast.makeText(
                    getApplication(),
                    "¡Fic importado con éxito offline! 🎉",
                    Toast.LENGTH_LONG
                ).show()
                
                // Open newly imported story in reader
                openStoryInReader(story)
            } catch (e: Exception) {
                isImporting = false
                Toast.makeText(getApplication(), "Error al importar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun saveScrapedChapterAndPlay(
        scrapedTitle: String,
        scrapedAuthor: String,
        scrapedChapterTitle: String,
        scrapedContent: String,
        storyId: String,
        chapterNum: Int
    ) {
        viewModelScope.launch {
            val content = scrapedContent.trim()
            if (content.isEmpty()) {
                Toast.makeText(getApplication(), "No se pudo extraer texto del capítulo.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Create or update Story
            val story = Story(
                id = storyId,
                title = scrapedTitle.trim().ifEmpty { "Fanfic Extraído ($storyId)" },
                author = scrapedAuthor.trim().ifEmpty { "Autor de FanFiction.net" },
                synopsis = "Fanfic extraído dinámicamente desde el Navegador Fusión offline.",
                category = "Extracción Web",
                wordCount = content.split("\\s+".toRegex()).size,
                chaptersCount = chapterNum
            )

            val newChapter = Chapter(
                id = "${storyId}_$chapterNum",
                storyId = storyId,
                chapterIndex = chapterNum,
                title = scrapedChapterTitle.trim().ifEmpty { "Capítulo $chapterNum" },
                content = content
            )

            // Save to SQLite offline DB
            repository.saveStoryAndChapters(story, listOf(newChapter))

            // Load active state
            _activeStory.value = story
            _currentChapterIndex.value = chapterNum

            // Get current chapters from Db and notify flow
            try {
                val chapters = repository.getChaptersForStory(story.id).first()
                val resolvedChapters = if (chapters.isEmpty()) listOf(newChapter) else chapters
                _activeChapters.value = resolvedChapters
                
                speakActiveChapter()
                Toast.makeText(getApplication(), "¡Audio generado y guardado! 🎧 Narrando: ${newChapter.title}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                _activeChapters.value = listOf(newChapter)
                speakActiveChapter()
            }
        }
    }

    // --- Custom Manual Paste Importer ---
    fun importManualStory() {
        val title = manualTitle.trim()
        val author = manualAuthor.trim()
        val content = manualContent.trim()
        val category = manualCategory.trim()

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(getApplication(), "El título y contenido no pueden estar vacíos", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            val storyId = "manual_" + System.currentTimeMillis()
            
            // Separate chapters by looking for typical markers like "Chapter" or "Capítulo", or split evenly if immense
            val chaptersList = mutableListOf<Chapter>()
            val chapterSeparators = listOf("Chapter", "CHAPTER", "Capítulo", "CAPÍTULO", "capítulo")
            
            val hasExplicitChapters = chapterSeparators.any { separator -> content.contains(separator) }
            
            if (hasExplicitChapters) {
                // Approximate division based on chapter headers
                val contentSpliterator = Regex("(?=(?:Chapter|CHAPTER|Capítulo|CAPÍTULO)\\s+\\d+)")
                val rawChapters = content.split(contentSpliterator).filter { it.isNotBlank() }
                rawChapters.forEachIndexed { idx, chContent ->
                    val index = idx + 1
                    chaptersList.add(
                        Chapter(
                            id = "${storyId}_$index",
                            storyId = storyId,
                            chapterIndex = index,
                            title = "Capítulo $index",
                            content = chContent.trim()
                        )
                    )
                }
            } else {
                // Single huge chapter
                chaptersList.add(
                    Chapter(
                        id = "${storyId}_1",
                        storyId = storyId,
                        chapterIndex = 1,
                        title = "Capítulo Único",
                        content = content
                    )
                )
            }

            val story = Story(
                id = storyId,
                title = title,
                author = if (author.isEmpty()) "Autor Anónimo" else author,
                synopsis = "Fanfic copiado manualmente de fanfiction.net. Guardado para leer y escuchar en modo offline.",
                category = category,
                wordCount = content.split("\\s+".toRegex()).size,
                chaptersCount = chaptersList.size
            )

            repository.saveStoryAndChapters(story, chaptersList)
            
            // Clean inputs
            manualTitle = ""
            manualAuthor = ""
            manualContent = ""
            isShowingPasteForm = false

            Toast.makeText(getApplication(), "¡Fanfic Copiado Guardado! 📥", Toast.LENGTH_LONG).show()
            openStoryInReader(story)
        }
    }

    // --- Reader Navigation ---
    fun openStoryInReader(story: Story) {
        _activeStory.value = story
        _currentChapterIndex.value = 1
        clearHighlight()
        
        viewModelScope.launch {
            repository.getChaptersForStory(story.id).collect { chapters ->
                _activeChapters.value = chapters
                selectTab("reader")
            }
        }
    }

    fun deleteStory(story: Story) {
        viewModelScope.launch {
            if (_activeStory.value?.id == story.id) {
                ttsManager.stop()
                _activeStory.value = null
                _activeChapters.value = emptyList()
                selectTab("library")
            }
            repository.deleteStory(story.id)
            Toast.makeText(getApplication(), "Lector: Eliminado offline: ${story.title}", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Immersive Swipe Navigation for Reader Mode (Capítulos) ---
    fun nextChapter() {
        val total = _activeChapters.value.size
        val current = _currentChapterIndex.value
        if (current < total) {
            _currentChapterIndex.value = current + 1
            clearHighlight()
            // Reset speech immediately so it auto-narrates chapter text seamlessly
            if (ttsManager.isSpeaking.value) {
                speakActiveChapter()
            }
            Toast.makeText(getApplication(), "Capítulo Siguiente ➡️", Toast.LENGTH_SHORT).show()
        } else {
            ttsManager.stop()
            Toast.makeText(getApplication(), "Has alcanzado el desenlace final del fanfiction. 🎬", Toast.LENGTH_LONG).show()
        }
    }

    fun prevChapter() {
        val current = _currentChapterIndex.value
        if (current > 1) {
            _currentChapterIndex.value = current - 1
            clearHighlight()
            if (ttsManager.isSpeaking.value) {
                speakActiveChapter()
            }
            Toast.makeText(getApplication(), "⬅️ Capítulo Anterior", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(getApplication(), "Ya estás en el primer capítulo", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Reader Formatting Setters ---
    fun setTheme(theme: ReaderTheme) {
        _readerTheme.value = theme
    }

    fun setFont(font: ReaderFont) {
        _readerFont.value = font
    }

    fun changeFontSize(delta: Float) {
        val current = _fontSize.value
        val next = (current + delta).coerceIn(12f, 36f)
        _fontSize.value = next
    }

    fun setLineSpacing(spacing: Float) {
        _lineSpacing.value = spacing.coerceIn(1.1f, 2.5f)
    }

    // --- Text To Speech Control Deck ---
    fun toggleSpeech() {
        if (ttsManager.isSpeaking.value) {
            ttsManager.pause()
            clearHighlight()
        } else {
            speakActiveChapter()
        }
    }

    fun speakActiveChapter() {
        val chapter = currentChapter.value ?: return
        val rawText = chapter.content
        
        // Exclude headers or metadata from body reads and trigger speaker
        val textToSpeak = "${chapter.title}. ${rawText}"
        
        ttsManager.speak(
            text = textToSpeak,
            speed = _playbackSpeed.value,
            pitch = _playbackPitch.value,
            onWordHighlight = { start, end ->
                // Account for the length of "${chapter.title}. "
                val prefixOffset = chapter.title.length + 2
                if (start >= prefixOffset) {
                    _highlightStart.value = start - prefixOffset
                    _highlightEnd.value = end - prefixOffset
                } else {
                    _highlightStart.value = -1
                    _highlightEnd.value = -1
                }
            },
            onFinished = {
                clearHighlight()
                // Auto advances onto next chapter on finish!
                viewModelScope.launch {
                    val current = _currentChapterIndex.value
                    val total = _activeChapters.value.size
                    if (current < total) {
                        nextChapter()
                    } else {
                        Log.i("FicSwipeViewModel", "Story completely read via TTS.")
                    }
                }
            }
        )
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed.coerceIn(0.5f, 3.0f)
        if (ttsManager.isSpeaking.value) {
            // Re-trigger narration to immediately pick up speed multiplier change
            speakActiveChapter()
        }
    }

    fun setPitch(pitch: Float) {
        _playbackPitch.value = pitch.coerceIn(0.5f, 2.0f)
        if (ttsManager.isSpeaking.value) {
            speakActiveChapter()
        }
    }

    fun speakFromParagraph(offset: Int) {
        val chapter = currentChapter.value ?: return
        val prefixOffset = chapter.title.length + 2
        // Calculate original textToSpeak equivalent index
        val targetAbsoluteOffset = offset + prefixOffset
        ttsManager.seekToOffset(targetAbsoluteOffset)
    }

    private fun clearHighlight() {
        _highlightStart.value = -1
        _highlightEnd.value = -1
    }

    override fun onCleared() {
        ttsManager.shutdown()
        super.onCleared()
    }
}
