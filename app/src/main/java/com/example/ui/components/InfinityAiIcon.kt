package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ai.CustomAppShortcut
import com.example.ai.PhoneUpgradeId
import com.example.ai.UpgradeCategory
import com.example.system.DeviceStatus
import com.example.ui.InfinityChatItem
import kotlinx.coroutines.launch

@Composable
fun InfinitySymbol(
  modifier: Modifier = Modifier,
  color: Color = Color(0xFFA855F7),
  strokeWidth: Dp = 3.5.dp,
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val cx = w * 0.50f
    val cy = h * 0.50f
    val edgeX = w * 0.14f
    val rightEdgeX = w * 0.86f
    val loopCtrlX1 = w * 0.34f
    val loopCtrlX2 = w * 0.66f
    val topCtrlY = h * 0.14f
    val bottomCtrlY = h * 0.86f

    val infinityPath =
      Path().apply {
        moveTo(cx, cy)
        cubicTo(loopCtrlX2, topCtrlY, rightEdgeX, topCtrlY, rightEdgeX, cy)
        cubicTo(rightEdgeX, bottomCtrlY, loopCtrlX2, bottomCtrlY, cx, cy)
        cubicTo(loopCtrlX1, topCtrlY, edgeX, topCtrlY, edgeX, cy)
        cubicTo(edgeX, bottomCtrlY, loopCtrlX1, bottomCtrlY, cx, cy)
        close()
      }

    drawPath(
      path = infinityPath,
      color = color,
      style =
        Stroke(
          width = strokeWidth.toPx(),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round,
        ),
    )
  }
}

@Composable
fun InfinityAiAppIcon(
  modifier: Modifier = Modifier,
  iconSize: Dp = 54.dp,
  cornerRadius: Dp = 11.dp,
  symbolColor: Color = Color(0xFFA855F7),
) {
  Surface(
    shape = RoundedCornerShape(cornerRadius),
    color = Color.Black,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    shadowElevation = 2.dp,
    modifier = modifier.size(iconSize),
  ) {
    Box(contentAlignment = Alignment.Center) {
      InfinitySymbol(
        color = symbolColor,
        modifier = Modifier.size(width = iconSize * 0.72f, height = iconSize * 0.44f),
        strokeWidth = (iconSize.value * 0.068f).dp,
      )
    }
  }
}

