package com.example.data.repository

import com.example.data.local.Memory
import com.example.data.local.MemoryDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository class that abstracts data access logic for the Memory entity from the ViewModel.
 */
class MemoryRepository(private val memoryDao: MemoryDao) {

    /**
     * Flow emitting the list of all stored interaction memories in descending order of timestamp.
     */
    val allMemories: Flow<List<Memory>> = memoryDao.getAllMemories()

    /**
     * Search stored memories by content query.
     */
    fun searchMemories(query: String): Flow<List<Memory>> {
        return if (query.isBlank()) {
            memoryDao.getAllMemories()
        } else {
            memoryDao.searchMemories(query.trim())
        }
    }

    /**
     * Insert a new memory record into the database.
     */
    suspend fun insert(memory: Memory): Long {
        return memoryDao.insertMemory(memory)
    }

    /**
     * Convenience method to record a user interaction string as a Memory.
     */
    suspend fun recordInteraction(content: String): Long {
        return memoryDao.insertMemory(
            Memory(
                content = content.trim(),
                timestamp = System.currentTimeMillis()
            )
        )
    }

    /**
     * Delete a specific memory entity.
     */
    suspend fun delete(memory: Memory) {
        memoryDao.deleteMemory(memory)
    }

    /**
     * Delete a memory entity by its unique ID.
     */
    suspend fun deleteById(id: Long) {
        memoryDao.deleteMemoryById(id)
    }

    /**
     * Purge all stored interaction memories.
     */
    suspend fun clearAll() {
        memoryDao.clearMemories()
    }
}
