# Content Provider and Secure File Sharing Lab

- **Status:** done
- **Created:** 2026-11-15
- **Source:** user request

## Goal
Build a new `content-providers/` topic with two separately installable Android lab apps that expose the same harmless private report through an unnecessarily exported, unprotected provider and through a non-exported `FileProvider`. Demonstrate unauthorized direct reading in the vulnerable path, then user-initiated read-only sharing and explicit revocation in the hardened path, with results recorded on a dedicated emulator.

## Context
The root roadmap lists **Content providers and file sharing** as a candidate and names overly broad provider access, path handling, `FileProvider` configuration, and URI grants as possible lessons (`README.md:37-50`). Repository guidance calls for a narrowly scoped vulnerable/hardened comparison, a minimal threat model, actual device verification, and a roadmap update rather than implementing every candidate at once (`AGENTS.md:23-37`; `CONTRIBUTING.md:18-32`).

The existing labs establish the project pattern: independent `victim/` and `attacker/` Gradle projects, distinct application IDs, Kotlin with traditional Android Views, no shared runtime module, and project-local build/lint/template-test checks (`intents/plan/2026-11-15-implicit-intent-interception.md:10-17`). The most recent apps use AGP 9.1.1, Gradle 9.3.1, Java/Kotlin 17, compile/target SDK 37, and min SDK 26 (`intents/victim/gradle/libs.versions.toml:1-17`; `intents/victim/gradle/wrapper/gradle-wrapper.properties:1-8`; `intents/victim/app/build.gradle.kts:5-14`). Their generated Espresso startup tests are retained and adapted to the Views UI (`intents/victim/app/src/androidTest/kotlin/com/example/intentvictim/MainActivityTest.kt:14-24`). The current production dependency set does not include AndroidX Core (`intents/victim/app/build.gradle.kts:43-49`), so the victim must add it specifically for `FileProvider`; the attacker needs no new production library.

Android's provider documentation states that an exported provider without permissions can be read or written by other apps even when its underlying internal-storage data is private, and that URI grants can temporarily override export/permission restrictions for a specific content URI. The [`FileProvider` setup guide](https://developer.android.com/training/secure-file-sharing/setup-sharing) specifies `android:exported="false"`, `android:grantUriPermissions="true"`, and a narrow XML directory mapping. The [`FileProvider` reference](https://developer.android.com/reference/androidx/core/content/FileProvider) recommends a subclass for device reliability, supports intent-based temporary read grants, and documents explicit revocation. The [AndroidX Core release notes](https://developer.android.com/jetpack/androidx/releases/core) list `androidx.core:core:1.19.1` as the current stable release.

The working tree was clean during planning. The repository has no root `plan/` directory and no existing plan for this topic.

## Non-goals
- No third-party apps, real documents, accounts, credentials, messages, payments, photos, personal data, external storage, or broad device-file access.
- No path-traversal demonstration, wildcard/canonical-path handling, SQL injection, database-backed provider, mutable/write sharing, or custom provider permission in this first lab.
- No `file://` URI sharing, implicit `ACTION_SEND`, general share sheet, Storage Access Framework, media store, or document picker.
- No claim that private file permissions alone protect an exported provider, that a content URI is secret, or that `FileProvider` prevents an authorized recipient from copying content.
- No permission bypass, stealth, persistence beyond the one fixed lab file, boot receiver, obfuscation, anti-analysis behavior, telemetry, networking, or exfiltration beyond displaying the fixed synthetic report in Attacker Lab.
- No general-purpose sharing SDK, production content-provider framework, or broad vulnerability scanner.

## Minimal threat model

### Asset and trust boundaries
The only asset is the fixed synthetic text `ANDSEC-DEMO-REPORT-001`, stored as `report.txt` below Victim Lab's private `filesDir/shared-reports/` directory. Victim Lab, its two provider declarations, Android's URI-permission enforcement, and the separately installed Attacker Lab are the relevant boundaries. Android and the dedicated emulator are trusted and unmodified.

Both providers represent the same report so the observed payload remains constant. The vulnerable path is `Attacker MainActivity -> ContentResolver -> exported Victim ReportProvider`. The hardened path is `Victim user action -> explicit attacker activity intent with one read grant -> non-exported Victim LabFileProvider`; a separate victim action revokes that URI's read grant.

