package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room @Entity class storing user interactions and memory logs.
 */
@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
