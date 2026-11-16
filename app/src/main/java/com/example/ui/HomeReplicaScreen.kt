package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ai.LlmEvolutionTier
import com.example.ai.PhoneUpgradeId
import com.example.library.LibraryAppCategory
import com.example.ui.components.AppContextMenuDialog
import com.example.ui.components.AppLibraryAppIcon
import com.example.ui.components.AppLibraryScreen
import com.example.ui.components.BrickBreakerGameScreen
import com.example.ui.components.BrowserAppIcon
import com.example.ui.components.BrowserAppScreen
import com.example.ui.components.CalculatorMiniAppScreen
import com.example.ui.components.ChangeLogAppIcon
import com.example.ui.components.ChangeLogAppScreen
import com.example.ui.components.ChromaMatchGameScreen
import com.example.ui.components.CipherKeyAppScreen
import com.example.ui.components.ClockMiniAppScreen
import com.example.ui.components.CosmicStarfieldLayer
import com.example.ui.components.CustomForgedMiniAppScreen
import com.example.ui.components.ExpenseTrackerAppScreen
import com.example.ui.components.FocusTimerAppScreen
import com.example.ui.components.HabitForgeAppScreen
import com.example.ui.components.HomeClockWidget
import com.example.ui.components.HomeSettingsDialog
import com.example.ui.components.HorizonGlowLine
import com.example.ui.components.InfinityAiAppIcon
import com.example.ui.components.InfinityAiScreen
import com.example.ui.components.LibraryItemBadgeIcon
import com.example.ui.components.LocalLlmAppIcon
import com.example.ui.components.LocalLlmScreen
import com.example.ui.components.LockScreenOverlay
import com.example.ui.components.NeonMinesGameScreen
import com.example.ui.components.NeonSnakeGameScreen
import com.example.ui.components.NotesMiniAppScreen
import com.example.ui.components.NotificationShadeOverlay
import com.example.ui.components.PlayStoreScreen
import com.example.ui.components.PlayStoreTriangleIcon
import com.example.ui.components.Puzzle2048GameScreen
import com.example.ui.components.RecentsAppCard
import com.example.ui.components.RecentsOverviewScreen
import com.example.ui.components.ReplicaNavigationBar
import com.example.ui.components.ReplicaStatusBar
import com.example.ui.components.SplitTipAppScreen
import com.example.ui.components.TaskFlowAppScreen
import com.example.ui.components.UnitConverterAppScreen
import com.example.ui.components.UtilityAppIconBadge
import com.example.ui.components.resolveAccentColor
import java.util.Locale
import kotlinx.coroutines.launch

