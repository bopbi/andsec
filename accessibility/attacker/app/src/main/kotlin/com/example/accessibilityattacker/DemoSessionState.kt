package com.example.accessibilityattacker

import android.os.SystemClock

internal object DemoSessionState {
  private const val ARM_WINDOW_MS = 30_000L
  private var armedUntilElapsedRealtime = 0L

  fun arm() {
    armedUntilElapsedRealtime = SystemClock.elapsedRealtime() + ARM_WINDOW_MS
  }

  fun cancel() {
    armedUntilElapsedRealtime = 0L
  }

  fun hasRequest(): Boolean = armedUntilElapsedRealtime != 0L

  fun consume(): ConsumeResult {
    val deadline = armedUntilElapsedRealtime
    armedUntilElapsedRealtime = 0L
    return when {
      deadline == 0L -> ConsumeResult.NOT_ARMED
      SystemClock.elapsedRealtime() >= deadline -> ConsumeResult.EXPIRED
      else -> ConsumeResult.ARMED
    }
  }

  enum class ConsumeResult {
    ARMED,
    EXPIRED,
    NOT_ARMED,
  }
}
