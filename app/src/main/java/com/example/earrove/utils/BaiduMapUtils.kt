package com.example.earrove.utils

import android.util.Log
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
import com.baidu.mapapi.search.poi.PoiSortType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object BaiduMapUtils {
    private const val TAG = "BaiduMapUtils"

    /** POI 周边搜索默认半径（米） */
    private const val POI_SEARCH_RADIUS = 50000 // 50公里

    /**
     * 传统地理编码（不考虑当前位置），作为备用方案
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
                // 不需要实现
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
     * 基于当前位置的 POI 周边搜索，优先返回距离用户最近的结果。
     * 如果周边搜索无结果，自动回退到全国地理编码。
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
     * 地理编码备用（当 POI 搜索无结果时使用）
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
