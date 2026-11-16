package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AudioAnalysisResult
import com.example.ai.DocumentAnalysisResult
import com.example.ai.LlmEvolutionTier
import com.example.ai.LlmInputModality
import com.example.ai.LlmSystemPreset
import com.example.ai.LocalLlmEngine
import com.example.ai.LocalLlmMediaAnalyzer
import com.example.ai.LocalLlmModel
import com.example.ai.PhoneUpgradeId
import com.example.ai.VisionAnalysisResult
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private data class LocalLlmChatTurn(
  val id: String,
  val isUser: Boolean,
  val text: String,
  val modelLabel: String,
)

@Composable
fun LocalLlmAppIcon(
  modifier: Modifier = Modifier,
  iconSize: Dp = 54.dp,
  cornerRadius: Dp = 11.dp,
  evolutionTier: LlmEvolutionTier = LlmEvolutionTier.BASE_LLM,
) {
  val rimColor =
    when (evolutionTier) {
      LlmEvolutionTier.BASE_LLM -> Color(0xFF22D3EE)
      LlmEvolutionTier.FULL_FLEDGED_LLM -> Color(0xFFA855F7)
      LlmEvolutionTier.MULTIMODAL_AI -> Color(0xFF38BDF8)
      LlmEvolutionTier.AGI_CORE -> Color(0xFF2DD4BF)
    }
  val label =
    when (evolutionTier) {
      LlmEvolutionTier.BASE_LLM -> "LLM"
      LlmEvolutionTier.FULL_FLEDGED_LLM -> "LLM+"
      LlmEvolutionTier.MULTIMODAL_AI -> "MM"
      LlmEvolutionTier.AGI_CORE -> "AGI"
    }

  Surface(
    shape = RoundedCornerShape(cornerRadius),
    color = Color(0xFF0A141D),
    border = BorderStroke(1.dp, rimColor.copy(alpha = 0.65f)),
    shadowElevation = 2.dp,
    modifier = modifier.size(iconSize),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize().background(
          Brush.linearGradient(listOf(Color(0xFF0F202E), Color(0xFF091219)))
        ),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        color = rimColor,
        fontSize = (iconSize.value * 0.27f).sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.6.sp,
      )
    }
  }
}