@Composable
fun InfinityAiScreen(
  chatItems: List<InfinityChatItem>,
  deviceStatus: DeviceStatus,
  activeUpgrades: Set<PhoneUpgradeId>,
  disabledUpgrades: Set<PhoneUpgradeId>,
  changesCount: Int,
  isLocalLlmInstalled: Boolean,
  selectedAiPersona: String,
  customApps: List<CustomAppShortcut>,
  hasCompletedIntroPause: Boolean,
  isUserChatPaused: Boolean,
  isAiTyping: Boolean,
  onUserTappedToReply: () -> Unit,
  onSendMessage: (String) -> Unit,
  onApplySuggestedUpgrade: (PhoneUpgradeId) -> Unit,
  onDisableUpgrade: (PhoneUpgradeId) -> Unit,
  onOpenChangeLog: () -> Unit,
  onOpenLocalLlm: () -> Unit,
  onSelectPersona: (String) -> Unit,
  onSaveReplyToNotes: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val logoDesc = stringResource(R.string.cd_infinity_ai_logo)
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  var draftReply by remember { mutableStateOf("") }
  var showUpgradeExplorer by remember { mutableStateOf(false) }
  var copiedToast by remember { mutableStateOf(false) }

  val accentColor = resolveAccentColor(activeUpgrades)
  val isOled = PhoneUpgradeId.OLED_OBSIDIAN_MODE in activeUpgrades
  val isPlasmaHeader = PhoneUpgradeId.DUAL_TONE_PLASMA in activeUpgrades
  val isCompact = PhoneUpgradeId.AI_COMPACT_MODE in activeUpgrades
  val isSoftBubbles = PhoneUpgradeId.CHAT_BUBBLE_RADIUS_SOFT in activeUpgrades
  val showQuickPrompts = PhoneUpgradeId.QUICK_PROMPT_CHIPS in activeUpgrades
  val showExportBtn = PhoneUpgradeId.EXPORT_CHAT_TRANSCRIPT in activeUpgrades
  val showMemoryBadge = PhoneUpgradeId.AI_MEMORY_COUNTER in activeUpgrades
  val showPersonaRow = PhoneUpgradeId.MULTI_PERSONA_SWITCH in activeUpgrades
  val showScrollFab = PhoneUpgradeId.SCROLL_TO_BOTTOM_FAB in activeUpgrades
  val showForgeTemplates = PhoneUpgradeId.PINNED_COMMAND_FAVORITES in activeUpgrades
  val showWaveform = PhoneUpgradeId.WAVEFORM_THINKING in activeUpgrades
  val showTimestamps = PhoneUpgradeId.TIMESTAMPED_CHATS in activeUpgrades
  val showLlmBridge = PhoneUpgradeId.LOCAL_LLM_QUICK_SUMMON in activeUpgrades && isLocalLlmInstalled
  val showSaveNotes = PhoneUpgradeId.SMART_CLIPBOARD_NOTES in activeUpgrades

  val showJumpButton by remember {
    derivedStateOf {
      val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
      chatItems.isNotEmpty() && lastVisible < chatItems.lastIndex - 1
    }
  }

  LaunchedEffect(isUserChatPaused) {
    if (isUserChatPaused) {
      focusManager.clearFocus(force = true)
      keyboardController?.hide()
    }
  }

  LaunchedEffect(chatItems.size, isAiTyping) {
    val totalCount = chatItems.size + if (isAiTyping) 1 else 0
    if (totalCount > 0) {
      listState.animateScrollToItem(totalCount - 1)
    }
  }

  fun submitDraft() {
    if (!hasCompletedIntroPause) {
      focusManager.clearFocus(force = true)
      keyboardController?.hide()
      onUserTappedToReply()
    } else if (!isUserChatPaused && !isAiTyping && draftReply.isNotBlank()) {
      val message = draftReply
      draftReply = ""
      onSendMessage(message)
    }
  }

  val headerBrush =
    if (isPlasmaHeader) {
      Brush.horizontalGradient(listOf(Color(0xFF140D24), Color(0xFF0E1826), Color(0xFF0D0D12)))
    } else {
      Brush.horizontalGradient(listOf(Color(0xFF0D0D12), Color(0xFF0D0D12)))
    }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(if (isOled) Color.Black else Color(0xFF030408))
        .padding(top = 30.dp, bottom = 52.dp)
        .imePadding()
        .testTag("infinity_ai_screen"),
  ) {
    // Top chat header
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(headerBrush)
          .padding(horizontal = 12.dp, vertical = 8.dp)
          .testTag("infinity_ai_header"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f),
      ) {
        InfinityAiAppIcon(iconSize = 32.dp, cornerRadius = 9.dp, symbolColor = accentColor)

        Column(verticalArrangement = Arrangement.Center) {
          Text(
            text = stringResource(R.string.app_infinity_ai),
            color = Color.White,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { contentDescription = logoDesc },
          )
          val subText =
            if (PhoneUpgradeId.AI_STATUS_SUBTITLE_LIVE in activeUpgrades) {
              "${activeUpgrades.size}/100 Active · ${customApps.size} Built"
            } else if (showMemoryBadge) {
              "Standalone · ${chatItems.size} turns"
            } else {
              stringResource(R.string.infinity_ai_subtitle)
            }
          Text(
            text = subText,
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xFF19132B),
          border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
          modifier =
            Modifier.clip(RoundedCornerShape(10.dp))
              .clickable { showUpgradeExplorer = true }
              .testTag("infinity_header_upgrades_button"),
        ) {
          Text(
            text = "Upgrades ${activeUpgrades.size}/100",
            color = accentColor,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
          )
        }

        if (changesCount > 0) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0E1D21),
            border = BorderStroke(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.55f)),
            modifier =
              Modifier.clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenChangeLog)
                .testTag("infinity_header_changes_button"),
          ) {
            Text(
              text = "Changes ($changesCount)",
              color = Color(0xFF5EEAD4),
              fontSize = 10.5.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            )
          }
        }
      }
    }

    // Optional persona / export / Local LLM bridge strip
    if (showPersonaRow || showExportBtn || showLlmBridge) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF0D0F18))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (showPersonaRow) {
          listOf("Balanced", "Concise", "Technical", "Creative").forEach { persona ->
            val selected = persona == selectedAiPersona
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (selected) accentColor.copy(alpha = 0.22f) else Color(0xFF171A26),
              border = BorderStroke(1.dp, if (selected) accentColor else Color.White.copy(alpha = 0.08f)),
              modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onSelectPersona(persona) },
            ) {
              Text(
                text = persona,
                color = if (selected) accentColor else Color(0xFF9CA3AF),
                fontSize = 10.5.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
        }
        if (showLlmBridge) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0F202E),
            border = BorderStroke(1.dp, Color(0xFF22D3EE).copy(alpha = 0.6f)),
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onOpenLocalLlm),
          ) {
            Text(
              text = "Open Local LLM",
              color = Color(0xFF22D3EE),
              fontSize = 10.5.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
        if (showExportBtn) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF171A26),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
            modifier =
              Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val logText =
                  chatItems.joinToString("\n\n") {
                    when (it) {
                      is InfinityChatItem.UserMessage -> "User: ${it.text}"
                      is InfinityChatItem.AiMessage -> "Infinity AI: ${it.text}"
                      is InfinityChatItem.ActionBlock -> "[Action] ${it.label}"
                      is InfinityChatItem.SuggestedActionBlock -> "[Suggestion] ${it.upgradeId.title}"
                    }
                  }
                cm?.setPrimaryClip(ClipData.newPlainText("Infinity Chat", logText))
                copiedToast = true
              },
          ) {
            Text(
              text = if (copiedToast) "Copied Log!" else "Copy Chat",
              color = if (copiedToast) Color(0xFF4ADE80) else Color.White,
              fontSize = 10.5.sp,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
      }
    }

    // Optional live telemetry bar unlocked via capability action block
    if (PhoneUpgradeId.TELEMETRY_BAR in activeUpgrades) {
      val netStatus =
        when {
          deviceStatus.isWifiConnected -> "Wi-Fi"
          deviceStatus.isCellularConnected -> "LTE"
          else -> "Offline"
        }
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF14111E))
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .testTag("infinity_telemetry_bar"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "PWR ${deviceStatus.batteryLevel}%${if (deviceStatus.isCharging) " ⚡" else ""}",
          color = accentColor,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = "NET $netStatus",
          color = Color(0xFF5EEAD4),
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
        )
        val tokText =
          if (PhoneUpgradeId.TOKEN_LATENCY_BADGE in activeUpgrades) "82 tok/s · ${activeUpgrades.size}/100"
          else "MODS ${activeUpgrades.size}/${PhoneUpgradeId.entries.size}"
        Text(
          text = tokText,
          color = Color(0xFFE5E7EB),
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

    // Chat timeline
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag("infinity_ai_chat_list"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = if (isCompact) 8.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 7.dp else 11.dp),
      ) {
        itemsIndexed(chatItems, key = { _, item -> item.id }) { index, item ->
          when (item) {
            is InfinityChatItem.ActionBlock ->
              ActionBlockCard(label = item.label, accentColor = accentColor, compact = isCompact)
            is InfinityChatItem.SuggestedActionBlock ->
              TappableSuggestedActionBlock(
                upgradeId = item.upgradeId,
                isApplied = item.isApplied,
                accentOverride = accentColor,
                onClick = { onApplySuggestedUpgrade(item.upgradeId) },
              )
            is InfinityChatItem.AiMessage ->
              AiMessageBubble(
                text = item.text,
                turnLabel = if (showTimestamps) "AI · #${index + 1}" else null,
                accentColor = accentColor,
                softCorner = isSoftBubbles,
                showSaveNotes = showSaveNotes,
                onSaveToNotes = { onSaveReplyToNotes(item.text) },
              )
            is InfinityChatItem.UserMessage ->
              UserMessageBubble(
                text = item.text,
                turnLabel = if (showTimestamps) "You · #${index + 1}" else null,
                accentColor = accentColor,
                softCorner = isSoftBubbles,
              )
          }
        }

        if (isAiTyping) {
          item(key = "ai_typing_indicator") {
            AiTypingBubble(accentColor = accentColor, waveform = showWaveform)
          }
        }
      }

      if (showScrollFab && showJumpButton) {
        Surface(
          shape = CircleShape,
          color = accentColor,
          shadowElevation = 6.dp,
          modifier =
            Modifier.align(Alignment.BottomEnd)
              .padding(14.dp)
              .size(40.dp)
              .clickable {
                scope.launch {
                  if (chatItems.isNotEmpty()) listState.animateScrollToItem(chatItems.lastIndex)
                }
              },
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Jump to latest", tint = Color.Black)
          }
        }
      }
    }

    // Quick prompt ribbon / Forge templates when unlocked
    if ((showQuickPrompts || showForgeTemplates) && hasCompletedIntroPause && !isUserChatPaused) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(Color(0xFF0A0B11))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (showQuickPrompts) {
          listOf("System status", "Battery?", "Show App Library", "Suggest 2 upgrades").forEach { q ->
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = Color(0xFF151722),
              border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
              modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable { onSendMessage(q) },
            ) {
              Text(
                text = q,
                color = Color(0xFFE5E7EB),
                fontSize = 11.5.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
          }
        }
        if (showForgeTemplates) {
          listOf("Make a habit tracker app", "Create an expense budget app", "Build a sketch pad app").forEach { tpl ->
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = Color(0xFF111C1B),
              border = BorderStroke(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.4f)),
              modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable { onSendMessage(tpl) },
            ) {
              Text(
                text = tpl,
                color = Color(0xFF5EEAD4),
                fontSize = 11.5.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
          }
        }
      }
    }

    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

    // Bottom reply input bar
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(Color(0xFF0D0D12))
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .testTag("infinity_ai_reply_bar"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      val hintText =
        when {
          isUserChatPaused -> stringResource(R.string.infinity_ai_reply_paused_hint)
          !hasCompletedIntroPause -> stringResource(R.string.infinity_ai_reply_hint)
          else -> stringResource(R.string.infinity_ai_active_hint)
        }

      OutlinedTextField(
        value = draftReply,
        onValueChange = { draftReply = it },
        enabled = !isUserChatPaused,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { submitDraft() }),
        placeholder = {
          Text(
            text = hintText,
            color = if (isUserChatPaused) Color(0xFF6B7280) else Color(0xFF9CA3AF),
            fontSize = 14.sp,
          )
        },
        shape = RoundedCornerShape(24.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF17171F),
            unfocusedContainerColor = Color(0xFF17171F),
            disabledContainerColor = Color(0xFF1F2026),
            focusedBorderColor = accentColor,
            unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
            disabledBorderColor = Color(0xFF343640),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            disabledTextColor = Color(0xFF6B7280),
          ),
        modifier =
          Modifier.weight(1f)
            .onFocusChanged { focusState ->
              if (focusState.isFocused && !hasCompletedIntroPause && !isUserChatPaused) {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                onUserTappedToReply()
              }
            }
            .testTag("infinity_ai_reply_input"),
      )

      val sendEnabled =
        !isUserChatPaused &&
          (!hasCompletedIntroPause || (!isAiTyping && draftReply.isNotBlank()))

      IconButton(
        onClick = { submitDraft() },
        enabled = sendEnabled,
        colors =
          IconButtonDefaults.iconButtonColors(
            containerColor = accentColor,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF2C2D35),
            disabledContentColor = Color(0xFF6B7280),
          ),
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .size(48.dp)
            .background(
              color = if (sendEnabled) accentColor else Color(0xFF2C2D35),
              shape = CircleShape,
            )
            .testTag("infinity_ai_send_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = stringResource(R.string.infinity_ai_send_cd),
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }

  if (showUpgradeExplorer) {
    UpgradeExplorerDialog(
      activeUpgrades = activeUpgrades,
      disabledUpgrades = disabledUpgrades,
      accentColor = accentColor,
      onApplyUpgrade = onApplySuggestedUpgrade,
      onDisableUpgrade = onDisableUpgrade,
      onDismiss = { showUpgradeExplorer = false },
    )
  }
}

