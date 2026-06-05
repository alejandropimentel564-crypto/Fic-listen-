package com.example.data.repository

import com.example.data.database.StoryDao
import com.example.data.model.Story
import com.example.data.model.Chapter
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

class StoryRepository(private val storyDao: StoryDao) {

    val allStories: Flow<List<Story>> = storyDao.getAllStories()

    fun getChaptersForStory(storyId: String): Flow<List<Chapter>> = 
        storyDao.getChaptersForStory(storyId)

    suspend fun getChapter(storyId: String, chapterIndex: Int): Chapter? =
        storyDao.getChapter(storyId, chapterIndex)

    suspend fun saveStoryAndChapters(story: Story, chapters: List<Chapter>) {
        storyDao.insertStory(story)
        storyDao.insertChapters(chapters)
    }

    suspend fun updateChapter(chapter: Chapter) {
        storyDao.updateChapter(chapter)
    }

    suspend fun deleteStory(storyId: String) {
        storyDao.deleteStoryAndChapters(storyId)
    }

    // Curated catalog of classic fanfictions for current "Discovery swipe cards"
    val swipeCatalog = listOf(
        CatalogItem(
            id = "hp_silent_resonance",
            title = "Harry Potter and the Silent Resonance",
            author = "LadyEmerald88",
            category = "Harry Potter",
            synopsis = "During the quiet months at Privet Drive, Harry discovers an ancient, silent magic that alters his understanding of the Mind Arts. When Voldemort learns of his new gift, a deadly game of shadows begins in the Hogwarts library. Decades-old secrets will be parsed.",
            wordCount = 14500,
            chaptersCount = 3,
            chapters = listOf(
                "Chapter 1: The Whispers of Privet Drive\n\n" +
                    "The night in Surrey was unusually still. Harry Potter sat on his lumpy bed, staring out of the cracked window. Outside, the streetlights cast long, skeletal shadows across Privet Drive. He could hear Uncle Vernon’s rattling snores through the thin walls, but inside, Harry felt an oppressive, roaring silence.\n\n" +
                    "In his hands lay an old, dusty leather notebook he had found tucked under the loose floorboard in the attic. The pages were yellowed and brittle, but the ink was as jet-black as if it had been written yesterday. There were no magic runes or standard spells; instead, it spoke of 'resonance' - the acoustic connection of magical minds.\n\n" +
                    "He closed his eyes and began to breathe deeply, aligning his thoughts with the rhythmic humming of the old grandfather clock downstairs. The silence changed. It became heavier, carrying a distinct frequency. He was listening, not with his ears, but with his core.",
                "Chapter 2: Hogwarts Express Reunion\n\n" +
                    "The crimson locomotive chugged, spitting thick plumes of slate-colored smoke into the chilly autumn air. Hermione Granger was reading frantic notes on ancient arithmancy, while Ron Weasley was trying to piece together a squashed pumpkin pasty.\n\n" +
                    "Harry watched them, but his mind was somewhere else. He ran his fingers along his wand, feeling the faint, rhythmic pulse of resonance he had unlocked over the summer. It was like a miniature heartbeat, whispering secrets of silent magic.\n\n" +
                    "\"Are you alright, Harry?\" Hermione asked, peering over her heavy reading glasses. \"You've been incredibly quiet since we boarded.\" Harry shook his head and offered a reassuring smile. \"Just thinking about the year ahead, Hermione. Resonance is real.\"",
                "Chapter 3: The Library Shadows\n\n" +
                    "The Restricted Section of the Hogwarts library was icy during midnight. Harry walked silently down the mahogany aisles, his footsteps muffled by ancient silent spells. Madam Pince was long asleep, but the books themselves seemed to rustle in their leather bindings.\n\n" +
                    "He stopped at the shelf dedicated to the 'Mind Resonance and Legilimens Keys.' The book described on Privet Drive had a sister volume here. Placing his warm palm on the wooden shelf, he closed his eyes and sent a soft pulse of magical resonance. The shelf vibrated back. A single, dark velvet book slid forward, offering its mysteries."
            )
        ),
        CatalogItem(
            id = "naruto_shadow_light",
            title = "Naruto: Shadow and Light",
            author = "KyuubiWriter99",
            category = "Naruto",
            synopsis = "What if Naruto was trained in the covert Root division, but retained his bright orange stubbornness? An exploration of an operative who struggles to balance the absolute loyalty of shadows with the vibrant friendship of the Leaf. Will the Kyuubi's power serve the light?",
            wordCount = 18200,
            chaptersCount = 3,
            chapters = emptyList() // populated in init dynamically below
        ),
        CatalogItem(
            id = "pjo_demigod_rewrite",
            title = "The Demigod’s Rewrite",
            author = "SeaweedBrain_Extra",
            category = "Percy Jackson",
            synopsis = "After the war with Gaea, Percy Jackson is dragged by a temporal anomaly back to his twelve-year-old self. Armed with the memories of a hero, he must traverse the original quests again. But Tartarus's shadows are already awake, and they remember him.",
            wordCount = 22100,
            chaptersCount = 3,
            chapters = emptyList()
        ),
        CatalogItem(
            id = "twilight_eternal_crimson",
            title = "Twilight: Eternal Crimson",
            author = "Forkswriter",
            category = "Twilight",
            synopsis = "An alternative where Bella has a highly sensitive, kinetic defense shield that activates whenever a vampire draws near. When Edward encounters her on his first day, the clash of their gifts generates a kinetic explosion that changes Forks high school forever.",
            wordCount = 12900,
            chaptersCount = 3,
            chapters = emptyList()
        )
    )

