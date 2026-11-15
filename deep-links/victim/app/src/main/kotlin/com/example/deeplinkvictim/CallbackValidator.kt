package com.example.deeplinkvictim

import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest

sealed interface CallbackValidation {
  data class Accepted(val request: PendingLogin, val responseValue: String) : CallbackValidation
  data object Rejected : CallbackValidation
}

object CallbackValidator {
  fun validate(
    callback: URI,
    pending: PendingLogin,
    nowElapsedMillis: Long,
  ): CallbackValidation {
    if (nowElapsedMillis >= pending.expiresAtElapsedMillis) return CallbackValidation.Rejected
    if (
      callback.scheme != "andsec-login" ||
      callback.host != "auth" ||
      callback.path != "/callback" ||
      callback.port != -1 ||
      callback.userInfo != null ||
      callback.fragment != null
    ) {
      return CallbackValidation.Rejected
    }
    val parameters = parseQuery(callback.rawQuery) ?: return CallbackValidation.Rejected
    val responseKey = when (pending.flow) {
      LoginFlow.VULNERABLE_TOKEN -> "access_token"
      LoginFlow.HARDENED_CODE -> "code"
    }
    if (parameters.keys != setOf("state", responseKey)) return CallbackValidation.Rejected
    if (parameters.values.any { it.size != 1 }) return CallbackValidation.Rejected
    val state = parameters.getValue("state").single()
    val expectedState = pending.state.toByteArray(Charsets.UTF_8)
    if (!MessageDigest.isEqual(expectedState, state.toByteArray(Charsets.UTF_8))) {
      return CallbackValidation.Rejected
    }
    val responseValue = parameters.getValue(responseKey).single()
    if (responseValue.length !in 16..512) return CallbackValidation.Rejected
    return CallbackValidation.Accepted(pending, responseValue)
  }

  private fun parseQuery(rawQuery: String?): Map<String, List<String>>? {
    if (rawQuery.isNullOrEmpty()) return null
    val parameters = mutableMapOf<String, MutableList<String>>()
    try {
      for (component in rawQuery.split('&')) {
        val separator = component.indexOf('=')
        if (separator <= 0) return null
        val key = URLDecoder.decode(component.substring(0, separator), Charsets.UTF_8.name())
        val value = URLDecoder.decode(component.substring(separator + 1), Charsets.UTF_8.name())
        parameters.getOrPut(key) { mutableListOf() }.add(value)
      }
    } catch (_: IllegalArgumentException) {
      return null
    }
    return parameters
  }
}
