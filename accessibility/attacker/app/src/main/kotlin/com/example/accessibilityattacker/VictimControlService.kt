package com.example.accessibilityattacker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class VictimControlService : AccessibilityService() {
  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event?.packageName?.toString() != VICTIM_PACKAGE) {
      logWrongPackageIfArmed()
      return
    }

    val root = rootInActiveWindow
    if (root?.packageName?.toString() != VICTIM_PACKAGE) {
      logWrongPackageIfArmed()
      root?.recycle()
      return
    }

    when (DemoSessionState.consume()) {
      DemoSessionState.ConsumeResult.NOT_ARMED -> {
        root.recycle()
        return
      }
      DemoSessionState.ConsumeResult.EXPIRED -> {
        Log.i(TAG, "Armed click expired; no action attempted")
        root.recycle()
        return
      }
      DemoSessionState.ConsumeResult.ARMED -> Unit
    }

    val matches = root.findAccessibilityNodeInfosByViewId(TARGET_VIEW_ID)
    val target = matches.firstOrNull {
      it.isImportantForAccessibility && it.isVisibleToUser && it.isEnabled && it.isClickable
    }
    if (target == null) {
      Log.w(TAG, "Victim demo button unavailable; no action attempted")
    } else if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
      Log.i(TAG, "Victim demo button click succeeded")
    } else {
      Log.w(TAG, "Victim demo button click rejected")
    }
    matches.forEach { it.recycle() }
    root.recycle()
  }

  override fun onInterrupt() = Unit

  override fun onUnbind(intent: Intent?): Boolean {
    DemoSessionState.cancel()
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    DemoSessionState.cancel()
    super.onDestroy()
  }

  private fun logWrongPackageIfArmed() {
    if (DemoSessionState.hasRequest()) {
      Log.w(TAG, "Non-victim window ignored; no action attempted")
    }
  }

  private companion object {
    const val TAG = "A11yLab"
    const val VICTIM_PACKAGE = "com.example.accessibilityvictim"
    const val TARGET_VIEW_ID = "com.example.accessibilityvictim:id/demo_action_button"
  }
}
