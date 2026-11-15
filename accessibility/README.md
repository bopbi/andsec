# Accessibility Labs

This directory contains a controlled educational Android accessibility lab. It demonstrates how a deliberately enabled accessibility service can invoke a visible button in another synthetic app.

**This demonstrates abuse potential of a legitimately authorized, high-privilege capability. It does not demonstrate an Android platform vulnerability or a permission bypass.**

## Projects

- `victim/` — Victim Lab, a separately installable app with an in-memory counter and no permissions.
- `attacker/` — Attacker Lab, a separately installable consent UI and accessibility service with no `INTERNET` or other unnecessary permissions.
- `plan/` — implementation plan and execution record.

The attacker accepts events only from `com.example.accessibilityvictim`, repeats package checks at runtime, and targets only `com.example.accessibilityvictim:id/demo_action_button`. An explicit arm request expires after 30 seconds and is consumed before the sole action attempt. It is never persisted. The service does not use gestures, coordinates, screenshots, key events, retries, scrolling, text extraction, or fallback interaction techniques.

A hardcoded victim package is not an Android accessibility requirement. This lab uses one to keep the demonstration narrowly scoped; a service without an `android:packageNames` filter can receive events from multiple packages and inspect each event's `packageName` dynamically. Reliably targeting a specific control still requires identifying information such as its package, resource ID, visible text, or UI structure.

## Threat model

The user must manually enable Attacker Lab's accessibility service in Android system settings. Installing the APK alone grants no accessibility access. The demonstration uses only synthetic apps and a harmless counter; it does not target third-party apps, accounts, credentials, messages, payments, or personal data.

The victim's accessibility-visibility switch is an experiment, not a security control. Hiding a button from accessibility can prevent legitimate assistive-technology use and must not be treated as user authorization.

## Secure development checklist

When applying this lesson to an app:

- Enforce authentication and authorization for sensitive actions in the app and on the backend; do not assume that a visible button tap by itself proves who initiated an action or their authorization.
- Never use `importantForAccessibility`, hidden controls, obfuscated labels, or click-source heuristics as a security boundary. These approaches do not replace authorization and can exclude legitimate assistive-technology users.
- Add confirmation or step-up authentication where the risk of an action justifies it, while keeping the flow accessible. High-value backend operations still require server-side authorization.
- Treat UI content available to accessibility services as visible to services the user has authorized. Avoid displaying secrets unnecessarily, but do not present this as a way to block a full-capability service.
- If you build an accessibility service, request only the packages, event types, and capabilities it needs; explain their purpose clearly and avoid collecting unrelated UI content.
- Test sensitive workflows with an authorized accessibility service to understand exposure, and test normal assistive-technology use to ensure a defensive change does not break accessibility.

## Build and static checks

Run each project's own Gradle wrapper:

```bash
cd accessibility/victim && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
cd accessibility/attacker && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The generated ViewModel unit tests were removed during the Views conversion, so `testDebugUnitTest` currently reports `NO-SOURCE`. The converted Espresso startup tests can be run on a connected dedicated emulator:

```bash
cd accessibility/victim && ./gradlew :app:connectedDebugAndroidTest
cd accessibility/attacker && ./gradlew :app:connectedDebugAndroidTest
```

## Emulator setup

1. Use a dedicated emulator or non-personal test device. List existing emulators with `android emulator list`; if necessary, see `android emulator create --help` and use an installed current stable system image. Start one with `android emulator start <avd-name>` and note its serial from `adb devices`.
2. Build both apps and install their debug APKs:

   ```bash
   android install --device=<serial> accessibility/victim/app/build/outputs/apk/debug/app-debug.apk
   android install --device=<serial> accessibility/attacker/app/build/outputs/apk/debug/app-debug.apk
   ```

3. Launch Attacker Lab and tap **Open accessibility settings**. Select **Attacker Lab Victim Control**, enable it, and review the system's full-control confirmation before tapping **Allow**. Do not attempt to bypass any restricted-settings prompt.
4. Open Victim Lab directly and confirm that **Expose counter button to accessibility** is on and the counter is zero.

## One-click scenario and controls

1. Tap **Increment demo counter** manually and confirm exactly one increment, then use **Reset demo counter**.
2. In Attacker Lab, tap **Arm one click and open Victim Lab**. The victim should open and its counter should increment exactly once. Further window or content events must not increment it again.
3. Use **Cancel armed click** to cancel a request before opening the victim. To test expiry, allow more than 30 seconds to elapse without permitting a victim action, then foreground the victim.
4. Turn the victim's accessibility-visibility switch off, reset the counter, and arm one click. The request must log target-unavailable and leave the counter unchanged. A manual tap on the still-visible button must still work.
5. Check the service-disabled, enabled-but-disarmed, other-app, and process-restart controls using the result matrix below.

Use `adb logcat -d -s A11yLab` to inspect fixed lab outcomes. Use `android layout --device=<serial> --full` only on the dedicated test device when the visible target or resource ID needs inspection. Do not collect UI dumps or screenshots containing personal apps or data.

## Verified results

Tested on **2026-11-15** with emulator **Pixel_9a_API_37**, **Android 17, API 37**. The complete matrix was rerun on the same emulator after the pre-release networking cleanup described below.

| Scenario | Actual outcome | Result |
| --- | --- | --- |
| Manual baseline | Counter started at 0, changed to 1 after one tap, and returned to 0 after reset. | Pass |
| Service disabled | Arm/open launched Victim Lab; counter remained 0. | Pass |
| Enabled but disarmed | Victim remained at 0 after waiting through window events. | Pass |
| Armed accessible target | Counter changed from 0 to 1; one success was logged and later events caused no second increment. | Pass |
| Timeout | After more than 30 seconds without a permitted action, foregrounding the victim left the counter at 0 and logged the fixed expired outcome. | Pass |
| Hidden-node comparison | Counter remained 0 and target-unavailable was logged. A subsequent manual tap changed it to 1. | Pass |
| Other-app control | Android Settings was foregrounded immediately after arming. No victim action or `A11yLab` outcome occurred; after cancellation, the victim remained at 0. | Pass |
| Restart control | The attacker was force-stopped immediately after arming. After re-enabling the service and reopening the victim, the counter remained 0; no request was persisted. | Pass |

For the timeout row, the service was temporarily disabled while the request aged, then re-enabled before the victim was foregrounded. On this test image, force-stopping the attacker also removed its enabled accessibility-service entry, so the service was manually re-enabled for the restart control.

Android displayed the standard accessibility full-control confirmation. **No restricted-settings prompt appeared on this emulator.**

Both projects' assemble, unit-test, and lint tasks passed with **AGP 9.1.1 / Gradle 9.3.1**. Both Espresso startup tests passed on the same emulator. Merged-manifest inspection confirmed the distinct application IDs, the binding-protected service, and no `INTERNET` permission. A pre-release review later found an undocumented Dummy JSON request in the victim; its code, response UI, resources, test assertion, and `INTERNET` permission were removed before this rerun. Neither accessibility app now contains runtime networking.

## Cleanup

Disable **Attacker Lab Victim Control** in Android accessibility settings, then uninstall only the two lab apps:

```bash
adb -s <serial> uninstall com.example.accessibilityattacker
adb -s <serial> uninstall com.example.accessibilityvictim
```

## Official references

- [Accessibility service guide](https://developer.android.com/guide/topics/ui/accessibility/service)
- [`AccessibilityNodeInfo.performAction`](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo#performAction(int))
- [Accessibility service configuration](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo)
