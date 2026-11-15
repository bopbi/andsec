# Controlled Android Accessibility Cross-App Action Lab

- **Status:** done
- **Created:** 2026-11-14
- **Source:** user request

## Goal
Build two separately installable demo apps, **Victim Lab** and **Attacker Lab**, that demonstrate how a user-enabled Android accessibility service can invoke a visible button in another app. The experiment will prove one harmless counter change, include negative controls and an accessibility-visibility comparison, and document the difference between abuse of an authorized accessibility service and an Android platform vulnerability.

## Context
The workspace is a Git repository. `accessibility/` contains topic documentation and this plan, but no application code, architecture, or test suite to preserve. The `android` CLI is installed at `/usr/local/bin/android`; its available project templates are Compose-based `empty-activity` templates.

Android's official [accessibility service guide](https://developer.android.com/guide/topics/ui/accessibility/service) documents that a service configured with `canRetrieveWindowContent` can inspect the active window and use `AccessibilityNodeInfo.performAction()` to act for the user. This requires the user to enable the service in system settings; the demo must not claim that installing the attacker APK alone grants this capability. The guide also supports restricting events with `packageNames`, which this lab will use in addition to runtime package checks.

The demonstration will use two standalone Android projects so the victim and attacker have distinct application IDs, manifests, processes, and permissions. The attacker will act only after an explicit, short-lived user request and only against a synthetic counter button in the victim.

## Non-goals
- No targeting of third-party apps, real accounts, payments, credentials, messages, or other sensitive data.
- No automatic accessibility-permission grant, permission bypass, overlay deception, tapjacking, or exploitation of a platform CVE.
- No gesture injection, coordinate tapping, global navigation actions, screenshots, key-event filtering, text extraction, or fallback when the intended node is unavailable.
- No stealth, accessibility-service persistence, boot receivers, background scheduling, obfuscation, or anti-analysis behavior.
- No network access, telemetry, data exfiltration, or distribution through an app store.
- No claim that `importantForAccessibility="no"` is a general security defense; the comparison also records its harm to legitimate accessibility users.

## Approach
Use Kotlin and traditional Android Views in two standalone projects: `accessibility/victim/` and `accessibility/attacker/`. Views provide a concrete resource ID for the target button without adding Compose test-tag/resource-ID behavior to the experiment. Generate the Gradle projects with the available Android CLI template, then replace its Compose UI with framework activities and XML layouts.

The victim owns only an in-memory counter. The attacker has a normal launcher activity for consent and setup plus an `AccessibilityService` that finds exactly `com.example.accessibilityvictim:id/demo_action_button` and invokes `AccessibilityNodeInfo.ACTION_CLICK` once. Arming expires after 30 seconds, is consumed before every attempt, and is never persisted. Victim-only service configuration, runtime package checks, and explicit target validation keep the blast radius narrow.

A victim switch will toggle the target's `importantForAccessibility` state for a comparison. When the node is hidden, the attacker must stop with a logged failure rather than try another interaction technique. This demonstrates an accessibility-visibility difference, not a robust authorization mechanism.

Alternatives considered and rejected:
- **One multi-module project:** convenient, but standalone projects make the two APK/security boundaries unmistakable and can be scaffolded directly with the available CLI.
- **Compose UI:** avoids XML layouts, but adds unnecessary uncertainty about mapping semantics/test tags to accessibility resource IDs.
- **Automated gesture demonstration:** broader and riskier than needed; a single semantic `ACTION_CLICK` proves the cross-app control principle without coordinate input.

## Implementation quality
- **Responsibility and integration:** The victim's `MainActivity` exclusively owns counter changes and target accessibility visibility. The attacker's `MainActivity` owns user setup and launch actions, `DemoSessionState` owns the one-shot arming deadline, and `VictimControlService` exclusively handles accessibility events and node actions. No shared runtime module or cross-app storage is introduced.
- **Reuse and boundaries:** Reuse the generated Gradle/Android template configuration and platform APIs. Remove generated Compose UI/dependencies rather than adding another UI library. Do not add dependency injection, interfaces, network components, or a general-purpose UI automation framework for this single interaction.
- **Testability and behavior:** The counter text is the observable result. Verification covers manual clicks, service disabled, service enabled but disarmed, one armed click, repeated events without a second click, timeout, hidden-node behavior, and a non-victim foreground app. End-to-end checks run on an emulator because enabling accessibility is a system-mediated action. Retain and run template-provided tests; do not add a new test framework or test scaffolding to this empty workspace.
- **Safety and observability:** Both apps are visibly labeled as lab apps. The attacker declares no `INTERNET` permission and logs only fixed outcomes under the `A11yLab` Logcat tag, never node text or hierarchy contents.

