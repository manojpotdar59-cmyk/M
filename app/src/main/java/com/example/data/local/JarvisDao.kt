package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JarvisDao {
    // --- Messages (Short-term / Dialogue Memory) ---
    @Query("SELECT * FROM jarvis_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("DELETE FROM jarvis_messages")
    suspend fun clearHistory()

    // --- Directives (Action / Task Protocols) ---
    @Query("SELECT * FROM jarvis_directives ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllDirectives(): Flow<List<DirectiveEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirective(directive: DirectiveEntity): Long

    @Update
    suspend fun updateDirective(directive: DirectiveEntity)

    @Delete
    suspend fun deleteDirective(directive: DirectiveEntity)

    // --- User Long-Term Memories & Facts ---
    @Query("SELECT * FROM jarvis_user_memories ORDER BY timestamp DESC")
    fun getAllUserMemories(): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM jarvis_user_memories WHERE fact LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchUserMemories(query: String): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM jarvis_user_memories ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMemoriesSnapshot(limit: Int): List<UserMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserMemory(memory: UserMemoryEntity): Long

    @Update
    suspend fun updateUserMemory(memory: UserMemoryEntity)

    @Delete
    suspend fun deleteUserMemory(memory: UserMemoryEntity)

    @Query("DELETE FROM jarvis_user_memories")
    suspend fun clearAllUserMemories()

    // --- User Interactions (Memory Entity) ---
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<Memory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: Memory): Long

    @Delete
    suspend fun deleteMemory(memory: Memory)

    @Query("DELETE FROM memories")
    suspend fun clearMemories()
}
