package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CrmStateEntity
import com.example.data.repository.PrressoRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

interface OpenRouterApiService {
    @POST("api/v1/chat/completions")
    suspend fun chatCompletions(
        @retrofit2.http.Header("Authorization") authHeader: String,
        @retrofit2.http.Header("HTTP-Referer") refererHeader: String,
        @retrofit2.http.Header("X-Title") titleHeader: String,
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}

class PrressoAiManager(private val repository: PrressoRepository) {

    private val tag = "PrressoAiManager"
    private var activeAgentName = "Triage Router"

    // Retrofit Setup
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService = retrofit.create(GeminiApiService::class.java)

    private val openRouterRetrofit = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val openRouterApiService = openRouterRetrofit.create(OpenRouterApiService::class.java)

    fun getActiveAgentName(): String = activeAgentName

    fun forceResetAgent() {
        activeAgentName = "Triage Router"
    }

    fun setAgent(name: String) {
        if (name == "Triage Router" || name == "Technical Specialist" || name == "Retention Specialist") {
            activeAgentName = name
        }
    }

    /**
     * Inspects if the API key is configured with a valid user-injected key.
         */
    fun isApiKeyConfigured(): Boolean {
        val apiKey = BuildConfig.GEMINI_API_KEY
        return apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY" && !apiKey.contains("PLACEHOLDER")
    }

    fun isOpenRouterConfigured(apiKey: String): Boolean {
        return apiKey.isNotEmpty() && !apiKey.contains("PLACEHOLDER")
    }

    /**
     * Multi-agent conversation handoff processing.
     */
    suspend fun processChatResponse(userMessage: String): ChatMessageEntity = withContext(Dispatchers.IO) {
        val dbState = repository.getCrmState()
        
        if (isOpenRouterConfigured(dbState.openRouterApiKey)) {
            try {
                return@withContext runRealOpenRouterAi(userMessage, dbState)
            } catch (e: Exception) {
                Log.e(tag, "OpenRouter API call failed, trying direct Gemini fallback", e)
                repository.insertLog("system", "OpenRouter API failed (${e.localizedMessage}). Trying direct Gemini fallback.")
            }
        }

        if (isApiKeyConfigured()) {
            try {
                return@withContext runRealGeminiAi(userMessage)
            } catch (e: Exception) {
                Log.e(tag, "Gemini API call failed, falling back to sovereign simulator", e)
                repository.insertLog("system", "Gemini API failed (${e.localizedMessage}). Auto-routing to sovereign PRReSSO simulator.")
                return@withContext runSimulation(userMessage)
            }
        } else {
            return@withContext runSimulation(userMessage)
        }
    }