@Composable
fun LocalLlmScreen(
  turboBoost: Boolean,
  activeUpgrades: Set<PhoneUpgradeId> = emptySet(),
  phoneContextSummary: String = "",
  notesText: String = "",
  changeLogSummary: String = "",
  onExecuteAgiTools: (String) -> List<String> = { emptyList() },
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("local_llm_prefs", Context.MODE_PRIVATE) }
  val engine = remember { LocalLlmEngine() }
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  val evolutionTier =
    when {
      PhoneUpgradeId.LLM_AGI_CORE in activeUpgrades -> LlmEvolutionTier.AGI_CORE
      PhoneUpgradeId.LLM_MULTIMODAL in activeUpgrades -> LlmEvolutionTier.MULTIMODAL_AI
      PhoneUpgradeId.LLM_FULL_FLEDGE in activeUpgrades -> LlmEvolutionTier.FULL_FLEDGED_LLM
      else -> LlmEvolutionTier.BASE_LLM
    }

  val unlockedModels = remember(evolutionTier) { engine.availableModels(evolutionTier) }

  var selectedModel by remember(evolutionTier) {
    val saved = prefs.getString("model", LocalLlmModel.LLAMA_EDGE.name)
    val found = unlockedModels.find { it.name == saved }
    mutableStateOf(
      found
        ?: when (evolutionTier) {
          LlmEvolutionTier.AGI_CORE -> LocalLlmModel.INFINITY_AGI
          LlmEvolutionTier.MULTIMODAL_AI -> LocalLlmModel.OMNI_VISION
          LlmEvolutionTier.FULL_FLEDGED_LLM -> LocalLlmModel.TITAN_14B
          LlmEvolutionTier.BASE_LLM -> LocalLlmModel.LLAMA_EDGE
        }
    )
  }
  var selectedPreset by remember {
    val saved = prefs.getString("preset", LlmSystemPreset.ASSISTANT.name)
    mutableStateOf(LlmSystemPreset.entries.find { it.name == saved } ?: LlmSystemPreset.ASSISTANT)
  }
  var selectedModality by remember { mutableStateOf(LlmInputModality.TEXT) }
  var attachmentLabel by remember { mutableStateOf<String?>(null) }
  var visionResult by remember { mutableStateOf<VisionAnalysisResult?>(null) }
  var documentResult by remember { mutableStateOf<DocumentAnalysisResult?>(null) }
  var audioResult by remember { mutableStateOf<AudioAnalysisResult?>(null) }
  var speechStatus by remember { mutableStateOf<String?>(null) }
  var speakingTurnId by remember { mutableStateOf<String?>(null) }

  var showSystemPromptEditor by remember { mutableStateOf(false) }
  var customSystemPrompt by remember { mutableStateOf(prefs.getString("sys_prompt", "") ?: "") }
  var temperature by remember { mutableFloatStateOf(prefs.getFloat("temp", 0.6f)) }
  var messages by remember { mutableStateOf(loadLlmMessages(prefs)) }
  var draft by remember { mutableStateOf("") }
  var isGenerating by remember { mutableStateOf(false) }

  // On-device Android Text-To-Speech for Multimodal/AGI spoken output (zero permissions)
  var isTtsReady by remember { mutableStateOf(false) }
  val ttsEngine = remember(context) {
    var instance: TextToSpeech? = null
    runCatching {
      instance = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
          instance?.language = Locale.US
          isTtsReady = true
        }
      }
    }
    instance
  }
  DisposableEffect(ttsEngine) {
    onDispose {
      runCatching {
        ttsEngine?.stop()
        ttsEngine?.shutdown()
      }
    }
  }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        val parsed = LocalLlmMediaAnalyzer.analyzeImageUri(context, uri)
        if (parsed != null) {
          visionResult = parsed
          documentResult = null
          audioResult = null
          selectedModality = LlmInputModality.VISION
          attachmentLabel = parsed.displayName
          speechStatus = null
        } else {
          speechStatus = "Could not decode image pixels"
        }
      }
    }

  val documentPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        val parsed = LocalLlmMediaAnalyzer.analyzeDocumentUri(context, uri)
        if (parsed != null) {
          documentResult = parsed
          visionResult = null
          audioResult = null
          selectedModality = LlmInputModality.DOCUMENT
          attachmentLabel = parsed.displayName
          speechStatus = null
        } else {
          speechStatus = "Could not read selected document as UTF-8 text"
        }
      }
    }

  val audioPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        val parsed = LocalLlmMediaAnalyzer.analyzeAudioUri(context, uri)
        if (parsed != null) {
          audioResult = parsed
          visionResult = null
          documentResult = null
          selectedModality = LlmInputModality.AUDIO
          attachmentLabel = parsed.displayName
          speechStatus = null
        } else {
          speechStatus = "Could not inspect audio stream metadata"
        }
      }
    }

  val speechRecognizerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()
        if (!spoken.isNullOrBlank()) {
          draft = if (draft.isBlank()) spoken else "$draft $spoken"
          selectedModality = LlmInputModality.AUDIO
          speechStatus = "Voice transcribed to prompt"
        }
      }
    }

  fun clearAttachments() {
    visionResult = null
    documentResult = null
    audioResult = null
    attachmentLabel = null
    selectedModality = LlmInputModality.TEXT
    speechStatus = null
  }

  fun saveMessages(next: List<LocalLlmChatTurn>) {
    messages = next
    val arr = JSONArray()
    next.takeLast(60).forEach { m ->
      arr.put(
        JSONObject().apply {
          put("id", m.id)
          put("user", m.isUser)
          put("text", m.text)
          put("model", m.modelLabel)
        }
      )
    }
    prefs.edit().putString("history", arr.toString()).apply()
  }

  LaunchedEffect(messages.size, isGenerating) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  fun submit(textOverride: String? = null) {
    val clean = (textOverride ?: draft).trim()
    if (clean.isEmpty() || isGenerating) return
    if (textOverride == null) draft = ""
    speechStatus = null

    val modalTag =
      if (evolutionTier.ordinal >= LlmEvolutionTier.MULTIMODAL_AI.ordinal &&
        (selectedModality != LlmInputModality.TEXT || visionResult != null || documentResult != null || audioResult != null)
      ) {
        "[${selectedModality.label}${attachmentLabel?.let { ": $it" } ?: ""}] "
      } else {
        ""
      }

    // Collect full multi-turn user history (32K context window) BEFORE appending the current turn
    val priorTopics = messages.filter { it.isUser }.map { it.text }
    val userTurn =
      LocalLlmChatTurn(
        id = "u_${UUID.randomUUID()}",
        isUser = true,
        text = "$modalTag$clean",
        modelLabel = "You",
      )
    val baseList = messages + userTurn
    saveMessages(baseList)
    isGenerating = true

    val modelAtRun = selectedModel
    val presetAtRun = selectedPreset
    val tempAtRun = temperature
    val executedTools =
      if (evolutionTier == LlmEvolutionTier.AGI_CORE) onExecuteAgiTools(clean) else emptyList()

    val fullReply =
      engine.generateReply(
        prompt = clean,
        model = modelAtRun,
        preset = presetAtRun,
        temperature = tempAtRun,
        historyCount = baseList.size,
        tier = evolutionTier,
        modality = selectedModality,
        visionResult = visionResult,
        documentResult = documentResult,
        audioResult = audioResult,
        attachmentLabel = attachmentLabel,
        customSystemPrompt = customSystemPrompt,
        priorUserTopics = priorTopics,
        phoneContextSummary = phoneContextSummary,
        executedPhoneTools = executedTools,
      )
    val aiId = "a_${UUID.randomUUID()}"

    scope.launch {
      engine.streamTokens(fullReply, modelAtRun, turboBoost).collect { partial ->
        val updated =
          baseList +
            LocalLlmChatTurn(
              id = aiId,
              isUser = false,
              text = partial,
              modelLabel = "${modelAtRun.title} · ${if (turboBoost) modelAtRun.baseTokPerSec * 2 else modelAtRun.baseTokPerSec} tok/s",
            )
        messages = updated
      }
      saveMessages(messages)
      isGenerating = false
    }
  }

  val accent = Color(selectedModel.accentHex)

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF06090F))
        .padding(top = 30.dp, bottom = 52.dp)
        .imePadding()
        .testTag("local_llm_screen"),
  ) {
    // Header
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(Color(0xFF0B111C))
          .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f),
      ) {
        LocalLlmAppIcon(iconSize = 34.dp, cornerRadius = 9.dp, evolutionTier = evolutionTier)
        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "Local LLM",
              color = Color.White,
              fontSize = 16.sp,
              fontWeight = FontWeight.SemiBold,
            )
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = accent.copy(alpha = 0.18f),
              border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
              modifier = Modifier.testTag("local_llm_tier_badge"),
            ) {
              Text(
                text = evolutionTier.badgeLabel,
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
          Text(
            text =
              if (evolutionTier.ordinal >= LlmEvolutionTier.FULL_FLEDGED_LLM.ordinal) {
                "${selectedModel.title} · ${(messages.sumOf { it.text.length } / 4).coerceAtLeast(12)}/32K ctx"
              } else {
                "${selectedModel.title} · ${selectedModel.paramsLabel}"
              },
            color = Color(0xFF9CA3AF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        if (evolutionTier.ordinal >= LlmEvolutionTier.FULL_FLEDGED_LLM.ordinal) {
          IconButton(
            onClick = { showSystemPromptEditor = !showSystemPromptEditor },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("local_llm_sysprompt_toggle"),
          ) {
            Icon(
              imageVector = Icons.Filled.Settings,
              contentDescription = "System prompt settings",
              tint = if (showSystemPromptEditor) accent else Color(0xFF9CA3AF),
            )
          }
        }
        IconButton(
          onClick = {
            saveMessages(emptyList())
            ttsEngine?.stop()
            speakingTurnId = null
          },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("local_llm_clear"),
        ) {
          Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = "Clear LLM session",
            tint = Color(0xFF9CA3AF),
          )
        }
      }
    }

    // Optional Full-Fledged LLM+ System Prompt Editor (Enforced on every reply!)
    if (showSystemPromptEditor && evolutionTier.ordinal >= LlmEvolutionTier.FULL_FLEDGED_LLM.ordinal) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF0E1422))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = "System Prompt Directive (Enforced · try \"JSON\", \"one sentence\", \"bullet list\")",
          color = accent,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
          value = customSystemPrompt,
          onValueChange = {
            customSystemPrompt = it
            prefs.edit().putString("sys_prompt", it).apply()
          },
          singleLine = true,
          placeholder = { Text("e.g. Reply in JSON or 1 sentence…", color = Color(0xFF6B7280), fontSize = 12.sp) },
          shape = RoundedCornerShape(12.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = Color(0xFF131926),
              unfocusedContainerColor = Color(0xFF131926),
              focusedBorderColor = accent,
              unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
            ),
          modifier = Modifier.fillMaxWidth().testTag("local_llm_sysprompt_input"),
        )
      }
    }

    // Model + Preset + Multimodal controls
    Column(
      modifier = Modifier.fillMaxWidth().background(Color(0xFF090D16)).padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        unlockedModels.forEach { m ->
          val active = m == selectedModel
          val chipColor = Color(m.accentHex)
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (active) chipColor.copy(alpha = 0.20f) else Color(0xFF121826),
            border = BorderStroke(1.dp, if (active) chipColor else Color.White.copy(alpha = 0.09f)),
            modifier =
              Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                selectedModel = m
                prefs.edit().putString("model", m.name).apply()
              },
          ) {
            Text(
              text = m.title,
              color = if (active) chipColor else Color(0xFF9CA3AF),
              fontSize = 11.5.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }
        }
      }

      if (evolutionTier.ordinal >= LlmEvolutionTier.MULTIMODAL_AI.ordinal) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("local_llm_modality_row"),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          LlmInputModality.entries.forEach { mod ->
            val active = selectedModality == mod
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (active) accent.copy(alpha = 0.22f) else Color(0xFF121826),
              border = BorderStroke(1.dp, if (active) accent else Color.White.copy(alpha = 0.10f)),
              modifier =
                Modifier.clip(RoundedCornerShape(10.dp))
                  .clickable {
                    selectedModality = mod
                    if (mod == LlmInputModality.TEXT) {
                      clearAttachments()
                    }
                  }
                  .testTag("modality_${mod.name}"),
            ) {
              Text(
                text = mod.label,
                color = if (active) accent else Color(0xFF9CA3AF),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
              )
            }
          }

          ModalToolChip(
            label = "+ Photo",
            color = Color(0xFF38BDF8),
            tag = "local_llm_pick_photo",
          ) {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          }

          ModalToolChip(
            label = "+ File",
            color = Color(0xFFA855F7),
            tag = "local_llm_pick_doc",
          ) {
            documentPickerLauncher.launch(arrayOf("text/*", "application/json", "*/*"))
          }

          ModalToolChip(
            label = "Notes",
            color = Color(0xFFFBBF24),
            tag = "local_llm_attach_notes",
          ) {
            val content = notesText.ifBlank { "Notes app is currently empty." }
            documentResult = LocalLlmMediaAnalyzer.analyzeRawDocumentText("Phone Notes", content)
            visionResult = null
            audioResult = null
            selectedModality = LlmInputModality.DOCUMENT
            attachmentLabel = "Phone Notes (${documentResult?.wordCount ?: 0}w)"
            speechStatus = null
          }

          if (changeLogSummary.isNotBlank()) {
            ModalToolChip(
              label = "AI Log",
              color = Color(0xFF2DD4BF),
              tag = "local_llm_attach_changes",
            ) {
              documentResult = LocalLlmMediaAnalyzer.analyzeRawDocumentText("AI Changes Log", changeLogSummary)
              visionResult = null
              audioResult = null
              selectedModality = LlmInputModality.DOCUMENT
              attachmentLabel = "AI Changes Log (${documentResult?.lineCount ?: 0} lines)"
              speechStatus = null
            }
          }

          ModalToolChip(
            label = "Voice Input",
            color = Color(0xFFF472B6),
            tag = "local_llm_voice_input",
          ) {
            val intent =
              Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Local LLM")
              }
            runCatching { speechRecognizerLauncher.launch(intent) }.onFailure {
              speechStatus = "Speech service unavailable here — try + Audio File"
            }
          }

          ModalToolChip(
            label = "+ Audio",
            color = Color(0xFF34D399),
            tag = "local_llm_pick_audio",
          ) {
            audioPickerLauncher.launch(arrayOf("audio/*"))
          }
        }
      }

      // Active real multimodal attachment preview strip
      if (visionResult != null || documentResult != null || audioResult != null || speechStatus != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFF101827),
          border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth().testTag("local_llm_attachment_preview"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            val v = visionResult
            val d = documentResult
            val a = audioResult
            when {
              v != null -> {
                v.thumbnail?.let { bmp ->
                  Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Attached photo preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)),
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${v.displayName} · ${v.orientation}",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "${v.avgBrightnessPct}% bright · contrast ${v.contrastScore}",
                      color = Color(0xFF9CA3AF),
                      fontSize = 10.sp,
                    )
                    v.dominantColorsHex.forEach { hex ->
                      val swatch = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(accent)
                      Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(swatch))
                    }
                  }
                }
              }
              d != null -> {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Doc: ${d.displayName}",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "${d.wordCount} words · ${d.lineCount} lines" +
                      if (d.jsonKeys.isNotEmpty()) " · keys: ${d.jsonKeys.take(3).joinToString()}" else "",
                    color = Color(0xFF9CA3AF),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              a != null -> {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Audio: ${a.displayName}",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "Duration ${String.format(Locale.US, "%.1fs", a.durationMs / 1000.0)} · ${a.mimeType}",
                    color = Color(0xFF9CA3AF),
                    fontSize = 10.sp,
                  )
                }
              }
              speechStatus != null -> {
                Text(
                  text = speechStatus ?: "",
                  color = Color(0xFFF472B6),
                  fontSize = 11.sp,
                  modifier = Modifier.weight(1f),
                )
              }
            }
            IconButton(
              onClick = { clearAttachments() },
              modifier = Modifier.minimumInteractiveComponentSize().size(32.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Clear attachment",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          LlmSystemPreset.entries.forEach { p ->
            val active = p == selectedPreset
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (active) Color(0xFF1F2937) else Color.Transparent,
              border = BorderStroke(1.dp, if (active) accent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f)),
              modifier =
                Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                  selectedPreset = p
                  prefs.edit().putString("preset", p.name).apply()
                },
            ) {
              Text(
                text = p.label,
                color = if (active) Color.White else Color(0xFF9CA3AF),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Temp ${String.format(Locale.US, "%.1f", temperature)}",
          color = accent,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
        )
      }

      Slider(
        value = temperature,
        onValueChange = {
          temperature = it
          prefs.edit().putFloat("temp", it).apply()
        },
        valueRange = 0.1f..1.0f,
        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
        modifier = Modifier.height(22.dp),
      )
    }

    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

    // Chat stream
    if (messages.isEmpty()) {
      Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        LocalLlmAppIcon(iconSize = 56.dp, cornerRadius = 14.dp, evolutionTier = evolutionTier)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "${evolutionTier.badgeLabel} Ready",
          color = Color.White,
          fontSize = 17.sp,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = evolutionTier.subtitle,
          color = accent,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(14.dp))
        val starters =
          when (evolutionTier) {
            LlmEvolutionTier.AGI_CORE ->
              listOf(
                "Run autonomous phone audit",
                "Save note: Ship Offline AGI Core milestone",
                "Make an app called FocusLog for workout reps",
              )
            LlmEvolutionTier.MULTIMODAL_AI ->
              listOf(
                "15% of 240",
                "Write a JSON schema with id, name, status",
                "Remember my project codename is Atlas",
              )
            LlmEvolutionTier.FULL_FLEDGED_LLM ->
              listOf(
                "(148 * 24) / 6",
                "12 km to miles",
                "Remember my favorite color is cyan",
              )
            LlmEvolutionTier.BASE_LLM ->
              listOf(
                "(25 + 15) * 3",
                "Write a Kotlin fibonacci function",
                "Summarize: Focus blocks reduce context switching and improve deep output.",
              )
          }
        starters.forEach { sample ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF101725),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
            modifier =
              Modifier.fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { submit(sample) },
          ) {
            Text(
              text = sample,
              color = Color(0xFFE5E7EB),
              fontSize = 12.5.sp,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
          }
        }
      }
    } else {
      LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(messages, key = { it.id }) { turn ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (turn.isUser) Arrangement.End else Arrangement.Start,
          ) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = if (turn.isUser) Color(0xFF0E7490) else Color(0xFF121826),
              border =
                if (turn.isUser) null
                else BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
              modifier = Modifier.widthIn(max = 318.dp),
            ) {
              Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = turn.modelLabel,
                    color = if (turn.isUser) Color(0xFFCFFAFE) else accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                  )
                  if (!turn.isUser &&
                    evolutionTier.ordinal >= LlmEvolutionTier.MULTIMODAL_AI.ordinal &&
                    isTtsReady &&
                    ttsEngine != null
                  ) {
                    val isSpeakingThis = speakingTurnId == turn.id
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = accent.copy(alpha = if (isSpeakingThis) 0.28f else 0.12f),
                      modifier =
                        Modifier.clip(RoundedCornerShape(8.dp))
                          .clickable {
                            if (isSpeakingThis) {
                              ttsEngine.stop()
                              speakingTurnId = null
                            } else {
                              speakingTurnId = turn.id
                              ttsEngine.speak(turn.text, TextToSpeech.QUEUE_FLUSH, null, turn.id)
                            }
                          }
                          .testTag("local_llm_speak_${turn.id}"),
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                      ) {
                        Icon(
                          imageVector = if (isSpeakingThis) Icons.Filled.Close else Icons.Filled.PlayArrow,
                          contentDescription = if (isSpeakingThis) "Stop speaking" else "Speak response",
                          tint = accent,
                          modifier = Modifier.size(12.dp),
                        )
                        Text(
                          text = if (isSpeakingThis) "Stop" else "Speak",
                          color = accent,
                          fontSize = 9.5.sp,
                          fontWeight = FontWeight.SemiBold,
                        )
                      }
                    }
                  }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = turn.text,
                  color = Color.White,
                  fontSize = 13.5.sp,
                  lineHeight = 19.sp,
                )
              }
            }
          }
        }
      }
    }

    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

    // Input row
    Row(
      modifier = Modifier.fillMaxWidth().background(Color(0xFF0B101A)).padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { submit() }),
        placeholder = {
          val ph =
            when (evolutionTier) {
              LlmEvolutionTier.AGI_CORE -> "Command AGI Core (notes, dim, apps, math)…"
              LlmEvolutionTier.MULTIMODAL_AI -> "Ask about photo, doc, audio or math…"
              LlmEvolutionTier.FULL_FLEDGED_LLM -> "Prompt 32K Full-Fledged LLM…"
              LlmEvolutionTier.BASE_LLM -> "Prompt Local LLM offline…"
            }
          Text(ph, color = Color(0xFF6B7280), fontSize = 13.5.sp)
        },
        shape = RoundedCornerShape(22.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF131926),
            unfocusedContainerColor = Color(0xFF131926),
            focusedBorderColor = accent,
            unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
          ),
        modifier = Modifier.weight(1f).testTag("local_llm_input"),
      )

      IconButton(
        onClick = { submit() },
        enabled = draft.isNotBlank() && !isGenerating,
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .size(48.dp)
            .background(if (draft.isNotBlank() && !isGenerating) accent else Color(0xFF1F2937), CircleShape)
            .testTag("local_llm_send"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = "Send prompt to Local LLM",
          tint = if (draft.isNotBlank() && !isGenerating) Color.Black else Color(0xFF6B7280),
        )
      }
    }
  }
}

@Composable
private fun ModalToolChip(
  label: String,
  color: Color,
  tag: String,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = color.copy(alpha = 0.14f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
    modifier =
      Modifier.clip(RoundedCornerShape(10.dp))
        .clickable(onClick = onClick)
        .testTag(tag),
  ) {
    Text(
      text = label,
      color = color,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
    )
  }
}

private fun loadLlmMessages(prefs: android.content.SharedPreferences): List<LocalLlmChatTurn> {
  val raw = prefs.getString("history", null) ?: return emptyList()
  return runCatching {
    val arr = JSONArray(raw)
    buildList {
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
          LocalLlmChatTurn(
            id = o.getString("id"),
            isUser = o.getBoolean("user"),
            text = o.getString("text"),
            modelLabel = o.optString("model", "Local LLM"),
          )
        )
      }
    }
  }.getOrDefault(emptyList())
}
