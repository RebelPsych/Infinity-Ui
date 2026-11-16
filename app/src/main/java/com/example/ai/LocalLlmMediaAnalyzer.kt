package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import org.json.JSONObject

data class VisionAnalysisResult(
  val displayName: String,
  val mimeType: String,
  val width: Int,
  val height: Int,
  val avgBrightnessPct: Int,
  val brightnessLabel: String,
  val contrastScore: Int,
  val orientation: String,
  val dominantColorsHex: List<String>,
  val dominantColorNames: List<String>,
  val thumbnail: Bitmap?,
)

data class DocumentAnalysisResult(
  val displayName: String,
  val lineCount: Int,
  val wordCount: Int,
  val charCount: Int,
  val jsonKeys: List<String>,
  val excerpt: String,
  val fullText: String,
)

data class AudioAnalysisResult(
  val displayName: String,
  val mimeType: String,
  val durationMs: Long,
  val bitrateKbps: Int?,
  val sampleRateHz: Int?,
)

object LocalLlmMediaAnalyzer {

  fun queryDisplayName(context: Context, uri: Uri): String {
    return runCatching {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (idx >= 0) return@runCatching cursor.getString(idx)
        }
      }
      uri.lastPathSegment ?: "attached_file"
    }.getOrDefault(uri.lastPathSegment ?: "attached_file")
  }

  fun analyzeImageUri(context: Context, uri: Uri): VisionAnalysisResult? {
    return runCatching {
      val mime = context.contentResolver.getType(uri) ?: "image/*"
      val name = queryDisplayName(context, uri)

      val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOpts)
      }
      val origW = boundsOpts.outWidth.coerceAtLeast(1)
      val origH = boundsOpts.outHeight.coerceAtLeast(1)

      var sample = 1
      while (origW / sample > 256 || origH / sample > 256) {
        sample *= 2
      }
      val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
      val bmp =
        context.contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null

      analyzeBitmap(
        bitmap = bmp,
        displayName = name,
        mimeType = mime,
        originalWidth = origW,
        originalHeight = origH,
      )
    }.getOrNull()
  }

  fun analyzeBitmap(
    bitmap: Bitmap,
    displayName: String,
    mimeType: String,
    originalWidth: Int = bitmap.width,
    originalHeight: Int = bitmap.height,
  ): VisionAnalysisResult {
    val w = bitmap.width.coerceAtLeast(1)
    val h = bitmap.height.coerceAtLeast(1)
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    var lumSum = 0.0
    var diffSum = 0.0
    var diffCount = 0
    val buckets = HashMap<Int, Int>()

    for (y in 0 until h) {
      for (x in 0 until w) {
        val c = pixels[y * w + x]
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        val lum = (0.299 * r + 0.587 * g + 0.114 * b)
        lumSum += lum

        if (x + 1 < w) {
          val right = pixels[y * w + x + 1]
          val rl = 0.299 * ((right shr 16) and 0xFF) + 0.587 * ((right shr 8) and 0xFF) + 0.114 * (right and 0xFF)
          diffSum += abs(lum - rl)
          diffCount++
        }

        // Quantize RGB into 4-bit per channel bucket
        val qr = (r shr 5) shl 5
        val qg = (g shr 5) shl 5
        val qb = (b shr 5) shl 5
        val key = (0xFF shl 24) or (qr shl 16) or (qg shl 8) or qb
        buckets[key] = (buckets[key] ?: 0) + 1
      }
    }

    val totalPx = pixels.size.coerceAtLeast(1)
    val avgLumPct = ((lumSum / totalPx) / 255.0 * 100.0).roundToInt().coerceIn(0, 100)
    val brightnessLabel =
      when {
        avgLumPct < 22 -> "Dark / Low-Key"
        avgLumPct < 45 -> "Moody / Dim"
        avgLumPct < 75 -> "Balanced Exposure"
        else -> "Bright / High-Key"
      }
    val contrastScore =
      if (diffCount > 0) ((diffSum / diffCount) * 2.2).roundToInt().coerceIn(1, 100) else 25

    val topColors =
      buckets.entries
        .sortedByDescending { it.value }
        .take(3)
        .map { it.key }
    val hexes = topColors.map { String.format(Locale.US, "#%06X", it and 0xFFFFFF) }
    val names = topColors.map(::describeRgbColor)

    val orientation =
      when {
        originalWidth > originalHeight -> "Landscape (${originalWidth}x${originalHeight})"
        originalHeight > originalWidth -> "Portrait (${originalWidth}x${originalHeight})"
        else -> "Square (${originalWidth}x${originalHeight})"
      }

    return VisionAnalysisResult(
      displayName = displayName,
      mimeType = mimeType,
      width = originalWidth,
      height = originalHeight,
      avgBrightnessPct = avgLumPct,
      brightnessLabel = brightnessLabel,
      contrastScore = contrastScore,
      orientation = orientation,
      dominantColorsHex = hexes,
      dominantColorNames = names,
      thumbnail = bitmap,
    )
  }

  fun analyzeDocumentUri(context: Context, uri: Uri): DocumentAnalysisResult? {
    return runCatching {
      val name = queryDisplayName(context, uri)
      val text =
        context.contentResolver.openInputStream(uri)?.use { stream ->
          BufferedReader(InputStreamReader(stream)).readText()
        }?.take(24_000) ?: return null
      analyzeRawDocumentText(displayName = name, rawText = text)
    }.getOrNull()
  }

  fun analyzeRawDocumentText(displayName: String, rawText: String): DocumentAnalysisResult {
    val normalized = rawText.trim()
    val lines = if (normalized.isEmpty()) emptyList() else normalized.lines()
    val words =
      if (normalized.isEmpty()) emptyList()
      else normalized.split(Regex("\\s+")).filter { it.isNotBlank() }

    val jsonKeys =
      runCatching {
        val obj = JSONObject(normalized)
        obj.keys().asSequence().toList().take(12)
      }.getOrDefault(emptyList())

    return DocumentAnalysisResult(
      displayName = displayName,
      lineCount = lines.size,
      wordCount = words.size,
      charCount = normalized.length,
      jsonKeys = jsonKeys,
      excerpt = normalized.take(280),
      fullText = normalized,
    )
  }

  fun analyzeAudioUri(context: Context, uri: Uri): AudioAnalysisResult? {
    return runCatching {
      val name = queryDisplayName(context, uri)
      val mime = context.contentResolver.getType(uri) ?: "audio/*"
      val mmr = MediaMetadataRetriever()
      mmr.setDataSource(context, uri)
      val dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
      val bitrate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.div(1000)
      val sampleRate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
      mmr.release()
      AudioAnalysisResult(
        displayName = name,
        mimeType = mime,
        durationMs = dur,
        bitrateKbps = bitrate,
        sampleRateHz = sampleRate,
      )
    }.getOrNull()
  }

  private fun describeRgbColor(argb: Int): String {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return when {
      r < 45 && g < 45 && b < 45 -> "Deep Charcoal"
      r > 205 && g > 205 && b > 205 -> "Bright White"
      abs(r - g) < 25 && abs(g - b) < 25 -> "Neutral Gray"
      r > g + 45 && r > b + 45 -> "Crimson / Red"
      r > 160 && g > 110 && b < 80 -> "Warm Amber / Gold"
      g > r + 35 && g > b + 20 -> "Emerald / Green"
      b > r + 40 && b > g + 15 -> "Cobalt / Cyan-Blue"
      r > 120 && b > 120 && g < 95 -> "Violet / Magenta"
      else -> "Balanced Tone"
    }
  }
}
