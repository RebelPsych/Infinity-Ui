package com.example.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DeviceStatus(
  val formattedTime: String = "",
  val batteryLevel: Int = 100,
  val isCharging: Boolean = false,
  val isWifiConnected: Boolean = false,
  val wifiSignalBars: Int = 4,
  val isCellularConnected: Boolean = false,
  val hasInternet: Boolean = false,
)

class DeviceStatusMonitor(private val context: Context) {

  private val appContext = context.applicationContext
  private val connectivityManager =
    appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  private val _status = MutableStateFlow(readInitialStatus())
  val status: StateFlow<DeviceStatus> = _status.asStateFlow()

  private var isStarted = false

  private val timeAndBatteryReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(ctx: Context?, intent: Intent?) {
        when (intent?.action) {
          Intent.ACTION_TIME_TICK,
          Intent.ACTION_TIME_CHANGED,
          Intent.ACTION_TIMEZONE_CHANGED -> {
            _status.update { it.copy(formattedTime = formatCurrentTime()) }
          }
          Intent.ACTION_BATTERY_CHANGED -> {
            val (level, charging) = extractBatteryInfo(intent)
            _status.update { it.copy(batteryLevel = level, isCharging = charging) }
          }
        }
      }
    }

  private val networkCallback =
    object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        updateConnectivity()
      }

      override fun onLost(network: Network) {
        updateConnectivity()
      }

      override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities,
      ) {
        updateConnectivity()
      }
    }

  fun start() {
    if (isStarted) return
    isStarted = true

    _status.value = readInitialStatus()

    val filter =
      IntentFilter().apply {
        addAction(Intent.ACTION_TIME_TICK)
        addAction(Intent.ACTION_TIME_CHANGED)
        addAction(Intent.ACTION_TIMEZONE_CHANGED)
        addAction(Intent.ACTION_BATTERY_CHANGED)
      }
    ContextCompat.registerReceiver(
      appContext,
      timeAndBatteryReceiver,
      filter,
      ContextCompat.RECEIVER_EXPORTED,
    )

    runCatching { connectivityManager.registerDefaultNetworkCallback(networkCallback) }
  }

  fun stop() {
    if (!isStarted) return
    isStarted = false

    runCatching { appContext.unregisterReceiver(timeAndBatteryReceiver) }
    runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
  }

  private fun readInitialStatus(): DeviceStatus {
    val batteryIntent =
      ContextCompat.registerReceiver(
        appContext,
        null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ContextCompat.RECEIVER_EXPORTED,
      )
    val (batteryLevel, isCharging) = extractBatteryInfo(batteryIntent)
    val connectivity = readConnectivitySnapshot()

    return DeviceStatus(
      formattedTime = formatCurrentTime(),
      batteryLevel = batteryLevel,
      isCharging = isCharging,
      isWifiConnected = connectivity.isWifiConnected,
      wifiSignalBars = connectivity.wifiSignalBars,
      isCellularConnected = connectivity.isCellularConnected,
      hasInternet = connectivity.hasInternet,
    )
  }

  private fun updateConnectivity() {
    val snapshot = readConnectivitySnapshot()
    _status.update {
      it.copy(
        isWifiConnected = snapshot.isWifiConnected,
        wifiSignalBars = snapshot.wifiSignalBars,
        isCellularConnected = snapshot.isCellularConnected,
        hasInternet = snapshot.hasInternet,
      )
    }
  }

  private data class ConnectivitySnapshot(
    val isWifiConnected: Boolean,
    val wifiSignalBars: Int,
    val isCellularConnected: Boolean,
    val hasInternet: Boolean,
  )

  private fun readConnectivitySnapshot(): ConnectivitySnapshot {
    val activeNetwork = connectivityManager.activeNetwork
    val capabilities =
      activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }

    if (capabilities == null) {
      return ConnectivitySnapshot(
        isWifiConnected = false,
        wifiSignalBars = 0,
        isCellularConnected = false,
        hasInternet = false,
      )
    }

    val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

    val wifiBars =
      if (!isWifi) {
        0
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val rssi = capabilities.signalStrength
        when {
          rssi == NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED -> 4
          rssi >= -55 -> 4
          rssi >= -67 -> 3
          rssi >= -78 -> 2
          else -> 1
        }
      } else {
        4
      }

    return ConnectivitySnapshot(
      isWifiConnected = isWifi,
      wifiSignalBars = wifiBars,
      isCellularConnected = isCellular,
      hasInternet = hasInternet,
    )
  }

  private fun formatCurrentTime(): String {
    val pattern = if (DateFormat.is24HourFormat(appContext)) "HH:mm" else "h:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
  }

  private fun extractBatteryInfo(intent: Intent?): Pair<Int, Boolean> {
    if (intent == null) return 100 to false
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val pct =
      if (level >= 0 && scale > 0) {
        ((level * 100f) / scale).toInt().coerceIn(0, 100)
      } else {
        100
      }
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging =
      status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    return pct to charging
  }
}
