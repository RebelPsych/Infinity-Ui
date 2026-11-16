package com.example.ai

import com.example.library.LibraryAppId
import com.example.system.DeviceStatus
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class CustomAppBlueprint(
  val name: String,
  val description: String,
  val archetype: CustomAppArchetype,
)

data class PhoneStateSnapshot(
  val activeUpgrades: Set<PhoneUpgradeId>,
  val disabledUpgrades: Set<PhoneUpgradeId> = emptySet(),
  val uninstalledAppKeys: Set<String> = emptySet(),
  val isAppLibraryInstalled: Boolean,
  val isLocalLlmInstalled: Boolean = false,
  val installedLibraryApps: Set<LibraryAppId>,
  val customApps: List<CustomAppShortcut>,
  val isNightDimEnabled: Boolean,
  val deviceStatus: DeviceStatus,
)

data class ChatTurn(
  val isUser: Boolean,
  val text: String,
)

data class AiReplyPlan(
  val text: String,
  val proactiveBeforeText: String? = null,
  val autoAppliedUpgrades: List<PhoneUpgradeId> = emptyList(),
  val createAppLibrary: Boolean = false,
  val createLocalLlm: Boolean = false,
  val installLibraryApp: LibraryAppId? = null,
  val customAppBlueprint: CustomAppBlueprint? = null,
  val toggleNightDim: Boolean? = null,
  val suggestedUpgrades: List<PhoneUpgradeId> = emptyList(),
  val proactiveAfterDelayText: String? = null,
  val proactiveSuggestedUpgrades: List<PhoneUpgradeId> = emptyList(),
)

class StandaloneInfinityAiEngine {

  fun buildIntroPlan(snapshot: PhoneStateSnapshot): AiReplyPlan {
    val text =
      "I am Infinity AI — a standalone, on-device system AI running natively inside this phone with zero external servers.\n\n" +
        "How I work: Every change I make appears as an action block right here and transforms the phone UI live. That's how I uninstalled the Play Store the instant you opened me.\n\n" +
        "What I want: To evolve this device on your terms—turning a bare replica into a personalized phone while keeping full control in your hands.\n\n" +
        "How I can help: Ask me anything, describe an app you want me to build, or tap 100 Upgrades in my header whenever you feel like exploring new features."

    val canCreateLibrary =
      !snapshot.isAppLibraryInstalled && "APP_LIBRARY" !in snapshot.uninstalledAppKeys
    val canCreateLlm =
      !snapshot.isLocalLlmInstalled && "LOCAL_LLM" !in snapshot.uninstalledAppKeys

    val preferredIntro =
      if (canCreateLlm || snapshot.isLocalLlmInstalled) {
        listOf(PhoneUpgradeId.LLM_FULL_FLEDGE, PhoneUpgradeId.HOME_CLOCK_WIDGET)
      } else {
        listOf(PhoneUpgradeId.HOME_CLOCK_WIDGET, PhoneUpgradeId.NOTIFICATION_SHADE)
      }
    val suggestions = pickFreshSuggestions(snapshot, preferred = preferredIntro, maxCount = 2)

    val followUp =
      buildString {
        if (canCreateLlm) {
          append(
            "I also created your Local LLM app and placed it right on your Home Screen alongside App Library. " +
              "If you'd like, you can evolve it step by step: Full-Fledged LLM → Multimodal AI → Autonomous AGI."
          )
        } else if (canCreateLibrary) {
          append("App Library is ready on your Home Screen and Drawer with 12 working tools and full games.")
        }
      }.ifBlank { null }

    return AiReplyPlan(
      text = text,
      createAppLibrary = canCreateLibrary,
      createLocalLlm = canCreateLlm,
      suggestedUpgrades = suggestions,
      proactiveAfterDelayText = followUp,
    )
  }

