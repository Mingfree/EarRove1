package com.example.earrove.ui

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
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

        @JvmStatic
        fun grantRuntimePermissionsForTest() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val packageName = instrumentation.targetContext.packageName
            val uiAutomation = instrumentation.uiAutomation
            val permissions = listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
            permissions.forEach { permission ->
                runCatching {
                    uiAutomation.grantRuntimePermission(packageName, permission)
                }
            }
        }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun home_to_settings_save_home_address_persists() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val settingsText = ctx.getString(R.string.home_settings)
        val settingsTitle = ctx.getString(R.string.settings_title)
        val permissionTitle = ctx.getString(R.string.startup_permission_title)
        val permissionContinue = ctx.getString(R.string.startup_permission_continue)
        val configMissingTitle = ctx.getString(R.string.startup_config_missing_title)
        val configMissingCta = ctx.getString(R.string.startup_config_missing_cta)
        val initializingText = ctx.getString(R.string.app_initializing_navigation)
        val privacyTitle = ctx.getString(R.string.privacy_app_title)
        val privacyAgree = ctx.getString(R.string.privacy_agree_and_start)

        composeRule.waitUntil(timeoutMillis = 10_000) {
            nodeWithContentDescriptionExists(settingsText) ||
                nodeWithTextExists(permissionTitle) ||
                nodeWithTextExists(privacyTitle) ||
                nodeWithTextExists(initializingText) ||
                nodeWithTextExists(configMissingTitle)
        }

        if (nodeWithTextExists(privacyTitle)) {
            val toggles = composeRule.onAllNodes(isToggleable()).fetchSemanticsNodes()
            if (toggles.size >= 2) {
                composeRule.onAllNodes(isToggleable())[0].performClick()
                composeRule.onAllNodes(isToggleable())[1].performClick()
            }
            composeRule.onNodeWithText(privacyAgree).performClick()
        }

        if (nodeWithTextExists(permissionTitle)) {
            composeRule.waitUntil(timeoutMillis = 10_000) { nodeWithTextExists(permissionContinue) }
            composeRule.onNodeWithText(permissionContinue).performClick()
        }

        if (nodeWithTextExists(configMissingTitle)) {
            composeRule.onNodeWithText(configMissingCta).performClick()
        }

        composeRule.waitUntil(timeoutMillis = 15_000) { nodeWithContentDescriptionExists(settingsText) }
        composeRule.onNodeWithContentDescription(settingsText).performClick()
        composeRule.onNodeWithText(settingsTitle).assertIsDisplayed()

        val address = "北京市东城区东长安街"
        composeRule.onAllNodes(hasSetTextAction())[0]
            .performTextClearance()
        composeRule.onAllNodes(hasSetTextAction())[0]
            .performTextInput(address)

        composeRule.onNodeWithText("保存家地址").performClick()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            nodeWithTextExists(ctx.getString(R.string.settings_home_address_saved))
        }

        // 返回首页
        composeRule.onNodeWithContentDescription(ctx.getString(R.string.a11y_navigate_up)).performClick()
        composeRule.onNodeWithContentDescription(settingsText).assertIsDisplayed()

        // 再次进入设置，验证值仍存在
        composeRule.onNodeWithContentDescription(settingsText).performClick()
        composeRule.onNodeWithText(settingsTitle).assertIsDisplayed()
        composeRule.onNodeWithText(address).assertIsDisplayed()
    }

    private fun nodeWithTextExists(text: String): Boolean = runCatching {
        composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
    }.getOrDefault(false)

    private fun nodeWithContentDescriptionExists(text: String): Boolean = runCatching {
        composeRule.onAllNodes(hasContentDescription(text)).fetchSemanticsNodes().isNotEmpty()
    }.getOrDefault(false)
}

