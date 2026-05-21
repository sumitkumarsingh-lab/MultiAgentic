package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "crm_state")
data class CrmStateEntity(
    @PrimaryKey val id: Int = 1, // Singleton row
    val customerName: String = "Sumit Kumar",
    val tier: String = "Platinum",
    val bandwidthUsedGb: Int = 142,
    val bandwidthLimitGb: Int = 150,
    val pendingTopups: Int = 0
) {
    val isThrottled: Boolean
        get() = (bandwidthUsedGb.toDouble() / bandwidthLimitGb.toDouble()) >= 0.90
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val role: String, // "user" or "assistant"
    val agentName: String, // "Triage Router", "Technical Specialist", "Retention Specialist"
    val content: String,
    val apiCalled: String? = null,
    val apiResponse: String? = null
)

@Entity(tableName = "api_logs")
data class ApiLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "query_usage", "apply_topup", "handoff"
    val message: String
)
