package com.example.habittracker.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import com.example.habittracker.data.LocationLabelRules
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class PrayerLocationFix(val label: String, val latitude: Double, val longitude: Double)
sealed interface PrayerLocationResult {
    data class Success(val fix: PrayerLocationFix) : PrayerLocationResult
    data object ServicesDisabled : PrayerLocationResult
    data object Unavailable : PrayerLocationResult
}

class CurrentLocationProvider(private val context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)

    @SuppressLint("MissingPermission")
    suspend fun obtain(): PrayerLocationResult {
        val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
            ?: return PrayerLocationResult.ServicesDisabled
        val location = withTimeoutOrNull(15_000) { currentLocation(provider) } ?: return PrayerLocationResult.Unavailable
        val label = withTimeoutOrNull(5_000) { readableLabel(location) } ?: "Current location"
        return PrayerLocationResult.Success(PrayerLocationFix(label, location.latitude, location.longitude))
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(provider: String): Location? = suspendCancellableCoroutine { continuation ->
        if (Build.VERSION.SDK_INT >= 30) {
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            manager.getCurrentLocation(provider, signal, Executor { it.run() }) { location ->
                if (continuation.isActive) continuation.resume(location)
            }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) { manager.removeUpdates(this); if (continuation.isActive) continuation.resume(location) }
                @Deprecated("Deprecated in Android") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) { manager.removeUpdates(this); if (continuation.isActive) continuation.resume(null) }
            }
            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun readableLabel(location: Location): String = withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            val address: Address? = if (Build.VERSION.SDK_INT >= 33) suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(location.latitude, location.longitude, 1) { results -> if (continuation.isActive) continuation.resume(results.firstOrNull()) }
            } else geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
            LocationLabelRules.label(address?.locality, address?.subAdminArea, address?.adminArea, address?.countryName)
        }.getOrDefault("Current location")
    }
}
