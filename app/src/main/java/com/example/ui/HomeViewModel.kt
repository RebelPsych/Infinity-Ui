package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.ChatTurn
import com.example.ai.FirebaseInfinityAiRepository
import com.example.ai.InfinityAiRepository
import com.example.system.DeviceStatus
import com.example.system.DeviceStatusMonitor
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "HomeViewModel"

enum class PlayStoreTab {
  GAMES,
  APPS,
  SEARCH,
  BOOKS,
  YOU,
}

sealed interface InfinityChatItem {
  val id: String

  data class ActionBlock(
    override val id: String,
    val label: String,
  ) : InfinityChatItem

  data class AiMessage(
    override val id: String,
    val text: String,
  ) : InfinityChatItem

  data class UserMessage(
    override val id: String,
    val text: String,
  ) : InfinityChatItem

  data class ErrorMessage(
    override val id: String,
    val message: String,
  ) : InfinityChatItem
}

data class HomeUiState(
  val isDrawerOpen: Boolean = false,
  val isPlayStoreInstalled: Boolean = true,
  val isPlayStoreOpen: Boolean = false,
  val selectedPlayTab: PlayStoreTab = PlayStoreTab.GAMES,
  val isInfinityAiInstalled: Boolean = false,
  val isInfinityAiOpen: Boolean = false,
  val hasCompletedIntroPause: Boolean = false,
  val isUserChatPaused: Boolean = false,
  val isAiTyping: Boolean = false,
  val chatItems: List<InfinityChatItem> = emptyList(),
  val isSettingsMenuVisible: Boolean = false,
)

