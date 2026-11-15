package com.example.deeplinkattacker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import java.net.URI
import java.net.URISyntaxException
import kotlin.concurrent.thread

class MainActivity : Activity() {
  private lateinit var statusText: TextView
  private lateinit var testButton: Button
  private var interceptedResponse: InterceptedResponse? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    statusText = findViewById(R.id.status_text)
    testButton = findViewById(R.id.test_response_button)
    testButton.setOnClickListener { testInterceptedResponse() }
    receiveCallback(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    receiveCallback(intent)
  }

  private fun receiveCallback(intent: Intent) {
    if (intent.action != Intent.ACTION_VIEW) return
    interceptedResponse = null
    testButton.isEnabled = false
    val callback = try {
      intent.data?.let { URI(it.toString()) }
    } catch (_: URISyntaxException) {
      null
    }
    val response = callback?.let(InterceptedResponseParser::parse)
    if (response == null) {
      statusText.setText(R.string.invalid_intercepted_response)
      return
    }
    interceptedResponse = response
    statusText.setText(R.string.intercepted_response)
    testButton.isEnabled = true
  }

  private fun testInterceptedResponse() {
    val response = interceptedResponse ?: return
    interceptedResponse = null
    testButton.isEnabled = false
    statusText.setText(R.string.testing_intercepted_response)
    thread(name = "AttackerLabTest") {
      val outcome = ProviderApi.test(response)
      runOnUiThread {
        statusText.setText(
          when (outcome) {
            TestOutcome.PROFILE_RECEIVED -> R.string.attacker_profile_received
            TestOutcome.CODE_REJECTED -> R.string.attacker_code_rejected
            TestOutcome.REQUEST_FAILED -> R.string.attacker_request_failed
          }
        )
      }
    }
  }
}
