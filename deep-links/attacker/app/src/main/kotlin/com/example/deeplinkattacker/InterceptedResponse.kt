package com.example.deeplinkattacker

import java.net.URI
import java.net.URLDecoder

enum class ResponseKind {
  ACCESS_TOKEN,
  AUTHORIZATION_CODE,
}

data class InterceptedResponse(val kind: ResponseKind, val value: String)

object InterceptedResponseParser {
  fun parse(callback: URI): InterceptedResponse? {
    if (
      callback.scheme != "andsec-login" ||
      callback.host != "auth" ||
      callback.path != "/callback" ||
      callback.port != -1 ||
      callback.userInfo != null ||
      callback.fragment != null
    ) {
      return null
    }
    val parameters = parseQuery(callback.rawQuery) ?: return null
    val responseKey = when (parameters.keys) {
      setOf("state", "access_token") -> "access_token"
      setOf("state", "code") -> "code"
      else -> return null
    }
    if (parameters.values.any { it.size != 1 }) return null
    val state = parameters.getValue("state").single()
    val value = parameters.getValue(responseKey).single()
    if (state.isEmpty() || value.length !in 16..512) return null
    val kind = if (responseKey == "access_token") ResponseKind.ACCESS_TOKEN else ResponseKind.AUTHORIZATION_CODE
    return InterceptedResponse(kind, value)
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
