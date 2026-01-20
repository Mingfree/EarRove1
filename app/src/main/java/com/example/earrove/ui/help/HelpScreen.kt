package com.example.earrove.ui.help

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.earrove.ui.common.EarRoveTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(navController: NavController) {
    Scaffold(
        topBar = {
            EarRoveTopAppBar(title = "帮助中心", onNavigateUp = { navController.popBackStack() })
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            HelpSection(
                title = "智能导航",
                content = "通过语音输入目的地，应用将为您提供实时步行导航。在导航过程中，应用会通过语音和震动提示您前方的障碍物、红绿灯以及转向信息。"
            )

            HelpSection(
                title = "文字识别",
                content = "将摄像头对准需要识别的文字，如路牌、菜单或药品说明，应用将实时朗读识别到的内容。此功能完全在本地运行，保护您的隐私安全。"
            )

            HelpSection(
                title = "震动反馈说明",
                content = "- 障碍物警示：两次短促的震动\n- 转向提示：一次长震动"
            )
        }
    }
}

@Composable
private fun HelpSection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
