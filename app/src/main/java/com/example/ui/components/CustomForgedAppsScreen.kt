package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.CustomAppArchetype
import com.example.ai.CustomAppShortcut
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
fun CustomForgedMiniAppScreen(
  app: CustomAppShortcut,
  modifier: Modifier = Modifier,
) {
  val accent = Color(app.accentColorHex)

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF07080D))
        .padding(top = 42.dp, bottom = 62.dp, start = 18.dp, end = 18.dp)
        .testTag("mini_app_custom_${app.id}"),
  ) {
    // Forged App Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      UtilityAppIconBadge(
        label = app.name.take(2).uppercase(Locale.US),
        bgColor = Color(0xFF14141B),
        accentColor = accent,
        modifier = Modifier.size(44.dp),
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(text = app.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
          text = app.description,
          color = accent,
          fontSize = 12.sp,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    if (app.appFilePath.isNotBlank()) {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Saved app file: ${app.appFilePath.substringAfterLast('/')}",
        color = Color(0xFF64748B),
        fontSize = 10.5.sp,
        fontFamily = FontFamily.Monospace,
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      when (app.archetype) {
        CustomAppArchetype.COUNTER_TRACKER -> ForgedCounterBody(app, accent)
        CustomAppArchetype.EXPENSE_BUDGET -> ForgedBudgetBody(app, accent)
        CustomAppArchetype.TIMER_STOPWATCH -> ForgedTimerBody(accent)
        CustomAppArchetype.FLASHCARDS_NOTES -> ForgedListCardsBody(app, accent)
        CustomAppArchetype.DICE_RANDOM -> ForgedRandomizerBody(accent)
        CustomAppArchetype.DRAW_SKETCH -> ForgedSketchBody(accent)
        CustomAppArchetype.GENERAL_UTILITY -> ForgedGeneralUtilityBody(app, accent)
      }
    }
  }
}

@Composable
private fun ForgedCounterBody(app: CustomAppShortcut, accent: Color) {
  val context = LocalContext.current
  val prefs = remember(app.id) { context.getSharedPreferences("forged_${app.id}", Context.MODE_PRIVATE) }
  var count by remember(app.id) { mutableIntStateOf(prefs.getInt("count", 0)) }
  var goal by remember(app.id) { mutableIntStateOf(prefs.getInt("goal", 8).coerceAtLeast(1)) }

  fun updateCount(next: Int) {
    count = next.coerceAtLeast(0)
    prefs.edit().putInt("count", count).apply()
  }

  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = count.toString(),
      color = accent,
      fontSize = 68.sp,
      fontWeight = FontWeight.Bold,
    )
    Text(text = "Goal: $count / $goal", color = Color(0xFF9CA3AF), fontSize = 14.sp)
    Spacer(modifier = Modifier.height(22.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
      OutlinedButton(
        onClick = { updateCount(count - 1) },
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        modifier = Modifier.size(64.dp),
      ) {
        Text("−", fontSize = 24.sp)
      }
      Button(
        onClick = { updateCount(count + 1) },
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
        shape = CircleShape,
        modifier = Modifier.size(64.dp),
      ) {
        Text("+", fontSize = 26.sp, fontWeight = FontWeight.Bold)
      }
    }
    Spacer(modifier = Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedButton(onClick = { updateCount(0) }) { Text("Reset", color = Color(0xFF9CA3AF)) }
      OutlinedButton(
        onClick = {
          goal = (goal + 2).let { if (it > 20) 4 else it }
          prefs.edit().putInt("goal", goal).apply()
        },
      ) {
        Text("Goal +2", color = accent)
      }
    }
  }
}

