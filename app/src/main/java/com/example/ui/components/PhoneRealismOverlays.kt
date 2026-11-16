package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.system.DeviceStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun HomeClockWidget(
  status: DeviceStatus,
  activeUpgrades: Set<com.example.ai.PhoneUpgradeId> = emptySet(),
  modifier: Modifier = Modifier,
) {
  val dateLabel =
    remember(status.formattedTime) {
      SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
    }
  val accentColor = resolveAccentColor(activeUpgrades)
  val showWeatherPill = com.example.ai.PhoneUpgradeId.WEATHER_SYSTEM_PILL in activeUpgrades
  val showPowerEst = com.example.ai.PhoneUpgradeId.INLINE_POWER_ESTIMATE in activeUpgrades
  val chargingGlow = com.example.ai.PhoneUpgradeId.CHARGING_AURA_PULSE in activeUpgrades && status.isCharging
  val showBatteryRing = com.example.ai.PhoneUpgradeId.BATTERY_CORE_WIDGET in activeUpgrades

  Surface(
    shape = RoundedCornerShape(22.dp),
    color = Color.Black.copy(alpha = 0.42f),
    border =
      BorderStroke(
        width = if (chargingGlow) 1.5.dp else 1.dp,
        color =
          when {
            chargingGlow -> Color(0xFF4ADE80)
            showBatteryRing -> accentColor.copy(alpha = 0.45f)
            else -> Color.White.copy(alpha = 0.16f)
          },
      ),
    modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp).testTag("home_clock_widget"),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = status.formattedTime.ifBlank { "12:00" },
        color = Color.White,
        fontSize = 42.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = 1.sp,
      )
      Spacer(modifier = Modifier.height(2.dp))
      val netText =
        when {
          status.isWifiConnected -> "Wi-Fi"
          status.isCellularConnected -> "LTE"
          else -> "Offline"
        }
      val extraEst =
        if (showPowerEst) " · ~${(status.batteryLevel / 8).coerceAtLeast(1)}h left" else ""
      Text(
        text = "$dateLabel  •  ${status.batteryLevel}%  •  $netText$extraEst",
        color = Color.White.copy(alpha = 0.85f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
      )
      if (showWeatherPill) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "On-Device Core · Signal ${status.wifiSignalBars}/4 · ${activeUpgrades.size}/100 active",
          color = accentColor,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}

