package com.example.jiva.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.jiva.JivaColors
import com.example.jiva.R
import com.example.jiva.utils.ScreenUtils

/**
 * Responsive header component for all report screens
 * Adapts to different screen sizes and orientations
 */
@Composable
fun ResponsiveReportHeader(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    val isCompactScreen = ScreenUtils.isCompactScreen()
    val screenSize = ScreenUtils.getScreenSize()
    val orientation = ScreenUtils.getOrientation()

    // Get status bar height
    val view = LocalView.current
    val density = LocalDensity.current
    val statusBarHeight = with(density) {
        ViewCompat.getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())
            ?.top?.toDp() ?: 24.dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(JivaColors.DeepBlue, JivaColors.Purple)
                )
            )
            .padding(
                top = statusBarHeight + 8.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            )
    ) {
        if (isCompactScreen && orientation == ScreenUtils.Orientation.PORTRAIT) {
            // Compact layout for small screens
            CompactHeaderLayout(
                title = title,
                subtitle = subtitle,
                onBackClick = onBackClick,
                actions = actions
            )
        } else {
            // Standard layout for larger screens
            StandardHeaderLayout(
                title = title,
                subtitle = subtitle,
                onBackClick = onBackClick,
                actions = actions
            )
        }
    }
}

@Composable
private fun CompactHeaderLayout(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First row: Back button, Logo, Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(
                        JivaColors.White.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = JivaColors.White
                )
            }
            
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(32.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = JivaColors.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = JivaColors.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = actions
            )
        }
    }
}

@Composable
private fun StandardHeaderLayout(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(
                        JivaColors.White.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = JivaColors.White
                )
            }
            
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(32.dp)
            )
            
            Column {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = JivaColors.White
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = JivaColors.White.copy(alpha = 0.8f)
                )
            }
        }

        // Actions
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = actions
        )
    }
}



/**
 * Responsive WhatsApp button for Day End Report
 */
@Composable
fun ResponsiveWhatsAppButton(
    onClick: () -> Unit,
    isCompact: Boolean = ScreenUtils.isCompactScreen()
) {
    val buttonHeight = if (isCompact) 40.dp else ScreenUtils.getButtonHeight()
    val iconSize = if (isCompact) 16.dp else ScreenUtils.getIconSize()
    val fontSize = if (isCompact) 12.sp else 14.sp
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = JivaColors.Green
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .height(buttonHeight)
            .widthIn(min = if (isCompact) 100.dp else 140.dp),
        contentPadding = PaddingValues(
            horizontal = if (isCompact) 12.dp else 16.dp,
            vertical = 8.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "WhatsApp",
                tint = JivaColors.White,
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = "WhatsApp",
                color = JivaColors.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = fontSize,
                maxLines = 1
            )
        }
    }
}
