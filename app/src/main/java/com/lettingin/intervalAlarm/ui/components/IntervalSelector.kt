package com.lettingin.intervalAlarm.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Maximum allowed interval in minutes (12 hours)
 */
private const val MAX_INTERVAL_LIMIT = 720 // 12 hours

/**
 * Fixed step size for interval slider (5 minutes)
 */
private const val STEP_SIZE = 5

/**
 * Format interval duration for display
 * Handles minutes, hours, and mixed formats
 */
private fun formatInterval(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes minutes"
        minutes % 60 == 0 -> {
            val hours = minutes / 60
            "$hours hour${if (hours > 1) "s" else ""}"
        }
        else -> {
            val hours = minutes / 60
            val mins = minutes % 60
            "$hours hour${if (hours > 1) "s" else ""} $mins minutes"
        }
    }
}

/**
 * Slider component for interval selection with fixed 5-minute step size
 */
@Composable
fun IntervalSlider(
    currentInterval: Int,
    minInterval: Int = 5,
    maxInterval: Int,
    onIntervalChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate number of steps between min and max with 5-minute increments
    val steps = ((maxInterval - minInterval) / STEP_SIZE) - 1
    
    Column(modifier = modifier) {
        Slider(
            value = currentInterval.toFloat(),
            onValueChange = { value ->
                // Snap to nearest 5-minute step
                val snappedValue = (value / STEP_SIZE).roundToInt() * STEP_SIZE
                onIntervalChange(snappedValue.coerceIn(minInterval, maxInterval))
            },
            valueRange = minInterval.toFloat()..maxInterval.toFloat(),
            steps = steps.coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth()
        )
        
        // Min/max labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${minInterval}m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${maxInterval}m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Quick interval option buttons for common presets
 * Filters options based on maxInterval and highlights current selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickIntervalOptions(
    currentInterval: Int,
    maxInterval: Int,
    onIntervalSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Preset options: 15, 30, 45, 60 minutes
    val quickOptions = listOf(15, 30, 45, 60).filter { it <= maxInterval }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickOptions.forEach { interval ->
            FilterChip(
                selected = currentInterval == interval,
                onClick = { onIntervalSelect(interval) },
                label = { Text("${interval}m") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Complete interval selector component combining current value display,
 * quick option buttons, and slider control
 */
@Composable
fun IntervalSelector(
    currentInterval: Int,
    minInterval: Int = 5,
    maxInterval: Int,
    onIntervalChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Current interval display
        Text(
            text = formatInterval(currentInterval),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        // Quick option buttons
        QuickIntervalOptions(
            currentInterval = currentInterval,
            maxInterval = maxInterval,
            onIntervalSelect = onIntervalChange
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Slider control
        IntervalSlider(
            currentInterval = currentInterval,
            minInterval = minInterval,
            maxInterval = maxInterval,
            onIntervalChange = onIntervalChange
        )
    }
}
