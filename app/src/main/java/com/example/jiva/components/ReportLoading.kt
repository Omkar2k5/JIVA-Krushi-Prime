package com.example.jiva.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jiva.JivaColors

/**
 * Common loading UI for report screens to ensure consistent UX.
 *
 * - Shows a circular spinner, title, linear progress (determinate or indeterminate),
 *   a message line, and optional percent text.
 */
@Composable
fun ReportLoading(
    title: String,
    message: String,
    progressPercent: Int? = null, // if null -> show indeterminate linear
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(32.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = JivaColors.DeepBlue,
            strokeWidth = 4.dp
        )
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = JivaColors.DeepBlue
        )
        if (progressPercent != null) {
            LinearProgressIndicator(
                progress = { progressPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = JivaColors.DeepBlue,
                trackColor = JivaColors.LightGray
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = JivaColors.DeepBlue,
                trackColor = JivaColors.LightGray
            )
        }
        Text(
            text = message,
            fontSize = 12.sp,
            color = JivaColors.DarkGray,
            textAlign = TextAlign.Center
        )
        if (progressPercent != null) {
            Text(
                text = "$progressPercent% Complete",
                fontSize = 10.sp,
                color = JivaColors.DarkGray
            )
        }
    }
}