package com.example.wifiautomanager.ui.diagnostics

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wifiautomanager.domain.model.Decision
import com.example.wifiautomanager.domain.model.DecisionAction
import com.example.wifiautomanager.domain.model.InternetStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    onDecisionClick: (Long) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back"
                )
            }
            Text(
                text = "System Diagnostics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Current Wi-Fi State Snapshot
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Current Wi-Fi Snapshot",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "SSID: ${uiState.currentSsid ?: "Not connected"}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Signal: ${uiState.currentRssi?.let { "$it dBm" } ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Frequency: ${uiState.currentFrequency?.let { "$it MHz" } ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Internet Validation: ${if (uiState.currentInternetStatus == InternetStatus.AVAILABLE) "Validated (Online)" else "Unavailable / Checking"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Engine Status & Timing
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Decision Engine Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Engine Master Status: ${if (uiState.autoManagerEnabled) "Active" else "Disabled in Settings"}")
                Text(text = "Scanner State: ${uiState.lastScanTimeText}")
                Text(text = "Recent Decisions Evaluated: ${uiState.recentDecisions.size}")
                Text(text = "Anti-Flapping Guard: Active (hysteresis enforced)")
            }
        }

        // Android Wi-Fi Suggestion API Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.isSuggestionSupported) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (uiState.isSuggestionSupported) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Android Wi-Fi Suggestion API",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Compatibility: ${if (uiState.isSuggestionSupported) "Supported (API ${uiState.androidSdkInt})" else "Requires API 29+"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Active Registered Suggestions: ${uiState.activeSuggestionsCount} / ${uiState.totalSavedNetworks}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Background Execution & Services
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Background Execution Architecture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Foreground Service: ${if (uiState.isForegroundServiceRunning) "Running (Continuous observation)" else "Stopped"}")
                Text(text = "WorkManager Worker: Periodic (Battery-aware & Doze-friendly)")
                Text(text = "Boot Completed Receiver: Registered")
            }
        }

        // Recent 20 Decision Logs
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Recent Decision History (${uiState.recentDecisions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.recentDecisions.isEmpty()) {
                    Text(
                        text = "No decisions recorded yet. Decisions are logged during active scan and connection cycles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    uiState.recentDecisions.forEachIndexed { index, decision ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDecisionClick(decision.id) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = dateFormat.format(Date(decision.timestampMs)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    DecisionActionLabel(action = decision.action)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = decision.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "View detail",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (index < uiState.recentDecisions.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DecisionActionLabel(action: DecisionAction) {
    val (text, color) = when (action) {
        is DecisionAction.SuggestNetwork -> "Recommend Switch" to Color(0xFF2E7D32)
        is DecisionAction.StayOnCurrent -> "Stay" to Color(0xFF1976D2)
        is DecisionAction.CooldownActive -> "Cooldown" to Color(0xFFE65100)
        is DecisionAction.NoCandidates -> "No Candidates" to Color(0xFF757575)
        is DecisionAction.RulesDisabled -> "Disabled" to Color(0xFF9E9E9E)
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color
    )
}