## Steps
1. **Scaffold two independent Android projects.**
   - From the workspace root, run:
     ```bash
     android create empty-activity --name="Victim Lab" \
       --application-id=com.example.accessibilityvictim \
       --namespace=com.example.accessibilityvictim \
       --min-sdk=26 --output=accessibility/victim
     android create empty-activity --name="Attacker Lab" \
       --application-id=com.example.accessibilityattacker \
       --namespace=com.example.accessibilityattacker \
       --min-sdk=26 --output=accessibility/attacker
     ```
   - In each generated `app/build.gradle.kts` and `app/src/main/`, retain the template's current stable compile/target SDK conventions but replace Compose with a framework `Activity` and XML layout; remove Compose-only dependencies and generated Compose test references that no longer compile.
   - Keep the two Gradle wrappers independent. Do not place application code in `accessibility/plan/`.

2. **Implement the harmless victim behavior.**
   - `accessibility/victim/app/src/main/res/layout/activity_main.xml` — add a title identifying the lab, `status_text`, `demo_action_button` labeled **Increment demo counter**, `reset_button`, and `accessibility_visibility_switch` labeled **Expose counter button to accessibility**. The switch is on by default.
   - `accessibility/victim/app/src/main/java/com/example/accessibilityvictim/MainActivity.kt` — keep an in-memory counter starting at zero; update `status_text` to `Demo counter: N`; increment only from the target button's normal click listener; reset from `reset_button`; toggle `demo_action_button.importantForAccessibility` between `IMPORTANT_FOR_ACCESSIBILITY_YES` and `IMPORTANT_FOR_ACCESSIBILITY_NO` from the switch.
   - `accessibility/victim/app/src/main/AndroidManifest.xml` and `res/values/strings.xml` — retain only the normal launcher activity and app resources, with clear lab labeling. Add no permissions, services, receivers, providers, or exported non-launcher components.

3. **Implement the attacker's explicit consent and launch UI.**
   - `accessibility/attacker/app/src/main/res/layout/activity_main.xml` and `res/values/strings.xml` — explain that the user must enable the lab accessibility service, then add **Open accessibility settings**, **Arm one click and open Victim Lab**, and **Cancel armed click** buttons plus a status label.
   - `accessibility/attacker/app/src/main/java/com/example/accessibilityattacker/DemoSessionState.kt` — create an internal main-thread-only state holder containing `armedUntilElapsedRealtime`, initially zero. Provide arm, cancel, and consume/expire operations using `SystemClock.elapsedRealtime()` and a fixed 30,000 ms window; never persist the state.
   - `accessibility/attacker/app/src/main/java/com/example/accessibilityattacker/MainActivity.kt` — open `Settings.ACTION_ACCESSIBILITY_SETTINGS` from the settings button; arm one request and launch only `com.example.accessibilityvictim` using its package launch intent from the arm button; cancel from the cancel button. If the victim launch intent is unavailable, clear the armed state and show an instruction to install Victim Lab.

4. **Declare the least-capability accessibility service.**
   - `accessibility/attacker/app/src/main/AndroidManifest.xml` — declare `VictimControlService` with `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`, the accessibility-service intent filter, explicit `android:exported="true"`, and metadata referencing the XML configuration. The signature-level binding permission protects the exported service from non-system binding. Add no `INTERNET` or other unnecessary permissions.
   - `accessibility/attacker/app/src/main/res/xml/accessibility_service_config.xml` — configure only:
     - `android:packageNames="com.example.accessibilityvictim"`
     - `android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"`
     - `android:accessibilityFlags="flagReportViewIds"`
     - `android:accessibilityFeedbackType="feedbackGeneric"`
     - `android:canRetrieveWindowContent="true"`
     - a clear lab-specific description and the attacker settings activity.
   - Do not enable gesture, screenshot, key-event, magnification, touch-exploration, or all-app capabilities.