private enum class DrawerTab {
  ALL,
  TOOLS,
  GAMES,
  CUSTOM,
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeReplicaScreen(
  viewModel: HomeViewModel,
  onExitApp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val deviceStatus by viewModel.deviceStatus.collectAsStateWithLifecycle()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  val scope = rememberCoroutineScope()
  val haptic = LocalHapticFeedback.current
  val drawerProgress = remember { Animatable(0f) }
  var isDraggingDrawer by remember { mutableStateOf(false) }
  var drawerSearchQuery by remember { mutableStateOf("") }
  var drawerTab by remember { mutableStateOf(DrawerTab.ALL) }
  val drawerScrollState = rememberScrollState()

  val springStiffness =
    if (PhoneUpgradeId.INSTANT_TRANSITIONS in uiState.activeUpgrades) 420f else 280f
  val drawerAnimSpec =
    spring<Float>(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = springStiffness,
    )

  LaunchedEffect(uiState.isDrawerOpen, isDraggingDrawer) {
    if (!isDraggingDrawer) {
      val target = if (uiState.isDrawerOpen) 1f else 0f
      if (!uiState.isDrawerOpen) {
        drawerSearchQuery = ""
      }
      drawerProgress.animateTo(
        targetValue = target,
        animationSpec = drawerAnimSpec,
      )
    }
  }

  BackHandler(
    enabled =
      uiState.contextMenuTarget != null ||
        uiState.movingAppKey != null ||
        uiState.isSettingsMenuVisible ||
        uiState.isLockScreenVisible ||
        uiState.isNotificationShadeOpen ||
        uiState.isRecentsOpen ||
        uiState.activeAppScreen != ActiveAppScreen.NONE ||
        uiState.isDrawerOpen
  ) {
    when {
      uiState.contextMenuTarget != null -> viewModel.dismissAppContextMenu()
      uiState.movingAppKey != null -> viewModel.finishMovingApp()
      uiState.isSettingsMenuVisible -> viewModel.dismissSettingsMenu()
      uiState.isLockScreenVisible -> viewModel.unlockScreen()
      uiState.isNotificationShadeOpen -> viewModel.closeNotificationShade()
      uiState.isRecentsOpen -> viewModel.closeRecentsOverview()
      uiState.activeAppScreen != ActiveAppScreen.NONE -> viewModel.closeActiveApp()
      uiState.isDrawerOpen -> viewModel.closeDrawer()
    }
  }

  val accentColor = resolveAccentColor(uiState.activeUpgrades)
  val hasHomeWidget = PhoneUpgradeId.HOME_CLOCK_WIDGET in uiState.activeUpgrades
  val hasNotificationShade = PhoneUpgradeId.NOTIFICATION_SHADE in uiState.activeUpgrades
  val hasUtilityApps = PhoneUpgradeId.UTILITY_APPS in uiState.activeUpgrades
  val hasHomeDock = PhoneUpgradeId.HOME_DOCK in uiState.activeUpgrades
  val compactGrid = PhoneUpgradeId.COMPACT_ICON_GRID in uiState.activeUpgrades
  val bigIcons = PhoneUpgradeId.ICON_SCALE_BOOST in uiState.activeUpgrades
  val squircleIcons = PhoneUpgradeId.SQUIRCLE_ICONS in uiState.activeUpgrades
  val doubleTapDim = PhoneUpgradeId.DOUBLE_TAP_DIM in uiState.activeUpgrades
  val iconSize = if (bigIcons) 60.dp else 54.dp
  val iconRadius = if (squircleIcons) 20.dp else 11.dp

  val llmTier =
    when {
      PhoneUpgradeId.LLM_AGI_CORE in uiState.activeUpgrades -> LlmEvolutionTier.AGI_CORE
      PhoneUpgradeId.LLM_MULTIMODAL in uiState.activeUpgrades -> LlmEvolutionTier.MULTIMODAL_AI
      PhoneUpgradeId.LLM_FULL_FLEDGE in uiState.activeUpgrades -> LlmEvolutionTier.FULL_FLEDGED_LLM
      else -> LlmEvolutionTier.BASE_LLM
    }

  val strInfinityAi = stringResource(R.string.app_infinity_ai)
  val strLocalLlm = stringResource(R.string.app_local_llm)
  val strAppLibrary = stringResource(R.string.app_library)
  val strAiChanges = stringResource(R.string.app_ai_changes)
  val strPlayStore = stringResource(R.string.app_play_store)
  val strClock = stringResource(R.string.app_clock)
  val strCalculator = stringResource(R.string.app_calculator)
  val strNotes = stringResource(R.string.app_notes)
  val strBrowser = stringResource(R.string.app_browser)

  fun resolveEntry(key: String): ResolvedAppEntry? =
    resolveAppEntryForKey(
      key = key,
      uiState = uiState,
      accentColor = accentColor,
      iconSize = iconSize,
      cornerRadius = iconRadius,
      llmTier = llmTier,
      infinityLabel = strInfinityAi,
      localLlmLabel = strLocalLlm,
      appLibraryLabel = strAppLibrary,
      aiChangesLabel = strAiChanges,
      playStoreLabel = strPlayStore,
      clockLabel = strClock,
      calculatorLabel = strCalculator,
      notesLabel = strNotes,
      browserLabel = strBrowser,
      onOpenInfinityAi = viewModel::openInfinityAi,
      onOpenLocalLlm = viewModel::openLocalLlm,
      onOpenAppLibrary = viewModel::openAppLibrary,
      onOpenChangeLog = viewModel::openChangeLogApp,
      onOpenPlayStore = viewModel::openPlayStore,
      onOpenBrowser = viewModel::openBrowser,
      onOpenLibraryApp = viewModel::openLibraryApp,
      onOpenMiniApp = viewModel::openMiniApp,
    )

  fun longPressMenu(target: AppContextTarget, source: AppContextSource): () -> Unit = {
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    viewModel.openAppContextMenu(target, source)
  }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .then(
          if (PhoneUpgradeId.SYSTEM_CYBER_FRAME in uiState.activeUpgrades) {
            Modifier.border(1.5.dp, accentColor.copy(alpha = 0.65f))
          } else {
            Modifier
          }
        )
        .testTag("home_replica_root"),
  ) {
    val density = LocalDensity.current
    val screenHeightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }
    val appDrawerDesc = stringResource(R.string.cd_app_drawer)

    fun settleDrawerDrag() {
      isDraggingDrawer = false
      // User must pull halfway up (0.50f) to open / keep open the App Drawer
      val shouldOpen = drawerProgress.value >= 0.50f
      scope.launch {
        if (shouldOpen) {
          viewModel.openDrawer()
          drawerProgress.animateTo(1f, animationSpec = drawerAnimSpec)
        } else {
          viewModel.closeDrawer()
          drawerProgress.animateTo(0f, animationSpec = drawerAnimSpec)
        }
      }
    }

