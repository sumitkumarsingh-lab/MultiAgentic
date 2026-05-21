package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.PrressoAiManager
import com.example.data.database.PrressoDatabase
import com.example.data.model.ApiLogEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CrmStateEntity
import com.example.data.repository.PrressoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PrressoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PrressoRepository
    private val aiManager: PrressoAiManager

    val crmState: StateFlow<CrmStateEntity?>
    val chatMessages: StateFlow<List<ChatMessageEntity>>
    val apiLogs: StateFlow<List<ApiLogEntity>>

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _currentAgent = MutableStateFlow("Triage Router")
    val currentAgent: StateFlow<String> = _currentAgent.asStateFlow()

    init {
        val database = PrressoDatabase.getDatabase(application, viewModelScope)
        val dao = database.prressoDao()
        repository = PrressoRepository(dao)
        aiManager = PrressoAiManager(repository)

        crmState = repository.crmState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        chatMessages = repository.allMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        apiLogs = repository.allLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Keep active agent state in sync
        viewModelScope.launch {
            repository.allMessages.collect {
                _currentAgent.value = aiManager.getActiveAgentName()
            }
        }

        // Pre-populate welcome message if empty
        viewModelScope.launch {
            val existing = repository.allMessages.firstOrNull() ?: emptyList()
            if (existing.isEmpty()) {
                repository.insertMessage(ChatMessageEntity(
                    role = "assistant",
                    agentName = "Triage Router",
                    content = "Greetings **Sumit Kumar**! I am the **Triage Router** for PRReSSO.\n\n" +
                            "I monitor live network indices. If you are experiencing slower speeds, connection drops, or want to check billing adjustments and premium loyalty rewards, describe your issue here!"
                ))
            }
        }
    }

    fun isApiKeyConfigured(): Boolean = aiManager.isApiKeyConfigured()

    fun sendMessage(text: String) {
        if (text.isBlank() || _isGenerating.value) return

        viewModelScope.launch {
            _isGenerating.value = true
            
            // 1. Insert user message in DB
            val userMsg = ChatMessageEntity(
                role = "user",
                agentName = _currentAgent.value,
                content = text
            )
            repository.insertMessage(userMsg)

            // 2. Process with AI Manager (Live Gemini or Simulator)
            try {
                aiManager.processChatResponse(text)
            } catch (e: Exception) {
                // Fail-safe manual fallback insert
                repository.insertMessage(ChatMessageEntity(
                    role = "assistant",
                    agentName = _currentAgent.value,
                    content = "Dynamic system error: ${e.localizedMessage}. Ready for manual self-care commands."
                ))
            } finally {
                _currentAgent.value = aiManager.getActiveAgentName()
                _isGenerating.value = false
            }
        }
    }

    fun manualQueryUsage() {
        viewModelScope.launch {
            _isGenerating.value = true
            repository.insertLog("system", "[Manual Action]: Customer Sumit triggered manual usage query diagnostic.")
            val response = repository.apiQueryCurrentUsage()
            repository.insertMessage(ChatMessageEntity(
                role = "assistant",
                agentName = "Technical Specialist",
                content = "Executing manual service network diagnostic:\n\n*${response}*"
            ))
            aiManager.setAgent("Technical Specialist")
            _currentAgent.value = "Technical Specialist"
            _isGenerating.value = false
        }
    }

    fun manualApplyTopup() {
        viewModelScope.launch {
            _isGenerating.value = true
            repository.insertLog("system", "[Manual Action]: Customer Sumit triggered manual topup reservation.")
            val response = repository.apiApplyPlatinumTopup()
            repository.insertMessage(ChatMessageEntity(
                role = "assistant",
                agentName = "Retention Specialist",
                content = "Applying manual premium loyalty bandwidth top-up:\n\n*${response}*"
            ))
            aiManager.setAgent("Retention Specialist")
            _currentAgent.value = "Retention Specialist"
            _isGenerating.value = false
        }
    }

    fun manualChangeAgent(agentName: String) {
        viewModelScope.launch {
            aiManager.setAgent(agentName)
            _currentAgent.value = agentName
            repository.insertLog("handoff", "[Handoff Matrix - Manual]: Customer manually matched stream context to $agentName.")
            repository.insertMessage(ChatMessageEntity(
                role = "assistant",
                agentName = agentName,
                content = "Agent Handoff Complete. Hello Sumit, how can I assist you as custom **$agentName**?"
            ))
        }
    }

    fun resetSession() {
        viewModelScope.launch {
            repository.clearChat()
            repository.clearLogs()
            // Reset CRM to baseline
            repository.updateCrmState(CrmStateEntity())
            aiManager.forceResetAgent()
            _currentAgent.value = "Triage Router"
            
            // Insert greeting message from Triage Router
            repository.insertMessage(ChatMessageEntity(
                role = "assistant",
                agentName = "Triage Router",
                content = "Orchestrated session reset complete. Authoritative CRM initialized to default platinum state.\n\nGreetings **Sumit Kumar**! I am the **Triage Router** for PRReSSO. Tap are ready for network checks or upgrade negotiations. Let me know how I can help!"
            ))
        }
    }
}
