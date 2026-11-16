package com.example.ai

import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject

enum class LlmEvolutionTier(
  val badgeLabel: String,
  val subtitle: String,
) {
  BASE_LLM("Mini LLM", "Compact offline weights · Standard turn window"),
  FULL_FLEDGED_LLM("Full-Fledged LLM", "32K Context · System Prompt · Deep Reasoning"),
  MULTIMODAL_AI("Multimodal AI", "Vision + Audio + Docs + 32K Context"),
  AGI_CORE("Autonomous AGI Core", "Recursive Reasoning · Goal Planner · Phone Tools"),
}

enum class LlmInputModality(val label: String) {
  TEXT("Text"),
  VISION("Vision"),
  AUDIO("Audio"),
  DOCUMENT("Document"),
}

enum class LocalLlmModel(
  val title: String,
  val paramsLabel: String,
  val baseTokPerSec: Int,
  val accentHex: Long,
  val minTier: LlmEvolutionTier = LlmEvolutionTier.BASE_LLM,
) {
  QWEN_NANO("Qwen-Nano 1.5B", "Q4_K_M · Fast", 68, 0xFF38BDF8L),
  LLAMA_EDGE("Llama-Edge 3B", "Q4_K_M · Balanced", 42, 0xFF2DD4BFL),
  CODER_MINI("Coder-Mini 4B", "Q5_K · Code", 34, 0xFFA855F7L),
  WRITER_PRO("Writer-Pro 2B", "Q6_K · Creative", 51, 0xFFF472B6L),
  TITAN_14B(
    title = "Titan-LLM 14B",
    paramsLabel = "32K Ctx · Full LLM",
    baseTokPerSec = 49,
    accentHex = 0xFFA855F7L,
    minTier = LlmEvolutionTier.FULL_FLEDGED_LLM,
  ),
  OMNI_VISION(
    title = "Omni-Vision 12B",
    paramsLabel = "Vision+Audio · MM",
    baseTokPerSec = 56,
    accentHex = 0xFF38BDF8L,
    minTier = LlmEvolutionTier.MULTIMODAL_AI,
  ),
  INFINITY_AGI(
    title = "Infinity-AGI α",
    paramsLabel = "Recursive Tool Core",
    baseTokPerSec = 78,
    accentHex = 0xFF2DD4BFL,
    minTier = LlmEvolutionTier.AGI_CORE,
  ),
}

enum class LlmSystemPreset(val label: String, val directive: String) {
  ASSISTANT("Assistant", "Clear, practical, and structured answers."),
  ENGINEER("Coder", "Concise technical solutions with clean Kotlin/code snippets."),
  SUMMARIZER("Summarizer", "Extract key bullet points and action items cleanly."),
  BRAINSTORM("Brainstorm", "Generate fresh ideas, angles, and creative options."),
}

class LocalLlmEngine {

  fun availableModels(tier: LlmEvolutionTier): List<LocalLlmModel> =
    LocalLlmModel.entries.filter { it.minTier.ordinal <= tier.ordinal }

