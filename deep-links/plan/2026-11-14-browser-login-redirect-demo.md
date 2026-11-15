# Browser Login Custom-Scheme Interception Lab

- **Status:** done
- **Created:** 2026-11-14
- **Source:** user request

## Goal

Build a controlled demonstration of a native Android app that starts a synthetic login in a Chrome Custom Tab or external browser and returns through a custom-scheme redirect. A second installed app will claim the same scheme and receive the callback when the user explicitly selects it. The vulnerable variant will expose a synthetic access token in the redirect; the hardened variant will use a one-time authorization code with PKCE so the intercepted code cannot be redeemed by the attacker.

The experiment will demonstrate redirect interception and the different impact of the two response designs. It will not claim that Chrome Custom Tabs, external browsers, or custom schemes themselves are Android vulnerabilities.

## Context

The repository currently has documentation but no implemented Android apps. The accessibility plan establishes the convention of independent applications with distinct application IDs, Kotlin and traditional Android Views, generated Gradle wrappers, and emulator verification. This lab will follow those conventions under `deep-links/`.

RFC 8252 recommends an external user agent, such as the system browser or Chrome Custom Tabs, for native-app authorization and requires PKCE for authorization-code flows. A custom URI scheme can be claimed by more than one installed app. Routing can depend on user selection and device defaults, so verification must explicitly select the attacker rather than claim that installation always hijacks the callback.

PKCE protects an intercepted authorization code at the token endpoint; it does not stop another app from receiving the callback, guarantee which app Android opens, replace `state`, or prevent denial of login. Verified HTTPS App Links are a related routing defense but are not part of this first lab because meaningful verification requires a controlled HTTPS domain and Digital Asset Links configuration.

## Non-goals

- No third-party apps, real identity providers, real accounts, credentials, payments, messages, personal data, or production tokens.
- No browser exploitation, Android platform vulnerability claim, App Link verification bypass, phishing, overlays, accessibility interaction, or network interception.
- No embedded WebView login, OpenID Connect implementation, broad deep-link scanner, or general-purpose OAuth client library demonstration.
- No persistent token store, background listener, boot receiver, stealth, obfuscation, anti-analysis behavior, or data exfiltration.
- No claim that PKCE prevents interception or that a verified App Link alone provides request/response binding.
- No automatic routing through the attacker as a precondition for success; use explicit user selection for a repeatable negative control.

## Minimal threat model

### Assets and trust boundaries

The only assets are a synthetic login session and a fixed lab profile named `Demo User`. The mock provider issues short-lived, one-time values and serves only local test traffic. The victim's pending request and PKCE verifier remain private to its in-memory state. Android, the browser, and the mock provider are trusted and unmodified.

The authorization response crosses from the browser through Android's URI routing into whichever app receives the callback. The token endpoint separately checks the authorization code, client identity, redirect URI, and PKCE verifier. Neither the callback handler nor the token endpoint alone establishes all of these properties.

### Attacker capabilities and preconditions

The attacker is a separately installed lab app with a distinct application ID. It can declare the same custom URI scheme, receive an explicitly routed intent, read that intent's URI, and call the local mock provider. It cannot read the victim's memory, modify the provider or browser, bypass Android routing, or intercept other network traffic.

Both apps must be installed and the user must initiate the lab login. The callback must be routed to the attacker through the system chooser or an already recorded user default. Tests must distinguish an observed default from an explicit test selection.

### Expected security properties

- In the vulnerable variant, a synthetic access token returned in the redirect is usable by the attacker that receives it.
- In the hardened variant, the attacker may receive the code but cannot redeem it without the victim's verifier.
- The legitimate victim can redeem its own code with the correct verifier, exact redirect URI, and matching request.
- Both variants reject an unexpected or replayed `state`; the provider rejects incorrect verifiers and reused codes.
- Interception may prevent the victim from completing that login. This availability effect must not be reported as account compromise in the hardened variant.

## Approach

Use two standalone Android projects, `deep-links/victim/` and `deep-links/attacker/`, plus a dependency-free local Python server in `deep-links/mock-provider/`. The emulator will reach the host server through `10.0.2.2`; any cleartext exception will be limited to that test endpoint. A physical-device setup is out of scope unless it has an equivalently isolated local configuration.

