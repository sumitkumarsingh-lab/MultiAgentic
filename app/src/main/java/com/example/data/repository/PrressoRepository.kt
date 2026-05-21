package com.example.data.repository

import com.example.data.dao.PrressoDao
import com.example.data.model.CrmStateEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ApiLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class PrressoRepository(private val prressoDao: PrressoDao) {

    val crmState: Flow<CrmStateEntity?> = prressoDao.getCrmStateFlow()
    val allMessages: Flow<List<ChatMessageEntity>> = prressoDao.getAllMessagesFlow()
    val allLogs: Flow<List<ApiLogEntity>> = prressoDao.getAllLogsFlow()

    suspend fun getCrmState(): CrmStateEntity {
        return prressoDao.getCrmState() ?: CrmStateEntity().also {
            prressoDao.insertCrmState(it)
        }
    }

    suspend fun updateCrmState(state: CrmStateEntity) {
        prressoDao.updateCrmState(state)
    }

    suspend fun insertMessage(message: ChatMessageEntity) {
        prressoDao.insertMessage(message)
    }

    suspend fun clearChat() {
        prressoDao.clearChatMessages()
    }

    suspend fun insertLog(type: String, message: String) {
        prressoDao.insertLog(ApiLogEntity(type = type, message = message))
    }

    suspend fun clearLogs() {
        prressoDao.clearLogs()
        // Re-inject core initialization log
        prressoDao.insertLog(ApiLogEntity(
            type = "system",
            message = "Authoritative System of Record (SoR) initialised. Live CRM loaded."
        ))
    }

    // --- SECURE BACKEND WRAPPERS (Mirrors Python Code) ---

    suspend fun apiQueryCurrentUsage(): String {
        val state = getCrmState()
        val response = "Customer Tier: ${state.tier} | Usage: ${state.bandwidthUsedGb}GB used out of ${state.bandwidthLimitGb}GB limit."
        insertLog("query_usage", "[Backend API Execution]: Querying LIVE_CRM_DATABASE -> $response")
        return response
    }

    suspend fun apiApplyPlatinumTopup(): String {
        val state = getCrmState()
        val newLimit = state.bandwidthLimitGb + 50
        val newTopups = state.pendingTopups + 1
        val newState = state.copy(bandwidthLimitGb = newLimit, pendingTopups = newTopups)
        
        updateCrmState(newState)
        
        val response = "Success! Added 50GB. System of Record updated. New absolute data allotment limit is now ${newLimit}GB. Current status: ${state.bandwidthUsedGb}GB used out of ${newLimit}GB total limit."
        
        insertLog("apply_topup", "[Backend API Execution]: Mutating state in LIVE_CRM_DATABASE... Saved: +50GB Data Loyalty Bonus. Limit changed: ${state.bandwidthLimitGb}GB -> ${newLimit}GB.")
        return response
    }
}