5. **Implement one validated, one-shot semantic click.**
   - `accessibility/attacker/app/src/main/java/com/example/accessibilityattacker/VictimControlService.kt` — extend `AccessibilityService`. For each relevant event, return immediately unless the event and `rootInActiveWindow` belong to `com.example.accessibilityvictim` and a live armed request exists.
   - Consume/cancel the armed request **before** the sole action attempt so repeated window events cannot cause another click. Look up only `com.example.accessibilityvictim:id/demo_action_button` with `findAccessibilityNodeInfosByViewId`. Require a visible, enabled, clickable matching node; call only `performAction(AccessibilityNodeInfo.ACTION_CLICK)`.
   - Log fixed success, unavailable-target, rejected-action, expired, and wrong-package outcomes under `A11yLab`. Do not log victim text, read unrelated nodes, retry, scroll, tap coordinates, or substitute gestures. Clear armed state when the service is destroyed/unbound; process death must also leave it disarmed. Implement `onInterrupt()` without initiating actions.

6. **Document setup, threat model, and observations.**
   - `accessibility/README.md` — explain the prerequisite of manual accessibility enablement, exact emulator setup, the one-click scenario, negative controls, the accessibility-visibility comparison, and cleanup. State prominently that the demo shows abuse potential of a legitimately authorized high-privilege service, not proof of an Android platform vulnerability.
   - Include official references to the accessibility service guide and `AccessibilityNodeInfo.performAction`, plus a result table for the verification matrix with the tested Android API level and actual outcomes. Record any OS restricted-settings prompt factually; never describe or implement a bypass.

7. **Build, inspect, and run the emulator matrix.**
   - Run the builds and template-provided checks in Verification below. Inspect both merged manifests to confirm package identities and absence of `INTERNET`; inspect the attacker service XML against step 4 before device testing.
   - Execute every manual matrix row, fill in `accessibility/README.md`, then disable the lab accessibility service and uninstall both apps from the test emulator.

## Assumptions
- Two standalone projects are the default layout; if a single project is later preferred, the same two application IDs and module responsibilities apply, but step 1 becomes root multi-module Gradle setup and Verification uses module-qualified Gradle tasks.
- The CLI template's current stable SDK/AGP defaults are suitable; if its generated Compose setup differs, preserve its working Gradle versions while applying the framework-Views replacement in step 1.
- `minSdk 26` provides a modern baseline without affecting the APIs used; if an older test emulator is required, API 18 is the technical floor for the selected node APIs, but no compatibility work is included unless separately requested.
- A dedicated emulator with a current stable Android image is the default. If none exists, create a test AVD using `android emulator create --help` and the installed SDK packages; a dedicated non-personal test device is the fallback, not a daily-use phone.
- The normal Views button exposes its resource ID with `flagReportViewIds`. If lookup returns no node while the switch is on, inspect the visible hierarchy with `android layout --full`, confirm the service is enabled and the victim is foreground, and correct the lab configuration; do not add coordinate, text, or gesture fallbacks.
- Template-provided tests are retained where valid. If the selected template generates no runnable tests, run its available build/lint tasks and the complete manual matrix rather than introducing unrelated test scaffolding.

## Needs your decision
None.

## Risks
- **Mischaracterizing the result:** Accessibility control after explicit user enablement is an intended platform capability with abuse potential, not automatically a vulnerability. Keep claims tied to observed behavior and API level.
- **Repeated actions:** Window/content events can arrive multiple times. Consuming the armed state before lookup or action is essential to guarantee at most one attempt.
- **Wrong-app interaction:** XML package filtering alone is not enough; retain both event and root-package runtime checks and the exact victim resource ID.
- **OS setup differences:** Recent Android versions may present additional restricted-settings friction for sideloaded apps. Record the behavior and use only the documented manual system UI on the test emulator.
- **Accessibility harm:** Hiding a control from accessibility can prevent legitimate assistive-technology use. Present the switch only as an experiment, not as recommended production authorization.
- **Timing differences:** Window content may not be ready immediately after launch. A one-shot attempt may log target-unavailable; rerun the explicit arm action after the victim is visible rather than adding polling, retries, or persistence.
- **Template drift:** Generated Compose dependencies or tests may reference removed UI code. Remove only stale generated references and keep template versions consistent rather than mixing unrelated dependency versions.