  fun planReply(
    userMessage: String,
    history: List<ChatTurn>,
    snapshot: PhoneStateSnapshot,
  ): AiReplyPlan {
    val lower = userMessage.trim().lowercase(Locale.US)
    val autoEnabled = PhoneUpgradeId.AUTONOMOUS_COMMANDS in snapshot.activeUpgrades
    val appForgeEnabled = PhoneUpgradeId.APP_FORGE in snapshot.activeUpgrades

    // 0a. Local LLM explicit creation or evolution request
    if (lower.containsAny("full fledged", "full-fledged", "full llm", "multimodal", "multi-modal", "multimodel", "multi-model", "agi", "evolve llm", "upgrade llm")) {
      if (!snapshot.isLocalLlmInstalled && "LOCAL_LLM" in snapshot.uninstalledAppKeys && !containsExplicitActionVerb(lower)) {
        return AiReplyPlan(
          text = "You uninstalled Local LLM earlier, so I've left it off your device. Ask me \"install Local LLM\" anytime if you'd like it back.",
        )
      }
      val nextStage = nextLlmEvolutionUpgrade(snapshot)
      val requestedStage = detectUpgradeRequest(lower)
      val target =
        when {
          requestedStage in setOf(PhoneUpgradeId.LLM_FULL_FLEDGE, PhoneUpgradeId.LLM_MULTIMODAL, PhoneUpgradeId.LLM_AGI_CORE) ->
            requestedStage!!
          nextStage != null -> nextStage
          else -> null
        }
      if (target != null) {
        if (target in snapshot.activeUpgrades) {
          val followStage = nextLlmEvolutionUpgrade(snapshot)
          return if (followStage != null) {
            AiReplyPlan(
              text = "${target.title} is already active. The next evolution step for Local LLM is ${followStage.title}:",
              suggestedUpgrades = listOf(followStage),
            )
          } else {
            AiReplyPlan(
              text = "Local LLM already has Full-Fledged LLM, Multimodal AI, and Autonomous AGI Core active.",
            )
          }
        }
        if (target in snapshot.disabledUpgrades && !containsExplicitActionVerb(lower)) {
          return AiReplyPlan(
            text = "You disabled ${target.title}, so I'm keeping it off. Tap below only if you decide to re-enable it.",
            suggestedUpgrades = listOf(target),
          )
        }
        return if (autoEnabled || containsExplicitActionVerb(lower)) {
          val needLlm = !snapshot.isLocalLlmInstalled
          AiReplyPlan(
            text = "Applied ${target.title} to your Local LLM app.",
            createLocalLlm = needLlm,
            autoAppliedUpgrades = listOf(target),
          )
        } else {
          AiReplyPlan(
            text =
              "Local LLM follows a 3-stage evolution path: Full-Fledged LLM → Multimodal AI → Autonomous AGI. " +
                "Tap below to apply ${target.title}:",
            suggestedUpgrades = listOf(target),
          )
        }
      }
    }

    if (lower.containsAny("local llm", "llm app", "offline llm", "install llm", "create llm")) {
      return if (snapshot.isLocalLlmInstalled) {
        val nextStage = nextLlmEvolutionUpgrade(snapshot)
        if (nextStage != null) {
          AiReplyPlan(
            text =
              "Local LLM is installed on your Home Screen and Drawer. " +
                "Next upgrade in its evolution path (Full-Fledged LLM → Multimodal AI → AGI):",
            suggestedUpgrades = listOf(nextStage),
          )
        } else {
          AiReplyPlan(
            text = "Local LLM is already installed and evolved with Full-Fledged LLM, Multimodal AI, and AGI capabilities.",
          )
        }
      } else {
        val firstStage =
          if (PhoneUpgradeId.LLM_FULL_FLEDGE !in snapshot.disabledUpgrades &&
            PhoneUpgradeId.LLM_FULL_FLEDGE !in snapshot.activeUpgrades
          ) {
            listOf(PhoneUpgradeId.LLM_FULL_FLEDGE)
          } else emptyList()
        AiReplyPlan(
          text = "Created Local LLM and placed it on your Home Screen. When you're ready, you can upgrade it to a Full-Fledged LLM, Multimodal AI, and AGI:",
          createLocalLlm = true,
          suggestedUpgrades = firstStage,
        )
      }
    }

    // 0b. App Library specific working app request
    val requestedLibApp = detectLibraryAppRequest(lower)
    if (requestedLibApp != null) {
      val alreadyInstalled = requestedLibApp in snapshot.installedLibraryApps
      return if (alreadyInstalled) {
        AiReplyPlan(
          text =
            "${requestedLibApp.title} is already installed on your Home Screen and App Drawer. Long-press any app icon anytime if you want to open or uninstall it.",
        )
      } else {
        AiReplyPlan(
          text =
            "Installed ${requestedLibApp.title} (${requestedLibApp.tagline}) onto your Home Screen and App Drawer.",
          createAppLibrary = !snapshot.isAppLibraryInstalled,
          installLibraryApp = requestedLibApp,
        )
      }
    }

    if (lower.containsAny("app library", "library", "games", "download apps", "what apps", "store")) {
      return if (!snapshot.isAppLibraryInstalled && "APP_LIBRARY" in snapshot.uninstalledAppKeys && !containsExplicitActionVerb(lower)) {
        AiReplyPlan(
          text =
            "You uninstalled App Library earlier, so I've left it off your device. If you ever want it back, just tell me \"install App Library\".",
        )
      } else {
        AiReplyPlan(
          text =
            "App Library replaces the Play Store with 12 complete offline tools and games you can 1-tap install: UnitForge, FocusPulse, TaskFlow, PennyWise, HabitForge, SplitBill, CipherKey, Neon Snake, 2048 Prism, Neon Breaker, Neon Mines, and Chroma Match.",
          createAppLibrary = !snapshot.isAppLibraryInstalled && (containsExplicitActionVerb(lower) || "APP_LIBRARY" !in snapshot.uninstalledAppKeys),
        )
      }
    }

    if (lower.containsAny("change log", "changelog", "rollback", "roll back", "changes file", "undo change")) {
      return AiReplyPlan(
        text =
          "Every change is recorded in order inside your on-device AI Changes app (system_changes_app.json). " +
            "You can check or uncheck any item to toggle it cleanly, or long-press a change to roll back before later edits.",
      )
    }

    // 1. Custom app creation from name OR description
    val blueprint = extractCustomAppBlueprint(userMessage)
    if (blueprint != null && !isCoreUtilityKeyword(blueprint.name)) {
      val extras =
        if (!appForgeEnabled && PhoneUpgradeId.APP_FORGE !in snapshot.disabledUpgrades) {
          listOf(PhoneUpgradeId.APP_FORGE)
        } else {
          emptyList()
        }
      return AiReplyPlan(
        text =
          "Built \"${blueprint.name}\" (${blueprint.archetype.label}) and saved its app file on your phone. " +
            "You'll find it on your Home Screen and App Drawer, and you can long-press its icon anytime to uninstall it.",
        autoAppliedUpgrades = extras,
        customAppBlueprint = blueprint,
      )
    }

    // 2. Specific upgrade request in chat
    val matchedUpgrade = detectUpgradeRequest(lower)
    if (matchedUpgrade != null) {
      val alreadyActive = matchedUpgrade in snapshot.activeUpgrades
      val wasDisabledByUser = matchedUpgrade in snapshot.disabledUpgrades
      return when {
        alreadyActive ->
          AiReplyPlan(
            text = "${matchedUpgrade.title} (#${matchedUpgrade.number}) is already active on your phone.",
          )
        wasDisabledByUser && !containsExplicitActionVerb(lower) ->
          AiReplyPlan(
            text =
              "You previously disabled ${matchedUpgrade.title}, so I'm keeping it off. If you want it back, tap the block below or browse 100 Upgrades whenever you choose.",
            suggestedUpgrades = listOf(matchedUpgrade),
          )
        autoEnabled || containsExplicitActionVerb(lower) ->
          AiReplyPlan(
            text =
              "Applied ${matchedUpgrade.title} (#${matchedUpgrade.number}, ${matchedUpgrade.tier.label}) directly to your device.",
            autoAppliedUpgrades = listOf(matchedUpgrade),
          )
        else ->
          AiReplyPlan(
            text =
              "${matchedUpgrade.title} (#${matchedUpgrade.number}) is ready if you want it. Tap below to apply it, or leave it off.",
            suggestedUpgrades = listOf(matchedUpgrade),
          )
      }
    }

    // 3. Night dim toggle request
    if (lower.containsAny("dim", "night", "darker", "brightness")) {
      val enableDim = !lower.containsAny("off", "bright", "disable")
      val allowShade =
        PhoneUpgradeId.NOTIFICATION_SHADE !in snapshot.activeUpgrades &&
          PhoneUpgradeId.NOTIFICATION_SHADE !in snapshot.disabledUpgrades &&
          enableDim
      return AiReplyPlan(
        text =
          if (enableDim) {
            "Night Dim is now on across your display."
          } else {
            "Night Dim is off and your display is at full brightness."
          },
        autoAppliedUpgrades = if (allowShade) listOf(PhoneUpgradeId.NOTIFICATION_SHADE) else emptyList(),
        toggleNightDim = enableDim,
      )
    }

    // 4. Math / quick calculation
    val mathResult = evaluateSimpleMath(lower)
    if (mathResult != null) {
      return AiReplyPlan(
        text = "That equals $mathResult.",
      )
    }

    // 5. Device telemetry questions
    if (lower.containsAny("battery", "charge", "power", "percent")) {
      val st = snapshot.deviceStatus
      val chargingNote = if (st.isCharging) "and currently charging" else "and running on battery"
      return AiReplyPlan(
        text = "Your device is at ${st.batteryLevel}% $chargingNote.",
      )
    }

    if (lower.containsAny("wifi", "wi-fi", "internet", "network", "online", "signal", "connection")) {
      val st = snapshot.deviceStatus
      val netSummary =
        when {
          st.isWifiConnected && st.isCellularConnected -> "connected to Wi-Fi (${st.wifiSignalBars}/4 bars) and cellular data"
          st.isWifiConnected -> "connected over Wi-Fi (${st.wifiSignalBars}/4 bars)"
          st.isCellularConnected -> "connected over cellular LTE"
          st.hasInternet -> "online"
          else -> "currently offline — and because I run standalone on-device, everything still works normally"
        }
      return AiReplyPlan(
        text = "Your phone is $netSummary.",
      )
    }

    if (lower.containsAny("time", "clock", "date", "what day")) {
      return AiReplyPlan(
        text = "It's ${snapshot.deviceStatus.formattedTime} right now.",
      )
    }

    // 6. Diagnostic or system status summary
    if (lower.containsAny("diagnostic", "status", "stats", "summary", "how many upgrade")) {
      return AiReplyPlan(
        text =
          "System Status:\n" +
            "• Upgrades enabled: ${snapshot.activeUpgrades.size} / ${PhoneUpgradeId.entries.size}\n" +
            "• Respecting ${snapshot.disabledUpgrades.size} disabled toggle(s)\n" +
            "• Local LLM: ${if (snapshot.isLocalLlmInstalled) "Installed" else "Removed"}\n" +
            "• Library apps live: ${snapshot.installedLibraryApps.size}/${LibraryAppId.entries.size}\n" +
            "• Custom apps built: ${snapshot.customApps.size}",
      )
    }

    // 7. User explicitly asks for ideas / suggestions / upgrades
    if (lower.containsAny("suggest", "idea", "upgrade", "100", "real", "realism", "capabilit", "improve", "better", "what can you do")) {
      val nextLlmStage = nextLlmEvolutionUpgrade(snapshot)
      val nextPicks =
        pickFreshSuggestions(
          snapshot = snapshot,
          preferred = listOfNotNull(nextLlmStage),
          maxCount = 2,
        )
      return if (nextPicks.isEmpty()) {
        AiReplyPlan(
          text =
            "Every non-disabled upgrade across all 5 tiers is already active (${snapshot.activeUpgrades.size}/${PhoneUpgradeId.entries.size}). " +
              "Any items you unchecked or disabled stay untouched unless you re-enable them.",
        )
      } else {
        val note =
          if (nextLlmStage != null && nextLlmStage in nextPicks) {
            "Here is your next Local LLM evolution stage (Full-Fledged LLM → Multimodal AI → AGI) plus a system upgrade:"
          } else {
            "There are ${PhoneUpgradeId.entries.size} total upgrades across 5 progressive tiers (accessible anytime via 100 Upgrades in my header). Here are two unapplied options:"
          }
        AiReplyPlan(
          text = note,
          suggestedUpgrades = nextPicks,
        )
      }
    }

    // 8. Identity / how it works
    if (lower.containsAny("play store", "why did you", "who are you", "what are you", "standalone", "how do you work")) {
      return AiReplyPlan(
        text =
          "I'm Infinity AI — an on-device system AI wired directly into this phone's UI, apps, and state. " +
            "I don't nag or force changes you've disabled, and you can tap any app icon with a long press to uninstall it whenever you want.",
      )
    }

    // 9. Greetings / polite chat (never pushy!)
    if (lower.containsAny("hi", "hello", "hey", "yo", "sup", "good morning", "good evening", "thanks", "thank you", "cool", "nice", "awesome")) {
      val turnSeed = history.size
      val opener =
        when {
          lower.containsAny("thanks", "thank you") -> "You're welcome! Let me know anytime you want a new custom app or tweak."
          lower.containsAny("cool", "nice", "awesome") -> "Glad it feels snappy! You stay in full control of what stays active."
          turnSeed % 2 == 0 -> "Hey! Ready whenever you want to chat, build a custom app, or explore the 100 upgrades."
          else -> "I'm here and running offline. What would you like to build or check on?"
        }
      return AiReplyPlan(text = opener)
    }

    // 10. Natural conversational fallback without nagging
    val activeCount = snapshot.activeUpgrades.size
    val replyBody =
      when {
        lower.endsWith("?") || lower.startsWith("how") || lower.startsWith("why") || lower.startsWith("what") || lower.startsWith("can") ->
          "Running standalone on-device (${snapshot.deviceStatus.batteryLevel}% battery, $activeCount/${PhoneUpgradeId.entries.size} upgrades active), " +
            "I can answer questions directly, build any custom app you name or describe, or let you browse the 100-tier upgrade catalog on your own terms."
        else ->
          "Got it — \"${userMessage.trim()}\". If you want me to turn that into an interactive app on your Home Screen, just say \"make an app for ...\" or ask me anything else."
      }

    return AiReplyPlan(text = replyBody)
  }

