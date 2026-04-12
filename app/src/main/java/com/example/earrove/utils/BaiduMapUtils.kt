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

    /** 默认 50km，优先覆盖同城常见目的地。 */
    private const val POI_SEARCH_RADIUS = 50000 // 50公里

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
     * 全国地理编码备用方案：没有当前位置时也能给出结果。
     */
    fun geocodeAddress(address: String): Flow<LatLng?> = callbackFlow {
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
                geoCoder.destroy()
            }

            override fun onGetReverseGeoCodeResult(result: ReverseGeoCodeResult?) {
                // 这里只走正向地理编码，逆地理结果忽略
            }
        }

        geoCoder.setOnGetGeoCodeResultListener(listener)
        geoCoder.geocode(
            com.baidu.mapapi.search.geocode.GeoCodeOption()
                .address(address)
                .city(AppConfig.DEFAULT_CITY)
        )

        awaitClose {
            geoCoder.destroy()
        }
    }

    /**
     * 将用户输入解析为坐标：先正向地理编码（适合门牌），失败则 Sug 联想（适合高校/校区/POI 名），
     * 再按需做城市内 POI 检索。与保存家地址、导航「家」共用，避免仅地理编码无法识别地名。
     */
    fun resolveAddressOrPoiToLatLng(address: String): Flow<LatLng?> = callbackFlow {
        val trimmed = address.trim()
        if (trimmed.isEmpty()) {
            trySend(null)
            return@callbackFlow
        }

        var loc = geocodeAddress(trimmed).first()
        if (loc != null) {
            trySend(loc)
            return@callbackFlow
        }

        val compact = trimmed.replace(Regex("\\s+"), "")
        if (compact.length >= 4 && compact != trimmed) {
            loc = geocodeAddress(compact).first()
            if (loc != null) {
                trySend(loc)
                return@callbackFlow
            }
        }

        loc = firstLatLngFromSuggestion(trimmed)
        if (loc != null) {
            trySend(loc)
            return@callbackFlow
        }

        if (compact.length >= 4 && compact != trimmed) {
            loc = firstLatLngFromSuggestion(compact)
            if (loc != null) {
                trySend(loc)
                return@callbackFlow
            }
        }

        val city = inferCityForPoiCitySearch(trimmed)
        if (city != null) {
            loc = firstLatLngFromPoiCity(trimmed, city) ?: firstLatLngFromPoiCity(compact, city)
            if (loc != null) {
                trySend(loc)
                return@callbackFlow
            }
        }

        trySend(null)
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
                val option = SuggestionSearchOption()
                    .keyword(keyword)
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
