package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.PlayStoreTab

private val PlayBottomBarColor = Color(0xFF1E1F21)
private val PlayActivePillColor = Color(0xFF004B79)

@Composable
fun PlayStoreScreen(
  selectedTab: PlayStoreTab,
  isInfinityAiInstalled: Boolean,
  onSelectTab: (PlayStoreTab) -> Unit,
  onInfinityAiClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val logoDesc = stringResource(R.string.cd_play_store_logo)
  val notifDesc = stringResource(R.string.cd_play_notifications)
  val profileDesc = stringResource(R.string.cd_play_profile)

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black)
        .padding(top = 30.dp, bottom = 52.dp)
        .testTag("play_store_screen"),
  ) {
    // Play Store top app bar (Play logo left, Bell + Smiley profile avatar right)
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(54.dp)
          .background(Color(0xFF131313))
          .padding(horizontal = 16.dp)
          .testTag("play_store_top_bar"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      PlayStoreTriangleIcon(
        modifier =
          Modifier.size(26.dp)
            .semantics { contentDescription = logoDesc }
            .testTag("play_store_top_logo"),
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        PlayBellOutlineIcon(
          modifier =
            Modifier.semantics { contentDescription = notifDesc }
              .testTag("play_store_notifications_icon"),
        )

        SmileyProfileAvatar(
          modifier =
            Modifier.semantics { contentDescription = profileDesc }
              .testTag("play_store_profile_avatar"),
        )
      }
    }

    // Black Play Store body with "Suggested for you" + Infinity AI listing
    Column(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .background(Color.Black)
          .padding(horizontal = 20.dp, vertical = 20.dp)
          .testTag("play_store_body"),
    ) {
      Text(
        text = stringResource(R.string.play_suggested_for_you),
        color = Color(0xFFE3E3E3),
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.testTag("play_suggested_header"),
      )

      Spacer(modifier = Modifier.height(16.dp))

      InfinityAiStoreListing(
        isInstalled = isInfinityAiInstalled,
        onClick = onInfinityAiClick,
      )
    }

    // Play Store bottom navigation bar with tappable tab highlight
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(68.dp)
          .background(PlayBottomBarColor)
          .padding(horizontal = 6.dp)
          .testTag("play_store_bottom_bar"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceAround,
    ) {
      PlayBottomNavItem(
        label = stringResource(R.string.play_tab_games),
        selected = selectedTab == PlayStoreTab.GAMES,
        onClick = { onSelectTab(PlayStoreTab.GAMES) },
        testTag = "play_tab_games",
      ) { tint ->
        GamesTabIcon(
          tint = tint,
          cutoutColor =
            if (selectedTab == PlayStoreTab.GAMES) PlayActivePillColor else PlayBottomBarColor,
        )
      }
      PlayBottomNavItem(
        label = stringResource(R.string.play_tab_apps),
        selected = selectedTab == PlayStoreTab.APPS,
        onClick = { onSelectTab(PlayStoreTab.APPS) },
        testTag = "play_tab_apps",
      ) { tint ->
        AppsTabIcon(tint = tint)
      }
      PlayBottomNavItem(
        label = stringResource(R.string.play_tab_search),
        selected = selectedTab == PlayStoreTab.SEARCH,
        onClick = { onSelectTab(PlayStoreTab.SEARCH) },
        testTag = "play_tab_search",
      ) { tint ->
        SearchTabIcon(tint = tint)
      }
      PlayBottomNavItem(
        label = stringResource(R.string.play_tab_books),
        selected = selectedTab == PlayStoreTab.BOOKS,
        onClick = { onSelectTab(PlayStoreTab.BOOKS) },
        testTag = "play_tab_books",
      ) { tint ->
        BooksTabIcon(tint = tint)
      }
      PlayBottomNavItem(
        label = stringResource(R.string.play_tab_you),
        selected = selectedTab == PlayStoreTab.YOU,
        onClick = { onSelectTab(PlayStoreTab.YOU) },
        testTag = "play_tab_you",
      ) { tint ->
        YouTabIcon(tint = tint)
      }
    }
  }
}

@Composable
private fun InfinityAiStoreListing(
  isInstalled: Boolean,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(14.dp))
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(color = Color.White),
          role = Role.Button,
          onClick = onClick,
        )
        .padding(vertical = 8.dp, horizontal = 4.dp)
        .testTag("play_store_infinity_ai_item"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    InfinityAiAppIcon(iconSize = 58.dp, cornerRadius = 12.dp)

    Spacer(modifier = Modifier.width(16.dp))

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(
        text = stringResource(R.string.app_infinity_ai),
        color = Color(0xFFF1F3F4),
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        maxLines = 1,
      )

      Text(
        text = stringResource(R.string.infinity_ai_category),
        color = Color(0xFFBDC1C6),
        fontSize = 13.sp,
        maxLines = 1,
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier =
            Modifier.background(Color(0xFF23262C), RoundedCornerShape(6.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Text(
            text = stringResource(R.string.infinity_ai_rating),
            color = Color(0xFFE3E3E3),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
          )
          SmallStarIcon(color = Color(0xFF4C8DF6), modifier = Modifier.size(10.dp))
        }

        Text(
          text = stringResource(R.string.infinity_ai_size),
          color = Color(0xFFBDC1C6),
          fontSize = 12.5.sp,
        )

        if (isInstalled) {
          Text(
            text = stringResource(R.string.infinity_ai_installed),
            color = Color(0xFF8AB4F8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.testTag("play_store_infinity_ai_installed_badge"),
          )
        }
      }
    }
  }
}

@Composable
private fun SmallStarIcon(
  color: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val star =
      Path().apply {
        moveTo(w * 0.50f, h * 0.04f)
        lineTo(w * 0.63f, h * 0.36f)
        lineTo(w * 0.98f, h * 0.38f)
        lineTo(w * 0.71f, h * 0.60f)
        lineTo(w * 0.80f, h * 0.94f)
        lineTo(w * 0.50f, h * 0.74f)
        lineTo(w * 0.20f, h * 0.94f)
        lineTo(w * 0.29f, h * 0.60f)
        lineTo(w * 0.02f, h * 0.38f)
        lineTo(w * 0.37f, h * 0.36f)
        close()
      }
    drawPath(star, color = color)
  }
}

@Composable
private fun PlayBottomNavItem(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
  icon: @Composable (Color) -> Unit,
) {
  val iconTint = if (selected) Color(0xFFC2E7FF) else Color(0xFFC4C7C5)
  val labelColor = if (selected) Color(0xFF8AB4F8) else Color(0xFFC4C7C5)
  val interactionSource = remember { MutableInteractionSource() }

  Column(
    modifier =
      Modifier.width(64.dp)
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(14.dp))
        .semantics { this.selected = selected }
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(bounded = true, color = Color.White),
          role = Role.Tab,
          onClick = onClick,
        )
        .padding(vertical = 6.dp)
        .testTag(testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(
      modifier =
        Modifier.width(56.dp)
          .height(30.dp)
          .then(
            if (selected) {
              Modifier.background(PlayActivePillColor, RoundedCornerShape(15.dp))
            } else {
              Modifier
            }
          ),
      contentAlignment = Alignment.Center,
    ) {
      icon(iconTint)
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = label,
      color = labelColor,
      fontSize = 11.5.sp,
      fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
      maxLines = 1,
    )
  }
}
