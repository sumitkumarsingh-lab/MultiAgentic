package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ApiLogEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CrmStateEntity
import com.example.ui.viewmodel.PrressoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom modern Bento Grid colors (Light Theme style)
val DeepSpaceBlue = Color(0xFFF7F9FF)  // Bento background canvas
val SurfaceSlate = Color(0xFFEEF1F8)   // Active tab row/track backgrounds
val SurfaceCard = Color(0xFFFFFFFF)    // Pure white Bento containers
val HyperCyan = Color(0xFF005AC1)      // Primary M3 Brand Royal Blue
val TechOrange = Color(0xFFD65C00)     // Bright high-contrast orange for alert metrics
val PlatinumSilver = Color(0xFF1A1C1E)  // Dark slate for text elements
val SuccessEmerald = Color(0xFF0F5132)  // Deep elegant forest green
val TextGray = Color(0xFF5E6272)       // Muted slate gray for secondary labels
val BorderSlate = Color(0xFFDDE2F1)    // Thin elegant panel stroke line
val TextDark = Color(0xFF1A1C1E)       // Ultimate high-contrast body text
val TextNavy = Color(0xFF001D44)       // Rich deep brand navy for headings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainDashboardScreen(
    viewModel: PrressoViewModel,
    modifier: Modifier = Modifier
) {
    val crmState by viewModel.crmState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val apiLogs by viewModel.apiLogs.collectAsStateWithLifecycle()
    val currentAgent by viewModel.currentAgent.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var chatInputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val listState = rememberLazyListState()

    // Scroll to bottom whenever messages list increases
    LaunchedEffect(chatMessages.size, isGenerating) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Outer Edge-To-Edge scaffolding with dark system gradient backing
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepSpaceBlue, Color(0xFFE8EEF9))
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // High-fidelity App bar
            PRReSSOHeader(
                activeAgent = currentAgent,
                onResetClick = { viewModel.resetSession() }
            )

            // Subscriber Compact Summary bar
            SubscriberMiniProfile(crmState = crmState)

            // Modern Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = HyperCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = HyperCyan
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceSlate)
                    .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dashboard, contentDescription = null, tint = if (selectedTab == 0) HyperCyan else TextGray, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manager Dashboard", color = if (selectedTab == 0) HyperCyan else TextGray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_dashboard")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = if (selectedTab == 1) HyperCyan else TextGray, modifier = Modifier.size(18.dp))
                                if (isGenerating) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.TopEnd)
                                            .background(TechOrange, CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Swarm Chat", color = if (selectedTab == 1) HyperCyan else TextGray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_chat")
                )
            }

            // Screen Content Pages
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
            ) {
                if (selectedTab == 0) {
                    // Manager Dashboard & Database State
                    DashboardPage(
                        crmState = crmState,
                        apiLogs = apiLogs,
                        currentAgent = currentAgent,
                        isWorking = isGenerating,
                        onTriggerDiagnostics = { viewModel.manualQueryUsage() },
                        onTriggerTopup = { viewModel.manualApplyTopup() },
                        onAgentSelect = { viewModel.manualChangeAgent(it) }
                    )
                } else {
                    // AI Swarm Chat Console
                    ChatPage(
                        messages = chatMessages,
                        isGenerating = isGenerating,
                        currentAgent = currentAgent,
                        inputText = chatInputText,
                        onInputChanged = { chatInputText = it },
                        onSendClick = {
                            if (chatInputText.isNotBlank()) {
                                viewModel.sendMessage(chatInputText)
                                chatInputText = ""
                                keyboardController?.hide()
                            }
                        },
                        onAgentSelect = { viewModel.manualChangeAgent(it) },
                        lazyListState = listState,
                        isApiKeyConfigured = viewModel.isApiKeyConfigured()
                    )
                }
            }
        }
    }
}