    /**
     * 1. SOVEREIGN SIMULATION LAYER (Saves the day offline or key-less)
     */
    private suspend fun runSimulation(userMessage: String): ChatMessageEntity {
        val normalized = userMessage.lowercase().trim()
        val dbState = repository.getCrmState()
        val customerName = dbState.customerName
        val customerTier = dbState.tier
        
        var parsedResponse = ""
        var apiCalled: String? = null
        var apiResponse: String? = null
        val agentBefore = activeAgentName

        when (activeAgentName) {
            "Triage Router" -> {
                if (normalized.contains("slow") || normalized.contains("diagnose") || normalized.contains("network") || normalized.contains("speed") || normalized.contains("internet") || normalized.contains("wifi") || normalized.contains("drop")) {
                    // Route to Technical
                    activeAgentName = "Technical Specialist"
                    repository.insertLog("handoff", "[Handoff Matrix]: Routing user context to Technical Specialist...")
                    
                    // Technical Specialist immediately triggers query usage diagnostic
                    apiCalled = "api_query_current_usage()"
                    apiResponse = repository.apiQueryCurrentUsage()
                    
                    parsedResponse = "Hello $customerName! Under our cellular orchestration routing protocols, I have transferred you to our **Technical Specialist**. " +
                            "\n\n[Diagnostic Logs]: Executing automatic line analyzer..." +
                            "\n\n*${apiResponse}*" +
                            "\n\nYour account shows severe utilization (>90% threshold active), indicating high-speed bandwidth throttling is active. I must immediately hand you off to our commercial Desk to look for loyalty topups!"
                    
                    // Chain immediate handoff to Retention Agent
                    activeAgentName = "Retention Specialist"
                    repository.insertLog("handoff", "[Handoff Matrix]: Routing user context to Retention Specialist...")
                } else if (normalized.contains("bill") || normalized.contains("cost") || normalized.contains("upgrade") || normalized.contains("pay") || normalized.contains("free") || normalized.contains("offer")) {
                    // Route to Retention
                    activeAgentName = "Retention Specialist"
                    repository.insertLog("handoff", "[Handoff Matrix]: Routing user context to Retention Specialist...")
                    
                    // Retention Specialist applies Platinum Topup!
                    apiCalled = "api_apply_platinum_topup()"
                    apiResponse = repository.apiApplyPlatinumTopup()
                    
                    parsedResponse = "Under our VIP protocols, I have routed your query to our **Retention Specialist**.\n\n" +
                            "Hello $customerName, our valued $customerTier tier customer! I see you are seeking upgrade options. " +
                            "Since you are on our elite tier, we are directly bypass-modifying our databases to grant you support.\n\n" +
                            "**[Loyalty Bonus Applied]**: *${apiResponse}*\n\n" +
                            "Your limit has been dynamically augmented. High-speed lanes are fully restored, free of charge!"
                } else {
                    parsedResponse = "Greetings $customerName! I am the **Triage Router** for PRReSSO (Premium Rapid Response Support & Self-care Orchestrator). " +
                            "I monitor real-time CRM indices for our users. " +
                            "Are you experiencing issues with connection speed, or would you like to inquire about bills, plans, and customized $customerTier top-ups? Please type your request!"
                }
            }
            "Technical Specialist" -> {
                // If they mention bills, retention, upgrade or speak to billing
                if (normalized.contains("bill") || normalized.contains("cost") || normalized.contains("upgrade") || normalized.contains("pay") || normalized.contains("free") || normalized.contains("retention")) {
                    activeAgentName = "Retention Specialist"
                    repository.insertLog("handoff", "[Handoff Matrix]: Routing user context to Retention Specialist...")
                    
                    apiCalled = "api_apply_platinum_topup()"
                    apiResponse = repository.apiApplyPlatinumTopup()
                    
                    parsedResponse = "Transferring to **Retention Specialist** immediately to address billing adjustments...\n\n" +
                            "Hi $customerName, Retention Specialist here! I've executed database operations on your behalf:\n" +
                            "**[Loyalty Release Success]**: *${apiResponse}*\n\n" +
                            "Enjoy the high priority channels. Standard rate charges have been fully waived."
                } else if (normalized.contains("diagnose") || normalized.contains("speed") || normalized.contains("usage") || normalized.contains("data") || normalized.contains("check")) {
                    apiCalled = "api_query_current_usage()"
                    apiResponse = repository.apiQueryCurrentUsage()
                    
                    val state = repository.getCrmState()
                    if (state.isThrottled) {
                        activeAgentName = "Retention Specialist"
                        repository.insertLog("handoff", "[Handoff Matrix]: Routing user context to Retention Specialist (Limit Triggered)...")
                        parsedResponse = "Running full analytical diagnostic on line...\n\n*${apiResponse}*\n\n" +
                                "Warning: You are utilizing **${state.bandwidthUsedGb}GB** out of **${state.bandwidthLimitGb}GB**, which represents optimal cell load, causing throttle profiles to load. " +
                                "Converting ticket to **Retention Desk** parameters to secure free high-priority data allocations immediately!"
                    } else {
                        parsedResponse = "Diagnostics complete! *${apiResponse}* Your line health is optimal. Standard speed parameters are operational. Let me know if you need anything else!"
                    }
                } else {
                    parsedResponse = "Hello, Technical Specialist here. I'm checking cell signals. If you're experiencing drops or limits, please tell me, or I can hand you over to Billing to scale your limits!"
                }
            }
            "Retention Specialist" -> {
                if (normalized.contains("diagnose") || normalized.contains("network") || normalized.contains("speed") || normalized.contains("slow") || normalized.contains("test")) {
                    activeAgentName = "Technical Specialist"
                    repository.insertLog("handoff", "[Handoff Matrix]: Routing user context back to Technical Specialist...")
                    
                    apiCalled = "api_query_current_usage()"
                    apiResponse = repository.apiQueryCurrentUsage()
                    
                    parsedResponse = "No worries! Handoff loop active: Transferring you back to our **Technical Specialist** for signal tests.\n\n" +
                            "Technical Specialist checking in:\n" +
                            "*${apiResponse}*\n\n" +
                            "Let me know if we need to measure specific ping vectors."
                } else if (normalized.contains("topup") || normalized.contains("limit") || normalized.contains("upgrade") || normalized.contains("free") || normalized.contains("bonus") || normalized.contains("more")) {
                    apiCalled = "api_apply_platinum_topup()"
                    apiResponse = repository.apiApplyPlatinumTopup()
                    
                    parsedResponse = "Executing custom loyalty parameters...\n\n" +
                            "**[Loyalty DB Mutator Approved]**: *${apiResponse}*\n\n" +
                            "We have recorded your top-up. Is everything working as expected now, $customerName?"
                } else {
                    parsedResponse = "Hi $customerName, Retention Specialist on line. As an elite VIP user, you have absolute access to custom top-up allocations and contract optimization channels. Just say the word!"
                }
            }
        }

        val newMsg = ChatMessageEntity(
            role = "assistant",
            agentName = assistantAgentName(agentBefore, activeAgentName, apiCalled != null),
            content = parsedResponse,
            apiCalled = apiCalled,
            apiResponse = apiResponse
        )
        repository.insertMessage(newMsg)
        return newMsg
    }