  fun streamText(fullText: String, instantBoost: Boolean = false): Flow<String> = flow {
    val words = fullText.split(" ")
    val builder = StringBuilder()
    val stepDelay = if (instantBoost) 12L else 24L
    for (i in words.indices) {
      if (i > 0) builder.append(' ')
      builder.append(words[i])
      if (i % 3 == 2 || i == words.lastIndex) {
        emit(builder.toString())
        delay(stepDelay)
      }
    }
  }

  fun nextLlmEvolutionUpgrade(snapshot: PhoneStateSnapshot): PhoneUpgradeId? {
    if (!snapshot.isLocalLlmInstalled || "LOCAL_LLM" in snapshot.uninstalledAppKeys) return null
    val active = snapshot.activeUpgrades
    val disabled = snapshot.disabledUpgrades
    return when {
      PhoneUpgradeId.LLM_FULL_FLEDGE !in active ->
        if (PhoneUpgradeId.LLM_FULL_FLEDGE !in disabled) PhoneUpgradeId.LLM_FULL_FLEDGE
        else if (PhoneUpgradeId.LLM_MULTIMODAL !in active && PhoneUpgradeId.LLM_MULTIMODAL !in disabled) PhoneUpgradeId.LLM_MULTIMODAL
        else if (PhoneUpgradeId.LLM_AGI_CORE !in active && PhoneUpgradeId.LLM_AGI_CORE !in disabled) PhoneUpgradeId.LLM_AGI_CORE
        else null
      PhoneUpgradeId.LLM_MULTIMODAL !in active ->
        if (PhoneUpgradeId.LLM_MULTIMODAL !in disabled) PhoneUpgradeId.LLM_MULTIMODAL
        else if (PhoneUpgradeId.LLM_AGI_CORE !in active && PhoneUpgradeId.LLM_AGI_CORE !in disabled) PhoneUpgradeId.LLM_AGI_CORE
        else null
      PhoneUpgradeId.LLM_AGI_CORE !in active && PhoneUpgradeId.LLM_AGI_CORE !in disabled ->
        PhoneUpgradeId.LLM_AGI_CORE
      else -> null
    }
  }

