package com.example.ui

import com.example.ai.CustomAppArchetype
import com.example.ai.CustomAppShortcut
import com.example.ai.PhoneUpgradeId
import com.example.library.LibraryAppId
import org.json.JSONArray
import org.json.JSONObject

internal object HomePrefsSerializer {

  fun encodeUpgrades(upgrades: Set<PhoneUpgradeId>): String =
    JSONArray().apply { upgrades.forEach { put(it.name) } }.toString()

  fun decodeUpgrades(raw: String?): Set<PhoneUpgradeId> {
    if (raw.isNullOrBlank()) return emptySet()
    return runCatching {
      val arr = JSONArray(raw)
      buildSet {
        for (i in 0 until arr.length()) {
          PhoneUpgradeId.entries.find { it.name == arr.getString(i) }?.let(::add)
        }
      }
    }.getOrDefault(emptySet())
  }

  fun encodeStringSet(values: Set<String>): String =
    JSONArray().apply { values.forEach { put(it) } }.toString()

  fun decodeStringSet(raw: String?): Set<String> {
    if (raw.isNullOrBlank()) return emptySet()
    return runCatching {
      val arr = JSONArray(raw)
      buildSet {
        for (i in 0 until arr.length()) add(arr.getString(i))
      }
    }.getOrDefault(emptySet())
  }

  fun encodeStringList(values: List<String>): String =
    JSONArray().apply { values.forEach { put(it) } }.toString()

  fun decodeStringList(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) add(arr.getString(i))
      }
    }.getOrDefault(emptyList())
  }

  fun encodeLibraryApps(apps: Set<LibraryAppId>): String =
    JSONArray().apply { apps.forEach { put(it.name) } }.toString()

  fun decodeLibraryApps(raw: String?): Set<LibraryAppId> {
    if (raw.isNullOrBlank()) return emptySet()
    return runCatching {
      val arr = JSONArray(raw)
      buildSet {
        for (i in 0 until arr.length()) {
          LibraryAppId.entries.find { it.name == arr.getString(i) }?.let(::add)
        }
      }
    }.getOrDefault(emptySet())
  }

  fun encodeCustomApps(apps: List<CustomAppShortcut>): String =
    JSONArray()
      .apply {
        apps.forEach { app ->
          put(
            JSONObject().apply {
              put("id", app.id)
              put("name", app.name)
              put("description", app.description)
              put("archetype", app.archetype.name)
              put("color", app.accentColorHex)
              put("filePath", app.appFilePath)
            }
          )
        }
      }
      .toString()

  fun decodeCustomApps(raw: String?): List<CustomAppShortcut> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          val arch =
            CustomAppArchetype.entries.find { it.name == obj.optString("archetype") }
              ?: CustomAppArchetype.GENERAL_UTILITY
          add(
            CustomAppShortcut(
              id = obj.getString("id"),
              name = obj.getString("name"),
              description = obj.optString("description", "Built by Infinity AI"),
              archetype = arch,
              accentColorHex = obj.getLong("color"),
              appFilePath = obj.optString("filePath", ""),
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  fun encodeChatItems(items: List<InfinityChatItem>): String =
    JSONArray()
      .apply {
        items.forEach { item ->
          val obj = JSONObject()
          obj.put("id", item.id)
          when (item) {
            is InfinityChatItem.ActionBlock -> {
              obj.put("type", "action")
              obj.put("text", item.label)
            }
            is InfinityChatItem.SuggestedActionBlock -> {
              obj.put("type", "suggest")
              obj.put("upgrade", item.upgradeId.name)
            }
            is InfinityChatItem.AiMessage -> {
              obj.put("type", "ai")
              obj.put("text", item.text)
            }
            is InfinityChatItem.UserMessage -> {
              obj.put("type", "user")
              obj.put("text", item.text)
            }
          }
          put(obj)
        }
      }
      .toString()

  fun decodeChatItems(raw: String?, activeUpgrades: Set<PhoneUpgradeId>): List<InfinityChatItem> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
      val array = JSONArray(raw)
      buildList {
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val id = obj.getString("id")
          when (obj.getString("type")) {
            "action" -> add(InfinityChatItem.ActionBlock(id = id, label = obj.getString("text")))
            "suggest" -> {
              val upgrade = PhoneUpgradeId.entries.find { it.name == obj.getString("upgrade") }
              if (upgrade != null) {
                add(
                  InfinityChatItem.SuggestedActionBlock(
                    id = id,
                    upgradeId = upgrade,
                    isApplied = upgrade in activeUpgrades,
                  )
                )
              }
            }
            "ai" -> add(InfinityChatItem.AiMessage(id = id, text = obj.getString("text")))
            "user" -> add(InfinityChatItem.UserMessage(id = id, text = obj.getString("text")))
          }
        }
      }
    }.getOrDefault(emptyList())
  }
}