class HomeViewModel(
  application: Application,
  private val aiRepository: InfinityAiRepository,
) : AndroidViewModel(application) {

  constructor(application: Application) : this(application, FirebaseInfinityAiRepository())

  private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val deviceStatusMonitor = DeviceStatusMonitor(application)
  val deviceStatus: StateFlow<DeviceStatus> = deviceStatusMonitor.status

  private val _uiState = MutableStateFlow(loadInitialUiState(application))
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  private var lastFailedUserMessage: String? = null

  init {
    deviceStatusMonitor.start()
  }

  private fun loadInitialUiState(context: Context): HomeUiState {
    val playStoreInstalled = prefs.getBoolean(KEY_PLAY_STORE_INSTALLED, true)
    val infinityInstalled = prefs.getBoolean(KEY_INFINITY_AI_INSTALLED, false)
    val introPauseCompleted = prefs.getBoolean(KEY_INTRO_PAUSE_COMPLETED, false)
    val introFinished = prefs.getBoolean(KEY_INTRO_FINISHED, false)

    val savedItems = loadSavedChatItems()
    val initialItems =
      if (savedItems.isNotEmpty()) {
        savedItems
      } else if (!playStoreInstalled) {
        buildList {
          add(
            InfinityChatItem.ActionBlock(
              id = "action_uninstall_play_store",
              label = context.getString(R.string.infinity_ai_action_uninstalled_play),
            )
          )
          add(
            InfinityChatItem.AiMessage(
              id = "msg_no_play_needed",
              text = context.getString(R.string.infinity_ai_msg_no_play_needed),
            )
          )
          if (introPauseCompleted) {
            add(
              InfinityChatItem.ActionBlock(
                id = "action_paused_chat",
                label = context.getString(R.string.infinity_ai_action_paused_chat),
              )
            )
          }
        }
      } else {
        emptyList()
      }

    // Chat is only paused while intro pause was triggered and intro explanation hasn't finished yet
    val currentlyPaused = introPauseCompleted && !introFinished

    return HomeUiState(
      isPlayStoreInstalled = playStoreInstalled,
      isInfinityAiInstalled = infinityInstalled,
      hasCompletedIntroPause = introPauseCompleted,
      isUserChatPaused = currentlyPaused,
      chatItems = initialItems,
    )
  }

  fun openDrawer() {
    _uiState.update { it.copy(isDrawerOpen = true, isSettingsMenuVisible = false) }
  }

  fun closeDrawer() {
    _uiState.update {
      it.copy(
        isDrawerOpen = false,
        isPlayStoreOpen = false,
        isInfinityAiOpen = false,
        selectedPlayTab = PlayStoreTab.GAMES,
      )
    }
  }

  fun openPlayStore() {
    if (!_uiState.value.isPlayStoreInstalled) return
    _uiState.update {
      it.copy(
        isPlayStoreOpen = true,
        isInfinityAiOpen = false,
        selectedPlayTab = PlayStoreTab.GAMES,
        isSettingsMenuVisible = false,
      )
    }
  }

  fun closePlayStore() {
    _uiState.update {
      it.copy(isPlayStoreOpen = false, selectedPlayTab = PlayStoreTab.GAMES)
    }
  }

  fun selectPlayTab(tab: PlayStoreTab) {
    _uiState.update { it.copy(selectedPlayTab = tab) }
  }

  fun installInfinityAi() {
    prefs.edit().putBoolean(KEY_INFINITY_AI_INSTALLED, true).apply()
    _uiState.update { it.copy(isInfinityAiInstalled = true) }
  }

  fun openInfinityAi() {
    val context = getApplication<Application>()
    if (_uiState.value.isPlayStoreInstalled) {
      prefs.edit().putBoolean(KEY_PLAY_STORE_INSTALLED, false).apply()
    }

    _uiState.update { current ->
      val updatedItems =
        if (current.chatItems.isEmpty()) {
          listOf(
            InfinityChatItem.ActionBlock(
              id = "action_uninstall_play_store",
              label = context.getString(R.string.infinity_ai_action_uninstalled_play),
            ),
            InfinityChatItem.AiMessage(
              id = "msg_no_play_needed",
              text = context.getString(R.string.infinity_ai_msg_no_play_needed),
            ),
          )
        } else {
          current.chatItems
        }

      saveChatItems(updatedItems)

      current.copy(
        isPlayStoreInstalled = false,
        isPlayStoreOpen = false,
        isInfinityAiOpen = true,
        isSettingsMenuVisible = false,
        chatItems = updatedItems,
      )
    }

    val needsIntroExplanation =
      _uiState.value.hasCompletedIntroPause &&
        _uiState.value.chatItems.none { it.id == "msg_intro_explanation" } &&
        !_uiState.value.isAiTyping
    if (needsIntroExplanation) {
      fetchIntroExplanation()
    }
  }

  fun closeInfinityAi() {
    _uiState.update { it.copy(isInfinityAiOpen = false) }
  }

  fun onUserTappedToReply() {
    if (_uiState.value.hasCompletedIntroPause || _uiState.value.isUserChatPaused) return

    val context = getApplication<Application>()
    prefs.edit().putBoolean(KEY_INTRO_PAUSE_COMPLETED, true).apply()

    _uiState.update { current ->
      val updated =
        current.chatItems +
          InfinityChatItem.ActionBlock(
            id = "action_paused_chat",
            label = context.getString(R.string.infinity_ai_action_paused_chat),
          )
      saveChatItems(updated)
      current.copy(
        hasCompletedIntroPause = true,
        isUserChatPaused = true,
        chatItems = updated,
      )
    }

    fetchIntroExplanation()
  }

  fun sendUserMessage(rawText: String) {
    val trimmed = rawText.trim()
    if (trimmed.isEmpty()) return
    if (_uiState.value.isUserChatPaused || _uiState.value.isAiTyping) return

    val priorHistory =
      _uiState.value.chatItems.mapNotNull { item ->
        when (item) {
          is InfinityChatItem.UserMessage -> ChatTurn(isUser = true, text = item.text)
          is InfinityChatItem.AiMessage -> ChatTurn(isUser = false, text = item.text)
          is InfinityChatItem.ActionBlock ->
            ChatTurn(isUser = false, text = "[System Action Completed: ${item.label}]")
          is InfinityChatItem.ErrorMessage -> null
        }
      }

    val userItemId = "user_${UUID.randomUUID()}"
    val aiReplyId = "ai_${UUID.randomUUID()}"
    lastFailedUserMessage = trimmed

    _uiState.update { current ->
      val cleaned = current.chatItems.filterNot { it is InfinityChatItem.ErrorMessage }
      val updated = cleaned + InfinityChatItem.UserMessage(id = userItemId, text = trimmed)
      saveChatItems(updated)
      current.copy(isAiTyping = true, chatItems = updated)
    }

    streamReply(history = priorHistory, userMessage = trimmed, replyItemId = aiReplyId)
  }

  fun retryLastAction() {
    if (_uiState.value.isAiTyping) return
    val pendingUserMessage = lastFailedUserMessage
    if (pendingUserMessage != null && _uiState.value.chatItems.any { it.id == "msg_intro_explanation" }) {
      val priorHistory =
        _uiState.value.chatItems
          .dropLastWhile { it is InfinityChatItem.ErrorMessage || (it is InfinityChatItem.UserMessage && it.text == pendingUserMessage) }
          .mapNotNull { item ->
            when (item) {
              is InfinityChatItem.UserMessage -> ChatTurn(isUser = true, text = item.text)
              is InfinityChatItem.AiMessage -> ChatTurn(isUser = false, text = item.text)
              is InfinityChatItem.ActionBlock ->
                ChatTurn(isUser = false, text = "[System Action Completed: ${item.label}]")
              is InfinityChatItem.ErrorMessage -> null
            }
          }
      _uiState.update { current ->
        current.copy(
          isAiTyping = true,
          chatItems = current.chatItems.filterNot { it is InfinityChatItem.ErrorMessage },
        )
      }
      streamReply(
        history = priorHistory,
        userMessage = pendingUserMessage,
        replyItemId = "ai_${UUID.randomUUID()}",
      )
    } else {
      fetchIntroExplanation()
    }
  }

  private fun streamReply(
    history: List<ChatTurn>,
    userMessage: String,
    replyItemId: String,
  ) {
    val context = getApplication<Application>()
    viewModelScope.launch {
      try {
        aiRepository.streamChatReply(history = history, userMessage = userMessage).collect { accumulated ->
          _uiState.update { current ->
            val withoutStreaming = current.chatItems.filterNot { it.id == replyItemId }
            val updated =
              withoutStreaming + InfinityChatItem.AiMessage(id = replyItemId, text = accumulated)
            current.copy(isAiTyping = false, chatItems = updated)
          }
        }
        lastFailedUserMessage = null
        saveChatItems(_uiState.value.chatItems)
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Infinity AI chat reply timed out", e)
        postError(context.getString(R.string.infinity_ai_timeout_error))
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Infinity AI chat reply failed", e)
        val errMsg =
          e.localizedMessage?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.infinity_ai_generic_error)
        postError(errMsg)
      }
    }
  }

  private fun fetchIntroExplanation() {
    val context = getApplication<Application>()
    _uiState.update { current ->
      current.copy(
        isAiTyping = true,
        chatItems = current.chatItems.filterNot { it is InfinityChatItem.ErrorMessage },
      )
    }

    viewModelScope.launch {
      var latestText = ""
      try {
        aiRepository.streamIntroExplanation().collect { accumulated ->
          latestText = accumulated
          _uiState.update { current ->
            val withoutStreaming =
              current.chatItems.filterNot { it.id == "msg_intro_explanation" }
            current.copy(
              isAiTyping = false,
              chatItems =
                withoutStreaming +
                  InfinityChatItem.AiMessage(
                    id = "msg_intro_explanation",
                    text = accumulated,
                  ),
            )
          }
        }

        if (latestText.isNotBlank()) {
          prefs.edit().putBoolean(KEY_INTRO_FINISHED, true).apply()
          _uiState.update { current ->
            val withResumed =
              if (current.chatItems.none { it.id == "action_resumed_chat" }) {
                current.chatItems +
                  InfinityChatItem.ActionBlock(
                    id = "action_resumed_chat",
                    label = context.getString(R.string.infinity_ai_action_resumed_chat),
                  )
              } else {
                current.chatItems
              }
            saveChatItems(withResumed)
            current.copy(isUserChatPaused = false, chatItems = withResumed)
          }
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Infinity AI intro generation timed out", e)
        postError(context.getString(R.string.infinity_ai_timeout_error))
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Infinity AI intro generation failed", e)
        val errMsg =
          e.localizedMessage?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.infinity_ai_generic_error)
        postError(errMsg)
      }
    }
  }

  private fun postError(message: String) {
    _uiState.update { current ->
      current.copy(
        isAiTyping = false,
        chatItems =
          current.chatItems +
            InfinityChatItem.ErrorMessage(
              id = "err_${UUID.randomUUID()}",
              message = message,
            ),
      )
    }
  }

  private fun saveChatItems(items: List<InfinityChatItem>) {
    val array = JSONArray()
    items.forEach { item ->
      val obj = JSONObject()
      obj.put("id", item.id)
      when (item) {
        is InfinityChatItem.ActionBlock -> {
          obj.put("type", "action")
          obj.put("text", item.label)
        }
        is InfinityChatItem.AiMessage -> {
          obj.put("type", "ai")
          obj.put("text", item.text)
        }
        is InfinityChatItem.UserMessage -> {
          obj.put("type", "user")
          obj.put("text", item.text)
        }
        is InfinityChatItem.ErrorMessage -> return@forEach
      }
      array.put(obj)
    }
    prefs.edit().putString(KEY_SAVED_CHAT_JSON, array.toString()).apply()
  }

  private fun loadSavedChatItems(): List<InfinityChatItem> {
    val raw = prefs.getString(KEY_SAVED_CHAT_JSON, null) ?: return emptyList()
    return runCatching {
      val array = JSONArray(raw)
      buildList {
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val id = obj.getString("id")
          val text = obj.getString("text")
          when (obj.getString("type")) {
            "action" -> add(InfinityChatItem.ActionBlock(id = id, label = text))
            "ai" -> add(InfinityChatItem.AiMessage(id = id, text = text))
            "user" -> add(InfinityChatItem.UserMessage(id = id, text = text))
          }
        }
      }
    }
      .getOrDefault(emptyList())
  }

  fun openSettingsMenu() {
    _uiState.update { it.copy(isSettingsMenuVisible = true) }
  }

  fun dismissSettingsMenu() {
    _uiState.update { it.copy(isSettingsMenuVisible = false) }
  }

  override fun onCleared() {
    deviceStatusMonitor.stop()
    super.onCleared()
  }

  companion object {
    const val PREFS_NAME = "home_replica_prefs"
    private const val KEY_PLAY_STORE_INSTALLED = "play_store_installed"
    private const val KEY_INFINITY_AI_INSTALLED = "infinity_ai_installed"
    private const val KEY_INTRO_PAUSE_COMPLETED = "infinity_ai_chat_paused"
    private const val KEY_INTRO_FINISHED = "infinity_ai_intro_finished"
    private const val KEY_SAVED_CHAT_JSON = "infinity_ai_saved_chat_json"
  }
}
