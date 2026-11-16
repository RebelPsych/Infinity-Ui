package com.example.ai

import android.content.Context
import com.example.library.LibraryAppId
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

enum class CustomAppArchetype(val label: String) {
  COUNTER_TRACKER("Tracker"),
  EXPENSE_BUDGET("Finance"),
  TIMER_STOPWATCH("Timer"),
  FLASHCARDS_NOTES("Notes & Cards"),
  DICE_RANDOM("Randomizer"),
  DRAW_SKETCH("Canvas"),
  GENERAL_UTILITY("Custom Utility"),
}

data class CustomAppShortcut(
  val id: String,
  val name: String,
  val description: String = "Built by Infinity AI",
  val archetype: CustomAppArchetype = CustomAppArchetype.GENERAL_UTILITY,
  val accentColorHex: Long,
  val appFilePath: String = "",
)

sealed interface ChangePayload {
  data object UninstallPlayStore : ChangePayload

  data object CreateAppLibrary : ChangePayload

  data object CreateLocalLlm : ChangePayload

  data class InstallLibraryApp(val appId: LibraryAppId) : ChangePayload

  data class ApplyUpgrade(val upgradeId: PhoneUpgradeId) : ChangePayload

  data class ForgeCustomApp(val app: CustomAppShortcut) : ChangePayload

  data class SetNightDim(val enabled: Boolean) : ChangePayload
}

data class ChangeRecord(
  val id: String,
  val orderNumber: Int,
  val title: String,
  val detail: String,
  val timestampLabel: String,
  val isChecked: Boolean,
  val payload: ChangePayload,
)

data class ComputedPhoneState(
  val isPlayStoreInstalled: Boolean = true,
  val isAppLibraryInstalled: Boolean = false,
  val isLocalLlmInstalled: Boolean = false,
  val installedLibraryApps: Set<LibraryAppId> = emptySet(),
  val activeUpgrades: Set<PhoneUpgradeId> = emptySet(),
  val disabledUpgrades: Set<PhoneUpgradeId> = emptySet(),
  val customApps: List<CustomAppShortcut> = emptyList(),
  val isNightDimEnabled: Boolean = false,
)

class ChangeLogStore(private val context: Context) {

  private val changesFile: File
    get() = File(context.filesDir, "system_changes_app.json")

  private val forgedAppsDir: File
    get() = File(context.filesDir, "forged_apps").apply { mkdirs() }

  val changeLogFilePath: String
    get() = changesFile.absolutePath

  fun writeForgedAppFile(app: CustomAppShortcut): String {
    val safeName = app.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    val file = File(forgedAppsDir, "$safeName.json")
    val json =
      JSONObject().apply {
        put("id", app.id)
        put("name", app.name)
        put("description", app.description)
        put("archetype", app.archetype.name)
        put("accentColorHex", app.accentColorHex)
        put("createdAt", System.currentTimeMillis())
      }
    file.writeText(json.toString(2))
    return file.absolutePath
  }

  fun deleteForgedAppFile(appId: String) {
    val safeName = appId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    val file = File(forgedAppsDir, "$safeName.json")
    if (file.exists()) {
      file.delete()
    }
  }

  fun listForgedAppFiles(): List<File> =
    forgedAppsDir.listFiles()?.filter { it.isFile && it.extension == "json" }?.sortedBy { it.name }
      ?: emptyList()

  fun saveChanges(changes: List<ChangeRecord>) {
    val arr = JSONArray()
    changes.forEach { rec ->
      val obj =
        JSONObject().apply {
          put("id", rec.id)
          put("orderNumber", rec.orderNumber)
          put("title", rec.title)
          put("detail", rec.detail)
          put("timestampLabel", rec.timestampLabel)
          put("isChecked", rec.isChecked)
          put("payload", serializePayload(rec.payload))
        }
      arr.put(obj)
    }
    val root =
      JSONObject().apply {
        put("appFile", "Infinity AI System Changes")
        put("updatedAt", System.currentTimeMillis())
        put("changes", arr)
      }
    changesFile.writeText(root.toString(2))
  }

