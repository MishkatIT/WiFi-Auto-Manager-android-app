package com.example.wifiautomanager.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToDiagnostics: () -> Unit = {},
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
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Auto Manager master toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.autoManagerEnabled) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto Manager Monitoring",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.autoManagerEnabled) "Active in background & foreground" else "Monitoring paused",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.autoManagerEnabled,
                    onCheckedChange = { viewModel.onToggleAutoManager(it) }
                )
            }
        }

        // Decision Notifications toggle
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Decision Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Notify when switching or recommending networks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.showDecisionNotifications,
                    onCheckedChange = { viewModel.onToggleNotifications(it) }
                )
            }
        }

        // Engine intervals with sliders
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = "Engine Timing Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // Scan Interval
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Scan Interval (battery-safe)", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "${uiState.scanIntervalSeconds}s", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = uiState.scanIntervalSeconds.toFloat(),
                        onValueChange = { viewModel.onUpdateScanInterval(it.roundToInt()) },
                        valueRange = 15f..120f,
                        steps = 6
                    )
                }

                HorizontalDivider()

                // Switch Cooldown
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Switch Cooldown (Anti-Flapping)", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "${uiState.switchCooldownSeconds}s", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = uiState.switchCooldownSeconds.toFloat(),
                        onValueChange = { viewModel.onUpdateSwitchCooldown(it.roundToInt()) },
                        valueRange = 30f..300f,
                        steps = 8
                    )
                }

                HorizontalDivider()

                // Internet Check Interval
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Internet Validation Check", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "${uiState.internetCheckIntervalSeconds}s", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = uiState.internetCheckIntervalSeconds.toFloat(),
                        onValueChange = { viewModel.onUpdateInternetCheckInterval(it.roundToInt()) },
                        valueRange = 5f..60f,
                        steps = 10
                    )
                }

                HorizontalDivider()

                // Log Retention Days
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Decision Log Retention", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "${uiState.logRetentionDays} days", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = uiState.logRetentionDays.toFloat(),
                        onValueChange = { viewModel.onUpdateLogRetention(it.roundToInt()) },
                        valueRange = 1f..30f,
                        steps = 28
                    )
                }
            }
        }

        // Diagnostics link
        Card(
            onClick = onNavigateToDiagnostics,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Open System Diagnostics", style = MaterialTheme.typography.titleMedium)
                Text(text = "→", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
