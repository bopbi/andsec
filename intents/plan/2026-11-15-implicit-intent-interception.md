# Implicit Intent Interception Lab

- **Status:** done
- **Created:** 2026-11-15
- **Source:** user request

## Goal
Build the roadmap's next topic as two separately installable Android lab apps that demonstrate how an implicit activity intent can disclose a harmless synthetic report to another app claiming the same custom action. Compare that vulnerable variant with an explicit-component intent that reaches only the intended victim receiver, and record actual routing behavior on a dedicated emulator.

## Context
The root roadmap lists **Intents and inter-process communication** immediately after the two implemented topics and names implicit-intent interception among its possible demonstrations (`README.md:32-44`). Repository guidance requires a narrowly scoped vulnerable/hardened comparison, a minimal threat model, a verification matrix, and a root roadmap status update rather than implementing every IPC candidate together (`AGENTS.md:23-37`).

The accessibility and deep-links labs establish the conventions to reuse: independent `victim/` and `attacker/` Gradle projects, distinct application IDs, Kotlin with traditional Android Views, no shared runtime module, and project-local build/lint/template-test checks followed by manual emulator verification (`accessibility/plan/2026-11-14-android-accessibility-demo.md:25-41`; `deep-links/plan/2026-11-14-browser-login-redirect-demo.md:52-74`). The existing Android projects use AGP 9.1.1 and JDK 17 (`deep-links/victim/gradle/libs.versions.toml:1-19`), compile/target SDK 37 with min SDK 26 (`deep-links/victim/app/build.gradle.kts:5-14`), and retain AndroidX Views/Espresso startup tests (`deep-links/victim/app/src/androidTest/kotlin/com/example/deeplinkvictim/MainActivityTest.kt:14-28`). The Android CLI is available at `/usr/local/bin/android`.

Android's current implicit-intent-hijacking guidance says that an intent without a fully qualified component or package can be received by another app with a matching filter, exposing its extras. It recommends making intents explicit when the destination is known and omitting sensitive content when implicit routing is required. The intent guide distinguishes component-targeted explicit intents from resolver-mediated implicit intents and notes that multiple matching activities can produce a chooser. Therefore, explicit attacker selection can prove impact if the attacker receives the intent, but cannot by itself prove automatic hijacking after installation.

The working tree was clean during planning. No existing `intents/plan/` file covers this topic.

## Non-goals
- No third-party apps, real accounts, credentials, messages, payments, personal data, or sensitive production payloads.
- No intent redirection, nested intents, unsafe `Parcelable`/`Serializable` extras, mutable `PendingIntent`, Binder/AIDL interfaces, services, broadcast receivers, or content providers in this first lab.
- No claim that every implicit intent is unsafe, that a custom action is secret, or that installing Attacker Lab guarantees interception.
- No permission bypass, chooser manipulation, overlay, accessibility interaction, stealth, persistence, boot receiver, obfuscation, anti-analysis behavior, telemetry, network access, or data exfiltration beyond displaying the fixed synthetic payload in the receiving lab app.
- No general-purpose IPC framework, production SDK, or broad scanner.
- No claim that explicit routing authenticates callers or makes an exported receiver's input trustworthy.

## Minimal threat model

### Assets and trust boundaries
The only asset is the fixed synthetic string `ANDSEC-DEMO-REPORT-001`, carried as a string extra. It has no real-world value. The victim sender, Android's intent resolver, intended victim receiver, and separately installed attacker receiver are the relevant boundaries; Android and the emulator are trusted and unmodified.

The vulnerable path is `Victim MainActivity -> Android intent resolution -> matching activity`. Both the intended `ReportActivity` and Attacker Lab declare the same custom action. The hardened path names the victim's `ReportActivity` directly, so Android does not select Attacker Lab for that request.

### Attacker capabilities and preconditions
Attacker Lab has a distinct application ID and can declare a matching activity intent filter, receive an intent routed to it by Android, and read its string extra. It cannot read victim memory, alter the resolver, force itself to be selected, or receive the hardened explicit intent through the demonstrated flow.

Both apps must be installed for the two-handler scenarios. The user must press a victim send button, and an interception row must explicitly record whether Android used a chooser, an existing default, or direct routing. Explicitly choosing Attacker Lab is a controlled impact demonstration, not evidence of automatic interception.

