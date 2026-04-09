package com.example.earrove.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.earrove.MainActivity
import com.example.earrove.R
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeNavigationAndSettingsTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun presetPrivacyAgreements() {
            val ctx = InstrumentationRegistry.getInstrumentation().targetContext
            ctx.getSharedPreferences("earrove_privacy_preferences", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("app_privacy_agreed", true)
                .putBoolean("baidu_map_privacy_agreed", true)
                .apply()
        }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun home_to_settings_save_home_address_persists() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val settingsText = ctx.getString(R.string.home_settings)
        val settingsTitle = ctx.getString(R.string.settings_title)

        composeRule.onNodeWithText(settingsText).performClick()
        composeRule.onNodeWithText(settingsTitle).assertIsDisplayed()

        val address = "北京市海淀区XX路1号"
        composeRule.onAllNodes(hasSetTextAction())[0]
            .performTextClearance()
        composeRule.onAllNodes(hasSetTextAction())[0]
            .performTextInput(address)

        composeRule.onNodeWithText("保存家地址").performClick()

        // 返回首页
        composeRule.onNodeWithContentDescription(ctx.getString(R.string.a11y_navigate_up)).performClick()
        composeRule.onNodeWithText(settingsText).assertIsDisplayed()

        // 再次进入设置，验证值仍存在
        composeRule.onNodeWithText(settingsText).performClick()
        composeRule.onNodeWithText(settingsTitle).assertIsDisplayed()
        composeRule.onNodeWithText(address).assertIsDisplayed()
    }
}

