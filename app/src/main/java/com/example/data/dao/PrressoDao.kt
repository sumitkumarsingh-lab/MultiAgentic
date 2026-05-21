package com.example.data.dao

import androidx.room.*
import com.example.data.model.CrmStateEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ApiLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrressoDao {

    // CRM State Queries
    @Query("SELECT * FROM crm_state WHERE id = 1 LIMIT 1")
    fun getCrmStateFlow(): Flow<CrmStateEntity?>

    @Query("SELECT * FROM crm_state WHERE id = 1 LIMIT 1")
    suspend fun getCrmState(): CrmStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrmState(state: CrmStateEntity)

    @Update
    suspend fun updateCrmState(state: CrmStateEntity)

    // Chat Queries
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatMessages()

    // API Logs
    @Query("SELECT * FROM api_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<ApiLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ApiLogEntity)

    @Query("DELETE FROM api_logs")
    suspend fun clearLogs()
}
