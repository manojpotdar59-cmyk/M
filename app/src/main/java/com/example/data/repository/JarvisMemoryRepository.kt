package com.example.data.repository

import com.example.data.local.DirectiveEntity
import com.example.data.local.JarvisDao
import com.example.data.local.Memory
import com.example.data.local.MessageEntity
import com.example.data.local.UserMemoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository pattern implementation for Jarvis local persistence,
 * abstracting data access for conversation messages, directives, and user memories,
 * delegating interaction memory access to MemoryRepository.
 */
class JarvisMemoryRepository(
    private val dao: JarvisDao,
    private val memoryRepository: MemoryRepository
) {

    // --- Message Streams ---
    val allMessages: Flow<List<MessageEntity>> = dao.getAllMessages()

    suspend fun saveMessage(sender: String, content: String, protocol: String = "GENERAL"): Long {
        return dao.insertMessage(
            MessageEntity(
                sender = sender,
                content = content,
                protocolType = protocol
            )
        )
    }

    suspend fun clearMessages() {
        dao.clearHistory()
    }

    // --- Directive Streams ---
    val allDirectives: Flow<List<DirectiveEntity>> = dao.getAllDirectives()

    suspend fun insertDirective(directive: DirectiveEntity): Long {
        return dao.insertDirective(directive)
    }

    suspend fun updateDirective(directive: DirectiveEntity) {
        dao.updateDirective(directive)
    }

    suspend fun deleteDirective(directive: DirectiveEntity) {
        dao.deleteDirective(directive)
    }

    // --- User Memory Streams ---
    val allMemories: Flow<List<UserMemoryEntity>> = dao.getAllUserMemories()

    fun searchMemories(query: String): Flow<List<UserMemoryEntity>> {
        return if (query.isBlank()) {
            dao.getAllUserMemories()
        } else {
            dao.searchUserMemories(query.trim())
        }
    }

    suspend fun getRecentMemoriesSnapshot(limit: Int = 15): List<UserMemoryEntity> {
        return dao.getRecentMemoriesSnapshot(limit)
    }

    suspend fun insertMemory(category: String, fact: String, importance: String = "MEDIUM"): Long {
        return dao.insertUserMemory(
            UserMemoryEntity(
                category = category,
                fact = fact.trim(),
                importance = importance
            )
        )
    }

    suspend fun updateMemory(memory: UserMemoryEntity) {
        dao.updateUserMemory(memory)
    }

    suspend fun deleteMemory(memory: UserMemoryEntity) {
        dao.deleteUserMemory(memory)
    }

    suspend fun clearAllMemories() {
        dao.clearAllUserMemories()
    }

    // --- Memory Entity Interactions via MemoryRepository ---
    val allInteractionMemories: Flow<List<Memory>> = memoryRepository.allMemories

    fun searchInteractionMemories(query: String): Flow<List<Memory>> {
        return memoryRepository.searchMemories(query)
    }

    suspend fun recordInteractionMemory(content: String): Long {
        return memoryRepository.recordInteraction(content)
    }

    suspend fun deleteInteractionMemory(memory: Memory) {
        memoryRepository.delete(memory)
    }

    suspend fun clearInteractionMemories() {
        memoryRepository.clearAll()
    }
}