    init {
        // Initialize rich chapters for simulated data
        val narutoChapters = listOf(
            "Chapter 1: The Shadow Anomaly\n\n" +
                "The training grounds of the Root division were dug deep beneath the busy streets of Konohagakure. It was empty of sunlight, empty of laughter, and filled with dark shadows. Naruto, wearing a blank porcelain mask, stood motionless like a stone pillar.\n\n" +
                "\"Remember, child,\" Danzo's rasping voice echoed in the cavern. \"An operative has no name, no past, and no emotional attachments. You are the root that holds the tree afloat under the soil.\"\n\n" +
                "Naruto stared back with dull, sapphire eyes. In the deep cavities of his mind, the giant Nine-Tailed Fox began to rumble with amusement. *Root of Konoha? Ridiculous! You belong to the grand canopy!* Naruto smiled silently. Even in the deepest shadows, light was bound to slip in.",
            "Chapter 2: Team 7 Assignment\n\n" +
                "The Third Hokage sat behind his heavy desk, puffing on his pipe. He looked at the orange jumpsuit Naruto had insisted on wearing, ignoring Danzo's protestations. \"You are to join Team 7, under Kakashi Hatake,\" Sarutobi said warmly.\n\n" +
                "Naruto felt a rush of warmth. He would finally meet Sasuke Uchiha and Sakura Haruno in a normal academy setting. Kakashi looked up from his orange booklet, his single visible eye curving into an amused squint. \"Well, you seem like an interesting bunch,\" Kakashi muttered, reading Naruto's files. \"Let's see what shadows you hide.\"",
            "Chapter 3: The Bridge of Waves\n\n" +
                "The iron bridge was shrouded in a dense, misty fog during their mission to the Land of Waves. Zabuza, the demon of the mist, smirked as he appeared from the gloom, his giant Cleaver reflecting the pale sky. Sasuke stood shivering, his chakra core trembling.\n\n" +
                "Naruto stepped forward, clicking his shadow guards privately. \"Don't worry, Sasuke,\" Naruto whispered, a golden aura beginning to filter out of his body. \"The shadows are my home. I will clear this fog for good!\" With a swift hand-seal, the misty bridge exploded into a flurry of light."
        )
        
        val pjoChapters = listOf(
            "Chapter 1: The Cabin of Memories\n\n" +
                "Percy Jackson woke up with a gasp, expecting the burning claws of Tartarus or the roaring wind of Gaea. Instead, he smelled sea salt and old wood. He looked down and gasped - his hands were tiny, skin dotted with freckles, lacking the epic battle scars of his future.\n\n" +
                "He was back in Cabin 11 at Camp Half-Blood. It was his first summer. The sleeping bags of the Hermes cabin turned around him, filled with snores of young demigods.\n\n" +
                "\"Percy? Are you alright?\" Luke Castellan's voice came from the darkness near the doorway. Percy froze. Looking at Luke, alive and whole, made a painful lump rise in his throat. \"Just a nightmare, Luke,\" Percy whispered, squeezing his small fists. \"Just a terrible nightmare.\"",
            "Chapter 2: Capture the Flag, Redux\n\n" +
                "The pine forest was damp with dew. Annabeth Chase was yelling instructions from the high tactical rock. Percy held his bronze shield, enjoying the familiar weight. He knew exactly what was about to happen - the Ares cabin would attack, and the hellhound would breach the border.\n\n" +
                "\"Get ready!\" Percy yelled to his teammates, running towards the creek. When Clarisse came splashing through the trees with her electric spear, Percy simply smiled. He stepped into the stream, and the water rose in a majestic torrent to greet him, matching the memories of his legendary adult powers.",
            "Chapter 3: The Prophecy's Shadow\n\n" +
                "The attic of the Big House was hot and cluttered. The Oracle of Delphi, a mummified body in a colorful bohemian dress, sat resting. As Percy walked in, green smoke began to pour from its lips.\n\n" +
                "*You shall go west, and face the god who has turned...* The ancient prophecy erupted. Percy listened, but this time, he wasn't afraid. He knew exactly where the Lightning Bolt was hidden, and how to outsmart Ares. But as he turned to leave, the Oracle whispered a new, unrecorded line: *And the hero who rewrote his path shall pay with a darker bond...*"
        )

        val twilightChapters = listOf(
            "Chapter 1: The First Glimpse of Pale Forks\n\n" +
                "Forks was exactly as green and misty as Bella Swan remembered from her childhood visits, but the air felt charged. The moment she stepped into the school parking lot, a strange, electric static tickled her hands. It was a sensory shield she didn't know she possessed.\n\n" +
                "Then she saw them. The Cullens. They walked across the yard together, pale, beautiful, and utterly detached from the high school ecosystem.\n\n" +
                "Edward Cullen walked in the front, his bronze hair disheveled. As he walked past Bella, she felt her static shield surge. The air between them crackled violently. Edward stopped dead, his golden eyes wide with shock, sniffing the air as if a physical shockwave had struck his pale skin.",
            "Chapter 2: Biology Confrontation\n\n" +
                "The classroom smelled of formaldehyde and rain. Bella sat at the lab desk, heart hammering. Edward was already seated, his muscles tense like a coiled spring. There was a physical distance between them, but the static force field was roaring.\n\n" +
                "\"Hello,\" Edward said, his voice like velvet wind. \"I am Edward Cullen.\" As Bella shook his hand, an actual kinetic spark popped between their fingers. The teacher's glassware rattled on the nearby stand. Edward withdrew his hand, looking at Bella with a mix of terror and fascination. \"You are... different,\" he spoke.",
            "Chapter 3: The Forest Reveal\n\n" +
                "The Olympic forest was ancient and carpeted in deep green moss. Bella stood in a small meadow, facing Edward. He was standing in a beam of sunlight, skin sparkling like a thousand diamonds.\n\n" +
                "\"This is what I am,\" Edward whispered, looking down in shame. \"I am a predator.\" Bella took a step closer, letting her kinetic shield ripple. The force field deflected the cold wind around them. \"And this is what I am,\" she answered, as a protective sphere of shimmering blue energy enveloped her. \"We are both anomalies, Edward.\""
            )

        // Hydrate categories
        swipeCatalog[1].chapters = narutoChapters
        swipeCatalog[2].chapters = pjoChapters
        swipeCatalog[3].chapters = twilightChapters
    }