### Expected security properties
- When the vulnerable intent is routed to Attacker Lab, the attacker can recover and display the synthetic report.
- When the hardened intent is sent with both apps installed, only the victim receiver displays the report; no attacker selection or default changes that destination.
- Both receiver UIs handle an absent payload with a fixed rejection message rather than crashing or inventing content.
- Neither app stores, forwards, logs, or transmits the payload.

## Approach
Create `intents/victim/` and `intents/attacker/` as standalone Kotlin/Views projects. Victim Lab contains its normal launcher/send UI and an exported `ReportActivity` with the custom action `com.example.intentvictim.action.VIEW_REPORT`. Attacker Lab's launcher activity also declares that action and category `DEFAULT`, making it a second matching receiver.

Victim Lab offers **Send vulnerable implicit intent** and **Send hardened explicit intent**. Both variants attach exactly the same fixed string extra. The vulnerable variant sets only the action; the hardened variant additionally sets `ReportActivity` as the explicit component. Keep the receiver manifest and payload identical between variants so sender routing is the only experimental change.

Use a fully qualified component rather than only `setPackage()` for the hardened variant because the intended receiver is known and component targeting makes the expected destination unambiguous. Use an activity rather than a service or broadcast because it provides a visible, user-mediated result with the least background behavior. Reuse the established project template and platform `Intent` APIs; add no runtime dependency or shared code module.

Alternatives considered and rejected:
- **A standard share/action intent:** could involve non-lab apps and real user content, violating the controlled scope.
- **An implicit broadcast:** would introduce broadcast visibility, delivery, and background-execution variables that obscure the basic interception lesson.
- **A bound or started service:** Android's background restrictions and invisible service behavior add complexity without improving the first demonstration.
- **Combining intent redirection or unsafe extras:** would introduce a second defect and prevent a clean attribution of disclosure to implicit routing.
- **Making the hardened receiver non-exported as the only change:** would test component exposure rather than isolate sender-side explicit routing. The receiver remains exported and harmless in both variants; its direct-invocation limitation is documented separately.

## Implementation quality
- **Responsibility and integration:** Victim `MainActivity` owns construction and user-initiated sending of the two intents. Victim `ReportActivity` owns validation and display of a received lab report. Attacker `MainActivity` owns its normal startup state and display of any routed lab report. The Android resolver remains the real external collaborator; do not add a repository, service locator, dependency-injection layer, or interface around one `startActivity` call.
- **Reuse and boundaries:** Reuse the repository's Android CLI/Gradle/Views template, existing AGP/SDK/test versions, and framework `Intent`, `ComponentName`, and activity APIs. Duplicate the small action/extra/payload constants in the two apps rather than creating a shared runtime library that would imply a trust relationship. No network, storage, or concurrency seam is needed.
- **Testability and behavior:** Observable behavior is which visibly labeled app displays `ANDSEC-DEMO-REPORT-001`. Retain and adapt template-provided Espresso startup tests to the replacement Views UI; use them for labels, controls, and initial states only. Test actual resolution on the emulator instead of mocking Android's resolver or adding an intent-testing dependency. Manual cases distinguish vulnerable delivery to each possible receiver, hardened delivery with both apps installed, existing-default behavior, malformed/missing extras, and process relaunch without persisted content.
- **Safety and observability:** Every activity is visibly labeled as an intents lab. Only the fixed synthetic value may appear in receiver UI. Logcat, if used, contains fixed outcome labels such as `victim-report-received` or `attacker-report-received` under an `IntentLab` tag, never arbitrary extras or full intent dumps. Both apps declare no permissions, including no `INTERNET` permission.

## Steps
1. **Scaffold the two independent Android projects.**
   - From the repository root, run:
     ```bash
     android create empty-activity --name="Intent Victim Lab" \
       --application-id=com.example.intentvictim \
       --namespace=com.example.intentvictim \
       --min-sdk=26 --output=intents/victim
     android create empty-activity --name="Intent Attacker Lab" \
       --application-id=com.example.intentattacker \
       --namespace=com.example.intentattacker \
       --min-sdk=26 --output=intents/attacker
     ```
   - Preserve independent Gradle wrappers and the working repository conventions: AGP 9.1.1, Gradle 9.3.1, Java/Kotlin 17, compile/target SDK 37, and min SDK 26. Replace generated Compose UI with framework activities and XML layouts, removing only Compose-only dependencies and stale generated test references. Keep JUnit/AndroidX test dependencies already supplied by the template; add no production library.

