# Implicit Intent Interception Lab

This directory contains a controlled activity-routing lab using two separately installed synthetic apps and the fixed string `ANDSEC-DEMO-REPORT-001`. It compares an implicit activity intent with an explicit-component intent that names the intended receiver.

**Implicit intents are intended Android platform capabilities. This lab does not demonstrate an Android platform vulnerability.** The vulnerable result comes from sending data without selecting a trusted destination. Explicitly choosing Attacker Lab demonstrates the impact if Android routes the intent there; it does not prove that installing Attacker Lab guarantees automatic interception.

## Components

- `victim/` — Victim Lab sends both variants and contains the intended exported `ReportActivity`.
- `attacker/` — Attacker Lab is a separately installed, visibly labeled activity claiming the same custom action.
- `plan/` — the threat model, implementation plan, and execution record.

Both apps use Kotlin, traditional Android Views, and no permissions, including no `INTERNET` permission. Neither app stores, forwards, or transmits the report. Logcat messages use fixed `IntentLab` outcome labels and never include intent extras.

## Minimal threat model

The only asset is the harmless synthetic report, carried as the string extra `com.example.intentvictim.extra.REPORT`. The victim sender, Android's intent resolver, intended victim receiver, and attacker receiver are the trust boundaries. Android and the dedicated emulator are trusted and unmodified.

Both apps must be installed for two-handler scenarios. Attacker Lab can declare the matching action `com.example.intentvictim.action.VIEW_REPORT` and read the extra if Android routes a request to it. It cannot read victim memory, alter the resolver, or force its own selection. The user must press a victim send button and, when a chooser appears, explicitly select a receiver.

The vulnerable path is `Victim MainActivity -> Android intent resolution -> matching activity`. The hardened path names Victim `ReportActivity` directly. Both paths send the same action and fixed payload; only component targeting differs.

## Send and receiver behavior

- **Send vulnerable implicit intent** creates an intent with only the custom action and report extra. Android may open the intended victim activity, present a choice, or use an existing default.
- **Send hardened explicit intent** creates the same intent and additionally calls `setClass` for Victim `ReportActivity`. Attacker Lab is not a candidate for that request.
- Victim `ReportActivity` and Attacker Lab display the fixed report only when the action and exact synthetic payload match. Missing or malformed requests display fixed invalid states.
- Normal Attacker Lab launcher startup displays its initial state rather than loading a report from storage.

Explicit routing selects a destination; it does not authenticate callers or validate direct invocations of an exported activity. Victim `ReportActivity` remains exported in both variants to isolate sender routing, and its direct-invocation limitation is therefore separate from this lesson.

## Build and static checks

Use each standalone project's Gradle wrapper:

