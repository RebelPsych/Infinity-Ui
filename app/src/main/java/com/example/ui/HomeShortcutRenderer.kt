package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ai.LlmEvolutionTier
import com.example.ai.PhoneUpgradeId
import com.example.library.LibraryAppId
import com.example.ui.components.AppLibraryAppIcon
import com.example.ui.components.BrowserAppIcon
import com.example.ui.components.ChangeLogAppIcon
import com.example.ui.components.InfinityAiAppIcon
import com.example.ui.components.LibraryItemBadgeIcon
import com.example.ui.components.LocalLlmAppIcon
import com.example.ui.components.PlayStoreTriangleIcon
import com.example.ui.components.UtilityAppIconBadge
import java.util.Locale

internal data class ResolvedAppEntry(
  val key: String,
  val label: String,
  val target: AppContextTarget,
  val badgeCount: Int? = null,
  val testTagSuffix: String,
  val onOpen: () -> Unit,
  val icon: @Composable () -> Unit,
)

internal fun resolveAppEntryForKey(
  key: String,
  uiState: HomeUiState,
  accentColor: Color,
  iconSize: Dp,
  cornerRadius: Dp,
  llmTier: LlmEvolutionTier,
  infinityLabel: String,
  localLlmLabel: String,
  appLibraryLabel: String,
  aiChangesLabel: String,
  playStoreLabel: String,
  clockLabel: String,
  calculatorLabel: String,
  notesLabel: String,
  browserLabel: String,
  onOpenInfinityAi: () -> Unit,
  onOpenLocalLlm: () -> Unit,
  onOpenAppLibrary: () -> Unit,
  onOpenChangeLog: () -> Unit,
  onOpenPlayStore: () -> Unit,
  onOpenBrowser: () -> Unit,
  onOpenLibraryApp: (LibraryAppId) -> Unit,
  onOpenMiniApp: (ActiveAppScreen, String?) -> Unit,
): ResolvedAppEntry? {
  return when {
    key == HomeViewModel.KEY_APP_INFINITY_AI && uiState.isInfinityAiInstalled ->
      ResolvedAppEntry(
        key = key,
        label = infinityLabel,
        target = AppContextTarget.InfinityAi,
        testTagSuffix = "infinity_ai",
        onOpen = onOpenInfinityAi,
        icon = {
          InfinityAiAppIcon(
            iconSize = iconSize,
            cornerRadius = cornerRadius,
            symbolColor = accentColor,
          )
        },
      )
    key == HomeViewModel.KEY_APP_LOCAL_LLM && uiState.isLocalLlmInstalled ->
      ResolvedAppEntry(
        key = key,
        label = localLlmLabel,
        target = AppContextTarget.LocalLlm,
        testTagSuffix = "local_llm",
        onOpen = onOpenLocalLlm,
        icon = {
          LocalLlmAppIcon(
            iconSize = iconSize,
            cornerRadius = cornerRadius,
            evolutionTier = llmTier,
          )
        },
      )
    key == HomeViewModel.KEY_APP_BROWSER && uiState.isBrowserInstalled ->
      ResolvedAppEntry(
        key = key,
        label = browserLabel,
        target = AppContextTarget.Browser,
        testTagSuffix = "browser",
        onOpen = onOpenBrowser,
        icon = { BrowserAppIcon(iconSize = iconSize, cornerRadius = cornerRadius) },
      )
    key == HomeViewModel.KEY_APP_LIBRARY && uiState.isAppLibraryInstalled ->
      ResolvedAppEntry(
        key = key,
        label = appLibraryLabel,
        target = AppContextTarget.AppLibrary,
        testTagSuffix = "app_library",
        onOpen = onOpenAppLibrary,
        icon = { AppLibraryAppIcon(iconSize = iconSize, cornerRadius = cornerRadius) },
      )
    key == HomeViewModel.KEY_APP_AI_CHANGES && uiState.changeLog.isNotEmpty() -> {
      val badge =
        if (PhoneUpgradeId.HOME_ICON_BADGES in uiState.activeUpgrades &&
          PhoneUpgradeId.FOCUS_MODE_SHIELD !in uiState.activeUpgrades
        ) {
          uiState.changeLog.size
        } else null
      ResolvedAppEntry(
        key = key,
        label = aiChangesLabel,
        target = AppContextTarget.AiChanges,
        badgeCount = badge,
        testTagSuffix = "ai_changes",
        onOpen = onOpenChangeLog,
        icon = { ChangeLogAppIcon(iconSize = iconSize, cornerRadius = cornerRadius) },
      )
    }
    key == HomeViewModel.KEY_APP_PLAY_STORE && uiState.isPlayStoreInstalled ->
      ResolvedAppEntry(
        key = key,
        label = playStoreLabel,
        target = AppContextTarget.PlayStore,
        testTagSuffix = "play_store",
        onOpen = onOpenPlayStore,
        icon = {
          Surface(
            shape = RoundedCornerShape(cornerRadius),
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.size(iconSize),
          ) {
            Box(contentAlignment = Alignment.Center) {
              PlayStoreTriangleIcon(modifier = Modifier.size(34.dp))
            }
          }
        },
      )
    key == HomeViewModel.KEY_APP_CLOCK && PhoneUpgradeId.UTILITY_APPS in uiState.activeUpgrades ->
      ResolvedAppEntry(
        key = key,
        label = clockLabel,
        target = AppContextTarget.UtilityClock,
        testTagSuffix = "clock",
        onOpen = { onOpenMiniApp(ActiveAppScreen.CLOCK, null) },
        icon = { UtilityAppIconBadge("12", Color(0xFF141824), Color(0xFF38BDF8)) },
      )
    key == HomeViewModel.KEY_APP_CALCULATOR && PhoneUpgradeId.UTILITY_APPS in uiState.activeUpgrades ->
      ResolvedAppEntry(
        key = key,
        label = calculatorLabel,
        target = AppContextTarget.UtilityCalculator,
        testTagSuffix = "calculator",
        onOpen = { onOpenMiniApp(ActiveAppScreen.CALCULATOR, null) },
        icon = { UtilityAppIconBadge("±", Color(0xFF181424), Color(0xFFC084FC)) },
      )
    key == HomeViewModel.KEY_APP_NOTES && PhoneUpgradeId.UTILITY_APPS in uiState.activeUpgrades ->
      ResolvedAppEntry(
        key = key,
        label = notesLabel,
        target = AppContextTarget.UtilityNotes,
        testTagSuffix = "notes",
        onOpen = { onOpenMiniApp(ActiveAppScreen.NOTES, null) },
        icon = { UtilityAppIconBadge("N", Color(0xFF1D1A12), Color(0xFFFBBF24)) },
      )
    key.startsWith("lib:") -> {
      val libName = key.removePrefix("lib:")
      val libApp = uiState.installedLibraryApps.find { it.name == libName } ?: return null
      ResolvedAppEntry(
        key = key,
        label = libApp.title,
        target = AppContextTarget.LibraryApp(libApp),
        testTagSuffix = "lib_${libApp.name}",
        onOpen = { onOpenLibraryApp(libApp) },
        icon = { LibraryItemBadgeIcon(app = libApp, iconSize = iconSize) },
      )
    }
    key.startsWith("custom:") -> {
      val customId = key.removePrefix("custom:")
      val custom = uiState.customApps.find { it.id == customId } ?: return null
      ResolvedAppEntry(
        key = key,
        label = custom.name,
        target = AppContextTarget.CustomApp(custom),
        testTagSuffix = "custom_${custom.id}",
        onOpen = { onOpenMiniApp(ActiveAppScreen.CUSTOM, custom.id) },
        icon = {
          UtilityAppIconBadge(
            label = custom.name.take(2).uppercase(Locale.US),
            bgColor = Color(0xFF14141B),
            accentColor = Color(custom.accentColorHex),
          )
        },
      )
    }
    else -> null
  }
}

