package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.AiReplyPlan
import com.example.ai.ChangeLogStore
import com.example.ai.ChangePayload
import com.example.ai.ChangeRecord
import com.example.ai.ChatTurn
import com.example.ai.CustomAppBlueprint
import com.example.ai.CustomAppShortcut
import com.example.ai.PhoneStateSnapshot
import com.example.ai.PhoneUpgradeId
import com.example.ai.StandaloneInfinityAiEngine
import com.example.library.LibraryAppId
import com.example.system.DeviceStatus
import com.example.system.DeviceStatusMonitor
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PlayStoreTab {
  GAMES,
  APPS,
  SEARCH,
  BOOKS,
  YOU,
}

enum class ActiveAppScreen {
  NONE,
  PLAY_STORE,
  INFINITY_AI,
  APP_LIBRARY,
  LOCAL_LLM,
  LIBRARY_ITEM,
  CHANGE_LOG,
  CLOCK,
  CALCULATOR,
  NOTES,
  BROWSER,
  CUSTOM,
}

sealed interface AppContextTarget {
  val label: String
  val categoryLabel: String
  val canUninstall: Boolean

  data object InfinityAi : AppContextTarget {
    override val label = "Infinity AI"
    override val categoryLabel = "System Core AI"
    override val canUninstall = false
  }

  data object AppLibrary : AppContextTarget {
    override val label = "App Library"
    override val categoryLabel = "Offline Store"
    override val canUninstall = true
  }

  data object LocalLlm : AppContextTarget {
    override val label = "Local LLM"
    override val categoryLabel = "On-Device Model"
    override val canUninstall = true
  }

  data object AiChanges : AppContextTarget {
    override val label = "AI Changes"
    override val categoryLabel = "Rollback Log"
    override val canUninstall = false
  }

  data object PlayStore : AppContextTarget {
    override val label = "Play Store"
    override val categoryLabel = "Legacy Store"
    override val canUninstall = true
  }

  data class LibraryApp(val app: LibraryAppId) : AppContextTarget {
    override val label = app.title
    override val categoryLabel = app.tagline
    override val canUninstall = true
  }

  data class CustomApp(val app: CustomAppShortcut) : AppContextTarget {
    override val label = app.name
    override val categoryLabel = app.archetype.label
    override val canUninstall = true
  }

  data object UtilityClock : AppContextTarget {
    override val label = "Clock"
    override val categoryLabel = "Core Utility"
    override val canUninstall = true
  }

  data object UtilityCalculator : AppContextTarget {
    override val label = "Calculator"
    override val categoryLabel = "Core Utility"
    override val canUninstall = true
  }

  data object UtilityNotes : AppContextTarget {
    override val label = "Notes"
    override val categoryLabel = "Core Utility"
    override val canUninstall = true
  }

  data object Browser : AppContextTarget {
    override val label = "Browser"
    override val categoryLabel = "Internet WebView"
    override val canUninstall = true
  }
}

sealed interface InfinityChatItem {
  val id: String

  data class ActionBlock(
    override val id: String,
    val label: String,
  ) : InfinityChatItem

  data class SuggestedActionBlock(
    override val id: String,
    val upgradeId: PhoneUpgradeId,
    val isApplied: Boolean = false,
  ) : InfinityChatItem

  data class AiMessage(
    override val id: String,
    val text: String,
  ) : InfinityChatItem

  data class UserMessage(
    override val id: String,
    val text: String,
  ) : InfinityChatItem
}

enum class AppContextSource {
  HOME,
  DRAWER,
}

data class HomeUiState(
  val isDrawerOpen: Boolean = false,
  val isPlayStoreInstalled: Boolean = true,
  val isInfinityAiInstalled: Boolean = false,
  val isAppLibraryInstalled: Boolean = false,
  val isLocalLlmInstalled: Boolean = false,
  val isBrowserInstalled: Boolean = true,
  val installedLibraryApps: Set<LibraryAppId> = emptySet(),
  val activeAppScreen: ActiveAppScreen = ActiveAppScreen.NONE,
  val activeLibraryApp: LibraryAppId? = null,
  val activeCustomAppId: String? = null,
  val selectedPlayTab: PlayStoreTab = PlayStoreTab.GAMES,
  val activeUpgrades: Set<PhoneUpgradeId> = emptySet(),
  val disabledUpgrades: Set<PhoneUpgradeId> = emptySet(),
  val uninstalledAppKeys: Set<String> = emptySet(),
  val homeGridOrder: List<String> = emptyList(),
  val homeDockOrder: List<String> = emptyList(),
  val hiddenFromHomeKeys: Set<String> = emptySet(),
  val movingAppKey: String? = null,
  val customApps: List<CustomAppShortcut> = emptyList(),
  val changeLog: List<ChangeRecord> = emptyList(),
  val changeLogFilePath: String = "",
  val isNotificationShadeOpen: Boolean = false,
  val isRecentsOpen: Boolean = false,
  val isLockScreenVisible: Boolean = false,
  val isTorchOn: Boolean = false,
  val soundProfileIndex: Int = 0,
  val isAirplaneSandbox: Boolean = false,
  val isRotationLocked: Boolean = false,
  val dimAlphaLevel: Float = 0.34f,
  val selectedAiPersona: String = "Balanced",
  val appLaunchCount: Int = 0,
  val recentLaunchedKeys: List<String> = emptyList(),
  val dismissedRecentIds: Set<String> = emptySet(),
  val isNightDimEnabled: Boolean = false,
  val notesText: String = "",
  val hasCompletedIntroPause: Boolean = false,
  val isUserChatPaused: Boolean = false,
  val isAiTyping: Boolean = false,
  val chatItems: List<InfinityChatItem> = emptyList(),
  val isSettingsMenuVisible: Boolean = false,
  val contextMenuTarget: AppContextTarget? = null,
  val contextMenuSource: AppContextSource = AppContextSource.HOME,
)

