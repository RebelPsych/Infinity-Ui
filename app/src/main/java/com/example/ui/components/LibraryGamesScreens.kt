package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.delay

private enum class SnakeDir { UP, DOWN, LEFT, RIGHT }

@Composable
fun NeonSnakeGameScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("arcade_scores", Context.MODE_PRIVATE) }
  val cols = 15
  val rows = 18

  var snake by remember { mutableStateOf(listOf(7 to 9, 6 to 9, 5 to 9)) }
  var direction by remember { mutableStateOf(SnakeDir.RIGHT) }
  var pendingDirection by remember { mutableStateOf(SnakeDir.RIGHT) }
  var food by remember { mutableStateOf(11 to 9) }
  var score by remember { mutableIntStateOf(0) }
  var highScore by remember { mutableIntStateOf(prefs.getInt("snake_high", 0)) }
  var isGameOver by remember { mutableStateOf(false) }
  var isPaused by remember { mutableStateOf(false) }

  fun spawnFood(occupied: List<Pair<Int, Int>>): Pair<Int, Int> {
    val free = buildList {
      for (x in 0 until cols) for (y in 0 until rows) {
        if ((x to y) !in occupied) add(x to y)
      }
    }
    return if (free.isEmpty()) 0 to 0 else free[Random.nextInt(free.size)]
  }

  fun resetGame() {
    snake = listOf(7 to 9, 6 to 9, 5 to 9)
    direction = SnakeDir.RIGHT
    pendingDirection = SnakeDir.RIGHT
    food = spawnFood(snake)
    score = 0
    isGameOver = false
    isPaused = false
  }

  fun changeDir(next: SnakeDir) {
    val opposite =
      (direction == SnakeDir.UP && next == SnakeDir.DOWN) ||
        (direction == SnakeDir.DOWN && next == SnakeDir.UP) ||
        (direction == SnakeDir.LEFT && next == SnakeDir.RIGHT) ||
        (direction == SnakeDir.RIGHT && next == SnakeDir.LEFT)
    if (!opposite) pendingDirection = next
  }

  LaunchedEffect(isGameOver, isPaused) {
    while (!isGameOver && !isPaused) {
      val tickMs = (165L - (score * 2L)).coerceAtLeast(80L)
      delay(tickMs)
      direction = pendingDirection
      val head = snake.first()
      val nextHead =
        when (direction) {
          SnakeDir.UP -> head.first to (head.second - 1)
          SnakeDir.DOWN -> head.first to (head.second + 1)
          SnakeDir.LEFT -> (head.first - 1) to head.second
          SnakeDir.RIGHT -> (head.first + 1) to head.second
        }
      val hitWall = nextHead.first !in 0 until cols || nextHead.second !in 0 until rows
      val ateFood = nextHead == food
      val bodyToCheck = if (ateFood) snake else snake.dropLast(1)
      if (hitWall || nextHead in bodyToCheck) {
        isGameOver = true
        if (score > highScore) {
          highScore = score
          prefs.edit().putInt("snake_high", highScore).apply()
        }
      } else {
        val nextSnake = if (ateFood) listOf(nextHead) + snake else listOf(nextHead) + snake.dropLast(1)
        snake = nextSnake
        if (ateFood) {
          score += 10
          if (score > highScore) {
            highScore = score
            prefs.edit().putInt("snake_high", highScore).apply()
          }
          food = spawnFood(nextSnake)
        }
      }
    }
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF050807))
        .padding(top = 42.dp, bottom = 62.dp, start = 16.dp, end = 16.dp)
        .testTag("game_neon_snake"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text("Neon Snake", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Score: $score  ·  Best: $highScore", color = Color(0xFF4ADE80), fontSize = 13.sp)
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = { isPaused = !isPaused },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Text(if (isPaused) "Resume" else "Pause", fontSize = 12.sp)
        }
        Button(
          onClick = ::resetGame,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.Black),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("snake_restart_button"),
        ) {
          Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Box(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .background(Color(0xFF0B110E), RoundedCornerShape(16.dp))
          .pointerInput(Unit) {
            var totalDx = 0f
            var totalDy = 0f
            detectDragGestures(
              onDragStart = {
                totalDx = 0f
                totalDy = 0f
              },
              onDrag = { change, dragAmount ->
                change.consume()
                totalDx += dragAmount.x
                totalDy += dragAmount.y
                if (abs(totalDx) > 36f || abs(totalDy) > 36f) {
                  if (abs(totalDx) > abs(totalDy)) {
                    changeDir(if (totalDx > 0) SnakeDir.RIGHT else SnakeDir.LEFT)
                  } else {
                    changeDir(if (totalDy > 0) SnakeDir.DOWN else SnakeDir.UP)
                  }
                  totalDx = 0f
                  totalDy = 0f
                }
              },
            )
          },
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        val cellW = size.width / cols
        val cellH = size.height / rows
        // Food
        drawCircle(
          color = Color(0xFFF43F5E),
          radius = minOf(cellW, cellH) * 0.36f,
          center = Offset((food.first + 0.5f) * cellW, (food.second + 0.5f) * cellH),
        )
        // Snake
        snake.forEachIndexed { idx, seg ->
          val segColor = if (idx == 0) Color(0xFF4ADE80) else Color(0xFF22C55E).copy(alpha = 0.80f)
          drawRoundRect(
            color = segColor,
            topLeft = Offset(seg.first * cellW + 2f, seg.second * cellH + 2f),
            size = Size((cellW - 4f).coerceAtLeast(1f), (cellH - 4f).coerceAtLeast(1f)),
            cornerRadius = CornerRadius(6f, 6f),
          )
        }
      }

      if (isGameOver) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color.Black.copy(alpha = 0.88f),
          border = BorderStroke(1.dp, Color(0xFF4ADE80)),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text("Game Over", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Final Score: $score", color = Color(0xFF4ADE80), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = ::resetGame,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ADE80), contentColor = Color.Black),
            ) {
              Text("Play Again", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // D-Pad Controls
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
      DpadButton("▲") { changeDir(SnakeDir.UP) }
      Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        DpadButton("◀") { changeDir(SnakeDir.LEFT) }
        DpadButton("▶") { changeDir(SnakeDir.RIGHT) }
      }
      DpadButton("▼") { changeDir(SnakeDir.DOWN) }
    }
  }
}