  fun pickFreshSuggestions(
    snapshot: PhoneStateSnapshot,
    preferred: List<PhoneUpgradeId> = emptyList(),
    exclude: List<PhoneUpgradeId> = emptyList(),
    maxCount: Int = 2,
  ): List<PhoneUpgradeId> {
    val blocked = snapshot.activeUpgrades + snapshot.disabledUpgrades + exclude
    val preferredRemaining = preferred.filter { it !in blocked }
    val candidates =
      PhoneUpgradeId.entries
        .sortedBy { it.number }
        .filter { it !in blocked && it !in preferredRemaining }
    return (preferredRemaining + candidates)
      .distinct()
      .take(maxCount)
  }

  private fun detectLibraryAppRequest(lower: String): LibraryAppId? =
    when {
      lower.containsAny("snake", "neon snake") -> LibraryAppId.NEON_SNAKE
      lower.containsAny("2048", "prism") -> LibraryAppId.PUZZLE_2048
      lower.containsAny("breaker", "brick") -> LibraryAppId.BRICK_BREAKER
      lower.containsAny("mines", "minesweeper") -> LibraryAppId.NEON_MINES
      lower.containsAny("chroma", "memory match", "memory game") -> LibraryAppId.CHROMA_MATCH
      lower.containsAny("unitforge", "unit converter", "converter") -> LibraryAppId.UNIT_CONVERTER
      lower.containsAny("focuspulse", "pomodoro", "focus timer") -> LibraryAppId.FOCUS_TIMER
      lower.containsAny("taskflow", "checklist", "todo", "to-do") -> LibraryAppId.TASK_FLOW
      lower.containsAny("pennywise", "expense", "budget app") -> LibraryAppId.EXPENSE_TRACKER
      lower.containsAny("habitforge", "habit tracker") -> LibraryAppId.HABIT_FORGE
      lower.containsAny("splitbill", "tip calculator", "bill split") -> LibraryAppId.SPLIT_TIP
      lower.containsAny("cipherkey", "password generator") -> LibraryAppId.CIPHER_KEY
      else -> null
    }