@Composable
private fun ForgedBudgetBody(app: CustomAppShortcut, accent: Color) {
  val context = LocalContext.current
  val prefs = remember(app.id) { context.getSharedPreferences("forged_${app.id}", Context.MODE_PRIVATE) }
  var entries by remember(app.id) {
    val raw = prefs.getString("entries", "") ?: ""
    val list =
      raw.split("\n").filter { it.contains("|") }.map {
        val parts = it.split("|", limit = 2)
        parts[0] to (parts[1].toDoubleOrNull() ?: 0.0)
      }
    mutableStateOf(list)
  }
  var labelInput by remember { mutableStateOf("") }
  var amountInput by remember { mutableStateOf("") }

  fun saveEntries(next: List<Pair<String, Double>>) {
    entries = next
    prefs.edit().putString("entries", next.joinToString("\n") { "${it.first}|${it.second}" }).apply()
  }

  val total = entries.sumOf { it.second }

  Column(modifier = Modifier.fillMaxSize()) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color(0xFF121522),
      border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Total Tracked", color = Color(0xFF9CA3AF), fontSize = 12.sp)
        Text(
          text = String.format(Locale.US, "$%.2f", total),
          color = accent,
          fontSize = 30.sp,
          fontWeight = FontWeight.Bold,
        )
      }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        value = labelInput,
        onValueChange = { labelInput = it },
        singleLine = true,
        placeholder = { Text("Item", color = Color(0xFF6B7280)) },
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = accent),
        modifier = Modifier.weight(1.2f),
      )
      OutlinedTextField(
        value = amountInput,
        onValueChange = { amountInput = it },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        placeholder = { Text("$", color = Color(0xFF6B7280)) },
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = accent),
        modifier = Modifier.weight(0.8f),
      )
      IconButton(
        onClick = {
          val amt = amountInput.toDoubleOrNull()
          val lbl = labelInput.trim().ifBlank { "Entry" }
          if (amt != null) {
            saveEntries(listOf(lbl to amt) + entries)
            labelInput = ""
            amountInput = ""
          }
        },
        modifier = Modifier.minimumInteractiveComponentSize().background(accent, CircleShape),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Add expense", tint = Color.Black)
      }
    }
    Spacer(modifier = Modifier.height(12.dp))
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(entries) { item ->
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF12141E), modifier = Modifier.fillMaxWidth()) {
          Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item.first, color = Color.White, modifier = Modifier.weight(1f))
            Text(String.format(Locale.US, "$%.2f", item.second), color = accent, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}

@Composable
private fun ForgedTimerBody(accent: Color) {
  var running by remember { mutableStateOf(false) }
  var elapsedMs by remember { mutableLongStateOf(0L) }

  LaunchedEffect(running) {
    var prev = System.currentTimeMillis()
    while (running) {
      delay(80L)
      val now = System.currentTimeMillis()
      elapsedMs += now - prev
      prev = now
    }
  }

  val mins = (elapsedMs / 60000)
  val secs = (elapsedMs / 1000) % 60
  val tenths = (elapsedMs / 100) % 10

  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = String.format(Locale.US, "%02d:%02d.%d", mins, secs, tenths),
      color = accent,
      fontSize = 52.sp,
      fontWeight = FontWeight.Light,
    )
    Spacer(modifier = Modifier.height(22.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Button(
        onClick = { running = !running },
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text(if (running) "Pause" else "Start", fontWeight = FontWeight.Bold)
      }
      Button(
        onClick = {
          running = false
          elapsedMs = 0L
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text("Reset")
      }
    }
  }
}

