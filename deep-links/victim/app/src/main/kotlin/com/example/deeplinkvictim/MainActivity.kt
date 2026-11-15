package com.example.deeplinkvictim

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent

class MainActivity : Activity() {
  private lateinit var statusText: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    statusText = findViewById(R.id.status_text)
    findViewById<Button>(R.id.start_login_button).setOnClickListener { startLogin() }
    findViewById<Button>(R.id.cancel_login_button).setOnClickListener {
      PendingLoginStore.cancel()
      statusText.setText(R.string.login_cancelled)
    }
    showOutcome(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    showOutcome(intent)
  }

  override fun onResume() {
    super.onResume()
    PendingLoginStore.expireIfNeeded(SystemClock.elapsedRealtime())
  }

  private fun startLogin() {
    val flow = when (findViewById<RadioGroup>(R.id.flow_group).checkedRadioButtonId) {
      R.id.hardened_flow_button -> LoginFlow.HARDENED_CODE
      else -> LoginFlow.VULNERABLE_TOKEN
    }
    val pending = LoginRequestFactory.create(flow, SystemClock.elapsedRealtime())
    PendingLoginStore.replace(pending)
    val authorizationUri = Uri.Builder()
      .scheme("http")
      .encodedAuthority("10.0.2.2:8765")
      .path("/authorize")
      .appendQueryParameter("client_id", "andsec-victim")
      .appendQueryParameter("redirect_uri", "andsec-login://auth/callback")
      .appendQueryParameter("response_type", flow.responseType)
      .appendQueryParameter("state", pending.state)
      .apply {
        if (pending.codeChallenge != null) {
          appendQueryParameter("code_challenge", pending.codeChallenge)
          appendQueryParameter("code_challenge_method", "S256")
        }
      }
      .build()
    try {
      when (findViewById<RadioGroup>(R.id.user_agent_group).checkedRadioButtonId) {
        R.id.external_browser_button -> startActivity(Intent(Intent.ACTION_VIEW, authorizationUri))
        else -> CustomTabsIntent.Builder().build().launchUrl(this, authorizationUri)
      }
      statusText.setText(R.string.waiting_for_login)
    } catch (_: ActivityNotFoundException) {
      PendingLoginStore.cancel()
      statusText.setText(R.string.browser_unavailable)
    } catch (_: SecurityException) {
      PendingLoginStore.cancel()
      statusText.setText(R.string.browser_unavailable)
    }
  }

  private fun showOutcome(intent: Intent) {
    when (intent.getStringExtra(EXTRA_OUTCOME)) {
      OUTCOME_SUCCESS -> statusText.setText(R.string.login_success)
      OUTCOME_REJECTED -> statusText.setText(R.string.callback_rejected)
      OUTCOME_FAILED -> statusText.setText(R.string.login_failed)
    }
  }

  companion object {
    const val EXTRA_OUTCOME = "com.example.deeplinkvictim.OUTCOME"
    const val OUTCOME_SUCCESS = "success"
    const val OUTCOME_REJECTED = "rejected"
    const val OUTCOME_FAILED = "failed"
  }
}
