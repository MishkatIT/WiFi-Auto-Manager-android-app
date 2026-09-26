package com.example.wifiautomanager.ui.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wifiautomanager.domain.model.CandidateResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionDetailScreen(
    viewModel: DecisionDetailViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Decision Analysis", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.decision == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Decision not found", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Primary Action Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (uiState.actionTypeBadge) {
                            "SWITCH_RECOMMENDED" -> MaterialTheme.colorScheme.primaryContainer
                            "COOLDOWN_ACTIVE" -> Color(0xFFFFF3E0)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.formattedTimestamp,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ActionBadge(badge = uiState.actionTypeBadge)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = uiState.actionTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.reasonExplanation,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Core Rationale: "Why didn't it switch?" / "Why did it switch?"
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Evaluation Explanation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.whyNotSwitchExplanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Current Wi-Fi state snapshot
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Current State Snapshot",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.currentNetworkSummary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Candidates Evaluation Table
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Evaluated Candidates (${uiState.candidates.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.candidates.isEmpty()) {
                            Text(
                                text = "No candidate networks were found or evaluated during this cycle.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.candidates.forEachIndexed { index, candidate ->
                                CandidateRow(candidate = candidate)
                                if (index < uiState.candidates.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(candidate: CandidateResult) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.network.ssid,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Priority: ${candidate.network.priority} • Min signal: ${candidate.network.minimumSignalDbm} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (candidate.qualified) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (candidate.qualified) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (candidate.qualified) Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (candidate.qualified) "Qualified" else "Rejected",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (candidate.qualified) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (candidate.scannedNetwork != null) {
            Text(
                text = "Scanned signal: ${candidate.scannedNetwork.rssi} dBm (${candidate.scannedNetwork.frequencyMhz} MHz)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Network not detected in scan results",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        candidate.signalImprovement?.let { delta ->
            Text(
                text = "Signal improvement: ${if (delta >= 0) "+$delta" else "$delta"} dBm",
                style = MaterialTheme.typography.bodySmall,
                color = if (delta >= candidate.network.minimumImprovementDbm) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
            )
        }

        candidate.rejectionReason?.let { reason ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Disqualification: $reason",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFC62828),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ActionBadge(badge: String) {
    val (label, bg, fg) = when (badge) {
        "SWITCH_RECOMMENDED" -> Triple("Switch", Color(0xFF2E7D32), Color.White)
        "COOLDOWN_ACTIVE" -> Triple("Cooldown Active", Color(0xFFE65100), Color.White)
        "STAY" -> Triple("Stay", Color(0xFF1976D2), Color.White)
        "NO_CANDIDATE" -> Triple("No Candidates", Color(0xFF757575), Color.White)
        "RULES_DISABLED" -> Triple("Rules Disabled", Color(0xFF9E9E9E), Color.White)
        else -> Triple(badge, Color(0xFF757575), Color.White)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}
