package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class BrowserBookmark(
  val title: String,
  val url: String,
  val accentHex: Long = 0xFF38BDF8L,
)

private data class BrowserTabState(
  val id: String,
  val title: String = "Start Page",
  val url: String = "",
  val isSpeedDial: Boolean = true,
  val canGoBack: Boolean = false,
  val canGoForward: Boolean = false,
  val isLoading: Boolean = false,
  val loadProgress: Int = 0,
  val errorDescription: String? = null,
)

object BrowserUrlNormalizer {
  private val SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")
  private val DOMAIN_LIKE_REGEX = Regex("^[^\\s]+\\.[a-zA-Z]{2,}(/.*)?$")

  fun normalizeInputToUrl(rawInput: String): String {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) return "https://duckduckgo.com"
    if (SCHEME_REGEX.matches(trimmed)) {
      val scheme = trimmed.substringBefore("://").lowercase(Locale.US)
      return if (scheme == "http" || scheme == "https") trimmed
      else "https://duckduckgo.com/?q=${URLEncoder.encode(trimmed, "UTF-8")}"
    }
    if (!trimmed.contains(' ') && (DOMAIN_LIKE_REGEX.matches(trimmed) || trimmed.startsWith("localhost", ignoreCase = true))) {
      return "https://$trimmed"
    }
    val encoded = URLEncoder.encode(trimmed, "UTF-8")
    return "https://duckduckgo.com/?q=$encoded"
  }

  fun displayHost(url: String): String =
    runCatching {
      val host = Uri.parse(url).host.orEmpty()
      host.removePrefix("www.").ifBlank { url }
    }.getOrDefault(url)
}

private val DEFAULT_BOOKMARKS =
  listOf(
    BrowserBookmark("DuckDuckGo", "https://duckduckgo.com", 0xFFFB923CL),
    BrowserBookmark("Wikipedia", "https://en.m.wikipedia.org", 0xFF38BDF8L),
    BrowserBookmark("Android Docs", "https://developer.android.com", 0xFF34D399L),
    BrowserBookmark("GitHub", "https://github.com", 0xFFA855F7L),
    BrowserBookmark("MDN Web", "https://developer.mozilla.org", 0xFF60A5FAL),
    BrowserBookmark("Hacker News", "https://news.ycombinator.com", 0xFFF59E0BL),
  )