2. **Implement Victim Lab's sender and intended receiver.**
   - `intents/victim/app/src/main/res/layout/activity_main.xml` and `res/values/strings.xml` — visibly identify **Intent Victim Lab**, explain the synthetic payload, and add the two send buttons and a fixed instructional status view.
   - `intents/victim/app/src/main/kotlin/com/example/intentvictim/MainActivity.kt` — define the package-prefixed action, package-prefixed extra key, and fixed `ANDSEC-DEMO-REPORT-001` payload. The vulnerable button creates `Intent(ACTION_VIEW_REPORT)` with only the string extra and calls `startActivity`. The hardened button creates the same intent but calls `setClass(this, ReportActivity::class.java)` before `startActivity`. Catch `ActivityNotFoundException` and show a fixed no-handler message; do not retry or fall back to another action.
   - `intents/victim/app/src/main/kotlin/com/example/intentvictim/ReportActivity.kt` and `res/layout/activity_report.xml` — on creation and any reused-activity intent, require the exact custom action and a non-null string extra. Display the received synthetic report only for that contract; otherwise show a fixed invalid-report message. Perform no privileged operation, navigation, persistence, or forwarding.
   - `intents/victim/app/src/main/AndroidManifest.xml` — retain the exported launcher activity and declare exported `ReportActivity` with only the custom action and `CATEGORY_DEFAULT` filter. Add no permissions, URI filters, services, receivers, or providers.

3. **Implement Attacker Lab as a visible, limited receiver.**
   - `intents/attacker/app/src/main/res/layout/activity_main.xml` and `res/values/strings.xml` — identify **Intent Attacker Lab**, explain that it claims the same lab action, and provide distinct initial, intercepted-report, and invalid-report states.
   - `intents/attacker/app/src/main/kotlin/com/example/intentattacker/MainActivity.kt` — independently define the matching action, extra key, and fixed lab contract. Handle the launching intent and `onNewIntent` if `singleTop` is used. On a matching action with a string extra, display the intercepted synthetic report; otherwise display the fixed initial/invalid state. Do not persist, forward, automatically reopen the victim, or issue network requests.
   - `intents/attacker/app/src/main/AndroidManifest.xml` — give the launcher activity a second filter containing exactly the same custom action and `CATEGORY_DEFAULT`, with `android:exported="true"`. Add no permissions or other components. Do not declare `CATEGORY_BROWSABLE`, a URI scheme, or unrelated actions.

4. **Retain focused template tests and inspect the static contract.**
   - Adapt each generated startup test to the new Views UI, following `deep-links/victim/app/src/androidTest/kotlin/com/example/deeplinkvictim/MainActivityTest.kt:14-28`: assert visible lab labeling and controls/initial state, not Android routing internals. Keep any still-valid generated JVM test; do not introduce Robolectric, Espresso Intents, a new test framework, or resolver mocks.
   - Assemble both apps and inspect their merged manifests. Confirm the distinct application IDs, exact matching custom-action filters, intended exported activities, absence of unrelated components, and absence of all permissions. Review both send paths to ensure they differ only by explicit component targeting.

5. **Document the topic, limitations, and repository status.**
   - Create `intents/README.md` with component descriptions, the minimal threat model above, setup/build commands, exact send and receiver behavior, the verification matrix, cleanup, and official links to Android's **Implicit intent hijacking** and **Intents and intent filters** guides.
   - State prominently that implicit intents are intended platform capabilities; the vulnerable result comes from sending data without selecting a trusted destination. Explain that explicit selection of Attacker Lab demonstrates conditional impact, not guaranteed automatic hijacking, and that explicit routing does not validate direct calls to an exported activity.
   - Update `README.md` to add `intents/` to the project structure and an implemented-topic summary. Change the roadmap status to implemented only after the build and device matrix pass, recording the actual Android version/API level. Leave the other IPC candidates as future possibilities rather than marking them implemented.
   - Update `AGENTS.md` with the new topic layout, implementation/API verification summary, and project-local build commands so the canonical guide matches the repository. Provider-specific guide symlinks require no separate edits.