    // Home screen wallpaper layer + ordered home shortcuts + smooth halfway-pull swipe
    Box(
      modifier =
        Modifier.fillMaxSize()
          .graphicsLayer {
            val scale = 1f - drawerProgress.value * 0.04f
            scaleX = scale
            scaleY = scale
          }
          .pointerInput(doubleTapDim, uiState.movingAppKey) {
            detectTapGestures(
              onDoubleTap = {
                if (doubleTapDim && uiState.movingAppKey == null) viewModel.toggleNightDim()
              },
              onLongPress = {
                if (uiState.movingAppKey == null) {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  viewModel.openSettingsMenu()
                }
              },
            )
          }
          .pointerInput(screenHeightPx, hasNotificationShade, uiState.movingAppKey) {
            detectVerticalDragGestures(
              onDragStart = {
                if (uiState.movingAppKey == null) {
                  isDraggingDrawer = true
                }
              },
              onVerticalDrag = { change, dragAmount ->
                if (uiState.movingAppKey != null) return@detectVerticalDragGestures
                if (hasNotificationShade && change.position.y < 140f && dragAmount > 12f && drawerProgress.value <= 0.02f) {
                  change.consume()
                  isDraggingDrawer = false
                  viewModel.openNotificationShade()
                } else {
                  change.consume()
                  val delta = -dragAmount / screenHeightPx
                  scope.launch {
                    drawerProgress.snapTo((drawerProgress.value + delta).coerceIn(0f, 1f))
                  }
                }
              },
              onDragEnd = {
                if (uiState.movingAppKey == null) settleDrawerDrag()
              },
              onDragCancel = {
                if (uiState.movingAppKey == null) settleDrawerDrag()
              },
            )
          }
          .testTag("home_wallpaper_surface"),
    ) {
      Image(
        painter = painterResource(id = R.drawable.wallpaper_home),
        contentDescription = stringResource(R.string.cd_wallpaper),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )

      if (PhoneUpgradeId.SPLIT_TONE_WALLPAPER in uiState.activeUpgrades) {
        Box(
          modifier =
            Modifier.fillMaxSize().background(
              Brush.verticalGradient(listOf(Color(0x441A0B2E), Color(0x33051923)))
            )
        )
      }

      if (PhoneUpgradeId.ADAPTIVE_WALLPAPER_AURA in uiState.activeUpgrades) {
        Box(
          modifier =
            Modifier.fillMaxSize().background(
              Brush.radialGradient(listOf(Color.Transparent, accentColor.copy(alpha = 0.22f)))
            )
        )
      }

      if (PhoneUpgradeId.WALLPAPER_VIGNETTE in uiState.activeUpgrades) {
        Box(
          modifier =
            Modifier.fillMaxSize().background(
              Brush.verticalGradient(listOf(Color(0x44000000), Color.Transparent, Color(0x77000000)))
            )
        )
      }

      if (PhoneUpgradeId.HOME_STARFIELD in uiState.activeUpgrades) {
        CosmicStarfieldLayer(accentColor = accentColor)
      }

      // Top Move Mode Bar or Clock Widget + Greeting
      Column(
        modifier =
          Modifier.align(Alignment.TopCenter)
            .padding(top = 52.dp)
            .fillMaxWidth()
            .graphicsLayer { alpha = (1f - drawerProgress.value * 1.3f).coerceIn(0f, 1f) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        val movingKey = uiState.movingAppKey
        if (movingKey != null) {
          val movingEntry = resolveEntry(movingKey)
          HomeMoveModeBar(
            movingLabel = movingEntry?.label ?: "App",
            inDock = movingKey in uiState.homeDockOrder,
            accentColor = accentColor,
            onMoveLeft = { viewModel.moveActiveAppStep(-1) },
            onMoveRight = { viewModel.moveActiveAppStep(1) },
            onToggleContainer = viewModel::toggleActiveAppContainer,
            onDone = viewModel::finishMovingApp,
          )
        } else {
          if (hasHomeWidget) {
            HomeClockWidget(
              status = deviceStatus,
              activeUpgrades = uiState.activeUpgrades,
            )
          }

          if (PhoneUpgradeId.GREETING_BANNER in uiState.activeUpgrades) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = Color.Black.copy(alpha = 0.45f),
              border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
            ) {
              Text(
                text =
                  if (PhoneUpgradeId.CUSTOM_DEVICE_TAG in uiState.activeUpgrades) {
                    "Infinity Edition · ${uiState.activeUpgrades.size}/100 Active"
                  } else {
                    "On-Device Workspace · Long-press icons to manage"
                  },
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
              )
            }
          }
        }
      }

      // Dynamic Ordered Home Grid Shelf
      val gridEntries = uiState.homeGridOrder.mapNotNull(::resolveEntry)
      val dockEntries = uiState.homeDockOrder.mapNotNull(::resolveEntry)
      val inMoveMode = uiState.movingAppKey != null

      if (gridEntries.isNotEmpty()) {
        val shelfModifier =
          Modifier.align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 164.dp)
            .graphicsLayer { alpha = (1f - drawerProgress.value * 1.4f).coerceIn(0f, 1f) }

