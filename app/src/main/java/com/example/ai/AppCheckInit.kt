package com.example.ai

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
  try {
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }

    if (BuildConfig.DEBUG) {
      val persistenceKey = FirebaseApp.getInstance().persistenceKey
      val prefs =
        context.getSharedPreferences(
          "$DEBUG_STORE_PREFIX$persistenceKey",
          Context.MODE_PRIVATE,
        )
      val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)

      if (!debugToken.isNullOrEmpty()) {
        prefs.edit().putString(DEBUG_SECRET_KEY, debugToken).commit()
        FirebaseAppCheck.getInstance()
          .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        Log.d(TAG, "DebugAppCheckProviderFactory installed with intent debug token.")
      } else if (!prefs.getString(DEBUG_SECRET_KEY, null).isNullOrEmpty()) {
        FirebaseAppCheck.getInstance()
          .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        Log.d(TAG, "DebugAppCheckProviderFactory installed with persisted debug token.")
      } else {
        // Sideloaded debug APK outside AI Studio harness: use reCAPTCHA Enterprise when configured
        if (!installRecaptchaProvider(context)) {
          FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
          Log.w(TAG, "No debug token or reCAPTCHA key; installed DebugAppCheckProviderFactory.")
        }
      }
    } else {
      installRecaptchaProvider(context)
    }
  } catch (e: Exception) {
    Log.e(TAG, "Failed to initialize Firebase App Check", e)
  }
}

private fun installRecaptchaProvider(context: Context): Boolean {
  val resId =
    context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
  if (resId == 0) {
    Log.e(TAG, "recaptcha_site_key string resource not found in package.")
    return false
  }
  val siteKey = context.getString(resId)
  if (siteKey.isBlank()) {
    Log.e(TAG, "recaptcha_site_key resource string is blank.")
    return false
  }
  FirebaseAppCheck.getInstance()
    .installAppCheckProviderFactory(RecaptchaAppCheckProviderFactory.getInstance(siteKey))
  Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
  return true
}