6. **Run static checks and the complete emulator matrix.**
   - Run every command in Verification, including connected template tests on a dedicated emulator. Record actual API level, device/AVD, chooser/default behavior, and outcomes in `intents/README.md`; never infer device results from source inspection.
   - After the matrix, clear only lab-created default associations, uninstall only the two lab application IDs, and confirm no lab process remains. Update the root roadmap and `AGENTS.md` with only the verified status.

## Assumptions
- The ordered root roadmap makes intents/IPC the selected next topic; its first lab isolates implicit activity interception. If a later roadmap reorder is desired, this plan's file paths and scope still represent a standalone lab rather than a prerequisite for other topics.
- The existing SDK 37 / AGP 9.1.1 / Gradle 9.3.1 configuration remains the working baseline (`deep-links/victim/gradle/libs.versions.toml:1-19`). If the CLI generates different versions, align them with the existing working projects instead of upgrading unrelated dependencies; if that baseline cannot resolve, record the blocker rather than mixing arbitrary versions.
- A dedicated current Android emulator is available or can be created with the installed Android CLI. If routing UI differs by API level, record the actual behavior and use explicit attacker selection for the interception row; do not claim default hijacking.
- The template supplies connected Android tests. If it generates no valid test after the Views conversion, run assemble/lint and the complete manual matrix rather than adding a new framework; document any `NO-SOURCE` result accurately.
- Attacker Lab can display the fixed payload because disclosure is the harmless observable being tested. No secret-redaction requirement applies to this synthetic report, but arbitrary intent contents must still not be logged or dumped.

## Needs your decision
None.

## Risks
- **Overclaiming interception:** Matching filters do not guarantee which app Android opens. Record chooser, default, and explicit-selection evidence separately.
- **Confusing routing with caller authentication:** The hardened intent selects a destination but does not stop another app from explicitly invoking exported `ReportActivity`. Keep that activity harmless and document the limitation.
- **Accidentally changing two variables:** Do not change payload, action, receiver export status, or receiver validation between vulnerable and hardened sends. Only component targeting differs.
- **Real-app interaction:** Use only the package-prefixed custom action and lab apps; do not substitute `ACTION_SEND`, web URIs, or third-party handlers.
- **Payload leakage beyond the demonstration:** Do not log full intents/extras, persist the report, forward it, or add network permissions.
- **Template drift:** Compose-generated tests or stale dependencies can fail after Views conversion. Remove only invalidated generated references and retain the repository's established versions.
- **Task/process restoration:** Android may recreate an activity with its last intent. Verify fresh launcher behavior after force-stop/uninstall-reinstall rather than claiming the app has its own persistence based solely on task restoration.

## Verification
1. Run the standard checks for each standalone project:
   ```bash
   cd intents/victim && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
   cd intents/attacker && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
   ```
   Both commands must pass. Run retained connected tests on the dedicated emulator:
   ```bash
   cd intents/victim && ./gradlew :app:connectedDebugAndroidTest
   cd intents/attacker && ./gradlew :app:connectedDebugAndroidTest
   ```

2. Inspect both merged manifests under each project's `app/build/intermediates/merged_manifests/debug/`. Confirm only the intended launcher/receiver activities are exported, both custom-action filters match exactly, no `CATEGORY_BROWSABLE` or URI filter is present, and neither app requests `INTERNET` or any other permission.

3. Install the debug APKs on one dedicated emulator and record its Android version/API level. Exercise and record this matrix:
   - **Vulnerable intended baseline:** With Attacker Lab absent or not selected, send the vulnerable intent. Victim `ReportActivity` displays the fixed report.
   - **Vulnerable intended receiver with both installed:** When Android presents a choice, select Intent Victim Lab. The victim displays the report and Attacker Lab does not receive it.
   - **Vulnerable interception:** Explicitly select Intent Attacker Lab for the same vulnerable intent. It displays the fixed report, proving disclosure if routed there; the victim receiver does not display that request.
   - **Hardened with both installed:** Send the hardened intent. Android opens only Victim `ReportActivity`, with no attacker option and no attacker receipt.
   - **Hardened against a lab-created default:** If the test image supports choosing Attacker Lab as default for the custom action, create that default through normal system UI and repeat the hardened send. The explicit intent must still open only the victim; clear the lab-created default afterward. If no default can be created, record that limitation rather than fabricating the row.
   - **Missing-extra control:** Open each receiver without the expected extra, using normal attacker launcher startup and an explicit `adb shell am start -n com.example.intentvictim/.ReportActivity -a com.example.intentvictim.action.VIEW_REPORT` for the victim. Each shows its fixed initial/invalid state and does not crash.
   - **Fresh-start control:** Force-stop Attacker Lab after an interception and open it afresh from its launcher. It must not load a report from app storage. Record any Android task/intent restoration separately if observed.
   - **Routing observation:** For every vulnerable run, record whether resolution was direct, chooser-mediated, or default-mediated. Do not convert explicit attacker selection into a claim of automatic interception.

