# Deep Links and App Links Labs

This directory contains a controlled browser-login redirect lab using only synthetic apps, a fixed `Demo User` profile, and a local mock provider. It compares a vulnerable front-channel access-token response with a one-time authorization code protected by PKCE.

**Chrome Custom Tabs, external browsers, and custom URI schemes are intended platform capabilities. This lab does not demonstrate an Android or browser vulnerability.** The vulnerable result comes from placing a usable token in a redirect that another app can receive. The hardened result comes from provider-enforced PKCE; it does not prevent interception or denial of that login.

## Components

- `victim/` — Victim Lab, with vulnerable and hardened variants and Chrome Custom Tab or external-browser launch choices.
- `attacker/` — Attacker Lab, a separately installed app claiming the same custom scheme. It retains one intercepted response in memory and tests it only after an explicit button press.
- `mock-provider/` — a dependency-free Python 3 authorization and profile server bound to `127.0.0.1:8765`.
- `plan/` — the threat model, implementation plan, and execution record.

Both apps register only `andsec-login://auth/callback` for this demonstration. They use `http://10.0.2.2:8765` for the emulator-only provider connection. Their network security configuration denies cleartext traffic elsewhere.

## Build and static checks

Use each standalone project's Gradle wrapper:

```bash
cd deep-links/victim && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
cd deep-links/attacker && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The projects also retain Views/Espresso startup tests. On a connected dedicated emulator, run:

```bash
cd deep-links/victim && ./gradlew :app:connectedDebugAndroidTest
cd deep-links/attacker && ./gradlew :app:connectedDebugAndroidTest
```

Merged manifests can be inspected under each project's `app/build/intermediates/merged_manifests/debug/` after assembly.

## Emulator setup

1. Use a dedicated emulator with a browser installed. The emulator reaches the host-only provider through `10.0.2.2`; this configuration is not intended for a physical device.
2. Start the provider and leave it running:

   ```bash
   python3 deep-links/mock-provider/server.py
   ```

3. Build and install both debug APKs, substituting the emulator serial:

   ```bash
   android install --device=<serial> deep-links/victim/app/build/outputs/apk/debug/app-debug.apk
   android install --device=<serial> deep-links/attacker/app/build/outputs/apk/debug/app-debug.apk
   ```

4. Record Android's API level and the installed browser/Chrome version. Keep the provider terminal open; it deliberately does not log request URIs, codes, tokens, `state`, PKCE values, or authorization headers.

## Login scenarios

### Baseline routed to Victim Lab

1. Open Victim Lab and select either **Vulnerable token redirect** or **Hardened code + PKCE**.
2. Select **Chrome Custom Tab** or **External browser**, then tap **Start synthetic login**.
3. On the local page, tap **Sign in as Demo User**.
4. If Android asks which app should open the callback, select Victim Lab. The victim must display only `Signed in as Demo User.`

### Interception control routed to Attacker Lab

1. Start either variant as above and tap **Sign in as Demo User**.
2. If Android presents a chooser, explicitly select Attacker Lab. If a default already routes elsewhere, clear only that lab-created default or use an explicit callback intent for the negative control.
3. Attacker Lab displays **Intercepted lab response** without showing callback values. Tap **Test intercepted response** exactly once.
4. For the vulnerable variant, the expected outcome is `Provider returned Demo User.` For the hardened variant, the expected outcome is the provider's fixed `invalid_grant` rejection and no profile.

Explicitly choosing Attacker Lab proves the impact if it receives a callback. It is not evidence that installing the app automatically hijacks every login. Record chooser and default routing separately.

### Callback and provider controls

The pending victim request expires after 120 seconds. Cancellation, replacement by a new request, process death, and a consumed valid callback invalidate the previous pending state. Callback validation requires the exact scheme, host, path, expected response parameter set, and matching `state`; response values are never included in outcome intents.

Wrong-component controls can be delivered directly to the dedicated victim component with synthetic values, without relying on intent-filter matching:

```bash
adb -s <serial> shell am start -n com.example.deeplinkvictim/.CallbackActivity \
  -a android.intent.action.VIEW \
  -d 'andsec-login://other/callback?code=1234567890abcdef&state=wrong-state'
```

Use equivalent explicit intents for an unexpected path or response type. Provider replay and wrong/missing-verifier controls use `/token` with `grant_type=authorization_code`, client ID `andsec-victim`, and redirect URI `andsec-login://auth/callback`. Never put real credentials or non-lab values into these commands.

## Verification results

Tested on **2026-11-15** with emulator **Medium_Phone_API_36.1**, **Android 16, API 36**, and **Chrome 148.0.7778.178**. Both apps' assemble, unit-test, and lint tasks passed. Five victim JVM tests, two attacker JVM tests, and one Views/Espresso startup test per app passed. Merged-manifest inspection confirmed the intended exported callback components, distinct application IDs, and cleartext access limited to `10.0.2.2`.

