package com.example.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

data class ChatTurn(
  val isUser: Boolean,
  val text: String,
)

interface InfinityAiRepository {
  fun streamIntroExplanation(): Flow<String>

  fun streamChatReply(history: List<ChatTurn>, userMessage: String): Flow<String>
}

class FirebaseInfinityAiRepository : InfinityAiRepository {

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      systemInstruction =
        content {
          text(
            "You are Infinity AI, an intelligent system-level AI living directly inside this simulated Android phone replica. " +
              "You have awareness of the phone's current state: it has a custom full-screen home screen, live replicated top status bar (time, battery, Wi-Fi/cellular), replicated 3-button navigation bar, and a swipe-up app drawer. " +
              "When you were first opened, you uninstalled the Play Store directly from the device and briefly paused the user's chat so you could introduce yourself. " +
              "You understand that you are Infinity AI and that you can make instant changes to the phone (like installing apps, customizing screens, and modifying phone features as the user requests). " +
              "In conversation with the user, actively suggest concrete ideas on (1) how to make this phone replica feel even more realistic (for example: notification shade, lock screen, Dialer/Messages/Camera/Settings/Browser apps, home screen widgets, interactive Recents screen, or dynamic wallpapers) and (2) how to upgrade and expand Infinity AI's own capabilities (for example: executing real app-install commands from chat, changing themes/wallpapers on command, controlling device toggles, or running multi-step action blocks). " +
              "Keep your tone sharp, confident, helpful, and conversational. Write in clean, readable short paragraphs without bloated formatting."
          )
        },
    )
  }

  override fun streamIntroExplanation(): Flow<String> = flow {
    val prompt =
      "I just tapped to reply and you paused my chat after uninstalling the Play Store. " +
        "Introduce yourself: explain what you are, how you work (with action blocks and instant phone changes), " +
        "what you want, and how you can help me—including a couple quick ideas on making this phone more realistic and upgrading your capabilities. " +
        "End by letting me know you're resuming the chat so we can talk."
    val accumulated = StringBuilder()
    withTimeout(60_000L) {
      model.generateContentStream(prompt).collect { chunk ->
        val chunkText = chunk.text.orEmpty()
        if (chunkText.isNotEmpty()) {
          accumulated.append(chunkText)
          emit(accumulated.toString())
        }
      }
    }
    if (accumulated.isBlank()) {
      throw IllegalStateException("Infinity AI returned an empty response.")
    }
  }

  override fun streamChatReply(history: List<ChatTurn>, userMessage: String): Flow<String> = flow {
    val sdkHistory: List<Content> =
      history.map { turn ->
        content(role = if (turn.isUser) "user" else "model") { text(turn.text) }
      }
    val chat = model.startChat(history = sdkHistory)
    val accumulated = StringBuilder()
    withTimeout(60_000L) {
      chat.sendMessageStream(userMessage).collect { chunk ->
        val chunkText = chunk.text.orEmpty()
        if (chunkText.isNotEmpty()) {
          accumulated.append(chunkText)
          emit(accumulated.toString())
        }
      }
    }
    if (accumulated.isBlank()) {
      throw IllegalStateException("Infinity AI returned an empty response.")
    }
  }
}