4. Inspect app logs during the matrix. Only fixed `IntentLab` outcomes are permitted; no arbitrary extra values, full intent dumps, personal data, or non-lab app information may be collected.

5. Fill `intents/README.md` with actual outcomes and versions, then clean up:
   ```bash
   adb -s <serial> uninstall com.example.intentattacker
   adb -s <serial> uninstall com.example.intentvictim
   ```
   Clear only defaults created for these lab apps and confirm no lab process remains. Do not mark device scenarios verified when only static checks have run.

## Model recommendation
- **Overall tier:** standard — the scope is explicit and follows an established two-app pattern, but it introduces new Android component behavior and requires non-trivial resolver/device verification rather than mechanical edits.
- **Compatible model range:** `claude-sonnet-5-5`; `gpt-6.1-terra`; `gemini-3.8-flash`; `kimi-k3.1-pro` with `reasoning_effort: "high"`; `qwen3.7-plus`; `grok-4.6`.
- **Execution compatibility:** Any listed model, or a verified higher-tier model, is acceptable even when it differs from the model used to create the plan. Record the actual provider, model, and tier in the execution log.

## How to execute
Run `/run-plan intents/plan/2026-11-15-implicit-intent-interception.md` when you are ready. There are no open decision lines to fill in.

## Execution log
- **Planned tier / compatible model range:** standard / `claude-sonnet-5-5`; `gpt-6.1-terra`; `gemini-3.8-flash`; `kimi-k3.1-pro` with `reasoning_effort: "high"`; `qwen3.7-plus`; `grok-4.6`.
- **Executed:** 2026-11-15 on Kimi (Moonshot) / `kimi-k3.1-pro` with high reasoning (standard).
- **Provider switch:** none.
- **Steps:** 1 ✅, 2 ✅, 3 ✅, 4 ✅, 5 ✅, 6 ✅ (the unsupported attacker-default variation was recorded as the plan permitted).
- **Verification:** Both `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` commands passed; JVM tests were `NO-SOURCE`. Both `:app:connectedDebugAndroidTest` commands passed one startup test per app on Pixel_9a_API_37/Android 17/API 37 and Medium_Phone_API_36.1/Android 16/API 36. Merged-manifest inspection passed. The API 37 manual matrix passed the baseline, chooser-mediated victim and attacker delivery, hardened direct delivery, missing-extra, fresh-start, routing, and fixed-log checks. The resolver did not offer a way to make alternate Attacker Lab the default, so that allowed limitation was documented rather than inferred. Both apps were uninstalled; no lab package or process remained, and the emulator started for the matrix was stopped.
- **Quality review:** Passed. Victim sender/receiver and attacker receiver retain their stated responsibilities; framework intent APIs and the existing Views/test template were reused without runtime dependencies or unnecessary abstractions. Focused Espresso tests cover labels, controls, and initial state, while actual resolver behavior was exercised on-device. Payloads, logs, permissions, and components remain within the fixed synthetic contract.
- **Deviations / decisions made:**
  - The CLI generated AGP 9.0.1, Gradle 9.1.0, and a Compose/Navigation template; these were aligned to the planned AGP 9.1.1, Gradle 9.3.1, and Views baseline as the assumptions directed.
  - Generated ViewModel JVM tests depended on removed template code and were deleted, producing the documented `NO-SOURCE` result; valid Espresso tests were retained and adapted.
  - Receivers require equality with the fixed synthetic payload, not merely a non-null string, to enforce the plan's fixed-value-only UI safety contract.
  - The API 37 resolver offered **Always** for primary Victim Lab but only one-shot selection for alternate Attacker Lab. No attacker default was created, and the corresponding matrix row records that limitation.
- **Follow-ups:** none.