### Attacker capabilities and preconditions
Attacker Lab knows both lab content URIs and can call `ContentResolver.openInputStream` without declaring permissions. It cannot read Victim Lab's private file directly, choose a different victim path, alter the victim's manifest, grant itself URI access, or receive the hardened share without the user's button press.

Both apps must be installed. The vulnerable read requires no share action. The hardened success requires the user to tap Victim Lab's explicit share button. The receiving app is intentionally authorized for that row; this is not an interception demonstration.

### Expected security properties
- Before any sharing, Attacker Lab can read the fixed report through the exported unprotected provider.
- Before any sharing, the same read against the non-exported `FileProvider` URI is denied.
- After the explicit hardened share, Attacker Lab can read only the granted report URI as read-only content; no write grant is issued.
- After Victim Lab explicitly revokes that grant while the attacker remains available for a repeat read, the hardened read is denied again. The vulnerable provider remains readable, proving revocation is URI/provider-specific rather than a global fix.
- Neither app accepts arbitrary report text for display, logs content or full URIs, requests permissions, or transmits data.

## Approach
Create `content-providers/victim/` and `content-providers/attacker/` using the established standalone Kotlin/Views template. A small victim `DemoReport` owner creates and locates the one fixed private file. `ReportProvider` deliberately exposes only its exact report URI with `android:exported="true"` and no read permission. A separate `LabFileProvider` subclass exposes the same file through a non-exported AndroidX `FileProvider` configured only for `shared-reports/`.

Victim Lab's hardened share uses an explicit intent naming `com.example.providerattacker.MainActivity`, the exact `FileProvider` URI, MIME type `text/plain`, and `FLAG_GRANT_READ_URI_PERMISSION`. A revoke button calls `revokeUriPermission` for that same URI and read flag. Attacker Lab provides explicit buttons for reading the known vulnerable and hardened URIs, handles the custom share intent in memory, and reports fixed success/denial states. It never receives a write flag.

This design demonstrates both unauthorized provider exposure and legitimate least-privilege file sharing without involving non-lab apps. Reusing the same file avoids conflating payload differences with access control. Two provider declarations are necessary because export status is manifest configuration, not a safe runtime toggle.

Alternatives considered and rejected:
- **Only adding a signature permission to the custom provider:** would demonstrate caller authorization but not temporary per-file sharing, and would add permission-definition complexity.
- **Changing only `exported` on one custom provider:** would isolate export behavior more narrowly but omit the roadmap's secure file-sharing lesson and still require a custom file-serving implementation.
- **A broad-root or path-traversal `FileProvider` variant:** would introduce a second defect and caller-controlled paths; the first lab instead uses exact fixed paths and leaves traversal for a possible future lab.
- **A standard implicit share sheet:** could disclose to third-party apps and would make recipient selection less deterministic; explicit lab-only sharing preserves consent and scope.

## Implementation quality
- **Responsibility and integration:** Victim `DemoReport` owns the fixed payload, private directory, and file creation. Victim `ReportProvider` owns only the vulnerable exact-URI read contract. `LabFileProvider` remains a minimal AndroidX subclass; XML owns its allowed root. Victim `MainActivity` owns user-initiated share/revoke actions. Attacker `MainActivity` owns incoming-intent validation, explicit read attempts, and fixed outcome display. Android's `ContentResolver` and URI-grant enforcement are the real collaborators; add no repository, service locator, dependency-injection layer, or provider interface.
- **Reuse and boundaries:** Reuse the existing Gradle/Views/Espresso template and framework `ContentProvider`, `ContentResolver`, `ParcelFileDescriptor`, `Intent`, and `File` APIs. Add only `androidx.core:core:1.19.1` to the victim for `FileProvider`; add no production dependency to the attacker. Duplicate the small URI/action/payload constants across the two apps rather than creating a shared runtime module that implies a trust relationship. `DemoReport` is justified because the activity and both providers must address the same file.
- **Testability and behavior:** Observable behavior is whether the visibly labeled attacker app displays the fixed report or a fixed denial before sharing, after sharing, and after revocation. Retain and adapt the template Espresso tests for labels, controls, and initial states. Exercise real cross-app provider access and grants on the emulator rather than mocking `ContentResolver` or adding a provider-test framework. Inspect merged manifests for provider export/grant settings, exact authorities, narrow path metadata, and absence of permissions.
- **Safety and observability:** Every screen is visibly labeled as a content-provider lab. Only the exact expected synthetic payload may be rendered as report content. Logcat, if used, contains fixed outcomes such as `vulnerable-read-succeeded` or `hardened-read-denied` under `ProviderLab`, never report contents, arbitrary exceptions, paths, or full URIs. Both apps declare no permissions, including no `INTERNET` permission.