The mock provider will present a lab page with a single **Sign in as Demo User** button, not a credential form. The victim will offer vulnerable and hardened variants and a choice between Chrome Custom Tabs and the external browser. Both variants will generate fresh `state`; only the hardened variant will generate a PKCE verifier and S256 challenge.

The vulnerable variant models the deprecated front-channel token pattern by placing a short-lived synthetic access token and `state` in the custom-scheme callback. The hardened variant places a one-time authorization code and `state` in the callback. Both apps will register the same custom scheme, host, and path. The attacker will have an explicit button to test an intercepted response against the mock provider; it will not persist, forward, or automatically distribute it.

Alternatives considered and rejected:

- **One app with no attacker:** It could show callback handling but could not demonstrate the separate-app routing boundary or intercepted-token impact.
- **A public identity provider:** It would add credentials, network exposure, accounts, and provider-specific behavior without improving the core lesson.
- **Verified App Links in the first lab:** A controlled public HTTPS origin is required for meaningful verification. The first lab instead isolates custom-scheme interception and PKCE; App Links remain in the topic roadmap.
- **Combining missing `state` with token interception:** Multiple simultaneous defects would make the cause of each observed result less clear. Both variants therefore validate `state` from the start.

## Implementation quality

- **Responsibility and integration:** The victim owns request creation, in-memory pending-login state, browser launch, exact callback validation, and hardened code exchange. The attacker owns only receipt and explicit testing of its intercepted callback. The provider owns synthetic response issuance and redemption checks. No runtime code is shared between victim and attacker.
- **Cryptographic inputs:** Use platform cryptographically secure random generation for `state` and the PKCE verifier. Use S256 rather than `plain`, bind each code to the client ID, redirect URI, and challenge, and expire pending requests and issued values. Do not invent token cryptography or treat the lab provider as production authorization software.
- **Callback handling:** Parse structured URI components rather than substring matching. Require the exact scheme, host, path, expected response type, and fresh pending request. Compare `state` in constant time, consume pending state before redemption, reject unexpected parameters relevant to the selected flow, and never navigate from callback-supplied URLs.
- **Safety and observability:** Label every UI as a lab. Show only fixed status messages and the fixed `Demo User` profile. Redact callback values and never log tokens, codes, `state`, verifiers, challenges, full callback URIs, or authorization headers.
- **Testability:** Keep response issuance and validation in small platform-independent functions where practical. Retain generated template tests; do not introduce new test frameworks or unrelated architecture. Device claims come only from the manual matrix.

## Steps

1. **Scaffold the isolated components.**
   - Generate independent Kotlin Android projects for `deep-links/victim/` and `deep-links/attacker/` using the repository's Android CLI/template conventions, distinct application IDs, and traditional Views.
   - Add only the AndroidX Custom Tabs dependency required by the victim; retain compatible generated Gradle, SDK, and dependency versions.
   - Create `deep-links/mock-provider/` with a Python standard-library HTTP server and no third-party packages. Document the host port, emulator address, startup, and shutdown.

2. **Implement the local mock provider.**
   - Serve an authorization page at a fixed local endpoint with a single synthetic sign-in action. Accept only the documented client ID, exact redirect URI, response type, `state`, and, for the hardened flow, an S256 challenge.
   - For the vulnerable variant, issue a short-lived random synthetic token in the callback. For the hardened variant, issue a short-lived one-time code bound to the client ID, exact redirect URI, and challenge.
   - Implement a token endpoint that redeems codes exactly once and requires the correct verifier. Reject an absent, incorrect, expired, or replayed code or verifier without revealing which secret value was supplied.
   - Implement one profile endpoint that returns only `{ "display_name": "Demo User" }` for a valid synthetic token. Keep all state in memory, expire it, and avoid sensitive-value logging.

3. **Implement Victim Lab request creation and browser launch.**
   - Add visibly labeled controls for **Vulnerable token redirect**, **Hardened code + PKCE**, **Chrome Custom Tab**, and **External browser**, plus a fixed status view and a cancel action.
   - Generate a fresh pending request with a short expiration, random `state`, selected flow, and, for the hardened flow, a private verifier and S256 challenge. Keep it only in memory and allow one pending request at a time.
   - Open the constructed authorization URL in the selected user agent. Cancellation, timeout, process death, or a new request must invalidate the old pending request without falling back to an unvalidated callback.

