package com.example.deeplinkvictim

import java.net.URI

object PendingLoginStore {
  private var pendingLogin: PendingLogin? = null

  @Synchronized
  fun replace(pendingLogin: PendingLogin) {
    this.pendingLogin = pendingLogin
  }

  @Synchronized
  fun cancel() {
    pendingLogin = null
  }

  @Synchronized
  fun expireIfNeeded(nowElapsedMillis: Long) {
    val pending = pendingLogin
    if (pending != null && nowElapsedMillis >= pending.expiresAtElapsedMillis) {
      pendingLogin = null
    }
  }

  @Synchronized
  fun consumeValidCallback(callback: URI, nowElapsedMillis: Long): CallbackValidation {
    val pending = pendingLogin ?: return CallbackValidation.Rejected
    val validation = CallbackValidator.validate(callback, pending, nowElapsedMillis)
    if (validation is CallbackValidation.Accepted) {
      pendingLogin = null
    } else if (nowElapsedMillis >= pending.expiresAtElapsedMillis) {
      pendingLogin = null
    }
    return validation
  }
}