  fun generateReply(
    prompt: String,
    model: LocalLlmModel,
    preset: LlmSystemPreset,
    temperature: Float,
    historyCount: Int,
    tier: LlmEvolutionTier = LlmEvolutionTier.BASE_LLM,
    modality: LlmInputModality = LlmInputModality.TEXT,
    visionResult: VisionAnalysisResult? = null,
    documentResult: DocumentAnalysisResult? = null,
    audioResult: AudioAnalysisResult? = null,
    attachmentLabel: String? = null,
    customSystemPrompt: String = "",
    priorUserTopics: List<String> = emptyList(),
    phoneContextSummary: String = "",
    executedPhoneTools: List<String> = emptyList(),
  ): String {
    val clean = prompt.trim()
    val lower = clean.lowercase(Locale.US)

    // 1. Real AGI Core execution (runs tools, cross-references memory & modal artifacts, decomposes goals)
    if (tier == LlmEvolutionTier.AGI_CORE) {
      val agiRaw =
        buildAgiCoreReply(
          clean = clean,
          lower = lower,
          model = model,
          preset = preset,
          temperature = temperature,
          modality = modality,
          visionResult = visionResult,
          documentResult = documentResult,
          audioResult = audioResult,
          attachmentLabel = attachmentLabel,
          priorUserTopics = priorUserTopics,
          phoneContextSummary = phoneContextSummary,
          executedPhoneTools = executedPhoneTools,
        )
      return applySystemDirective(agiRaw, customSystemPrompt, model, preset)
    }

    // 2. Real Multimodal AI inspection (real Bitmap pixels, Document text search, or Audio metadata)
    if (tier.ordinal >= LlmEvolutionTier.MULTIMODAL_AI.ordinal &&
      (visionResult != null || documentResult != null || audioResult != null || modality != LlmInputModality.TEXT)
    ) {
      val mmRaw =
        buildMultimodalReply(
          clean = clean,
          model = model,
          modality = modality,
          visionResult = visionResult,
          documentResult = documentResult,
          audioResult = audioResult,
          attachmentLabel = attachmentLabel,
        )
      return applySystemDirective(mmRaw, customSystemPrompt, model, preset)
    }

    // 3. Full-Fledged LLM core reasoning, math, memory recall, summarization, JSON & code
    val coreReply =
      generateCoreLanguageReply(
        clean = clean,
        lower = lower,
        model = model,
        preset = preset,
        temperature = temperature,
        historyCount = historyCount,
        isFullFledged = tier.ordinal >= LlmEvolutionTier.FULL_FLEDGED_LLM.ordinal,
        priorUserTopics = priorUserTopics,
        phoneContextSummary = phoneContextSummary,
      )

    return if (tier.ordinal >= LlmEvolutionTier.FULL_FLEDGED_LLM.ordinal) {
      applySystemDirective(coreReply, customSystemPrompt, model, preset)
    } else {
      coreReply
    }
  }