        val shelfContent: @Composable () -> Unit = {
          FlowRow(
            modifier =
              if (PhoneUpgradeId.HOME_FAVORITES_HEADER in uiState.activeUpgrades) {
                Modifier.fillMaxWidth().padding(10.dp)
              } else {
                Modifier.fillMaxWidth()
              },
            horizontalArrangement = Arrangement.spacedBy(if (compactGrid) 4.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactGrid) 6.dp else 10.dp),
          ) {
            gridEntries.forEach { entry ->
              AppShortcutItem(
                label = entry.label,
                onClick = {
                  if (inMoveMode) viewModel.moveOrSwapWithKey(entry.key) else entry.onOpen()
                },
                onLongClick = longPressMenu(entry.target, AppContextSource.HOME),
                showTextShadow = true,
                compact = compactGrid,
                isMovingHighlight = uiState.movingAppKey == entry.key,
                inMoveMode = inMoveMode,
                accentColor = accentColor,
                badgeCount = entry.badgeCount,
                testTag = "home_item_${entry.testTagSuffix}",
              ) {
                entry.icon()
              }
            }
          }
        }

        if (PhoneUpgradeId.HOME_FAVORITES_HEADER in uiState.activeUpgrades) {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.36f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
            modifier = shelfModifier,
          ) {
            shelfContent()
          }
        } else {
          Box(modifier = shelfModifier) { shelfContent() }
        }
      }

      if (PhoneUpgradeId.HORIZON_VISUALIZER in uiState.activeUpgrades) {
        HorizonGlowLine(
          accentColor = accentColor,
          modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 152.dp),
        )
      }

      // Ordered Home Screen Dock
      if (dockEntries.isNotEmpty() || hasHomeDock) {
        val dockModifier =
          Modifier.align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(bottom = 64.dp, start = 14.dp, end = 14.dp)
            .graphicsLayer { alpha = (1f - drawerProgress.value * 1.4f).coerceIn(0f, 1f) }

        val dockContent: @Composable () -> Unit = {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compactGrid) 2.dp else 6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            dockEntries.forEach { entry ->
              AppShortcutItem(
                label = entry.label,
                onClick = {
                  if (inMoveMode) viewModel.moveOrSwapWithKey(entry.key) else entry.onOpen()
                },
                onLongClick = longPressMenu(entry.target, AppContextSource.HOME),
                showTextShadow = true,
                compact = compactGrid,
                isMovingHighlight = uiState.movingAppKey == entry.key,
                inMoveMode = inMoveMode,
                accentColor = accentColor,
                badgeCount = entry.badgeCount,
                testTag = "home_item_${entry.testTagSuffix}",
              ) {
                entry.icon()
              }
            }
          }
        }

        val frostedDock = PhoneUpgradeId.DOCK_FROSTED_GLASS in uiState.activeUpgrades
        if (hasHomeDock || frostedDock) {
          Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = if (frostedDock) 0.56f else 0.45f),
            border =
              BorderStroke(
                1.dp,
                if (frostedDock) accentColor.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.14f),
              ),
            modifier = dockModifier.testTag("home_screen_dock"),
          ) {
            dockContent()
          }
        } else {
          Box(modifier = dockModifier) { dockContent() }
        }
      }
    }

    // Smooth halfway-pull App Drawer (with nested scroll so dragging down at top closes smoothly)
    if (drawerProgress.value > 0.005f) {
      val drawerBg =
        if (PhoneUpgradeId.DRAWER_BLUR_BACKDROP in uiState.activeUpgrades) {
          Brush.verticalGradient(listOf(Color(0xF514101C), Color(0xFA0A0B12)))
        } else {
          Brush.verticalGradient(listOf(Color(0xF41E1A1D), Color(0xF8171417)))
        }

      val drawerNestedScroll =
        remember(screenHeightPx) {
          object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
              val dy = available.y
              if (dy < 0f && drawerProgress.value < 1f) {
                isDraggingDrawer = true
                scope.launch {
                  drawerProgress.snapTo((drawerProgress.value + (-dy / screenHeightPx)).coerceIn(0f, 1f))
                }
                return available
              }
              return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
              val dy = available.y
              if (dy > 0f && drawerScrollState.value == 0) {
                isDraggingDrawer = true
                scope.launch {
                  drawerProgress.snapTo((drawerProgress.value - (dy / screenHeightPx)).coerceIn(0f, 1f))
                }
                return available
              }
              return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
              if (isDraggingDrawer) {
                settleDrawerDrag()
              }
              return super.onPostFling(consumed, available)
            }
          }
        }

      Column(
        modifier =
          Modifier.fillMaxSize()
            .graphicsLayer {
              translationY = (1f - drawerProgress.value) * screenHeightPx
              alpha = (drawerProgress.value * 1.45f).coerceIn(0f, 1f)
            }
            .background(drawerBg)
            .semantics { contentDescription = appDrawerDesc }
            .nestedScroll(drawerNestedScroll)
            .pointerInput(screenHeightPx) {
              detectVerticalDragGestures(
                onDragStart = { isDraggingDrawer = true },
                onVerticalDrag = { change, dragAmount ->
                  change.consume()
                  val delta = -dragAmount / screenHeightPx
                  scope.launch {
                    drawerProgress.snapTo((drawerProgress.value + delta).coerceIn(0f, 1f))
                  }
                },
                onDragEnd = { settleDrawerDrag() },
                onDragCancel = { settleDrawerDrag() },
              )
            }
            .padding(top = 34.dp, start = 16.dp, end = 16.dp, bottom = 58.dp)
            .testTag("app_drawer_container"),
      ) {
        // Sleek drag handle indicator
        Box(
          modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.width(42.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.28f))
          )
        }

        if (PhoneUpgradeId.DRAWER_ICON_COUNT_PILL in uiState.activeUpgrades ||
          PhoneUpgradeId.DRAWER_A_TO_Z_INDEX in uiState.activeUpgrades
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text =
                if (PhoneUpgradeId.DRAWER_A_TO_Z_INDEX in uiState.activeUpgrades) "Applications · A–Z"
                else "Applications",
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
            )
            val totalInstalled =
              (if (uiState.isPlayStoreInstalled) 1 else 0) +
                (if (uiState.isBrowserInstalled) 1 else 0) +
                (if (uiState.isInfinityAiInstalled) 1 else 0) +
                (if (uiState.isLocalLlmInstalled) 1 else 0) +
                (if (uiState.isAppLibraryInstalled) 1 else 0) +
                (if (uiState.changeLog.isNotEmpty()) 1 else 0) +
                uiState.installedLibraryApps.size +
                (if (hasUtilityApps) 3 else 0) +
                uiState.customApps.size
            Text(
              text = "$totalInstalled installed",
              color = accentColor,
              fontSize = 11.5.sp,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }

        if (PhoneUpgradeId.DRAWER_SEARCH_BAR in uiState.activeUpgrades) {
          OutlinedTextField(
            value = drawerSearchQuery,
            onValueChange = { drawerSearchQuery = it },
            singleLine = true,
            placeholder = { Text("Search installed apps…", color = Color(0xFF9CA3AF), fontSize = 13.sp) },
            shape = RoundedCornerShape(16.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF141620),
                unfocusedContainerColor = Color(0xFF141620),
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
              ),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).testTag("drawer_search_input"),
          )
        }

        if (PhoneUpgradeId.DRAWER_CATEGORY_TABS in uiState.activeUpgrades) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            DrawerTab.entries.forEach { t ->
              val active = drawerTab == t
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (active) accentColor.copy(alpha = 0.24f) else Color(0xFF151722),
                border = BorderStroke(1.dp, if (active) accentColor else Color.White.copy(alpha = 0.10f)),
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { drawerTab = t },
              ) {
                Text(
                  text = t.name,
                  color = if (active) accentColor else Color(0xFF9CA3AF),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
              }
            }
          }
        }

        val matchesQuery: (String) -> Boolean = { label ->
          drawerSearchQuery.isBlank() || label.contains(drawerSearchQuery.trim(), ignoreCase = true)
        }

        FlowRow(
          modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(drawerScrollState),
          horizontalArrangement = Arrangement.spacedBy(if (compactGrid) 6.dp else 10.dp),
          verticalArrangement = Arrangement.spacedBy(if (compactGrid) 8.dp else 14.dp),
        ) {
          if (uiState.isPlayStoreInstalled && matchesQuery("Play Store") && (drawerTab == DrawerTab.ALL || drawerTab == DrawerTab.TOOLS)) {
            AppShortcutItem(
              label = strPlayStore,
              onClick = viewModel::openPlayStore,
              onLongClick = longPressMenu(AppContextTarget.PlayStore, AppContextSource.DRAWER),
              showTextShadow = false,
              compact = compactGrid,
              testTag = "drawer_item_play_store",
            ) {
              Surface(
                shape = RoundedCornerShape(11.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.size(iconSize),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  PlayStoreTriangleIcon(modifier = Modifier.size(34.dp))
                }
              }
            }
          }

          if (uiState.isInfinityAiInstalled && matchesQuery("Infinity AI") && (drawerTab == DrawerTab.ALL || drawerTab == DrawerTab.CUSTOM)) {
            AppShortcutItem(
              label = strInfinityAi,
              onClick = viewModel::openInfinityAi,
              onLongClick = longPressMenu(AppContextTarget.InfinityAi, AppContextSource.DRAWER),
              showTextShadow = false,
              compact = compactGrid,
              testTag = "drawer_item_infinity_ai",
            ) {
              InfinityAiAppIcon(iconSize = iconSize, symbolColor = accentColor)
            }
          }

          if (uiState.isLocalLlmInstalled && matchesQuery("Local LLM") && (drawerTab == DrawerTab.ALL || drawerTab == DrawerTab.CUSTOM)) {
            AppShortcutItem(
              label = strLocalLlm,
              onClick = viewModel::openLocalLlm,
              onLongClick = longPressMenu(AppContextTarget.LocalLlm, AppContextSource.DRAWER),
              showTextShadow = false,
              compact = compactGrid,
              testTag = "drawer_item_local_llm",
            ) {
              LocalLlmAppIcon(iconSize = iconSize, evolutionTier = llmTier)
            }
          }

          if (uiState.isAppLibraryInstalled && matchesQuery("App Library") && (drawerTab == DrawerTab.ALL || drawerTab == DrawerTab.TOOLS)) {
            AppShortcutItem(
              label = strAppLibrary,
              onClick = viewModel::openAppLibrary,
              onLongClick = longPressMenu(AppContextTarget.AppLibrary, AppContextSource.DRAWER),
              showTextShadow = false,
              compact = compactGrid,
              testTag = "drawer_item_app_library",
            ) {
              AppLibraryAppIcon(iconSize = iconSize)
            }
          }

          if (uiState.isBrowserInstalled && matchesQuery("Browser") && (drawerTab == DrawerTab.ALL || drawerTab == DrawerTab.TOOLS)) {
            AppShortcutItem(
              label = strBrowser,
              onClick = viewModel::openBrowser,
              onLongClick = longPressMenu(AppContextTarget.Browser, AppContextSource.DRAWER),
              showTextShadow = false,
              compact = compactGrid,
              testTag = "drawer_item_browser",
            ) {
              BrowserAppIcon(iconSize = iconSize, cornerRadius = iconRadius)
            }
          }

          if (uiState.changeLog.isNotEmpty() && matchesQuery("AI Changes") && (drawerTab == DrawerTab.ALL || drawerTab == DrawerTab.CUSTOM)) {
            AppShortcutItem(
              label = strAiChanges,
              onClick = viewModel::openChangeLogApp,
              onLongClick = longPressMenu(AppContextTarget.AiChanges, AppContextSource.DRAWER),
              showTextShadow = false,
              compact = compactGrid,
              testTag = "drawer_item_ai_changes",
            ) {
              ChangeLogAppIcon(iconSize = iconSize)
            }
          }

          uiState.installedLibraryApps
            .filter { libApp ->
              matchesQuery(libApp.title) &&
                when (drawerTab) {
                  DrawerTab.ALL -> true
                  DrawerTab.TOOLS -> libApp.category == LibraryAppCategory.TOOL
                  DrawerTab.GAMES -> libApp.category == LibraryAppCategory.GAME
                  DrawerTab.CUSTOM -> false
                }
            }
            .forEach { libApp ->
              AppShortcutItem(
                label = libApp.title,
                onClick = { viewModel.openLibraryApp(libApp) },
                onLongClick = longPressMenu(AppContextTarget.LibraryApp(libApp), AppContextSource.DRAWER),
                showTextShadow = false,
                compact = compactGrid,
                testTag = "drawer_item_lib_${libApp.name}",
              ) {
                LibraryItemBadgeIcon(app = libApp, iconSize = iconSize)
              }
            }

          if (hasUtilityApps && (drawerTab == DrawerTab.ALL || drawerTab == DrawerTab.TOOLS)) {
            if (matchesQuery("Clock")) {
              AppShortcutItem(
                label = strClock,
                onClick = { viewModel.openMiniApp(ActiveAppScreen.CLOCK) },
                onLongClick = longPressMenu(AppContextTarget.UtilityClock, AppContextSource.DRAWER),
                showTextShadow = false,
                compact = compactGrid,
                testTag = "drawer_item_clock",
              ) {
                UtilityAppIconBadge("12", Color(0xFF141824), Color(0xFF38BDF8))
              }
            }
            if (matchesQuery("Calculator")) {
              AppShortcutItem(
                label = strCalculator,
                onClick = { viewModel.openMiniApp(ActiveAppScreen.CALCULATOR) },
                onLongClick = longPressMenu(AppContextTarget.UtilityCalculator, AppContextSource.DRAWER),
                showTextShadow = false,
                compact = compactGrid,
                testTag = "drawer_item_calculator",
              ) {
                UtilityAppIconBadge("±", Color(0xFF181424), Color(0xFFC084FC))
              }
            }
            if (matchesQuery("Notes")) {
              AppShortcutItem(
                label = strNotes,
                onClick = { viewModel.openMiniApp(ActiveAppScreen.NOTES) },
                onLongClick = longPressMenu(AppContextTarget.UtilityNotes, AppContextSource.DRAWER),
                showTextShadow = false,
                compact = compactGrid,
                testTag = "drawer_item_notes",
              ) {
                UtilityAppIconBadge("N", Color(0xFF1D1A12), Color(0xFFFBBF24))
              }
            }
          }

          if (drawerTab == DrawerTab.ALL || drawerTab == DrawerTab.CUSTOM) {
            uiState.customApps.filter { matchesQuery(it.name) }.forEach { custom ->
              AppShortcutItem(
                label = custom.name,
                onClick = { viewModel.openMiniApp(ActiveAppScreen.CUSTOM, custom.id) },
                onLongClick = longPressMenu(AppContextTarget.CustomApp(custom), AppContextSource.DRAWER),
                showTextShadow = false,
                compact = compactGrid,
                testTag = "drawer_item_custom_${custom.id}",
              ) {
                UtilityAppIconBadge(
                  label = custom.name.take(2).uppercase(Locale.US),
                  bgColor = Color(0xFF14141B),
                  accentColor = Color(custom.accentColorHex),
                )
              }
            }
          }
        }
      }
    }

    // Active app screens
    when (uiState.activeAppScreen) {
      ActiveAppScreen.PLAY_STORE ->
        PlayStoreScreen(
          selectedTab = uiState.selectedPlayTab,
          isInfinityAiInstalled = uiState.isInfinityAiInstalled,
          onSelectTab = viewModel::selectPlayTab,
          onInfinityAiClick = {
            if (!uiState.isInfinityAiInstalled) {
              viewModel.installInfinityAi()
            } else {
              viewModel.openInfinityAi()
            }
          },
        )
      ActiveAppScreen.INFINITY_AI ->
        InfinityAiScreen(
          chatItems = uiState.chatItems,
          deviceStatus = deviceStatus,
          activeUpgrades = uiState.activeUpgrades,
          disabledUpgrades = uiState.disabledUpgrades,
          changesCount = uiState.changeLog.size,
          isLocalLlmInstalled = uiState.isLocalLlmInstalled,
          selectedAiPersona = uiState.selectedAiPersona,
          customApps = uiState.customApps,
          hasCompletedIntroPause = uiState.hasCompletedIntroPause,
          isUserChatPaused = uiState.isUserChatPaused,
          isAiTyping = uiState.isAiTyping,
          onUserTappedToReply = viewModel::onUserTappedToReply,
          onSendMessage = viewModel::sendUserMessage,
          onApplySuggestedUpgrade = viewModel::applySuggestedUpgrade,
          onDisableUpgrade = viewModel::disableUpgrade,
          onOpenChangeLog = viewModel::openChangeLogApp,
          onOpenLocalLlm = viewModel::openLocalLlm,
          onSelectPersona = viewModel::selectAiPersona,
          onSaveReplyToNotes = viewModel::appendReplyToNotes,
        )
      ActiveAppScreen.LOCAL_LLM ->
        LocalLlmScreen(
          turboBoost = PhoneUpgradeId.LOCAL_LLM_TURBO in uiState.activeUpgrades,
          activeUpgrades = uiState.activeUpgrades,
          phoneContextSummary =
            "Battery ${deviceStatus.batteryLevel}%, ${uiState.installedLibraryApps.size + uiState.customApps.size} user apps, ${uiState.activeUpgrades.size}/100 upgrades",
          notesText = uiState.notesText,
          changeLogSummary =
            uiState.changeLog.joinToString("\n") { rec ->
              "#${rec.orderNumber} [${if (rec.isChecked) "x" else " "}] ${rec.title} — ${rec.detail}"
            },
          onExecuteAgiTools = viewModel::executeAgiToolActions,
        )
      ActiveAppScreen.APP_LIBRARY ->
        AppLibraryScreen(
          installedLibraryApps = uiState.installedLibraryApps,
          showCollectorBanner = PhoneUpgradeId.APP_LIBRARY_PRO_BADGE in uiState.activeUpgrades,
          onInstallApp = viewModel::installLibraryApp,
          onUninstallApp = viewModel::uninstallLibraryApp,
          onOpenApp = viewModel::openLibraryApp,
        )
      ActiveAppScreen.LIBRARY_ITEM -> {
        when (uiState.activeLibraryApp) {
          com.example.library.LibraryAppId.UNIT_CONVERTER -> UnitConverterAppScreen()
          com.example.library.LibraryAppId.FOCUS_TIMER -> FocusTimerAppScreen()
          com.example.library.LibraryAppId.TASK_FLOW -> TaskFlowAppScreen()
          com.example.library.LibraryAppId.EXPENSE_TRACKER -> ExpenseTrackerAppScreen()
          com.example.library.LibraryAppId.HABIT_FORGE -> HabitForgeAppScreen()
          com.example.library.LibraryAppId.SPLIT_TIP -> SplitTipAppScreen()
          com.example.library.LibraryAppId.CIPHER_KEY -> CipherKeyAppScreen()
          com.example.library.LibraryAppId.NEON_SNAKE -> NeonSnakeGameScreen()
          com.example.library.LibraryAppId.PUZZLE_2048 -> Puzzle2048GameScreen()
          com.example.library.LibraryAppId.BRICK_BREAKER -> BrickBreakerGameScreen()
          com.example.library.LibraryAppId.NEON_MINES -> NeonMinesGameScreen()
          com.example.library.LibraryAppId.CHROMA_MATCH -> ChromaMatchGameScreen()
          null -> Unit
        }
      }
      ActiveAppScreen.CHANGE_LOG ->
        ChangeLogAppScreen(
          changes = uiState.changeLog,
          changeLogFilePath = uiState.changeLogFilePath,
          onToggleChangeChecked = viewModel::toggleChangeChecked,
          onRollbackToIndex = viewModel::rollbackChangesTo,
        )
      ActiveAppScreen.CLOCK -> ClockMiniAppScreen()
      ActiveAppScreen.CALCULATOR -> CalculatorMiniAppScreen()
      ActiveAppScreen.NOTES ->
        NotesMiniAppScreen(
          savedNotes = uiState.notesText,
          onNotesChange = viewModel::updateNotes,
        )
      ActiveAppScreen.BROWSER ->
        BrowserAppScreen(onCloseBrowser = viewModel::closeActiveApp)
      ActiveAppScreen.CUSTOM -> {
        val customApp = uiState.customApps.find { it.id == uiState.activeCustomAppId }
        if (customApp != null) {
          CustomForgedMiniAppScreen(app = customApp)
        }
      }
      ActiveAppScreen.NONE -> Unit
    }

    // Recents Overview Switcher
    if (uiState.isRecentsOpen) {
      val cards =
        buildList {
          add(RecentsAppCard("home", "Home Screen", Color(0xFF38BDF8)))
          if (uiState.isBrowserInstalled) {
            add(RecentsAppCard("browser", "Browser", Color(0xFF38BDF8)))
          }
          if (uiState.isInfinityAiInstalled) {
            add(RecentsAppCard("infinity_ai", "Infinity AI", accentColor))
          }
          if (uiState.isLocalLlmInstalled) {
            add(RecentsAppCard("local_llm", "Local LLM", Color(0xFF22D3EE)))
          }
          if (uiState.isAppLibraryInstalled) {
            add(RecentsAppCard("app_library", "App Library", Color(0xFF38BDF8)))
          }
          if (uiState.changeLog.isNotEmpty()) {
            add(RecentsAppCard("change_log", "AI Changes", Color(0xFF2DD4BF)))
          }
          uiState.installedLibraryApps.forEach { libApp ->
            add(RecentsAppCard("lib_${libApp.name}", libApp.title, Color(libApp.accentColorHex)))
          }
          if (hasUtilityApps) {
            add(RecentsAppCard("clock", "Clock", Color(0xFF38BDF8)))
            add(RecentsAppCard("calculator", "Calculator", Color(0xFFC084FC)))
            add(RecentsAppCard("notes", "Notes", Color(0xFFFBBF24)))
          }
          uiState.customApps.forEach { custom ->
            add(RecentsAppCard("custom_${custom.id}", custom.name, Color(custom.accentColorHex)))
          }
        }.filterNot { it.id in uiState.dismissedRecentIds }

      RecentsOverviewScreen(
        cards = cards,
        showMemoryBar = PhoneUpgradeId.RECENTS_MEMORY_BAR in uiState.activeUpgrades,
        showQuickClearHeader = PhoneUpgradeId.RECENTS_QUICK_CLEAR in uiState.activeUpgrades,
        onOpenCard = viewModel::openFromRecents,
        onDismissCard = viewModel::dismissRecentCard,
        onClearAll = viewModel::clearAllRecents,
        onCloseOverview = viewModel::closeRecentsOverview,
      )
    }

    // Notification Shade Overlay
    if (uiState.isNotificationShadeOpen) {
      NotificationShadeOverlay(
        status = deviceStatus,
        isNightDimEnabled = uiState.isNightDimEnabled,
        dimAlphaLevel = uiState.dimAlphaLevel,
        isTorchOn = uiState.isTorchOn,
        soundProfileIndex = uiState.soundProfileIndex,
        isAirplaneSandbox = uiState.isAirplaneSandbox,
        isRotationLocked = uiState.isRotationLocked,
        activeUpgrades = uiState.activeUpgrades,
        forgedFilesCount = uiState.customApps.size + if (uiState.changeLog.isNotEmpty()) 1 else 0,
        appLaunchCount = uiState.appLaunchCount,
        onToggleNightDim = viewModel::toggleNightDim,
        onDimAlphaChange = viewModel::updateDimAlpha,
        onToggleTorch = viewModel::toggleTorch,
        onCycleSoundProfile = viewModel::cycleSoundProfile,
        onToggleAirplane = viewModel::toggleAirplaneSandbox,
        onToggleRotateLock = viewModel::toggleRotationLock,
        onLockScreen = viewModel::showLockScreen,
        onOpenInfinityAi = viewModel::openInfinityAi,
        onDismiss = viewModel::closeNotificationShade,
      )
    }

    // Torch Screen Overlay when enabled
    if (uiState.isTorchOn) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(Color.White.copy(alpha = 0.25f))
            .clickable { viewModel.toggleTorch() },
      )
    }

    // Night Dim / Eye-Comfort overlay when toggled on
    if (uiState.isNightDimEnabled || PhoneUpgradeId.NIGHT_OWL_REDSHIFT in uiState.activeUpgrades) {
      val filterColor =
        if (uiState.isNightDimEnabled) Color(0xFF120A05).copy(alpha = uiState.dimAlphaLevel)
        else Color(0x28FF8C22)
      Box(
        modifier = Modifier.fillMaxSize().background(filterColor).testTag("night_dim_overlay"),
      )
    }

    if (uiState.isLockScreenVisible) {
      LockScreenOverlay(
        status = deviceStatus,
        accentColor = accentColor,
        activeUpgradesCount = uiState.activeUpgrades.size,
        onUnlock = viewModel::unlockScreen,
      )
    }

    // Replicated top status bar
    ReplicaStatusBar(
      status = deviceStatus,
      activeUpgrades = uiState.activeUpgrades,
      modifier =
        Modifier.align(Alignment.TopCenter)
          .pointerInput(hasNotificationShade) {
            detectVerticalDragGestures(
              onVerticalDrag = { change, dragAmount ->
                change.consume()
                if (hasNotificationShade && dragAmount > 8f) {
                  viewModel.openNotificationShade()
                }
              }
            )
          }
          .then(
            if (hasNotificationShade) {
              Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = viewModel::openNotificationShade,
              )
            } else {
              Modifier
            }
          ),
    )

    // Replicated bottom navigation bar
    val openCountBadge =
      if (PhoneUpgradeId.MULTI_TASK_BADGE in uiState.activeUpgrades) {
        (if (uiState.isInfinityAiInstalled) 1 else 0) +
          (if (uiState.isLocalLlmInstalled) 1 else 0) +
          uiState.installedLibraryApps.size +
          uiState.customApps.size
      } else null

    ReplicaNavigationBar(
      onBackClick = {
        when {
          uiState.contextMenuTarget != null -> viewModel.dismissAppContextMenu()
          uiState.movingAppKey != null -> viewModel.finishMovingApp()
          uiState.isSettingsMenuVisible -> viewModel.dismissSettingsMenu()
          uiState.isLockScreenVisible -> viewModel.unlockScreen()
          uiState.isNotificationShadeOpen -> viewModel.closeNotificationShade()
          uiState.isRecentsOpen -> viewModel.closeRecentsOverview()
          uiState.activeAppScreen != ActiveAppScreen.NONE -> viewModel.closeActiveApp()
          uiState.isDrawerOpen || drawerProgress.value > 0f -> {
            viewModel.closeDrawer()
            scope.launch {
              drawerProgress.animateTo(0f, animationSpec = drawerAnimSpec)
            }
          }
        }
      },
      onHomeClick = {
        viewModel.dismissAppContextMenu()
        viewModel.finishMovingApp()
        viewModel.dismissSettingsMenu()
        viewModel.closeNotificationShade()
        viewModel.closeRecentsOverview()
        if (
          uiState.activeAppScreen != ActiveAppScreen.NONE ||
            uiState.isDrawerOpen ||
            drawerProgress.value > 0f
        ) {
          viewModel.closeDrawer()
          scope.launch {
            drawerProgress.animateTo(0f, animationSpec = drawerAnimSpec)
          }
        }
      },
      onRecentsClick = { viewModel.toggleRecentsOverview() },
      gesturePillMode = PhoneUpgradeId.GESTURE_PILL_NAV in uiState.activeUpgrades,
      hapticEnabled = PhoneUpgradeId.NAV_HAPTICS in uiState.activeUpgrades,
      hologramGlow = PhoneUpgradeId.HOLOGRAM_NAV_GLOW in uiState.activeUpgrades,
      accentColor = accentColor,
      taskCountBadge = openCountBadge,
      modifier = Modifier.align(Alignment.BottomCenter),
    )

    if (uiState.isSettingsMenuVisible) {
      val kernelBanner =
        if (PhoneUpgradeId.INFINITY_KERNEL_BANNER in uiState.activeUpgrades ||
          PhoneUpgradeId.OMEGA_UNIFIED_OS in uiState.activeUpgrades
        ) {
          "Infinity OS Kernel v5.0 · ${uiState.activeUpgrades.size}/100 Upgrades · ${uiState.installedLibraryApps.size + uiState.customApps.size} User Apps"
        } else null
      HomeSettingsDialog(
        kernelInfo = kernelBanner,
        onExitApp = onExitApp,
        onDismiss = viewModel::dismissSettingsMenu,
      )
    }

    val contextTarget = uiState.contextMenuTarget
    if (contextTarget != null) {
      AppContextMenuDialog(
        target = contextTarget,
        isOnHomeScreen = viewModel.isTargetOnHomeScreen(contextTarget),
        accentColor = accentColor,
        onOpenApp = { viewModel.openContextMenuTarget(contextTarget) },
        onMoveApp = { viewModel.startMovingApp(contextTarget) },
        onRemoveFromHome = { viewModel.removeFromHomeScreen(contextTarget) },
        onAddToHome = { viewModel.addToHomeScreen(contextTarget) },
        onUninstallApp = { viewModel.uninstallApp(contextTarget) },
        onDismiss = viewModel::dismissAppContextMenu,
      )
    }
  }
}
