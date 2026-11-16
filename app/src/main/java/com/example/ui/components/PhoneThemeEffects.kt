package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.PhoneUpgradeId
import com.example.system.DeviceStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

fun resolveAccentColor(activeUpgrades: Set<PhoneUpgradeId>): Color =
  when {
    PhoneUpgradeId.OMEGA_UNIFIED_OS in activeUpgrades -> Color(0xFF00F5D4)
    PhoneUpgradeId.ACCENT_EMERALD_MATRIX in activeUpgrades -> Color(0xFF10B981)
    PhoneUpgradeId.ACCENT_CRIMSON_NOVA in activeUpgrades -> Color(0xFFF43F5E)
    PhoneUpgradeId.ACCENT_SOLAR_AMBER in activeUpgrades -> Color(0xFFF59E0B)
    PhoneUpgradeId.ACCENT_CYBER_CYAN in activeUpgrades -> Color(0xFF22D3EE)
    else -> Color(0xFFA855F7)
  }

@Composable
fun CosmicStarfieldLayer(
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  val transition = rememberInfiniteTransition(label = "starfield")
  val phase by transition.animateFloat(
    initialValue = 0f,
    targetValue = 6.283f,
    animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
    label = "star_phase",
  )

  val stars = remember {
    listOf(
      0.12f to 0.14f,
      0.34f to 0.09f,
      0.67f to 0.18f,
      0.86f to 0.12f,
      0.22f to 0.29f,
      0.54f to 0.36f,
      0.78f to 0.42f,
      0.15f to 0.56f,
      0.43f to 0.61f,
      0.88f to 0.54f,
      0.28f to 0.72f,
      0.69f to 0.68f,
    )
  }

  Canvas(modifier = modifier.fillMaxSize()) {
    stars.forEachIndexed { idx, (rx, ry) ->
      val alpha = (0.30f + 0.55f * ((sin(phase + idx * 0.9f) + 1f) * 0.5f)).coerceIn(0.15f, 0.9f)
      val yDrift = ((phase * 8f + idx * 14f) % 40f)
      drawCircle(
        color = if (idx % 3 == 0) accentColor.copy(alpha = alpha) else Color.White.copy(alpha = alpha),
        radius = if (idx % 2 == 0) 2.3.dp.toPx() else 1.5.dp.toPx(),
        center = Offset(size.width * rx, (size.height * ry + yDrift) % size.height),
      )
    }
  }
}

@Composable
fun HorizonGlowLine(
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(3.dp)
        .background(
          Brush.horizontalGradient(
            listOf(Color.Transparent, accentColor.copy(alpha = 0.85f), Color(0xFF38BDF8), Color.Transparent)
          )
        ),
  )
}

@Composable
fun LockScreenOverlay(
  status: DeviceStatus,
  accentColor: Color,
  activeUpgradesCount: Int,
  onUnlock: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val dateLabel =
    remember(status.formattedTime) {
      SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(listOf(Color(0xFF05070E), Color(0xFF100B1C), Color(0xFF04060C)))
        )
        .pointerInput(Unit) {
          detectVerticalDragGestures { _, dragAmount ->
            if (dragAmount < -18f) onUnlock()
          }
        }
        .clickable(onClick = onUnlock)
        .testTag("lock_screen_overlay"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(top = 86.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = status.formattedTime.ifBlank { "12:00" },
        color = Color.White,
        fontSize = 62.sp,
        fontWeight = FontWeight.ExtraLight,
      )
      Text(text = dateLabel, color = accentColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
      Spacer(modifier = Modifier.height(26.dp))
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF131622),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier.padding(horizontal = 24.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          InfinityAiAppIcon(iconSize = 28.dp, cornerRadius = 8.dp)
          Column {
            Text("Infinity OS Ready", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("$activeUpgradesCount upgrades active · Battery ${status.batteryLevel}%", color = Color(0xFF9CA3AF), fontSize = 11.5.sp)
          }
        }
      }
    }

    Text(
      text = "Swipe up or tap anywhere to unlock",
      color = Color.White.copy(alpha = 0.72f),
      fontSize = 13.sp,
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 78.dp),
    )
  }
}