@Composable
private fun ActionBlockCard(label: String, accentColor: Color, compact: Boolean) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = Color(0xFF14101D),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.50f)),
    modifier = Modifier.fillMaxWidth().testTag("infinity_action_block"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = if (compact) 7.dp else 11.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      ActionBoltIcon(
        color = accentColor,
        modifier = Modifier.size(16.dp),
      )
      Text(
        text = label,
        color = Color(0xFFE9D5FF),
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}

@Composable
private fun TappableSuggestedActionBlock(
  upgradeId: PhoneUpgradeId,
  isApplied: Boolean,
  accentOverride: Color,
  onClick: () -> Unit,
) {
  val isRealism = upgradeId.category == UpgradeCategory.REALISM
  val accentColor = if (isRealism) Color(0xFF2DD4BF) else accentOverride
  val badgeLabel = "#${upgradeId.number} · ${upgradeId.category.badge}"
  val interactionSource = remember { MutableInteractionSource() }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (isApplied) Color(0xFF111816) else Color(0xFF171324),
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (isApplied) Color(0xFF22C55E).copy(alpha = 0.45f)
          else accentColor.copy(alpha = 0.65f),
      ),
    modifier =
      Modifier.fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(14.dp))
        .clickable(
          enabled = !isApplied,
          interactionSource = interactionSource,
          indication = ripple(color = accentColor),
          role = Role.Button,
          onClick = onClick,
        )
        .testTag("tappable_action_${upgradeId.name}"),
  ) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = accentColor.copy(alpha = 0.16f),
        ) {
          Text(
            text = badgeLabel,
            color = accentColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
          )
        }

        if (isApplied) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Check,
              contentDescription = null,
              tint = Color(0xFF4ADE80),
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = stringResource(R.string.applied_badge),
              color = Color(0xFF4ADE80),
              fontSize = 11.5.sp,
              fontWeight = FontWeight.SemiBold,
            )
          }
        } else {
          Text(
            text = stringResource(R.string.tap_to_apply),
            color = accentColor,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = upgradeId.title,
        color = Color.White,
        fontSize = 14.5.sp,
        fontWeight = FontWeight.SemiBold,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = upgradeId.subtitle,
        color = Color(0xFF9CA3AF),
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
      )
    }
  }
}