@Composable
fun NotificationShadeOverlay(
  status: DeviceStatus,
  isNightDimEnabled: Boolean,
  dimAlphaLevel: Float,
  isTorchOn: Boolean,
  soundProfileIndex: Int,
  isAirplaneSandbox: Boolean,
  isRotationLocked: Boolean,
  activeUpgrades: Set<com.example.ai.PhoneUpgradeId>,
  forgedFilesCount: Int,
  appLaunchCount: Int,
  onToggleNightDim: () -> Unit,
  onDimAlphaChange: (Float) -> Unit,
  onToggleTorch: () -> Unit,
  onCycleSoundProfile: () -> Unit,
  onToggleAirplane: () -> Unit,
  onToggleRotateLock: () -> Unit,
  onLockScreen: () -> Unit,
  onOpenInfinityAi: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val dateLabel =
    remember(status.formattedTime) {
      SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }
  val accentColor = resolveAccentColor(activeUpgrades)
  val soundLabels = listOf("Ring", "Vibrate", "Silent")
  var soundPlaying by remember { mutableStateOf(false) }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.72f))
        .clickable(onClick = onDismiss)
        .testTag("notification_shade_overlay"),
  ) {
    Surface(
      shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
      color = Color(0xFF13151C),
      border =
        BorderStroke(
          1.dp,
          if (com.example.ai.PhoneUpgradeId.NEON_TRIM_BORDERS in activeUpgrades) accentColor.copy(alpha = 0.55f)
          else Color.White.copy(alpha = 0.10f),
        ),
      modifier =
        Modifier.fillMaxWidth()
          .padding(top = 28.dp)
          .clickable(enabled = false, onClick = {}),
    ) {
      Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = status.formattedTime.ifBlank { "12:00" },
              color = Color.White,
              fontSize = 20.sp,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = dateLabel,
              color = Color(0xFF9CA3AF),
              fontSize = 12.sp,
            )
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (com.example.ai.PhoneUpgradeId.LOCK_SCREEN_OVERLAY in activeUpgrades) {
              TextButton(onClick = onLockScreen) {
                Text("Lock", color = accentColor, fontSize = 12.5.sp)
              }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.testTag("shade_close_button")) {
              Icon(Icons.Filled.Close, contentDescription = "Close shade", tint = Color.White)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          QuickTile(
            title = stringResource(R.string.shade_wifi),
            subtitle = if (status.isWifiConnected) "Connected" else "Off",
            isActive = status.isWifiConnected,
            activeColor = accentColor,
            onClick = {},
            modifier = Modifier.weight(1f),
          )
          QuickTile(
            title = stringResource(R.string.shade_night_dim),
            subtitle = if (isNightDimEnabled) "On" else "Off",
            isActive = isNightDimEnabled,
            activeColor = accentColor,
            onClick = onToggleNightDim,
            modifier = Modifier.weight(1f).testTag("shade_night_dim_tile"),
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          QuickTile(
            title = stringResource(R.string.shade_cellular),
            subtitle = if (status.isCellularConnected) "LTE Active" else "Standby",
            isActive = status.isCellularConnected,
            activeColor = accentColor,
            onClick = {},
            modifier = Modifier.weight(1f),
          )
          QuickTile(
            title = stringResource(R.string.shade_battery),
            subtitle = "${status.batteryLevel}%${if (status.isCharging) " ⚡" else ""}",
            isActive = status.isCharging,
            activeColor = accentColor,
            onClick = {},
            modifier = Modifier.weight(1f),
          )
        }

        // Extra Quick Tiles unlocked by Tier 1-3 upgrades
        val extraTiles = buildList<Triple<String, String, Pair<Boolean, () -> Unit>>> {
          if (com.example.ai.PhoneUpgradeId.SHADE_TORCH_TILE in activeUpgrades) {
            add(Triple("Torch", if (isTorchOn) "Bright On" else "Off", isTorchOn to onToggleTorch))
          }
          if (com.example.ai.PhoneUpgradeId.SHADE_SOUND_TILE in activeUpgrades) {
            add(Triple("Sound", soundLabels[soundProfileIndex % 3], (soundProfileIndex != 2) to onCycleSoundProfile))
          }
          if (com.example.ai.PhoneUpgradeId.SHADE_AIRPLANE_TILE in activeUpgrades) {
            add(Triple("Sandbox", if (isAirplaneSandbox) "Isolated" else "Normal", isAirplaneSandbox to onToggleAirplane))
          }
          if (com.example.ai.PhoneUpgradeId.SHADE_ROTATE_TILE in activeUpgrades) {
            add(Triple("Rotate Lock", if (isRotationLocked) "Locked" else "Auto", isRotationLocked to onToggleRotateLock))
          }
        }
        if (extraTiles.isNotEmpty()) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            extraTiles.forEach { (title, sub, pair) ->
              QuickTile(
                title = title,
                subtitle = sub,
                isActive = pair.first,
                activeColor = accentColor,
                onClick = pair.second,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }

        if (com.example.ai.PhoneUpgradeId.SHADE_BRIGHTNESS_SLIDER in activeUpgrades) {
          Spacer(modifier = Modifier.height(10.dp))
          Text("Display Dim Filter: ${(dimAlphaLevel * 100).toInt()}%", color = Color(0xFF9CA3AF), fontSize = 11.sp)
          androidx.compose.material3.Slider(
            value = dimAlphaLevel,
            onValueChange = onDimAlphaChange,
            valueRange = 0.10f..0.72f,
            modifier = Modifier.height(24.dp),
          )
        }

        if (com.example.ai.PhoneUpgradeId.SHADE_MEDIA_WIDGET in activeUpgrades) {
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1A1E2E),
            modifier = Modifier.fillMaxWidth().clickable { soundPlaying = !soundPlaying },
          ) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text("Ambient Focus Synth", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
              Text(if (soundPlaying) "Playing ♫" else "Paused", color = accentColor, fontSize = 12.sp)
            }
          }
        }

        if (com.example.ai.PhoneUpgradeId.LIVE_STORAGE_HUD in activeUpgrades ||
          com.example.ai.PhoneUpgradeId.SHADE_PROCESS_MONITOR in activeUpgrades ||
          com.example.ai.PhoneUpgradeId.SHADE_HUD_MATRIX in activeUpgrades
        ) {
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0E131D),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "HUD · Upgrades ${activeUpgrades.size}/100 · App Files: $forgedFilesCount · Launches: $appLaunchCount",
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFF1B1626),
          border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f)),
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .clickable(onClick = onOpenInfinityAi)
              .testTag("shade_infinity_notification"),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            InfinityAiAppIcon(iconSize = 36.dp, cornerRadius = 9.dp, symbolColor = accentColor)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Infinity AI Active",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = "${activeUpgrades.size} system upgrades active · Tap to open chat",
                color = accentColor,
                fontSize = 12.sp,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QuickTile(
  title: String,
  subtitle: String,
  isActive: Boolean,
  activeColor: Color = Color(0xFF6D28D9),
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = if (isActive) activeColor.copy(alpha = 0.35f) else Color(0xFF1E2029),
    border = if (isActive) BorderStroke(1.dp, activeColor.copy(alpha = 0.7f)) else null,
    modifier = modifier.height(60.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = title,
        color = Color.White,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
      )
      Text(
        text = subtitle,
        color = if (isActive) Color.White else Color(0xFF9CA3AF),
        fontSize = 11.sp,
        maxLines = 1,
      )
    }
  }
}