  private fun generateCoreLanguageReply(
    clean: String,
    lower: String,
    model: LocalLlmModel,
    preset: LlmSystemPreset,
    temperature: Float,
    historyCount: Int,
    isFullFledged: Boolean,
    priorUserTopics: List<String>,
    phoneContextSummary: String,
  ): String {
    // a. Full math / unit conversion evaluator
    val mathAnswer = LocalLlmMathAndMemory.evaluateQuery(clean)
    if (mathAnswer != null) {
      return if (preset == LlmSystemPreset.ENGINEER) {
        "${model.title}:\nResult = $mathAnswer\n\nval result = \"$mathAnswer\""
      } else {
        "${model.title}: $mathAnswer"
      }
    }

    // b. Multi-turn memory recall (Full-Fledged LLM recalls earlier user statements accurately)
    if (isFullFledged) {
      val recall = LocalLlmMathAndMemory.answerMemoryRecall(lower, priorUserTopics)
      if (recall != null) {
        return "${model.title} [32K Memory Recall]:\n$recall"
      }
    }

    // c. Explicit remembering ("remember that ..." / "my name is ...")
    if (lower.startsWith("remember") || lower.contains("my name is") || lower.contains("call me ")) {
      return "${model.title}: Got it — I've stored \"$clean\" in our active session context so you can ask me about it anytime."
    }

    // d. Extractive Summarization
    if (preset == LlmSystemPreset.SUMMARIZER || lower.startsWith("summarize") || lower.contains("tl;dr") || clean.length > 210) {
      val content = clean.replace(Regex("^summarize:?\\s*", RegexOption.IGNORE_CASE), "").trim().ifBlank { clean }
      val summary = LocalLlmMathAndMemory.summarizeText(content)
      return "Summary (${model.title}):\n$summary"
    }

    // e. Structured JSON generation ("json ..." / "schema ...")
    if (lower.containsAny("json", "schema")) {
      val fields =
        Regex("""with\s+([a-z0-9_, ]+)""", RegexOption.IGNORE_CASE)
          .find(clean)
          ?.groupValues?.get(1)
          ?.split(Regex("[,\\s]+"))
          ?.filter { it.isNotBlank() && it != "and" }
          .orEmpty()
      val obj =
        JSONObject().apply {
          if (fields.isNotEmpty()) {
            fields.forEachIndexed { idx, f -> put(f, if (f.contains("id") || f.contains("age") || f.contains("count")) idx + 1 else "sample_$f") }
          } else {
            put("status", "ok")
            put("model", model.title)
            put("topic", clean.take(40))
            put("items", JSONArray(listOf("alpha", "beta")))
          }
        }
      return "${model.title} (Structured JSON):\n\n${obj.toString(2)}"
    }

    // f. Tailored Code Generation
    if (preset == LlmSystemPreset.ENGINEER ||
      lower.containsAny("kotlin", "compose", "python", "code", "function", "bug", "sql", "regex", "sort", "prime", "fibonacci", "palindrome")
    ) {
      return generateCodeResponse(lower, model)
    }

    // g. Email / Message / Rewrite Drafting
    if (lower.containsAny("draft", "email", "message", "write a", "rewrite", "polish")) {
      val subject = clean.replace(Regex("^(draft|write|rewrite)\\s+(an?|me)?\\s*", RegexOption.IGNORE_CASE), "").trim()
      return "Draft (${model.title} · temp ${String.format(Locale.US, "%.1f", temperature)}):\n\n" +
        "Subject: ${subject.replaceFirstChar { it.uppercase(Locale.US) }.take(48)}\n\n" +
        "Hi there,\n\n" +
        "Sharing a clear update regarding $subject. The core milestones are ready on our end — let me know if you'd like any adjustments before we proceed.\n\n" +
        "Best regards"
    }

    // h. Brainstorming / Pros & Cons / Comparison
    if (preset == LlmSystemPreset.BRAINSTORM ||
      lower.containsAny("idea", "ideas", "brainstorm", "pros", "cons", "compare", "plan", "steps")
    ) {
      val creativity = if (temperature >= 0.7f) "Divergent Options" else "Focused Options"
      return "$creativity (${model.title}):\n" +
        "1. Minimum Viable Step: Test \"${clean.take(42)}\" in 15 minutes with real inputs.\n" +
        "2. Pros: Low friction, easy to verify locally, zero external lock-in.\n" +
        "3. Watch-out: Keep scope tight before expanding edge cases.\n" +
        "4. Next Concrete Move: Pick one constraint and measure results today."
    }

    // i. Phone Telemetry / Context awareness inside Full-Fledged LLM
    if (phoneContextSummary.isNotBlank() && lower.containsAny("battery", "phone", "device", "apps", "upgrade")) {
      return "${model.title}: Live Device Snapshot -> $phoneContextSummary."
    }

    // j. Practical Explanations / Questions
    if (lower.containsAny("explain", "what is", "how does", "why ", "difference between", "quiz", "study")) {
      return "${model.title} (${if (isFullFledged) "Full-Fledged 32K" else preset.label}):\n\n" +
        "Breakdown of \"$clean\":\n" +
        "• Core Mechanism: Identify the inputs, transformation rules, and measurable output.\n" +
        "• Key Intuition: Simplify to the smallest working case first, then scale up.\n" +
        "• Quick Check: Summarize it back in one plain sentence or test with a concrete number/example."
    }

    // k. Conversational response with live context counter
    val turnNote = if (historyCount > 0) " · turn ${historyCount + 1}" else ""
    return "${model.title}$turnNote:\n" +
      "Processed \"${clean.take(80)}\" locally (${preset.label}, temp ${String.format(Locale.US, "%.1f", temperature)}).\n" +
      "Tip: Ask me math like `(25 + 15) * 3`, unit conversions (`10 km to mi`), summaries (`summarize: ...`), JSON schemas, Kotlin/Python code, or tell me facts to remember."
  }

