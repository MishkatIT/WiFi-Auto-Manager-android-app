package com.example.wifiautomanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wifiautomanager.ui.theme.SignalExcellent
import com.example.wifiautomanager.ui.theme.SignalFair
import com.example.wifiautomanager.ui.theme.SignalGood
import com.example.wifiautomanager.ui.theme.SignalWeak

@Composable
fun SignalStrengthIndicator(
    rssi: Int,
    modifier: Modifier = Modifier
) {
    val level = when {
        rssi >= -60 -> 4
        rssi >= -70 -> 3
        rssi >= -80 -> 2
        rssi >= -90 -> 1
        else -> 0
    }

    val activeColor = when (level) {
        4 -> SignalExcellent
        3 -> SignalGood
        2 -> SignalFair
        else -> SignalWeak
    }

    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 1..4) {
            val barHeight = (4 + (i * 4)).dp
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .background(
                        color = if (i <= level) activeColor else inactiveColor,
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}
