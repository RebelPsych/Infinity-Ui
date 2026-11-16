package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.system.DeviceStatus

private val StatusTextShadow =
  Shadow(color = Color.Black.copy(alpha = 0.65f), offset = Offset(0f, 1.5f), blurRadius = 3.5f)

@Composable
fun ReplicaStatusBar(
  status: DeviceStatus,
  activeUpgrades: Set<com.example.ai.PhoneUpgradeId> = emptySet(),
  modifier: Modifier = Modifier,
) {
  val wifiDesc =
    if (status.isWifiConnected) {
      stringResource(R.string.cd_wifi_connected)
    } else {
      stringResource(R.string.cd_wifi_disconnected)
    }
  val networkDesc =
    when {
      status.hasInternet -> stringResource(R.string.cd_cellular_connected)
      else -> stringResource(R.string.cd_no_internet)
    }
  val batteryDesc = stringResource(R.string.cd_battery_status, status.batteryLevel)
  val showSeconds = com.example.ai.PhoneUpgradeId.STATUS_SECONDS in activeUpgrades
  val minimalMode = com.example.ai.PhoneUpgradeId.MINIMAL_STATUS_BAR in activeUpgrades
  val batteryPill = com.example.ai.PhoneUpgradeId.STATUS_BATTERY_PILL in activeUpgrades
  val showNetworkTag = com.example.ai.PhoneUpgradeId.STATUS_NETWORK_TAG in activeUpgrades
  val showFpsBadge = com.example.ai.PhoneUpgradeId.HUD_FPS_BADGE in activeUpgrades
  val accentColor = resolveAccentColor(activeUpgrades)

  val timeDisplay =
    if (showSeconds) {
      java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
    } else {
      status.formattedTime
    }

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.displayCutout)
        .padding(horizontal = 12.dp, vertical = 4.dp)
        .height(22.dp)
        .testTag("replica_status_bar"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = timeDisplay,
        style =
          TextStyle(
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            shadow = StatusTextShadow,
          ),
        modifier = Modifier.testTag("status_time_text"),
      )
      if (showFpsBadge && !minimalMode) {
        Text(
          text = "60Hz",
          color = accentColor,
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      if (!minimalMode) {
        if (showNetworkTag && status.isWifiConnected) {
          Text(
            text = "${status.wifiSignalBars * 25}%",
            color = accentColor,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }
        WifiStatusIcon(
          isConnected = status.isWifiConnected,
          signalBars = status.wifiSignalBars,
          modifier = Modifier.semantics { contentDescription = wifiDesc }.testTag("status_wifi_icon"),
        )

        if (status.isCellularConnected || !status.hasInternet) {
          CellularStatusCluster(
            isCellularConnected = status.isCellularConnected,
            hasInternet = status.hasInternet,
            modifier =
              Modifier.semantics { contentDescription = networkDesc }
                .testTag("status_network_icon"),
          )
        }
      }

      val batteryComposable: @Composable () -> Unit = {
        BatteryStatusIndicator(
          level = status.batteryLevel,
          isCharging = status.isCharging,
          modifier =
            Modifier.semantics { contentDescription = batteryDesc }.testTag("status_battery_icon"),
        )
      }

      if (batteryPill) {
        Box(
          modifier =
            Modifier.background(Color.Black.copy(alpha = 0.50f), RoundedCornerShape(10.dp))
              .padding(horizontal = 6.dp, vertical = 1.dp),
        ) {
          batteryComposable()
        }
      } else {
        batteryComposable()
      }
    }
  }
}

@Composable
private fun WifiStatusIcon(isConnected: Boolean, signalBars: Int, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.size(width = 18.dp, height = 14.dp)) {
    val w = size.width
    val h = size.height

    val apex = Offset(w * 0.60f, h * 0.88f)
    val fanRadius = h * 0.78f

    if (isConnected) {
      // Subtle activity chevrons on bottom-left matching screenshot
      val chevronStroke = 1.1.dp.toPx()
      val upPath =
        Path().apply {
          moveTo(w * 0.06f, h * 0.42f)
          lineTo(w * 0.14f, h * 0.24f)
          lineTo(w * 0.22f, h * 0.42f)
        }
      drawPath(upPath, Color.White, style = Stroke(width = chevronStroke, cap = StrokeCap.Round))

      // Dim background wedge
      drawArc(
        color = Color.White.copy(alpha = 0.30f),
        startAngle = -132f,
        sweepAngle = 84f,
        useCenter = true,
        topLeft = Offset(apex.x - fanRadius, apex.y - fanRadius),
        size = Size(fanRadius * 2f, fanRadius * 2f),
      )

      // Active signal wedge proportional to bars (1..4)
      val activeFraction = (signalBars.coerceIn(1, 4) / 4f)
      val activeRadius = fanRadius * activeFraction
      drawArc(
        color = Color.White,
        startAngle = -132f,
        sweepAngle = 84f,
        useCenter = true,
        topLeft = Offset(apex.x - activeRadius, apex.y - activeRadius),
        size = Size(activeRadius * 2f, activeRadius * 2f),
      )
    } else {
      // Disconnected Wi-Fi outline + slash
      drawArc(
        color = Color.White.copy(alpha = 0.45f),
        startAngle = -132f,
        sweepAngle = 84f,
        useCenter = true,
        topLeft = Offset(apex.x - fanRadius, apex.y - fanRadius),
        size = Size(fanRadius * 2f, fanRadius * 2f),
      )
      drawLine(
        color = Color.White.copy(alpha = 0.9f),
        start = Offset(w * 0.22f, h * 0.14f),
        end = Offset(w * 0.92f, h * 0.90f),
        strokeWidth = 1.4.dp.toPx(),
        cap = StrokeCap.Round,
      )
    }
  }
}

