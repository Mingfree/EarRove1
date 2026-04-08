package com.example.earrove.utils

import android.content.Context
import android.util.Log
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.model.LatLng
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// 不再需要 BaiduMapInitializer 对象，因为已经在 Application 中初始化了

class LocationManager(context: Context) {
    private val locationClient: LocationClient
    private var isStarted = false

    init {
        try {
            val appContext = context.applicationContext

            Log.d("LocationManager", "开始创建 LocationClient")
            LocationClient.setAgreePrivacy(true)

            SDKInitializer.setAgreePrivacy(appContext, true)
            Log.d("LocationManager", "SDKInitializer隐私政策已设置")

            // 添加延迟
            Thread.sleep(100)

            // 检查SDK初始化状态
            val sdkInitialized = SDKInitializer.isInitialized()
            Log.d("LocationManager", "SDK初始化状态: $sdkInitialized")

            // 再次设置隐私政策
            SDKInitializer.setAgreePrivacy(appContext, true)
            Log.d("LocationManager", "SDKInitializer隐私政策已再次确认")

            // 再次延迟
            Thread.sleep(100)

            // 尝试创建LocationClient
            Log.d("LocationManager", "准备创建LocationClient")
            locationClient = LocationClient(appContext)
            Log.d("LocationManager", "LocationClient 创建成功")

            Log.d("LocationManager", "LocationClient 初始化完成")

            val option = LocationClientOption().apply {
                locationMode = LocationClientOption.LocationMode.Hight_Accuracy
                setCoorType("bd09ll")
                setScanSpan(2000)
                setIsNeedAddress(true)
                isNeedNewVersionRgc = true
                setIsNeedLocationDescribe(true)
                setIsNeedAltitude(true)
                setOpenGps(true)
                setLocationNotify(true)
            }
            locationClient.locOption = option

            Log.d("LocationManager", "LocationClient 配置完成")
        } catch (e: Exception) {
            Log.e("LocationManager", "创建 LocationClient 失败: ${e.message}")
            throw RuntimeException("无法创建定位客户端: ${e.message}")
        }
    }

    fun startLocation(): Flow<BDLocation> = callbackFlow {
        val listener = object : BDAbstractLocationListener() {
            override fun onReceiveLocation(location: BDLocation) {
                trySend(location)
            }
        }

        try {
            locationClient.registerLocationListener(listener)
            locationClient.start()
            isStarted = true
            Log.d("LocationManager", "定位服务启动成功")
        } catch (e: Exception) {
            Log.e("LocationManager", "启动定位服务失败: ${e.message}")
        }

        awaitClose {
            try {
                locationClient.unRegisterLocationListener(listener)
                locationClient.stop()
                isStarted = false
                Log.d("LocationManager", "定位服务停止成功")
            } catch (e: Exception) {
                Log.e("LocationManager", "停止定位服务失败: ${e.message}")
            }
        }
    }

    fun stopLocation() {
        if (isStarted) {
            try {
                locationClient.stop()
                isStarted = false
            } catch (e: Exception) {
                Log.e("LocationManager", "停止定位失败: ${e.message}")
            }
        }
    }
}