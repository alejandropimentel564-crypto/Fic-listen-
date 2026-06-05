package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stories")
data class Story(
    @PrimaryKey val id: String, // ID of the fanfic (e.g. "1391234")
    val title: String,
    val author: String,
    val synopsis: String,
    val category: String, // e.g. "Harry Potter", "Naruto", "Anime", "Twilight"
    val wordCount: Int,
    val chaptersCount: Int,
    val isDownloaded: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)