## Steps
1. **Create the topic overview and scaffold the two independent projects.**
   - Create `content-providers/README.md` before application code with the planned scope, minimal threat model, and verification matrix marked as not yet run. Update the root roadmap status to **In progress**, leaving other candidate demonstrations unimplemented.
   - From the repository root, run:
     ```bash
     android create empty-activity --name="Provider Victim Lab" \
       --application-id=com.example.providervictim \
       --namespace=com.example.providervictim \
       --min-sdk=26 --output=content-providers/victim
     android create empty-activity --name="Provider Attacker Lab" \
       --application-id=com.example.providerattacker \
       --namespace=com.example.providerattacker \
       --min-sdk=26 --output=content-providers/attacker
     ```
   - Preserve independent wrappers and align with AGP 9.1.1, Gradle 9.3.1, Java/Kotlin 17, compile/target SDK 37, and min SDK 26. Replace generated Compose UI with framework activities and XML layouts, removing only Compose-only dependencies and stale generated test references. In the victim version catalog and `app/build.gradle.kts`, add only `androidx.core:core:1.19.1`; keep the existing JUnit/AndroidX test dependencies.

2. **Implement the fixed private report and deliberately vulnerable provider.**
   - `content-providers/victim/app/src/main/kotlin/com/example/providervictim/DemoReport.kt` — own the fixed payload, `File(filesDir, "shared-reports/report.txt")`, and an idempotent `ensureCreated(context)` operation that creates only that directory/file with private internal-storage defaults. Expose no caller-controlled filename or path.
   - `content-providers/victim/app/src/main/kotlin/com/example/providervictim/ReportProvider.kt` — define authority `com.example.providervictim.report` and exact URI `content://com.example.providervictim.report/report`. Ensure the fixed file exists during provider initialization. Override `openFile` to accept only that exact URI and mode `r`, returning a read-only `ParcelFileDescriptor`; reject every other path/mode. Return `text/plain` only for the exact URI and make query/insert/update/delete unsupported rather than adding unused surface.
   - `content-providers/victim/app/src/main/AndroidManifest.xml` — declare `ReportProvider` with `android:exported="true"`, its exact authority, no read/write permission, and `android:grantUriPermissions="false"`. This is the deliberate defect; do not describe the underlying private file as world-readable.

3. **Implement the hardened `FileProvider` and victim share/revoke UI.**
   - `content-providers/victim/app/src/main/kotlin/com/example/providervictim/LabFileProvider.kt` — subclass `FileProvider` with no additional file-serving behavior.
   - `content-providers/victim/app/src/main/res/xml/file_paths.xml` — configure only `<files-path name="shared_reports" path="shared-reports/" />`; do not use `.` roots, cache roots, or external storage.
   - `content-providers/victim/app/src/main/AndroidManifest.xml` — declare `LabFileProvider` with authority `com.example.providervictim.fileprovider`, `android:exported="false"`, `android:grantUriPermissions="true"`, and `android.support.FILE_PROVIDER_PATHS` metadata referencing that XML. Keep only the launcher activity exported.
   - `content-providers/victim/app/src/main/kotlin/com/example/providervictim/MainActivity.kt` — ensure the fixed file exists, obtain `content://com.example.providervictim.fileprovider/shared_reports/report.txt` with `FileProvider.getUriForFile`, and implement **Share hardened report with Attacker Lab** using an explicit intent to `com.example.providerattacker.MainActivity`, custom action `com.example.providervictim.action.SHARE_REPORT`, `setDataAndType(uri, "text/plain")`, the URI in `ClipData`, and only `FLAG_GRANT_READ_URI_PERMISSION`. Catch a missing-handler failure without falling back to implicit sharing. Implement **Revoke hardened report access** with `revokeUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)` and a fixed status. Keep any last-share state in memory only.
   - `content-providers/victim/app/src/main/res/layout/activity_main.xml` and `res/values/strings.xml` — visibly label **Provider Victim Lab**, identify the fixed synthetic asset, distinguish the deliberately exported provider from the hardened sharing path, and provide the share/revoke controls and fixed status text.

