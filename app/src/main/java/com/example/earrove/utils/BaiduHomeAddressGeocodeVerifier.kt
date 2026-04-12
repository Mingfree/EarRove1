package com.example.earrove.utils

import com.example.earrove.domain.validation.HomeAddressGeocodeOutcome
import com.example.earrove.domain.validation.HomeAddressGeocodeVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BaiduHomeAddressGeocodeVerifier : HomeAddressGeocodeVerifier {

    override suspend fun verify(address: String): HomeAddressGeocodeOutcome = withContext(Dispatchers.IO) {
        try {
            val loc = BaiduMapUtils.resolveAddressOrPoiToLatLng(address)
            if (loc != null) HomeAddressGeocodeOutcome.Resolved else HomeAddressGeocodeOutcome.NotFound
        } catch (_: Exception) {
            HomeAddressGeocodeOutcome.Error
        }
    }
}
