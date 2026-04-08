package com.example.earrove.navigation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.BitmapDescriptorFactory
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.MapView
import com.baidu.mapapi.map.MarkerOptions
import com.baidu.mapapi.map.MyLocationConfiguration
import com.baidu.mapapi.map.MyLocationData
import com.baidu.mapapi.map.OverlayOptions
import com.baidu.mapapi.map.PolylineOptions
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.core.SearchResult
import com.baidu.mapapi.search.route.BikingRouteResult
import com.baidu.mapapi.search.route.DrivingRouteResult
import com.baidu.mapapi.search.route.IndoorRouteResult
import com.baidu.mapapi.search.route.IntegralRouteResult
import com.baidu.mapapi.search.route.MassTransitRouteResult
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener
import com.baidu.mapapi.search.route.PlanNode
import com.baidu.mapapi.search.route.RoutePlanSearch
import com.baidu.mapapi.search.route.TransitRouteResult
import com.baidu.mapapi.search.route.WalkingRouteLine
import com.baidu.mapapi.search.route.WalkingRoutePlanOption
import com.baidu.mapapi.search.route.WalkingRouteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NavigationStep(
    val instruction: String,
    val distance: Int, // 米
    val duration: Int, // 秒
    val turnType: String,
    val points: List<LatLng> = emptyList()
)

data class RouteInfo(
    val totalDistance: Int,
    val totalDuration: Int,
    val steps: List<NavigationStep>,
    val destination: String,
    val allPoints: List<LatLng> = emptyList()
)

class NavigationService(context: Context) {
    private val routePlanSearch = RoutePlanSearch.newInstance()
    private val _currentRoute = MutableStateFlow<RouteInfo?>(null)
    private val _currentStepIndex = MutableStateFlow(0)
    private val _isNavigating = MutableStateFlow(false)
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    private val _destinationLocation = MutableStateFlow<LatLng?>(null)

    private var mapView: MapView? = null
    private var baiduMap: BaiduMap? = null

    val currentRoute: StateFlow<RouteInfo?> = _currentRoute.asStateFlow()
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()
    val destinationLocation: StateFlow<LatLng?> = _destinationLocation.asStateFlow()

    init {
        routePlanSearch.setOnGetRoutePlanResultListener(object : OnGetRoutePlanResultListener {
            override fun onGetWalkingRouteResult(result: WalkingRouteResult?) {
                if (result?.error == SearchResult.ERRORNO.NO_ERROR && result.routeLines != null && result.routeLines.isNotEmpty()) {
                    // 修改1: routePlans -> routeLines
                    val route = result.routeLines[0] as WalkingRouteLine

                    val steps = mutableListOf<NavigationStep>()
                    val allPoints = mutableListOf<LatLng>()

                    route.allStep.forEach { step ->
                        val points = step.wayPoints ?: emptyList()
                        allPoints.addAll(points)

                        steps.add(
                            NavigationStep(
                                instruction = step.instructions ?: "继续前进",
                                distance = step.distance,
                                duration = step.duration,
                                turnType = parseTurnType(step.instructions ?: ""),
                                points = points
                            )
                        )
                    }

                    val routeInfo = RouteInfo(
                        totalDistance = route.distance,
                        totalDuration = route.duration,
                        steps = steps,
                        destination = _currentRoute.value?.destination ?: "目的地",
                        allPoints = allPoints
                    )

                    _currentRoute.value = routeInfo
                    _currentStepIndex.value = 0

                    // 在地图上绘制路线
                    drawRouteOnMap(allPoints)

                    Log.d("NavigationService", "路线规划成功: ${steps.size}步，总距离${route.distance}米")
                } else {
                    Log.e("NavigationService", "路线规划失败: ${result?.error}")
                }
            }

            override fun onGetTransitRouteResult(p0: TransitRouteResult?) {}
            override fun onGetMassTransitRouteResult(p0: MassTransitRouteResult?) {}
            override fun onGetDrivingRouteResult(p0: DrivingRouteResult?) {}
            override fun onGetIndoorRouteResult(p0: IndoorRouteResult?) {}
            override fun onGetBikingRouteResult(p0: BikingRouteResult?) {}
            override fun onGetIntegralRouteResult(p0: IntegralRouteResult?) {}
        })
    }

