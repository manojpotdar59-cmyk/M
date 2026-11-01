package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jarvis_messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "user" or "jarvis"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val protocolType: String = "GENERAL"
)

@Entity(tableName = "jarvis_directives")
data class DirectiveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val details: String = "",
    val priority: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    val category: String = "TASK", // "SYSTEM", "SECURITY", "TASK", "PROTOCOL"
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Long-term user memories and facts stored by Jarvis in Room Database.
 * Allows Jarvis to personalize interactions, retain personal preferences, projects,
 * schedules, and notes across sessions.
 */
@Entity(tableName = "jarvis_user_memories")
data class UserMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String = "PREFERENCE", // "PREFERENCE", "PROJECT", "PEOPLE", "NOTE", "ROUTINE"
    val fact: String,
    val importance: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    val timestamp: Long = System.currentTimeMillis()
)