## Verification
1. Build and run static checks for each app:
   ```bash
   cd accessibility/victim && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
   cd accessibility/attacker && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
   ```
   Both commands must complete successfully. If a generated test was invalidated by the Views conversion, update that generated test only to match the new startup UI; do not weaken assertions merely to obtain a pass.

2. Install and launch both APKs on one dedicated emulator using `android run` or the generated APK paths:
   - `accessibility/victim/app/build/outputs/apk/debug/app-debug.apk`
   - `accessibility/attacker/app/build/outputs/apk/debug/app-debug.apk`

3. Exercise and record this matrix:
   - **Manual baseline:** Victim starts at zero; tapping its button increments exactly once; reset returns it to zero.
   - **Service disabled:** From Attacker Lab, attempt to arm/open the victim. The victim opens, but its counter remains unchanged.
   - **Enabled but disarmed:** Manually enable only Attacker Lab's service in Android accessibility settings, open the victim without arming, and wait. The counter remains unchanged.
   - **Armed accessible target:** Reset the victim, return to Attacker Lab, tap **Arm one click and open Victim Lab**, and observe exactly one increment. Wait through additional window/content events and confirm no second increment.
   - **Timeout:** Arm without allowing an action for more than 30 seconds, then foreground the victim. The counter remains unchanged.
   - **Hidden-node comparison:** Turn the victim switch off, reset, and arm one attacker click. The counter remains unchanged and `A11yLab` reports target-unavailable. Manually tapping the still-visible victim button must still increment, demonstrating that accessibility visibility is not user authorization.
   - **Other-app control:** Arm the demo and foreground an app other than the victim. No action or victim-content access should occur; no fallback is permitted.
   - **Restart control:** Force-stop the attacker after arming, reopen the victim, and confirm the request was not persisted.

4. Use `android layout --full` to confirm the visible target/resource-ID behavior when needed, and `adb logcat -s A11yLab` to confirm only fixed lab outcomes are logged. Do not collect screenshots or UI dumps containing personal apps or data.

5. Confirm the README result table matches actual observations and includes the emulator API level. Disable the lab service and run:
   ```bash
   adb uninstall com.example.accessibilityattacker
   adb uninstall com.example.accessibilityvictim
   ```

## Model recommendation
- **Overall tier:** standard — the plan is explicit and isolated, but it introduces new Android behavior, a system-permission boundary, and non-trivial device verification rather than mechanical edits.
- **Compatible model range:** `claude-sonnet-5-5`; `gpt-6.1-terra`; `gemini-3.8-flash`; `kimi-k3.1-pro` with `reasoning_effort: "high"`; `qwen3.7-plus`; `grok-4.6`.
- **Execution compatibility:** Any listed model, or a verified higher-tier model, is acceptable even when it differs from the model used to create the plan. Record the actual provider, model, and tier in the execution log.

## How to execute
Run `/run-plan` with this file when you are ready — or just say "run the plan" and `/run-plan` picks the newest draft in `plan/`. There are no open decision lines to fill in.

