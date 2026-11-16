package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.library.LibraryAppCategory
import com.example.library.LibraryAppId

private enum class LibraryFilter {
  ALL,
  TOOLS,
  GAMES,
}

@Composable
fun AppLibraryAppIcon(
  modifier: Modifier = Modifier,
  iconSize: Dp = 54.dp,
  cornerRadius: Dp = 11.dp,
) {
  Surface(
    shape = RoundedCornerShape(cornerRadius),
    color = Color(0xFF0A0D16),
    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f)),
    shadowElevation = 2.dp,
    modifier = modifier.size(iconSize),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize().background(
          Brush.linearGradient(
            listOf(Color(0xFF131129), Color(0xFF08141E)),
          )
        ),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.size(iconSize * 0.56f)) {
        val gap = size.width * 0.14f
        val cell = (size.width - gap) / 2f
        val r = CornerRadius(cell * 0.28f, cell * 0.28f)
        drawRoundRect(
          color = Color(0xFFA855F7),
          topLeft = Offset(0f, 0f),
          size = Size(cell, cell),
          cornerRadius = r,
        )
        drawRoundRect(
          color = Color(0xFF38BDF8),
          topLeft = Offset(cell + gap, 0f),
          size = Size(cell, cell),
          cornerRadius = r,
        )
        drawRoundRect(
          color = Color(0xFF2DD4BF),
          topLeft = Offset(0f, cell + gap),
          size = Size(cell, cell),
          cornerRadius = r,
        )
        drawRoundRect(
          color = Color(0xFFF472B6),
          topLeft = Offset(cell + gap, cell + gap),
          size = Size(cell, cell),
          cornerRadius = r,
        )
      }
    }
  }
}

@Composable
fun LibraryItemBadgeIcon(
  app: LibraryAppId,
  modifier: Modifier = Modifier,
  iconSize: Dp = 54.dp,
) {
  val accent = Color(app.accentColorHex)
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = Color(0xFF11141E),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
    shadowElevation = 2.dp,
    modifier = modifier.size(iconSize),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize().background(
          Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.24f), Color.Transparent),
          )
        ),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = app.shortCode,
        color = accent,
        fontSize = if (app.shortCode.length > 1) 17.sp else 22.sp,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
fun AppLibraryScreen(
  installedLibraryApps: Set<LibraryAppId>,
  showCollectorBanner: Boolean = false,
  onInstallApp: (LibraryAppId) -> Unit,
  onUninstallApp: (LibraryAppId) -> Unit,
  onOpenApp: (LibraryAppId) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedFilter by remember { mutableStateOf(LibraryFilter.ALL) }

  val filteredApps =
    remember(selectedFilter) {
      when (selectedFilter) {
        LibraryFilter.ALL -> LibraryAppId.entries
        LibraryFilter.TOOLS -> LibraryAppId.entries.filter { it.category == LibraryAppCategory.TOOL }
        LibraryFilter.GAMES -> LibraryAppId.entries.filter { it.category == LibraryAppCategory.GAME }
      }
    }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF07080D))
        .padding(top = 30.dp, bottom = 52.dp)
        .testTag("app_library_screen"),
  ) {
    // Top Header
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(Color(0xFF0D0F18))
          .padding(horizontal = 16.dp, vertical = 12.dp)
          .testTag("app_library_header"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        AppLibraryAppIcon(iconSize = 36.dp, cornerRadius = 9.dp)
        Column {
          Text(
            text = stringResource(R.string.app_library),
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = stringResource(R.string.app_library_subtitle),
            color = Color(0xFF38BDF8),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF171C2B),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
      ) {
        Text(
          text = "${installedLibraryApps.size}/${LibraryAppId.entries.size} live",
          color = Color(0xFFD8B4FE),
          fontSize = 11.5.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
      }
    }

    if (showCollectorBanner) {
      Surface(
        shape = RoundedCornerShape(0.dp),
        color = Color(0xFF101929),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = "Library Collector: ${installedLibraryApps.size} of ${LibraryAppId.entries.size} working offline apps installed",
          color = Color(0xFF38BDF8),
          fontSize = 11.5.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
      }
    }

    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

    // Filter Chips Row
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      LibraryFilterChip(
        label = stringResource(R.string.app_library_filter_all),
        selected = selectedFilter == LibraryFilter.ALL,
        onClick = { selectedFilter = LibraryFilter.ALL },
        testTag = "library_filter_all",
      )
      LibraryFilterChip(
        label = stringResource(R.string.app_library_filter_tools),
        selected = selectedFilter == LibraryFilter.TOOLS,
        onClick = { selectedFilter = LibraryFilter.TOOLS },
        testTag = "library_filter_tools",
      )
      LibraryFilterChip(
        label = stringResource(R.string.app_library_filter_games),
        selected = selectedFilter == LibraryFilter.GAMES,
        onClick = { selectedFilter = LibraryFilter.GAMES },
        testTag = "library_filter_games",
      )
    }

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth().testTag("app_library_list"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(filteredApps, key = { it.name }) { app ->
        val isInstalled = app in installedLibraryApps
        LibraryAppCard(
          app = app,
          isInstalled = isInstalled,
          onInstallClick = { onInstallApp(app) },
          onOpenClick = { onOpenApp(app) },
          onUninstallClick = { onUninstallApp(app) },
        )
      }
      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }
}

@Composable
private fun LibraryFilterChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = if (selected) Color(0xFF7C3AED) else Color(0xFF141722),
    border =
      BorderStroke(
        1.dp,
        if (selected) Color(0xFFA855F7) else Color.White.copy(alpha = 0.10f),
      ),
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(18.dp))
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(color = Color.White),
          role = Role.Tab,
          onClick = onClick,
        )
        .testTag(testTag),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
      Text(
        text = label,
        color = if (selected) Color.White else Color(0xFF9CA3AF),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
private fun LibraryAppCard(
  app: LibraryAppId,
  isInstalled: Boolean,
  onInstallClick: () -> Unit,
  onOpenClick: () -> Unit,
  onUninstallClick: () -> Unit,
) {
  val accent = Color(app.accentColorHex)

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = Color(0xFF11131C),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
    modifier = Modifier.fillMaxWidth().testTag("library_card_${app.name}"),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LibraryItemBadgeIcon(app = app, iconSize = 54.dp)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = app.title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = "${app.tagline} · ${app.sizeLabel}",
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
          )
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = app.description,
            color = Color(0xFF9CA3AF),
            fontSize = 12.5.sp,
            lineHeight = 16.5.sp,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (isInstalled) {
          OutlinedButton(
            onClick = onUninstallClick,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9CA3AF)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("library_remove_${app.name}"),
          ) {
            Text("Remove", fontSize = 12.5.sp)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = onOpenClick,
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("library_open_${app.name}"),
          ) {
            Text(
              text = stringResource(R.string.app_library_open),
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        } else {
          Button(
            onClick = onInstallClick,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7C3AED),
                contentColor = Color.White,
              ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("library_get_${app.name}"),
          ) {
            Text(
              text = stringResource(R.string.app_library_install),
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }
  }
}