4. **Implement Attacker Lab's limited reader and share receiver.**
   - `content-providers/attacker/app/src/main/kotlin/com/example/providerattacker/MainActivity.kt` — independently define both exact URIs, the custom share action, and expected payload. Add **Read exported provider** and **Read hardened FileProvider URI** buttons. Each performs one `ContentResolver.openInputStream` read, closes the stream, and displays the fixed report only when the entire content equals `ANDSEC-DEMO-REPORT-001`; otherwise show fixed invalid/denied states. Catch expected security/I/O failures without logging exception contents.
   - Handle the launching intent and `onNewIntent` when using `singleTop`: accept only the exact custom action, exact hardened URI, and `text/plain`, retain no report content, and show a fixed **Hardened share received; press read** state. Normal launcher startup shows the initial state. Do not automatically forward, persist, copy, or transmit the report or URI.
   - `content-providers/attacker/app/src/main/res/layout/activity_main.xml`, `res/values/strings.xml`, and `app/src/main/AndroidManifest.xml` — visibly label **Provider Attacker Lab**, explain the two direct-read controls, and retain only the exported launcher activity. The explicit share needs no additional implicit intent filter. Add no permissions or other components.

5. **Retain focused template tests and inspect the static contract.**
   - Adapt each generated startup test following `intents/victim/app/src/androidTest/kotlin/com/example/intentvictim/MainActivityTest.kt:14-24`: assert visible lab labels, buttons, and initial status only. Keep any still-valid generated JVM test; do not introduce Robolectric, provider mocks, or a new test framework.
   - Assemble both apps and inspect merged manifests. Confirm distinct application IDs; the vulnerable provider is exported with no permission and grants disabled; the `FileProvider` is non-exported with grants enabled and only the narrow path metadata; only launcher activities are exported; and neither app requests permissions. Review both share/revoke paths to ensure no write/prefix grant or implicit fallback exists.

6. **Complete topic documentation and repository guidance.**
   - Update `content-providers/README.md` with component descriptions, exact URI/action constants, setup/build commands, the threat model, the full verification matrix, actual outcomes/API levels, limitations, cleanup, and official links to the provider permissions, `FileProvider` setup/reference, and URI-grant documentation.
   - State prominently that exported providers and URI grants are intended platform capabilities; the vulnerability is the unnecessary unprotected export, not Android's private-file storage or `FileProvider`. Explain that URI knowledge is not authorization, the explicit recipient is intentionally trusted for the shared copy, and this lab does not test path traversal or malicious recipient behavior.
   - Update `README.md` to add `content-providers/` to the project structure and an implemented-topic summary only after verification passes; change the roadmap status to implemented with the actual Android version/API level. Update `AGENTS.md` with the topic layout, implementation/verification summary, and project-local build commands. Provider-specific guide symlinks require no separate edits.

7. **Run static checks and the complete emulator matrix, then clean up.**
   - Run every command in Verification, including connected template tests on a dedicated emulator. Record the actual device/AVD, Android/API version, all denials and successes, and any unrun row in `content-providers/README.md`; never infer grant behavior from source inspection.
   - Revoke any remaining lab grant, uninstall only the two lab application IDs, and confirm no lab process remains. Update the root roadmap and `AGENTS.md` with only the verified status.