data class RecentsAppCard(
  val id: String,
  val title: String,
  val accentColor: Color,
)

@Composable
fun RecentsOverviewScreen(
  cards: List<RecentsAppCard>,
  showMemoryBar: Boolean = false,
  showQuickClearHeader: Boolean = false,
  onOpenCard: (String) -> Unit,
  onDismissCard: (String) -> Unit,
  onClearAll: () -> Unit,
  onCloseOverview: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.88f))
        .padding(top = 48.dp, bottom = 68.dp)
        .testTag("recents_overview_screen"),
  ) {
    if (showMemoryBar || showQuickClearHeader) {
      val rt = Runtime.getRuntime()
      val usedMb = ((rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)).coerceAtLeast(1)
      val maxMb = (rt.maxMemory() / (1024 * 1024)).coerceAtLeast(usedMb)
      Row(
        modifier = Modifier.align(Alignment.TopStart).padding(start = 18.dp, end = 60.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (showMemoryBar) {
          Text(
            text = "RAM $usedMb / $maxMb MB",
            color = Color(0xFF5EEAD4),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }
        if (showQuickClearHeader && cards.isNotEmpty()) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1F2433),
            modifier = Modifier.clickable(onClick = onClearAll),
          ) {
            Text(
              text = "Clear (${cards.size})",
              color = Color.White,
              fontSize = 11.5.sp,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            )
          }
        }
      }
    }

    if (cards.isEmpty()) {
      Text(
        text = stringResource(R.string.recents_empty),
        color = Color(0xFF9CA3AF),
        fontSize = 15.sp,
        modifier = Modifier.align(Alignment.Center),
      )
    } else {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 28.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          cards.forEach { card ->
            Surface(
              shape = RoundedCornerShape(22.dp),
              color = Color(0xFF151720),
              border = BorderStroke(1.dp, card.accentColor.copy(alpha = 0.55f)),
              modifier =
                Modifier.size(width = 210.dp, height = 340.dp)
                  .clip(RoundedCornerShape(22.dp))
                  .clickable { onOpenCard(card.id) }
                  .testTag("recents_card_${card.id}"),
            ) {
              Column(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = card.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                  )
                  IconButton(
                    onClick = { onDismissCard(card.id) },
                    modifier = Modifier.size(28.dp),
                  ) {
                    Icon(
                      Icons.Filled.Close,
                      contentDescription = "Close ${card.title}",
                      tint = Color(0xFF9CA3AF),
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }

                Box(
                  modifier =
                    Modifier.fillMaxWidth()
                      .weight(1f)
                      .padding(vertical = 12.dp)
                      .background(Color.Black, RoundedCornerShape(14.dp)),
                  contentAlignment = Alignment.Center,
                ) {
                  Box(
                    modifier =
                      Modifier.size(44.dp)
                        .background(card.accentColor.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = card.title.take(1),
                      color = card.accentColor,
                      fontSize = 20.sp,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(22.dp))

        TextButton(
          onClick = onClearAll,
          modifier = Modifier.testTag("recents_clear_all_button"),
        ) {
          Text(
            text = stringResource(R.string.recents_clear_all),
            color = Color.White,
            fontSize = 14.sp,
          )
        }
      }
    }

    IconButton(
      onClick = onCloseOverview,
      modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp),
    ) {
      Icon(Icons.Filled.Close, contentDescription = "Close overview", tint = Color.White)
    }
  }
}

@Composable
fun UtilityAppIconBadge(
  label: String,
  bgColor: Color,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(11.dp),
    color = bgColor,
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
    shadowElevation = 2.dp,
    modifier = modifier.size(54.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = label,
        color = accentColor,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
fun ClockMiniAppScreen(modifier: Modifier = Modifier) {
  var currentTime by remember { mutableStateOf("") }
  var currentDate by remember { mutableStateOf("") }
  var stopwatchRunning by remember { mutableStateOf(false) }
  var elapsedMillis by remember { mutableLongStateOf(0L) }

  LaunchedEffect(Unit) {
    while (true) {
      val now = Date()
      currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
      currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now)
      delay(500L)
    }
  }

  LaunchedEffect(stopwatchRunning) {
    var lastTick = System.currentTimeMillis()
    while (stopwatchRunning) {
      delay(100L)
      val now = System.currentTimeMillis()
      elapsedMillis += (now - lastTick)
      lastTick = now
    }
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black)
        .padding(top = 48.dp, bottom = 68.dp, start = 24.dp, end = 24.dp)
        .testTag("mini_app_clock"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(text = currentTime, color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Light)
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = currentDate, color = Color(0xFF9CA3AF), fontSize = 15.sp)

    Spacer(modifier = Modifier.height(44.dp))

    val secs = (elapsedMillis / 1000) % 60
    val mins = (elapsedMillis / 60000)
    val tenths = (elapsedMillis / 100) % 10
    Text(
      text = String.format(Locale.US, "%02d:%02d.%d", mins, secs, tenths),
      color = Color(0xFFA855F7),
      fontSize = 34.sp,
      fontWeight = FontWeight.Medium,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Button(
        onClick = { stopwatchRunning = !stopwatchRunning },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
      ) {
        Text(if (stopwatchRunning) "Pause" else "Start")
      }
      Button(
        onClick = {
          stopwatchRunning = false
          elapsedMillis = 0L
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
      ) {
        Text("Reset")
      }
    }
  }
}

@Composable
fun CalculatorMiniAppScreen(modifier: Modifier = Modifier) {
  var display by remember { mutableStateOf("0") }
  var pendingOp by remember { mutableStateOf<String?>(null) }
  var storedValue by remember { mutableStateOf<Double?>(null) }
  var resetOnNextDigit by remember { mutableStateOf(false) }

  fun pressDigit(d: String) {
    display =
      if (resetOnNextDigit || display == "0") {
        resetOnNextDigit = false
        d
      } else {
        display + d
      }
  }

  fun commitOp(nextOp: String?) {
    val current = display.toDoubleOrNull() ?: 0.0
    val prev = storedValue
    val op = pendingOp
    val result =
      if (prev != null && op != null && !resetOnNextDigit) {
        when (op) {
          "+" -> prev + current
          "-" -> prev - current
          "×" -> prev * current
          "÷" -> if (current == 0.0) 0.0 else prev / current
          else -> current
        }
      } else {
        current
      }
    storedValue = result
    display =
      if (result % 1.0 == 0.0) result.toLong().toString()
      else String.format(Locale.US, "%.4f", result).trimEnd('0').trimEnd('.')
    pendingOp = nextOp
    resetOnNextDigit = true
  }

  val rows =
    listOf(
      listOf("7", "8", "9", "÷"),
      listOf("4", "5", "6", "×"),
      listOf("1", "2", "3", "-"),
      listOf("C", "0", "=", "+"),
    )

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black)
        .padding(top = 48.dp, bottom = 68.dp, start = 20.dp, end = 20.dp)
        .testTag("mini_app_calculator"),
    verticalArrangement = Arrangement.Bottom,
  ) {
    Text(
      text = display,
      color = Color.White,
      fontSize = 48.sp,
      fontWeight = FontWeight.Light,
      modifier = Modifier.align(Alignment.End).padding(bottom = 24.dp),
      maxLines = 1,
    )

    rows.forEach { row ->
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        row.forEach { key ->
          val isOp = key in listOf("÷", "×", "-", "+", "=")
          val isClear = key == "C"
          Button(
            onClick = {
              when {
                key == "C" -> {
                  display = "0"
                  pendingOp = null
                  storedValue = null
                  resetOnNextDigit = false
                }
                key == "=" -> commitOp(null)
                isOp -> commitOp(key)
                else -> pressDigit(key)
              }
            },
            shape = CircleShape,
            colors =
              ButtonDefaults.buttonColors(
                containerColor =
                  when {
                    isOp -> Color(0xFF7C3AED)
                    isClear -> Color(0xFF3F3F46)
                    else -> Color(0xFF18181B)
                  },
              ),
            modifier = Modifier.weight(1f).height(64.dp),
          ) {
            Text(key, fontSize = 22.sp, color = Color.White)
          }
        }
      }
    }
  }
}

@Composable
fun NotesMiniAppScreen(
  savedNotes: String,
  onNotesChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black)
        .padding(top = 48.dp, bottom = 68.dp, start = 20.dp, end = 20.dp)
        .testTag("mini_app_notes"),
  ) {
    Text(
      text = stringResource(R.string.app_notes),
      color = Color.White,
      fontSize = 22.sp,
      fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
      value = savedNotes,
      onValueChange = onNotesChange,
      placeholder = { Text("Write a note on-device…", color = Color(0xFF6B7280)) },
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedContainerColor = Color(0xFF121218),
          unfocusedContainerColor = Color(0xFF121218),
          focusedBorderColor = Color(0xFFA855F7),
          unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
        ),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxSize(),
    )
  }
}

