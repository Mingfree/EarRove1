package com.example.earrove.domain.validation

/**
 * 保存家地址前用与导航一致的地理编码校验地址是否可被解析。
 */
enum class HomeAddressGeocodeOutcome {
    Resolved,
    NotFound,
    Error
}

fun interface HomeAddressGeocodeVerifier {
    suspend fun verify(address: String): HomeAddressGeocodeOutcome
}