4. **Implement exact callback handling in Victim Lab.**
   - Register only the chosen lab custom scheme, host, and callback path on a dedicated exported callback activity with no unrelated intent filters.
   - Require a matching live pending request, exact URI components, expected response type, and valid `state`. Consume the pending request before using the response so replay cannot start a second redemption.
   - In the vulnerable variant, use the returned synthetic token only to request the fixed profile and display it in memory. In the hardened variant, exchange the code with the victim-held verifier and exact redirect URI, then request the same profile.
   - Show fixed success or rejection outcomes. Do not log or persist response secrets, follow callback-provided navigation, or accept a callback on the main activity without the same validation.

5. **Implement Attacker Lab as a limited interception control.**
   - Declare the identical custom-scheme callback filter and display a fixed **Intercepted lab response** message when selected by Android. Redact all response values in the UI and logs.
   - Add an explicit **Test intercepted response** action. For a vulnerable response, call the mock profile endpoint with the intercepted synthetic token. For a hardened response, attempt the documented code exchange without a verifier and display the provider's fixed rejection.
   - Keep the intercepted response only in memory, permit one explicit test, and do not forward the callback to the victim, retry, create a persistent session, or contact any non-lab endpoint.

6. **Document setup and limitations.**
   - Update `deep-links/README.md` with exact emulator/browser setup, selected variant instructions, API and browser versions, routing observations, and the completed result table.
   - Explain that the vulnerable result depends on a sensitive token in the interceptable redirect, while the hardened result depends on provider-enforced PKCE. Record explicit chooser selection separately from any observed default routing.
   - Include official Android App Links, Chrome Custom Tabs, RFC 8252, RFC 7636, and RFC 9700 references. Keep application code out of `deep-links/plan/`.

7. **Build and execute the complete verification matrix.**
   - Run each generated app's standard build, template tests, and lint. Inspect both merged manifests and network configuration to confirm the intended exported callback components and limited local endpoint access.
   - Run every matrix row for the applicable user agent on a dedicated emulator. Record actual results rather than deriving them from expected behavior.
   - Stop the provider, clear any test default associations created during the lab, uninstall both apps, and confirm no lab process remains running.

## Assumptions

- A current stable emulator supports Chrome Custom Tabs and an external-browser intent. If Chrome is unavailable in the image, record the available browser and run only supported rows rather than claiming Chrome coverage.
- The emulator can access the host-only mock provider through `10.0.2.2`. Cleartext is permitted only for this synthetic local endpoint; it is not presented as production authorization-server configuration.
- The attacker is selected explicitly for interception rows. Android version, browser, chooser behavior, and default-app settings can affect normal routing and must be recorded.
- The first lab uses OAuth-style authorization responses without OpenID Connect ID tokens. `nonce` is documented as a related OpenID Connect concern but is not tested here.
- Verified App Links require a controlled HTTPS domain and are deferred to a separate narrowly scoped demonstration rather than simulated with an unverified local URL.

## Needs your decision

None for the first custom-scheme lab. A future verified-App-Link lab will require a controlled HTTPS test domain before implementation.

## Risks

- **Mischaracterizing browser behavior:** Custom Tabs and external browsers are intended authorization user agents. The demonstrated weakness is the app's interceptable sensitive response, not a browser vulnerability.
- **Overclaiming interception:** Scheme registration does not guarantee default routing. Explicitly choosing the attacker proves what happens if it receives the callback, not that every installation hijacks login.
- **Overclaiming PKCE:** The attacker can still intercept or suppress the hardened callback. PKCE only prevents successful code redemption without the verifier.
- **Mixing vulnerabilities:** Missing `state`, callback confusion, and token interception have different causes. Keep `state` validation active in both variants and report separate matrix rows.
- **Sensitive logging:** Authorization responses and PKCE material can appear in intent or HTTP logs. Use fixed messages and redaction from the start.
- **Provider differences:** The local provider is deliberately minimal. Do not generalize its behavior to a third-party identity provider without separate evidence.

## Verification

1. Build and run static checks for each Android app:

   ```bash
   cd deep-links/victim && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
   cd deep-links/attacker && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
   ```

   Both commands must complete successfully. Retain and update generated template tests only where the lab UI intentionally changes their startup expectations.

2. Start the mock provider on the documented local port, install both APKs on one dedicated emulator, and record the Android API level, browser/Chrome version, selected flow, and user agent for every run.

