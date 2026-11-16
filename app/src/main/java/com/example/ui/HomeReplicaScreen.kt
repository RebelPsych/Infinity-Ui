package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.HomeSettingsDialog
import com.example.ui.components.InfinityAiAppIcon
import com.example.ui.components.InfinityAiScreen
import com.example.ui.components.PlayStoreScreen
import com.example.ui.components.PlayStoreTriangleIcon
import com.example.ui.components.ReplicaNavigationBar
import com.example.ui.components.ReplicaStatusBar
import kotlinx.coroutines.launch

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

  LaunchedEffect(uiState.isDrawerOpen) {
    val target = if (uiState.isDrawerOpen) 1f else 0f
    if (drawerProgress.targetValue != target) {
      drawerProgress.animateTo(
        targetValue = target,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      )
    }
  }

  BackHandler(
    enabled =
      uiState.isSettingsMenuVisible ||
        uiState.isInfinityAiOpen ||
        uiState.isPlayStoreOpen ||
        uiState.isDrawerOpen
  ) {
    when {
      uiState.isSettingsMenuVisible -> viewModel.dismissSettingsMenu()
      uiState.isInfinityAiOpen -> viewModel.closeInfinityAi()
      uiState.isPlayStoreOpen -> viewModel.closePlayStore()
      uiState.isDrawerOpen -> viewModel.closeDrawer()
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("home_replica_root")) {
    val density = LocalDensity.current
    val screenHeightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }
    val appDrawerDesc = stringResource(R.string.cd_app_drawer)

    // Home screen layer with wallpaper + installed apps + long-press + swipe-up gesture
    Box(
      modifier =
        Modifier.fillMaxSize()
          .pointerInput(Unit) {
            detectTapGestures(
              onLongPress = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.openSettingsMenu()
              }
            )
          }
          .pointerInput(screenHeightPx) {
            detectVerticalDragGestures(
              onVerticalDrag = { change, dragAmount ->
                change.consume()
                val delta = -dragAmount / screenHeightPx
                scope.launch {
                  drawerProgress.snapTo((drawerProgress.value + delta).coerceIn(0f, 1f))
                }
              },
              onDragEnd = {
                val shouldOpen = drawerProgress.value >= 0.22f
                scope.launch {
                  if (shouldOpen) {
                    viewModel.openDrawer()
                    drawerProgress.animateTo(
                      1f,
                      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    )
                  } else {
                    viewModel.closeDrawer()
                    drawerProgress.animateTo(
                      0f,
                      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    )
                  }
                }
              },
              onDragCancel = {
                val target = if (uiState.isDrawerOpen) 1f else 0f
                scope.launch {
                  drawerProgress.animateTo(
                    target,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                  )
                }
              },
            )
          }
          .testTag("home_wallpaper_surface")
    ) {
      Image(
        painter = painterResource(id = R.drawable.wallpaper_home),
        contentDescription = stringResource(R.string.cd_wallpaper),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )

      if (uiState.isInfinityAiInstalled) {
        Row(
          modifier =
            Modifier.align(Alignment.BottomCenter)
              .fillMaxWidth()
              .padding(bottom = 72.dp, start = 24.dp, end = 24.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          AppShortcutItem(
            label = stringResource(R.string.app_infinity_ai),
            onClick = viewModel::openInfinityAi,
            showTextShadow = true,
            testTag = "home_item_infinity_ai",
          ) {
            InfinityAiAppIcon()
          }
        }
      }
    }

    // App drawer revealed on swipe up
    if (drawerProgress.value > 0f) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .graphicsLayer { translationY = (1f - drawerProgress.value) * screenHeightPx }
            .background(Color(0xFF1E1A1D))
            .semantics { contentDescription = appDrawerDesc }
            .pointerInput(screenHeightPx) {
              detectVerticalDragGestures(
                onVerticalDrag = { change, dragAmount ->
                  change.consume()
                  val delta = -dragAmount / screenHeightPx
                  scope.launch {
                    drawerProgress.snapTo((drawerProgress.value + delta).coerceIn(0f, 1f))
                  }
                },
                onDragEnd = {
                  val shouldStayOpen = drawerProgress.value >= 0.78f
                  scope.launch {
                    if (shouldStayOpen) {
                      viewModel.openDrawer()
                      drawerProgress.animateTo(
                        1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                      )
                    } else {
                      viewModel.closeDrawer()
                      drawerProgress.animateTo(
                        0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                      )
                    }
                  }
                },
                onDragCancel = {
                  val target = if (uiState.isDrawerOpen) 1f else 0f
                  scope.launch {
                    drawerProgress.animateTo(
                      target,
                      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    )
                  }
                },
              )
            }
            .padding(top = 42.dp, start = 16.dp, end = 16.dp, bottom = 56.dp)
            .testTag("app_drawer_container")
      ) {
        Row(
          modifier = Modifier.align(Alignment.TopStart),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.Top,
        ) {
          if (uiState.isPlayStoreInstalled) {
            AppShortcutItem(
              label = stringResource(R.string.app_play_store),
              onClick = viewModel::openPlayStore,
              showTextShadow = false,
              testTag = "drawer_item_play_store",
            ) {
              Surface(
                shape = RoundedCornerShape(11.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.size(54.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  PlayStoreTriangleIcon(modifier = Modifier.size(34.dp))
                }
              }
            }
          }

          if (uiState.isInfinityAiInstalled) {
            AppShortcutItem(
              label = stringResource(R.string.app_infinity_ai),
              onClick = viewModel::openInfinityAi,
              showTextShadow = false,
              testTag = "drawer_item_infinity_ai",
            ) {
              InfinityAiAppIcon()
            }
          }
        }
      }
    }

    // Play Store replica screen opened from the app drawer
    if (uiState.isPlayStoreOpen) {
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
    }

    // Infinity AI basic chat window
    if (uiState.isInfinityAiOpen) {
      InfinityAiScreen(
        chatItems = uiState.chatItems,
        hasCompletedIntroPause = uiState.hasCompletedIntroPause,
        isUserChatPaused = uiState.isUserChatPaused,
        isAiTyping = uiState.isAiTyping,
        onUserTappedToReply = viewModel::onUserTappedToReply,
        onSendMessage = viewModel::sendUserMessage,
        onRetryExplanation = viewModel::retryLastAction,
      )
    }

    // Custom replicated top status bar
    ReplicaStatusBar(
      status = deviceStatus,
      modifier = Modifier.align(Alignment.TopCenter),
    )

    // Custom replicated bottom 3-button navigation bar
    ReplicaNavigationBar(
      onBackClick = {
        when {
          uiState.isSettingsMenuVisible -> viewModel.dismissSettingsMenu()
          uiState.isInfinityAiOpen -> viewModel.closeInfinityAi()
          uiState.isPlayStoreOpen -> viewModel.closePlayStore()
          uiState.isDrawerOpen || drawerProgress.value > 0f -> {
            viewModel.closeDrawer()
            scope.launch {
              drawerProgress.animateTo(
                0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
              )
            }
          }
        }
      },
      onHomeClick = {
        viewModel.dismissSettingsMenu()
        if (
          uiState.isInfinityAiOpen ||
            uiState.isPlayStoreOpen ||
            uiState.isDrawerOpen ||
            drawerProgress.value > 0f
        ) {
          viewModel.closeDrawer()
          scope.launch {
            drawerProgress.animateTo(
              0f,
              animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            )
          }
        }
      },
      onRecentsClick = {
        // Intentional no-op per step-by-step instruction; ripple provides tactile feedback
      },
      modifier = Modifier.align(Alignment.BottomCenter),
    )

    if (uiState.isSettingsMenuVisible) {
      HomeSettingsDialog(
        onExitApp = onExitApp,
        onDismiss = viewModel::dismissSettingsMenu,
      )
    }
  }
}

@Composable
private fun AppShortcutItem(
  label: String,
  onClick: () -> Unit,
  showTextShadow: Boolean,
  testTag: String,
  modifier: Modifier = Modifier,
  iconContent: @Composable () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val labelStyle =
    if (showTextShadow) {
      TextStyle(
        color = Color.White,
        fontSize = 12.sp,
        shadow =
          Shadow(
            color = Color.Black.copy(alpha = 0.80f),
            offset = Offset(0f, 1.5f),
            blurRadius = 4f,
          ),
      )
    } else {
      TextStyle(color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
    }

  Column(
    modifier =
      modifier
        .width(80.dp)
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(12.dp))
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(color = Color.White),
          role = Role.Button,
          onClick = onClick,
        )
        .padding(vertical = 6.dp, horizontal = 4.dp)
        .testTag(testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Top,
  ) {
    iconContent()
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = label,
      style = labelStyle,
      textAlign = TextAlign.Center,
      maxLines = 1,
    )
  }
}