@Composable
private fun AiMessageBubble(
  text: String,
  turnLabel: String?,
  accentColor: Color,
  softCorner: Boolean,
  showSaveNotes: Boolean,
  onSaveToNotes: () -> Unit,
) {
  var savedFlag by remember(text) { mutableStateOf(false) }
  val radius = if (softCorner) 22.dp else 18.dp
  Row(
    modifier = Modifier.fillMaxWidth().testTag("infinity_ai_message"),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.Start,
  ) {
    InfinityAiAppIcon(iconSize = 28.dp, cornerRadius = 8.dp, symbolColor = accentColor)
    Spacer(modifier = Modifier.width(10.dp))

    Column(horizontalAlignment = Alignment.Start) {
      Surface(
        shape =
          RoundedCornerShape(
            topStart = 4.dp,
            topEnd = radius,
            bottomStart = radius,
            bottomEnd = radius,
          ),
        color = Color(0xFF181820),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.14f)),
        modifier = Modifier.widthIn(max = 300.dp),
      ) {
        Text(
          text = text,
          color = Color(0xFFF3F4F6),
          fontSize = 14.5.sp,
          lineHeight = 20.sp,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
        )
      }
      if (turnLabel != null || showSaveNotes) {
        Row(
          modifier = Modifier.padding(top = 3.dp, start = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (turnLabel != null) {
            Text(text = turnLabel, color = Color(0xFF64748B), fontSize = 10.sp)
          }
          if (showSaveNotes) {
            Text(
              text = if (savedFlag) "Saved to Notes" else "Save to Notes",
              color = if (savedFlag) Color(0xFF4ADE80) else accentColor,
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold,
              modifier =
                Modifier.clickable {
                  onSaveToNotes()
                  savedFlag = true
                },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun UserMessageBubble(
  text: String,
  turnLabel: String?,
  accentColor: Color,
  softCorner: Boolean,
) {
  val radius = if (softCorner) 22.dp else 18.dp
  Column(
    modifier = Modifier.fillMaxWidth().testTag("infinity_user_message"),
    horizontalAlignment = Alignment.End,
  ) {
    Surface(
      shape =
        RoundedCornerShape(
          topStart = radius,
          topEnd = 4.dp,
          bottomStart = radius,
          bottomEnd = radius,
        ),
      color = accentColor.copy(alpha = 0.85f),
      modifier = Modifier.widthIn(max = 280.dp),
    ) {
      Text(
        text = text,
        color = Color.White,
        fontSize = 14.5.sp,
        lineHeight = 20.sp,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      )
    }
    if (turnLabel != null) {
      Text(text = turnLabel, color = Color(0xFF64748B), fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp, end = 4.dp))
    }
  }
}

@Composable
private fun AiTypingBubble(accentColor: Color, waveform: Boolean) {
  Row(
    modifier = Modifier.fillMaxWidth().testTag("infinity_ai_typing"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    InfinityAiAppIcon(iconSize = 28.dp, cornerRadius = 8.dp, symbolColor = accentColor)
    Spacer(modifier = Modifier.width(10.dp))
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color(0xFF181820),
      border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
    ) {
      if (waveform) {
        val transition = rememberInfiniteTransition(label = "wave")
        val pulse by transition.animateFloat(
          initialValue = 0.35f,
          targetValue = 1f,
          animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
          label = "bar",
        )
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          listOf(10f, 16f, 12f, 18f).forEachIndexed { idx, base ->
            val factor = if (idx % 2 == 0) pulse else (1.35f - pulse)
            Box(
              modifier =
                Modifier.width(3.dp)
                  .height((base * factor).dp)
                  .background(accentColor, RoundedCornerShape(2.dp)),
            )
          }
        }
      } else {
        Text(
          text = "•••",
          color = accentColor,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
      }
    }
  }
}

@Composable
private fun ActionBoltIcon(
  color: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val bolt =
      Path().apply {
        moveTo(w * 0.58f, h * 0.06f)
        lineTo(w * 0.22f, h * 0.54f)
        lineTo(w * 0.48f, h * 0.54f)
        lineTo(w * 0.40f, h * 0.94f)
        lineTo(w * 0.78f, h * 0.46f)
        lineTo(w * 0.52f, h * 0.46f)
        close()
      }
    drawPath(path = bolt, color = color)
  }
}
