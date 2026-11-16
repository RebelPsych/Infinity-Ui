package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ai.ChangeRecord

@Composable
fun ChangeLogAppIcon(
  modifier: Modifier = Modifier,
  iconSize: Dp = 54.dp,
  cornerRadius: Dp = 11.dp,
) {
  Surface(
    shape = RoundedCornerShape(cornerRadius),
    color = Color(0xFF10131D),
    border = BorderStroke(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.55f)),
    shadowElevation = 2.dp,
    modifier = modifier.size(iconSize),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = Icons.Filled.Menu,
        contentDescription = null,
        tint = Color(0xFF2DD4BF),
        modifier = Modifier.size(iconSize * 0.52f),
      )
      Box(
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .padding(6.dp)
            .size(16.dp)
            .background(Color(0xFFA855F7), RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.Check,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(11.dp),
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChangeLogAppScreen(
  changes: List<ChangeRecord>,
  changeLogFilePath: String,
  onToggleChangeChecked: (String) -> Unit,
  onRollbackToIndex: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  var pendingRollbackIndex by remember { mutableStateOf<Int?>(null) }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF07090F))
        .padding(top = 30.dp, bottom = 52.dp)
        .testTag("change_log_app_screen"),
  ) {
    // Header
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(Color(0xFF0D1019))
          .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        ChangeLogAppIcon(iconSize = 36.dp, cornerRadius = 9.dp)
        Column {
          Text(
            text = stringResource(R.string.app_ai_changes),
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = stringResource(R.string.ai_changes_subtitle),
            color = Color(0xFF2DD4BF),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF151928),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
      ) {
        val checkedCount = changes.count { it.isChecked }
        Text(
          text = "$checkedCount/${changes.size} active",
          color = Color(0xFF5EEAD4),
          fontSize = 11.5.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
      }
    }

    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

    // File info banner + hint
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = Color(0xFF101420),
      border = BorderStroke(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.25f)),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
      Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
        Text(
          text = "App File: system_changes_app.json",
          color = Color(0xFFE2E8F0),
          fontSize = 12.5.sp,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = changeLogFilePath,
          color = Color(0xFF94A3B8),
          fontSize = 10.5.sp,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = stringResource(R.string.ai_changes_hint),
          color = Color(0xFFA855F7),
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Medium,
        )
      }
    }

    if (changes.isEmpty()) {
      Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
          text = stringResource(R.string.ai_changes_empty),
          color = Color(0xFF9CA3AF),
          fontSize = 14.sp,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth().testTag("change_log_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        itemsIndexed(changes, key = { _, item -> item.id }) { index, record ->
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (record.isChecked) Color(0xFF121521) else Color(0xFF0E1017),
            border =
              BorderStroke(
                1.dp,
                if (record.isChecked) Color(0xFF2DD4BF).copy(alpha = 0.35f)
                else Color.White.copy(alpha = 0.08f),
              ),
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .combinedClickable(
                  onClick = { onToggleChangeChecked(record.id) },
                  onLongClick = { pendingRollbackIndex = index },
                )
                .testTag("change_record_item_$index"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1D2234),
              ) {
                Text(
                  text = "#${index + 1}",
                  color = Color(0xFF38BDF8),
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                )
              }

              Spacer(modifier = Modifier.width(10.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = record.title,
                  color = if (record.isChecked) Color.White else Color(0xFF6B7280),
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold,
                  textDecoration =
                    if (record.isChecked) TextDecoration.None else TextDecoration.LineThrough,
                )
                if (record.detail.isNotBlank()) {
                  Text(
                    text = record.detail,
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Text(
                  text = "${record.timestampLabel} · Hold to rollback here",
                  color = Color(0xFF64748B),
                  fontSize = 10.5.sp,
                )
              }

              Checkbox(
                checked = record.isChecked,
                onCheckedChange = { onToggleChangeChecked(record.id) },
                colors =
                  CheckboxDefaults.colors(
                    checkedColor = Color(0xFF2DD4BF),
                    uncheckedColor = Color(0xFF6B7280),
                    checkmarkColor = Color.Black,
                  ),
                modifier = Modifier.minimumInteractiveComponentSize().testTag("change_checkbox_$index"),
              )
            }
          }
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
      }
    }
  }

  val rollbackIdx = pendingRollbackIndex
  if (rollbackIdx != null && rollbackIdx in changes.indices) {
    val selectedChange = changes[rollbackIdx]
    val laterCount = changes.lastIndex - rollbackIdx
    val message =
      if (laterCount > 0) {
        "Roll back the phone before the $laterCount change(s) made after \"${selectedChange.title}\" (#${rollbackIdx + 1})? All later changes will be removed."
      } else {
        "Roll back \"${selectedChange.title}\" (#${rollbackIdx + 1})? This will undo the latest change."
      }

    AlertDialog(
      onDismissRequest = { pendingRollbackIndex = null },
      containerColor = Color(0xFF141622),
      titleContentColor = Color.White,
      textContentColor = Color(0xFFD1D5DB),
      title = {
        Text(
          text = stringResource(R.string.rollback_confirm_title),
          fontWeight = FontWeight.SemiBold,
        )
      },
      text = { Text(text = message) },
      confirmButton = {
        Button(
          onClick = {
            onRollbackToIndex(rollbackIdx)
            pendingRollbackIndex = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("rollback_confirm_button"),
        ) {
          Text(stringResource(R.string.rollback_confirm_action), color = Color.White)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { pendingRollbackIndex = null },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("rollback_cancel_button"),
        ) {
          Text(stringResource(R.string.action_cancel), color = Color(0xFF9CA3AF))
        }
      },
      modifier = Modifier.testTag("rollback_confirm_dialog"),
    )
  }
}
