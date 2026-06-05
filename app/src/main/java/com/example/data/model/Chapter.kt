package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapters")
data class Chapter(
    @PrimaryKey val id: String, // Composite unique ID, e.g. "storyId_chapterIndex"
    val storyId: String,
    val chapterIndex: Int, // 1-based index (e.g. 1, 2, 3...)
    val title: String,
    val content: String,
    val lastReadCharPosition: Int = 0
)
