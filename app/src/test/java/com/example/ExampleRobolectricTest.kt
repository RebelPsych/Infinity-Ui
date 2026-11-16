package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.ChatTurn
import com.example.ai.InfinityAiRepository
import com.example.ui.HomeViewModel
import com.example.ui.InfinityChatItem
import com.example.ui.PlayStoreTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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

  private class FakeInfinityAiRepository : InfinityAiRepository {
    override fun streamIntroExplanation(): Flow<String> =
      flowOf("I am Infinity AI, your system-level AI.")

    override fun streamChatReply(history: List<ChatTurn>, userMessage: String): Flow<String> =
      flowOf("Reply to '$userMessage' (history=${history.size})")
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `app name matches Home Screen`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Home Screen", appName)
  }

  @Test
  fun `infinity ai intro resumes chat and supports multi-turn Gemini messaging`() = runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    app.getSharedPreferences(HomeViewModel.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    val viewModel = HomeViewModel(app, FakeInfinityAiRepository())

    viewModel.openDrawer()
    viewModel.openPlayStore()
    viewModel.selectPlayTab(PlayStoreTab.APPS)
    viewModel.installInfinityAi()

    // Opening Infinity AI uninstalls Play Store
    viewModel.openInfinityAi()
    assertTrue(viewModel.uiState.value.isInfinityAiOpen)
    assertFalse(viewModel.uiState.value.isPlayStoreInstalled)
    assertEquals(2, viewModel.uiState.value.chatItems.size)

    // Tapping to reply pauses chat, explains, then resumes chat
    viewModel.onUserTappedToReply()
    assertTrue(viewModel.uiState.value.isUserChatPaused)

    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.isAiTyping)
    assertFalse(viewModel.uiState.value.isUserChatPaused)
    assertTrue(viewModel.uiState.value.hasCompletedIntroPause)
    assertEquals(5, viewModel.uiState.value.chatItems.size)
    assertEquals(
      "Paused user chat",
      (viewModel.uiState.value.chatItems[2] as InfinityChatItem.ActionBlock).label,
    )
    assertEquals(
      "Resumed user chat",
      (viewModel.uiState.value.chatItems[4] as InfinityChatItem.ActionBlock).label,
    )

    // User can now send messages to Gemini
    viewModel.sendUserMessage("What should we add next?")
    advanceUntilIdle()
    assertEquals(7, viewModel.uiState.value.chatItems.size)
    assertEquals(
      "What should we add next?",
      (viewModel.uiState.value.chatItems[5] as InfinityChatItem.UserMessage).text,
    )
    assertTrue(
      (viewModel.uiState.value.chatItems[6] as InfinityChatItem.AiMessage)
        .text
        .contains("What should we add next?")
    )

    // Verify full conversation state persists across reload
    val reloaded = HomeViewModel(app, FakeInfinityAiRepository())
    assertFalse(reloaded.uiState.value.isPlayStoreInstalled)
    assertFalse(reloaded.uiState.value.isUserChatPaused)
    assertTrue(reloaded.uiState.value.hasCompletedIntroPause)
    assertEquals(7, reloaded.uiState.value.chatItems.size)
  }
}