@Composable
private fun DpadButton(label: String, onClick: () -> Unit) {
  Button(
    onClick = onClick,
    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18221D), contentColor = Color(0xFF4ADE80)),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.size(54.dp, 48.dp),
  ) {
    Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
  }
}

private enum class MoveDir { UP, DOWN, LEFT, RIGHT }

@Composable
fun Puzzle2048GameScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("arcade_scores", Context.MODE_PRIVATE) }

  var bestScore by remember { mutableIntStateOf(prefs.getInt("2048_best", 0)) }
  var score by remember { mutableIntStateOf(0) }
  var board by remember { mutableStateOf(createInitial2048Board()) }
  var isGameOver by remember { mutableStateOf(false) }

  fun restart() {
    board = createInitial2048Board()
    score = 0
    isGameOver = false
  }

  fun performMove(dir: MoveDir) {
    if (isGameOver) return
    val (nextBoard, gained, moved) = slide2048(board, dir)
    if (!moved) return
    val withTile = spawn2048Tile(nextBoard)
    board = withTile
    score += gained
    if (score > bestScore) {
      bestScore = score
      prefs.edit().putInt("2048_best", bestScore).apply()
    }
    if (!canMove2048(withTile)) {
      isGameOver = true
    }
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF0A0812))
        .padding(top = 42.dp, bottom = 62.dp, start = 18.dp, end = 18.dp)
        .testTag("game_2048"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text("2048 Prism", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Score: $score  ·  Best: $bestScore", color = Color(0xFFC084FC), fontSize = 13.sp)
      }
      Button(
        onClick = ::restart,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
        modifier = Modifier.minimumInteractiveComponentSize().testTag("2048_new_game"),
      ) {
        Text("New Game", fontSize = 12.5.sp)
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    Surface(
      shape = RoundedCornerShape(18.dp),
      color = Color(0xFF151221),
      border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.35f)),
      modifier =
        Modifier.fillMaxWidth()
          .aspectRatio(1f)
          .pointerInput(Unit) {
            var dx = 0f
            var dy = 0f
            detectDragGestures(
              onDragStart = {
                dx = 0f
                dy = 0f
              },
              onDrag = { change, dragAmount ->
                change.consume()
                dx += dragAmount.x
                dy += dragAmount.y
              },
              onDragEnd = {
                if (abs(dx) > 40f || abs(dy) > 40f) {
                  if (abs(dx) > abs(dy)) {
                    performMove(if (dx > 0) MoveDir.RIGHT else MoveDir.LEFT)
                  } else {
                    performMove(if (dy > 0) MoveDir.DOWN else MoveDir.UP)
                  }
                }
              },
            )
          },
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        for (r in 0 until 4) {
          Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            for (c in 0 until 4) {
              val v = board[r * 4 + c]
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = tile2048Color(v),
                modifier = Modifier.weight(1f).fillMaxSize(),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  if (v > 0) {
                    Text(
                      text = v.toString(),
                      color = Color.White,
                      fontSize = if (v >= 1024) 18.sp else 22.sp,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (isGameOver) {
      Text("No moves left — tap New Game!", color = Color(0xFFF472B6), fontWeight = FontWeight.SemiBold)
      Spacer(modifier = Modifier.height(8.dp))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(onClick = { performMove(MoveDir.LEFT) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF231D36))) { Text("◀") }
      Button(onClick = { performMove(MoveDir.UP) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF231D36))) { Text("▲") }
      Button(onClick = { performMove(MoveDir.DOWN) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF231D36))) { Text("▼") }
      Button(onClick = { performMove(MoveDir.RIGHT) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF231D36))) { Text("▶") }
    }

    Spacer(modifier = Modifier.weight(1f))
  }
}