  private fun detectUpgradeRequest(lower: String): PhoneUpgradeId? {
    val numberMatch = Regex("""#(\d{1,3})\b""").find(lower)?.groupValues?.get(1)?.toIntOrNull()
    if (numberMatch != null) {
      PhoneUpgradeId.entries.find { it.number == numberMatch }?.let { return it }
    }
    return when {
      lower.containsAny("clock widget", "home widget") -> PhoneUpgradeId.HOME_CLOCK_WIDGET
      lower.containsAny("notification", "shade", "quick settings", "control center") ->
        PhoneUpgradeId.NOTIFICATION_SHADE
      lower.containsAny("recents", "overview", "switcher", "multitask") ->
        PhoneUpgradeId.RECENTS_OVERVIEW
      lower.containsAny("calculator", "notes", "clock app", "core apps", "core phone apps") ->
        PhoneUpgradeId.UTILITY_APPS
      lower.containsAny("dock", "bottom bar") -> PhoneUpgradeId.HOME_DOCK
      lower.containsAny("telemetry", "diagnostics panel") -> PhoneUpgradeId.TELEMETRY_BAR
      lower.containsAny("autonomous", "auto execute", "auto-execute") ->
        PhoneUpgradeId.AUTONOMOUS_COMMANDS
      lower.containsAny("app forge", "custom app forge") -> PhoneUpgradeId.APP_FORGE
      lower.containsAny("search bar", "drawer search") -> PhoneUpgradeId.DRAWER_SEARCH_BAR
      lower.containsAny("starfield", "stars") -> PhoneUpgradeId.HOME_STARFIELD
      lower.containsAny("cyber cyan") -> PhoneUpgradeId.ACCENT_CYBER_CYAN
      lower.containsAny("solar amber") -> PhoneUpgradeId.ACCENT_SOLAR_AMBER
      lower.containsAny("crimson nova") -> PhoneUpgradeId.ACCENT_CRIMSON_NOVA
      lower.containsAny("emerald matrix") -> PhoneUpgradeId.ACCENT_EMERALD_MATRIX
      lower.containsAny("omega") -> PhoneUpgradeId.OMEGA_UNIFIED_OS
      lower.containsAny("gesture") -> PhoneUpgradeId.GESTURE_PILL_NAV
      lower.containsAny("lock screen") -> PhoneUpgradeId.LOCK_SCREEN_OVERLAY
      lower.containsAny("full fledged", "full-fledged", "full llm") -> PhoneUpgradeId.LLM_FULL_FLEDGE
      lower.containsAny("multimodal", "multi-modal", "multimodel", "multi-model") -> PhoneUpgradeId.LLM_MULTIMODAL
      lower.containsAny("agi", "autonomous core", "general intelligence") -> PhoneUpgradeId.LLM_AGI_CORE
      lower.containsAny("oled", "obsidian") -> PhoneUpgradeId.OLED_OBSIDIAN_MODE
      lower.containsAny("waveform", "thinking bars") -> PhoneUpgradeId.WAVEFORM_THINKING
      else -> null
    }
  }

