package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.PhoneUpgradeId
import com.example.ai.UpgradeTier

private enum class ExplorerFilter(val label: String) {
  ALL("All (100)"),
  T1("T1"),
  T2("T2"),
  T3("T3"),
  T4("T4"),
  T5("T5"),
  ACTIVE("Enabled"),
  DISABLED("Disabled"),
}

@Composable
fun UpgradeExplorerDialog(
  activeUpgrades: Set<PhoneUpgradeId>,
  disabledUpgrades: Set<PhoneUpgradeId>,
  accentColor: Color,
  onApplyUpgrade: (PhoneUpgradeId) -> Unit,
  onDisableUpgrade: (PhoneUpgradeId) -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedFilter by remember { mutableStateOf(ExplorerFilter.ALL) }
  var searchQuery by remember { mutableStateOf("") }

  val filteredList =
    remember(selectedFilter, searchQuery, activeUpgrades, disabledUpgrades) {
      val base =
        when (selectedFilter) {
          ExplorerFilter.ALL -> PhoneUpgradeId.entries
          ExplorerFilter.T1 -> PhoneUpgradeId.entries.filter { it.tier == UpgradeTier.TIER_1 }
          ExplorerFilter.T2 -> PhoneUpgradeId.entries.filter { it.tier == UpgradeTier.TIER_2 }
          ExplorerFilter.T3 -> PhoneUpgradeId.entries.filter { it.tier == UpgradeTier.TIER_3 }
          ExplorerFilter.T4 -> PhoneUpgradeId.entries.filter { it.tier == UpgradeTier.TIER_4 }
          ExplorerFilter.T5 -> PhoneUpgradeId.entries.filter { it.tier == UpgradeTier.TIER_5 }
          ExplorerFilter.ACTIVE -> PhoneUpgradeId.entries.filter { it in activeUpgrades }
          ExplorerFilter.DISABLED -> PhoneUpgradeId.entries.filter { it in disabledUpgrades }
        }
      val q = searchQuery.trim()
      if (q.isEmpty()) base
      else {
        base.filter {
          it.title.contains(q, ignoreCase = true) ||
            it.subtitle.contains(q, ignoreCase = true) ||
            it.category.badge.contains(q, ignoreCase = true) ||
            "#${it.number}".contains(q)
        }
      }
    }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = Color(0xFF0A0C14),
      border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f)),
      modifier =
        Modifier.fillMaxSize()
          .padding(horizontal = 10.dp, vertical = 28.dp)
          .testTag("upgrade_explorer_dialog"),
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(Color(0xFF101320))
              .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "100 System Upgrades",
              color = Color.White,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "${activeUpgrades.size} / ${PhoneUpgradeId.entries.size} enabled · Optional & reversible",
              color = accentColor,
              fontSize = 11.5.sp,
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("explorer_close_button"),
          ) {
            Icon(Icons.Filled.Close, contentDescription = "Close upgrades", tint = Color.White)
          }
        }

        LinearProgressIndicator(
          progress = { activeUpgrades.size.toFloat() / PhoneUpgradeId.entries.size.coerceAtLeast(1) },
          color = accentColor,
          trackColor = Color(0xFF1B2030),
          modifier = Modifier.fillMaxWidth().height(4.dp),
        )

        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            placeholder = { Text("Search 100 upgrades by name, #number, UI, AI…", color = Color(0xFF6B7280), fontSize = 13.sp) },
            shape = RoundedCornerShape(14.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF131622),
                unfocusedContainerColor = Color(0xFF131622),
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
              ),
            modifier = Modifier.fillMaxWidth().testTag("explorer_search_input"),
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            ExplorerFilter.entries.forEach { f ->
              val selected = f == selectedFilter
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (selected) accentColor.copy(alpha = 0.22f) else Color(0xFF141824),
                border = BorderStroke(1.dp, if (selected) accentColor else Color.White.copy(alpha = 0.10f)),
                modifier =
                  Modifier.clip(RoundedCornerShape(14.dp))
                    .clickable { selectedFilter = f }
                    .testTag("explorer_filter_${f.name}"),
              ) {
                Text(
                  text = f.label,
                  color = if (selected) accentColor else Color(0xFF9CA3AF),
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                )
              }
            }
          }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth().testTag("explorer_list"),
          contentPadding = PaddingValues(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(filteredList, key = { it.name }) { upgrade ->
            val isActive = upgrade in activeUpgrades
            val isDisabled = !isActive && upgrade in disabledUpgrades
            Surface(
              shape = RoundedCornerShape(14.dp),
              color =
                when {
                  isActive -> Color(0xFF0F1D18)
                  isDisabled -> Color(0xFF111218)
                  else -> Color(0xFF131624)
                },
              border =
                BorderStroke(
                  1.dp,
                  when {
                    isActive -> Color(0xFF22C55E).copy(alpha = 0.45f)
                    isDisabled -> Color.White.copy(alpha = 0.06f)
                    else -> accentColor.copy(alpha = 0.30f)
                  },
                ),
              modifier = Modifier.fillMaxWidth().testTag("explorer_item_${upgrade.number}"),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = "#${upgrade.number}",
                      color = accentColor,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                    )
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = Color.White.copy(alpha = 0.07f),
                    ) {
                      Text(
                        text = "${upgrade.tier.label.substringBefore(" ·")} · ${upgrade.category.badge}",
                        color = Color(0xFFD1D5DB),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      )
                    }
                    if (isDisabled) {
                      Text(
                        text = "Disabled",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = upgrade.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Text(
                    text = upgrade.subtitle,
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                  )
                }

                if (isActive) {
                  OutlinedButton(
                    onClick = { onDisableUpgrade(upgrade) },
                    border = BorderStroke(1.dp, Color(0xFF4ADE80).copy(alpha = 0.45f)),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("explorer_disable_${upgrade.number}"),
                  ) {
                    Text("Disable", color = Color(0xFF4ADE80), fontSize = 12.sp)
                  }
                } else {
                  Button(
                    onClick = { onApplyUpgrade(upgrade) },
                    colors =
                      ButtonDefaults.buttonColors(
                        containerColor = if (isDisabled) Color(0xFF272B3A) else accentColor,
                        contentColor = if (isDisabled) Color.White else Color.Black,
                      ),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("explorer_apply_${upgrade.number}"),
                  ) {
                    Text(
                      text = if (isDisabled) "Enable" else "Apply",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