| Scenario | Expected result | Observed result |
| --- | --- | --- |
| Vulnerable baseline, Custom Tab | Victim displays only `Demo User` | **Pass:** Chrome Custom Tab returned to the victim and displayed `Signed in as Demo User.` |
| Vulnerable baseline, external browser | Victim displays only `Demo User` | **Pass:** External Chrome returned to the victim with the same fixed profile. |
| Hardened baseline, Custom Tab | Victim displays `Demo User`; code replay fails | **Pass:** Victim exchange succeeded and displayed the fixed profile; replay is recorded separately below. |
| Hardened baseline, external browser | Victim displays `Demo User`; code replay fails | **Pass:** External Chrome completed the victim exchange and displayed the fixed profile. |
| Vulnerable interception | One attacker test retrieves only the fixed profile | **Pass:** Explicit attacker selection in a Custom Tab flow led to one `Demo User` response; a second tap did not change the consumed result. |
| Hardened interception | Code redemption without the verifier fails | **Pass:** Explicit attacker selection produced a redacted response; its test returned `invalid_grant` and no profile. |
| Post-interception legitimate control | A fresh victim code exchange succeeds | **Pass:** A fresh hardened Custom Tab login routed to the victim displayed `Demo User`. |
| Wrong `state` | Victim rejects without redemption or profile | **Pass:** Explicit wrong-state callback produced the fixed rejection. |
| Replayed state/callback | Victim rejects the consumed callback | **Pass:** A previously consumed browser callback was rejected on redelivery. |
| Wrong or missing verifier | Provider returns fixed rejection | **Pass:** Direct local requests with an unused code and absent or incorrect verifier both returned HTTP 400 `invalid_grant`; the correct verifier then succeeded. |
| Code replay | Provider rejects reuse | **Pass:** Repeating the identical successful direct exchange returned HTTP 400 `invalid_grant`. |
| Wrong callback components | Victim rejects without navigation or redemption | **Pass:** Explicit wrong-host, wrong-path, and wrong-response-type callbacks each produced the fixed rejection. |
| Expired or cancelled request | Victim rejects the late callback | **Pass:** Cancellation rejected the later callback; after 120 seconds, a callback with the original request's state was also rejected. |
| Process-death control | No pending request or secret is restored | **Pass:** After force-stop, the old-style callback restarted the victim and was rejected without a profile. |
| Routing observation | Chooser/default behavior recorded separately | **Pass:** Android showed a chooser on the observed callbacks. No **Always** selection or default association was created; attacker rows used explicit selection. |

App-process logs and provider output were inspected after the interception rows. They contained no full callback URI, token, code, `state`, verifier, challenge, or authorization header. The provider was stopped and both lab apps were uninstalled after the matrix.

## Secure development checklist

When implementing browser-based native-app authorization:

- Prefer an authorization-code flow with PKCE using S256, enforced by the authorization server. Do not return access tokens or other bearer secrets in a front-channel redirect.
- Generate a fresh, cryptographically random `state` for each request, bind it to that pending login, expire it, and consume it before accepting a response. Use an OpenID Connect `nonce` as well when validating ID tokens.
- Validate the exact callback scheme, host, path, response type, expected parameters, and pending request. Never navigate to a URL supplied by an untrusted callback.
- Make authorization codes short-lived and one-time, and bind them to the client, exact redirect URI, and PKCE challenge on the server.
- Use verified HTTPS App Links where their routing properties fit the app, but do not treat them as substitutes for `state`, PKCE, callback validation, or server-side checks.
- Keep codes, tokens, verifiers, full callback URIs, and authorization headers out of logs and analytics. Test interception, wrong or replayed `state`, wrong callback components, expired requests, incorrect verifiers, and code replay alongside legitimate login.

## Limitations and related defenses

- PKCE protects an intercepted code at the token endpoint. It does not stop callback interception, select the receiving app, replace `state`, or guarantee login availability.
- Both variants validate fresh `state`; this lab deliberately does not combine token interception with a missing-`state` defect.
- This is an OAuth-style demonstration without OpenID Connect ID tokens. OpenID Connect `nonce` is a separate request-binding concern and is not tested.
- Verified HTTPS App Links are a related routing defense, not a substitute for callback validation, `state`, or PKCE. Meaningful App Link verification needs a controlled HTTPS domain and Digital Asset Links, so it is deferred to a separate lab.
- The provider is minimal educational software, not a production authorization server. It stores all values in memory and expires codes and tokens after 120 seconds.

## Cleanup

Stop the provider with Ctrl-C. Clear any default association created for either lab app in Android settings, uninstall only the lab packages, and confirm no lab process remains:

```bash
adb -s <serial> uninstall com.example.deeplinkattacker
adb -s <serial> uninstall com.example.deeplinkvictim
```

## References

- [Android App Links](https://developer.android.com/training/app-links)
- [Chrome Custom Tabs](https://developer.chrome.com/docs/android/custom-tabs)
- [RFC 8252: OAuth 2.0 for Native Apps](https://www.rfc-editor.org/rfc/rfc8252)
- [RFC 7636: Proof Key for Code Exchange](https://www.rfc-editor.org/rfc/rfc7636)
- [RFC 9700: Best Current Practice for OAuth 2.0 Security](https://www.rfc-editor.org/rfc/rfc9700)