@Composable
fun PRReSSOHeader(
    activeAgent: String,
    onResetClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // High-polish brand icon matching the Bento HTML template
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(HyperCyan, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "P",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PRReSSO",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = HyperCyan, // Royal Blue
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(HyperCyan.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .border(0.5.dp, HyperCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SWARM ACTIVE",
                            fontSize = 8.sp,
                            color = HyperCyan,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Text(
                    text = "Premium Support Orchestrator",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        color = TextGray
                    )
                )
            }
        }

        IconButton(
            onClick = onResetClick,
            modifier = Modifier
                .background(SurfaceSlate, RoundedCornerShape(12.dp))
                .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                .size(40.dp)
                .testTag("reset_session_button"),
            colors = IconButtonDefaults.iconButtonColors(contentColor = TextGray)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset State", modifier = Modifier.size(18.dp), tint = TechOrange)
        }
    }
}

@Composable
fun SubscriberMiniProfile(crmState: CrmStateEntity?) {
    if (crmState == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .background(SurfaceSlate, RoundedCornerShape(16.dp))
            .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                tint = HyperCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = crmState.customerName,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(6.dp))
                    .border(0.5.dp, BorderSlate, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = crmState.tier,
                    color = HyperCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(if (crmState.isThrottled) TechOrange else SuccessEmerald, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (crmState.isThrottled) "SPEED THROTTLED" else "HI-SPEED RESTORED",
                color = if (crmState.isThrottled) TechOrange else SuccessEmerald,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BentoQuickStatsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Balance Card (Bento Style)
        Card(
            modifier = Modifier
                .weight(1.0f)
                .height(130.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, BorderSlate)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.Wallet,
                    contentDescription = null,
                    tint = HyperCyan,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Next Bill",
                        color = TextGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$84.50",
                        color = TextDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Due in 12 days",
                        color = TextGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        // Active Plan Card (Bento Style)
        Card(
            modifier = Modifier
                .weight(1.0f)
                .height(130.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, BorderSlate)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = HyperCyan,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Active Plan",
                        color = TextGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Infinite Pro",
                        color = TextDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "5G ENABLED",
                        color = SuccessEmerald,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardPage(
    crmState: CrmStateEntity?,
    apiLogs: List<ApiLogEntity>,
    currentAgent: String,
    isWorking: Boolean,
    onTriggerDiagnostics: () -> Unit,
    onTriggerTopup: () -> Unit,
    onAgentSelect: (String) -> Unit
) {
    if (crmState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = HyperCyan)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Dynamic Animated Progress Meter
        item {
            UsageMeterCard(crmState = crmState)
        }

        // Bento Quick Stats Row (Balance & Plan)
        item {
            BentoQuickStatsRow()
        }

        // Active Diagnostics Handoff status
        item {
            HandoffStatusCard(
                currentAgent = currentAgent,
                isWorking = isWorking,
                onAgentSelect = onAgentSelect
            )
        }

        // Authoritative Action Tools
        item {
            AuthoritativeActionCard(
                onTriggerDiagnostics = onTriggerDiagnostics,
                onTriggerTopup = onTriggerTopup
            )
        }

        // System of Record Logs
        item {
            SystemLedgerCard(logs = apiLogs)
        }
    }
}

@Composable
fun UsageMeterCard(crmState: CrmStateEntity) {
    val animatedProgress = animateFloatAsState(
        targetValue = crmState.bandwidthUsedGb.toFloat() / crmState.bandwidthLimitGb.toFloat(),
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "usage_progress"
    )

    val percentage = (animatedProgress.value * 100).toInt().coerceIn(0, 100)
    val limitColor = if (crmState.isThrottled) TechOrange else HyperCyan

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFD3E4FF)),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = "Bandwidth Consumption",
                    color = TextNavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Auth Database Live Metrics",
                    color = TextNavy.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${crmState.bandwidthUsedGb} ",
                        color = TextNavy,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp
                    )
                    Text(
                        text = "GB used",
                        color = TextNavy.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Text(
                    text = "Absolute Limit: ${crmState.bandwidthLimitGb} GB",
                    color = limitColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )

                Text(
                    text = "Loyalty Topups: ${crmState.pendingTopups}",
                    color = SuccessEmerald,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    fontWeight = FontWeight.Medium
                )
            }

            // Custom Circular Gauge
            Box(
                modifier = Modifier
                    .size(95.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(85.dp)) {
                    // Gray Track Background
                    drawArc(
                        color = TextNavy.copy(alpha = 0.1f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Active Colored Gauge
                    drawArc(
                        color = limitColor,
                        startAngle = 135f,
                        sweepAngle = 270f * animatedProgress.value,
                        useCenter = false,
                        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percentage%",
                        color = TextNavy,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "CAP CITY",
                        color = TextNavy.copy(alpha = 0.6f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun HandoffStatusCard(
    currentAgent: String,
    isWorking: Boolean,
    onAgentSelect: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active AI Support Agent",
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Swarm Session Context Hub",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }

                if (isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = HyperCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pulse Agent indicator
            AgentBadge(agentName = currentAgent)

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderSlate, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Manual Override / Route Agent Context",
                color = TextDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val agentList = listOf("Triage Router", "Technical Specialist", "Retention Specialist")
                agentList.forEach { agent ->
                    val isSelected = currentAgent.contains(agent)
                    val color = when (agent) {
                        "Triage Router" -> Color(0xFF64748B)
                        "Technical Specialist" -> TechOrange
                        else -> SuccessEmerald
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .background(
                                if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) color else BorderSlate,
                                shape = RoundedCornerShape(12.dp)
                              )
                            .clickable { onAgentSelect(agent) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = agent.split(" ")[0] + "\n" + (agent.split(" ").getOrNull(1) ?: ""),
                            color = if (isSelected) color else TextGray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgentBadge(agentName: String) {
    val (color, desc, icon) = when {
        agentName.contains("Technical") -> Triple(TechOrange, "Technical Specialist: Diagnoses network, checks line speeds, and hands off when throttled.", Icons.Default.NetworkCheck)
        agentName.contains("Retention") -> Triple(SuccessEmerald, "Retention Specialist: Manages VIP loyalty accounts and issues free 50GB topups.", Icons.Default.Star)
        else -> Triple(Color(0xFF64748B), "Triage Router: Reads request intent and matches to the optimal support engineer.", Icons.Default.Directions)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(color.copy(alpha = 0.2f), CircleShape)
                .padding(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1.0f)) {
            Text(
                text = agentName,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = desc,
                color = TextDark.copy(alpha = 0.8f),
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun AuthoritativeActionCard(
    onTriggerDiagnostics: () -> Unit,
    onTriggerTopup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Secure CRM Modification Tools",
                color = TextDark,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "Manual Authoritative API Executions",
                color = TextGray,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Diagnostics API Call
                Button(
                    onClick = onTriggerDiagnostics,
                    modifier = Modifier
                        .weight(1.0f)
                        .height(44.dp)
                        .testTag("action_diagnostics"),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceSlate),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderSlate)
                ) {
                    Icon(
                        Icons.Default.NetworkCheck,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TechOrange
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Query Usage",
                        color = TextDark,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Topup API Call
                Button(
                    onClick = onTriggerTopup,
                    modifier = Modifier
                        .weight(1.0f)
                        .height(44.dp)
                        .testTag("action_topup"),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SuccessEmerald.copy(alpha = 0.3f))
                ) {
                    Icon(
                        Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = SuccessEmerald
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Platinum Topup",
                        color = SuccessEmerald,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SystemLedgerCard(logs: List<ApiLogEntity>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Auth System of Record Ledger",
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Immutable Backend DB Mutations",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(HyperCyan.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LIVE FEED",
                        color = HyperCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No ledger changes recorded.", color = TextGray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth()
                        .background(Color(0xFF0F141C), RoundedCornerShape(12.dp))
                        .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(logs) { log ->
                        val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                        val (color, tagText) = when (log.type) {
                            "query_usage" -> Pair(TechOrange, "QUERY")
                            "apply_topup" -> Pair(SuccessEmerald, "MUTATION")
                            "handoff" -> Pair(HyperCyan, "HANDOFF")
                            else -> Pair(PlatinumSilver, "SYSTEM")
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "[$timeString]",
                                    color = TextGray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                               )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "[$tagText]",
                                    color = color,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = log.message,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatPage(
    messages: List<ChatMessageEntity>,
    isGenerating: Boolean,
    currentAgent: String,
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onAgentSelect: (String) -> Unit,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    isApiKeyConfigured: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Warning if default local simulation vs live AI
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(
                    if (isApiKeyConfigured) SuccessEmerald.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f),
                    RoundedCornerShape(8.dp)
                )
                .border(
                    0.5.dp,
                    if (isApiKeyConfigured) SuccessEmerald.copy(alpha = 0.3f) else BorderSlate.copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isApiKeyConfigured) Icons.Default.VerifiedUser else Icons.Default.Info,
                contentDescription = null,
                tint = if (isApiKeyConfigured) SuccessEmerald else TextGray,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isApiKeyConfigured) {
                    "Live cognitive Gemini Swarm orchestrating API calls."
                } else {
                    "Sovereign sandbox agent mimicking Swarm handoffs."
                },
                color = if (isApiKeyConfigured) TextDark else TextGray,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Active conversation bubble list
        Box(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = BorderSlate,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No conversation yet",
                            color = TextGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Say hello to set up the session!",
                            color = TextGray.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp)
                ) {
                    items(messages) { message ->
                        ChatBubble(message = message)
                    }

                    if (isGenerating) {
                        item {
                            TypingIndicatorBubble(agentName = currentAgent)
                        }
                    }
                }
            }
        }

        // Horizontal line separator
        HorizontalDivider(color = BorderSlate, thickness = 0.5.dp)

        // Typing Box Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = onInputChanged,
                placeholder = {
                    Text(
                        text = "Talk to AI Agents (e.g. slow speed)...",
                        color = TextGray.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .weight(1.0f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                    .testTag("chat_input_textfield"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard,
                    disabledContainerColor = SurfaceCard,
                    cursorColor = HyperCyan,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            onSendClick()
                        }
                    }
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendClick,
                enabled = inputText.isNotBlank() && !isGenerating,
                modifier = Modifier
                    .background(
                        if (inputText.isNotBlank() && !isGenerating) HyperCyan else SurfaceSlate,
                        RoundedCornerShape(12.dp)
                    )
                    .size(48.dp)
                    .testTag("send_message_button"),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (inputText.isNotBlank()) DeepSpaceBlue else TextGray
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Intent",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessageEntity) {
    val isUser = message.role == "user"
    val agentColor = when {
        message.agentName.contains("Technical") -> TechOrange
        message.agentName.contains("Retention") -> SuccessEmerald
        else -> Color(0xFF64748B)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender Metadata Badge
        Text(
            text = if (isUser) "You [Sumit Kumar]" else "🤖 Agent Hand-off: ${message.agentName}",
            color = if (isUser) TextGray else agentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            fontFamily = FontFamily.Monospace
        )

        // Text bubble frame
        Box(
            modifier = Modifier
                .widthIn(max = 295.dp)
                .background(
                    color = if (isUser) HyperCyan.copy(alpha = 0.08f) else SurfaceCard,
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 12.dp
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) HyperCyan.copy(alpha = 0.3f) else BorderSlate,
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 12.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    color = TextDark,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                // Tool Action Notification inside Chat Bubble
                if (message.apiCalled != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .background(SurfaceSlate, RoundedCornerShape(6.dp))
                            .border(0.5.dp, agentColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = agentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Exited system tool: ${message.apiCalled}",
                            color = agentColor,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble(agentName: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "indicator")
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 800
                0.4f at 0
                1.0f at 200
                0.4f at 400
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )
    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 800
                0.4f at 150
                1.0f at 350
                0.4f at 550
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )
    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 800
                0.4f at 300
                1.0f at 500
                0.4f at 700
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "🤖 $agentName is responding...",
            color = TextGray,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            fontFamily = FontFamily.Monospace
        )

        Row(
            modifier = Modifier
                .background(SurfaceCard, RoundedCornerShape(12.dp))
                .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .drawBehind { drawCircle(color = HyperCyan, radius = size.minDimension / 2 * dot1Scale) }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .drawBehind { drawCircle(color = HyperCyan, radius = size.minDimension / 2 * dot2Scale) }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .drawBehind { drawCircle(color = HyperCyan, radius = size.minDimension / 2 * dot3Scale) }
            )
        }
    }
}

// Separate styling helper for Material Design compliant tint color
@Composable
fun platinumSilverColorTint(): Color = PlatinumSilver