```bash
cd intents/victim && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
cd intents/attacker && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The projects retain Views/Espresso startup tests. On a connected dedicated emulator, run:

```bash
cd intents/victim && ./gradlew :app:connectedDebugAndroidTest
cd intents/attacker && ./gradlew :app:connectedDebugAndroidTest
```

After assembly, inspect merged manifests under each project's `app/build/intermediates/merged_manifests/debug/`. Confirm distinct application IDs, matching custom-action filters, intended exported activities, and no permissions, URI filters, or unrelated components.

## Emulator setup

1. Use a dedicated emulator or non-personal test device and record its Android version, API level, and device/AVD name.
2. Build and install both debug APKs, substituting the emulator serial:

   ```bash
   android install --device=<serial> intents/victim/app/build/outputs/apk/debug/app-debug.apk
   android install --device=<serial> intents/attacker/app/build/outputs/apk/debug/app-debug.apk
   ```

3. For the intended-only baseline, install Victim Lab without Attacker Lab or do not select Attacker Lab. Reinstall Attacker Lab for the two-handler rows.
4. Record every vulnerable send as direct, chooser-mediated, or default-mediated. Do not describe explicit attacker selection as automatic hijacking.

## Manual verification matrix

| Scenario | Expected result | Observed result |
| --- | --- | --- |
| Vulnerable intended baseline | Victim `ReportActivity` displays the fixed report | **Pass:** With Attacker Lab absent, Android routed directly to the victim and displayed `ANDSEC-DEMO-REPORT-001`. |
| Vulnerable intended receiver with both installed | Selecting Victim Lab displays the report only there | **Pass:** Android presented a chooser; selecting Victim Lab with **Just once** displayed the report only in the victim. |
| Vulnerable interception | Explicitly selecting Attacker Lab discloses the fixed report only there | **Pass:** Explicit attacker selection in the chooser displayed the fixed report only in Attacker Lab. This was not automatic interception. |
| Hardened with both installed | Only Victim `ReportActivity` opens, with no attacker option | **Pass:** The explicit intent opened the victim directly, without a chooser or attacker receipt. |
| Hardened against a lab-created default | A default for Attacker Lab, if supported, does not change the explicit destination | **Limitation recorded:** The resolver offered **Always** for its primary Victim Lab choice, but alternate Attacker Lab selection launched only once. No attacker default could be created through that UI, so this variation was not run. The hardened direct-routing row passed. |
| Missing-extra control | Both receivers show fixed initial/invalid states without crashing | **Pass:** Normal attacker startup showed its initial state; explicit victim delivery without the extra showed the fixed invalid message. |
| Fresh-start control | Force-stopped Attacker Lab does not restore a report from app storage | **Pass:** After interception, force-stop and fresh launcher startup showed no report. No task/intent restoration was observed. |
| Routing observation | Direct, chooser, and default behavior is recorded separately | **Pass:** The attacker-absent baseline was direct; both-installed vulnerable runs were chooser-mediated. No default association was created. |

For the victim missing-extra control, use only this explicit synthetic command:

```bash
adb -s <serial> shell am start \
  -n com.example.intentvictim/.ReportActivity \
  -a com.example.intentvictim.action.VIEW_REPORT
```

Open Attacker Lab normally from its launcher for its initial-state control. After an interception, force-stop Attacker Lab and open it afresh from the launcher:

```bash
adb -s <serial> shell am force-stop com.example.intentattacker
```

Record any Android task/intent restoration separately from app storage behavior. Inspect app logs during the matrix; only fixed `IntentLab` outcomes are permitted.

## Verification results

Tested on **2026-11-15** with **Pixel_9a_API_37**, **Android 17, API 37** (`sdk_gphone16k_arm64`). Both apps' assemble and lint tasks passed; their JVM unit-test tasks were `NO-SOURCE` after invalid generated ViewModel tests were removed. One retained Views/Espresso startup test per app passed on both Pixel_9a_API_37 and the additionally connected **Medium_Phone_API_36.1**, Android 16/API 36. Merged-manifest inspection confirmed distinct application IDs, exactly matching custom-action filters, only the intended exported activities, and no permissions, URI filters, or unrelated components.

Filtered app-log inspection found only `victim-report-received`, `victim-report-rejected`, and `attacker-report-received` under the `IntentLab` tag. No intent extras, full intents, personal data, or non-lab app information were collected. No lab-created default required clearing. Both lab apps were uninstalled after the matrix, and no lab package or process remained.

## Limitations

- A matching intent filter makes an activity eligible; it does not guarantee selection or interception.
- The fixed custom action is not a secret and provides no caller authentication.
- Explicit component targeting protects this send path's destination, not every possible use of the exported receiver.
- This lab intentionally excludes intent redirection, nested intents, unsafe parcelables, mutable `PendingIntent`, Binder/AIDL, services, broadcasts, and content providers.

## Cleanup

Clear only defaults created for these lab apps through normal Android settings. Uninstall only the two lab application IDs and confirm no lab process remains:

```bash
adb -s <serial> uninstall com.example.intentattacker
adb -s <serial> uninstall com.example.intentvictim
```

## References

- [Android: Implicit intent hijacking](https://developer.android.com/privacy-and-security/risks/implicit-intent-hijacking)
- [Android: Intents and intent filters](https://developer.android.com/guide/components/intents-filters)
