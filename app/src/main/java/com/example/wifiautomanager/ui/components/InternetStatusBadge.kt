package com.example.wifiautomanager.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wifiautomanager.domain.model.InternetStatus
import com.example.wifiautomanager.ui.theme.StatusError
import com.example.wifiautomanager.ui.theme.StatusSuccess
import com.example.wifiautomanager.ui.theme.StatusWarning

@Composable
fun InternetStatusBadge(
    status: InternetStatus,
    modifier: Modifier = Modifier
) {
    val (icon, text, color) = when (status) {
        InternetStatus.AVAILABLE -> Triple(Icons.Default.CheckCircle, "Internet Available", StatusSuccess)
        InternetStatus.UNAVAILABLE -> Triple(Icons.Default.Error, "No Internet", StatusError)
        InternetStatus.CHECKING -> Triple(Icons.Default.HourglassEmpty, "Checking...", StatusWarning)
        InternetStatus.UNKNOWN -> Triple(Icons.Default.Warning, "Unknown", MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}