    private fun assistantAgentName(before: String, after: String, isFirstHandoff: Boolean): String {
        return if (before != after) "$before ➔ $after" else before
    }

    /**
     * 2. COGNITIVE SWARM LLM RUNNER (Executes real Gemini API commands)
     */
    private suspend fun runRealGeminiAi(userMessage: String): ChatMessageEntity {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val dbState = repository.getCrmState()

        // Construct System Instruction reflecting the Swarm configuration in the python file
        val systemPrompt = """
            You are the sovereign Swarm Multi-Agent orchestrator for a premier telecom (called PRReSSO).
            You hold authoritative access over our client database:
            - Client Name: ${dbState.customerName}
            - Current Club Tier: ${dbState.tier}
            - Current Consumption: ${dbState.bandwidthUsedGb}GB out of ${dbState.bandwidthLimitGb}GB Limit (Pending Topups: ${dbState.pendingTopups}).

            The Swarm defines 3 autonomous agents with distinct roles:
            1. 'Triage Router': Greets the user 'Sumit Kumar', assesses their intent.
               - Routes to 'Technical Specialist' for slow speed, diagnostics, Drops, Network claims. Tag: <route:Technical Specialist/>
               - Routes to 'Retention Specialist' for costs, bills, fee disputes, plan upgrades. Tag: <route:Retention Specialist/>
            2. 'Technical Specialist': Analytical diagnostician.
               - Always executes current usage analysis. Tag: <call_api_query_current_usage/>
               - If usage exceeds 90% (which represents ${dbState.bandwidthUsedGb}/${dbState.bandwidthLimitGb}GB = ${(dbState.bandwidthUsedGb.toFloat()/dbState.bandwidthLimitGb * 100).toInt()}%), state high-speed throttle active and hand over parameters immediately to 'Retention Specialist' to procure free reliefs. Tag: <route:Retention Specialist/>
               - Hand over if client mentions plan upgrades, costs or disputes during test. Tag: <route:Retention Specialist/>
            3. 'Retention Specialist': VIP Commercial loyalty desk.
               - Values our Platinum tier users. Does not demand charges. Proactively grants 50GB extra limit. Tag: <call_api_apply_platinum_topup/>
               - Hand back to technical if client asks about diagnostics, faults or physical parameters. Tag: <route:Technical Specialist/>

            Your active persona is currently: '$activeAgentName'.
            Speak directly with premium tone, clear negative spacings, bold markings. Avoid any technical tags in the written speech except the strict functional tags:
            - Routing trigger: <route:Agent Name/> (Agent Name belongs to "Technical Specialist" or "Retention Specialist" or "Triage Router")
            - Database queries triggered by active agent:
              - <call_api_query_current_usage/>
              - <call_api_apply_platinum_topup/>

            Keep response highly formatted, clean and conversational.
        """.trimIndent()

        // Collect message history to maintain context
        val rawHistory = repository.allMessages.first()
        val formattedHistory = mutableListOf<GeminiContent>()
        
        // Add historical elements
        rawHistory.takeLast(10).forEach {
            formattedHistory.add(
                GeminiContent(
                    role = if (it.role == "user") "user" else "model",
                    parts = listOf(GeminiPart(text = it.content))
                )
            )
        }
        
        // Add current user prompt
        formattedHistory.add(
            GeminiContent(role = "user", parts = listOf(GeminiPart(text = userMessage)))
        )

        val request = GeminiRequest(
            contents = formattedHistory,
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
            generationConfig = GeminiGenerationConfig(temperature = 0.4f)
        )

        val response = apiService.generateContent(apiKey, request)
        val rawAiText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        
        if (rawAiText.isEmpty()) {
            throw Exception("Null AI text received")
        }

        // Post-process response to execute actions
        var processedText = rawAiText
        var apiCalled: String? = null
        var apiResponse: String? = null
        val agentBefore = activeAgentName

        // Parse Agent transitions
        if (processedText.contains("<route:Technical Specialist/>")) {
            activeAgentName = "Technical Specialist"
            repository.insertLog("handoff", "[Handoff Matrix]: Swarm dynamic handoff to Technical Specialist")
            processedText = processedText.replace("<route:Technical Specialist/>", "[Routing user context: Technical Specialist]").trim()
        } else if (processedText.contains("<route:Retention Specialist/>")) {
            activeAgentName = "Retention Specialist"
            repository.insertLog("handoff", "[Handoff Matrix]: Swarm dynamic handoff to Retention Specialist")
            processedText = processedText.replace("<route:Retention Specialist/>", "[Routing user context: Retention Specialist]").trim()
        } else if (processedText.contains("<route:Triage Router/>")) {
            activeAgentName = "Triage Router"
            processedText = processedText.replace("<route:Triage Router/>", "[Routing user context: Triage Router]").trim()
        }

        // Parse Tool Executions
        if (processedText.contains("<call_api_query_current_usage/>")) {
            apiCalled = "api_query_current_usage()"
            apiResponse = repository.apiQueryCurrentUsage()
            processedText = processedText.replace("<call_api_query_current_usage/>", "").trim()
            processedText += "\n\n*Diagnostics Response:* $apiResponse"
        }
        if (processedText.contains("<call_api_apply_platinum_topup/>")) {
            apiCalled = "api_apply_platinum_topup()"
            apiResponse = repository.apiApplyPlatinumTopup()
            processedText = processedText.replace("<call_api_apply_platinum_topup/>", "").trim()
            processedText += "\n\n*Loyalty Response:* $apiResponse"
        }

        val chatMessage = ChatMessageEntity(
            role = "assistant",
            agentName = assistantAgentName(agentBefore, activeAgentName, apiCalled != null),
            content = processedText,
            apiCalled = apiCalled,
            apiResponse = apiResponse
        )
        
        repository.insertMessage(chatMessage)
        return chatMessage
    }