@Composable
private fun CellularStatusCluster(
  isCellularConnected: Boolean,
  hasInternet: Boolean,
  modifier: Modifier = Modifier,
) {
  Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
    if (isCellularConnected) {
      Box(
        modifier =
          Modifier.background(Color.White, RoundedCornerShape(2.dp))
            .padding(horizontal = 2.5.dp, vertical = 0.5.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "HD",
          color = Color.Black,
          fontSize = 6.5.sp,
          fontWeight = FontWeight.ExtraBold,
          lineHeight = 7.sp,
        )
      }
      Spacer(modifier = Modifier.width(2.dp))
      Column(horizontalAlignment = Alignment.Start) {
        Text(
          text = "LTE",
          style =
            TextStyle(
              color = Color.White,
              fontSize = 6.5.sp,
              fontWeight = FontWeight.Bold,
              lineHeight = 7.sp,
              shadow = StatusTextShadow,
            ),
        )
      }
      Spacer(modifier = Modifier.width(1.5.dp))
    }

    Canvas(modifier = Modifier.size(width = 14.dp, height = 12.dp)) {
      val barCount = 4
      val gap = 1.5.dp.toPx()
      val barWidth = (size.width - gap * (barCount - 1)) / barCount
      val activeColor = if (isCellularConnected && hasInternet) Color.White else Color.White.copy(alpha = 0.35f)

      for (i in 0 until barCount) {
        val barHeight = size.height * (0.32f + 0.22f * i)
        val x = i * (barWidth + gap)
        val y = size.height - barHeight
        drawRoundRect(
          color = activeColor,
          topLeft = Offset(x, y),
          size = Size(barWidth, barHeight),
          cornerRadius = CornerRadius(1f, 1f),
        )
      }

      if (!hasInternet) {
        val stroke = 1.4.dp.toPx()
        val xStart = size.width * 0.58f
        val yStart = size.height * 0.08f
        val crossSize = size.width * 0.34f
        drawLine(
          color = Color(0xFFFF6B6B),
          start = Offset(xStart, yStart),
          end = Offset(xStart + crossSize, yStart + crossSize),
          strokeWidth = stroke,
          cap = StrokeCap.Round,
        )
        drawLine(
          color = Color(0xFFFF6B6B),
          start = Offset(xStart + crossSize, yStart),
          end = Offset(xStart, yStart + crossSize),
          strokeWidth = stroke,
          cap = StrokeCap.Round,
        )
      }
    }
  }
}

@Composable
private fun BatteryStatusIndicator(
  level: Int,
  isCharging: Boolean,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    Canvas(modifier = Modifier.size(width = 9.5.dp, height = 14.dp)) {
      val w = size.width
      val h = size.height
      val nubWidth = w * 0.44f
      val nubHeight = h * 0.11f
      val bodyTop = nubHeight
      val bodyHeight = h - bodyTop

      // Battery cap/nub
      drawRoundRect(
        color = Color.White.copy(alpha = 0.92f),
        topLeft = Offset((w - nubWidth) / 2f, 0f),
        size = Size(nubWidth, nubHeight + 1f),
        cornerRadius = CornerRadius(2f, 2f),
      )

      // Outer body border
      drawRoundRect(
        color = Color.White.copy(alpha = 0.32f),
        topLeft = Offset(0f, bodyTop),
        size = Size(w, bodyHeight),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
      )

      val strokePx = 1.dp.toPx()
      drawRoundRect(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(strokePx / 2f, bodyTop + strokePx / 2f),
        size = Size(w - strokePx, bodyHeight - strokePx),
        cornerRadius = CornerRadius(1.8.dp.toPx(), 1.8.dp.toPx()),
        style = Stroke(width = strokePx),
      )

      val fillColor =
        when {
          isCharging -> Color(0xFF4ADE80)
          level <= 20 -> Color(0xFFFF7A2F)
          else -> Color.White
        }

      val innerPadding = 1.4.dp.toPx()
      val maxFillHeight = (bodyHeight - innerPadding * 2f).coerceAtLeast(0f)
      val fillHeight = maxFillHeight * (level.coerceIn(0, 100) / 100f)
      if (fillHeight > 0f) {
        drawRoundRect(
          color = fillColor,
          topLeft = Offset(innerPadding, bodyTop + bodyHeight - innerPadding - fillHeight),
          size = Size((w - innerPadding * 2f).coerceAtLeast(0f), fillHeight),
          cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
        )
      }

      if (isCharging) {
        val bolt =
          Path().apply {
            moveTo(w * 0.56f, bodyTop + bodyHeight * 0.16f)
            lineTo(w * 0.28f, bodyTop + bodyHeight * 0.54f)
            lineTo(w * 0.50f, bodyTop + bodyHeight * 0.54f)
            lineTo(w * 0.42f, bodyTop + bodyHeight * 0.84f)
            lineTo(w * 0.72f, bodyTop + bodyHeight * 0.46f)
            lineTo(w * 0.50f, bodyTop + bodyHeight * 0.46f)
            close()
          }
        drawPath(
          path = bolt,
          color = Color.Black.copy(alpha = 0.85f),
          style = Stroke(width = 0.6.dp.toPx(), join = StrokeJoin.Round),
        )
        drawPath(path = bolt, color = Color.White)
      }
    }

    Text(
      text = "${level}%",
      style =
        TextStyle(
          color = Color.White,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          shadow = StatusTextShadow,
        ),
      modifier = Modifier.testTag("status_battery_text"),
    )
  }
}
