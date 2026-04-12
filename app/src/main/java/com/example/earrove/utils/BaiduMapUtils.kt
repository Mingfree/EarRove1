package com.example.earrove.utils

import android.location.Location
import android.util.Log
import kotlin.math.roundToInt
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.core.SearchResult
import com.baidu.mapapi.search.geocode.GeoCodeResult
import com.baidu.mapapi.search.geocode.GeoCoder
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener
import com.baidu.mapapi.search.poi.PoiDetailResult
import com.baidu.mapapi.search.poi.PoiDetailSearchResult
import com.baidu.mapapi.search.poi.PoiIndoorResult
import com.baidu.mapapi.search.poi.PoiNearbySearchOption
import com.baidu.mapapi.search.poi.PoiResult
import com.baidu.mapapi.search.poi.PoiSearch
import com.baidu.mapapi.search.poi.PoiCitySearchOption
import com.baidu.mapapi.search.poi.PoiSortType
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener
import com.baidu.mapapi.search.sug.SuggestionResult
import com.baidu.mapapi.search.sug.SuggestionSearch
import com.baidu.mapapi.search.sug.SuggestionSearchOption
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object BaiduMapUtils {
    private const val TAG = "BaiduMapUtils"

    /**
     * 与正向地理编码 [GeoCodeOption.city] 搭配：地址里已含「xx市」时，应优先用该市作为检索城市；
     * 全程使用「全国」时，易出现「北京市东城区…」这类完整地址解析失败。
     */
    private val GEO_CITY_PREFIXES: List<String> = listOf(
        "北京市", "上海市", "天津市", "重庆市",
        "广州市", "深圳市", "南京市", "成都市", "杭州市", "武汉市", "西安市", "苏州市",
        "郑州市", "长沙市", "沈阳市", "青岛市", "合肥市", "佛山市", "东莞市", "宁波市",
        "无锡市", "昆明市", "福州市", "石家庄市", "哈尔滨市", "长春市", "厦门市", "南宁市",
        "温州市", "常州市", "金华市", "烟台市", "泉州市", "唐山市", "大连市", "南昌市",
        "贵阳市", "海口市", "兰州市", "银川市", "西宁市", "乌鲁木齐市", "拉萨市", "呼和浩特市",
        "中山市", "惠州市", "保定市", "临沂市", "济宁市", "漳州市", "盐城市", "廊坊市"
    ).sortedByDescending { it.length }

    /** 默认 50km，优先覆盖同城常见目的地。 */
    private const val POI_SEARCH_RADIUS = 50000 // 50公里

    private fun inferGeoCityPrefix(address: String): String? =
        GEO_CITY_PREFIXES.firstOrNull { address.startsWith(it) }

    /**
     * 周边检索返回的 [com.baidu.mapapi.search.core.PoiInfo.distance] 在部分机型/SDK 上恒为 0，
     * 此时用当前检索中心点与 POI 坐标计算直线距离（米）作为展示与排序依据。
     */
    private fun distanceFromSearchCenterMeters(
        center: LatLng,
        poiLatLng: LatLng,
        sdkDistanceMeters: Int?
    ): Int {
        val d = sdkDistanceMeters
        if (d != null && d > 0) return d
        val results = FloatArray(1)
        Location.distanceBetween(
            center.latitude, center.longitude,
            poiLatLng.latitude, poiLatLng.longitude,
            results
        )
        return results[0].roundToInt().coerceAtLeast(0)
    }

    /**
     * 正向地理编码。[geoCity] 应与地址行政范围一致；完整「北京市xx区…」类输入请传「北京市」，勿固定「全国」。
     */
    fun geocodeAddress(address: String, geoCity: String = AppConfig.DEFAULT_CITY): Flow<LatLng?> = callbackFlow {
        val geoCoder = GeoCoder.newInstance()

        val listener = object : OnGetGeoCoderResultListener {
            override fun onGetGeoCodeResult(result: GeoCodeResult?) {
                if (result?.error == SearchResult.ERRORNO.NO_ERROR) {
                    Log.d(TAG, "地址解析成功: ${result.location.latitude}, ${result.location.longitude}")
                    trySend(result.location)
                } else {
                    Log.e(TAG, "地址解析失败: ${result?.error}")
                    trySend(null)
                }
            }

            override fun onGetReverseGeoCodeResult(result: ReverseGeoCodeResult?) {
                // 这里只走正向地理编码，逆地理结果忽略
            }
        }

        geoCoder.setOnGetGeoCodeResultListener(listener)
        geoCoder.geocode(
            com.baidu.mapapi.search.geocode.GeoCodeOption()
                .address(address)
                .city(geoCity)
        )

        awaitClose {
            // 仅在此处释放；勿在回调里 destroy，否则 first() 结束触发 awaitClose 会二次 destroy 抛异常
            runCatching { geoCoder.destroy() }
        }
    }

    private suspend fun geocodeWithCityVariants(addr: String): LatLng? {
        if (addr.isBlank()) return null
        val cities = buildList {
            inferGeoCityPrefix(addr)?.let { add(it) }
            add(AppConfig.DEFAULT_CITY)
        }.distinct()
        val variants = buildList {
            add(addr)
            inferGeoCityPrefix(addr)?.let { prefix ->
                if (addr.startsWith(prefix)) {
                    val rest = addr.removePrefix(prefix).trim()
                    if (rest.length >= 2) add(rest)
                }
            }
        }.distinct()
        for (city in cities) {
            for (v in variants) {
                geocodeAddress(v, city).first()?.let { return it }
            }
        }
        return null
    }

    /**
     * 将用户输入解析为坐标：先正向地理编码（适合门牌），失败则 Sug 联想（适合高校/校区/POI 名），
     * 再按需做城市内 POI 检索。与保存家地址、导航「家」共用，避免仅地理编码无法识别地名。
     * 结果会过滤掉境外误匹配点，避免乱码解析到国外仍被当作「家」。
     */
    suspend fun resolveAddressOrPoiToLatLng(address: String): LatLng? {
        val raw = resolveAddressOrPoiToLatLngRaw(address)
        return raw?.takeIf { isPlausibleDomesticNavigationTarget(it) }
    }

    private suspend fun resolveAddressOrPoiToLatLngRaw(address: String): LatLng? {
        val trimmed = address.trim()
        if (trimmed.isEmpty()) return null

        geocodeWithCityVariants(trimmed)?.let { return it }

        val compact = trimmed.replace(Regex("\\s+"), "")
        if (compact.length >= 4 && compact != trimmed) {
            geocodeWithCityVariants(compact)?.let { return it }
        }

        firstLatLngFromSuggestion(trimmed)?.let { return it }

        if (compact.length >= 4 && compact != trimmed) {
            firstLatLngFromSuggestion(compact)?.let { return it }
        }

        val city = inferCityForPoiCitySearch(trimmed)
        if (city != null) {
            firstLatLngFromPoiCity(trimmed, city)?.let { return it }
            if (compact.isNotBlank()) {
                firstLatLngFromPoiCity(compact, city)?.let { return it }
            }
        }

        return null
    }

    /**
     * 百度坐标下大致中国（含港澳台）范围，过滤乱码/误检索命中的境外坐标。
     */
    fun isPlausibleDomesticNavigationTarget(latLng: LatLng): Boolean {
        val lat = latLng.latitude
        val lng = latLng.longitude
        return lat in 17.2..55.0 && lng in 72.0..136.0
    }

    private suspend fun firstLatLngFromSuggestion(keyword: String): LatLng? =
        suspendCancellableCoroutine { cont ->
            if (keyword.isBlank()) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            val sug = SuggestionSearch.newInstance()
            fun finish(value: LatLng?) {
                runCatching { sug.destroy() }
                if (cont.isActive) cont.resume(value)
            }
            sug.setOnGetSuggestionResultListener(object : OnGetSuggestionResultListener {
                override fun onGetSuggestionResult(result: SuggestionResult?) {
                    val pt = if (result != null && result.error == SearchResult.ERRORNO.NO_ERROR) {
                        result.allSuggestions
                            ?.mapNotNull { it.pt }
                            ?.firstOrNull()
                    } else {
                        null
                    }
                    finish(pt)
                }
            })
            val ok = runCatching {
                val sugCity = inferGeoCityPrefix(keyword) ?: "全国"
                val option = SuggestionSearchOption()
                    .keyword(keyword)
                    .city(sugCity)
                    .citylimit(false)
                sug.requestSuggestion(option)
            }.getOrElse { e ->
                Log.e(TAG, "Sug 检索发起失败", e)
                false
            }
            if (!ok) finish(null)
            cont.invokeOnCancellation { runCatching { sug.destroy() } }
        }

    private suspend fun firstLatLngFromPoiCity(keyword: String, city: String): LatLng? =
        suspendCancellableCoroutine { cont ->
            if (keyword.isBlank()) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            val poiSearch = PoiSearch.newInstance()
            fun finish(value: LatLng?) {
                runCatching { poiSearch.destroy() }
                if (cont.isActive) cont.resume(value)
            }
            poiSearch.setOnGetPoiSearchResultListener(object : OnGetPoiSearchResultListener {
                override fun onGetPoiResult(result: PoiResult?) {
                    val pt = if (result?.error == SearchResult.ERRORNO.NO_ERROR
                        && result.allPoi != null
                        && result.allPoi.isNotEmpty()
                    ) {
                        result.allPoi[0].location
                    } else {
                        null
                    }
                    finish(pt)
                }

                override fun onGetPoiDetailResult(result: PoiDetailResult?) {}
                override fun onGetPoiDetailResult(result: PoiDetailSearchResult?) {}
                override fun onGetPoiIndoorResult(result: PoiIndoorResult?) {}
            })
            val ok = runCatching {
                poiSearch.searchInCity(
                    PoiCitySearchOption()
                        .city(city)
                        .keyword(keyword)
                        .pageNum(0)
                        .pageCapacity(10)
                )
            }.getOrElse { e ->
                Log.e(TAG, "城市 POI 检索发起失败", e)
                false
            }
            if (!ok) finish(null)
            cont.invokeOnCancellation { runCatching { poiSearch.destroy() } }
        }

    /**
     * 无「xx市」时根据常见关键词推断城市，用于 [firstLatLngFromPoiCity] 兜底。
     */
    private fun inferCityForPoiCitySearch(address: String): String? {
        when {
            address.contains("北京") -> return "北京市"
            address.contains("上海") -> return "上海市"
            address.contains("天津") -> return "天津市"
            address.contains("重庆") -> return "重庆市"
            address.contains("广州") -> return "广州市"
            address.contains("深圳") -> return "深圳市"
            address.contains("中央民族大学") -> return "北京市"
        }
        return null
    }

    /**
     * 基于当前位置的 POI 周边搜索，优先返回距离用户最近的结果。
     * 如果周边搜索无结果，自动回退到全国地理编码，尽量避免直接失败。
     *
     * @param keyword  搜索关键词（地名）
     * @param center   当前位置（经纬度）
     * @param radius   搜索半径（米），默认 50km
     */
    fun searchNearby(
        keyword: String,
        center: LatLng,
        radius: Int = POI_SEARCH_RADIUS
    ): Flow<LatLng?> = callbackFlow {
        val poiSearch = PoiSearch.newInstance()

        val listener = object : OnGetPoiSearchResultListener {
            override fun onGetPoiResult(result: PoiResult?) {
                if (result?.error == SearchResult.ERRORNO.NO_ERROR
                    && result.allPoi != null
                    && result.allPoi.isNotEmpty()
                ) {
                    // 结果已按距离排序，取第一个（最近）
                    val nearest = result.allPoi[0]
                    Log.d(TAG, "POI 周边搜索成功: ${nearest.name} (${nearest.location.latitude}, ${nearest.location.longitude})")
                    trySend(nearest.location)
                } else {
                    Log.w(TAG, "POI 周边搜索无结果，回退到地理编码: ${result?.error}")
                    // 回退到全国地理编码
                    fallbackGeocode(keyword) { location ->
                        trySend(location)
                    }
                }
                poiSearch.destroy()
            }

            override fun onGetPoiDetailResult(result: PoiDetailResult?) {}
            override fun onGetPoiDetailResult(result: PoiDetailSearchResult?) {}
            override fun onGetPoiIndoorResult(result: PoiIndoorResult?) {}
        }

        poiSearch.setOnGetPoiSearchResultListener(listener)
        poiSearch.searchNearby(
            PoiNearbySearchOption()
                .keyword(keyword)
                .location(center)
                .radius(radius)
                .sortType(PoiSortType.distance_from_near_to_far) // 由近到远排序
                .pageNum(0)
                .pageCapacity(5)
        )

        awaitClose {
            poiSearch.destroy()
        }
    }

    /**
     * 获取当前位置附近的推荐目的地名称（用于导航页推荐列表）。
     */
    fun searchNearbyPoiNames(
        center: LatLng,
        keyword: String = "生活服务",
        radius: Int = 3000,
        limit: Int = 8
    ): Flow<List<String>> = callbackFlow {
        val poiSearch = PoiSearch.newInstance()
        val listener = object : OnGetPoiSearchResultListener {
            override fun onGetPoiResult(result: PoiResult?) {
                val names = if (result?.error == SearchResult.ERRORNO.NO_ERROR) {
                    result.allPoi
                        ?.mapNotNull { it.name?.trim() }
                        ?.filter { it.isNotBlank() }
                        ?.distinct()
                        ?.take(limit)
                        .orEmpty()
                } else {
                    emptyList()
                }
                trySend(names)
                poiSearch.destroy()
            }

            override fun onGetPoiDetailResult(result: PoiDetailResult?) {}
            override fun onGetPoiDetailResult(result: PoiDetailSearchResult?) {}
            override fun onGetPoiIndoorResult(result: PoiIndoorResult?) {}
        }

        poiSearch.setOnGetPoiSearchResultListener(listener)
        poiSearch.searchNearby(
            PoiNearbySearchOption()
                .keyword(keyword)
                .location(center)
                .radius(radius)
                .sortType(PoiSortType.distance_from_near_to_far)
                .pageNum(0)
                .pageCapacity(limit)
        )

        awaitClose {
            poiSearch.destroy()
        }
    }

    /**
     * 获取附近候选 POI 列表（名称 + 坐标 + 距离），按距离从近到远排序。
     */
    fun searchNearbyPoiCandidates(
        keyword: String,
        center: LatLng,
        radius: Int = 3000,
        limit: Int = 5
    ): Flow<List<NearbyPoiCandidate>> = callbackFlow {
        val poiSearch = PoiSearch.newInstance()
        val listener = object : OnGetPoiSearchResultListener {
            override fun onGetPoiResult(result: PoiResult?) {
                val candidates = if (result?.error == SearchResult.ERRORNO.NO_ERROR) {
                    result.allPoi
                        ?.asSequence()
                        ?.mapNotNull { poi ->
                            val name = poi.name?.trim()
                            val location = poi.location
                            if (name.isNullOrBlank() || location == null) {
                                null
                            } else {
                                NearbyPoiCandidate(
                                    name = name,
                                    location = location,
                                    distanceMeters = distanceFromSearchCenterMeters(
                                        center,
                                        location,
                                        poi.distance
                                    )
                                )
                            }
                        }
                        ?.sortedBy { it.distanceMeters }
                        ?.distinctBy { it.name to it.location.latitude to it.location.longitude }
                        ?.take(limit)
                        ?.toList()
                        .orEmpty()
                } else {
                    emptyList()
                }
                trySend(candidates)
                poiSearch.destroy()
            }

            override fun onGetPoiDetailResult(result: PoiDetailResult?) {}
            override fun onGetPoiDetailResult(result: PoiDetailSearchResult?) {}
            override fun onGetPoiIndoorResult(result: PoiIndoorResult?) {}
        }

        poiSearch.setOnGetPoiSearchResultListener(listener)
        poiSearch.searchNearby(
            PoiNearbySearchOption()
                .keyword(keyword)
                .location(center)
                .radius(radius)
                .sortType(PoiSortType.distance_from_near_to_far)
                .pageNum(0)
                .pageCapacity(limit)
        )

        awaitClose {
            poiSearch.destroy()
        }
    }

    /**
     * 地理编码备用（当 POI 搜索无结果时使用，避免直接报错）
     */
    private fun fallbackGeocode(address: String, callback: (LatLng?) -> Unit) {
        val geoCoder = GeoCoder.newInstance()
        geoCoder.setOnGetGeoCodeResultListener(object : OnGetGeoCoderResultListener {
            override fun onGetGeoCodeResult(result: GeoCodeResult?) {
                if (result?.error == SearchResult.ERRORNO.NO_ERROR) {
                    Log.d(TAG, "地理编码备用成功: ${result.location.latitude}, ${result.location.longitude}")
                    callback(result.location)
                } else {
                    Log.e(TAG, "地理编码备用也失败: ${result?.error}")
                    callback(null)
                }
                geoCoder.destroy()
            }

            override fun onGetReverseGeoCodeResult(result: ReverseGeoCodeResult?) {}
        })
        geoCoder.geocode(
            com.baidu.mapapi.search.geocode.GeoCodeOption()
                .address(address)
                .city(AppConfig.DEFAULT_CITY)
        )
    }
}

data class NearbyPoiCandidate(
    val name: String,
    val location: LatLng,
    val distanceMeters: Int
)