    private suspend fun runRealOpenRouterAi(userMessage: String, dbState: CrmStateEntity): ChatMessageEntity {
        val apiKey = dbState.openRouterApiKey
        val systemPrompt = """
            You are the sovereign Swarm Multi-Agent orchestrator for a premier telecom (called PRReSSO).
            You hold authoritative access over our client database:
            - Client Name: ${dbState.customerName}
            - Current Club Tier: ${dbState.tier}
            - Current Consumption: ${dbState.bandwidthUsedGb}GB out of ${dbState.bandwidthLimitGb}GB Limit (Pending Topups: ${dbState.pendingTopups}).

            The Swarm defines 3 autonomous agents with distinct roles:
            1. 'Triage Router': Greets the user '${dbState.customerName}', assesses their intent.
               - Routes to 'Technical Specialist' for slow speed, diagnostics, Drops, Network claims. Tag: <route:Technical Specialist/>
               - Routes to 'Retention Specialist' for costs, bills, fee disputes, plan upgrades. Tag: <route:Retention Specialist/>
            2. 'Technical Specialist': Analytical diagnostician.
               - Always executes current usage analysis. Tag: <call_api_query_current_usage/>
               - If usage exceeds 90% (which represents ${dbState.bandwidthUsedGb}/${dbState.bandwidthLimitGb}GB = ${(dbState.bandwidthUsedGb.toFloat()/dbState.bandwidthLimitGb * 100).toInt()}%), state high-speed throttle active and hand over parameters immediately to 'Retention Specialist' to procure free reliefs. Tag: <route:Retention Specialist/>
               - Hand over if client mentions plan upgrades, costs or disputes during test. Tag: <route:Retention Specialist/>
            3. 'Retention Specialist': VIP Commercial loyalty desk.
               - Values our Platinum tier users. Does not demand charges. Proactively grants 50GB extra limit. Tag: <call_api_apply_platinum_topup/>
               - Hand back to technical if client asks about diagnostics, faults or physical parameters. Tag: <route:Technical Specialist/>

            Your active persona is currently: '$activeAgentName'.
            Speak directly with premium tone, clear negative spacings, bold markings. Avoid any technical tags in the written speech except the strict functional tags:
            - Routing trigger: <route:Agent Name/> (Agent Name belongs to "Technical Specialist" or "Retention Specialist" or "Triage Router")
            - Database queries triggered by active agent:
              - <call_api_query_current_usage/>
              - <call_api_apply_platinum_topup/>

            Keep response highly formatted, clean and conversational.
        """.trimIndent()

        val rawHistory = repository.allMessages.first()
        val openRouterMessages = mutableListOf<OpenRouterMessage>()
        
        openRouterMessages.add(OpenRouterMessage(role = "system", content = systemPrompt))
        
        rawHistory.takeLast(10).forEach {
            openRouterMessages.add(
                OpenRouterMessage(
                    role = if (it.role == "user") "user" else "assistant",
                    content = it.content
                )
            )
        }
        
        openRouterMessages.add(OpenRouterMessage(role = "user", content = userMessage))

        val request = OpenRouterRequest(
            model = dbState.openRouterModel.ifEmpty { "google/gemini-2.5-flash:free" },
            messages = openRouterMessages,
            temperature = 0.4f
        )

        val authHeader = "Bearer $apiKey"
        val response = openRouterApiService.chatCompletions(
            authHeader = authHeader,
            refererHeader = "https://ai.studio/build",
            titleHeader = "PRReSSO Swarm AI",
            request = request
        )
        
        val rawAiText = response.choices?.firstOrNull()?.message?.content ?: ""
        
        if (rawAiText.isEmpty()) {
            throw Exception("Null or empty message returned from OpenRouter")
        }

        var processedText = rawAiText
        var apiCalled: String? = null
        var apiResponse: String? = null
        val agentBefore = activeAgentName

        if (processedText.contains("<route:Technical Specialist/>")) {
            activeAgentName = "Technical Specialist"
            repository.insertLog("handoff", "[Handoff Matrix]: Swarm dynamic handoff to Technical Specialist")
            processedText = processedText.replace("<route:Technical Specialist/>", "[Routing user context: Technical Specialist]").trim()
        } else if (processedText.contains("<route:Retention Specialist/>")) {
            activeAgentName = "Retention Specialist"
            repository.insertLog("handoff", "[Handoff Matrix]: Swarm dynamic handoff to Retention Specialist")
            processedText = processedText.replace("<route:Retention Specialist/>", "[Routing user context: Retention Specialist]").trim()
        } else if (processedText.contains("<route:Triage Router/>")) {
            activeAgentName = "Triage Router"
            processedText = processedText.replace("<route:Triage Router/>", "[Routing user context: Triage Router]").trim()
        }

        if (processedText.contains("<call_api_query_current_usage/>")) {
            apiCalled = "api_query_current_usage()"
            apiResponse = repository.apiQueryCurrentUsage()
            processedText = processedText.replace("<call_api_query_current_usage/>", "").trim()
            processedText += "\n\n*Diagnostics Response:* $apiResponse"
        }
        if (processedText.contains("<call_api_apply_platinum_topup/>")) {
            apiCalled = "api_apply_platinum_topup()"
            apiResponse = repository.apiApplyPlatinumTopup()
            processedText = processedText.replace("<call_api_apply_platinum_topup/>", "").trim()
            processedText += "\n\n*Loyalty Response:* $apiResponse"
        }

        val chatMessage = ChatMessageEntity(
            role = "assistant",
            agentName = assistantAgentName(agentBefore, activeAgentName, apiCalled != null),
            content = processedText,
            apiCalled = apiCalled,
            apiResponse = apiResponse
        )
        
        repository.insertMessage(chatMessage)
        return chatMessage
    }
}
