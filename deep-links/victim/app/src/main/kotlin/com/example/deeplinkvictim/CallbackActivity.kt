package com.example.deeplinkvictim

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import java.net.URI
import java.net.URISyntaxException
import kotlin.concurrent.thread

class CallbackActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_callback)
    handleCallback(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleCallback(intent)
  }

  private fun handleCallback(intent: Intent) {
    val data = intent.data
    val callback = try {
      if (data == null) null else URI(data.toString())
    } catch (_: URISyntaxException) {
      null
    }
    val validation = if (callback == null) {
      CallbackValidation.Rejected
    } else {
      PendingLoginStore.consumeValidCallback(callback, SystemClock.elapsedRealtime())
    }
    if (validation !is CallbackValidation.Accepted) {
      returnOutcome(MainActivity.OUTCOME_REJECTED)
      return
    }
    thread(name = "VictimLabLogin") {
      val succeeded = ProviderApi.completeLogin(validation)
      runOnUiThread {
        returnOutcome(if (succeeded) MainActivity.OUTCOME_SUCCESS else MainActivity.OUTCOME_FAILED)
      }
    }
  }

  private fun returnOutcome(outcome: String) {
    val mainIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(MainActivity.EXTRA_OUTCOME, outcome)
    }
    startActivity(mainIntent)
    finish()
  }
}
