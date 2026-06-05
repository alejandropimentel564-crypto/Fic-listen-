package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.Story
import com.example.data.model.Chapter
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY addedAt DESC")
    fun getAllStories(): Flow<List<Story>>

    @Query("SELECT * FROM stories WHERE id = :storyId")
    suspend fun getStoryById(storyId: String): Story?

    @Query("SELECT * FROM chapters WHERE storyId = :storyId ORDER BY chapterIndex ASC")
    fun getChaptersForStory(storyId: String): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE storyId = :storyId AND chapterIndex = :chapterIndex LIMIT 1")
    suspend fun getChapter(storyId: String, chapterIndex: Int): Chapter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: Story)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<Chapter>)

    @Update
    suspend fun updateChapter(chapter: Chapter)

    @Query("DELETE FROM stories WHERE id = :storyId")
    suspend fun deleteStory(storyId: String)

    @Query("DELETE FROM chapters WHERE storyId = :storyId")
    suspend fun deleteChapters(storyId: String)

    @Transaction
    suspend fun deleteStoryAndChapters(storyId: String) {
        deleteStory(storyId)
        deleteChapters(storyId)
    }
}
