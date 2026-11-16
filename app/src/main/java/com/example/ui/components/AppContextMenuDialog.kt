package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.AppContextTarget

@Composable
fun AppContextMenuDialog(
  target: AppContextTarget,
  isOnHomeScreen: Boolean,
  accentColor: Color,
  onOpenApp: () -> Unit,
  onMoveApp: () -> Unit,
  onRemoveFromHome: () -> Unit,
  onAddToHome: () -> Unit,
  onUninstallApp: () -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = Color(0xFF131620),
      border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
      tonalElevation = 8.dp,
      modifier = Modifier.fillMaxWidth().testTag("app_context_menu"),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Column {
          Text(
            text = target.label,
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = target.categoryLabel,
            color = accentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
          )
        }

        OutlinedButton(
          onClick = onOpenApp,
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
          shape = RoundedCornerShape(14.dp),
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .height(48.dp)
              .testTag("context_menu_open"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.app_menu_open),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
          )
        }

        OutlinedButton(
          onClick = onMoveApp,
          border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
          shape = RoundedCornerShape(14.dp),
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .height(48.dp)
              .testTag("context_menu_move"),
        ) {
          Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.app_menu_move),
            color = accentColor,
            fontWeight = FontWeight.SemiBold,
          )
        }

        if (isOnHomeScreen) {
          OutlinedButton(
            onClick = onRemoveFromHome,
            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.55f)),
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .height(48.dp)
                .testTag("context_menu_remove_home"),
          ) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = null,
              tint = Color(0xFFFBBF24),
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.app_menu_remove_home),
              color = Color(0xFFFBBF24),
              fontWeight = FontWeight.SemiBold,
            )
          }
        } else {
          OutlinedButton(
            onClick = onAddToHome,
            border = BorderStroke(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.55f)),
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .height(48.dp)
                .testTag("context_menu_add_home"),
          ) {
            Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = null,
              tint = Color(0xFF2DD4BF),
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.app_menu_add_home),
              color = Color(0xFF2DD4BF),
              fontWeight = FontWeight.SemiBold,
            )
          }
        }

        if (target.canUninstall) {
          Button(
            onClick = onUninstallApp,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFFDC2626),
                contentColor = Color.White,
              ),
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .height(48.dp)
                .testTag("context_menu_uninstall"),
          ) {
            Icon(
              imageVector = Icons.Filled.Delete,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.app_menu_uninstall),
              fontWeight = FontWeight.Bold,
            )
          }
        } else {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1C2030),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = stringResource(R.string.app_menu_system_core),
              color = Color(0xFF9CA3AF),
              fontSize = 12.sp,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
          }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("context_menu_cancel"),
          ) {
            Text(stringResource(R.string.action_cancel), color = Color(0xFF9CA3AF))
          }
        }
      }
    }
  }
}