@Composable
internal fun HomeMoveModeBar(
  movingLabel: String,
  inDock: Boolean,
  accentColor: Color,
  onMoveLeft: () -> Unit,
  onMoveRight: () -> Unit,
  onToggleContainer: () -> Unit,
  onDone: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = Color(0xEE101420),
    border = BorderStroke(1.2.dp, accentColor),
    shadowElevation = 8.dp,
    modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp).testTag("home_move_bar"),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Moving: $movingLabel",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Tap any app to swap, or nudge below",
            color = Color(0xFF9CA3AF),
            fontSize = 11.5.sp,
          )
        }
        Button(
          onClick = onDone,
          colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("move_app_done"),
        ) {
          Text("Done", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onMoveLeft,
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("move_app_left"),
        ) {
          Text("← Left", color = Color.White, fontSize = 12.sp)
        }
        OutlinedButton(
          onClick = onMoveRight,
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("move_app_right"),
        ) {
          Text("Right →", color = Color.White, fontSize = 12.sp)
        }
        OutlinedButton(
          onClick = onToggleContainer,
          border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier.weight(1.1f).minimumInteractiveComponentSize().testTag("move_app_container"),
        ) {
          Text(
            text = if (inDock) "To Grid" else "To Dock",
            color = accentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppShortcutItem(
  label: String,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  showTextShadow: Boolean,
  compact: Boolean = false,
  isMovingHighlight: Boolean = false,
  inMoveMode: Boolean = false,
  accentColor: Color = Color(0xFFA855F7),
  badgeCount: Int? = null,
  testTag: String,
  modifier: Modifier = Modifier,
  iconContent: @Composable () -> Unit,
) {
  val labelStyle =
    if (showTextShadow) {
      TextStyle(
        color = Color.White,
        fontSize = 11.5.sp,
        shadow =
          Shadow(
            color = Color.Black.copy(alpha = 0.80f),
            offset = Offset(0f, 1.5f),
            blurRadius = 4f,
          ),
      )
    } else {
      TextStyle(color = Color.White.copy(alpha = 0.92f), fontSize = 11.5.sp)
    }

  val containerModifier =
    modifier
      .width(if (compact) 72.dp else 78.dp)
      .minimumInteractiveComponentSize()
      .clip(RoundedCornerShape(14.dp))
      .then(
        when {
          isMovingHighlight ->
            Modifier.background(accentColor.copy(alpha = 0.22f))
              .border(1.5.dp, accentColor, RoundedCornerShape(14.dp))
          inMoveMode ->
            Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
          else -> Modifier
        }
      )
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      )
      .padding(vertical = 5.dp, horizontal = 3.dp)
      .testTag(testTag)

  Column(
    modifier = containerModifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Top,
  ) {
    Box {
      iconContent()
      if (isMovingHighlight) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = accentColor,
          modifier = Modifier.align(Alignment.TopCenter),
        ) {
          Text(
            text = "MOVE",
            color = Color.Black,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
          )
        }
      } else if (badgeCount != null && badgeCount > 0) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFEF4444),
          modifier = Modifier.align(Alignment.TopEnd),
        ) {
          Text(
            text = badgeCount.coerceAtMost(99).toString(),
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(5.dp))
    Text(
      text = label,
      style = labelStyle,
      textAlign = TextAlign.Center,
      maxLines = 1,
    )
  }
}
