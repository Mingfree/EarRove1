plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

import java.util.Properties
import java.io.FileInputStream

android {
    namespace = "com.example.earrove"
    compileSdk = 36

    // 从根目录 local.properties 读取敏感配置（如果存在）
    val localProps = Properties().apply {
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            FileInputStream(localPropsFile).use { load(it) }
        }
    }

    defaultConfig {
        applicationId = "com.example.earrove"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // 百度地图配置 - 支持多种CPU架构
        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86")
            abiFilters.add("x86_64")
        }

        // 将敏感配置注入到 BuildConfig 和 Manifest 占位符
        val dashScopeKey = (localProps.getProperty("DASHSCOPE_API_KEY") ?: "sk-YOUR-DASHSCOPE-KEY").trim()
        val baiduMapKey = (localProps.getProperty("BAIDU_MAP_API_KEY") ?: "YOUR-BAIDU-MAP-KEY").trim()
        val baiduAppId = (localProps.getProperty("BAIDU_SPEECH_APP_ID") ?: "YOUR-BAIDU-SPEECH-APP-ID").trim()
        val baiduSpeechApiKey = (localProps.getProperty("BAIDU_SPEECH_API_KEY") ?: "YOUR-BAIDU-SPEECH-API-KEY").trim()
        val baiduSpeechSecretKey = (localProps.getProperty("BAIDU_SPEECH_SECRET_KEY") ?: "YOUR-BAIDU-SPEECH-SECRET-KEY").trim()
        val baiduTtsApiKey = (localProps.getProperty("BAIDU_TTS_API_KEY") ?: "YOUR-BAIDU-TTS-API-KEY").trim()
        val baiduTtsSecretKey = (localProps.getProperty("BAIDU_TTS_SECRET_KEY") ?: "YOUR-BAIDU-TTS-SECRET-KEY").trim()
        val arkApiKey = (localProps.getProperty("ARK_API_KEY") ?: "YOUR-ARK-API-KEY").trim()

        buildConfigField("String", "DASHSCOPE_API_KEY", "\"$dashScopeKey\"")
        buildConfigField("String", "BAIDU_MAP_API_KEY", "\"$baiduMapKey\"")
        buildConfigField("String", "BAIDU_SPEECH_APP_ID", "\"$baiduAppId\"")
        buildConfigField("String", "BAIDU_SPEECH_API_KEY", "\"$baiduSpeechApiKey\"")
        buildConfigField("String", "BAIDU_SPEECH_SECRET_KEY", "\"$baiduSpeechSecretKey\"")
        buildConfigField("String", "BAIDU_TTS_API_KEY", "\"$baiduTtsApiKey\"")
        buildConfigField("String", "BAIDU_TTS_SECRET_KEY", "\"$baiduTtsSecretKey\"")
        buildConfigField("String", "ARK_API_KEY", "\"$arkApiKey\"")

        manifestPlaceholders["BAIDU_MAP_API_KEY"] = baiduMapKey
        manifestPlaceholders["BAIDU_SPEECH_APP_ID"] = baiduAppId
        manifestPlaceholders["BAIDU_SPEECH_API_KEY"] = baiduSpeechApiKey
        manifestPlaceholders["BAIDU_SPEECH_SECRET_KEY"] = baiduSpeechSecretKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        jniLibs {
            // AIPE TTS SDK 需要以文件路径读取 so
            useLegacyPackaging = true
            // 多个 AAR 包含相同的 libc++_shared.so，取任意一个
            pickFirsts += listOf(
                "lib/arm64-v8a/libc++_shared.so",
                "lib/armeabi-v7a/libc++_shared.so",
                "lib/x86/libc++_shared.so",
                "lib/x86_64/libc++_shared.so"
            )
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }

    // 配置百度地图so文件位置
    sourceSets {
        getByName("main") {
            jniLibs.srcDir("libs")
        }
    }
}

dependencies {
    // 包含libs目录下所有jar和aar文件（百度SDK） - 使用更安全的写法
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    // AndroidX 核心库
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose 核心库
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // 导航组件
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // 协程（百度SDK需要）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // 添加 ConstraintLayout 依赖
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // 添加 Material Design 组件（包含 CardView）
    implementation("com.google.android.material:material:1.11.0")
    // 权限管理（无障碍功能需要）
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // 百度地图依赖库（确保与百度SDK兼容）
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okio:okio:3.6.0")

    // 位置服务（百度地图需要）- 使用兼容版本
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // CameraX（OCR功能需要）
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    implementation(libs.androidx.compose.foundation)

    // 测试库
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // 预览工具
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // 解决依赖冲突
    implementation("androidx.concurrent:concurrent-futures:1.1.0")
    implementation("androidx.annotation:annotation:1.7.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
}