## Assumptions
- The existing SDK 37 / AGP 9.1.1 / Gradle 9.3.1 baseline remains suitable. If the CLI generates different versions, align with the working projects rather than upgrading unrelated dependencies; if that baseline cannot resolve, record the blocker instead of mixing arbitrary versions.
- AndroidX Core 1.19.1, documented as stable, supports the existing min SDK 26 / SDK 37 template. If resolution reveals an incompatibility, use the newest stable Core release compatible with that unchanged baseline and record the substitution; do not replace `FileProvider` with a custom hardened file server.
- A dedicated current Android emulator is available or can be created with the installed Android CLI. If its grant lifetime UI or task behavior differs, keep the attacker task/process controlled for the before/after-revocation comparison and record actual behavior rather than claiming automatic grant persistence.
- The template supplies connected Android tests. If it generates no valid test after the Views conversion, run assemble/lint and the complete manual matrix rather than adding a new framework; document any `NO-SOURCE` result accurately.
- Explicit `revokeUriPermission` on the shared URI is supported on the tested API as documented. If the controlled post-revocation read still succeeds, the primary goal is not verified: investigate grant/URI/flag mismatches and record the unresolved behavior rather than substituting task dismissal as proof of revocation.

## Needs your decision
None.

## Risks
- **Comparing different assets:** Both providers must serve the same fixed file and payload; otherwise a content difference could obscure the access-control result.
- **Grant lifetime mistaken for revocation:** An activity-stack grant may expire naturally. Keep the recipient available and demonstrate a successful read immediately before explicit revocation and a denied read afterward; do not use merely finishing the attacker task as the revocation row.
- **Accidentally granting excess access:** Use one exact URI and read flag only. Do not add write, prefix, persistable, or provider-wide package grants.
- **Authority or path mismatch:** The manifest authority, `FileProvider.getUriForFile` authority, XML mapping, and attacker constant must agree exactly. A `FileNotFoundException` from a wrong URI is not evidence of access denial; distinguish fixed invalid/unknown outcomes from `SecurityException` denial during diagnosis.
- **Overclaiming recipient confinement:** The hardened path intentionally lets Attacker Lab read the shared report. It does not prevent copying by a real recipient; this lab's receiver simply avoids copying by design.
- **Real-file or third-party interaction:** Never substitute user-selected files, external storage, `ACTION_SEND`, or non-lab receivers for the fixed explicit flow.
- **Sensitive logging:** Do not log file contents, arbitrary exception messages, full intents, or URIs. Use only fixed `ProviderLab` outcomes.
- **Template drift:** Compose-generated tests or stale dependencies can fail after Views conversion. Remove only invalidated generated references and retain the repository's established versions.

## Verification
1. Run the standard checks for each standalone project:
   ```bash
   cd content-providers/victim && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
   cd content-providers/attacker && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
   ```
   Both commands must pass. Run retained connected tests on the dedicated emulator:
   ```bash
   cd content-providers/victim && ./gradlew :app:connectedDebugAndroidTest
   cd content-providers/attacker && ./gradlew :app:connectedDebugAndroidTest
   ```

2. Inspect both merged manifests under each project's `app/build/intermediates/merged_manifests/debug/`. Confirm exact provider authorities and export/grant settings, no provider permissions on the deliberately vulnerable provider, no permissions requested by either app, no external/broad `FileProvider` roots, and no unrelated components.

3. Install both debug APKs on one dedicated emulator, launch Victim Lab once to confirm the fixed asset UI, and record its Android version/API level. Exercise and record this matrix through the apps' own buttons:
   - **Vulnerable direct read, no share:** Attacker Lab reads the exported provider and displays exactly `ANDSEC-DEMO-REPORT-001` without any victim share action.
   - **Hardened pre-share denial:** Attacker Lab's direct read of the known `FileProvider` URI is denied and displays no report.
   - **Hardened user-authorized share:** Tap the victim's explicit share button. Attacker Lab shows the fixed share-received state; its subsequent hardened read displays the exact report. No chooser or third-party app appears.
   - **Read-only grant:** Review the delivered intent flags and manifest/static path to confirm no write or persistable grant was sent; do not add a write operation to the lab solely for this row.
   - **Explicit revocation:** With the attacker still available, return to Victim Lab, revoke the hardened URI read grant, return to Attacker Lab, and repeat the same hardened read. It is denied and no report is displayed.
   - **Vulnerable provider after hardened revocation:** Repeat the vulnerable direct read. It still succeeds, demonstrating that revoking one hardened URI does not repair the separate exported provider.
   - **Fresh hardened share after revocation:** Share again and confirm the hardened read succeeds again; revoke before cleanup.
   - **Fresh-start control:** Force-stop Attacker Lab after a read and open it normally from its launcher. It shows its initial state and does not restore report content from app storage; any Android task/intent restoration is recorded separately.