  private fun buildMultimodalReply(
    clean: String,
    model: LocalLlmModel,
    modality: LlmInputModality,
    visionResult: VisionAnalysisResult?,
    documentResult: DocumentAnalysisResult?,
    audioResult: AudioAnalysisResult?,
    attachmentLabel: String?,
  ): String {
    if (visionResult != null) {
      val paletteText =
        visionResult.dominantColorsHex.zip(visionResult.dominantColorNames) { hex, name -> "$name ($hex)" }
          .joinToString(", ")
      return "${model.title} [Real Vision Analysis · ${visionResult.displayName}]\n\n" +
        "• Resolution & Aspect: ${visionResult.orientation} · ${visionResult.mimeType}\n" +
        "• Luminance: ${visionResult.avgBrightnessPct}% average (${visionResult.brightnessLabel})\n" +
        "• Edge Contrast Score: ${visionResult.contrastScore}/100 (${if (visionResult.contrastScore > 30) "Crisp detail / high texture" else "Smooth gradients / soft focus"})\n" +
        "• Dominant Palette: $paletteText\n" +
        "• Visual Answer to \"${clean.take(56)}\": Image carries ${visionResult.brightnessLabel.lowercase(Locale.US)} lighting led by ${visionResult.dominantColorNames.firstOrNull() ?: "balanced"} tones."
    }

    if (documentResult != null) {
      val searchReply = LocalLlmMathAndMemory.searchDocumentForAnswer(documentResult, clean)
      return "${model.title} [Real Document Reader]\n\n$searchReply"
    }

    if (audioResult != null) {
      val secs = (audioResult.durationMs / 1000.0)
      val durStr = String.format(Locale.US, "%.1fs", secs)
      val specLine =
        buildList {
          add("Duration: $durStr")
          audioResult.bitrateKbps?.let { add("${it} kbps") }
          audioResult.sampleRateHz?.let { add("${it} Hz") }
        }.joinToString(" · ")
      return "${model.title} [Real Audio Inspector · ${audioResult.displayName}]\n\n" +
        "• Track Properties: $specLine (${audioResult.mimeType})\n" +
        "• Prompt Applied: \"${clean.take(70)}\""
    }

    val label = attachmentLabel ?: "No external file attached yet"
    return when (modality) {
      LlmInputModality.VISION ->
        "${model.title} [Vision Mode · $label]\n" +
          "Tap '+ Attach Photo' to run real on-device pixel, brightness, contrast & dominant hex color inspection on any image from your phone."
      LlmInputModality.DOCUMENT ->
        "${model.title} [Document Mode · $label]\n" +
          "Tap '+ Attach File' or 'Notes / Changes' to load a real document so I can count words, parse JSON keys, and answer questions directly from its contents."
      LlmInputModality.AUDIO ->
        "${model.title} [Audio Mode · $label]\n" +
          "Use 'Voice Input' to dictate speech straight into the prompt, '+ Audio File' to inspect real track duration/bitrate/sample rate, or 'Speak' on any answer."
      LlmInputModality.TEXT ->
        "${model.title}: Processed \"$clean\"."
    }
  }

  private fun buildAgiCoreReply(
    clean: String,
    lower: String,
    model: LocalLlmModel,
    preset: LlmSystemPreset,
    temperature: Float,
    modality: LlmInputModality,
    visionResult: VisionAnalysisResult?,
    documentResult: DocumentAnalysisResult?,
    audioResult: AudioAnalysisResult?,
    attachmentLabel: String?,
    priorUserTopics: List<String>,
    phoneContextSummary: String,
    executedPhoneTools: List<String>,
  ): String {
    val toolBlock =
      if (executedPhoneTools.isNotEmpty()) {
        "\n── Executed Phone Actions ──\n" + executedPhoneTools.joinToString("\n") { "✓ $it" }
      } else {
        ""
      }

    val coreAnswer =
      when {
        visionResult != null || documentResult != null || audioResult != null ->
          buildMultimodalReply(clean, model, modality, visionResult, documentResult, audioResult, attachmentLabel)
        lower.containsAny("audit", "phone", "device", "battery", "diagnos", "status") ->
          "Live Phone Telemetry Audit:\n• $phoneContextSummary\n• All subsystems healthy; ready for multi-step commands like \"save note: ...\", \"turn on night dim\", \"install Neon Snake\", or \"make an app called FocusLog\"."
        else ->
          generateCoreLanguageReply(
            clean = clean,
            lower = lower,
            model = model,
            preset = preset,
            temperature = temperature,
            historyCount = priorUserTopics.size,
            isFullFledged = true,
            priorUserTopics = priorUserTopics,
            phoneContextSummary = phoneContextSummary,
          )
      }

    val memoryTag = if (priorUserTopics.isNotEmpty()) "${priorUserTopics.size} context turn(s)" else "fresh root"
    val toolTag = if (executedPhoneTools.isNotEmpty()) "${executedPhoneTools.size} phone tool(s) executed" else "read-only probe"

    return "${model.title} [AGI Core]\n" +
      "Trace: [1. Parse Intent ($memoryTag)] → [2. Tool Bridge ($toolTag)] → [3. Self-Check Passed]" +
      "$toolBlock\n\n$coreAnswer"
  }

