package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.ChangeLogStore
import com.example.ai.CustomAppArchetype
import com.example.ai.PhoneUpgradeId
import com.example.library.LibraryAppId
import com.example.ui.ActiveAppScreen
import com.example.ui.AppContextTarget
import com.example.ui.HomeViewModel
import com.example.ui.InfinityChatItem
import com.example.ui.PlayStoreTab
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `app name matches Home Screen and catalogs contain 100 upgrades and 12 library apps`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Home Screen", appName)
    assertEquals(100, PhoneUpgradeId.entries.size)
    assertEquals((1..100).toList(), PhoneUpgradeId.entries.map { it.number }.sorted())
    assertEquals(12, LibraryAppId.entries.size)
  }

  @Test
  fun `standalone infinity ai creates Local LLM and App Library early, supports long-press uninstall, and never pushes disabled features`() =
    runTest(testDispatcher) {
      val app = ApplicationProvider.getApplicationContext<Application>()
      app.getSharedPreferences(HomeViewModel.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
      ChangeLogStore(app).clearAllFiles()

      val viewModel = HomeViewModel(app)

      viewModel.openDrawer()
      viewModel.openPlayStore()
      viewModel.selectPlayTab(PlayStoreTab.APPS)
      viewModel.installInfinityAi()

      // Opening Infinity AI uninstalls Play Store & records Change #1
      viewModel.openInfinityAi()
      assertEquals(ActiveAppScreen.INFINITY_AI, viewModel.uiState.value.activeAppScreen)
      assertFalse(viewModel.uiState.value.isPlayStoreInstalled)
      assertTrue(File(viewModel.uiState.value.changeLogFilePath).exists())

      // Intro completes and creates both App Library & Local LLM on the Home Screen
      viewModel.onUserTappedToReply()
      assertTrue(viewModel.uiState.value.isUserChatPaused)
      advanceTimeBy(4000L)
      advanceUntilIdle()
      assertFalse(viewModel.uiState.value.isUserChatPaused)
      assertTrue(viewModel.uiState.value.isAppLibraryInstalled)
      assertTrue(viewModel.uiState.value.isLocalLlmInstalled)

      // Opening Local LLM works directly
      viewModel.openLocalLlm()
      assertEquals(ActiveAppScreen.LOCAL_LLM, viewModel.uiState.value.activeAppScreen)

      // Open App Library and install everyday tool & game
      viewModel.openAppLibrary()
      viewModel.installLibraryApp(LibraryAppId.EXPENSE_TRACKER)
      viewModel.installLibraryApp(LibraryAppId.NEON_MINES)
      assertTrue(LibraryAppId.EXPENSE_TRACKER in viewModel.uiState.value.installedLibraryApps)
      assertTrue(LibraryAppId.NEON_MINES in viewModel.uiState.value.installedLibraryApps)

      // Apply Home Clock Widget upgrade, then disable it -> Infinity AI must respect disabled state
      viewModel.openInfinityAi()
      viewModel.applySuggestedUpgrade(PhoneUpgradeId.HOME_CLOCK_WIDGET)
      assertTrue(PhoneUpgradeId.HOME_CLOCK_WIDGET in viewModel.uiState.value.activeUpgrades)
      viewModel.disableUpgrade(PhoneUpgradeId.HOME_CLOCK_WIDGET)
      assertFalse(PhoneUpgradeId.HOME_CLOCK_WIDGET in viewModel.uiState.value.activeUpgrades)
      assertTrue(PhoneUpgradeId.HOME_CLOCK_WIDGET in viewModel.uiState.value.disabledUpgrades)

      // Asking for suggestions must NOT push the disabled Home Clock Widget
      viewModel.sendUserMessage("Suggest 2 upgrades")
      advanceTimeBy(3000L)
      advanceUntilIdle()
      val newSuggestions =
        viewModel.uiState.value.chatItems
          .filterIsInstance<InfinityChatItem.SuggestedActionBlock>()
          .takeLast(2)
          .map { it.upgradeId }
      assertFalse(PhoneUpgradeId.HOME_CLOCK_WIDGET in newSuggestions)

      // User describes an app and Infinity AI builds & writes JSON app file
      viewModel.sendUserMessage("Make an app that tracks my daily water intake")
      advanceTimeBy(4000L)
      advanceUntilIdle()
      val waterApp = viewModel.uiState.value.customApps.find { it.name == "Water Tracker" }
      assertTrue(waterApp != null)
      assertEquals(CustomAppArchetype.COUNTER_TRACKER, waterApp!!.archetype)
      assertTrue(File(waterApp.appFilePath).exists())

      // Long-press context menu uninstalls custom app and deletes its file
      viewModel.openAppContextMenu(AppContextTarget.CustomApp(waterApp))
      assertEquals(AppContextTarget.CustomApp(waterApp), viewModel.uiState.value.contextMenuTarget)
      viewModel.uninstallApp(AppContextTarget.CustomApp(waterApp))
      assertTrue(viewModel.uiState.value.customApps.isEmpty())
      assertFalse(File(waterApp.appFilePath).exists())

      // Long-press uninstall removes Local LLM and reopening Infinity AI does NOT force it back
      viewModel.openAppContextMenu(AppContextTarget.LocalLlm)
      viewModel.uninstallApp(AppContextTarget.LocalLlm)
      assertFalse(viewModel.uiState.value.isLocalLlmInstalled)
      viewModel.openInfinityAi()
      advanceTimeBy(1500L)
      advanceUntilIdle()
      assertFalse(viewModel.uiState.value.isLocalLlmInstalled)

      // Ordered change log rollback test
      viewModel.sendUserMessage("Create an app called SketchBook for drawing")
      advanceTimeBy(4000L)
      advanceUntilIdle()
      val sketchApp = viewModel.uiState.value.customApps.find { it.name == "SketchBook" }
      assertTrue(sketchApp != null)
      val firstChangeIdx = 0
      viewModel.rollbackChangesTo(firstChangeIdx)
      assertTrue(viewModel.uiState.value.customApps.isEmpty())
      assertFalse(File(sketchApp!!.appFilePath).exists())
    }

  @Test
  fun `llm evolution path suggests full llm then multimodal then agi, drawer closes on app open, and move or remove from home works`() =
    runTest(testDispatcher) {
      val app = ApplicationProvider.getApplicationContext<Application>()
      app.getSharedPreferences(HomeViewModel.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
      ChangeLogStore(app).clearAllFiles()

      val viewModel = HomeViewModel(app)
      viewModel.openDrawer()
      assertTrue(viewModel.uiState.value.isDrawerOpen)
      viewModel.openPlayStore()
      assertFalse(viewModel.uiState.value.isDrawerOpen)

      viewModel.installInfinityAi()
      viewModel.openInfinityAi()
      viewModel.onUserTappedToReply()
      advanceTimeBy(4000L)
      advanceUntilIdle()

      // Intro suggests LLM_FULL_FLEDGE first; applying it suggests LLM_MULTIMODAL; applying that suggests LLM_AGI_CORE
      val introSuggested =
        viewModel.uiState.value.chatItems
          .filterIsInstance<InfinityChatItem.SuggestedActionBlock>()
          .map { it.upgradeId }
      assertTrue(PhoneUpgradeId.LLM_FULL_FLEDGE in introSuggested)

      viewModel.applySuggestedUpgrade(PhoneUpgradeId.LLM_FULL_FLEDGE)
      assertTrue(PhoneUpgradeId.LLM_FULL_FLEDGE in viewModel.uiState.value.activeUpgrades)
      val afterFull =
        viewModel.uiState.value.chatItems
          .filterIsInstance<InfinityChatItem.SuggestedActionBlock>()
          .map { it.upgradeId }
      assertTrue(PhoneUpgradeId.LLM_MULTIMODAL in afterFull)

      viewModel.applySuggestedUpgrade(PhoneUpgradeId.LLM_MULTIMODAL)
      assertTrue(PhoneUpgradeId.LLM_MULTIMODAL in viewModel.uiState.value.activeUpgrades)
      val afterMulti =
        viewModel.uiState.value.chatItems
          .filterIsInstance<InfinityChatItem.SuggestedActionBlock>()
          .map { it.upgradeId }
      assertTrue(PhoneUpgradeId.LLM_AGI_CORE in afterMulti)

      viewModel.applySuggestedUpgrade(PhoneUpgradeId.LLM_AGI_CORE)
      assertTrue(PhoneUpgradeId.LLM_AGI_CORE in viewModel.uiState.value.activeUpgrades)

      // Opening any app from an open App Drawer closes the drawer
      viewModel.openDrawer()
      assertTrue(viewModel.uiState.value.isDrawerOpen)
      viewModel.openLocalLlm()
      assertFalse(viewModel.uiState.value.isDrawerOpen)
      assertEquals(ActiveAppScreen.LOCAL_LLM, viewModel.uiState.value.activeAppScreen)

      // Move app on Home Screen (reorder / move between Dock and Grid)
      viewModel.closeActiveApp()
      assertTrue(HomeViewModel.KEY_APP_LOCAL_LLM in viewModel.uiState.value.homeDockOrder)
      viewModel.startMovingApp(AppContextTarget.LocalLlm)
      assertEquals(HomeViewModel.KEY_APP_LOCAL_LLM, viewModel.uiState.value.movingAppKey)
      viewModel.toggleActiveAppContainer()
      assertTrue(HomeViewModel.KEY_APP_LOCAL_LLM in viewModel.uiState.value.homeGridOrder)
      assertFalse(HomeViewModel.KEY_APP_LOCAL_LLM in viewModel.uiState.value.homeDockOrder)
      viewModel.finishMovingApp()
      assertEquals(null, viewModel.uiState.value.movingAppKey)

      // Remove from Home Screen removes from Home Grid/Dock ONLY while keeping app installed in App Drawer
      viewModel.removeFromHomeScreen(AppContextTarget.LocalLlm)
      assertFalse(viewModel.isTargetOnHomeScreen(AppContextTarget.LocalLlm))
      assertTrue(viewModel.uiState.value.isLocalLlmInstalled)

      // Add back to Home Screen from Drawer restores it cleanly
      viewModel.addToHomeScreen(AppContextTarget.LocalLlm)
      assertTrue(viewModel.isTargetOnHomeScreen(AppContextTarget.LocalLlm))
    }

  @Test
  fun `local llm full fledged math, multi-turn memory, enforced system directive, multimodal bitmap and doc analysis all actually work`() {
    val llm = com.example.ai.LocalLlmEngine()

    // 1. Real math & unit conversion
    val mathReply =
      llm.generateReply(
        prompt = "(25 + 15) * 3",
        model = com.example.ai.LocalLlmModel.TITAN_14B,
        preset = com.example.ai.LlmSystemPreset.ASSISTANT,
        temperature = 0.4f,
        historyCount = 1,
        tier = com.example.ai.LlmEvolutionTier.FULL_FLEDGED_LLM,
      )
    assertTrue("Expected 120 in reply: $mathReply", mathReply.contains("120"))

    val unitReply =
      llm.generateReply(
        prompt = "10 km to mi",
        model = com.example.ai.LocalLlmModel.TITAN_14B,
        preset = com.example.ai.LlmSystemPreset.ASSISTANT,
        temperature = 0.4f,
        historyCount = 1,
        tier = com.example.ai.LlmEvolutionTier.FULL_FLEDGED_LLM,
      )
    assertTrue("Expected 6.2137 mi in reply: $unitReply", unitReply.contains("6.2137 mi"))

    // 2. Multi-turn fact memory recall
    val recallReply =
      llm.generateReply(
        prompt = "What is my name?",
        model = com.example.ai.LocalLlmModel.TITAN_14B,
        preset = com.example.ai.LlmSystemPreset.ASSISTANT,
        temperature = 0.5f,
        historyCount = 2,
        tier = com.example.ai.LlmEvolutionTier.FULL_FLEDGED_LLM,
        priorUserTopics = listOf("My name is Carl"),
      )
    assertTrue("Expected Carl in recall reply: $recallReply", recallReply.contains("Carl"))

    // 3. System Prompt Directive enforcement (JSON output mode)
    val jsonDirectiveReply =
      llm.generateReply(
        prompt = "15% of 240",
        model = com.example.ai.LocalLlmModel.TITAN_14B,
        preset = com.example.ai.LlmSystemPreset.ASSISTANT,
        temperature = 0.2f,
        historyCount = 1,
        tier = com.example.ai.LlmEvolutionTier.FULL_FLEDGED_LLM,
        customSystemPrompt = "Reply in JSON",
      )
    val parsedJson = org.json.JSONObject(jsonDirectiveReply)
    assertTrue(parsedJson.getString("response").contains("36"))

    // 4. Real Bitmap pixel vision inspection + grounded Multimodal reply
    val bmp = android.graphics.Bitmap.createBitmap(32, 16, android.graphics.Bitmap.Config.ARGB_8888)
    bmp.eraseColor(android.graphics.Color.rgb(20, 120, 230))
    val visionRes =
      com.example.ai.LocalLlmMediaAnalyzer.analyzeBitmap(
        bitmap = bmp,
        displayName = "sky_sample.png",
        mimeType = "image/png",
      )
    assertEquals("Landscape (32x16)", visionRes.orientation)
    assertTrue(visionRes.dominantColorsHex.isNotEmpty())
    val visionReply =
      llm.generateReply(
        prompt = "What colors are in this image?",
        model = com.example.ai.LocalLlmModel.OMNI_VISION,
        preset = com.example.ai.LlmSystemPreset.ASSISTANT,
        temperature = 0.5f,
        historyCount = 1,
        tier = com.example.ai.LlmEvolutionTier.MULTIMODAL_AI,
        modality = com.example.ai.LlmInputModality.VISION,
        visionResult = visionRes,
      )
    assertTrue(visionReply.contains("Landscape (32x16)"))
    assertTrue(visionReply.contains(visionRes.dominantColorsHex.first()))

    // 5. Real Document reader + keyword search grounding
    val docRes =
      com.example.ai.LocalLlmMediaAnalyzer.analyzeRawDocumentText(
        displayName = "config.json",
        rawText = "{\"project\":\"HomeScreen\",\"targetSdk\":36,\"offline\":true}",
      )
    assertTrue("targetSdk" in docRes.jsonKeys)
    val docReply =
      llm.generateReply(
        prompt = "What is the targetSdk?",
        model = com.example.ai.LocalLlmModel.OMNI_VISION,
        preset = com.example.ai.LlmSystemPreset.ASSISTANT,
        temperature = 0.5f,
        historyCount = 1,
        tier = com.example.ai.LlmEvolutionTier.MULTIMODAL_AI,
        modality = com.example.ai.LlmInputModality.DOCUMENT,
        documentResult = docRes,
      )
    assertTrue(docReply.contains("targetSdk"))
    assertTrue(docReply.contains("36"))
  }

  @Test
  fun `agi core executes real phone tool actions when unlocked`() =
    runTest(testDispatcher) {
      val app = ApplicationProvider.getApplicationContext<Application>()
      app.getSharedPreferences(HomeViewModel.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
      ChangeLogStore(app).clearAllFiles()

      val viewModel = HomeViewModel(app)
      // Must be gated until LLM_AGI_CORE is unlocked
      assertTrue(viewModel.executeAgiToolActions("Save note: gated check").isEmpty())

      viewModel.applySuggestedUpgrade(PhoneUpgradeId.LLM_AGI_CORE)
      val actions =
        viewModel.executeAgiToolActions(
          "Save note: Ship Offline AGI, enable night dim, turn on torch, install Neon Snake, create an app called HydrationBot for water tracking"
        )
      assertTrue(actions.size >= 4)
      assertTrue(viewModel.uiState.value.notesText.contains("Ship Offline AGI"))
      assertTrue(viewModel.uiState.value.isNightDimEnabled)
      assertTrue(viewModel.uiState.value.isTorchOn)
      assertTrue(LibraryAppId.NEON_SNAKE in viewModel.uiState.value.installedLibraryApps)
      val forged = viewModel.uiState.value.customApps.find { it.name == "HydrationBot" }
      assertTrue(forged != null)
      assertTrue(File(forged!!.appFilePath).exists())
    }

  @Test
  fun `real browser app is on home menu by default, normalizes URLs and searches, and supports remove or uninstall`() =
    runTest(testDispatcher) {
      val app = ApplicationProvider.getApplicationContext<Application>()
      app.getSharedPreferences(HomeViewModel.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
      ChangeLogStore(app).clearAllFiles()

      val viewModel = HomeViewModel(app)
      assertTrue(viewModel.uiState.value.isBrowserInstalled)
      assertTrue(HomeViewModel.KEY_APP_BROWSER in viewModel.uiState.value.homeGridOrder)

      // Open via Drawer auto-closes Drawer & opens Browser screen
      viewModel.openDrawer()
      assertTrue(viewModel.uiState.value.isDrawerOpen)
      viewModel.openBrowser()
      assertFalse(viewModel.uiState.value.isDrawerOpen)
      assertEquals(ActiveAppScreen.BROWSER, viewModel.uiState.value.activeAppScreen)

      // URL & Search query normalization
      assertEquals(
        "https://wikipedia.org",
        com.example.ui.components.BrowserUrlNormalizer.normalizeInputToUrl("wikipedia.org"),
      )
      assertEquals(
        "https://developer.android.com/compose",
        com.example.ui.components.BrowserUrlNormalizer.normalizeInputToUrl("https://developer.android.com/compose"),
      )
      assertEquals(
        "https://duckduckgo.com/?q=jetpack+compose",
        com.example.ui.components.BrowserUrlNormalizer.normalizeInputToUrl("jetpack compose"),
      )

      // Remove from Home keeps Browser installed; Uninstall removes it
      viewModel.closeActiveApp()
      viewModel.removeFromHomeScreen(AppContextTarget.Browser)
      assertFalse(viewModel.isTargetOnHomeScreen(AppContextTarget.Browser))
      assertTrue(viewModel.uiState.value.isBrowserInstalled)

      viewModel.addToHomeScreen(AppContextTarget.Browser)
      assertTrue(viewModel.isTargetOnHomeScreen(AppContextTarget.Browser))

      viewModel.uninstallApp(AppContextTarget.Browser)
      assertFalse(viewModel.uiState.value.isBrowserInstalled)
      assertFalse(viewModel.isTargetOnHomeScreen(AppContextTarget.Browser))
    }
}
