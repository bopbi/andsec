package com.example.providervictim

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.core.content.FileProvider

class MainActivity : Activity() {
  private lateinit var statusText: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    DemoReport.ensureCreated(this)
    statusText = findViewById(R.id.status_text)
    findViewById<Button>(R.id.share_hardened_button).setOnClickListener {
      shareHardenedReport()
    }
    findViewById<Button>(R.id.revoke_hardened_button).setOnClickListener {
      revokeHardenedReport()
    }
  }

  private fun shareHardenedReport() {
    val uri = hardenedReportUri()
    val intent = Intent(ACTION_SHARE_REPORT)
      .setClassName(ATTACKER_PACKAGE, ATTACKER_ACTIVITY)
      .setDataAndType(uri, "text/plain")
      .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      .apply {
        clipData = ClipData.newUri(contentResolver, "Synthetic report", uri)
      }
    try {
      startActivity(intent)
      statusText.setText(R.string.hardened_share_sent)
      Log.i(PROVIDER_LAB_TAG, "hardened-share-sent")
    } catch (_: ActivityNotFoundException) {
      statusText.setText(R.string.attacker_unavailable)
      Log.i(PROVIDER_LAB_TAG, "hardened-share-unavailable")
    }
  }

  private fun revokeHardenedReport() {
    revokeUriPermission(hardenedReportUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION)
    statusText.setText(R.string.hardened_access_revoked)
    Log.i(PROVIDER_LAB_TAG, "hardened-access-revoked")
  }

  private fun hardenedReportUri(): Uri =
    FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, DemoReport.ensureCreated(this))

  companion object {
    private const val FILE_PROVIDER_AUTHORITY = "com.example.providervictim.fileprovider"
    private const val ACTION_SHARE_REPORT = "com.example.providervictim.action.SHARE_REPORT"
    private const val ATTACKER_PACKAGE = "com.example.providerattacker"
    private const val ATTACKER_ACTIVITY = "com.example.providerattacker.MainActivity"
    private const val PROVIDER_LAB_TAG = "ProviderLab"
  }
}