    // Fast simulator that creates an automatic offline story when a user inputs a URL.
    fun simulateDownloadFromUrl(url: String): Pair<Story, List<Chapter>> {
        val storyId = parseStoryId(url)
        val defaultTitle = "FicSwipe Offline Title $storyId"
        val universe = guessUniverseFromUrl(url)
        val shortCategory = when (universe) {
            "harrypotter" -> "Harry Potter"
            "naruto" -> "Naruto"
            "percy" -> "Percy Jackson"
            "twilight" -> "Twilight"
            else -> "Classic Anime"
        }

        val dummyStory = Story(
            id = storyId,
            title = "The Resonating Echoes (ID: $storyId)",
            author = "OfflineStoryteller",
            synopsis = "A custom-imported FanFiction.net story downloaded from: $url. Ready for fully gesture-driven reading and text-to-speech audio narration in offline comfort.",
            category = shortCategory,
            wordCount = 9400,
            chaptersCount = 4
        )

        val chs = listOf(
            Chapter(
                id = "${storyId}_1",
                storyId = storyId,
                chapterIndex = 1,
                title = "Chapter 1: The Unlocked Gateway",
                content = "The digital portal buzzed. You pasted a FanFiction link: $url, in the FicSwipe app. Immediately, the offline mechanism stored this chapter onto your device.\n\n" +
                    "This is Chapter 1. Here, you can experience customizable serif, monospace, and open-dyslexic font combinations. Drag the slider to expand the font sizes or change text spacing to fit your ocular comfort.\n\n" +
                    "Click the floating Play Audio widget to enable Text-To-Speech. Adjust the speed multiplier up to 3.0x to speed-read books under the bedsheets. Swiping left reveals the next stored chapter."
            ),
            Chapter(
                id = "${storyId}_2",
                storyId = storyId,
                chapterIndex = 2,
                title = "Chapter 2: The Sound of Whispers",
                content = "The second chapter of this fanfiction unfolds in a majestic library where old parchment books hover in mid-air, rustling their yellow pages.\n\n" +
                    "The wizard sat down and closed his eyes. The ambient atmosphere was serene, isolated, and completely offline. This represents the pinnacle of reading independence, with the TTS engine translating pixels into sound waves directly on your Android phone.\n\n" +
                    "Slide with your finger from the right edge to slide back to Chapter 1, or slide left to continue to Chapter 3."
            ),
            Chapter(
                id = "${storyId}_3",
                storyId = storyId,
                chapterIndex = 3,
                title = "Chapter 3: The Gathering Storm",
                content = "Dark twilight clouds accumulated over the high castle towers. Winds whistled, carrying the metallic scent of static electricity and coming storm.\n\n" +
                "The protagonist knew that their journey was nearing an offline resolution. Armed with custom fonts and smooth TTS narrative speed, they prepared to face the epic challenges awaiting them."
            ),
            Chapter(
                id = "${storyId}_4",
                storyId = storyId,
                chapterIndex = 4,
                title = "Chapter 4: The Final Page",
                content = "The storm calmed down, revealing a starlit violet sky. The adventure was finished, but the memories were securely cached in the local SQLite Room database, ready for a lifetime of offline listening."
            )
        )

        return Pair(dummyStory, chs)
    }

    private fun parseStoryId(url: String): String {
        // e.g. https://www.fanfiction.net/s/12345/1/Story-Title
        val pattern = Regex("/s/(\\d+)")
        val match = pattern.find(url)
        return match?.groupValues?.get(1) ?: Random.nextInt(100000, 999999).toString()
    }

    private fun guessUniverseFromUrl(url: String): String {
        val norm = url.lowercase()
        return when {
            norm.contains("potter") || norm.contains("harry") -> "harrypotter"
            norm.contains("naruto") || norm.contains("uzumaki") -> "naruto"
            norm.contains("percy") || norm.contains("demigod") -> "percy"
            norm.contains("twilight") || norm.contains("cullen") -> "twilight"
            else -> "generic"
        }
    }
}

data class CatalogItem(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val synopsis: String,
    val wordCount: Int,
    val chaptersCount: Int,
    var chapters: List<String> = emptyList()
)