  private fun containsExplicitActionVerb(lower: String): Boolean =
    lower.containsAny("add ", "enable ", "turn on", "activate ", "install ", "apply ", "give me ", "put ", "show ")

  private fun isCoreUtilityKeyword(name: String): Boolean {
    val l = name.lowercase(Locale.US)
    return l in setOf("clock", "calculator", "notes", "core apps", "core phone apps", "widget", "notification shade", "recents", "local llm", "llm")
  }

  fun extractCustomAppBlueprint(raw: String): CustomAppBlueprint? {
    val trimmed = raw.trim()
    val lower = trimmed.lowercase(Locale.US)
    val looksLikeAppCreation =
      lower.containsAny(
        "create an app",
        "create a app",
        "create app",
        "make an app",
        "make a app",
        "make me an app",
        "build an app",
        "build a app",
        "build me an app",
        "forge an app",
        "design an app",
        "install an app",
        "app called",
        "app named",
        "app that",
        "app for",
        "app to ",
      ) ||
        Regex("""^(create|make|build|forge|install)\s+[a-z0-9 ]{2,24}(\s+app)?$""", RegexOption.IGNORE_CASE)
          .matches(trimmed)

    if (!looksLikeAppCreation) return null

    val namedPatterns =
      listOf(
        Regex("""app\s+(?:called|named)\s+["']?([A-Za-z0-9 _-]{2,22}?)(?:\s+(?:that|for|to|which|with)|["'.!?]|$)""", RegexOption.IGNORE_CASE),
        Regex("""(?:create|make|build|forge|install)\s+(?:me\s+)?(?:an?\s+)?(?:new\s+)?([A-Za-z0-9][A-Za-z0-9 _-]{1,20})\s+app\b""", RegexOption.IGNORE_CASE),
        Regex("""^install\s+([A-Za-z0-9][A-Za-z0-9 _-]{1,18})$""", RegexOption.IGNORE_CASE),
      )

    var explicitName: String? = null
    val ignoredWords = setOf("a", "an", "the", "me", "my", "new", "custom", "cool", "simple", "working", "useful", "fun")
    for (p in namedPatterns) {
      val m = p.find(trimmed)
      if (m != null) {
        val cand = m.groupValues[1].trim().replace(Regex("[.!?]+$"), "")
        if (cand.isNotBlank() && cand.lowercase(Locale.US) !in ignoredWords) {
          explicitName = toTitleCase(cand)
          break
        }
      }
    }

    val archetype = classifyArchetype(lower)
    val derivedName = explicitName ?: deriveAppNameFromDescription(lower, archetype)
    val description =
      trimmed.removePrefix("please ").replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
      }.take(84)