private data class BrickItem(val x: Float, val y: Float, val w: Float, val h: Float, val color: Color, val alive: Boolean = true)

@Composable
fun BrickBreakerGameScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("arcade_scores", Context.MODE_PRIVATE) }

  var widthPx by remember { mutableFloatStateOf(0f) }
  var heightPx by remember { mutableFloatStateOf(0f) }
  var paddleX by remember { mutableFloatStateOf(0.5f) }
  var ballX by remember { mutableFloatStateOf(0.5f) }
  var ballY by remember { mutableFloatStateOf(0.72f) }
  var velX by remember { mutableFloatStateOf(0.42f) }
  var velY by remember { mutableFloatStateOf(-0.55f) }
  var score by remember { mutableIntStateOf(0) }
  var highScore by remember { mutableIntStateOf(prefs.getInt("breaker_high", 0)) }
  var lives by remember { mutableIntStateOf(3) }
  var level by remember { mutableIntStateOf(1) }
  var isGameOver by remember { mutableStateOf(false) }
  var bricks by remember { mutableStateOf(createBreakerBricks()) }

  fun resetAll() {
    paddleX = 0.5f
    ballX = 0.5f
    ballY = 0.72f
    velX = 0.42f
    velY = -0.55f
    score = 0
    lives = 3
    level = 1
    isGameOver = false
    bricks = createBreakerBricks()
  }

  LaunchedEffect(isGameOver, widthPx, heightPx) {
    if (isGameOver || widthPx <= 0f || heightPx <= 0f) return@LaunchedEffect
    var lastNanos = withFrameNanos { it }
    while (!isGameOver) {
      val nowNanos = withFrameNanos { it }
      val dt = ((nowNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.032f)
      lastNanos = nowNanos

      var nx = ballX + velX * dt
      var ny = ballY + velY * dt

      if (nx <= 0.02f) {
        nx = 0.02f
        velX = abs(velX)
      } else if (nx >= 0.98f) {
        nx = 0.98f
        velX = -abs(velX)
      }
      if (ny <= 0.02f) {
        ny = 0.02f
        velY = abs(velY)
      }

      // Paddle collision
      val paddleHalfW = 0.13f
      val paddleTop = 0.88f
      if (velY > 0f && ny in (paddleTop - 0.02f)..(paddleTop + 0.03f) && nx in (paddleX - paddleHalfW)..(paddleX + paddleHalfW)) {
        val hitOffset = ((nx - paddleX) / paddleHalfW).coerceIn(-1f, 1f)
        velX = hitOffset * 0.65f
        velY = -abs(velY)
        ny = paddleTop - 0.02f
      }

      // Brick collisions
      var hitIndex = -1
      for (i in bricks.indices) {
        val b = bricks[i]
        if (b.alive && nx in b.x..(b.x + b.w) && ny in b.y..(b.y + b.h)) {
          hitIndex = i
          break
        }
      }
      if (hitIndex >= 0) {
        bricks = bricks.mapIndexed { idx, b -> if (idx == hitIndex) b.copy(alive = false) else b }
        velY = -velY
        score += 15 * level
        if (score > highScore) {
          highScore = score
          prefs.edit().putInt("breaker_high", highScore).apply()
        }
        if (bricks.none { it.alive }) {
          level += 1
          bricks = createBreakerBricks()
          velX *= 1.08f
          velY = -abs(velY) * 1.08f
          nx = 0.5f
          ny = 0.72f
        }
      }

      if (ny > 0.99f) {
        lives -= 1
        if (lives <= 0) {
          isGameOver = true
        } else {
          nx = paddleX
          ny = 0.72f
          velY = -abs(velY)
        }
      }

      ballX = nx
      ballY = ny
    }
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF09060D))
        .padding(top = 42.dp, bottom = 62.dp, start = 16.dp, end = 16.dp)
        .testTag("game_brick_breaker"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text("Neon Breaker · Lv $level", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text("Score: $score  ·  Best: $highScore  ·  Lives: $lives", color = Color(0xFFF472B6), fontSize = 12.5.sp)
      }
      Button(
        onClick = ::resetAll,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB2777)),
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text("Reset", fontSize = 12.sp)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Box(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .background(Color(0xFF100B18), RoundedCornerShape(16.dp))
          .onSizeChanged {
            widthPx = it.width.toFloat()
            heightPx = it.height.toFloat()
          }
          .pointerInput(widthPx) {
            detectDragGestures { change, dragAmount ->
              change.consume()
              if (widthPx > 0f) {
                paddleX = (paddleX + dragAmount.x / widthPx).coerceIn(0.14f, 0.86f)
              }
            }
          },
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        bricks.forEach { b ->
          if (b.alive) {
            drawRoundRect(
              color = b.color,
              topLeft = Offset(b.x * size.width + 2f, b.y * size.height + 2f),
              size = Size((b.w * size.width - 4f).coerceAtLeast(1f), (b.h * size.height - 4f).coerceAtLeast(1f)),
              cornerRadius = CornerRadius(6f, 6f),
            )
          }
        }
        // Paddle
        val pw = 0.26f * size.width
        val ph = 14.dp.toPx()
        drawRoundRect(
          color = Color(0xFF38BDF8),
          topLeft = Offset(paddleX * size.width - pw / 2f, 0.88f * size.height),
          size = Size(pw, ph),
          cornerRadius = CornerRadius(12f, 12f),
        )
        // Ball
        drawCircle(
          color = Color.White,
          radius = 8.dp.toPx(),
          center = Offset(ballX * size.width, ballY * size.height),
        )
      }

      if (isGameOver) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color.Black.copy(alpha = 0.90f),
          border = BorderStroke(1.dp, Color(0xFFF472B6)),
        ) {
          Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text("Game Over", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Score: $score", color = Color(0xFFF472B6), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = ::resetAll,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF472B6), contentColor = Color.Black),
            ) {
              Text("Play Again", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

private fun createBreakerBricks(): List<BrickItem> {
  val palette = listOf(Color(0xFFF472B6), Color(0xFFA855F7), Color(0xFF38BDF8), Color(0xFF2DD4BF))
  val rows = 4
  val cols = 6
  val padX = 0.04f
  val usableW = 1f - padX * 2f
  val bw = usableW / cols
  val bh = 0.055f
  return buildList {
    for (r in 0 until rows) {
      for (c in 0 until cols) {
        add(BrickItem(x = padX + c * bw, y = 0.08f + r * (bh + 0.012f), w = bw, h = bh, color = palette[r % palette.size]))
      }
    }
  }
}

private fun createInitial2048Board(): List<Int> {
  val empty = MutableList(16) { 0 }
  return spawn2048Tile(spawn2048Tile(empty))
}

private fun spawn2048Tile(board: List<Int>): List<Int> {
  val empties = board.indices.filter { board[it] == 0 }
  if (empties.isEmpty()) return board
  val idx = empties[Random.nextInt(empties.size)]
  val mutable = board.toMutableList()
  mutable[idx] = if (Random.nextFloat() < 0.9f) 2 else 4
  return mutable
}

private fun slideLineLeft(line: List<Int>): Pair<List<Int>, Int> {
  val nonZero = line.filter { it != 0 }
  val merged = mutableListOf<Int>()
  var scoreGain = 0
  var i = 0
  while (i < nonZero.size) {
    if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
      val sum = nonZero[i] * 2
      merged.add(sum)
      scoreGain += sum
      i += 2
    } else {
      merged.add(nonZero[i])
      i += 1
    }
  }
  while (merged.size < 4) merged.add(0)
  return merged to scoreGain
}

private fun slide2048(board: List<Int>, dir: MoveDir): Triple<List<Int>, Int, Boolean> {
  val result = MutableList(16) { 0 }
  var totalGain = 0
  for (i in 0 until 4) {
    val line =
      when (dir) {
        MoveDir.LEFT -> (0 until 4).map { board[i * 4 + it] }
        MoveDir.RIGHT -> (3 downTo 0).map { board[i * 4 + it] }
        MoveDir.UP -> (0 until 4).map { board[it * 4 + i] }
        MoveDir.DOWN -> (3 downTo 0).map { board[it * 4 + i] }
      }
    val (slid, gain) = slideLineLeft(line)
    totalGain += gain
    for (j in 0 until 4) {
      val destIdx =
        when (dir) {
          MoveDir.LEFT -> i * 4 + j
          MoveDir.RIGHT -> i * 4 + (3 - j)
          MoveDir.UP -> j * 4 + i
          MoveDir.DOWN -> (3 - j) * 4 + i
        }
      result[destIdx] = slid[j]
    }
  }
  return Triple(result, totalGain, result != board)
}

private fun canMove2048(board: List<Int>): Boolean {
  if (board.any { it == 0 }) return true
  for (r in 0 until 4) {
    for (c in 0 until 4) {
      val v = board[r * 4 + c]
      if (c + 1 < 4 && board[r * 4 + c + 1] == v) return true
      if (r + 1 < 4 && board[(r + 1) * 4 + c] == v) return true
    }
  }
  return false
}

private fun tile2048Color(value: Int): Color =
  when (value) {
    0 -> Color(0xFF1F1B2E)
    2 -> Color(0xFF2E2747)
    4 -> Color(0xFF3E3163)
    8 -> Color(0xFF6D28D9)
    16 -> Color(0xFF7C3AED)
    32 -> Color(0xFF9333EA)
    64 -> Color(0xFFC026D3)
    128 -> Color(0xFFDB2777)
    256 -> Color(0xFF0284C7)
    512 -> Color(0xFF0D9488)
    1024 -> Color(0xFFD97706)
    else -> Color(0xFF16A34A)
  }
