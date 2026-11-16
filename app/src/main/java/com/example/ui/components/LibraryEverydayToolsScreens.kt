package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

private data class ExpenseEntry(
  val id: String,
  val label: String,
  val category: String,
  val amount: Double,
  val isIncome: Boolean,
)

@Composable
fun ExpenseTrackerAppScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("pennywise_prefs", Context.MODE_PRIVATE) }
  var entries by remember { mutableStateOf(loadExpenses(prefs)) }
  var titleInput by remember { mutableStateOf("") }
  var amountInput by remember { mutableStateOf("") }
  var selectedCat by remember { mutableStateOf("Food") }
  var isIncome by remember { mutableStateOf(false) }

  val categories = listOf("Food", "Transit", "Bills", "Shopping", "Income")

  fun save(next: List<ExpenseEntry>) {
    entries = next
    val arr = JSONArray()
    next.forEach { e ->
      arr.put(
        JSONObject().apply {
          put("id", e.id)
          put("label", e.label)
          put("cat", e.category)
          put("amt", e.amount)
          put("inc", e.isIncome)
        }
      )
    }
    prefs.edit().putString("entries", arr.toString()).apply()
  }

  val totalIncome = entries.filter { it.isIncome }.sumOf { it.amount }
  val totalSpend = entries.filterNot { it.isIncome }.sumOf { it.amount }
  val netBalance = totalIncome - totalSpend

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF07090F))
        .padding(top = 42.dp, bottom = 62.dp, start = 18.dp, end = 18.dp)
        .testTag("lib_app_expense_tracker"),
  ) {
    Text("PennyWise Budget", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Text("Daily expense & income ledger", color = Color(0xFF34D399), fontSize = 12.sp)

    Spacer(modifier = Modifier.height(12.dp))

    Surface(
      shape = RoundedCornerShape(18.dp),
      color = Color(0xFF10181D),
      border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.40f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text("Net Balance", color = Color(0xFF9CA3AF), fontSize = 12.sp)
          Text(
            text = String.format(Locale.US, "%s$%.2f", if (netBalance < 0) "-" else "", kotlin.math.abs(netBalance)),
            color = if (netBalance >= 0) Color(0xFF34D399) else Color(0xFFF87171),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text("+$${String.format(Locale.US, "%.2f", totalIncome)}", color = Color(0xFF34D399), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
          Text("-$${String.format(Locale.US, "%.2f", totalSpend)}", color = Color(0xFFF87171), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      categories.forEach { cat ->
        val selected = cat == selectedCat
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = if (selected) Color(0xFF065F46) else Color(0xFF131622),
          border = BorderStroke(1.dp, if (selected) Color(0xFF34D399) else Color.White.copy(alpha = 0.08f)),
          modifier =
            Modifier.clip(RoundedCornerShape(14.dp)).clickable {
              selectedCat = cat
              isIncome = cat == "Income"
            },
        ) {
          Text(
            text = cat,
            color = if (selected) Color.White else Color(0xFF9CA3AF),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        value = titleInput,
        onValueChange = { titleInput = it },
        singleLine = true,
        placeholder = { Text("Description", color = Color(0xFF6B7280)) },
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF34D399)),
        modifier = Modifier.weight(1.2f),
      )
      OutlinedTextField(
        value = amountInput,
        onValueChange = { amountInput = it },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        placeholder = { Text("$", color = Color(0xFF6B7280)) },
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF34D399)),
        modifier = Modifier.weight(0.7f),
      )
      IconButton(
        onClick = {
          val amt = amountInput.toDoubleOrNull()
          if (amt != null && amt > 0.0) {
            val lbl = titleInput.trim().ifBlank { selectedCat }
            save(listOf(ExpenseEntry(UUID.randomUUID().toString(), lbl, selectedCat, amt, isIncome)) + entries)
            titleInput = ""
            amountInput = ""
          }
        },
        modifier = Modifier.minimumInteractiveComponentSize().background(Color(0xFF34D399), CircleShape),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Add entry", tint = Color.Black)
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(entries, key = { it.id }) { e ->
        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF121521), modifier = Modifier.fillMaxWidth()) {
          Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
              Text(e.label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
              Text(e.category, color = Color(0xFF9CA3AF), fontSize = 11.5.sp)
            }
            Text(
              text = "${if (e.isIncome) "+" else "-"}$${String.format(Locale.US, "%.2f", e.amount)}",
              color = if (e.isIncome) Color(0xFF34D399) else Color(0xFFF87171),
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
            )
            IconButton(onClick = { save(entries.filterNot { it.id == e.id }) }) {
              Icon(Icons.Filled.Delete, contentDescription = "Remove entry", tint = Color(0xFF6B7280), modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }
  }
}

private data class HabitItem(
  val id: String,
  val name: String,
  val streak: Int,
  val doneToday: Boolean,
)

@Composable
fun HabitForgeAppScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("habitforge_prefs", Context.MODE_PRIVATE) }
  var habits by remember { mutableStateOf(loadHabits(prefs)) }
  var newHabitName by remember { mutableStateOf("") }

  fun save(next: List<HabitItem>) {
    habits = next
    val arr = JSONArray()
    next.forEach { h ->
      arr.put(
        JSONObject().apply {
          put("id", h.id)
          put("name", h.name)
          put("streak", h.streak)
          put("done", h.doneToday)
        }
      )
    }
    prefs.edit().putString("habits", arr.toString()).apply()
  }

  val doneCount = habits.count { it.doneToday }
  val pct = if (habits.isEmpty()) 0f else doneCount.toFloat() / habits.size

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF07090F))
        .padding(top = 42.dp, bottom = 62.dp, start = 18.dp, end = 18.dp)
        .testTag("lib_app_habit_forge"),
  ) {
    Text("HabitForge", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Text("$doneCount of ${habits.size} daily habits completed", color = Color(0xFFFB923C), fontSize = 12.sp)

    Spacer(modifier = Modifier.height(10.dp))
    LinearProgressIndicator(
      progress = { pct },
      color = Color(0xFFFB923C),
      trackColor = Color(0xFF1F2433),
      modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
    )

    Spacer(modifier = Modifier.height(14.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        value = newHabitName,
        onValueChange = { newHabitName = it },
        singleLine = true,
        placeholder = { Text("Add daily habit…", color = Color(0xFF6B7280)) },
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFB923C)),
        modifier = Modifier.weight(1f),
      )
      IconButton(
        onClick = {
          val n = newHabitName.trim()
          if (n.isNotEmpty()) {
            save(listOf(HabitItem(UUID.randomUUID().toString(), n, 0, false)) + habits)
            newHabitName = ""
          }
        },
        modifier = Modifier.minimumInteractiveComponentSize().background(Color(0xFFFB923C), CircleShape),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Add habit", tint = Color.Black)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      items(habits, key = { it.id }) { h ->
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = if (h.doneToday) Color(0xFF1A1410) else Color(0xFF121521),
          border = BorderStroke(1.dp, if (h.doneToday) Color(0xFFFB923C).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)),
          modifier =
            Modifier.fillMaxWidth().clickable {
              val nextDone = !h.doneToday
              val nextStreak = if (nextDone) h.streak + 1 else (h.streak - 1).coerceAtLeast(0)
              save(habits.map { if (it.id == h.id) it.copy(doneToday = nextDone, streak = nextStreak) else it })
            },
        ) {
          Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
              checked = h.doneToday,
              onCheckedChange = { checked ->
                val nextStreak = if (checked) h.streak + 1 else (h.streak - 1).coerceAtLeast(0)
                save(habits.map { if (it.id == h.id) it.copy(doneToday = checked, streak = nextStreak) else it })
              },
              colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFB923C), checkmarkColor = Color.Black),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = h.name,
                color = if (h.doneToday) Color(0xFFD1D5DB) else Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (h.doneToday) TextDecoration.LineThrough else TextDecoration.None,
              )
              Text(
                text = "Streak: ${h.streak} day(s)",
                color = Color(0xFFFB923C),
                fontSize = 11.5.sp,
              )
            }
            IconButton(onClick = { save(habits.filterNot { it.id == h.id }) }) {
              Icon(Icons.Filled.Delete, contentDescription = "Delete habit", tint = Color(0xFF6B7280), modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
fun SplitTipAppScreen(modifier: Modifier = Modifier) {
  var billInput by remember { mutableStateOf("48.00") }
  var tipPercent by remember { mutableFloatStateOf(18f) }
  var peopleCount by remember { mutableIntStateOf(2) }

  val bill = billInput.toDoubleOrNull() ?: 0.0
  val tipAmt = bill * (tipPercent / 100.0)
  val total = bill + tipAmt
  val perPerson = total / peopleCount.coerceAtLeast(1)

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF07090F))
        .padding(top = 42.dp, bottom = 62.dp, start = 18.dp, end = 18.dp)
        .testTag("lib_app_split_tip"),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Text("SplitBill & Tip", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Text("Fast tip & group split calculator", color = Color(0xFF60A5FA), fontSize = 12.sp)

    OutlinedTextField(
      value = billInput,
      onValueChange = { billInput = it },
      label = { Text("Bill Amount ($)") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF60A5FA)),
      modifier = Modifier.fillMaxWidth(),
    )

    Column {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Tip Percentage", color = Color(0xFF9CA3AF), fontSize = 13.sp)
        Text("${tipPercent.roundToInt()}%", color = Color(0xFF60A5FA), fontSize = 15.sp, fontWeight = FontWeight.Bold)
      }
      Slider(
        value = tipPercent,
        onValueChange = { tipPercent = it },
        valueRange = 0f..35f,
        colors = SliderDefaults.colors(thumbColor = Color(0xFF60A5FA), activeTrackColor = Color(0xFF60A5FA)),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(10f, 15f, 18f, 20f, 25f).forEach { pct ->
          OutlinedButton(
            onClick = { tipPercent = pct },
            border = BorderStroke(1.dp, if (tipPercent.roundToInt() == pct.toInt()) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.14f)),
            modifier = Modifier.weight(1f),
          ) {
            Text("${pct.toInt()}%", fontSize = 11.sp, color = Color.White)
          }
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("Split Between", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { peopleCount = (peopleCount - 1).coerceAtLeast(1) }) { Text("−", color = Color.White) }
        Text("$peopleCount", color = Color(0xFF60A5FA), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = { peopleCount = (peopleCount + 1).coerceAtMost(20) }) { Text("+", color = Color.White) }
      }
    }

    Surface(
      shape = RoundedCornerShape(18.dp),
      color = Color(0xFF101827),
      border = BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.45f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Tip Amount", color = Color(0xFF9CA3AF))
          Text(String.format(Locale.US, "$%.2f", tipAmt), color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Total Bill", color = Color(0xFF9CA3AF))
          Text(String.format(Locale.US, "$%.2f", total), color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Each Person Pays", color = Color.White, fontWeight = FontWeight.Bold)
          Text(
            text = String.format(Locale.US, "$%.2f", perPerson),
            color = Color(0xFF60A5FA),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
fun CipherKeyAppScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  var length by remember { mutableFloatStateOf(16f) }
  var includeUpper by remember { mutableStateOf(true) }
  var includeDigits by remember { mutableStateOf(true) }
  var includeSymbols by remember { mutableStateOf(true) }
  var copiedNotice by remember { mutableStateOf(false) }

  fun generatePassword(): String {
    val random = SecureRandom()
    val pool = buildString {
      append("abcdefghijkmnopqrstuvwxyz")
      if (includeUpper) append("ABCDEFGHJKLMNPQRSTUVWXYZ")
      if (includeDigits) append("23456789")
      if (includeSymbols) append("!@#$%&*+-_=?")
    }
    return buildString {
      repeat(length.roundToInt().coerceIn(8, 32)) {
        append(pool[random.nextInt(pool.length)])
      }
    }
  }

  var generated by remember { mutableStateOf(generatePassword()) }

  val strengthLabel =
    when {
      length >= 16 && includeUpper && includeDigits && includeSymbols -> "Fortress · Very Strong"
      length >= 12 && (includeDigits || includeSymbols) -> "Strong"
      else -> "Moderate"
    }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF07090F))
        .padding(top = 42.dp, bottom = 62.dp, start = 18.dp, end = 18.dp)
        .testTag("lib_app_cipher_key"),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Text("CipherKey Generator", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Text("Offline cryptographic password generator", color = Color(0xFFC084FC), fontSize = 12.sp)

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color(0xFF141122),
      border = BorderStroke(1.dp, Color(0xFFC084FC).copy(alpha = 0.5f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = generated,
          color = Color.White,
          fontSize = 19.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = if (copiedNotice) "Copied to clipboard!" else strengthLabel,
          color = if (copiedNotice) Color(0xFF4ADE80) else Color(0xFFC084FC),
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
      Button(
        onClick = {
          generated = generatePassword()
          copiedNotice = false
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
        modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
      ) {
        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Generate")
      }
      OutlinedButton(
        onClick = {
          val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
          cm?.setPrimaryClip(ClipData.newPlainText("CipherKey", generated))
          copiedNotice = true
        },
        border = BorderStroke(1.dp, Color(0xFFC084FC)),
        modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
      ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Copy", color = Color.White)
      }
    }

    Column {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Length", color = Color(0xFF9CA3AF))
        Text("${length.roundToInt()} chars", color = Color(0xFFC084FC), fontWeight = FontWeight.Bold)
      }
      Slider(
        value = length,
        onValueChange = {
          length = it
          generated = generatePassword()
          copiedNotice = false
        },
        valueRange = 8f..28f,
        colors = SliderDefaults.colors(thumbColor = Color(0xFFC084FC), activeTrackColor = Color(0xFFC084FC)),
      )
    }

    CipherToggleRow("Uppercase Letters (A-Z)", includeUpper) {
      includeUpper = it
      generated = generatePassword()
    }
    CipherToggleRow("Digits (2-9)", includeDigits) {
      includeDigits = it
      generated = generatePassword()
    }
    CipherToggleRow("Symbols (!@#\$%&)", includeSymbols) {
      includeSymbols = it
      generated = generatePassword()
    }
  }
}

@Composable
private fun CipherToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, color = Color.White, fontSize = 14.sp)
    Switch(
      checked = checked,
      onCheckedChange = onChange,
      colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF9333EA)),
    )
  }
}

private fun loadExpenses(prefs: android.content.SharedPreferences): List<ExpenseEntry> {
  val raw = prefs.getString("entries", null) ?: return listOf(
    ExpenseEntry("seed1", "Coffee & Breakfast", "Food", 6.50, false),
    ExpenseEntry("seed2", "Transit Pass", "Transit", 12.00, false),
  )
  return runCatching {
    val arr = JSONArray(raw)
    buildList {
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(ExpenseEntry(o.getString("id"), o.getString("label"), o.getString("cat"), o.getDouble("amt"), o.getBoolean("inc")))
      }
    }
  }.getOrDefault(emptyList())
}

private fun loadHabits(prefs: android.content.SharedPreferences): List<HabitItem> {
  val raw = prefs.getString("habits", null) ?: return listOf(
    HabitItem("h1", "Drink 8 glasses of water", 2, false),
    HabitItem("h2", "20 min Deep Reading", 5, false),
    HabitItem("h3", "Evening Walk", 1, false),
  )
  return runCatching {
    val arr = JSONArray(raw)
    buildList {
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(HabitItem(o.getString("id"), o.getString("name"), o.getInt("streak"), o.getBoolean("done")))
      }
    }
  }.getOrDefault(emptyList())
}
