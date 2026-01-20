package com.example.earrove.ui.ocr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.earrove.ui.common.EarRoveTopAppBar
import com.example.earrove.ui.theme.EarRoveTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OcrScreen(navController: NavController) {
    var isPaused by remember { mutableStateOf(false) }
    var isFlashlightOn by remember { mutableStateOf(false) } // This would be updated by a real camera controller

    val accessibilityLabel = if (isPaused) {
        "文字识别已暂停。双击可恢复扫描。"
    } else {
        "实时文字识别中。请将文字放入框内。双击可暂停画面。"
    }

    Scaffold(
        topBar = {
            EarRoveTopAppBar(title = "文字识别", onNavigateUp = { navController.popBackStack() })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { isPaused = !isPaused }
                    )
                }
                .semantics { contentDescription = accessibilityLabel },
            contentAlignment = Alignment.Center
        ) {
            // 1. Viewfinder UI
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(200.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (!isPaused) {
                        Text(
                            text = "请将文字放入框内",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 2. Pause Icon (shown when paused)
            if (isPaused) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = null, // Covered by parent's description
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.scale(6f)
                )
            }

            // 3. Flashlight Icon (shown when on)
            if (isFlashlightOn) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null, // Description is in the text below
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.scale(2f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("已开启补光", Modifier.semantics { contentDescription = "已开启补光" })
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OcrScreenPreview() {
    EarRoveTheme {
        OcrScreen(navController = rememberNavController())
    }
}
