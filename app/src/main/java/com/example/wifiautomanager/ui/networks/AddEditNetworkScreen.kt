package com.example.wifiautomanager.ui.networks

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.wifiautomanager.domain.model.SecurityType
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNetworkScreen(
    viewModel: AddEditNetworkViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // SECURE LOCAL STATE: Password is NEVER in UiState or ViewModel
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AddEditNetworkEvent.SaveSuccess -> onBackClick()
                is AddEditNetworkEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) "Edit Network" else "Add Network",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Network Identification
            Text(
                text = "Network Credentials",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = uiState.ssid,
                onValueChange = viewModel::onSsidChanged,
                label = { Text("Network Name (SSID)") },
                placeholder = { Text("e.g. Home_WiFi") },
                isError = uiState.ssidError != null,
                supportingText = {
                    uiState.ssidError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Wifi, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // Security Type selector
            Column {
                Text(
                    text = "Security Type",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        SecurityType.WPA2_PSK to "WPA2",
                        SecurityType.WPA3_SAE to "WPA3",
                        SecurityType.OPEN to "Open"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = uiState.securityType == type,
                            onClick = { viewModel.onSecurityTypeChanged(type) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Password Field (if not OPEN)
            if (uiState.securityType != SecurityType.OPEN) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = {
                        if (uiState.isEditMode && uiState.hasStoredPassword) {
                            Text("Leave blank to keep existing")
                        } else {
                            Text("Minimum 8 characters")
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                    },
                    isError = uiState.passwordError != null,
                    supportingText = {
                        if (uiState.passwordError != null) {
                            Text(uiState.passwordError!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("🔒 Stored securely in device Keystore", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Section 2: Connection Preferences
            Text(
                text = "Connection Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Priority Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Priority", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("${uiState.priority} / 100", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = uiState.priority.toFloat(),
                            onValueChange = { viewModel.onPriorityChanged(it.toInt()) },
                            valueRange = 1f..100f,
                            steps = 98
                        )
                        Text(
                            "Higher priority networks are evaluated first",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Minimum Signal Threshold Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Minimum Signal Threshold", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("${uiState.minimumSignalDbm} dBm", fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = uiState.minimumSignalDbm.toFloat(),
                            onValueChange = { viewModel.onMinimumSignalChanged(it.toInt()) },
                            valueRange = -90f..-40f
                        )
                        Text(
                            "Do not connect or stay connected if weaker than this",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Minimum Candidate Signal Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Minimum Candidate Signal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("${uiState.minimumCandidateSignalDbm} dBm", fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = uiState.minimumCandidateSignalDbm.toFloat(),
                            onValueChange = { viewModel.onMinimumCandidateSignalChanged(it.toInt()) },
                            valueRange = -90f..-40f
                        )
                        Text(
                            "Required strength to consider switching to this network",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Minimum Improvement Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Minimum Improvement Required", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("${uiState.minimumImprovementDbm} dBm", fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = uiState.minimumImprovementDbm.toFloat(),
                            onValueChange = { viewModel.onMinimumImprovementChanged(it.toInt()) },
                            valueRange = 0f..30f
                        )
                        Text(
                            "Delta required above current signal to avoid flapping",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Require Internet Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Require Internet", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                "Prefer only when validated internet access is confirmed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = uiState.requiresInternet,
                            onCheckedChange = viewModel::onRequiresInternetChanged
                        )
                    }

                    // Enabled Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enabled", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                "Allow auto-manager to connect to this network",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = uiState.enabled,
                            onCheckedChange = viewModel::onEnabledChanged
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: Cancel and Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = { viewModel.onSave(password) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Network")
                }
            }
        }
    }
}
