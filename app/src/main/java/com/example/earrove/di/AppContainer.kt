package com.example.earrove.di

import android.app.Application
import com.example.earrove.data.privacy.PrivacyRepository
import com.example.earrove.data.privacy.PrivacyRepositoryImpl
import com.example.earrove.data.settings.SettingsRepository
import com.example.earrove.data.settings.SettingsRepositoryImpl
import com.example.earrove.domain.usecase.privacy.PrivacyConsentInteractor
import com.example.earrove.domain.usecase.settings.SettingsInteractor
import com.example.earrove.domain.validation.HomeAddressGeocodeVerifier
import com.example.earrove.utils.BaiduHomeAddressGeocodeVerifier

/**
 * 应用级依赖入口，单例仓库与用例，避免各 Composable 重复 [remember] 拼装。
 */
class AppContainer(application: Application) {

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(application)
    }

    val privacyRepository: PrivacyRepository by lazy {
        PrivacyRepositoryImpl(application)
    }

    val settingsInteractor: SettingsInteractor by lazy {
        SettingsInteractor(settingsRepository)
    }

    val privacyInteractor: PrivacyConsentInteractor by lazy {
        PrivacyConsentInteractor(privacyRepository)
    }

    val homeAddressGeocodeVerifier: HomeAddressGeocodeVerifier by lazy {
        BaiduHomeAddressGeocodeVerifier()
    }
}
