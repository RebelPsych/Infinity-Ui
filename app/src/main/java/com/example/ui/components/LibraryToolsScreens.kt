package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private enum class ConverterCategory(val label: String) {
  LENGTH("Length"),
  WEIGHT("Weight"),
  TEMP("Temp"),
  DATA("Data"),
  SPEED("Speed"),
}

private data class UnitOption(
  val symbol: String,
  val name: String,
  val toBase: (Double) -> Double,
  val fromBase: (Double) -> Double,
)

private val converterUnits: Map<ConverterCategory, List<UnitOption>> =
  mapOf(
    ConverterCategory.LENGTH to
      listOf(
        UnitOption("m", "Meters", { it }, { it }),
        UnitOption("km", "Kilometers", { it * 1000.0 }, { it / 1000.0 }),
        UnitOption("ft", "Feet", { it * 0.3048 }, { it / 0.3048 }),
        UnitOption("mi", "Miles", { it * 1609.344 }, { it / 1609.344 }),
        UnitOption("in", "Inches", { it * 0.0254 }, { it / 0.0254 }),
      ),
    ConverterCategory.WEIGHT to
      listOf(
        UnitOption("kg", "Kilograms", { it }, { it }),
        UnitOption("g", "Grams", { it / 1000.0 }, { it * 1000.0 }),
        UnitOption("lb", "Pounds", { it * 0.45359237 }, { it / 0.45359237 }),
        UnitOption("oz", "Ounces", { it * 0.0283495 }, { it / 0.0283495 }),
      ),
    ConverterCategory.TEMP to
      listOf(
        UnitOption("°C", "Celsius", { it }, { it }),
        UnitOption("°F", "Fahrenheit", { (it - 32.0) * 5.0 / 9.0 }, { it * 9.0 / 5.0 + 32.0 }),
        UnitOption("K", "Kelvin", { it - 273.15 }, { it + 273.15 }),
      ),
    ConverterCategory.DATA to
      listOf(
        UnitOption("MB", "Megabytes", { it }, { it }),
        UnitOption("GB", "Gigabytes", { it * 1024.0 }, { it / 1024.0 }),
        UnitOption("TB", "Terabytes", { it * 1048576.0 }, { it / 1048576.0 }),
        UnitOption("KB", "Kilobytes", { it / 1024.0 }, { it * 1024.0 }),
      ),
    ConverterCategory.SPEED to
      listOf(
        UnitOption("km/h", "km/h", { it / 3.6 }, { it * 3.6 }),
        UnitOption("mph", "mph", { it * 0.44704 }, { it / 0.44704 }),
        UnitOption("m/s", "m/s", { it }, { it }),
      ),
  )

@Composable
fun UnitConverterAppScreen(modifier: Modifier = Modifier) {
  var category by remember { mutableStateOf(ConverterCategory.LENGTH) }
  val units = converterUnits[category].orEmpty()
  var fromIndex by remember(category) { mutableIntStateOf(0) }
  var inputText by remember { mutableStateOf("10") }
  val inputVal = inputText.toDoubleOrNull() ?: 0.0
  val baseVal = units.getOrNull(fromIndex)?.toBase?.invoke(inputVal) ?: 0.0

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF070910))
        .padding(top = 44.dp, bottom = 64.dp, start = 18.dp, end = 18.dp)
        .testTag("tool_unit_converter"),
  ) {
    Text(
      text = "UnitForge",
      color = Color.White,
      fontSize = 22.sp,
      fontWeight = FontWeight.SemiBold,
    )
    Text(
      text = "Live multi-unit conversion",
      color = Color(0xFF38BDF8),
      fontSize = 12.5.sp,
    )

    Spacer(modifier = Modifier.height(14.dp))

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ConverterCategory.entries.forEach { cat ->
        val selected = cat == category
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = if (selected) Color(0xFF0284C7) else Color(0xFF141824),
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(14.dp))
              .clickable { category = cat },
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          ) {
            Text(
              text = cat.label,
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      OutlinedTextField(
        value = inputText,
        onValueChange = { if (it.length <= 12) inputText = it },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        label = { Text("Value") },
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF38BDF8),
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = Color(0xFF38BDF8),
            unfocusedLabelColor = Color(0xFF9CA3AF),
          ),
        modifier = Modifier.weight(1f).testTag("converter_value_input"),
      )

      Button(
        onClick = { fromIndex = (fromIndex + 1) % units.size.coerceAtLeast(1) },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(54.dp),
      ) {
        Text(
          text = "${units.getOrNull(fromIndex)?.symbol ?: ""} ⇄",
          color = Color(0xFF38BDF8),
          fontWeight = FontWeight.Bold,
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(units) { unit ->
        val converted = unit.fromBase(baseVal)
        val formatted =
          if (kotlin.math.abs(converted) >= 10000 || (converted != 0.0 && kotlin.math.abs(converted) < 0.001)) {
            String.format(Locale.US, "%.4g", converted)
          } else {
            String.format(Locale.US, "%.3f", converted).trimEnd('0').trimEnd('.')
          }
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF111521),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
          modifier = Modifier.fillMaxWidth().clickable { fromIndex = units.indexOf(unit) },
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(unit.name, color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
              Text(unit.symbol, color = Color(0xFF9CA3AF), fontSize = 12.sp)
            }
            Text(
              text = "$formatted ${unit.symbol}",
              color = Color(0xFF38BDF8),
              fontSize = 18.sp,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }
  }
}