  fun loadChanges(): List<ChangeRecord> {
    if (!changesFile.exists()) return emptyList()
    return runCatching {
      val root = JSONObject(changesFile.readText())
      val arr = root.getJSONArray("changes")
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          val payload = deserializePayload(obj.getJSONObject("payload")) ?: continue
          add(
            ChangeRecord(
              id = obj.getString("id"),
              orderNumber = obj.optInt("orderNumber", i + 1),
              title = obj.getString("title"),
              detail = obj.optString("detail", ""),
              timestampLabel = obj.optString("timestampLabel", currentTimeLabel()),
              isChecked = obj.optBoolean("isChecked", true),
              payload = payload,
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  fun recomputeState(changes: List<ChangeRecord>): ComputedPhoneState {
    var playInstalled = true
    var appLibraryInstalled = false
    var localLlmInstalled = false
    val libApps = linkedSetOf<LibraryAppId>()
    val upgrades = linkedSetOf<PhoneUpgradeId>()
    val disabledUpgrades = linkedSetOf<PhoneUpgradeId>()
    val customApps = mutableListOf<CustomAppShortcut>()
    var nightDim = false

    changes.forEach { record ->
      val p = record.payload
      if (!record.isChecked) {
        if (p is ChangePayload.ApplyUpgrade) {
          disabledUpgrades.add(p.upgradeId)
        }
        return@forEach
      }
      when (p) {
        ChangePayload.UninstallPlayStore -> playInstalled = false
        ChangePayload.CreateAppLibrary -> appLibraryInstalled = true
        ChangePayload.CreateLocalLlm -> localLlmInstalled = true
        is ChangePayload.InstallLibraryApp -> libApps.add(p.appId)
        is ChangePayload.ApplyUpgrade -> {
          upgrades.add(p.upgradeId)
          disabledUpgrades.remove(p.upgradeId)
        }
        is ChangePayload.ForgeCustomApp -> {
          if (customApps.none { it.id == p.app.id }) {
            customApps.add(p.app)
          }
        }
        is ChangePayload.SetNightDim -> nightDim = p.enabled
      }
    }

    return ComputedPhoneState(
      isPlayStoreInstalled = playInstalled,
      isAppLibraryInstalled = appLibraryInstalled,
      isLocalLlmInstalled = localLlmInstalled,
      installedLibraryApps = libApps,
      activeUpgrades = upgrades,
      disabledUpgrades = disabledUpgrades,
      customApps = customApps,
      isNightDimEnabled = nightDim,
    )
  }

  fun clearAllFiles() {
    if (changesFile.exists()) changesFile.delete()
    listForgedAppFiles().forEach { it.delete() }
  }

  private fun serializePayload(payload: ChangePayload): JSONObject =
    JSONObject().apply {
      when (payload) {
        ChangePayload.UninstallPlayStore -> put("type", "UNINSTALL_PLAY_STORE")
        ChangePayload.CreateAppLibrary -> put("type", "CREATE_APP_LIBRARY")
        ChangePayload.CreateLocalLlm -> put("type", "CREATE_LOCAL_LLM")
        is ChangePayload.InstallLibraryApp -> {
          put("type", "INSTALL_LIBRARY_APP")
          put("appId", payload.appId.name)
        }
        is ChangePayload.ApplyUpgrade -> {
          put("type", "APPLY_UPGRADE")
          put("upgradeId", payload.upgradeId.name)
        }
        is ChangePayload.ForgeCustomApp -> {
          put("type", "FORGE_CUSTOM_APP")
          put("id", payload.app.id)
          put("name", payload.app.name)
          put("description", payload.app.description)
          put("archetype", payload.app.archetype.name)
          put("color", payload.app.accentColorHex)
          put("filePath", payload.app.appFilePath)
        }
        is ChangePayload.SetNightDim -> {
          put("type", "SET_NIGHT_DIM")
          put("enabled", payload.enabled)
        }
      }
    }

  private fun deserializePayload(obj: JSONObject): ChangePayload? =
    when (obj.optString("type")) {
      "UNINSTALL_PLAY_STORE" -> ChangePayload.UninstallPlayStore
      "CREATE_APP_LIBRARY" -> ChangePayload.CreateAppLibrary
      "CREATE_LOCAL_LLM" -> ChangePayload.CreateLocalLlm
      "INSTALL_LIBRARY_APP" ->
        LibraryAppId.entries.find { it.name == obj.optString("appId") }?.let {
          ChangePayload.InstallLibraryApp(it)
        }
      "APPLY_UPGRADE" ->
        PhoneUpgradeId.entries.find { it.name == obj.optString("upgradeId") }?.let {
          ChangePayload.ApplyUpgrade(it)
        }
      "FORGE_CUSTOM_APP" -> {
        val arch =
          CustomAppArchetype.entries.find { it.name == obj.optString("archetype") }
            ?: CustomAppArchetype.GENERAL_UTILITY
        ChangePayload.ForgeCustomApp(
          CustomAppShortcut(
            id = obj.getString("id"),
            name = obj.getString("name"),
            description = obj.optString("description", "Built by Infinity AI"),
            archetype = arch,
            accentColorHex = obj.optLong("color", 0xFFA855F7L),
            appFilePath = obj.optString("filePath", ""),
          )
        )
      }
      "SET_NIGHT_DIM" -> ChangePayload.SetNightDim(obj.optBoolean("enabled", false))
      else -> null
    }

  companion object {
    fun currentTimeLabel(): String =
      SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
  }
}
