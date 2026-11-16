package com.example.ai

import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object LocalLlmMathAndMemory {

  fun evaluateQuery(raw: String): String? {
    val trimmed = raw.trim()
    val lower = trimmed.lowercase(Locale.US)

    // 1. Unit conversions ("5 miles to km", "100 f to c", "12 kg in lbs", "2 gb to mb")
    val convMatch =
      Regex("""(-?\d+(?:\.\d+)?)\s*(km|kilometers?|mi|miles?|kg|kilograms?|lb|lbs|pounds?|c|celsius|f|fahrenheit|gb|mb|kb|hr|hrs|hours?|min|mins|minutes?)\s+(?:to|in|as)\s+(km|kilometers?|mi|miles?|kg|kilograms?|lb|lbs|pounds?|c|celsius|f|fahrenheit|gb|mb|kb|hr|hrs|hours?|min|mins|minutes?)""")
        .find(lower)
    if (convMatch != null) {
      val v = convMatch.groupValues[1].toDoubleOrNull()
      val from = normalizeUnit(convMatch.groupValues[2])
      val to = normalizeUnit(convMatch.groupValues[3])
      if (v != null && from != null && to != null) {
        val converted = convertUnits(v, from, to)
        if (converted != null) {
          return "${formatNumber(v)} $from = ${formatNumber(converted)} $to"
        }
      }
    }

    // 2. Percentage shortcuts ("15% of 240", "240 + 15%", "20% off 85")
    val pctOf = Regex("""(-?\d+(?:\.\d+)?)\s*%\s+of\s+(-?\d+(?:\.\d+)?)""").find(lower)
    if (pctOf != null) {
      val pct = pctOf.groupValues[1].toDoubleOrNull() ?: 0.0
      val base = pctOf.groupValues[2].toDoubleOrNull() ?: 0.0
      return "${formatNumber(pct)}% of ${formatNumber(base)} = ${formatNumber(base * (pct / 100.0))}"
    }
    val pctOff = Regex("""(-?\d+(?:\.\d+)?)\s*%\s+off\s+(-?\d+(?:\.\d+)?)""").find(lower)
    if (pctOff != null) {
      val pct = pctOff.groupValues[1].toDoubleOrNull() ?: 0.0
      val base = pctOff.groupValues[2].toDoubleOrNull() ?: 0.0
      val finalVal = base * (1.0 - pct / 100.0)
      return "${formatNumber(pct)}% off ${formatNumber(base)} = ${formatNumber(finalVal)}"
    }

    // 3. Extract math expression with parentheses/functions/operators
    val candidate =
      lower
        .replace("×", "*")
        .replace("x", "*")
        .replace("÷", "/")
        .replace("what is", "")
        .replace("calculate", "")
        .replace("compute", "")
        .replace("eval", "")
        .replace("=", "")
        .trim()

    val hasOperatorOrFunc =
      candidate.any { it in "+-*/^()" } ||
        candidate.contains("sqrt(") ||
        candidate.contains("sin(") ||
        candidate.contains("cos(") ||
        candidate.contains("abs(")
    if (!hasOperatorOrFunc) return null

    // Verify the stripped expression is genuinely mathematical (not plain prose)
    val allowedCharsOnly =
      candidate.replace(Regex("""sqrt|sin|cos|abs|pi|\s"""), "")
    if (allowedCharsOnly.isEmpty() || allowedCharsOnly.any { it !in "0123456789.+-*/^()" }) {
      return null
    }

    val value =
      runCatching { MathParser(candidate).parse() }.getOrNull() ?: return null
    if (value.isNaN() || value.isInfinite()) return "Undefined (division by zero or out of range)"
    return formatNumber(value)
  }

  fun summarizeText(rawText: String): String {
    val clean = rawText.trim()
    val sentences =
      clean.split(Regex("(?<=[.!?])\\s+|\\n+"))
        .map { it.trim() }
        .filter { it.length >= 12 }

    if (sentences.isEmpty()) {
      return "• ${clean.take(140)}"
    }
    if (sentences.size <= 3) {
      return sentences.joinToString("\n") { "• $it" }
    }

    val stopWords = setOf("the", "and", "for", "that", "this", "with", "from", "are", "was", "have", "your", "into", "about", "they", "will")
    val freq = HashMap<String, Int>()
    val allWords = clean.lowercase(Locale.US).split(Regex("[^a-z0-9]+")).filter { it.length >= 3 && it !in stopWords }
    allWords.forEach { freq[it] = (freq[it] ?: 0) + 1 }

    val topIndices =
      sentences.mapIndexed { idx, s ->
        val words = s.lowercase(Locale.US).split(Regex("[^a-z0-9]+"))
        val score = words.sumOf { freq[it] ?: 0 } + if (idx == 0) 2 else 0
        idx to score
      }
        .sortedByDescending { it.second }
        .take(4)
        .map { it.first }
        .sorted()

    val wordCount = clean.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    val bullets = topIndices.joinToString("\n") { "• ${sentences[it]}" }
    return "$bullets\n\n($wordCount words distilled into ${topIndices.size} key points)"
  }

  fun answerMemoryRecall(lowerQuery: String, priorUserTurns: List<String>): String? {
    if (priorUserTurns.isEmpty()) return null
    val asksMemory =
      lowerQuery.contains("what is my") ||
        lowerQuery.contains("what's my") ||
        lowerQuery.contains("who am i") ||
        lowerQuery.contains("what did i") ||
        lowerQuery.contains("do you remember") ||
        lowerQuery.contains("recall") ||
        lowerQuery.contains("earlier")

    if (!asksMemory) return null

    // Check explicit "my name is X" / "i am X"
    if (lowerQuery.contains("name") || lowerQuery.contains("who am i")) {
      for (turn in priorUserTurns.asReversed()) {
        val m = Regex("""(?:my name is|call me|i am|i'm)\s+([A-Za-z][A-Za-z0-9 _-]{1,28})""", RegexOption.IGNORE_CASE).find(turn)
        if (m != null) {
          val found = m.groupValues[1].trim().replace(Regex("[.!?]+$"), "")
          return "You told me your name is $found."
        }
      }
    }

    // Search "my <X> is <Y>" facts
    val myFieldMatch = Regex("""(?:what is|what's)\s+my\s+([a-z0-9 ]{2,24})""", RegexOption.IGNORE_CASE).find(lowerQuery)
    if (myFieldMatch != null) {
      val field = myFieldMatch.groupValues[1].trim().replace("?", "")
      for (turn in priorUserTurns.asReversed()) {
        val m = Regex("""my\s+${Regex.escape(field)}\s+(?:is|are|=|:)\s+(.+)""", RegexOption.IGNORE_CASE).find(turn)
        if (m != null) {
          return "From earlier in our conversation: your $field is ${m.groupValues[1].trim()}."
        }
      }
    }

    // Search "remember <fact>"
    for (turn in priorUserTurns.asReversed()) {
      if (turn.lowercase(Locale.US).contains("remember")) {
        val cleaned = turn.replace(Regex("^remember(?: that|:)?\\s*", RegexOption.IGNORE_CASE), "").trim()
        if (cleaned.isNotBlank()) {
          return "I remembered from our context: \"$cleaned\"."
        }
      }
    }

    if (lowerQuery.contains("what did i") || lowerQuery.contains("earlier")) {
      return "Earlier in this session you said: \"${priorUserTurns.last()}\"."
    }
    return null
  }

  fun searchDocumentForAnswer(doc: DocumentAnalysisResult, query: String): String {
    val keywords =
      query.lowercase(Locale.US)
        .split(Regex("[^a-z0-9]+"))
        .filter { it.length >= 3 }
        .toSet()
    val lines = doc.fullText.lines().map { it.trim() }.filter { it.isNotBlank() }
    val matchingLines =
      if (keywords.isEmpty()) lines.take(4)
      else {
        lines.map { line ->
          val lower = line.lowercase(Locale.US)
          line to keywords.count { lower.contains(it) }
        }
          .filter { it.second > 0 }
          .sortedByDescending { it.second }
          .take(4)
          .map { it.first }
      }

    val keysNote =
      if (doc.jsonKeys.isNotEmpty()) "\n• JSON Top Keys: ${doc.jsonKeys.joinToString(", ")}" else ""
    val matchesBlock =
      if (matchingLines.isNotEmpty()) {
        matchingLines.joinToString("\n") { "  - $it" }
      } else {
        "  - ${doc.excerpt.take(180)}"
      }

    return "Document Inspection (${doc.displayName}):\n" +
      "• Stats: ${doc.lineCount} lines, ${doc.wordCount} words, ${doc.charCount} chars$keysNote\n" +
      "• Relevant Excerpts:\n$matchesBlock"
  }

  private fun normalizeUnit(u: String): String? =
    when {
      u.startsWith("km") || u.startsWith("kilometer") -> "km"
      u == "mi" || u.startsWith("mile") -> "mi"
      u == "kg" || u.startsWith("kilogram") -> "kg"
      u.startsWith("lb") || u.startsWith("pound") -> "lbs"
      u == "c" || u == "celsius" -> "°C"
      u == "f" || u == "fahrenheit" -> "°F"
      u == "gb" -> "GB"
      u == "mb" -> "MB"
      u == "kb" -> "KB"
      u.startsWith("hr") || u.startsWith("hour") -> "hours"
      u.startsWith("min") -> "minutes"
      else -> null
    }

  private fun convertUnits(v: Double, from: String, to: String): Double? {
    if (from == to) return v
    return when ("$from->$to") {
      "km->mi" -> v * 0.621371
      "mi->km" -> v * 1.60934
      "kg->lbs" -> v * 2.20462
      "lbs->kg" -> v / 2.20462
      "°C->°F" -> v * 9.0 / 5.0 + 32.0
      "°F->°C" -> (v - 32.0) * 5.0 / 9.0
      "GB->MB" -> v * 1024.0
      "MB->GB" -> v / 1024.0
      "MB->KB" -> v * 1024.0
      "KB->MB" -> v / 1024.0
      "hours->minutes" -> v * 60.0
      "minutes->hours" -> v / 60.0
      else -> null
    }
  }

  private fun formatNumber(value: Double): String {
    val rounded = (value * 10000.0).roundToInt() / 10000.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString()
    else String.format(Locale.US, "%.4f", rounded).trimEnd('0').trimEnd('.')
  }

  private class MathParser(private val input: String) {
    private var pos = -1
    private var ch = 0

    private fun nextChar() {
      pos++
      ch = if (pos < input.length) input[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
      while (ch == ' '.code) nextChar()
      if (ch == charToEat) {
        nextChar()
        return true
      }
      return false
    }

    fun parse(): Double {
      nextChar()
      val x = parseExpression()
      while (ch == ' '.code) nextChar()
      if (pos < input.length) throw IllegalArgumentException("Unexpected char")
      return x
    }

    private fun parseExpression(): Double {
      var x = parseTerm()
      while (true) {
        when {
          eat('+'.code) -> x += parseTerm()
          eat('-'.code) -> x -= parseTerm()
          else -> return x
        }
      }
    }

    private fun parseTerm(): Double {
      var x = parseFactor()
      while (true) {
        when {
          eat('*'.code) -> x *= parseFactor()
          eat('/'.code) -> x /= parseFactor()
          else -> return x
        }
      }
    }

    private fun parseFactor(): Double {
      var x = parseUnary()
      if (eat('^'.code)) {
        x = x.pow(parseFactor())
      }
      return x
    }

    private fun parseUnary(): Double =
      when {
        eat('+'.code) -> parseUnary()
        eat('-'.code) -> -parseUnary()
        else -> parseAtom()
      }

    private fun parseAtom(): Double {
      while (ch == ' '.code) nextChar()
      val start = pos
      if (eat('('.code)) {
        val x = parseExpression()
        if (!eat(')'.code)) throw IllegalArgumentException("Missing )")
        return x
      }
      if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
        while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
        return input.substring(start, pos).toDouble()
      }
      if (ch in 'a'.code..'z'.code) {
        while (ch in 'a'.code..'z'.code) nextChar()
        val name = input.substring(start, pos)
        if (name == "pi") return Math.PI
        if (!eat('('.code)) throw IllegalArgumentException("Expected (")
        val arg = parseExpression()
        if (!eat(')'.code)) throw IllegalArgumentException("Expected )")
        return when (name) {
          "sqrt" -> sqrt(arg)
          "abs" -> abs(arg)
          "sin" -> sin(Math.toRadians(arg))
          "cos" -> cos(Math.toRadians(arg))
          else -> throw IllegalArgumentException("Unknown function $name")
        }
      }
      throw IllegalArgumentException("Invalid token")
    }
  }
}