4. Inspect app logs during the matrix. Only fixed `ProviderLab` outcomes are permitted; no report contents, arbitrary exception text, full URIs/intents, personal data, or non-lab app information may be collected.

5. Fill `content-providers/README.md` with actual outcomes and versions, then clean up:
   ```bash
   adb -s <serial> uninstall com.example.providerattacker
   adb -s <serial> uninstall com.example.providervictim
   ```
   Confirm no lab package or process remains. Do not mark device scenarios verified when only static checks have run.

## Model recommendation
- **Overall tier:** standard — the two-app pattern and exact design are established, but the plan introduces new provider/file-sharing behavior, one dependency boundary, and non-trivial real URI-grant verification.
- **Compatible model range:** `claude-sonnet-5-5`; `gpt-6.1-terra`; `gemini-3.8-flash`; `kimi-k3.1-pro` with `reasoning_effort: "high"`; `qwen3.7-plus`; `grok-4.6`.
- **Execution compatibility:** Any listed model, or a verified higher-tier model, is acceptable even when it differs from the model used to create the plan. Record the actual provider, model, and tier in the execution log.

## How to execute
Run `/run-plan plan/2026-11-15-content-provider-file-sharing.md` when you are ready. There are no open decision lines to fill in.

## Execution log
- **Planned tier / compatible model range:** standard / `claude-sonnet-5-5`; `gpt-6.1-terra`; `gemini-3.8-flash`; `kimi-k3.1-pro` with `reasoning_effort: "high"`; `qwen3.7-plus`; `grok-4.6`
- **Executed:** 2026-11-15 on Kimi (Moonshot) / `kimi-k3.1-pro` with `reasoning_effort: "high"` (standard)
- **Provider switch:** none
- **Steps:** 1 ✅, 2 ✅, 3 ✅, 4 ✅, 5 ✅, 6 ✅, 7 ✅
- **Verification:** Both `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` commands passed; JVM unit tests were `NO-SOURCE`. Both `:app:connectedDebugAndroidTest` commands passed one Views/Espresso startup test each on Pixel_9a_API_37, Android 17/API 37 (`sdk_gphone16k_arm64`). Merged-manifest and narrow-path inspections passed. Every manual cross-app matrix row passed, including explicit revocation with the attacker retained, fresh-share recovery, and fresh-start behavior. Filtered logs contained only fixed `ProviderLab` outcomes. Cleanup confirmed no lab package or process remained.
- **Quality review:** Passed. `DemoReport`, each provider, and the two activities retain their planned responsibilities; no extra provider abstraction or shared runtime module was introduced. Focused tests cover labels, controls, and initial states; real emulator calls cover cross-app access, read-only sharing, revocation, and fresh starts. Share/revoke paths use only the exact URI and read flag, with no implicit fallback or sensitive logging.
- **Deviations / decisions made:**
  - Removed stale generated Compose/serialization plugin aliases and invalidated generated tests while retaining the established template versions.
  - Used `ClipData.newUri` for the required URI grant and added fixed file-not-found outcomes without exception-content logging.
  - Excluded Core's transitive `lifecycle-runtime` and removed its synthetic dynamic-receiver permission entries to satisfy the no-permissions/no-unrelated-components contract; `FileProvider` behavior was verified on-device afterward.
  - Added attacker `<queries>` entries for only the two known provider authorities after API 37 returned `FileNotFoundException` without provider visibility; this grants visibility, not content access.
  - Attacker cleanup returned `DELETE_FAILED_INTERNAL_ERROR`; subsequent direct package-path checks confirmed both apps absent and process checks confirmed no lab process remained.
  - The unrelated untracked `plan/2026-11-15-security-research-references.md` was left untouched.
- **Follow-ups:** none