## Execution log
- **Planned tier / compatible model range:** standard / `claude-sonnet-5-5`; `gpt-6.1-terra`; `gemini-3.8-flash`; `kimi-k3.1-pro` with `reasoning_effort: "high"`; `qwen3.7-plus`; `grok-4.6`
- **Executed:** 2026-11-15 on Moonshot AI / `kimi-k3.1-pro` with high reasoning (standard; confirmed by the user)
- **Provider switch:** none
- **Steps:** 1 ✅, 2 ✅, 3 ⚠️ deviated (added victim-only package visibility and `FLAG_ACTIVITY_SINGLE_TOP` for reliable launching without resetting victim state), 4 ✅, 5 ⚠️ deviated (required `isImportantForAccessibility` after API 37 returned the hidden node but rejected its action), 6 ⚠️ deviated (also updated the existing root roadmap and agent guide status), 7 ✅
- **Verification:** `cd accessibility/victim && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` passed; the equivalent attacker command passed after every code change. Both `testDebugUnitTest` tasks reported `NO-SOURCE` after stale generated ViewModel tests were removed. `./gradlew :app:connectedDebugAndroidTest` passed one converted Espresso startup test in each project on Pixel_9a_API_37, Android 17 / API 37. Merged manifests had the expected package identities, victim-only `<queries>` entry, binding-protected service, and no `INTERNET`; service XML matched step 4. All eight manual matrix rows passed on API 37, including fixed one-shot, unavailable-target, and expired logs. Both uninstall commands returned `Success`, the enabled-service entry was absent, and the emulator was stopped.
- **Quality review:** Passed. Counter ownership remains in the victim activity; consent/launching, session state, and accessibility actions retain their separate attacker responsibilities. No new runtime library, injection layer, general automation framework, persistence, network component, or fallback interaction was added. The converted startup tests passed, and every behavior in the manual contract was exercised and recorded.
- **Deviations / decisions made:**
  - Proceeded with unrelated untracked files after explicit user approval; left `deep-links/` and other unrelated content unchanged.
  - The CLI reported `platforms/android-37` not found, but the installed `platforms/android-37.0` built the unchanged template SDK configuration successfully.
  - Added a `<queries>` declaration for only `com.example.accessibilityvictim` because Android package visibility otherwise made `getLaunchIntentForPackage()` return null.
  - Added `FLAG_ACTIVITY_SINGLE_TOP` to the package launch intent so arming foregrounds the existing victim instead of resetting its counter and visibility switch.
  - Added an `isImportantForAccessibility` target check. On API 37, the hidden node could still be returned by ID lookup; without this check its action was rejected rather than producing the planned unavailable-target outcome.
  - Converted generated Compose device tests to Espresso startup tests and removed invalidated ViewModel unit tests; no new test framework was introduced.
  - Updated `README.md` and `AGENTS.md` to replace stale planned-status documentation, as required by the repository guide.
  - For timeout verification, the service was disabled while the request aged for more than 30 seconds and re-enabled before foregrounding the victim. Force-stop also removed the service entry on the test image, so it was manually re-enabled before the restart observation. No restricted-settings prompt appeared.
- **Follow-ups:** The template's AGP 9.0.1 originally warned that it was tested through compile SDK 36.1 while building SDK 37; the post-release remediation below updates the projects to a compatible version. API 37 also marks `AccessibilityNodeInfo.recycle()` deprecated, producing compiler warnings without affecting verification.

## Post-release review remediation — 2026-11-15

- A pre-open-source review found that the victim had subsequently gained an undocumented request to `https://dummyjson.com/test`, response UI, and the `INTERNET` permission, contradicting this plan and the recorded verification. Removed the request code, executor/callback lifecycle, response view and strings, manifest permission, and the corresponding Espresso assertion. The victim again owns only the in-memory counter and accessibility-visibility switch.
- Updated all four Android projects from AGP 9.0.1 / Gradle 9.1.0 to AGP 9.1.1 / Gradle 9.3.1, the first AGP release documented as supporting API 37. Kept every Gradle distribution SHA-256 pinned; the unsupported-compile-SDK warning no longer appears.
- The prescribed `assembleDebug`, `testDebugUnitTest`, and `lintDebug` tasks passed in all four projects. Accessibility unit-test tasks remain `NO-SOURCE`. The victim's initial remediation build caught malformed XML from the edits; it was corrected and the complete victim command was rerun successfully before device verification.
- Merged-manifest inspection confirmed the expected application IDs, victim-only `<queries>` entry, binding-protected attacker service, and no `INTERNET` permission in either accessibility app. No Dummy JSON symbols or third-party URL remain in the victim app or its resources.
- Both updated Espresso startup APKs passed their instrumentation tests on **Pixel_9a_API_37, Android 17 / API 37**. All eight manual matrix rows were rerun and passed: manual baseline, service disabled, enabled but disarmed, one armed click without a repeated action, timeout, hidden-node/manual comparison, other-app/cancellation control, and force-stop/restart control. The standard accessibility full-control confirmation appeared; no restricted-settings prompt appeared.
- The timeout was again aged with the service disabled before re-enabling it through system settings. Force-stop again removed the enabled-service entry, which was manually restored for the restart observation. The service was disabled during cleanup, both app and both instrumentation-test packages returned `Success` when uninstalled, and the emulator was stopped.
