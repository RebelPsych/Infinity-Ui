package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

@Composable
fun PlayStoreTriangleIcon(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val leftX = w * 0.14f
    val rightX = w * 0.90f
    val topY = h * 0.08f
    val bottomY = h * 0.92f
    val midY = h * 0.50f
    val centerX = w * 0.56f
    val cornerJoinX = w * 0.67f
    val cornerTopY = h * 0.36f
    val cornerBottomY = h * 0.64f

    val outerTriangle =
      Path().apply {
        moveTo(leftX, topY)
        lineTo(rightX, midY)
        lineTo(leftX, bottomY)
        close()
      }

    clipPath(outerTriangle) {
      val bluePath =
        Path().apply {
          moveTo(leftX, topY)
          lineTo(centerX, midY)
          lineTo(leftX, bottomY)
          close()
        }
      drawPath(bluePath, Color(0xFF00B4FF))

      val greenPath =
        Path().apply {
          moveTo(leftX, topY)
          lineTo(cornerJoinX, cornerTopY)
          lineTo(centerX, midY)
          close()
        }
      drawPath(greenPath, Color(0xFF00E676))

      val yellowPath =
        Path().apply {
          moveTo(cornerJoinX, cornerTopY)
          lineTo(rightX, midY)
          lineTo(cornerJoinX, cornerBottomY)
          lineTo(centerX, midY)
          close()
        }
      drawPath(yellowPath, Color(0xFFFFC400))

      val redPath =
        Path().apply {
          moveTo(centerX, midY)
          lineTo(cornerJoinX, cornerBottomY)
          lineTo(leftX, bottomY)
          close()
        }
      drawPath(redPath, Color(0xFFFF3D00))
    }
  }
}

@Composable
fun PlayBellOutlineIcon(
  tint: Color = Color(0xFFE3E3E3),
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.size(22.dp)) {
    val w = size.width
    val h = size.height
    val stroke = 1.7.dp.toPx()

    drawLine(
      color = tint,
      start = Offset(w * 0.50f, h * 0.11f),
      end = Offset(w * 0.50f, h * 0.17f),
      strokeWidth = stroke,
      cap = StrokeCap.Round,
    )

    val bell =
      Path().apply {
        moveTo(w * 0.24f, h * 0.68f)
        lineTo(w * 0.28f, h * 0.42f)
        cubicTo(
          w * 0.28f,
          h * 0.23f,
          w * 0.72f,
          h * 0.23f,
          w * 0.72f,
          h * 0.42f,
        )
        lineTo(w * 0.76f, h * 0.68f)
        lineTo(w * 0.83f, h * 0.74f)
        lineTo(w * 0.17f, h * 0.74f)
        close()
      }
    drawPath(
      path = bell,
      color = tint,
      style = Stroke(width = stroke, join = StrokeJoin.Round, cap = StrokeCap.Round),
    )

    drawArc(
      color = tint,
      startAngle = 15f,
      sweepAngle = 150f,
      useCenter = false,
      topLeft = Offset(w * 0.40f, h * 0.74f),
      size = Size(w * 0.20f, h * 0.13f),
      style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
  }
}

@Composable
fun SmileyProfileAvatar(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.size(32.dp)) {
    val radius = size.minDimension / 2f
    val ringStroke = 2.dp.toPx()

    drawCircle(
      color = Color(0xFF4C8DF6),
      radius = radius - ringStroke / 2f,
      style = Stroke(width = ringStroke),
    )

    val faceRadius = radius - ringStroke * 1.6f
    drawCircle(color = Color(0xFFFFD54F), radius = faceRadius)

    val eyeRadius = faceRadius * 0.12f
    val eyeY = center.y - faceRadius * 0.20f
    val eyeOffsetX = faceRadius * 0.34f
    drawCircle(
      color = Color(0xFF1E1E1E),
      radius = eyeRadius,
      center = Offset(center.x - eyeOffsetX, eyeY),
    )
    drawCircle(
      color = Color(0xFF1E1E1E),
      radius = eyeRadius,
      center = Offset(center.x + eyeOffsetX, eyeY),
    )

    val smileRadius = faceRadius * 0.56f
    drawArc(
      color = Color(0xFF1E1E1E),
      startAngle = 20f,
      sweepAngle = 140f,
      useCenter = false,
      topLeft = Offset(center.x - smileRadius, center.y - smileRadius * 0.72f),
      size = Size(smileRadius * 2f, smileRadius * 1.75f),
      style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
    )
  }
}

