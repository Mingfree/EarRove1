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
import org.junit.Before
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

    /**
     * 预授权运行时权限，避免进入导航/OCR 页时触发系统权限弹窗影响稳定性。
     * 需在 Activity 启动后执行，故放在 @Before 而非 @BeforeClass。
     */
    @Before
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

    @Test
    fun home_to_settings_save_home_address_persists() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        dismissStartupGates()

        val settingsText = ctx.getString(R.string.home_settings)
        val settingsTitle = ctx.getString(R.string.settings_title)

        composeRule.onNodeWithContentDescription(settingsText).performClick()
        composeRule.onNodeWithText(settingsTitle).assertIsDisplayed()

        val address = "北京市东城区东长安街"
        composeRule.onAllNodes(hasSetTextAction())[0]
            .performTextClearance()
        composeRule.onAllNodes(hasSetTextAction())[0]
            .performTextInput(address)

        composeRule.onNodeWithText(ctx.getString(R.string.settings_home_address_save)).performClick()
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

    @Test
    fun home_to_navigation_screen_jumps() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        dismissStartupGates()

        composeRule.onNodeWithContentDescription(ctx.getString(R.string.a11y_home_nav_entry)).performClick()

        // 进入导航页：先经过 SDK 初始化门禁（"正在初始化导航服务..."），成功后再进入待机页。
        composeRule.waitUntil(timeoutMillis = 15_000) {
            nodeWithTextExists(ctx.getString(R.string.nav_init_navigation_service)) ||
                nodeWithTextExists(ctx.getString(R.string.nav_standby_prompt_title))
        }
    }

    @Test
    fun home_to_ocr_screen_jumps() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        dismissStartupGates()

        composeRule.onNodeWithContentDescription(ctx.getString(R.string.a11y_home_ocr_entry)).performClick()

        composeRule.waitUntil(timeoutMillis = 15_000) {
            nodeWithTextExists(ctx.getString(R.string.ocr_screen_title))
        }
    }

    /** 处理启动门禁（隐私同意 / 权限引导 / 配置缺失），直到主页出现。 */
    private fun dismissStartupGates() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val settingsText = ctx.getString(R.string.home_settings)
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
    }

    private fun nodeWithTextExists(text: String): Boolean = runCatching {
        composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
    }.getOrDefault(false)

    private fun nodeWithContentDescriptionExists(text: String): Boolean = runCatching {
        composeRule.onAllNodes(hasContentDescription(text)).fetchSemanticsNodes().isNotEmpty()
    }.getOrDefault(false)
}
