package com.example.earrove.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.earrove.ui.theme.DeepGray
import com.example.earrove.ui.theme.EarRoveTheme

@Composable
fun HomeScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Navigation Mode Card
        HomeCard(
            modifier = Modifier.weight(1f),
            title = "智能导航",
            subtitle = "实时指引，规避障碍",
            backgroundColor = MaterialTheme.colorScheme.background,
            icon = Icons.Default.Navigation,
            contentDescription = "导航模式。双击进入。",
            onClick = { navController.navigate("navigation") }
        )

        // 2. OCR Mode Card
        HomeCard(
            modifier = Modifier.weight(1f),
            title = "文字识别",
            subtitle = "即时阅读，信息无碍",
            backgroundColor = DeepGray,
            icon = Icons.Default.TextFields,
            contentDescription = "文字识别模式。双击进入。",
            onClick = { navController.navigate("ocr") }
        )

        // 3. Bottom Action Bar
        BottomActionBar(navController)
    }
}

@Composable
private fun HomeCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(32.dp)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (backgroundColor == DeepGray) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (backgroundColor == DeepGray) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (backgroundColor == DeepGray) Color.Gray else Color.DarkGray
                )
            )
        }
    }
}

@Composable
private fun BottomActionBar(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            ActionButton(
                icon = Icons.Default.Settings,
                contentDescription = "设置",
                onClick = { navController.navigate("settings") }
            )
            ActionButton(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = "帮助",
                onClick = { navController.navigate("help") }
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .size(28.dp)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    EarRoveTheme {
        HomeScreen(navController = rememberNavController())
    }
}