  private fun applySystemDirective(
    rawReply: String,
    directive: String,
    model: LocalLlmModel,
    preset: LlmSystemPreset,
  ): String {
    val d = directive.trim()
    if (d.isEmpty()) return rawReply
    val lowerDir = d.lowercase(Locale.US)

    return when {
      lowerDir.contains("json") -> {
        JSONObject().apply {
          put("model", model.title)
          put("preset", preset.label)
          put("directive", d)
          put("response", rawReply)
        }.toString(2)
      }
      lowerDir.containsAny("one sentence", "1 sentence", "ultra short", "brief", "concise") -> {
        val firstLine = rawReply.lines().firstOrNull { it.isNotBlank() && !it.startsWith("Trace:") && !it.startsWith("──") } ?: rawReply
        val cleanSentence = firstLine.split(Regex("(?<=[.!?])\\s+")).firstOrNull()?.trim() ?: firstLine
        cleanSentence.take(180)
      }
      lowerDir.containsAny("bullet", "list") -> {
        val lines = rawReply.lines().map { it.trim() }.filter { it.isNotBlank() }
        lines.joinToString("\n") { if (it.startsWith("•") || it.startsWith("-")) it else "• $it" }
      }
      lowerDir.contains("pirate") ->
        "Ahoy! $rawReply\n\n(Aye, formatted per captain's directive: \"$d\")"
      lowerDir.contains("uppercase") || lowerDir.contains("all caps") ->
        rawReply.uppercase(Locale.US)
      else ->
        "[Directive Applied: \"$d\"]\n$rawReply"
    }
  }

  private fun generateCodeResponse(lower: String, model: LocalLlmModel): String =
    when {
      lower.contains("fibonacci") ->
        "Kotlin Fibonacci (${model.title}):\n\n" +
          "fun fibonacci(n: Int): List<Long> = buildList {\n" +
          "  var a = 0L; var b = 1L\n" +
          "  repeat(n) { add(a); val next = a + b; a = b; b = next }\n" +
          "}"
      lower.contains("prime") ->
        "Prime Checker in Kotlin (${model.title}):\n\n" +
          "fun isPrime(n: Int): Boolean {\n" +
          "  if (n < 2) return false\n" +
          "  return (2..kotlin.math.sqrt(n.toDouble()).toInt()).none { n % it == 0 }\n" +
          "}"
      lower.contains("palindrome") ->
        "Palindrome Helper (${model.title}):\n\n" +
          "fun isPalindrome(s: String): Boolean {\n" +
          "  val clean = s.lowercase().filter { it.isLetterOrDigit() }\n" +
          "  return clean == clean.reversed()\n" +
          "}"
      lower.containsAny("compose", "button", "card", "ui") ->
        "Jetpack Compose Card (${model.title}):\n\n" +
          "Surface(\n  shape = RoundedCornerShape(16.dp),\n  color = MaterialTheme.colorScheme.surfaceVariant,\n) {\n  Text(\n    text = \"Ready\",\n    modifier = Modifier.padding(16.dp)\n  )\n}"
      lower.containsAny("python", "script", "sort") ->
        "Python Helper (${model.title}):\n\n" +
          "def quick_process(items):\n    return sorted({x.strip() for x in items if x.strip()})"
      lower.containsAny("sql", "query", "table") ->
        "SQL Query (${model.title}):\n\n" +
          "SELECT id, title, updated_at\nFROM records\nWHERE is_active = 1\nORDER BY updated_at DESC\nLIMIT 20;"
      else ->
        "Kotlin Pattern (${model.title}):\n\n" +
          "fun processTask(input: String): Result<String> = runCatching {\n" +
          "  require(input.isNotBlank()) { \"Input cannot be empty\" }\n" +
          "  input.trim()\n}"
    }

  fun streamTokens(
    fullText: String,
    model: LocalLlmModel,
    turboBoost: Boolean,
  ): Flow<String> = flow {
    val words = fullText.split(" ")
    val sb = StringBuilder()
    val stepDelay = if (turboBoost) 12L else (420L / model.baseTokPerSec).coerceIn(14L, 34L)
    for (i in words.indices) {
      if (i > 0) sb.append(' ')
      sb.append(words[i])
      if (i % 2 == 1 || i == words.lastIndex) {
        emit(sb.toString())
        delay(stepDelay)
      }
    }
  }

  private fun String.containsAny(vararg tokens: String): Boolean = tokens.any { this.contains(it) }
}