@Composable
private fun ForgedListCardsBody(app: CustomAppShortcut, accent: Color) {
  val context = LocalContext.current
  val prefs = remember(app.id) { context.getSharedPreferences("forged_${app.id}", Context.MODE_PRIVATE) }
  var itemsList by remember(app.id) {
    val raw = prefs.getString("cards", "") ?: ""
    mutableStateOf(if (raw.isBlank()) listOf("Tap + to add your first entry") else raw.split("\n"))
  }
  var draft by remember { mutableStateOf("") }

  fun save(next: List<String>) {
    itemsList = next
    prefs.edit().putString("cards", next.joinToString("\n")).apply()
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        singleLine = true,
        placeholder = { Text("Add item or card…", color = Color(0xFF6B7280)) },
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = accent),
        modifier = Modifier.weight(1f),
      )
      IconButton(
        onClick = {
          val t = draft.trim()
          if (t.isNotEmpty()) {
            save(listOf(t) + itemsList)
            draft = ""
          }
        },
        modifier = Modifier.minimumInteractiveComponentSize().background(accent, CircleShape),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Add card", tint = Color.Black)
      }
    }
    Spacer(modifier = Modifier.height(12.dp))
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(itemsList) { entry ->
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF12141E), border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))) {
          Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(entry, color = Color.White, modifier = Modifier.weight(1f))
            IconButton(onClick = { save(itemsList - entry) }, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFF6B7280), modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ForgedRandomizerBody(accent: Color) {
  var diceValue by remember { mutableIntStateOf(6) }
  var coinValue by remember { mutableStateOf("Heads") }
  var history by remember { mutableStateOf(listOf<String>()) }

  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = Color(0xFF141624),
      border = BorderStroke(2.dp, accent),
      modifier = Modifier.size(130.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(text = diceValue.toString(), color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.Bold)
      }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text("Coin: $coinValue", color = accent, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Button(
        onClick = {
          diceValue = Random.nextInt(1, 7)
          history = (listOf("d6 → $diceValue") + history).take(5)
        },
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text("Roll d6", fontWeight = FontWeight.Bold)
      }
      Button(
        onClick = {
          coinValue = if (Random.nextBoolean()) "Heads" else "Tails"
          history = (listOf("Coin → $coinValue") + history).take(5)
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text("Flip Coin")
      }
    }
    if (history.isNotEmpty()) {
      Spacer(modifier = Modifier.height(16.dp))
      Text(history.joinToString("  ·  "), color = Color(0xFF9CA3AF), fontSize = 12.sp)
    }
  }
}

private data class DrawStroke(val points: List<Offset>, val color: Color)

@Composable
private fun ForgedSketchBody(accent: Color) {
  val strokes = remember { mutableStateListOf<DrawStroke>() }
  var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
  var selectedColor by remember { mutableStateOf(accent) }
  val palette = listOf(accent, Color(0xFF38BDF8), Color(0xFF4ADE80), Color(0xFFF472B6), Color.White)

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        palette.forEach { c ->
          Surface(
            shape = CircleShape,
            color = c,
            border = if (c == selectedColor) BorderStroke(2.dp, Color.White) else null,
            modifier = Modifier.size(30.dp).clickable { selectedColor = c },
          ) {}
        }
      }
      OutlinedButton(onClick = { strokes.clear() }) { Text("Clear", color = Color.White) }
    }
    Spacer(modifier = Modifier.height(10.dp))
    Box(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .background(Color(0xFF11131C), RoundedCornerShape(16.dp))
          .pointerInput(selectedColor) {
            detectDragGestures(
              onDragStart = { offset -> currentPoints = listOf(offset) },
              onDrag = { change, _ ->
                change.consume()
                currentPoints = currentPoints + change.position
              },
              onDragEnd = {
                if (currentPoints.isNotEmpty()) {
                  strokes.add(DrawStroke(currentPoints, selectedColor))
                  currentPoints = emptyList()
                }
              },
            )
          },
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val drawList = if (currentPoints.isNotEmpty()) strokes + DrawStroke(currentPoints, selectedColor) else strokes
        drawList.forEach { s ->
          if (s.points.size >= 2) {
            val path = Path().apply {
              moveTo(s.points.first().x, s.points.first().y)
              s.points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, color = s.color, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
          }
        }
      }
    }
  }
}

@Composable
private fun ForgedGeneralUtilityBody(app: CustomAppShortcut, accent: Color) {
  ForgedListCardsBody(app = app, accent = accent)
}