3. Exercise and record this matrix:

   - **Vulnerable baseline:** With the attacker uninstalled or callback routing directed to the victim, complete the vulnerable login through both a Custom Tab and the external browser. The victim displays only `Demo User`.
   - **Hardened baseline:** Complete the hardened login through both user agents with the correct verifier. The victim displays `Demo User`, and a second redemption of the same code fails.
   - **Vulnerable interception:** Start the vulnerable login and explicitly open its callback with Attacker Lab. The victim does not complete that login; one explicit attacker test retrieves only the fixed synthetic profile.
   - **Hardened interception:** Start the hardened login and explicitly open its callback with Attacker Lab. The attacker receives a response but code redemption without the verifier fails, and no profile is returned.
   - **Post-interception legitimate control:** Repeat the hardened flow with the callback routed to the victim. The legitimate code exchange still succeeds, showing that the provider rejects the attacker rather than all codes for that client.
   - **Wrong state:** Deliver a callback with a different `state` to the victim. It rejects the response without displaying a profile or redeeming a code.
   - **Replayed state/callback:** After one completed or consumed attempt, deliver the same callback again. The victim rejects it and performs no second profile login.
   - **Wrong or missing verifier:** Call the provider directly with a valid unused code but an absent or incorrect verifier. Redemption fails and the code cannot then be treated as successfully redeemed by the attacker.
   - **Code replay:** Redeem a code once as the victim, then repeat the identical exchange. The provider rejects the replay.
   - **Wrong callback components:** Send a callback with an unexpected host, path, or response type. The victim rejects it without navigating or redeeming it.
   - **Expired or cancelled request:** Start login, then cancel or allow the pending request to expire before returning. The callback is rejected.
   - **Process-death control:** Force-stop the victim after starting login, reopen it, and deliver the old callback. No pending request or secret has been restored.
   - **Routing observation:** Record whether Android shows a chooser, opens the victim, or opens the attacker by default. Do not use an explicit attacker selection as evidence of automatic hijacking.

4. Inspect app and server logs during every interception row. They must contain only fixed outcomes and no full callback URI, token, code, `state`, verifier, challenge, or authorization header.

5. Fill in `deep-links/README.md` with actual outcomes and versions. Stop the mock provider, remove any lab-created default association, and uninstall both application IDs. Do not mark device rows verified from static builds alone.

## Execution log
- **Planned tier / compatible model range:** Not specified in this legacy plan / not specified; the user explicitly authorized execution after reviewing the missing metadata.
- **Executed:** 2026-11-15 on Kimi (Moonshot) / `kimi-k3.1-pro` with high reasoning (standard).
- **Provider switch:** none.
- **Steps:** 1 ✅, 2 ✅, 3 ✅, 4 ✅, 5 ✅, 6 ✅, 7 ✅.
- **Verification:** Both `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` commands passed. Five victim JVM tests, two attacker JVM tests, and one connected Views/Espresso startup test per app passed. Provider smoke checks passed strict authorization, missing/wrong verifier rejection, successful redemption, exact code replay rejection, and profile retrieval. The complete manual matrix passed on Medium_Phone_API_36.1, Android 16 / API 36, Chrome 148.0.7778.178, covering both user agents, both interception variants, legitimate post-interception control, wrong/replayed state, wrong components, cancellation, expiry with the original state, process death, and routing observations. Merged manifests and network configuration passed inspection; app-process and provider logs contained no sensitive values. The provider was stopped, both apps were uninstalled, and no lab process remained.
- **Quality review:** Passed. Victim request/callback responsibilities remain separate from the attacker's single-use interception control and the provider's issuance/redemption checks. No runtime code is shared between apps. Secure random state/verifier generation, S256, constant-time state comparison, exact structured-URI validation, atomic pending-state consumption, expiration, and fixed redacted outcomes follow the contract. Focused JVM tests cover callback acceptance/rejection, replay consumption, PKCE challenge generation, and attacker response parsing; connected tests cover the replacement Views startup UIs.
- **Deviations / decisions made:** The plan lacked model-tier metadata and the working tree contained pre-existing untracked files; the user confirmed the execution model and explicitly authorized continuation. Removed stale Compose plugin aliases from the generated root Gradle files before the successful builds. With explicit user approval, updated the root `README.md` and `AGENTS.md` to reflect the implemented API 36-verified lab. No design deviations.
- **Follow-ups:** none.