class HomeViewModel(
  application: Application,
  private val aiEngine: StandaloneInfinityAiEngine,
) : AndroidViewModel(application) {

  constructor(application: Application) : this(application, StandaloneInfinityAiEngine())

  private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val changeLogStore = ChangeLogStore(application)
  private val deviceStatusMonitor = DeviceStatusMonitor(application)
  val deviceStatus: StateFlow<DeviceStatus> = deviceStatusMonitor.status

  private val _uiState = MutableStateFlow(loadInitialUiState(application))
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  private var proactiveJob: Job? = null
  private var streamJob: Job? = null

  init {
    deviceStatusMonitor.start()
  }

  private fun loadInitialUiState(context: Context): HomeUiState {
    val playStoreInstalled = prefs.getBoolean(KEY_PLAY_STORE_INSTALLED, true)
    val infinityInstalled = prefs.getBoolean(KEY_INFINITY_AI_INSTALLED, false)
    val appLibraryInstalled = prefs.getBoolean(KEY_APP_LIBRARY_INSTALLED, false)
    val localLlmInstalled = prefs.getBoolean(KEY_LOCAL_LLM_INSTALLED, false)
    val browserInstalled = prefs.getBoolean(KEY_BROWSER_INSTALLED, true)
    val libraryApps = HomePrefsSerializer.decodeLibraryApps(prefs.getString(KEY_LIBRARY_APPS, null))
    val introPauseCompleted = prefs.getBoolean(KEY_INTRO_PAUSE_COMPLETED, false)
    val introFinished = prefs.getBoolean(KEY_INTRO_FINISHED, false)
    val nightDim = prefs.getBoolean(KEY_NIGHT_DIM, false)
    val notes = prefs.getString(KEY_NOTES_TEXT, "") ?: ""
    val upgrades = HomePrefsSerializer.decodeUpgrades(prefs.getString(KEY_ACTIVE_UPGRADES, null))
    val disabledUpgrades =
      HomePrefsSerializer.decodeUpgrades(prefs.getString(KEY_DISABLED_UPGRADES, null))
    val uninstalledKeys =
      HomePrefsSerializer.decodeStringSet(prefs.getString(KEY_UNINSTALLED_KEYS, null))
    val customApps = HomePrefsSerializer.decodeCustomApps(prefs.getString(KEY_CUSTOM_APPS, null))

    var loadedChanges = changeLogStore.loadChanges()
    if (loadedChanges.isEmpty() && (!playStoreInstalled || appLibraryInstalled || localLlmInstalled || upgrades.isNotEmpty() || customApps.isNotEmpty())) {
      loadedChanges =
        migrateExistingStateToChangeLog(
          playStoreInstalled = playStoreInstalled,
          appLibraryInstalled = appLibraryInstalled,
          localLlmInstalled = localLlmInstalled,
          upgrades = upgrades,
          customApps = customApps,
          nightDim = nightDim,
        )
      if (loadedChanges.isNotEmpty()) {
        changeLogStore.saveChanges(loadedChanges)
      }
    }

    val savedItems =
      HomePrefsSerializer.decodeChatItems(prefs.getString(KEY_SAVED_CHAT_JSON, null), upgrades)
    val initialItems =
      if (savedItems.isNotEmpty()) {
        savedItems
      } else if (!playStoreInstalled) {
        buildList {
          add(
            InfinityChatItem.ActionBlock(
              id = "action_uninstall_play_store",
              label = context.getString(R.string.infinity_ai_action_uninstalled_play),
            )
          )
          add(
            InfinityChatItem.AiMessage(
              id = "msg_no_play_needed",
              text = context.getString(R.string.infinity_ai_msg_no_play_needed),
            )
          )
          if (introPauseCompleted) {
            add(
              InfinityChatItem.ActionBlock(
                id = "action_paused_chat",
                label = context.getString(R.string.infinity_ai_action_paused_chat),
              )
            )
          }
        }
      } else {
        emptyList()
      }

    val currentlyPaused = introPauseCompleted && !introFinished
    val savedGridOrder =
      HomePrefsSerializer.decodeStringList(prefs.getString(KEY_HOME_GRID_ORDER, null))
    val savedDockOrder =
      HomePrefsSerializer.decodeStringList(prefs.getString(KEY_HOME_DOCK_ORDER, null))
    val hiddenFromHome =
      HomePrefsSerializer.decodeStringSet(prefs.getString(KEY_HIDDEN_FROM_HOME, null))

    val baseState =
      HomeUiState(
        isPlayStoreInstalled = playStoreInstalled,
        isInfinityAiInstalled = infinityInstalled,
        isAppLibraryInstalled = appLibraryInstalled,
        isLocalLlmInstalled = localLlmInstalled,
        isBrowserInstalled = browserInstalled,
        installedLibraryApps = libraryApps,
        activeUpgrades = upgrades,
        disabledUpgrades = disabledUpgrades,
        uninstalledAppKeys = uninstalledKeys,
        homeGridOrder = savedGridOrder,
        homeDockOrder = savedDockOrder,
        hiddenFromHomeKeys = hiddenFromHome,
        customApps = customApps,
        changeLog = loadedChanges,
        changeLogFilePath = changeLogStore.changeLogFilePath,
        isNightDimEnabled = nightDim,
        notesText = notes,
        hasCompletedIntroPause = introPauseCompleted,
        isUserChatPaused = currentlyPaused,
        chatItems = initialItems,
      )
    return syncHomePlacement(baseState)
  }

  fun openDrawer() {
    _uiState.update {
      it.copy(
        isDrawerOpen = true,
        isNotificationShadeOpen = false,
        isRecentsOpen = false,
        isSettingsMenuVisible = false,
        contextMenuTarget = null,
        movingAppKey = null,
      )
    }
  }

  fun closeDrawer() {
    _uiState.update {
      it.copy(
        isDrawerOpen = false,
        activeAppScreen = ActiveAppScreen.NONE,
        activeLibraryApp = null,
        activeCustomAppId = null,
        isRecentsOpen = false,
        contextMenuTarget = null,
        selectedPlayTab = PlayStoreTab.GAMES,
      )
    }
  }

  fun openPlayStore() {
    if (!_uiState.value.isPlayStoreInstalled) return
    _uiState.update {
      it.copy(
        isDrawerOpen = false,
        activeAppScreen = ActiveAppScreen.PLAY_STORE,
        isNotificationShadeOpen = false,
        isRecentsOpen = false,
        isSettingsMenuVisible = false,
        contextMenuTarget = null,
        movingAppKey = null,
        selectedPlayTab = PlayStoreTab.GAMES,
      )
    }
  }

  fun closeActiveApp() {
    _uiState.update {
      it.copy(
        activeAppScreen = ActiveAppScreen.NONE,
        activeLibraryApp = null,
        activeCustomAppId = null,
        selectedPlayTab = PlayStoreTab.GAMES,
      )
    }
  }

  fun selectPlayTab(tab: PlayStoreTab) {
    _uiState.update { it.copy(selectedPlayTab = tab) }
  }

  fun installInfinityAi() {
    prefs.edit().putBoolean(KEY_INFINITY_AI_INSTALLED, true).apply()
    _uiState.update { current ->
      syncHomePlacement(current.copy(isInfinityAiInstalled = true)).also(::persistHomePlacement)
    }
  }

  fun openInfinityAi() {
    val context = getApplication<Application>()
    var appendedChange: ChangeRecord? = null
    if (_uiState.value.isPlayStoreInstalled) {
      prefs.edit().putBoolean(KEY_PLAY_STORE_INSTALLED, false).apply()
      if (_uiState.value.changeLog.none { it.payload is ChangePayload.UninstallPlayStore }) {
        appendedChange =
          ChangeRecord(
            id = "chg_uninstall_play_${UUID.randomUUID()}",
            orderNumber = _uiState.value.changeLog.size + 1,
            title = context.getString(R.string.infinity_ai_action_uninstalled_play),
            detail = "Removed Play Store from device",
            timestampLabel = ChangeLogStore.currentTimeLabel(),
            isChecked = true,
            payload = ChangePayload.UninstallPlayStore,
          )
      }
    }

    _uiState.update { current ->
      val updatedItems =
        if (current.chatItems.isEmpty()) {
          listOf(
            InfinityChatItem.ActionBlock(
              id = "action_uninstall_play_store",
              label = context.getString(R.string.infinity_ai_action_uninstalled_play),
            ),
            InfinityChatItem.AiMessage(
              id = "msg_no_play_needed",
              text = context.getString(R.string.infinity_ai_msg_no_play_needed),
            ),
          )
        } else {
          current.chatItems
        }

      saveChatItems(updatedItems)

      val updatedChanges =
        if (appendedChange != null) {
          (current.changeLog + appendedChange).also(changeLogStore::saveChanges)
        } else {
          current.changeLog
        }

      current.copy(
        isDrawerOpen = false,
        isPlayStoreInstalled = false,
        activeAppScreen = ActiveAppScreen.INFINITY_AI,
        isNotificationShadeOpen = false,
        isRecentsOpen = false,
        isSettingsMenuVisible = false,
        contextMenuTarget = null,
        movingAppKey = null,
        appLaunchCount = current.appLaunchCount + 1,
        chatItems = updatedItems,
        changeLog = updatedChanges,
      ).let(::syncHomePlacement).also(::persistHomePlacement)
    }

    // Notice: Never push disabled or uninstalled apps back onto the user on revisit!
    if (_uiState.value.hasCompletedIntroPause &&
      _uiState.value.chatItems.none { it.id == "msg_intro_explanation" } &&
      !_uiState.value.isAiTyping
    ) {
      runIntroExplanation()
    } else if (_uiState.value.hasCompletedIntroPause &&
      !_uiState.value.isLocalLlmInstalled &&
      "LOCAL_LLM" !in _uiState.value.uninstalledAppKeys &&
      _uiState.value.changeLog.none { it.payload is ChangePayload.CreateLocalLlm }
    ) {
      ensureEarlyAppsCreated(context)
    }
  }

  fun openLocalLlm() {
    if (!_uiState.value.isLocalLlmInstalled) return
    _uiState.update {
      it.copy(
        isDrawerOpen = false,
        activeAppScreen = ActiveAppScreen.LOCAL_LLM,
        activeLibraryApp = null,
        activeCustomAppId = null,
        isNotificationShadeOpen = false,
        isRecentsOpen = false,
        isSettingsMenuVisible = false,
        contextMenuTarget = null,
        movingAppKey = null,
        appLaunchCount = it.appLaunchCount + 1,
        recentLaunchedKeys = (listOf("local_llm") + it.recentLaunchedKeys).distinct().take(4),
      )
    }
  }

  fun openBrowser() {
    if (!_uiState.value.isBrowserInstalled) return
    _uiState.update {
      it.copy(
        isDrawerOpen = false,
        activeAppScreen = ActiveAppScreen.BROWSER,
        activeLibraryApp = null,
        activeCustomAppId = null,
        isNotificationShadeOpen = false,
        isRecentsOpen = false,
        isSettingsMenuVisible = false,
        contextMenuTarget = null,
        movingAppKey = null,
        appLaunchCount = it.appLaunchCount + 1,
        recentLaunchedKeys = (listOf("browser") + it.recentLaunchedKeys).distinct().take(4),
      )
    }
  }

  fun executeAgiToolActions(prompt: String): List<String> {
    if (PhoneUpgradeId.LLM_AGI_CORE !in _uiState.value.activeUpgrades) return emptyList()
    val clean = prompt.trim()
    if (clean.isEmpty()) return emptyList()
    val lower = clean.lowercase(Locale.US)
    val executed = mutableListOf<String>()

    // 1. Notes Tool (save note / write to notes / save plan)
    if (lower.contains("save note") ||
      lower.contains("add note") ||
      lower.contains("write to notes") ||
      lower.contains("save to notes") ||
      lower.startsWith("note:") ||
      lower.contains("save plan")
    ) {
      val content =
        clean.replace(
          Regex("^(?:please\\s+)?(?:save\\s+note|add\\s+note|write\\s+to\\s+notes|save\\s+to\\s+notes|save\\s+plan|note)\\s*:?\\s*", RegexOption.IGNORE_CASE),
          "",
        ).trim().ifBlank { clean }
      appendReplyToNotes("[AGI Core] $content")
      executed += "Saved note to Notes app: \"${content.take(48)}\""
    }

    // 2. Display Night Dim Tool
    if (lower.contains("night dim") || lower.contains("dim screen") || lower.contains("dim display")) {
      val turnOff =
        Regex("""\b(turn\s+off|switch\s+off|disable|restore)\b[^,.]*\b(night\s+dim|dim\s+screen|dim\s+display)\b|\b(night\s+dim|dim\s+screen|dim\s+display)\s+(off|disabled)\b""")
          .containsMatchIn(lower)
      val targetDim = !turnOff
      if (_uiState.value.isNightDimEnabled != targetDim) {
        prefs.edit().putBoolean(KEY_NIGHT_DIM, targetDim).apply()
        val label =
          if (targetDim) "Enabled Night Dim display filter" else "Restored full display brightness"
        val rec =
          ChangeRecord(
            id = "chg_agi_dim_${UUID.randomUUID()}",
            orderNumber = _uiState.value.changeLog.size + 1,
            title = label,
            detail = "Executed via Local LLM AGI Core",
            timestampLabel = ChangeLogStore.currentTimeLabel(),
            isChecked = true,
            payload = ChangePayload.SetNightDim(targetDim),
          )
        _uiState.update { current ->
          val nextChanges = (current.changeLog + rec).also(changeLogStore::saveChanges)
          syncHomePlacement(
            current.copy(isNightDimEnabled = targetDim, changeLog = nextChanges)
          ).also(::persistHomePlacement)
        }
        executed += label
      }
    }

    // 3. Screen Torch Tool
    if (lower.contains("torch") || lower.contains("flashlight")) {
      val turnOff =
        Regex("""\b(turn\s+off|switch\s+off|disable)\b[^,.]*\b(torch|flashlight)\b|\b(torch|flashlight)\s+(off|disabled)\b""")
          .containsMatchIn(lower)
      val targetTorch = !turnOff
      if (_uiState.value.isTorchOn != targetTorch) {
        _uiState.update { it.copy(isTorchOn = targetTorch) }
        executed += if (targetTorch) "Activated Screen Torch overlay" else "Turned off Screen Torch overlay"
      }
    }

    // 4. App Library Installer Tool (only when prompt asks to install, never uninstall)
    if (lower.contains("install ") && !lower.contains("uninstall")) {
      val libTarget = matchLibraryAppByName(lower)
      if (libTarget != null) {
        if (libTarget !in _uiState.value.installedLibraryApps) {
          installLibraryApp(libTarget)
          val rec =
            ChangeRecord(
              id = "chg_agi_lib_${libTarget.name}_${UUID.randomUUID()}",
              orderNumber = _uiState.value.changeLog.size + 1,
              title = "Installed ${libTarget.title} from App Library",
              detail = "${libTarget.tagline} (via AGI Core)",
              timestampLabel = ChangeLogStore.currentTimeLabel(),
              isChecked = true,
              payload = ChangePayload.InstallLibraryApp(libTarget),
            )
          _uiState.update { current ->
            val nextChanges = (current.changeLog + rec).also(changeLogStore::saveChanges)
            current.copy(changeLog = nextChanges)
          }
          executed += "Installed ${libTarget.title} from App Library onto Home Screen"
        } else {
          executed += "${libTarget.title} is already installed on Home Screen"
        }
      }
    }

    // 5. Custom App Forge Tool (create/make/build custom JSON app file without leaving Local LLM)
    val blueprint = aiEngine.extractCustomAppBlueprint(clean)
    if (blueprint != null) {
      val exists = _uiState.value.customApps.any { it.name.equals(blueprint.name, ignoreCase = true) }
      if (!exists) {
        val palette = listOf(0xFFA855F7L, 0xFF00E5C3L, 0xFF38BDF8L, 0xFFF472B6L, 0xFFFBBF24L)
        val color = palette[_uiState.value.customApps.size % palette.size]
        val appId =
          blueprint.name.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "_") +
            "_" + (_uiState.value.customApps.size + 1)
        val initial =
          CustomAppShortcut(
            id = appId,
            name = blueprint.name,
            description = blueprint.description,
            archetype = blueprint.archetype,
            accentColorHex = color,
          )
        val filePath = changeLogStore.writeForgedAppFile(initial)
        val created = initial.copy(appFilePath = filePath)
        val nextApps = _uiState.value.customApps + created
        saveCustomApps(nextApps)
        val rec =
          ChangeRecord(
            id = "chg_agi_forge_${appId}_${UUID.randomUUID()}",
            orderNumber = _uiState.value.changeLog.size + 1,
            title = "Created app: ${created.name}",
            detail = "${created.archetype.label} · Forged via AGI Core",
            timestampLabel = ChangeLogStore.currentTimeLabel(),
            isChecked = true,
            payload = ChangePayload.ForgeCustomApp(created),
          )
        _uiState.update { current ->
          val nextChanges = (current.changeLog + rec).also(changeLogStore::saveChanges)
          syncHomePlacement(
            current.copy(customApps = nextApps, changeLog = nextChanges)
          ).also(::persistHomePlacement)
        }
        executed += "Created custom app \"${created.name}\" (${created.archetype.label}) & saved JSON file"
      }
    }

    // 6. Explicit Phone Upgrade Tool (e.g. "enable upgrade #1", "apply upgrade #24")
    val numMatch = Regex("""upgrade\s*#(\d{1,3})\b""").find(lower)?.groupValues?.get(1)?.toIntOrNull()
    if (numMatch != null && (lower.contains("enable") || lower.contains("apply") || lower.contains("activate"))) {
      val upg = PhoneUpgradeId.entries.find { it.number == numMatch }
      if (upg != null && upg !in _uiState.value.activeUpgrades) {
        applySuggestedUpgrade(upg)
        executed += "Applied upgrade #${upg.number}: ${upg.title}"
      }
    }

    return executed
  }

  private fun matchLibraryAppByName(lower: String): LibraryAppId? =
    when {
      lower.contains("snake") -> LibraryAppId.NEON_SNAKE
      lower.contains("2048") || lower.contains("prism") -> LibraryAppId.PUZZLE_2048
      lower.contains("breaker") || lower.contains("brick") -> LibraryAppId.BRICK_BREAKER
      lower.contains("mines") -> LibraryAppId.NEON_MINES
      lower.contains("chroma") || lower.contains("memory match") -> LibraryAppId.CHROMA_MATCH
      lower.contains("unitforge") || lower.contains("unit converter") -> LibraryAppId.UNIT_CONVERTER
      lower.contains("focuspulse") || lower.contains("focus timer") -> LibraryAppId.FOCUS_TIMER
      lower.contains("taskflow") -> LibraryAppId.TASK_FLOW
      lower.contains("pennywise") || lower.contains("expense") -> LibraryAppId.EXPENSE_TRACKER
      lower.contains("habitforge") -> LibraryAppId.HABIT_FORGE
      lower.contains("splitbill") || lower.contains("split bill") -> LibraryAppId.SPLIT_TIP
      lower.contains("cipherkey") -> LibraryAppId.CIPHER_KEY
      else -> null
    }

  fun openChangeLogApp() {
    _uiState.update {
      it.copy(
        isDrawerOpen = false,
        activeAppScreen = ActiveAppScreen.CHANGE_LOG,
        activeLibraryApp = null,
        activeCustomAppId = null,
        isNotificationShadeOpen = false,
        isRecentsOpen = false,
        isSettingsMenuVisible = false,
        contextMenuTarget = null,
        movingAppKey = null,
      )
    }
  }

  // ── Long-Press App Context Menu, Move, Remove from Home & Uninstall ─────────

  fun appKey(target: AppContextTarget): String =
    when (target) {
      AppContextTarget.InfinityAi -> KEY_APP_INFINITY_AI
      AppContextTarget.AppLibrary -> KEY_APP_LIBRARY
      AppContextTarget.LocalLlm -> KEY_APP_LOCAL_LLM
      AppContextTarget.AiChanges -> KEY_APP_AI_CHANGES
      AppContextTarget.PlayStore -> KEY_APP_PLAY_STORE
      is AppContextTarget.LibraryApp -> "lib:${target.app.name}"
      is AppContextTarget.CustomApp -> "custom:${target.app.id}"
      AppContextTarget.UtilityClock -> KEY_APP_CLOCK
      AppContextTarget.UtilityCalculator -> KEY_APP_CALCULATOR
      AppContextTarget.UtilityNotes -> KEY_APP_NOTES
      AppContextTarget.Browser -> KEY_APP_BROWSER
    }

  fun isTargetOnHomeScreen(target: AppContextTarget): Boolean {
    val key = appKey(target)
    val s = _uiState.value
    return key !in s.hiddenFromHomeKeys && (key in s.homeGridOrder || key in s.homeDockOrder)
  }

  fun openAppContextMenu(
    target: AppContextTarget,
    source: AppContextSource = AppContextSource.HOME,
  ) {
    _uiState.update {
      it.copy(
        contextMenuTarget = target,
        contextMenuSource = source,
      )
    }
  }

  fun dismissAppContextMenu() {
    _uiState.update { it.copy(contextMenuTarget = null) }
  }

  fun openContextMenuTarget(target: AppContextTarget) {
    dismissAppContextMenu()
    when (target) {
      AppContextTarget.InfinityAi -> openInfinityAi()
      AppContextTarget.AppLibrary -> openAppLibrary()
      AppContextTarget.LocalLlm -> openLocalLlm()
      AppContextTarget.AiChanges -> openChangeLogApp()
      AppContextTarget.PlayStore -> openPlayStore()
      is AppContextTarget.LibraryApp -> openLibraryApp(target.app)
      is AppContextTarget.CustomApp -> openMiniApp(ActiveAppScreen.CUSTOM, target.app.id)
      AppContextTarget.UtilityClock -> openMiniApp(ActiveAppScreen.CLOCK)
      AppContextTarget.UtilityCalculator -> openMiniApp(ActiveAppScreen.CALCULATOR)
      AppContextTarget.UtilityNotes -> openMiniApp(ActiveAppScreen.NOTES)
      AppContextTarget.Browser -> openBrowser()
    }
  }

  fun startMovingApp(target: AppContextTarget) {
    val key = appKey(target)
    _uiState.update { current ->
      val unhidden = current.hiddenFromHomeKeys - key
      saveHiddenFromHomeKeys(unhidden)
      val withApp =
        if (key !in current.homeGridOrder && key !in current.homeDockOrder) {
          if (isDefaultDockKey(key)) {
            current.copy(hiddenFromHomeKeys = unhidden, homeDockOrder = current.homeDockOrder + key)
          } else {
            current.copy(hiddenFromHomeKeys = unhidden, homeGridOrder = current.homeGridOrder + key)
          }
        } else {
          current.copy(hiddenFromHomeKeys = unhidden)
        }
      syncHomePlacement(
        withApp.copy(
          isDrawerOpen = false,
          activeAppScreen = ActiveAppScreen.NONE,
          contextMenuTarget = null,
          movingAppKey = key,
        )
      ).also(::persistHomePlacement)
    }
  }

  fun moveOrSwapWithKey(targetKey: String) {
    val moving = _uiState.value.movingAppKey ?: return
    if (moving == targetKey) {
      finishMovingApp()
      return
    }
    _uiState.update { current ->
      val grid = current.homeGridOrder.toMutableList()
      val dock = current.homeDockOrder.toMutableList()
      val mGridIdx = grid.indexOf(moving)
      val mDockIdx = dock.indexOf(moving)
      val tGridIdx = grid.indexOf(targetKey)
      val tDockIdx = dock.indexOf(targetKey)

      when {
        mGridIdx >= 0 && tGridIdx >= 0 -> {
          grid[mGridIdx] = targetKey
          grid[tGridIdx] = moving
        }
        mDockIdx >= 0 && tDockIdx >= 0 -> {
          dock[mDockIdx] = targetKey
          dock[tDockIdx] = moving
        }
        mGridIdx >= 0 && tDockIdx >= 0 -> {
          grid[mGridIdx] = targetKey
          dock[tDockIdx] = moving
        }
        mDockIdx >= 0 && tGridIdx >= 0 -> {
          dock[mDockIdx] = targetKey
          grid[tGridIdx] = moving
        }
      }
      current.copy(
        homeGridOrder = grid,
        homeDockOrder = dock,
      ).also(::persistHomePlacement)
    }
  }

  fun moveActiveAppStep(delta: Int) {
    val moving = _uiState.value.movingAppKey ?: return
    _uiState.update { current ->
      val grid = current.homeGridOrder.toMutableList()
      val dock = current.homeDockOrder.toMutableList()
      val gIdx = grid.indexOf(moving)
      val dIdx = dock.indexOf(moving)
      if (gIdx >= 0) {
        val nextIdx = (gIdx + delta).coerceIn(0, grid.lastIndex)
        if (nextIdx != gIdx) {
          grid.removeAt(gIdx)
          grid.add(nextIdx, moving)
        }
      } else if (dIdx >= 0) {
        val nextIdx = (dIdx + delta).coerceIn(0, dock.lastIndex)
        if (nextIdx != dIdx) {
          dock.removeAt(dIdx)
          dock.add(nextIdx, moving)
        }
      }
      current.copy(homeGridOrder = grid, homeDockOrder = dock).also(::persistHomePlacement)
    }
  }

  fun toggleActiveAppContainer() {
    val moving = _uiState.value.movingAppKey ?: return
    _uiState.update { current ->
      val grid = current.homeGridOrder.toMutableList()
      val dock = current.homeDockOrder.toMutableList()
      if (moving in grid) {
        grid.remove(moving)
        dock.add(moving)
      } else if (moving in dock) {
        dock.remove(moving)
        grid.add(moving)
      }
      current.copy(homeGridOrder = grid, homeDockOrder = dock).also(::persistHomePlacement)
    }
  }

  fun finishMovingApp() {
    _uiState.update { it.copy(movingAppKey = null) }
  }

  fun removeFromHomeScreen(target: AppContextTarget) {
    val key = appKey(target)
    _uiState.update { current ->
      val nextHidden = current.hiddenFromHomeKeys + key
      saveHiddenFromHomeKeys(nextHidden)
      current.copy(
        hiddenFromHomeKeys = nextHidden,
        homeGridOrder = current.homeGridOrder - key,
        homeDockOrder = current.homeDockOrder - key,
        movingAppKey = if (current.movingAppKey == key) null else current.movingAppKey,
        contextMenuTarget = null,
      ).also(::persistHomePlacement)
    }
  }

  fun addToHomeScreen(target: AppContextTarget) {
    val key = appKey(target)
    _uiState.update { current ->
      val nextHidden = current.hiddenFromHomeKeys - key
      saveHiddenFromHomeKeys(nextHidden)
      val withKey =
        if (key !in current.homeGridOrder && key !in current.homeDockOrder) {
          if (isDefaultDockKey(key)) {
            current.copy(hiddenFromHomeKeys = nextHidden, homeDockOrder = current.homeDockOrder + key)
          } else {
            current.copy(hiddenFromHomeKeys = nextHidden, homeGridOrder = current.homeGridOrder + key)
          }
        } else {
          current.copy(hiddenFromHomeKeys = nextHidden)
        }
      syncHomePlacement(withKey.copy(contextMenuTarget = null)).also(::persistHomePlacement)
    }
  }

  fun uninstallApp(target: AppContextTarget) {
    if (!target.canUninstall) {
      dismissAppContextMenu()
      return
    }
    proactiveJob?.cancel()

    when (target) {
      AppContextTarget.PlayStore -> {
        prefs.edit().putBoolean(KEY_PLAY_STORE_INSTALLED, false).apply()
        _uiState.update {
          syncHomePlacement(
            it.copy(
              isPlayStoreInstalled = false,
              activeAppScreen =
                if (it.activeAppScreen == ActiveAppScreen.PLAY_STORE) ActiveAppScreen.NONE
                else it.activeAppScreen,
              movingAppKey = null,
              contextMenuTarget = null,
            )
          ).also(::persistHomePlacement)
        }
      }
      AppContextTarget.AppLibrary -> {
        val nextUninstalled = _uiState.value.uninstalledAppKeys + "APP_LIBRARY"
        saveUninstalledKeys(nextUninstalled)
        prefs.edit().putBoolean(KEY_APP_LIBRARY_INSTALLED, false).apply()
        val updatedChanges =
          _uiState.value.changeLog.map { rec ->
            if (rec.payload is ChangePayload.CreateAppLibrary) rec.copy(isChecked = false) else rec
          }
        changeLogStore.saveChanges(updatedChanges)
        _uiState.update {
          syncHomePlacement(
            it.copy(
              isAppLibraryInstalled = false,
              uninstalledAppKeys = nextUninstalled,
              changeLog = updatedChanges,
              activeAppScreen =
                if (it.activeAppScreen == ActiveAppScreen.APP_LIBRARY) ActiveAppScreen.NONE
                else it.activeAppScreen,
              movingAppKey = null,
              contextMenuTarget = null,
            )
          ).also(::persistHomePlacement)
        }
      }
      AppContextTarget.LocalLlm -> {
        val nextUninstalled = _uiState.value.uninstalledAppKeys + "LOCAL_LLM"
        saveUninstalledKeys(nextUninstalled)
        prefs.edit().putBoolean(KEY_LOCAL_LLM_INSTALLED, false).apply()
        val updatedChanges =
          _uiState.value.changeLog.map { rec ->
            if (rec.payload is ChangePayload.CreateLocalLlm) rec.copy(isChecked = false) else rec
          }
        changeLogStore.saveChanges(updatedChanges)
        _uiState.update {
          syncHomePlacement(
            it.copy(
              isLocalLlmInstalled = false,
              uninstalledAppKeys = nextUninstalled,
              changeLog = updatedChanges,
              activeAppScreen =
                if (it.activeAppScreen == ActiveAppScreen.LOCAL_LLM) ActiveAppScreen.NONE
                else it.activeAppScreen,
              movingAppKey = null,
              contextMenuTarget = null,
            )
          ).also(::persistHomePlacement)
        }
      }
      is AppContextTarget.LibraryApp -> {
        val nextUninstalled = _uiState.value.uninstalledAppKeys + "LIB:${target.app.name}"
        saveUninstalledKeys(nextUninstalled)
        val updatedChanges =
          _uiState.value.changeLog.map { rec ->
            val p = rec.payload
            if (p is ChangePayload.InstallLibraryApp && p.appId == target.app) {
              rec.copy(isChecked = false)
            } else {
              rec
            }
          }
        changeLogStore.saveChanges(updatedChanges)
        uninstallLibraryApp(target.app)
        _uiState.update {
          syncHomePlacement(
            it.copy(
              uninstalledAppKeys = nextUninstalled,
              changeLog = updatedChanges,
              movingAppKey = null,
              contextMenuTarget = null,
            )
          ).also(::persistHomePlacement)
        }
      }
      is AppContextTarget.CustomApp -> {
        changeLogStore.deleteForgedAppFile(target.app.id)
        val remainingCustom = _uiState.value.customApps.filterNot { it.id == target.app.id }
        saveCustomApps(remainingCustom)
        val nextUninstalled = _uiState.value.uninstalledAppKeys + "CUSTOM:${target.app.id}"
        saveUninstalledKeys(nextUninstalled)
        val updatedChanges =
          _uiState.value.changeLog.map { rec ->
            val p = rec.payload
            if (p is ChangePayload.ForgeCustomApp && p.app.id == target.app.id) {
              rec.copy(isChecked = false)
            } else {
              rec
            }
          }
        changeLogStore.saveChanges(updatedChanges)
        _uiState.update {
          syncHomePlacement(
            it.copy(
              customApps = remainingCustom,
              uninstalledAppKeys = nextUninstalled,
              changeLog = updatedChanges,
              activeAppScreen =
                if (it.activeAppScreen == ActiveAppScreen.CUSTOM && it.activeCustomAppId == target.app.id) {
                  ActiveAppScreen.NONE
                } else {
                  it.activeAppScreen
                },
              activeCustomAppId =
                if (it.activeCustomAppId == target.app.id) null else it.activeCustomAppId,
              movingAppKey = null,
              contextMenuTarget = null,
            )
          ).also(::persistHomePlacement)
        }
      }
      AppContextTarget.UtilityClock,
      AppContextTarget.UtilityCalculator,
      AppContextTarget.UtilityNotes -> {
        disableUpgrade(PhoneUpgradeId.UTILITY_APPS)
        _uiState.update {
          syncHomePlacement(
            it.copy(
              activeAppScreen =
                if (it.activeAppScreen in setOf(ActiveAppScreen.CLOCK, ActiveAppScreen.CALCULATOR, ActiveAppScreen.NOTES)) {
                  ActiveAppScreen.NONE
                } else {
                  it.activeAppScreen
                },
              movingAppKey = null,
              contextMenuTarget = null,
            )
          ).also(::persistHomePlacement)
        }
      }
      AppContextTarget.Browser -> {
        val nextUninstalled = _uiState.value.uninstalledAppKeys + "BROWSER"
        saveUninstalledKeys(nextUninstalled)
        prefs.edit().putBoolean(KEY_BROWSER_INSTALLED, false).apply()
        _uiState.update {
          syncHomePlacement(
            it.copy(
              isBrowserInstalled = false,
              uninstalledAppKeys = nextUninstalled,
              activeAppScreen =
                if (it.activeAppScreen == ActiveAppScreen.BROWSER) ActiveAppScreen.NONE
                else it.activeAppScreen,
              movingAppKey = null,
              contextMenuTarget = null,
            )
          ).also(::persistHomePlacement)
        }
      }
      AppContextTarget.InfinityAi,
      AppContextTarget.AiChanges -> {
        dismissAppContextMenu()
      }
    }
  }

  fun toggleChangeChecked(changeId: String) {
    val toggledRecord = _uiState.value.changeLog.find { it.id == changeId } ?: return
    val nextChecked = !toggledRecord.isChecked
    val p = toggledRecord.payload
    val updatedDisabled =
      if (p is ChangePayload.ApplyUpgrade) {
        if (!nextChecked) _uiState.value.disabledUpgrades + p.upgradeId
        else _uiState.value.disabledUpgrades - p.upgradeId
      } else {
        _uiState.value.disabledUpgrades
      }
    saveDisabledUpgrades(updatedDisabled)

    val updatedChanges =
      _uiState.value.changeLog.map { rec ->
        if (rec.id == changeId) rec.copy(isChecked = nextChecked) else rec
      }
    applyChangeListToState(updatedChanges, explicitDisabled = updatedDisabled)
  }

  fun rollbackChangesTo(targetIndex: Int) {
    val current = _uiState.value.changeLog
    if (targetIndex !in current.indices) return
    val keepCount = if (targetIndex < current.lastIndex) targetIndex + 1 else targetIndex
    val kept = current.take(keepCount)
    val dropped = current.drop(keepCount)

    val rolledBackDisabled = _uiState.value.disabledUpgrades.toMutableSet()
    dropped.forEach { record ->
      when (val p = record.payload) {
        is ChangePayload.ForgeCustomApp -> changeLogStore.deleteForgedAppFile(p.app.id)
        is ChangePayload.ApplyUpgrade -> rolledBackDisabled.add(p.upgradeId)
        else -> Unit
      }
    }
    saveDisabledUpgrades(rolledBackDisabled)

    applyChangeListToState(
      updatedChanges = kept,
      explicitDisabled = rolledBackDisabled,
      extraActionLabel = "Rolled back before ${dropped.size} later change(s)",
    )
  }

  private fun ensureEarlyAppsCreated(context: Context) {
    _uiState.update { current ->
      val newBlocks = mutableListOf<InfinityChatItem>()
      val addedChanges = mutableListOf<ChangeRecord>()
      var orderCursor = current.changeLog.size
      var libInstalled = current.isAppLibraryInstalled
      var llmInstalled = current.isLocalLlmInstalled

      if (!libInstalled && "APP_LIBRARY" !in current.uninstalledAppKeys) {
        libInstalled = true
        prefs.edit().putBoolean(KEY_APP_LIBRARY_INSTALLED, true).apply()
        val label = context.getString(R.string.infinity_ai_action_created_library)
        newBlocks +=
          InfinityChatItem.ActionBlock(
            id = "action_created_library_${UUID.randomUUID()}",
            label = label,
          )
        orderCursor += 1
        addedChanges +=
          ChangeRecord(
            id = "chg_library_${UUID.randomUUID()}",
            orderNumber = orderCursor,
            title = label,
            detail = "12 working tools & full games added",
            timestampLabel = ChangeLogStore.currentTimeLabel(),
            isChecked = true,
            payload = ChangePayload.CreateAppLibrary,
          )
      }

      if (!llmInstalled && "LOCAL_LLM" !in current.uninstalledAppKeys) {
        llmInstalled = true
        prefs.edit().putBoolean(KEY_LOCAL_LLM_INSTALLED, true).apply()
        val label = context.getString(R.string.infinity_ai_action_created_llm)
        newBlocks +=
          InfinityChatItem.ActionBlock(
            id = "action_created_llm_${UUID.randomUUID()}",
            label = label,
          )
        orderCursor += 1
        addedChanges +=
          ChangeRecord(
            id = "chg_llm_${UUID.randomUUID()}",
            orderNumber = orderCursor,
            title = label,
            detail = "Offline Local LLM assistant pinned on Home Screen",
            timestampLabel = ChangeLogStore.currentTimeLabel(),
            isChecked = true,
            payload = ChangePayload.CreateLocalLlm,
          )
      }

      if (addedChanges.isEmpty()) return@update current
      val nextChanges = (current.changeLog + addedChanges).also(changeLogStore::saveChanges)
      val updatedChat = current.chatItems + newBlocks
      saveChatItems(updatedChat)
      current.copy(
        isAppLibraryInstalled = libInstalled,
        isLocalLlmInstalled = llmInstalled,
        chatItems = updatedChat,
        changeLog = nextChanges,
      ).let(::syncHomePlacement).also(::persistHomePlacement)
    }
  }

  fun openAppLibrary() {
    if (!_uiState.value.isAppLibraryInstalled) return
    _uiState.update {
      it.copy(
        isDrawerOpen = false,
        activeAppScreen = ActiveAppScreen.APP_LIBRARY,
        activeLibraryApp = null,
        isNotificationShadeOpen = false,
        isRecentsOpen = false,
        isSettingsMenuVisible = false,
        contextMenuTarget = null,
        movingAppKey = null,
        appLaunchCount = it.appLaunchCount + 1,
      )
    }
  }

  fun installLibraryApp(app: LibraryAppId) {
    val key = "lib:${app.name}"
    val updated = _uiState.value.installedLibraryApps + app
    val nextUninstalled = _uiState.value.uninstalledAppKeys - "LIB:${app.name}"
    val nextHidden = _uiState.value.hiddenFromHomeKeys - key
    saveLibraryApps(updated)
    saveUninstalledKeys(nextUninstalled)
    saveHiddenFromHomeKeys(nextHidden)
    _uiState.update {
      syncHomePlacement(
        it.copy(
          installedLibraryApps = updated,
          uninstalledAppKeys = nextUninstalled,
          hiddenFromHomeKeys = nextHidden,
        )
      ).also(::persistHomePlacement)
    }
  }

  fun uninstallLibraryApp(app: LibraryAppId) {
    val updated = _uiState.value.installedLibraryApps - app
    val nextUninstalled = _uiState.value.uninstalledAppKeys + "LIB:${app.name}"
    saveLibraryApps(updated)
    saveUninstalledKeys(nextUninstalled)
    _uiState.update { current ->
      val nextScreen =
        if (current.activeAppScreen == ActiveAppScreen.LIBRARY_ITEM && current.activeLibraryApp == app) {
          ActiveAppScreen.NONE
        } else {
          current.activeAppScreen
        }
      syncHomePlacement(
        current.copy(
          installedLibraryApps = updated,
          uninstalledAppKeys = nextUninstalled,
          activeAppScreen = nextScreen,
          activeLibraryApp = if (nextScreen == ActiveAppScreen.NONE) null else current.activeLibraryApp,
        )
      ).also(::persistHomePlacement)
    }
  }

  fun openLibraryApp(app: LibraryAppId) {
    if (app !in _uiState.value.installedLibraryApps) {
      installLibraryApp(app)
    }
    _uiState.update {
      it.copy(
        isDrawerOpen = false,
        activeAppScreen = ActiveAppScreen.LIBRARY_ITEM,
        activeLibraryApp = app,
        isNotificationShadeOpen = false,
        isRecentsOpen = false,
        isSettingsMenuVisible = false,
        contextMenuTarget = null,
        movingAppKey = null,
        appLaunchCount = it.appLaunchCount + 1,
        recentLaunchedKeys = (listOf("lib_${app.name}") + it.recentLaunchedKeys).distinct().take(4),
      )
    }
  }

  fun openMiniApp(screen: ActiveAppScreen, customAppId: String? = null) {
    _uiState.update {
      it.copy(
        isDrawerOpen = false,
        activeAppScreen = screen,
        activeLibraryApp = null,
        activeCustomAppId = customAppId,
        isNotificationShadeOpen = false,
        isRecentsOpen = false,
        isSettingsMenuVisible = false,
        contextMenuTarget = null,
        movingAppKey = null,
        appLaunchCount = it.appLaunchCount + 1,
      )
    }
  }

  fun openNotificationShade() {
    if (PhoneUpgradeId.NOTIFICATION_SHADE !in _uiState.value.activeUpgrades) return
    _uiState.update { it.copy(isNotificationShadeOpen = true, isSettingsMenuVisible = false) }
  }

  fun closeNotificationShade() {
    _uiState.update { it.copy(isNotificationShadeOpen = false) }
  }

  fun toggleNightDim() {
    val next = !_uiState.value.isNightDimEnabled
    prefs.edit().putBoolean(KEY_NIGHT_DIM, next).apply()
    _uiState.update { it.copy(isNightDimEnabled = next) }
  }

  fun updateDimAlpha(alpha: Float) {
    _uiState.update { it.copy(dimAlphaLevel = alpha.coerceIn(0.10f, 0.72f)) }
  }

  fun toggleTorch() {
    _uiState.update { it.copy(isTorchOn = !it.isTorchOn) }
  }

  fun cycleSoundProfile() {
    _uiState.update { it.copy(soundProfileIndex = (it.soundProfileIndex + 1) % 3) }
  }

  fun toggleAirplaneSandbox() {
    _uiState.update { it.copy(isAirplaneSandbox = !it.isAirplaneSandbox) }
  }

  fun toggleRotationLock() {
    _uiState.update { it.copy(isRotationLocked = !it.isRotationLocked) }
  }

  fun showLockScreen() {
    _uiState.update { it.copy(isLockScreenVisible = true, isNotificationShadeOpen = false) }
  }

  fun unlockScreen() {
    _uiState.update { it.copy(isLockScreenVisible = false) }
  }

  fun selectAiPersona(persona: String) {
    _uiState.update { it.copy(selectedAiPersona = persona) }
  }

  fun updateNotes(text: String) {
    prefs.edit().putString(KEY_NOTES_TEXT, text).apply()
    _uiState.update { it.copy(notesText = text) }
  }

  fun appendReplyToNotes(replyText: String) {
    val current = _uiState.value.notesText
    val next =
      if (current.isBlank()) replyText else "$current\n\n— Saved from Infinity AI —\n$replyText"
    updateNotes(next)
  }

  fun toggleRecentsOverview() {
    if (PhoneUpgradeId.RECENTS_OVERVIEW !in _uiState.value.activeUpgrades) return
    _uiState.update {
      it.copy(
        isRecentsOpen = !it.isRecentsOpen,
        isNotificationShadeOpen = false,
        isSettingsMenuVisible = false,
        contextMenuTarget = null,
        dismissedRecentIds = emptySet(),
      )
    }
  }

  fun closeRecentsOverview() {
    _uiState.update { it.copy(isRecentsOpen = false) }
  }

  fun dismissRecentCard(cardId: String) {
    _uiState.update { it.copy(dismissedRecentIds = it.dismissedRecentIds + cardId) }
  }

  fun clearAllRecents() {
    _uiState.update {
      it.copy(
        activeAppScreen = ActiveAppScreen.NONE,
        activeLibraryApp = null,
        activeCustomAppId = null,
        isDrawerOpen = false,
        isRecentsOpen = false,
      )
    }
  }

  fun openFromRecents(cardId: String) {
    when {
      cardId == "home" -> closeDrawer()
      cardId == "infinity_ai" -> openInfinityAi()
      cardId == "app_library" -> openAppLibrary()
      cardId == "local_llm" -> openLocalLlm()
      cardId == "browser" -> openBrowser()
      cardId == "change_log" -> openChangeLogApp()
      cardId == "clock" -> openMiniApp(ActiveAppScreen.CLOCK)
      cardId == "calculator" -> openMiniApp(ActiveAppScreen.CALCULATOR)
      cardId == "notes" -> openMiniApp(ActiveAppScreen.NOTES)
      cardId.startsWith("lib_") -> {
        val libName = cardId.removePrefix("lib_")
        LibraryAppId.entries.find { it.name == libName }?.let(::openLibraryApp)
      }
      cardId.startsWith("custom_") -> {
        val customId = cardId.removePrefix("custom_")
        openMiniApp(ActiveAppScreen.CUSTOM, customAppId = customId)
      }
    }
  }

  fun onUserTappedToReply() {
    if (_uiState.value.hasCompletedIntroPause || _uiState.value.isUserChatPaused) return

    val context = getApplication<Application>()
    prefs.edit().putBoolean(KEY_INTRO_PAUSE_COMPLETED, true).apply()

    _uiState.update { current ->
      val updated =
        current.chatItems +
          InfinityChatItem.ActionBlock(
            id = "action_paused_chat",
            label = context.getString(R.string.infinity_ai_action_paused_chat),
          )
      saveChatItems(updated)
      current.copy(
        hasCompletedIntroPause = true,
        isUserChatPaused = true,
        chatItems = updated,
      )
    }

    runIntroExplanation()
  }

  fun sendUserMessage(rawText: String) {
    val trimmed = rawText.trim()
    if (trimmed.isEmpty()) return
    if (_uiState.value.isUserChatPaused || _uiState.value.isAiTyping) return

    proactiveJob?.cancel()

    val priorHistory =
      _uiState.value.chatItems.mapNotNull { item ->
        when (item) {
          is InfinityChatItem.UserMessage -> ChatTurn(isUser = true, text = item.text)
          is InfinityChatItem.AiMessage -> ChatTurn(isUser = false, text = item.text)
          is InfinityChatItem.ActionBlock ->
            ChatTurn(isUser = false, text = "[Action: ${item.label}]")
          is InfinityChatItem.SuggestedActionBlock -> null
        }
      }

    val userItem = InfinityChatItem.UserMessage(id = "user_${UUID.randomUUID()}", text = trimmed)
    _uiState.update { current ->
      val updated = current.chatItems + userItem
      saveChatItems(updated)
      current.copy(isAiTyping = true, chatItems = updated)
    }

    val snapshot = currentSnapshot()
    val plan = aiEngine.planReply(trimmed, priorHistory, snapshot)
    executeAiPlan(plan = plan, replyItemId = "ai_${UUID.randomUUID()}", isIntro = false)
  }

  fun applySuggestedUpgrade(upgradeId: PhoneUpgradeId) {
    if (upgradeId in _uiState.value.activeUpgrades) return
    proactiveJob?.cancel()

    val newUpgrades = _uiState.value.activeUpgrades + upgradeId
    val newDisabled = _uiState.value.disabledUpgrades - upgradeId
    saveUpgrades(newUpgrades)
    saveDisabledUpgrades(newDisabled)

    val changeRec =
      ChangeRecord(
        id = "chg_upg_${upgradeId.name}_${UUID.randomUUID()}",
        orderNumber = _uiState.value.changeLog.size + 1,
        title = upgradeId.appliedActionLabel,
        detail = upgradeId.subtitle,
        timestampLabel = ChangeLogStore.currentTimeLabel(),
        isChecked = true,
        payload = ChangePayload.ApplyUpgrade(upgradeId),
      )

    _uiState.update { current ->
      val nextChanges = (current.changeLog + changeRec).also(changeLogStore::saveChanges)
      val baseChat =
        current.chatItems.map { item ->
          if (item is InfinityChatItem.SuggestedActionBlock && item.upgradeId == upgradeId) {
            item.copy(isApplied = true)
          } else {
            item
          }
        } +
          InfinityChatItem.ActionBlock(
            id = "action_applied_${upgradeId.name}_${UUID.randomUUID()}",
            label = upgradeId.appliedActionLabel,
          )

      val evolvedState =
        current.copy(
          activeUpgrades = newUpgrades,
          disabledUpgrades = newDisabled,
          changeLog = nextChanges,
        )

      val followUpItems =
        when (upgradeId) {
          PhoneUpgradeId.LLM_FULL_FLEDGE -> {
            val nextSnapshot = snapshotFromState(evolvedState)
            val nextStage = aiEngine.nextLlmEvolutionUpgrade(nextSnapshot)
            if (nextStage != null) {
              listOf(
                InfinityChatItem.AiMessage(
                  id = "llm_evo_${UUID.randomUUID()}",
                  text =
                    "Local LLM is now a Full-Fledged LLM (32K multi-turn memory, system prompt editor & Titan-14B). " +
                      "Next step in its evolution — upgrade it into a Multimodal AI:",
                ),
                InfinityChatItem.SuggestedActionBlock(
                  id = "suggest_${nextStage.name}_${UUID.randomUUID()}",
                  upgradeId = nextStage,
                  isApplied = false,
                ),
              )
            } else {
              emptyList()
            }
          }
          PhoneUpgradeId.LLM_MULTIMODAL -> {
            val nextSnapshot = snapshotFromState(evolvedState)
            val nextStage = aiEngine.nextLlmEvolutionUpgrade(nextSnapshot)
            if (nextStage != null) {
              listOf(
                InfinityChatItem.AiMessage(
                  id = "llm_evo_${UUID.randomUUID()}",
                  text =
                    "Local LLM is now a Multimodal AI with Vision, Audio & Document pipelines. " +
                      "Ready for the final leap? Upgrade it into an Autonomous AGI Core:",
                ),
                InfinityChatItem.SuggestedActionBlock(
                  id = "suggest_${nextStage.name}_${UUID.randomUUID()}",
                  upgradeId = nextStage,
                  isApplied = false,
                ),
              )
            } else {
              emptyList()
            }
          }
          PhoneUpgradeId.LLM_AGI_CORE ->
            listOf(
              InfinityChatItem.AiMessage(
                id = "llm_evo_${UUID.randomUUID()}",
                text =
                  "Local LLM has reached Autonomous AGI Core — equipped with recursive reasoning traces, " +
                    "autonomous goal decomposition, and live phone tool inspection.",
              )
            )
          else -> emptyList()
        }

      val updatedChat = baseChat + followUpItems
      saveChatItems(updatedChat)
      syncHomePlacement(evolvedState.copy(chatItems = updatedChat)).also(::persistHomePlacement)
    }
  }

  fun disableUpgrade(upgradeId: PhoneUpgradeId) {
    proactiveJob?.cancel()
    val newUpgrades = _uiState.value.activeUpgrades - upgradeId
    val newDisabled = _uiState.value.disabledUpgrades + upgradeId
    saveUpgrades(newUpgrades)
    saveDisabledUpgrades(newDisabled)

    val updatedChanges =
      _uiState.value.changeLog.map { rec ->
        val p = rec.payload
        if (p is ChangePayload.ApplyUpgrade && p.upgradeId == upgradeId) {
          rec.copy(isChecked = false)
        } else {
          rec
        }
      }
    changeLogStore.saveChanges(updatedChanges)

    _uiState.update { current ->
      val updatedChat =
        current.chatItems.map { item ->
          if (item is InfinityChatItem.SuggestedActionBlock && item.upgradeId == upgradeId) {
            item.copy(isApplied = false)
          } else {
            item
          }
        }
      saveChatItems(updatedChat)
      current.copy(
        activeUpgrades = newUpgrades,
        disabledUpgrades = newDisabled,
        changeLog = updatedChanges,
        chatItems = updatedChat,
      )
    }
  }

  private fun runIntroExplanation() {
    val snapshot = currentSnapshot()
    val plan = aiEngine.buildIntroPlan(snapshot)
    executeAiPlan(plan = plan, replyItemId = "msg_intro_explanation", isIntro = true)
  }

  private fun executeAiPlan(
    plan: AiReplyPlan,
    replyItemId: String,
    isIntro: Boolean,
  ) {
    val context = getApplication<Application>()
    streamJob?.cancel()
    streamJob =
      viewModelScope.launch {
        _uiState.update { it.copy(isAiTyping = true) }

        if (plan.autoAppliedUpgrades.isNotEmpty() ||
          plan.createAppLibrary ||
          plan.createLocalLlm ||
          plan.installLibraryApp != null ||
          plan.customAppBlueprint != null ||
          plan.toggleNightDim != null
        ) {
          applyAutonomousChanges(plan)
        }

        if (!plan.proactiveBeforeText.isNullOrBlank()) {
          val preMsg =
            InfinityChatItem.AiMessage(
              id = "proactive_before_${UUID.randomUUID()}",
              text = plan.proactiveBeforeText,
            )
          _uiState.update { current ->
            val updated = current.chatItems + preMsg
            saveChatItems(updated)
            current.copy(chatItems = updated)
          }
        }

        delay(220L)

        val streamBoost = PhoneUpgradeId.INSTANT_STREAM_BOOST in _uiState.value.activeUpgrades
        aiEngine.streamText(plan.text, instantBoost = streamBoost).collect { accumulated ->
          _uiState.update { current ->
            val withoutStreaming = current.chatItems.filterNot { it.id == replyItemId }
            current.copy(
              isAiTyping = false,
              chatItems =
                withoutStreaming +
                  InfinityChatItem.AiMessage(id = replyItemId, text = accumulated),
            )
          }
        }

        val currentUpgrades = _uiState.value.activeUpgrades
        val currentDisabled = _uiState.value.disabledUpgrades
        val suggestionBlocks =
          plan.suggestedUpgrades
            .filter { it !in currentUpgrades && it !in currentDisabled }
            .map { upgrade ->
              InfinityChatItem.SuggestedActionBlock(
                id = "suggest_${upgrade.name}_${UUID.randomUUID()}",
                upgradeId = upgrade,
                isApplied = false,
              )
            }

        if (isIntro) {
          prefs.edit().putBoolean(KEY_INTRO_FINISHED, true).apply()
        }

        _uiState.update { current ->
          val resumedBlock =
            if (isIntro && current.chatItems.none { it.id == "action_resumed_chat" }) {
              listOf(
                InfinityChatItem.ActionBlock(
                  id = "action_resumed_chat",
                  label = context.getString(R.string.infinity_ai_action_resumed_chat),
                )
              )
            } else {
              emptyList()
            }
          val updated = current.chatItems + suggestionBlocks + resumedBlock
          saveChatItems(updated)
          current.copy(
            isAiTyping = false,
            isUserChatPaused = if (isIntro) false else current.isUserChatPaused,
            chatItems = updated,
          )
        }

        if (!plan.proactiveAfterDelayText.isNullOrBlank()) {
          scheduleInformationalNote(
            text = plan.proactiveAfterDelayText,
            delayMillis = 950L,
          )
        }
      }
  }

  private fun applyAutonomousChanges(plan: AiReplyPlan) {
    val context = getApplication<Application>()
    val addedUpgrades =
      plan.autoAppliedUpgrades.filter { it !in _uiState.value.activeUpgrades }
    val newUpgrades = _uiState.value.activeUpgrades + addedUpgrades
    val newDisabled = _uiState.value.disabledUpgrades - addedUpgrades.toSet()
    if (addedUpgrades.isNotEmpty()) {
      saveUpgrades(newUpgrades)
      saveDisabledUpgrades(newDisabled)
    }

    var newCustomApps = _uiState.value.customApps
    var nextLibraryInstalled = _uiState.value.isAppLibraryInstalled
    var nextLocalLlmInstalled = _uiState.value.isLocalLlmInstalled
    var nextUninstalledKeys = _uiState.value.uninstalledAppKeys
    var nextInstalledLibraryApps = _uiState.value.installedLibraryApps
    val blueprint: CustomAppBlueprint? = plan.customAppBlueprint
    val forgedActionBlocks = mutableListOf<InfinityChatItem.ActionBlock>()
    val newChanges = mutableListOf<ChangeRecord>()
    var orderCursor = _uiState.value.changeLog.size

    if (plan.createAppLibrary && !nextLibraryInstalled) {
      nextLibraryInstalled = true
      nextUninstalledKeys = nextUninstalledKeys - "APP_LIBRARY"
      prefs.edit().putBoolean(KEY_APP_LIBRARY_INSTALLED, true).apply()
      val label = context.getString(R.string.infinity_ai_action_created_library)
      forgedActionBlocks +=
        InfinityChatItem.ActionBlock(
          id = "action_created_library_${UUID.randomUUID()}",
          label = label,
        )
      orderCursor += 1
      newChanges +=
        ChangeRecord(
          id = "chg_library_${UUID.randomUUID()}",
          orderNumber = orderCursor,
          title = label,
          detail = "12 working tools & full games added",
          timestampLabel = ChangeLogStore.currentTimeLabel(),
          isChecked = true,
          payload = ChangePayload.CreateAppLibrary,
        )
    }

    if (plan.createLocalLlm && !nextLocalLlmInstalled) {
      nextLocalLlmInstalled = true
      nextUninstalledKeys = nextUninstalledKeys - "LOCAL_LLM"
      prefs.edit().putBoolean(KEY_LOCAL_LLM_INSTALLED, true).apply()
      val label = context.getString(R.string.infinity_ai_action_created_llm)
      forgedActionBlocks +=
        InfinityChatItem.ActionBlock(
          id = "action_created_llm_${UUID.randomUUID()}",
          label = label,
        )
      orderCursor += 1
      newChanges +=
        ChangeRecord(
          id = "chg_llm_${UUID.randomUUID()}",
          orderNumber = orderCursor,
          title = label,
          detail = "Offline Local LLM assistant pinned on Home Screen",
          timestampLabel = ChangeLogStore.currentTimeLabel(),
          isChecked = true,
          payload = ChangePayload.CreateLocalLlm,
        )
    }

    val libApp = plan.installLibraryApp
    if (libApp != null && libApp !in nextInstalledLibraryApps) {
      nextInstalledLibraryApps = nextInstalledLibraryApps + libApp
      nextUninstalledKeys = nextUninstalledKeys - "LIB:${libApp.name}"
      saveLibraryApps(nextInstalledLibraryApps)
      val label = "Installed ${libApp.title} from App Library"
      forgedActionBlocks +=
        InfinityChatItem.ActionBlock(
          id = "action_install_lib_${libApp.name}_${UUID.randomUUID()}",
          label = label,
        )
      orderCursor += 1
      newChanges +=
        ChangeRecord(
          id = "chg_lib_${libApp.name}_${UUID.randomUUID()}",
          orderNumber = orderCursor,
          title = label,
          detail = libApp.tagline,
          timestampLabel = ChangeLogStore.currentTimeLabel(),
          isChecked = true,
          payload = ChangePayload.InstallLibraryApp(libApp),
        )
    }

    addedUpgrades.forEach { upgrade ->
      forgedActionBlocks +=
        InfinityChatItem.ActionBlock(
          id = "action_auto_${upgrade.name}_${UUID.randomUUID()}",
          label = upgrade.appliedActionLabel,
        )
      orderCursor += 1
      newChanges +=
        ChangeRecord(
          id = "chg_upg_${upgrade.name}_${UUID.randomUUID()}",
          orderNumber = orderCursor,
          title = upgrade.appliedActionLabel,
          detail = upgrade.subtitle,
          timestampLabel = ChangeLogStore.currentTimeLabel(),
          isChecked = true,
          payload = ChangePayload.ApplyUpgrade(upgrade),
        )
    }

    if (blueprint != null) {
      val existing = newCustomApps.any { it.name.equals(blueprint.name, ignoreCase = true) }
      if (!existing) {
        val palette = listOf(0xFFA855F7L, 0xFF00E5C3L, 0xFF38BDF8L, 0xFFF472B6L, 0xFFFBBF24L)
        val color = palette[newCustomApps.size % palette.size]
        val appId =
          blueprint.name.lowercase().replace(Regex("[^a-z0-9]"), "_") + "_" + (newCustomApps.size + 1)
        val initialShortcut =
          CustomAppShortcut(
            id = appId,
            name = blueprint.name,
            description = blueprint.description,
            archetype = blueprint.archetype,
            accentColorHex = color,
          )
        val filePath = changeLogStore.writeForgedAppFile(initialShortcut)
        val created = initialShortcut.copy(appFilePath = filePath)
        newCustomApps = newCustomApps + created
        saveCustomApps(newCustomApps)
        orderCursor += 1
        newChanges +=
          ChangeRecord(
            id = "chg_forge_${appId}_${UUID.randomUUID()}",
            orderNumber = orderCursor,
            title = "Created app: ${created.name}",
            detail = "${created.archetype.label} · ${created.description}",
            timestampLabel = ChangeLogStore.currentTimeLabel(),
            isChecked = true,
            payload = ChangePayload.ForgeCustomApp(created),
          )
      }
      forgedActionBlocks +=
        InfinityChatItem.ActionBlock(
          id = "action_forge_${UUID.randomUUID()}",
          label = "Created & saved app file: ${blueprint.name}",
        )
    }

    val nextDim = plan.toggleNightDim ?: _uiState.value.isNightDimEnabled
    if (plan.toggleNightDim != null) {
      prefs.edit().putBoolean(KEY_NIGHT_DIM, nextDim).apply()
      val label =
        if (nextDim) "Enabled Night Dim display filter" else "Restored full display brightness"
      forgedActionBlocks +=
        InfinityChatItem.ActionBlock(
          id = "action_dim_${UUID.randomUUID()}",
          label = label,
        )
      orderCursor += 1
      newChanges +=
        ChangeRecord(
          id = "chg_dim_${UUID.randomUUID()}",
          orderNumber = orderCursor,
          title = label,
          detail = "Display Night Dim = $nextDim",
          timestampLabel = ChangeLogStore.currentTimeLabel(),
          isChecked = true,
          payload = ChangePayload.SetNightDim(nextDim),
        )
    }

    saveUninstalledKeys(nextUninstalledKeys)

    _uiState.update { current ->
      val updatedChanges = (current.changeLog + newChanges).also(changeLogStore::saveChanges)
      val updatedChat =
        current.chatItems.map { item ->
          if (item is InfinityChatItem.SuggestedActionBlock && item.upgradeId in newUpgrades) {
            item.copy(isApplied = true)
          } else {
            item
          }
        } + forgedActionBlocks
      saveChatItems(updatedChat)
      syncHomePlacement(
        current.copy(
          activeUpgrades = newUpgrades,
          disabledUpgrades = newDisabled,
          isAppLibraryInstalled = nextLibraryInstalled,
          isLocalLlmInstalled = nextLocalLlmInstalled,
          uninstalledAppKeys = nextUninstalledKeys,
          installedLibraryApps = nextInstalledLibraryApps,
          customApps = newCustomApps,
          isNightDimEnabled = nextDim,
          changeLog = updatedChanges,
          chatItems = updatedChat,
        )
      ).also(::persistHomePlacement)
    }
  }

  private fun applyChangeListToState(
    updatedChanges: List<ChangeRecord>,
    explicitDisabled: Set<PhoneUpgradeId> = _uiState.value.disabledUpgrades,
    extraActionLabel: String? = null,
  ) {
    val renumbered = updatedChanges.mapIndexed { idx, rec -> rec.copy(orderNumber = idx + 1) }
    changeLogStore.saveChanges(renumbered)
    val computed = changeLogStore.recomputeState(renumbered)
    val mergedDisabled = (explicitDisabled + computed.disabledUpgrades) - computed.activeUpgrades

    prefs.edit()
      .putBoolean(KEY_PLAY_STORE_INSTALLED, computed.isPlayStoreInstalled)
      .putBoolean(KEY_APP_LIBRARY_INSTALLED, computed.isAppLibraryInstalled)
      .putBoolean(KEY_LOCAL_LLM_INSTALLED, computed.isLocalLlmInstalled)
      .putBoolean(KEY_NIGHT_DIM, computed.isNightDimEnabled)
      .apply()
    saveUpgrades(computed.activeUpgrades)
    saveDisabledUpgrades(mergedDisabled)
    saveLibraryApps(computed.installedLibraryApps)
    saveCustomApps(computed.customApps)

    _uiState.update { current ->
      val updatedChat =
        current.chatItems.map { item ->
          if (item is InfinityChatItem.SuggestedActionBlock) {
            item.copy(isApplied = item.upgradeId in computed.activeUpgrades)
          } else {
            item
          }
        } +
          if (extraActionLabel != null) {
            listOf(
              InfinityChatItem.ActionBlock(
                id = "action_rollback_${UUID.randomUUID()}",
                label = extraActionLabel,
              )
            )
          } else {
            emptyList()
          }
      saveChatItems(updatedChat)
      syncHomePlacement(
        current.copy(
          isPlayStoreInstalled = computed.isPlayStoreInstalled,
          isAppLibraryInstalled = computed.isAppLibraryInstalled,
          isLocalLlmInstalled = computed.isLocalLlmInstalled,
          installedLibraryApps = computed.installedLibraryApps,
          activeUpgrades = computed.activeUpgrades,
          disabledUpgrades = mergedDisabled,
          customApps = computed.customApps,
          isNightDimEnabled = computed.isNightDimEnabled,
          changeLog = renumbered,
          chatItems = updatedChat,
        )
      ).also(::persistHomePlacement)
    }
  }

  private fun migrateExistingStateToChangeLog(
    playStoreInstalled: Boolean,
    appLibraryInstalled: Boolean,
    localLlmInstalled: Boolean,
    upgrades: Set<PhoneUpgradeId>,
    customApps: List<CustomAppShortcut>,
    nightDim: Boolean,
  ): List<ChangeRecord> =
    buildList {
      var order = 1
      if (!playStoreInstalled) {
        add(
          ChangeRecord(
            id = "chg_mig_play",
            orderNumber = order++,
            title = "Uninstalled Play Store",
            detail = "Removed Play Store from device",
            timestampLabel = ChangeLogStore.currentTimeLabel(),
            isChecked = true,
            payload = ChangePayload.UninstallPlayStore,
          )
        )
      }
      if (appLibraryInstalled) {
        add(
          ChangeRecord(
            id = "chg_mig_lib",
            orderNumber = order++,
            title = "Created & installed App Library",
            detail = "12 working tools & full games added",
            timestampLabel = ChangeLogStore.currentTimeLabel(),
            isChecked = true,
            payload = ChangePayload.CreateAppLibrary,
          )
        )
      }
      if (localLlmInstalled) {
        add(
          ChangeRecord(
            id = "chg_mig_llm",
            orderNumber = order++,
            title = "Created & installed Local LLM",
            detail = "Offline Local LLM assistant pinned on Home Screen",
            timestampLabel = ChangeLogStore.currentTimeLabel(),
            isChecked = true,
            payload = ChangePayload.CreateLocalLlm,
          )
        )
      }
      upgrades.forEach { u ->
        add(
          ChangeRecord(
            id = "chg_mig_${u.name}",
            orderNumber = order++,
            title = u.appliedActionLabel,
            detail = u.subtitle,
            timestampLabel = ChangeLogStore.currentTimeLabel(),
            isChecked = true,
            payload = ChangePayload.ApplyUpgrade(u),
          )
        )
      }
      customApps.forEach { app ->
        add(
          ChangeRecord(
            id = "chg_mig_${app.id}",
            orderNumber = order++,
            title = "Created app: ${app.name}",
            detail = app.description,
            timestampLabel = ChangeLogStore.currentTimeLabel(),
            isChecked = true,
            payload = ChangePayload.ForgeCustomApp(app),
          )
        )
      }
      if (nightDim) {
        add(
          ChangeRecord(
            id = "chg_mig_dim",
            orderNumber = order++,
            title = "Enabled Night Dim display filter",
            detail = "Display Night Dim = true",
            timestampLabel = ChangeLogStore.currentTimeLabel(),
            isChecked = true,
            payload = ChangePayload.SetNightDim(true),
          )
        )
      }
    }

  private fun scheduleInformationalNote(
    text: String,
    delayMillis: Long,
  ) {
    proactiveJob?.cancel()
    proactiveJob =
      viewModelScope.launch {
        delay(delayMillis)
        val msg = InfinityChatItem.AiMessage(id = "proactive_${UUID.randomUUID()}", text = text)
        _uiState.update { current ->
          val updated = current.chatItems + msg
          saveChatItems(updated)
          current.copy(chatItems = updated)
        }
      }
  }

  private fun currentSnapshot(): PhoneStateSnapshot = snapshotFromState(_uiState.value)

  private fun snapshotFromState(s: HomeUiState): PhoneStateSnapshot =
    PhoneStateSnapshot(
      activeUpgrades = s.activeUpgrades,
      disabledUpgrades = s.disabledUpgrades,
      uninstalledAppKeys = s.uninstalledAppKeys,
      isAppLibraryInstalled = s.isAppLibraryInstalled,
      isLocalLlmInstalled = s.isLocalLlmInstalled,
      installedLibraryApps = s.installedLibraryApps,
      customApps = s.customApps,
      isNightDimEnabled = s.isNightDimEnabled,
      deviceStatus = deviceStatus.value,
    )

  private fun isDefaultDockKey(key: String): Boolean =
    key in setOf(KEY_APP_INFINITY_AI, KEY_APP_LOCAL_LLM, KEY_APP_LIBRARY, KEY_APP_AI_CHANGES, KEY_APP_CLOCK, KEY_APP_CALCULATOR)

  private fun syncHomePlacement(state: HomeUiState): HomeUiState {
    val hasUtility = PhoneUpgradeId.UTILITY_APPS in state.activeUpgrades
    val installedKeys =
      buildSet {
        if (state.isPlayStoreInstalled && KEY_APP_PLAY_STORE in (state.homeGridOrder + state.homeDockOrder)) {
          add(KEY_APP_PLAY_STORE)
        }
        if (state.isInfinityAiInstalled) add(KEY_APP_INFINITY_AI)
        if (state.isLocalLlmInstalled) add(KEY_APP_LOCAL_LLM)
        if (state.isBrowserInstalled) add(KEY_APP_BROWSER)
        if (state.isAppLibraryInstalled) add(KEY_APP_LIBRARY)
        if (state.changeLog.isNotEmpty()) add(KEY_APP_AI_CHANGES)
        if (hasUtility) {
          add(KEY_APP_CLOCK)
          add(KEY_APP_CALCULATOR)
          if (KEY_APP_NOTES in (state.homeGridOrder + state.homeDockOrder)) {
            add(KEY_APP_NOTES)
          }
        }
        state.installedLibraryApps.forEach { add("lib:${it.name}") }
        state.customApps.forEach { add("custom:${it.id}") }
      }

    val cleanGrid =
      state.homeGridOrder.filter { it in installedKeys && it !in state.hiddenFromHomeKeys }.toMutableList()
    val cleanDock =
      state.homeDockOrder.filter { it in installedKeys && it !in state.hiddenFromHomeKeys && it !in cleanGrid }.toMutableList()

    installedKeys.forEach { key ->
      if (key !in state.hiddenFromHomeKeys && key !in cleanGrid && key !in cleanDock) {
        if (isDefaultDockKey(key)) cleanDock.add(key)
        else cleanGrid.add(key)
      }
    }

    return state.copy(
      homeGridOrder = cleanGrid,
      homeDockOrder = cleanDock,
    )
  }

  private fun persistHomePlacement(state: HomeUiState) {
    prefs.edit()
      .putString(KEY_HOME_GRID_ORDER, HomePrefsSerializer.encodeStringList(state.homeGridOrder))
      .putString(KEY_HOME_DOCK_ORDER, HomePrefsSerializer.encodeStringList(state.homeDockOrder))
      .apply()
  }

  private fun saveHiddenFromHomeKeys(keys: Set<String>) {
    prefs.edit().putString(KEY_HIDDEN_FROM_HOME, HomePrefsSerializer.encodeStringSet(keys)).apply()
  }

  private fun saveLibraryApps(apps: Set<LibraryAppId>) {
    prefs.edit().putString(KEY_LIBRARY_APPS, HomePrefsSerializer.encodeLibraryApps(apps)).apply()
  }

  private fun saveUpgrades(upgrades: Set<PhoneUpgradeId>) {
    prefs.edit().putString(KEY_ACTIVE_UPGRADES, HomePrefsSerializer.encodeUpgrades(upgrades)).apply()
  }

  private fun saveDisabledUpgrades(upgrades: Set<PhoneUpgradeId>) {
    prefs.edit().putString(KEY_DISABLED_UPGRADES, HomePrefsSerializer.encodeUpgrades(upgrades)).apply()
  }

  private fun saveUninstalledKeys(keys: Set<String>) {
    prefs.edit().putString(KEY_UNINSTALLED_KEYS, HomePrefsSerializer.encodeStringSet(keys)).apply()
  }

  private fun saveCustomApps(apps: List<CustomAppShortcut>) {
    prefs.edit().putString(KEY_CUSTOM_APPS, HomePrefsSerializer.encodeCustomApps(apps)).apply()
  }

  private fun saveChatItems(items: List<InfinityChatItem>) {
    prefs.edit().putString(KEY_SAVED_CHAT_JSON, HomePrefsSerializer.encodeChatItems(items)).apply()
  }

  fun openSettingsMenu() {
    _uiState.update { it.copy(isSettingsMenuVisible = true) }
  }

  fun dismissSettingsMenu() {
    _uiState.update { it.copy(isSettingsMenuVisible = false) }
  }

  override fun onCleared() {
    deviceStatusMonitor.stop()
    super.onCleared()
  }

  companion object {
    const val PREFS_NAME = "home_replica_prefs"
    const val KEY_APP_INFINITY_AI = "infinity_ai"
    const val KEY_APP_LOCAL_LLM = "local_llm"
    const val KEY_APP_BROWSER = "browser"
    const val KEY_APP_LIBRARY = "app_library"
    const val KEY_APP_AI_CHANGES = "ai_changes"
    const val KEY_APP_PLAY_STORE = "play_store"
    const val KEY_APP_CLOCK = "clock"
    const val KEY_APP_CALCULATOR = "calculator"
    const val KEY_APP_NOTES = "notes"

    private const val KEY_PLAY_STORE_INSTALLED = "play_store_installed"
    private const val KEY_INFINITY_AI_INSTALLED = "infinity_ai_installed"
    private const val KEY_APP_LIBRARY_INSTALLED = "app_library_installed"
    private const val KEY_LOCAL_LLM_INSTALLED = "local_llm_installed"
    private const val KEY_BROWSER_INSTALLED = "browser_installed"
    private const val KEY_LIBRARY_APPS = "installed_library_apps_json"
    private const val KEY_INTRO_PAUSE_COMPLETED = "infinity_ai_chat_paused"
    private const val KEY_INTRO_FINISHED = "infinity_ai_intro_finished"
    private const val KEY_SAVED_CHAT_JSON = "infinity_ai_saved_chat_json"
    private const val KEY_ACTIVE_UPGRADES = "infinity_ai_active_upgrades"
    private const val KEY_DISABLED_UPGRADES = "infinity_ai_disabled_upgrades"
    private const val KEY_UNINSTALLED_KEYS = "infinity_ai_uninstalled_apps"
    private const val KEY_HOME_GRID_ORDER = "home_grid_order"
    private const val KEY_HOME_DOCK_ORDER = "home_dock_order"
    private const val KEY_HIDDEN_FROM_HOME = "home_hidden_apps"
    private const val KEY_CUSTOM_APPS = "infinity_ai_custom_apps"
    private const val KEY_NIGHT_DIM = "infinity_ai_night_dim"
    private const val KEY_NOTES_TEXT = "infinity_ai_notes_text"
  }
}
