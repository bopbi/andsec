package com.example.deeplinkvictim

import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CallbackValidatorTest {
  @Test
  fun acceptsExactTokenAndCodeCallbacks() {
    assertTrue(validate("access_token=1234567890abcdef") is CallbackValidation.Accepted)
    assertTrue(
      validate("code=1234567890abcdef", LoginFlow.HARDENED_CODE) is CallbackValidation.Accepted
    )
  }

  @Test
  fun rejectsWrongStateAndUnexpectedParameters() {
    assertEquals(CallbackValidation.Rejected, validate("access_token=1234567890abcdef", state = "unexpected-state"))
    assertEquals(CallbackValidation.Rejected, validate("access_token=1234567890abcdef&error=access_denied"))
    assertEquals(CallbackValidation.Rejected, validate("access_token=1234567890abcdef&state=expected-state"))
  }

  @Test
  fun rejectsWrongComponentsAndResponseType() {
    assertEquals(CallbackValidation.Rejected, validate("code=1234567890abcdef"))
    assertEquals(CallbackValidation.Rejected, validate("access_token=1234567890abcdef", host = "other"))
    assertEquals(CallbackValidation.Rejected, validate("access_token=1234567890abcdef", path = "/other"))
    assertEquals(CallbackValidation.Rejected, validate("access_token=1234567890abcdef", scheme = "https"))
  }

  @Test
  fun rejectsExpiredCallbackAndConsumesAcceptedCallback() {
    val pending = pendingLogin()
    assertEquals(
      CallbackValidation.Rejected,
      CallbackValidator.validate(callback("access_token=1234567890abcdef"), pending, 2_000L),
    )
    PendingLoginStore.replace(pending)
    val accepted = PendingLoginStore.consumeValidCallback(callback("access_token=1234567890abcdef"), 1_000L)
    assertTrue(accepted is CallbackValidation.Accepted)
    assertEquals(
      CallbackValidation.Rejected,
      PendingLoginStore.consumeValidCallback(callback("access_token=1234567890abcdef"), 1_001L),
    )
  }

  @Test
  fun createsPkceVerifierAndS256Challenge() {
    val pending = LoginRequestFactory.create(LoginFlow.HARDENED_CODE, 1_000L)
    assertTrue(pending.codeVerifier != null)
    assertEquals(LoginRequestFactory.s256Challenge(pending.codeVerifier!!), pending.codeChallenge)
    assertEquals(
      "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
      LoginRequestFactory.s256Challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
    )
  }

  private fun validate(
    response: String,
    flow: LoginFlow = LoginFlow.VULNERABLE_TOKEN,
    state: String? = "expected-state",
    host: String = "auth",
    path: String = "/callback",
    scheme: String = "andsec-login",
  ): CallbackValidation {
    val stateQuery = state?.let { "&state=$it" }.orEmpty()
    val callback = URI("$scheme://$host$path?$response$stateQuery")
    return CallbackValidator.validate(callback, pendingLogin(flow), 1_000L)
  }

  private fun callback(query: String) = URI("andsec-login://auth/callback?$query&state=expected-state")

  private fun pendingLogin(flow: LoginFlow = LoginFlow.VULNERABLE_TOKEN) = PendingLogin(
    flow = flow,
    state = "expected-state",
    codeVerifier = if (flow == LoginFlow.HARDENED_CODE) "test-verifier" else null,
    codeChallenge = null,
    expiresAtElapsedMillis = 1_500L,
  )
}
