package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.InfinityChatItem

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
        modifier = Modifier.size(width = iconSize * 0.72f, height = iconSize * 0.44f),
        strokeWidth = (iconSize.value * 0.068f).dp,
      )
    }
  }
}

@Composable
fun InfinityAiScreen(
  chatItems: List<InfinityChatItem>,
  hasCompletedIntroPause: Boolean,
  isUserChatPaused: Boolean,
  isAiTyping: Boolean,
  onUserTappedToReply: () -> Unit,
  onSendMessage: (String) -> Unit,
  onRetryExplanation: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val logoDesc = stringResource(R.string.cd_infinity_ai_logo)
  val listState = rememberLazyListState()
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  var draftReply by remember { mutableStateOf("") }

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

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black)
        .padding(top = 30.dp, bottom = 52.dp)
        .imePadding()
        .testTag("infinity_ai_screen"),
  ) {
    // Top chat header
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(54.dp)
          .background(Color(0xFF0D0D12))
          .padding(horizontal = 16.dp)
          .testTag("infinity_ai_header"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      InfinityAiAppIcon(iconSize = 34.dp, cornerRadius = 9.dp)

      Column(verticalArrangement = Arrangement.Center) {
        Text(
          text = stringResource(R.string.app_infinity_ai),
          color = Color.White,
          fontSize = 16.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.semantics { contentDescription = logoDesc },
        )
        Text(
          text = stringResource(R.string.infinity_ai_subtitle),
          color = Color(0xFFA855F7),
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Medium,
        )
      }
    }

    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

    // Chat timeline with Action Blocks, AI messages, User messages, and error recovery
    LazyColumn(
      state = listState,
      modifier = Modifier.weight(1f).fillMaxWidth().testTag("infinity_ai_chat_list"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(chatItems, key = { it.id }) { item ->
        when (item) {
          is InfinityChatItem.ActionBlock -> ActionBlockCard(label = item.label)
          is InfinityChatItem.AiMessage -> AiMessageBubble(text = item.text)
          is InfinityChatItem.UserMessage -> UserMessageBubble(text = item.text)
          is InfinityChatItem.ErrorMessage ->
            AiErrorBubble(message = item.message, onRetry = onRetryExplanation)
        }
      }

      if (isAiTyping) {
        item(key = "ai_typing_indicator") {
          AiTypingBubble()
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
            focusedBorderColor = Color(0xFFA855F7),
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
            containerColor = Color(0xFF9333EA),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF2C2D35),
            disabledContentColor = Color(0xFF6B7280),
          ),
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .size(48.dp)
            .background(
              color = if (sendEnabled) Color(0xFF9333EA) else Color(0xFF2C2D35),
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
}

@Composable
private fun ActionBlockCard(label: String) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = Color(0xFF14101D),
    border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.50f)),
    modifier = Modifier.fillMaxWidth().testTag("infinity_action_block"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      ActionBoltIcon(
        color = Color(0xFFC084FC),
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
private fun AiMessageBubble(text: String) {
  Row(
    modifier = Modifier.fillMaxWidth().testTag("infinity_ai_message"),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.Start,
  ) {
    InfinityAiAppIcon(iconSize = 28.dp, cornerRadius = 8.dp)
    Spacer(modifier = Modifier.width(10.dp))

    Surface(
      shape =
        RoundedCornerShape(
          topStart = 4.dp,
          topEnd = 18.dp,
          bottomStart = 18.dp,
          bottomEnd = 18.dp,
        ),
      color = Color(0xFF181820),
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
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
  }
}

@Composable
private fun UserMessageBubble(text: String) {
  Row(
    modifier = Modifier.fillMaxWidth().testTag("infinity_user_message"),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.End,
  ) {
    Surface(
      shape =
        RoundedCornerShape(
          topStart = 18.dp,
          topEnd = 4.dp,
          bottomStart = 18.dp,
          bottomEnd = 18.dp,
        ),
      color = Color(0xFF6D28D9),
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
  }
}

@Composable
private fun AiErrorBubble(
  message: String,
  onRetry: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().testTag("infinity_ai_error"),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.Start,
  ) {
    InfinityAiAppIcon(iconSize = 28.dp, cornerRadius = 8.dp)
    Spacer(modifier = Modifier.width(10.dp))

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color(0xFF281316),
      border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f)),
      modifier = Modifier.widthIn(max = 300.dp),
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(
          text = message,
          color = Color(0xFFFCA5A5),
          fontSize = 13.5.sp,
          lineHeight = 18.sp,
        )
        TextButton(
          onClick = onRetry,
          contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
          modifier = Modifier.testTag("infinity_ai_retry_button"),
        ) {
          Text(
            text = stringResource(R.string.action_retry),
            color = Color(0xFFFECACA),
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

@Composable
private fun AiTypingBubble() {
  Row(
    modifier = Modifier.fillMaxWidth().testTag("infinity_ai_typing"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    InfinityAiAppIcon(iconSize = 28.dp, cornerRadius = 8.dp)
    Spacer(modifier = Modifier.width(10.dp))
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color(0xFF181820),
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
      Text(
        text = "•••",
        color = Color(0xFFC084FC),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
      )
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