@Composable
fun GamesTabIcon(
  tint: Color,
  cutoutColor: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.size(22.dp)) {
    val w = size.width
    val h = size.height
    val body =
      Path().apply {
        moveTo(w * 0.24f, h * 0.28f)
        lineTo(w * 0.76f, h * 0.28f)
        cubicTo(w * 0.90f, h * 0.28f, w * 0.96f, h * 0.54f, w * 0.90f, h * 0.74f)
        cubicTo(w * 0.86f, h * 0.86f, w * 0.74f, h * 0.82f, w * 0.68f, h * 0.68f)
        lineTo(w * 0.32f, h * 0.68f)
        cubicTo(w * 0.26f, h * 0.82f, w * 0.14f, h * 0.86f, w * 0.10f, h * 0.74f)
        cubicTo(w * 0.04f, h * 0.54f, w * 0.10f, h * 0.28f, w * 0.24f, h * 0.28f)
        close()
      }
    drawPath(body, color = tint)

    val dpadStroke = 1.7.dp.toPx()
    drawLine(
      color = cutoutColor,
      start = Offset(w * 0.26f, h * 0.48f),
      end = Offset(w * 0.40f, h * 0.48f),
      strokeWidth = dpadStroke,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = cutoutColor,
      start = Offset(w * 0.33f, h * 0.41f),
      end = Offset(w * 0.33f, h * 0.55f),
      strokeWidth = dpadStroke,
      cap = StrokeCap.Round,
    )

    drawCircle(color = cutoutColor, radius = 1.3.dp.toPx(), center = Offset(w * 0.64f, h * 0.44f))
    drawCircle(color = cutoutColor, radius = 1.3.dp.toPx(), center = Offset(w * 0.72f, h * 0.52f))
  }
}

@Composable
fun AppsTabIcon(tint: Color, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.size(20.dp)) {
    val stroke = 1.7.dp.toPx()
    val gap = size.width * 0.16f
    val cell = (size.width - gap) / 2f
    val radius = CornerRadius(2.2.dp.toPx(), 2.2.dp.toPx())
    val positions =
      listOf(
        Offset(0f, 0f),
        Offset(cell + gap, 0f),
        Offset(0f, cell + gap),
        Offset(cell + gap, cell + gap),
      )
    positions.forEach { pos ->
      drawRoundRect(
        color = tint,
        topLeft = pos + Offset(stroke / 2f, stroke / 2f),
        size = Size(cell - stroke, cell - stroke),
        cornerRadius = radius,
        style = Stroke(width = stroke),
      )
    }
  }
}

@Composable
fun SearchTabIcon(tint: Color, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.size(20.dp)) {
    val stroke = 1.8.dp.toPx()
    val r = size.minDimension * 0.30f
    val c = Offset(size.width * 0.42f, size.height * 0.42f)
    drawCircle(color = tint, radius = r, center = c, style = Stroke(width = stroke))
    drawLine(
      color = tint,
      start = Offset(c.x + r * 0.72f, c.y + r * 0.72f),
      end = Offset(size.width * 0.88f, size.height * 0.88f),
      strokeWidth = stroke,
      cap = StrokeCap.Round,
    )
  }
}

@Composable
fun BooksTabIcon(tint: Color, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.size(20.dp)) {
    val w = size.width
    val h = size.height
    val stroke = 1.7.dp.toPx()
    drawRoundRect(
      color = tint,
      topLeft = Offset(w * 0.18f, h * 0.10f),
      size = Size(w * 0.64f, h * 0.80f),
      cornerRadius = CornerRadius(1.8.dp.toPx(), 1.8.dp.toPx()),
      style = Stroke(width = stroke),
    )
    val bookmark =
      Path().apply {
        moveTo(w * 0.42f, h * 0.10f)
        lineTo(w * 0.42f, h * 0.44f)
        lineTo(w * 0.52f, h * 0.36f)
        lineTo(w * 0.62f, h * 0.44f)
        lineTo(w * 0.62f, h * 0.10f)
      }
    drawPath(
      path = bookmark,
      color = tint,
      style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round),
    )
  }
}

@Composable
fun YouTabIcon(tint: Color, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.size(20.dp)) {
    val w = size.width
    val h = size.height
    val stroke = 1.7.dp.toPx()
    drawCircle(
      color = tint,
      radius = w * 0.18f,
      center = Offset(w * 0.50f, h * 0.31f),
      style = Stroke(width = stroke),
    )
    val shoulders =
      Path().apply {
        moveTo(w * 0.18f, h * 0.84f)
        cubicTo(w * 0.18f, h * 0.60f, w * 0.82f, h * 0.60f, w * 0.82f, h * 0.84f)
      }
    drawPath(
      path = shoulders,
      color = tint,
      style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
  }
}