@Composable
fun FocusTimerAppScreen(modifier: Modifier = Modifier) {
  var totalSeconds by remember { mutableIntStateOf(25 * 60) }
  var remainingSeconds by remember { mutableIntStateOf(25 * 60) }
  var isRunning by remember { mutableStateOf(false) }
  var completedSessions by remember { mutableIntStateOf(0) }
  var modeLabel by remember { mutableStateOf("Focus Session") }

  LaunchedEffect(isRunning) {
    while (isRunning && remainingSeconds > 0) {
      delay(1000L)
      remainingSeconds -= 1
      if (remainingSeconds == 0) {
        isRunning = false
        completedSessions += 1
      }
    }
  }

  fun selectPreset(minutes: Int, label: String) {
    isRunning = false
    totalSeconds = minutes * 60
    remainingSeconds = minutes * 60
    modeLabel = label
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF060A0B))
        .padding(top = 44.dp, bottom = 64.dp, start = 20.dp, end = 20.dp)
        .testTag("tool_focus_timer"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text("FocusPulse", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(modeLabel, color = Color(0xFF2DD4BF), fontSize = 12.5.sp)
      }
      Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF102422)) {
        Text(
          text = "$completedSessions done",
          color = Color(0xFF5EEAD4),
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf(25 to "Focus Session", 5 to "Short Break", 15 to "Long Break").forEach { (mins, label) ->
        OutlinedButton(
          onClick = { selectPreset(mins, label) },
          border = BorderStroke(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.45f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Text("${mins}m", fontSize = 13.sp)
        }
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    val progress = (remainingSeconds.toFloat() / totalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
    val minsDisplay = remainingSeconds / 60
    val secsDisplay = remainingSeconds % 60

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val strokePx = 12.dp.toPx()
        drawArc(
          color = Color(0xFF162124),
          startAngle = -90f,
          sweepAngle = 360f,
          useCenter = false,
          style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
        drawArc(
          color = Color(0xFF2DD4BF),
          startAngle = -90f,
          sweepAngle = 360f * progress,
          useCenter = false,
          style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
      }
      Text(
        text = String.format(Locale.US, "%02d:%02d", minsDisplay, secsDisplay),
        color = Color.White,
        fontSize = 44.sp,
        fontWeight = FontWeight.Light,
      )
    }

    Spacer(modifier = Modifier.weight(1f))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      OutlinedButton(
        onClick = {
          val updated = (remainingSeconds - 60).coerceAtLeast(60)
          remainingSeconds = updated
          totalSeconds = maxOf(totalSeconds, updated)
        },
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text("-1m")
      }
      Button(
        onClick = { isRunning = !isRunning },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14B8A6), contentColor = Color.Black),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.minimumInteractiveComponentSize().testTag("focus_start_pause_button"),
      ) {
        Text(if (isRunning) "Pause" else "Start", fontWeight = FontWeight.Bold)
      }
      Button(
        onClick = {
          isRunning = false
          remainingSeconds = totalSeconds
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text("Reset")
      }
      OutlinedButton(
        onClick = {
          remainingSeconds += 60
          totalSeconds = maxOf(totalSeconds, remainingSeconds)
        },
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text("+1m")
      }
    }
  }
}

private data class TaskItem(
  val id: String,
  val title: String,
  val isDone: Boolean,
  val isHighPriority: Boolean,
)

@Composable
fun TaskFlowAppScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("taskflow_prefs", Context.MODE_PRIVATE) }
  var tasks by remember {
    mutableStateOf(
      loadSavedTasks(prefs)
        ?: listOf(
          TaskItem(UUID.randomUUID().toString(), "Try Neon Snake & 2048 Prism", false, true),
          TaskItem(UUID.randomUUID().toString(), "Ask Infinity AI to customize the phone", false, false),
        )
    )
  }
  var newTaskText by remember { mutableStateOf("") }
  var highPriority by remember { mutableStateOf(false) }

  fun updateTasks(next: List<TaskItem>) {
    tasks = next
    saveTasks(prefs, next)
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF0A0906))
        .padding(top = 44.dp, bottom = 64.dp, start = 18.dp, end = 18.dp)
        .testTag("tool_task_flow"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text("TaskFlow", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        val remaining = tasks.count { !it.isDone }
        Text(
          text = "$remaining active · ${tasks.size - remaining} completed",
          color = Color(0xFFFBBF24),
          fontSize = 12.5.sp,
        )
      }
      if (tasks.any { it.isDone }) {
        TextButton(onClick = { updateTasks(tasks.filterNot { it.isDone }) }) {
          Text("Clear done", color = Color(0xFFFBBF24))
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedTextField(
        value = newTaskText,
        onValueChange = { newTaskText = it },
        singleLine = true,
        placeholder = { Text("Add a task…", color = Color(0xFF6B7280)) },
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFFBBF24),
            unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
          ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1f).testTag("taskflow_input"),
      )

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (highPriority) Color(0xFF78350F) else Color(0xFF18181B),
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable { highPriority = !highPriority },
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp)) {
          Text(
            text = if (highPriority) "!" else "·",
            color = if (highPriority) Color(0xFFFDE68A) else Color(0xFF9CA3AF),
            fontWeight = FontWeight.Bold,
          )
        }
      }

      IconButton(
        onClick = {
          val trimmed = newTaskText.trim()
          if (trimmed.isNotEmpty()) {
            updateTasks(listOf(TaskItem(UUID.randomUUID().toString(), trimmed, false, highPriority)) + tasks)
            newTaskText = ""
            highPriority = false
          }
        },
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .size(48.dp)
            .background(Color(0xFFFBBF24), CircleShape)
            .testTag("taskflow_add_button"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Add task", tint = Color.Black)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      contentPadding = PaddingValues(bottom = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(tasks, key = { it.id }) { task ->
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF141310),
          border =
            BorderStroke(
              1.dp,
              if (task.isHighPriority && !task.isDone) Color(0xFFFBBF24).copy(alpha = 0.5f)
              else Color.White.copy(alpha = 0.08f),
            ),
          modifier =
            Modifier.fillMaxWidth().clickable {
              updateTasks(tasks.map { if (it.id == task.id) it.copy(isDone = !it.isDone) else it })
            },
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier =
                Modifier.size(22.dp)
                  .background(
                    color = if (task.isDone) Color(0xFFFBBF24) else Color.Transparent,
                    shape = CircleShape,
                  )
                  .clip(CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              if (task.isDone) {
                Icon(Icons.Filled.Check, contentDescription = "Completed", tint = Color.Black, modifier = Modifier.size(14.dp))
              } else {
                Surface(
                  shape = CircleShape,
                  color = Color.Transparent,
                  border = BorderStroke(1.5.dp, Color(0xFFFBBF24)),
                  modifier = Modifier.fillMaxSize(),
                ) {}
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = task.title,
              color = if (task.isDone) Color(0xFF6B7280) else Color.White,
              fontSize = 14.5.sp,
              textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
              modifier = Modifier.weight(1f),
            )
            IconButton(
              onClick = { updateTasks(tasks.filterNot { it.id == task.id }) },
              modifier = Modifier.size(36.dp),
            ) {
              Icon(Icons.Filled.Delete, contentDescription = "Delete task", tint = Color(0xFF6B7280), modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }
  }
}

private fun loadSavedTasks(prefs: android.content.SharedPreferences): List<TaskItem>? {
  val raw = prefs.getString("tasks_json", null) ?: return null
  return runCatching {
    val arr = JSONArray(raw)
    buildList {
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        add(
          TaskItem(
            id = obj.getString("id"),
            title = obj.getString("title"),
            isDone = obj.getBoolean("done"),
            isHighPriority = obj.optBoolean("high", false),
          )
        )
      }
    }
  }.getOrNull()
}

private fun saveTasks(prefs: android.content.SharedPreferences, tasks: List<TaskItem>) {
  val arr = JSONArray()
  tasks.forEach { t ->
    arr.put(
      JSONObject().apply {
        put("id", t.id)
        put("title", t.title)
        put("done", t.isDone)
        put("high", t.isHighPriority)
      }
    )
  }
  prefs.edit().putString("tasks_json", arr.toString()).apply()
}
