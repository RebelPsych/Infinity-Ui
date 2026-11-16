package com.example

import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.HomeReplicaScreen
import com.example.ui.HomeViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    WindowCompat.setDecorFitsSystemWindows(window, false)
    @Suppress("DEPRECATION")
    window.addFlags(
      WindowManager.LayoutParams.FLAG_FULLSCREEN or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      window.attributes.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
    hideSystemBarsForImmersion()
    installSystemBarSuppressionListeners()

    setContent {
      MyApplicationTheme {
        val homeViewModel: HomeViewModel = viewModel()
        HomeReplicaScreen(
          viewModel = homeViewModel,
          onExitApp = { finishAndRemoveTask() },
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
      hideSystemBarsForImmersion()
    }
  }

  override fun onResume() {
    super.onResume()
    hideSystemBarsForImmersion()
  }

  private fun installSystemBarSuppressionListeners() {
    val decorView = window.decorView
    ViewCompat.setOnApplyWindowInsetsListener(decorView) { view, windowInsets ->
      val statusVisible = windowInsets.isVisible(WindowInsetsCompat.Type.statusBars())
      val navVisible = windowInsets.isVisible(WindowInsetsCompat.Type.navigationBars())
      if (statusVisible || navVisible) {
        hideSystemBarsForImmersion()
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && view.width > 0 && view.height > 0) {
        updateGestureExclusionRects(view)
      }
      WindowInsetsCompat.CONSUMED
    }

    decorView.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && view.width > 0 && view.height > 0) {
        updateGestureExclusionRects(view)
      }
    }
  }

  private fun updateGestureExclusionRects(view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val topZoneHeight = (96f * resources.displayMetrics.density).toInt().coerceAtMost(view.height)
      val bottomZoneHeight = (72f * resources.displayMetrics.density).toInt().coerceAtMost(view.height)
      val rects =
        listOf(
          Rect(0, 0, view.width, topZoneHeight),
          Rect(0, (view.height - bottomZoneHeight).coerceAtLeast(0), view.width, view.height),
        )
      ViewCompat.setSystemGestureExclusionRects(view, rects)
    }
  }

  private fun hideSystemBarsForImmersion() {
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.hide(WindowInsetsCompat.Type.systemBars())
  }
}
