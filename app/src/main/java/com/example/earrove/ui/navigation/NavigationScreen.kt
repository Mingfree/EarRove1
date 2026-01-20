package com.example.earrove.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.earrove.ui.common.EarRoveTopAppBar
import com.example.earrove.ui.theme.EarRoveTheme
import com.example.earrove.ui.theme.PremiumGold
import com.example.earrove.ui.theme.PureBlack
import com.example.earrove.ui.theme.PureWhite
import com.example.earrove.ui.theme.WarningOrange

@Composable
fun NavigationScreen(navController: NavController) {
    var isNavigating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            EarRoveTopAppBar(title = "导航模式", onNavigateUp = { navController.popBackStack() })
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (isNavigating) {
                NavigationInProgressUI(
                    nextInstruction = "向左转",
                    nextInstructionIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    isObstacleDetected = false, // Set to true to see obstacle warning
                    remainingDistance = "500米",
                    remainingTime = "预计5分钟"
                )
            } else {
                NavigationStandbyUI(onMicClick = { isNavigating = true })
            }
        }
    }
}

@Composable
fun NavigationStandbyUI(onMicClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onMicClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "请说话。双击屏幕任意位置开始语音输入目的地。"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.scale(5f)
            )
            Spacer(Modifier.height(48.dp))
            Text(
                text = "请说出目的地",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun NavigationInProgressUI(
    nextInstruction: String,
    nextInstructionIcon: ImageVector,
    isObstacleDetected: Boolean,
    remainingDistance: String,
    remainingTime: String
) {
    Box(modifier = Modifier.fillMaxSize().background(PureBlack)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween // 首尾分布
        ) {
            // 顶部：指令
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = nextInstructionIcon,
                    contentDescription = null,
                    tint = PremiumGold,
                    modifier = Modifier.size(120.dp)
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = nextInstruction,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp),
                    color = PureWhite
                )
            }

            // 底部：状态数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusItem(label = "距离", value = remainingDistance)
                StatusItem(label = "预计", value = remainingTime)
            }
        }

        // 障碍物警示：全屏覆盖但保持极简
        AnimatedVisibility(visible = isObstacleDetected, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WarningOrange.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "前方危险",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp),
                    color = PureBlack,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun StatusItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(name = "Navigation Screen Standby", showBackground = true)
@Composable
fun NavigationScreenStandbyPreview() {
    EarRoveTheme {
        NavigationStandbyUI()
    }
}

@Preview(name = "Navigation Screen In-Progress", showBackground = true)
@Composable
fun NavigationInProgressPreview() {
    EarRoveTheme {
        NavigationInProgressUI(
            nextInstruction = "向左转",
            nextInstructionIcon = Icons.AutoMirrored.Filled.ArrowBack,
            isObstacleDetected = false,
            remainingDistance = "500米",
            remainingTime = "预计5分钟"
        )
    }
}

@Preview(name = "Navigation Screen with Obstacle", showBackground = true)
@Composable
fun NavigationInProgressObstaclePreview() {
    EarRoveTheme {
        NavigationInProgressUI(
            nextInstruction = "向左转",
            nextInstructionIcon = Icons.AutoMirrored.Filled.ArrowBack,
            isObstacleDetected = true,
            remainingDistance = "500米",
            remainingTime = "预计5分钟"
        )
    }
}
