package com.example.wifiautomanager.ui.rules

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wifiautomanager.domain.rule.LogicalOperator
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleBuilderScreen(
    viewModel: RuleBuilderViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is RuleBuilderEvent.SaveSuccess -> onBackClick()
                is RuleBuilderEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) "Edit Rule" else "Build Rule",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            // Rule Name
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Rule Name") },
                placeholder = { Text("e.g. Weak Signal Switch") },
                isError = uiState.nameError != null,
                supportingText = {
                    uiState.nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // WHEN Section (Conditions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WHEN (Conditions)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Operator toggle (AND / OR)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = uiState.operator == LogicalOperator.AND,
                        onClick = { viewModel.onOperatorChanged(LogicalOperator.AND) },
                        label = { Text("ALL (AND)") }
                    )
                    FilterChip(
                        selected = uiState.operator == LogicalOperator.OR,
                        onClick = { viewModel.onOperatorChanged(LogicalOperator.OR) },
                        label = { Text("ANY (OR)") }
                    )
                }
            }

            // Condition Cards
            uiState.conditions.forEachIndexed { index, condition ->
                ConditionRowCard(
                    condition = condition,
                    canDelete = uiState.conditions.size > 1,
                    onTypeChange = { type -> viewModel.onConditionTypeChanged(index, type) },
                    onIntValueChange = { value -> viewModel.onConditionIntValueChange(index, value) },
                    onTextValueChange = { text -> viewModel.onConditionTextValueChange(index, text) },
                    onDelete = { viewModel.onRemoveCondition(index) }
                )
            }

            OutlinedButton(
                onClick = viewModel::onAddCondition,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Condition")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // THEN Section (Actions)
            Text(
                text = "THEN (Action)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionRadioButton(
                        selected = uiState.selectedActionType == "PREFER_BEST",
                        text = "Prefer best available candidate",
                        onClick = { viewModel.onActionTypeSelected("PREFER_BEST") }
                    )

                    ActionRadioButton(
                        selected = uiState.selectedActionType == "STAY",
                        text = "Stay on current network",
                        onClick = { viewModel.onActionTypeSelected("STAY") }
                    )

                    ActionRadioButton(
                        selected = uiState.selectedActionType == "PREFER_SPECIFIC",
                        text = "Prefer specific saved network",
                        onClick = { viewModel.onActionTypeSelected("PREFER_SPECIFIC") }
                    )

                    if (uiState.selectedActionType == "PREFER_SPECIFIC" && uiState.savedNetworks.isNotEmpty()) {
                        SpecificNetworkDropdown(
                            networks = uiState.savedNetworks,
                            selectedId = uiState.selectedNetworkId,
                            onSelect = viewModel::onSpecificNetworkSelected
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Live Preview Card
            Text(
                text = "Rule Preview",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(14.dp)
            ) {
                Text(
                    text = uiState.previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buttons
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
                    onClick = viewModel::onSaveRule,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Rule")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConditionRowCard(
    condition: EditableCondition,
    canDelete: Boolean,
    onTypeChange: (ConditionTypeUi) -> Unit,
    onIntValueChange: (Int) -> Unit,
    onTextValueChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dropdown for condition type
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = condition.type.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ConditionTypeUi.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    onTypeChange(type)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (canDelete) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove condition", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Controls depending on type
            when (condition.type) {
                ConditionTypeUi.CURRENT_SIGNAL_BELOW,
                ConditionTypeUi.CURRENT_SIGNAL_ABOVE,
                ConditionTypeUi.CANDIDATE_SIGNAL_ABOVE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Threshold", style = MaterialTheme.typography.bodySmall)
                        Text("${condition.intValue} dBm", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = condition.intValue.toFloat(),
                        onValueChange = { onIntValueChange(it.toInt()) },
                        valueRange = -90f..-40f
                    )
                }

                ConditionTypeUi.INTERNET_UNAVAILABLE_FOR -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Duration", style = MaterialTheme.typography.bodySmall)
                        Text("${condition.intValue} seconds", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = condition.intValue.toFloat(),
                        onValueChange = { onIntValueChange(it.toInt()) },
                        valueRange = 5f..60f
                    )
                }

                ConditionTypeUi.CURRENT_SSID_IS -> {
                    OutlinedTextField(
                        value = condition.textValue,
                        onValueChange = onTextValueChange,
                        label = { Text("SSID Name") },
                        placeholder = { Text("e.g. Home_WiFi") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun ActionRadioButton(
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpecificNetworkDropdown(
    networks: List<com.example.wifiautomanager.domain.model.WifiNetwork>,
    selectedId: Long?,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedNet = networks.find { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedNet?.ssid ?: "Select network...",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            networks.forEach { net ->
                DropdownMenuItem(
                    text = { Text(net.ssid) },
                    onClick = {
                        onSelect(net.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
