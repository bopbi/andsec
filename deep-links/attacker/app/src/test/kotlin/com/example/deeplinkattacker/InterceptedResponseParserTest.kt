package com.example.deeplinkattacker

import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InterceptedResponseParserTest {
  @Test
  fun acceptsOnlyExactTokenOrCodeResponses() {
    assertEquals(
      ResponseKind.ACCESS_TOKEN,
      parse("andsec-login://auth/callback?access_token=1234567890abcdef&state=synthetic")?.kind,
    )
    assertEquals(
      ResponseKind.AUTHORIZATION_CODE,
      parse("andsec-login://auth/callback?code=1234567890abcdef&state=synthetic")?.kind,
    )
  }

  @Test
  fun rejectsWrongComponentsExtrasAndDuplicateValues() {
    assertNull(parse("andsec-login://other/callback?code=1234567890abcdef&state=synthetic"))
    assertNull(parse("andsec-login://auth/callback?code=1234567890abcdef&state=synthetic&error=denied"))
    assertNull(parse("andsec-login://auth/callback?code=1234567890abcdef&state=one&state=two"))
    assertNull(parse("https://auth/callback?code=1234567890abcdef&state=synthetic"))
  }

  private fun parse(value: String) = InterceptedResponseParser.parse(URI(value))
}