@Composable
fun BrowserAppIcon(
  modifier: Modifier = Modifier,
  iconSize: Dp = 54.dp,
  cornerRadius: Dp = 11.dp,
) {
  Surface(
    shape = RoundedCornerShape(cornerRadius),
    color = Color(0xFF0A1628),
    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.65f)),
    shadowElevation = 2.dp,
    modifier = modifier.size(iconSize),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize().background(
          Brush.linearGradient(listOf(Color(0xFF0E294B), Color(0xFF091426)))
        ),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.size(iconSize * 0.62f)) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        val stroke = Stroke(width = size.minDimension * 0.085f)
        drawCircle(color = Color(0xFF38BDF8), radius = r * 0.90f, center = c, style = stroke)
        // Latitude & longitude rings for crisp globe emblem
        drawOval(
          color = Color(0xFF7DD3FC),
          topLeft = Offset(c.x - r * 0.45f, c.y - r * 0.90f),
          size = androidx.compose.ui.geometry.Size(r * 0.90f, r * 1.80f),
          style = Stroke(width = size.minDimension * 0.06f),
        )
        drawLine(
          color = Color(0xFF7DD3FC),
          start = Offset(c.x - r * 0.88f, c.y),
          end = Offset(c.x + r * 0.88f, c.y),
          strokeWidth = size.minDimension * 0.06f,
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserAppScreen(
  onCloseBrowser: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current
  val prefs = remember { context.getSharedPreferences("browser_prefs", Context.MODE_PRIVATE) }

  var tabs by remember {
    mutableStateOf(listOf(BrowserTabState(id = "tab_1", title = "Start Page", isSpeedDial = true)))
  }
  var activeTabIndex by remember { mutableIntStateOf(0) }
  val activeTab = tabs.getOrElse(activeTabIndex) { tabs.first() }

  var addressInput by remember(activeTab.id) { mutableStateOf(activeTab.url) }
  var bookmarks by remember { mutableStateOf(loadBookmarks(prefs)) }
  var recentUrls by remember { mutableStateOf(loadRecentUrls(prefs)) }
  var showBookmarksSheet by remember { mutableStateOf(false) }

  // Live Android WebView instance map keyed by Tab ID
  val webViewMap = remember { mutableStateMapOf<String, WebView>() }

  DisposableEffect(Unit) {
    onDispose {
      webViewMap.values.forEach { wv ->
        runCatching {
          (wv.parent as? ViewGroup)?.removeView(wv)
          wv.stopLoading()
          wv.destroy()
        }
      }
      webViewMap.clear()
    }
  }

  fun updateTab(tabId: String, transform: (BrowserTabState) -> BrowserTabState) {
    tabs = tabs.map { if (it.id == tabId) transform(it) else it }
  }

  fun getOrCreateWebView(tabId: String): WebView {
    return webViewMap.getOrPut(tabId) {
      WebView(context).apply {
        layoutParams =
          FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
          )
        settings.apply {
          javaScriptEnabled = true
          domStorageEnabled = true
          useWideViewPort = true
          loadWithOverviewMode = true
          builtInZoomControls = true
          displayZoomControls = false
          mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        webViewClient =
          object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
              view: WebView?,
              request: WebResourceRequest?,
            ): Boolean {
              val scheme = request?.url?.scheme?.lowercase(Locale.US)
              return scheme != "http" && scheme != "https" && scheme != "about"
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
              val targetUrl = url ?: return
              updateTab(tabId) { cur ->
                cur.copy(
                  url = targetUrl,
                  isSpeedDial = false,
                  isLoading = true,
                  errorDescription = null,
                  canGoBack = view?.canGoBack() ?: cur.canGoBack,
                  canGoForward = view?.canGoForward() ?: cur.canGoForward,
                )
              }
              if (tabs.getOrNull(activeTabIndex)?.id == tabId) {
                addressInput = targetUrl
              }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
              val targetUrl = url ?: return
              val pageTitle = view?.title?.takeIf { it.isNotBlank() } ?: BrowserUrlNormalizer.displayHost(targetUrl)
              updateTab(tabId) { cur ->
                cur.copy(
                  title = pageTitle,
                  url = targetUrl,
                  isSpeedDial = false,
                  isLoading = false,
                  loadProgress = 100,
                  canGoBack = view?.canGoBack() ?: false,
                  canGoForward = view?.canGoForward() ?: false,
                )
              }
              recentUrls = recordRecentUrl(prefs, recentUrls, pageTitle, targetUrl)
            }

            override fun onReceivedError(
              view: WebView?,
              request: WebResourceRequest?,
              error: WebResourceError?,
            ) {
              if (request?.isForMainFrame == true) {
                updateTab(tabId) { cur ->
                  cur.copy(
                    isLoading = false,
                    errorDescription = error?.description?.toString() ?: "Unable to load page. Check internet connectivity.",
                  )
                }
              }
            }
          }
        webChromeClient =
          object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
              updateTab(tabId) { cur ->
                cur.copy(loadProgress = newProgress, isLoading = newProgress in 1..99)
              }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
              if (!title.isNullOrBlank()) {
                updateTab(tabId) { cur -> cur.copy(title = title) }
              }
            }
          }
      }
    }
  }

  fun navigateTab(tabId: String, rawQueryOrUrl: String) {
    focusManager.clearFocus()
    showBookmarksSheet = false
    val normalized = BrowserUrlNormalizer.normalizeInputToUrl(rawQueryOrUrl)
    addressInput = normalized
    updateTab(tabId) {
      it.copy(
        url = normalized,
        title = BrowserUrlNormalizer.displayHost(normalized),
        isSpeedDial = false,
        isLoading = true,
        loadProgress = 10,
        errorDescription = null,
      )
    }
    getOrCreateWebView(tabId).loadUrl(normalized)
  }

  fun goHomeSpeedDial() {
    showBookmarksSheet = false
    addressInput = ""
    updateTab(activeTab.id) {
      it.copy(
        title = "Start Page",
        isSpeedDial = true,
        isLoading = false,
        errorDescription = null,
      )
    }
  }

  fun addTab() {
    val newId = "tab_${UUID.randomUUID().toString().take(8)}"
    tabs = tabs + BrowserTabState(id = newId, title = "Start Page", isSpeedDial = true)
    activeTabIndex = tabs.lastIndex
    addressInput = ""
    showBookmarksSheet = false
  }

  fun closeTab(index: Int) {
    if (tabs.size <= 1) {
      goHomeSpeedDial()
      return
    }
    val removed = tabs[index]
    webViewMap.remove(removed.id)?.let { wv ->
      runCatching {
        (wv.parent as? ViewGroup)?.removeView(wv)
        wv.stopLoading()
        wv.destroy()
      }
    }
    val nextTabs = tabs.filterIndexed { i, _ -> i != index }
    tabs = nextTabs
    val nextIdx = activeTabIndex.coerceAtMost(nextTabs.lastIndex)
    activeTabIndex = nextIdx
    addressInput = nextTabs[nextIdx].url
  }

  BackHandler(enabled = !activeTab.isSpeedDial) {
    val wv = webViewMap[activeTab.id]
    if (wv != null && wv.canGoBack()) {
      wv.goBack()
    } else if (activeTab.isSpeedDial) {
      onCloseBrowser()
    } else {
      goHomeSpeedDial()
    }
  }

  val isCurrentBookmarked =
    !activeTab.isSpeedDial &&
      activeTab.url.isNotBlank() &&
      bookmarks.any { it.url.equals(activeTab.url, ignoreCase = true) }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF070B14))
        .padding(top = 30.dp, bottom = 52.dp)
        .testTag("browser_screen"),
  ) {
    // Tab Strip
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(Color(0xFF0B1220))
          .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        tabs.forEachIndexed { idx, tab ->
          val isSelected = idx == activeTabIndex
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isSelected) Color(0xFF17253D) else Color(0xFF101827),
            border =
              BorderStroke(
                1.dp,
                if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f),
              ),
            modifier =
              Modifier.clip(RoundedCornerShape(10.dp))
                .clickable {
                  activeTabIndex = idx
                  addressInput = tab.url
                  showBookmarksSheet = false
                }
                .testTag("browser_tab_$idx"),
          ) {
            Row(
              modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = tab.title.take(18),
                color = if (isSelected) Color.White else Color(0xFF9CA3AF),
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (tabs.size > 1) {
                Box(
                  modifier =
                    Modifier.size(22.dp)
                      .clip(CircleShape)
                      .clickable { closeTab(idx) },
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close tab",
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(13.dp),
                  )
                }
              }
            }
          }
        }
      }

      IconButton(
        onClick = { addTab() },
        modifier = Modifier.minimumInteractiveComponentSize().size(36.dp).testTag("browser_new_tab"),
      ) {
        Icon(
          imageVector = Icons.Filled.Add,
          contentDescription = "New browser tab",
          tint = Color(0xFF38BDF8),
        )
      }
    }

    // URL Bar & Controls
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(Color(0xFF0E1626))
          .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      IconButton(
        onClick = { goHomeSpeedDial() },
        modifier = Modifier.minimumInteractiveComponentSize().size(40.dp).testTag("browser_home_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.Home,
          contentDescription = "Browser start page",
          tint = if (activeTab.isSpeedDial) Color(0xFF38BDF8) else Color(0xFF9CA3AF),
          modifier = Modifier.size(20.dp),
        )
      }

      OutlinedTextField(
        value = addressInput,
        onValueChange = { addressInput = it },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { navigateTab(activeTab.id, addressInput) }),
        leadingIcon = {
          Icon(
            imageVector =
              if (activeTab.url.startsWith("https://")) Icons.Filled.Lock else Icons.Filled.Search,
            contentDescription = null,
            tint =
              if (activeTab.url.startsWith("https://")) Color(0xFF34D399) else Color(0xFF9CA3AF),
            modifier = Modifier.size(16.dp),
          )
        },
        trailingIcon = {
          if (addressInput.isNotEmpty()) {
            IconButton(
              onClick = { addressInput = "" },
              modifier = Modifier.size(28.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Clear address",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(15.dp),
              )
            }
          }
        },
        placeholder = {
          Text("Search or enter URL…", color = Color(0xFF6B7280), fontSize = 12.5.sp)
        },
        shape = RoundedCornerShape(18.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF151E32),
            unfocusedContainerColor = Color(0xFF151E32),
            focusedBorderColor = Color(0xFF38BDF8),
            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
          ),
        modifier = Modifier.weight(1f).testTag("browser_url_input"),
      )

      IconButton(
        onClick = {
          if (addressInput.isNotBlank()) {
            navigateTab(activeTab.id, addressInput)
          } else {
            navigateTab(activeTab.id, "https://duckduckgo.com")
          }
        },
        modifier = Modifier.minimumInteractiveComponentSize().size(42.dp).testTag("browser_go_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = "Go to URL",
          tint = Color(0xFF38BDF8),
        )
      }
    }

    // Navigation Toolbar (Back / Forward / Reload / Bookmark)
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(Color(0xFF0A101D))
          .padding(horizontal = 8.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = {
            val wv = webViewMap[activeTab.id]
            if (wv != null && wv.canGoBack()) wv.goBack() else goHomeSpeedDial()
          },
          enabled = !activeTab.isSpeedDial,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("browser_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Page back",
            tint = if (!activeTab.isSpeedDial) Color.White else Color(0xFF4B5563),
          )
        }

        IconButton(
          onClick = {
            val wv = webViewMap[activeTab.id]
            if (wv != null && wv.canGoForward()) {
              updateTab(activeTab.id) { it.copy(isSpeedDial = false) }
              wv.goForward()
            }
          },
          enabled = activeTab.canGoForward,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("browser_forward_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Page forward",
            tint = if (activeTab.canGoForward) Color.White else Color(0xFF4B5563),
          )
        }

        IconButton(
          onClick = {
            val wv = webViewMap[activeTab.id]
            if (activeTab.isLoading) {
              wv?.stopLoading()
              updateTab(activeTab.id) { it.copy(isLoading = false) }
            } else if (wv != null && activeTab.url.isNotBlank()) {
              updateTab(activeTab.id) { it.copy(errorDescription = null, isLoading = true) }
              wv.reload()
            }
          },
          enabled = !activeTab.isSpeedDial && activeTab.url.isNotBlank(),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("browser_reload_button"),
        ) {
          Icon(
            imageVector = if (activeTab.isLoading) Icons.Filled.Close else Icons.Filled.Refresh,
            contentDescription = if (activeTab.isLoading) "Stop loading" else "Reload page",
            tint = if (!activeTab.isSpeedDial) Color.White else Color(0xFF4B5563),
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (!activeTab.isSpeedDial && activeTab.url.isNotBlank()) {
          IconButton(
            onClick = {
              bookmarks =
                if (isCurrentBookmarked) {
                  removeBookmark(prefs, bookmarks, activeTab.url)
                } else {
                  addBookmark(
                    prefs,
                    bookmarks,
                    BrowserBookmark(
                      title = activeTab.title.ifBlank { BrowserUrlNormalizer.displayHost(activeTab.url) },
                      url = activeTab.url,
                    ),
                  )
                }
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("browser_bookmark_toggle"),
          ) {
            Icon(
              imageVector = Icons.Filled.Star,
              contentDescription = if (isCurrentBookmarked) "Remove bookmark" else "Save bookmark",
              tint = if (isCurrentBookmarked) Color(0xFFFBBF24) else Color(0xFF9CA3AF),
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (showBookmarksSheet) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF141D2F),
          border =
            BorderStroke(
              1.dp,
              if (showBookmarksSheet) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.10f),
            ),
          modifier =
            Modifier.clip(RoundedCornerShape(10.dp))
              .clickable { showBookmarksSheet = !showBookmarksSheet }
              .testTag("browser_bookmarks_button"),
        ) {
          Text(
            text = "Bookmarks (${bookmarks.size})",
            color = if (showBookmarksSheet) Color(0xFF38BDF8) else Color(0xFFE5E7EB),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
      }
    }

    if (activeTab.isLoading) {
      LinearProgressIndicator(
        progress = { (activeTab.loadProgress / 100f).coerceIn(0.05f, 1f) },
        color = Color(0xFF38BDF8),
        trackColor = Color(0xFF111827),
        modifier = Modifier.fillMaxWidth().height(3.dp),
      )
    } else {
      HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
    }

    // Main Content: Bookmarks Sheet, Start Page Speed Dial, Error Card, or Live WebView
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      when {
        showBookmarksSheet -> {
          LazyColumn(
            modifier = Modifier.fillMaxSize().background(Color(0xFF070B14)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            item {
              Text(
                text = "Saved Bookmarks",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp),
              )
            }
            items(bookmarks, key = { it.url }) { bm ->
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF11192B),
                border = BorderStroke(1.dp, Color(bm.accentHex).copy(alpha = 0.35f)),
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { navigateTab(activeTab.id, bm.url) },
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(bm.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(bm.url, color = Color(0xFF9CA3AF), fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  }
                  if (DEFAULT_BOOKMARKS.none { it.url == bm.url }) {
                    IconButton(
                      onClick = { bookmarks = removeBookmark(prefs, bookmarks, bm.url) },
                      modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                      Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Delete bookmark",
                        tint = Color(0xFF9CA3AF),
                      )
                    }
                  }
                }
              }
            }
            if (recentUrls.isNotEmpty()) {
              item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "Recent History",
                  color = Color.White,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(bottom = 4.dp),
                )
              }
              items(recentUrls, key = { "hist_${it.url}" }) { item ->
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = Color(0xFF0E1525),
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .clickable { navigateTab(activeTab.id, item.url) },
                ) {
                  Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(item.title, color = Color(0xFFE5E7EB), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.url, color = Color(0xFF6B7280), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  }
                }
              }
            }
          }
        }

        activeTab.isSpeedDial -> {
          LazyColumn(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.verticalGradient(listOf(Color(0xFF0A1428), Color(0xFF060912)))
                )
                .testTag("browser_speed_dial"),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            item {
              Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                BrowserAppIcon(iconSize = 58.dp, cornerRadius = 16.dp)
                Text(
                  text = "Internet Browser",
                  color = Color.White,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "Real WebView browser with tabs, HTTPS search & bookmarks",
                  color = Color(0xFF9CA3AF),
                  fontSize = 12.sp,
                )
              }
            }

            item {
              Text(
                text = "Quick Sites",
                color = Color(0xFFE5E7EB),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
              )
              Spacer(modifier = Modifier.height(8.dp))
              FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                bookmarks.forEach { bm ->
                  val chipAccent = Color(bm.accentHex)
                  Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF10192D),
                    border = BorderStroke(1.dp, chipAccent.copy(alpha = 0.4f)),
                    modifier =
                      Modifier.width(104.dp)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { navigateTab(activeTab.id, bm.url) },
                  ) {
                    Column(
                      modifier = Modifier.padding(10.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Box(
                        modifier =
                          Modifier.size(34.dp)
                            .clip(CircleShape)
                            .background(chipAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                      ) {
                        Text(
                          text = bm.title.take(1).uppercase(Locale.US),
                          color = chipAccent,
                          fontSize = 15.sp,
                          fontWeight = FontWeight.ExtraBold,
                        )
                      }
                      Text(
                        text = bm.title,
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                }
              }
            }

            if (recentUrls.isNotEmpty()) {
              item {
                Text(
                  text = "Recently Visited",
                  color = Color(0xFFE5E7EB),
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                )
              }
              items(recentUrls.take(5), key = { "sd_${it.url}" }) { item ->
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = Color(0xFF0E1628),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .clickable { navigateTab(activeTab.id, item.url) },
                ) {
                  Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.url, color = Color(0xFF6B7280), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  }
                }
              }
            }
          }
        }

        else -> {
          val activeWv = remember(activeTab.id) { getOrCreateWebView(activeTab.id) }
          AndroidView(
            factory = { ctx ->
              FrameLayout(ctx).apply {
                (activeWv.parent as? ViewGroup)?.removeView(activeWv)
                addView(activeWv)
              }
            },
            update = { container ->
              val currentChild = container.getChildAt(0)
              if (currentChild !== activeWv) {
                container.removeAllViews()
                (activeWv.parent as? ViewGroup)?.removeView(activeWv)
                container.addView(activeWv)
              }
            },
            modifier = Modifier.fillMaxSize().testTag("browser_webview"),
          )

          val err = activeTab.errorDescription
          if (err != null) {
            Box(
              modifier = Modifier.fillMaxSize().background(Color(0xEE080C16)).padding(24.dp),
              contentAlignment = Alignment.Center,
            ) {
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF121A2B),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(20.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text(
                    text = "Page Could Not Load",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = err,
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.5.sp,
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                      onClick = { goHomeSpeedDial() },
                      modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                      Text("Start Page", color = Color.White)
                    }
                    Button(
                      onClick = {
                        updateTab(activeTab.id) { it.copy(errorDescription = null, isLoading = true) }
                        activeWv.reload()
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color.Black),
                      modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                      Text("Retry", fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

private fun loadBookmarks(prefs: android.content.SharedPreferences): List<BrowserBookmark> {
  val raw = prefs.getString("bookmarks_json", null) ?: return DEFAULT_BOOKMARKS
  return runCatching {
    val arr = JSONArray(raw)
    buildList {
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
          BrowserBookmark(
            title = o.getString("title"),
            url = o.getString("url"),
            accentHex = o.optLong("color", 0xFF38BDF8L),
          )
        )
      }
    }.ifEmpty { DEFAULT_BOOKMARKS }
  }.getOrDefault(DEFAULT_BOOKMARKS)
}

private fun saveBookmarks(prefs: android.content.SharedPreferences, list: List<BrowserBookmark>) {
  val arr = JSONArray()
  list.forEach { b ->
    arr.put(
      JSONObject().apply {
        put("title", b.title)
        put("url", b.url)
        put("color", b.accentHex)
      }
    )
  }
  prefs.edit().putString("bookmarks_json", arr.toString()).apply()
}

private fun addBookmark(
  prefs: android.content.SharedPreferences,
  current: List<BrowserBookmark>,
  item: BrowserBookmark,
): List<BrowserBookmark> {
  if (current.any { it.url.equals(item.url, ignoreCase = true) }) return current
  val next = listOf(item) + current
  saveBookmarks(prefs, next)
  return next
}

private fun removeBookmark(
  prefs: android.content.SharedPreferences,
  current: List<BrowserBookmark>,
  url: String,
): List<BrowserBookmark> {
  val next = current.filterNot { it.url.equals(url, ignoreCase = true) }
  saveBookmarks(prefs, next)
  return next
}

private fun loadRecentUrls(prefs: android.content.SharedPreferences): List<BrowserBookmark> {
  val raw = prefs.getString("recent_urls_json", null) ?: return emptyList()
  return runCatching {
    val arr = JSONArray(raw)
    buildList {
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(BrowserBookmark(title = o.getString("title"), url = o.getString("url")))
      }
    }
  }.getOrDefault(emptyList())
}

private fun recordRecentUrl(
  prefs: android.content.SharedPreferences,
  current: List<BrowserBookmark>,
  title: String,
  url: String,
): List<BrowserBookmark> {
  if (url.isBlank() || url == "about:blank") return current
  val next =
    (listOf(BrowserBookmark(title = title.take(40), url = url)) + current.filterNot { it.url == url })
      .take(15)
  val arr = JSONArray()
  next.forEach { h ->
    arr.put(JSONObject().apply { put("title", h.title); put("url", h.url) })
  }
  prefs.edit().putString("recent_urls_json", arr.toString()).apply()
  return next
}
