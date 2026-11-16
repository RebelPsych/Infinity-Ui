package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private data class MineCell(
  val isMine: Boolean = false,
  val isRevealed: Boolean = false,
  val isFlagged: Boolean = false,
  val adjacent: Int = 0,
)

private fun buildMineBoard(rows: Int, cols: Int, mines: Int): List<MineCell> {
  val indices = (0 until rows * cols).shuffled().take(mines).toSet()
  val grid =
    MutableList(rows * cols) { idx -> MineCell(isMine = idx in indices) }
  for (r in 0 until rows) {
    for (c in 0 until cols) {
      val idx = r * cols + c
      if (grid[idx].isMine) continue
      var count = 0
      for (dr in -1..1) {
        for (dc in -1..1) {
          val nr = r + dr
          val nc = c + dc
          if (nr in 0 until rows && nc in 0 until cols && grid[nr * cols + nc].isMine) {
            count++
          }
        }
      }
      grid[idx] = grid[idx].copy(adjacent = count)
    }
  }
  return grid
}

@Composable
fun NeonMinesGameScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("arcade_scores", Context.MODE_PRIVATE) }
  val size = 7
  val mineCount = 8
  var board by remember { mutableStateOf(buildMineBoard(size, size, mineCount)) }
  var flagMode by remember { mutableStateOf(false) }
  var gameLost by remember { mutableStateOf(false) }
  var gameWon by remember { mutableStateOf(false) }
  var seconds by remember { mutableIntStateOf(0) }
  var bestSeconds by remember { mutableIntStateOf(prefs.getInt("best_mines_sec", 999)) }

  LaunchedEffect(board, gameLost, gameWon) {
    if (!gameLost && !gameWon && board.any { it.isRevealed }) {
      while (!gameLost && !gameWon) {
        delay(1000L)
        seconds++
      }
    }
  }

  fun revealAt(startIdx: Int) {
    if (gameLost || gameWon) return
    val cell = board[startIdx]
    if (flagMode) {
      if (!cell.isRevealed) {
        board = board.mapIndexed { i, c -> if (i == startIdx) c.copy(isFlagged = !c.isFlagged) else c }
      }
      return
    }
    if (cell.isFlagged || cell.isRevealed) return
    if (cell.isMine) {
      board = board.map { if (it.isMine) it.copy(isRevealed = true) else it }
      gameLost = true
      return
    }
    val next = board.toMutableList()
    val queue = ArrayDeque<Int>()
    queue.add(startIdx)
    while (queue.isNotEmpty()) {
      val cur = queue.removeFirst()
      if (next[cur].isRevealed || next[cur].isMine || next[cur].isFlagged) continue
      next[cur] = next[cur].copy(isRevealed = true)
      if (next[cur].adjacent == 0) {
        val r = cur / size
        val c = cur % size
        for (dr in -1..1) {
          for (dc in -1..1) {
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until size && nc in 0 until size) {
              val nIdx = nr * size + nc
              if (!next[nIdx].isRevealed) queue.add(nIdx)
            }
          }
        }
      }
    }
    board = next
    if (next.all { it.isMine || it.isRevealed }) {
      gameWon = true
      if (seconds in 1 until bestSeconds) {
        bestSeconds = seconds
        prefs.edit().putInt("best_mines_sec", bestSeconds).apply()
      }
    }
  }

  val flagsUsed = board.count { it.isFlagged }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF07090F))
        .padding(top = 42.dp, bottom = 62.dp, start = 16.dp, end = 16.dp)
        .testTag("lib_game_neon_mines"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column {
        Text("Neon Mines", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
          text = "Mines ${mineCount - flagsUsed} · Time ${seconds}s" + if (bestSeconds < 999) " · Best ${bestSeconds}s" else "",
          color = Color(0xFFF87171),
          fontSize = 12.sp,
        )
      }
      OutlinedButton(
        onClick = {
          board = buildMineBoard(size, size, mineCount)
          gameLost = false
          gameWon = false
          seconds = 0
        },
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text("New", color = Color.White)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(
        onClick = { flagMode = false },
        colors = ButtonDefaults.buttonColors(containerColor = if (!flagMode) Color(0xFFF87171) else Color(0xFF181B29)),
        modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
      ) {
        Text("Reveal", color = if (!flagMode) Color.Black else Color.White, fontWeight = FontWeight.Bold)
      }
      Button(
        onClick = { flagMode = true },
        colors = ButtonDefaults.buttonColors(containerColor = if (flagMode) Color(0xFFFBBF24) else Color(0xFF181B29)),
        modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
      ) {
        Text("Flag Mode", color = if (flagMode) Color.Black else Color.White, fontWeight = FontWeight.Bold)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
      for (r in 0 until size) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
          for (c in 0 until size) {
            val idx = r * size + c
            val cell = board[idx]
            val bg =
              when {
                cell.isRevealed && cell.isMine -> Color(0xFF991B1B)
                cell.isRevealed -> Color(0xFF141826)
                cell.isFlagged -> Color(0xFF422006)
                else -> Color(0xFF1F2437)
              }
            val label =
              when {
                cell.isRevealed && cell.isMine -> "✸"
                cell.isRevealed && cell.adjacent > 0 -> cell.adjacent.toString()
                cell.isFlagged -> "⚑"
                else -> ""
              }
            val textColor =
              when {
                cell.isMine -> Color.White
                cell.isFlagged -> Color(0xFFFBBF24)
                cell.adjacent == 1 -> Color(0xFF38BDF8)
                cell.adjacent == 2 -> Color(0xFF4ADE80)
                cell.adjacent >= 3 -> Color(0xFFF87171)
                else -> Color.White
              }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = bg,
              border = BorderStroke(1.dp, Color.White.copy(alpha = if (cell.isRevealed) 0.06f else 0.14f)),
              modifier =
                Modifier.weight(1f)
                  .aspectRatio(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { revealAt(idx) },
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(text = label, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))
    if (gameWon) {
      Text("Field Cleared in ${seconds}s!", color = Color(0xFF4ADE80), fontSize = 18.sp, fontWeight = FontWeight.Bold)
    } else if (gameLost) {
      Text("Boom! Tap New to retry.", color = Color(0xFFF87171), fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
  }
}

private data class MemoryTile(
  val id: Int,
  val symbol: String,
  val colorHex: Long,
  val matched: Boolean = false,
)

private fun buildMemoryDeck(): List<MemoryTile> {
  val pairs =
    listOf(
      "★" to 0xFFFBBF24L,
      "◈" to 0xFFA855F7L,
      "⚡" to 0xFF22D3EEL,
      "❖" to 0xFFF472B6L,
      "✦" to 0xFF4ADE80L,
      "▲" to 0xFFFB923CL,
      "●" to 0xFF60A5FAL,
      "⬢" to 0xFFF87171L,
    )
  return (pairs + pairs)
    .shuffled()
    .mapIndexed { idx, p -> MemoryTile(id = idx, symbol = p.first, colorHex = p.second) }
}

@Composable
fun ChromaMatchGameScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("arcade_scores", Context.MODE_PRIVATE) }
  var tiles by remember { mutableStateOf(buildMemoryDeck()) }
  var firstPick by remember { mutableStateOf<Int?>(null) }
  var secondPick by remember { mutableStateOf<Int?>(null) }
  var moves by remember { mutableIntStateOf(0) }
  var bestMoves by remember { mutableIntStateOf(prefs.getInt("best_memory_moves", 999)) }
  var lockInput by remember { mutableStateOf(false) }

  LaunchedEffect(secondPick) {
    val a = firstPick
    val b = secondPick
    if (a != null && b != null) {
      lockInput = true
      delay(480L)
      if (tiles[a].symbol == tiles[b].symbol) {
        val next = tiles.mapIndexed { i, t -> if (i == a || i == b) t.copy(matched = true) else t }
        tiles = next
        if (next.all { it.matched } && moves < bestMoves) {
          bestMoves = moves
          prefs.edit().putInt("best_memory_moves", bestMoves).apply()
        }
      }
      firstPick = null
      secondPick = null
      lockInput = false
    }
  }

  val allMatched = tiles.all { it.matched }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF07090F))
        .padding(top = 42.dp, bottom = 62.dp, start = 16.dp, end = 16.dp)
        .testTag("lib_game_chroma_match"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column {
        Text("Chroma Match", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
          text = "Moves: $moves" + if (bestMoves < 999) " · Best: $bestMoves" else "",
          color = Color(0xFF22D3EE),
          fontSize = 12.sp,
        )
      }
      OutlinedButton(
        onClick = {
          tiles = buildMemoryDeck()
          firstPick = null
          secondPick = null
          moves = 0
          lockInput = false
        },
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text("Reset", color = Color.White)
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
      for (r in 0 until 4) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          for (c in 0 until 4) {
            val idx = r * 4 + c
            val tile = tiles[idx]
            val revealed = tile.matched || idx == firstPick || idx == secondPick
            val accent = Color(tile.colorHex)
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (revealed) Color(0xFF141B2D) else Color(0xFF11131D),
              border = BorderStroke(1.5.dp, if (revealed) accent else Color.White.copy(alpha = 0.12f)),
              modifier =
                Modifier.weight(1f)
                  .aspectRatio(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .clickable(enabled = !lockInput && !revealed) {
                    if (firstPick == null) {
                      firstPick = idx
                    } else if (secondPick == null && firstPick != idx) {
                      secondPick = idx
                      moves++
                    }
                  },
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = if (revealed) tile.symbol else "?",
                  color = if (revealed) accent else Color(0xFF475569),
                  fontSize = 26.sp,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }

    if (allMatched) {
      Spacer(modifier = Modifier.height(18.dp))
      Text("All Pairs Matched in $moves moves!", color = Color(0xFF4ADE80), fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
  }
}
