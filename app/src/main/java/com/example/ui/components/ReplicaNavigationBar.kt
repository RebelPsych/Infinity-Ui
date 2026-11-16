package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun ReplicaNavigationBar(
  onBackClick: () -> Unit,
  onHomeClick: () -> Unit,
  onRecentsClick: () -> Unit,
  gesturePillMode: Boolean = false,
  hapticEnabled: Boolean = false,
  hologramGlow: Boolean = false,
  accentColor: Color = Color.White,
  taskCountBadge: Int? = null,
  modifier: Modifier = Modifier,
) {
  val backDesc = stringResource(R.string.cd_nav_back)
  val homeDesc = stringResource(R.string.cd_nav_home)
  val recentsDesc = stringResource(R.string.cd_nav_recents)
  val haptic = LocalHapticFeedback.current

  fun withHaptic(action: () -> Unit): () -> Unit = {
    if (hapticEnabled) {
      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    action()
  }

  val bgBrush =
    if (hologramGlow) {
      Brush.verticalGradient(
        colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.22f), Color.Black.copy(alpha = 0.56f))
      )
    } else {
      Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f))
      )
    }

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .background(bgBrush)
        .height(52.dp)
        .testTag("replica_navigation_bar"),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    NavBarButton(
      description = backDesc,
      testTag = "nav_back_button",
      onClick = withHaptic(onBackClick),
    ) {
      Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val path =
          Path().apply {
            moveTo(w * 0.14f, h * 0.50f)
            lineTo(w * 0.84f, h * 0.10f)
            lineTo(w * 0.84f, h * 0.90f)
            close()
          }
        drawPath(
          path = path,
          color = Color.White.copy(alpha = 0.94f),
          style = Stroke(width = 1.7.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round),
        )
      }
    }

    NavBarButton(
      description = homeDesc,
      testTag = "nav_home_button",
      onClick = withHaptic(onHomeClick),
    ) {
      if (gesturePillMode) {
        Box(
          modifier =
            Modifier.width(52.dp)
              .height(5.dp)
              .background(
                if (hologramGlow) accentColor else Color.White.copy(alpha = 0.94f),
                RoundedCornerShape(3.dp),
              ),
        )
      } else {
        Canvas(modifier = Modifier.size(16.dp)) {
          drawCircle(
            color = if (hologramGlow) accentColor else Color.White.copy(alpha = 0.94f),
            radius = size.minDimension * 0.44f,
            style = Stroke(width = 1.7.dp.toPx()),
          )
        }
      }
    }

    NavBarButton(
      description = recentsDesc,
      testTag = "nav_recents_button",
      onClick = withHaptic(onRecentsClick),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(15.dp)) {
          val inset = 1.dp.toPx()
          drawRoundRect(
            color = Color.White.copy(alpha = 0.94f),
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2f, size.height - inset * 2f),
            cornerRadius = CornerRadius(2.2.dp.toPx(), 2.2.dp.toPx()),
            style = Stroke(width = 1.7.dp.toPx()),
          )
        }
        if (taskCountBadge != null && taskCountBadge > 0) {
          Text(
            text = taskCountBadge.coerceAtMost(9).toString(),
            color = accentColor,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 0.5.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun NavBarButton(
  description: String,
  testTag: String,
  onClick: () -> Unit,
  content: @Composable () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .size(width = 64.dp, height = 48.dp)
        .semantics { contentDescription = description }
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(bounded = false, radius = 26.dp, color = Color.White),
          role = Role.Button,
          onClick = onClick,
        )
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    content()
  }
}