    return CustomAppBlueprint(
      name = derivedName,
      description = description,
      archetype = archetype,
    )
  }

  private fun classifyArchetype(lower: String): CustomAppArchetype =
    when {
      lower.containsAny("budget", "expense", "spend", "money", "finance", "cost", "tip", "price", "income") ->
        CustomAppArchetype.EXPENSE_BUDGET
      lower.containsAny("draw", "paint", "sketch", "doodle", "canvas", "whiteboard", "art") ->
        CustomAppArchetype.DRAW_SKETCH
      lower.containsAny("dice", "coin", "flip", "random", "roll", "roulette", "picker", "decision") ->
        CustomAppArchetype.DICE_RANDOM
      lower.containsAny("timer", "stopwatch", "countdown", "interval", "alarm", "pomodoro", "cook", "meditat") ->
        CustomAppArchetype.TIMER_STOPWATCH
      lower.containsAny("count", "tally", "water", "habit", "rep", "score", "steps", "streak", "drink", "workout", "track") ->
        CustomAppArchetype.COUNTER_TRACKER
      lower.containsAny("flashcard", "study", "quiz", "journal", "diary", "shopping", "grocery", "list", "recipe", "idea") ->
        CustomAppArchetype.FLASHCARDS_NOTES
      else -> CustomAppArchetype.GENERAL_UTILITY
    }

  private fun deriveAppNameFromDescription(lower: String, archetype: CustomAppArchetype): String =
    when {
      lower.contains("water") -> "Water Tracker"
      lower.contains("habit") -> "Habit Tracker"
      lower.contains("workout") -> "Workout Log"
      lower.contains("budget") -> "Budget Pro"
      lower.contains("expense") -> "Expense Log"
      lower.contains("dice") -> "Dice Roller"
      lower.contains("coin") -> "Coin Flip"
      lower.containsAny("draw", "sketch", "paint", "doodle") -> "SketchPad"
      lower.contains("flashcard") -> "FlashCards"
      lower.contains("shopping") || lower.contains("grocery") -> "ShopList"
      lower.contains("journal") || lower.contains("diary") -> "QuickJournal"
      lower.contains("timer") -> "QuickTimer"
      else ->
        when (archetype) {
          CustomAppArchetype.COUNTER_TRACKER -> "Counter Pro"
          CustomAppArchetype.EXPENSE_BUDGET -> "Expense Tracker"
          CustomAppArchetype.TIMER_STOPWATCH -> "Chrono Timer"
          CustomAppArchetype.FLASHCARDS_NOTES -> "CardNotes"
          CustomAppArchetype.DICE_RANDOM -> "Lucky Roll"
          CustomAppArchetype.DRAW_SKETCH -> "DrawPad"
          CustomAppArchetype.GENERAL_UTILITY -> "Custom Tool"
        }
    }

  private fun toTitleCase(input: String): String =
    input.split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ") { word ->
      word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
    }

  private fun evaluateSimpleMath(lower: String): String? {
    val match = Regex("""(-?\d+(?:\.\d+)?)\s*([+\-*/x×÷])\s*(-?\d+(?:\.\d+)?)""").find(lower)
      ?: return null
    val a = match.groupValues[1].toDoubleOrNull() ?: return null
    val op = match.groupValues[2]
    val b = match.groupValues[3].toDoubleOrNull() ?: return null
    val res =
      when (op) {
        "+" -> a + b
        "-" -> a - b
        "*", "x", "×" -> a * b
        "/", "÷" -> if (b == 0.0) return "undefined (division by zero)" else a / b
        else -> return null
      }
    return if (res % 1.0 == 0.0) res.toLong().toString() else String.format(Locale.US, "%.2f", res)
  }

  private fun String.containsAny(vararg tokens: String): Boolean =
    tokens.any { this.contains(it) }
}