    fun planRoute(start: LatLng, end: LatLng, destinationName: String) {
        val startNode = PlanNode.withLocation(start)
        val endNode = PlanNode.withLocation(end)

        _destinationLocation.value = end

        // 修复1: WalkingRoutePlanOption() 的正确调用方式
        val walkingRoutePlanOption = WalkingRoutePlanOption()
        walkingRoutePlanOption.from(startNode)
        walkingRoutePlanOption.to(endNode)

        // 修复2: walkingRouteSearch() 的正确调用
        routePlanSearch.walkingSearch(walkingRoutePlanOption)
    }

    fun startNavigation(destination: String) {
        _isNavigating.value = true
        _currentRoute.value = _currentRoute.value?.copy(destination = destination)
    }

    fun moveToNextStep(): NavigationStep? {
        val route = _currentRoute.value ?: return null
        val currentIndex = _currentStepIndex.value

        return if (currentIndex < route.steps.size - 1) {
            _currentStepIndex.value = currentIndex + 1
            route.steps[currentIndex + 1]
        } else {
            // 到达目的地
            _isNavigating.value = false
            null
        }
    }

    fun getCurrentStep(): NavigationStep? {
        val route = _currentRoute.value ?: return null
        val index = _currentStepIndex.value
        return if (index < route.steps.size) route.steps[index] else null
    }

    fun updateCurrentLocation(location: LatLng) {
        _currentLocation.value = location
    }

    fun stopNavigation() {
        _isNavigating.value = false
        _currentRoute.value = null
        _currentStepIndex.value = 0
        _destinationLocation.value = null

        // 清除地图上的路线
        clearRouteFromMap()
    }

    fun setupMapView(mapView: MapView) {
        this.mapView = mapView
        this.baiduMap = mapView.map

        baiduMap?.apply {
            isMyLocationEnabled = true
            uiSettings.isCompassEnabled = true
            // 修复3: 使用正确的属性名 - 通常是isZoomGesturesEnabled
            uiSettings.isZoomGesturesEnabled = true

            // 设置定位配置
            val config = MyLocationConfiguration(
                MyLocationConfiguration.LocationMode.NORMAL,
                true,
                null
            )
            setMyLocationConfiguration(config)
        }
    }

    private fun drawRouteOnMap(points: List<LatLng>) {
        if (points.isEmpty() || baiduMap == null) return

        baiduMap?.clear()

        // 绘制路线
        val polylineOptions = PolylineOptions()
            .width(10)
            .color(0xAAFFD700.toInt()) // 金色
            .points(points)

        baiduMap?.addOverlay(polylineOptions)

        if (points.isNotEmpty()) {
            // 起点标记（绿色圆点）
            val startOverlay = MarkerOptions()
                .position(points.first())
                .icon(BitmapDescriptorFactory.fromBitmap(createMarkerBitmap(Color.GREEN)))
            baiduMap?.addOverlay(startOverlay)

            // 终点标记（红色圆点）
            val endOverlay = MarkerOptions()
                .position(points.last())
                .icon(BitmapDescriptorFactory.fromBitmap(createMarkerBitmap(Color.RED)))
            baiduMap?.addOverlay(endOverlay)
        }

        // 调整地图视野 - 修正3: 正确的MapStatusUpdateFactory调用
        if (points.size >= 2) {
            val boundsBuilder = com.baidu.mapapi.model.LatLngBounds.Builder()
                .include(points.first())
                .include(points.last())

            val bounds = boundsBuilder.build()
            val update = MapStatusUpdateFactory.newLatLngBounds(bounds)
            baiduMap?.animateMapStatus(update)
        }
    }

    private fun clearRouteFromMap() {
        baiduMap?.clear()
    }

    private fun createMarkerBitmap(color: Int): Bitmap {
        val size = 40
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            this.color = color
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return bitmap
    }

    private fun parseTurnType(instruction: String): String {
        return when {
            instruction.contains("左转") -> "LEFT"
            instruction.contains("右转") -> "RIGHT"
            instruction.contains("直行") || instruction.contains("前进") -> "STRAIGHT"
            instruction.contains("到达") || instruction.contains("目的") -> "ARRIVE"
            else -> "CONTINUE"
        }
    }

    fun release() {
        routePlanSearch.destroy()
        mapView?.onDestroy()
    }
}