# Content Provider and Secure File Sharing Lab

This directory contains a controlled lab using two separately installed synthetic apps and the fixed string `ANDSEC-DEMO-REPORT-001`. It compares an unnecessarily exported, unprotected content provider with a non-exported `FileProvider` and an explicit, read-only URI grant.

**Exported providers and URI grants are intended Android platform capabilities. This lab does not demonstrate an Android platform vulnerability.** The vulnerable result comes from exposing a provider without requiring caller authorization. Private file permissions do not protect data served by an exported provider, and `FileProvider` is not the source of the vulnerability.

## Components

- `victim/` — Victim Lab owns the fixed private report, the deliberately exported `ReportProvider`, the non-exported `LabFileProvider`, and the hardened sharing and revocation controls.
- `attacker/` — Attacker Lab directly reads two known content URIs and receives the explicit hardened share.
- `plan/` — the threat model, implementation plan, and execution record.

Both apps use Kotlin, traditional Android Views, and no permissions, including no `INTERNET` permission. Only the exact synthetic report may be displayed. Neither app transmits the report or logs report contents, arbitrary exceptions, paths, full URIs, or intents. Logcat messages use fixed `ProviderLab` outcome labels.

## Lab contract

The only asset is `report.txt`, created with private internal-storage defaults below Victim Lab's `filesDir/shared-reports/` directory. Its entire content is `ANDSEC-DEMO-REPORT-001`.

| Element | Exact value |
| --- | --- |
| Vulnerable URI | `content://com.example.providervictim.report/report` |
| Hardened URI | `content://com.example.providervictim.fileprovider/shared_reports/report.txt` |
| Share action | `com.example.providervictim.action.SHARE_REPORT` |
| MIME type | `text/plain` |
| Explicit recipient | `com.example.providerattacker.MainActivity` |

`ReportProvider` accepts only its exact URI and mode `r`. Its manifest declaration is deliberately exported, has no read/write permission, and disables URI grants. `LabFileProvider` is a minimal AndroidX `FileProvider` subclass. Its XML mapping exposes only `shared-reports/`; it is non-exported and allows URI grants.

Victim Lab's share button creates an explicit intent with the exact hardened URI, `ClipData`, and only `FLAG_GRANT_READ_URI_PERMISSION`. It never uses a chooser or implicit fallback. The revoke button calls `revokeUriPermission` for that same URI and read flag.

Attacker Lab validates the exact action, URI, and MIME type before showing its share-received state. Its buttons perform one read and display the report only if the entire content matches the fixed payload. It declares only the two known provider authorities under `<queries>` for Android package visibility; visibility alone does not grant read access. Incoming content is not persisted, copied, forwarded, or transmitted.

## Minimal threat model

Victim Lab, its provider declarations, Android's URI-permission enforcement, and the separately installed Attacker Lab are the trust boundaries. Android and the dedicated emulator are trusted and unmodified.

Attacker Lab knows both lab content URIs and can call `ContentResolver.openInputStream` without declaring permissions. It cannot read the private file directly, select another path, alter the victim manifest, grant itself access, or receive the hardened share without the user's button press.

The vulnerable path is `Attacker MainActivity -> ContentResolver -> exported Victim ReportProvider`. The hardened path is `Victim user action -> explicit attacker activity intent with one read grant -> non-exported Victim LabFileProvider`. A separate victim action revokes that exact URI's read grant.

The receiving app is intentionally authorized for the hardened row; this is not an interception demonstration. URI knowledge is not authorization: before a grant, knowing the non-exported provider's URI does not permit a read.

## Secure development checklist

- Make providers non-exported by default. If cross-app provider access is necessary, require an appropriate permission and expose the smallest possible contract.
- Do not rely on private file permissions to protect content that an exported provider serves to unauthorized callers.
- Use `FileProvider` with `android:exported="false"`, `android:grantUriPermissions="true"`, and narrow directory mappings. Avoid broad roots and external storage when sharing a specific private file.
- Share an exact content URI with an explicit trusted recipient. Include the URI in `ClipData` and grant only the access needed; do not add write, prefix, or persistable flags.
- Revoke temporary URI access when it is no longer needed, and verify both the authorized read and denial after revocation.
- Treat incoming actions, URIs, MIME types, and content as untrusted. Validate the expected contract and never log sensitive payloads or arbitrary exceptions.
- Remember that an authorized recipient can copy content it can read. URI grants control access; they do not provide recipient confinement or digital rights management.

## Build and static checks

Use each standalone project's Gradle wrapper:

```bash
cd content-providers/victim && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
cd content-providers/attacker && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The projects retain Views/Espresso startup tests. On a connected dedicated emulator, run:

```bash
cd content-providers/victim && ./gradlew :app:connectedDebugAndroidTest
cd content-providers/attacker && ./gradlew :app:connectedDebugAndroidTest
```

After assembly, inspect merged manifests under each project's `app/build/intermediates/merged_manifests/debug/`. Confirm distinct application IDs, exact provider authorities, the vulnerable provider's export and disabled grants, the hardened provider's non-exported status and enabled grants, narrow path metadata, only exported launcher activities, and no requested permissions or unrelated components. Review the share/revoke code for one exact read-only grant with no implicit fallback.

## Emulator setup

1. Use a dedicated emulator or non-personal test device and record its Android version, API level, and device/AVD name.
2. Build and install both debug APKs, substituting the emulator serial:

   ```bash
   android install --device=<serial> content-providers/victim/app/build/outputs/apk/debug/app-debug.apk
   android install --device=<serial> content-providers/attacker/app/build/outputs/apk/debug/app-debug.apk
   ```

3. Launch Victim Lab once to create the fixed private report and confirm its labeled UI.
4. Exercise the matrix through the app buttons. Keep Attacker Lab available for a successful hardened read immediately before explicit revocation and the denied read afterward.

## Manual verification matrix

| Scenario | Expected result | Observed result |
| --- | --- | --- |
| Vulnerable direct read, no share | Attacker Lab displays exactly `ANDSEC-DEMO-REPORT-001` without a victim share action | **Pass:** The separate attacker displayed the exact report without any share action. |
| Hardened pre-share denial | The known `FileProvider` URI is denied and no report is displayed | **Pass:** The direct read showed the fixed denial state and no report. |
| Hardened user-authorized share | The explicit share reaches Attacker Lab without a chooser; its subsequent read displays the exact report | **Pass:** Attacker Lab opened directly, showed the share-received state, and then displayed the exact report. |
| Read-only grant | Static/intent inspection confirms only a read grant, with no write, prefix, or persistable grant | **Pass:** The delivered-intent construction contains only `FLAG_GRANT_READ_URI_PERMISSION`; the non-exported provider and narrow path metadata matched the contract. |
| Explicit revocation | After a successful read, victim revocation causes the same attacker read to be denied while the attacker remains available | **Pass:** With the attacker activity/process retained, the read succeeded before revocation and was denied afterward. |
| Vulnerable provider after hardened revocation | The exported provider remains readable | **Pass:** The vulnerable direct read still displayed the exact report. |
| Fresh hardened share after revocation | A new explicit share restores read access; access is revoked again before cleanup | **Pass:** A second share and read succeeded; the victim then revoked the grant again. |
| Fresh-start control | Force-stopped Attacker Lab starts with no restored report content; task/intent restoration is recorded separately | **Pass:** Launcher startup after force-stop showed the initial state and no report. No task/intent restoration was observed. |

For the fresh-start control, force-stop Attacker Lab after a read and open it normally from its launcher:

```bash
adb -s <serial> shell am force-stop com.example.providerattacker
```

Inspect app logs during the matrix. Only fixed `ProviderLab` outcomes are permitted; do not collect report contents, arbitrary exception text, full URIs/intents, personal data, or non-lab app information.

## Verification results

Tested on **2026-11-15** with **Pixel_9a_API_37**, **Android 17, API 37** (`sdk_gphone16k_arm64`). Both apps' assemble and lint tasks passed; their JVM unit-test tasks were `NO-SOURCE` after invalid generated Compose/ViewModel tests were removed. One retained Views/Espresso startup test per app passed. Merged-manifest inspection confirmed distinct application IDs, only the two known provider authorities in attacker package-visibility queries, exact provider export/grant settings, narrow path metadata, only exported launcher activities, and no requested permissions or unrelated components.

Every manual matrix row passed. Filtered app-log inspection found only fixed `ProviderLab` outcomes. No report contents, arbitrary exception text, full URIs/intents, personal data, or non-lab app information were collected in those logs. The final hardened grant was revoked before cleanup. The attacker uninstall command reported `DELETE_FAILED_INTERNAL_ERROR`; direct package-path checks then confirmed that both lab packages were absent, and no lab process remained.

## Limitations

- This lab intentionally excludes path traversal, wildcard/canonical-path handling, SQL injection, database-backed providers, mutable/write sharing, custom provider permissions, and broad-root `FileProvider` variants.
- It does not use `file://` URIs, implicit `ACTION_SEND`, a share sheet, Storage Access Framework, media store, document picker, external storage, or user-selected files.
- The explicit Attacker Lab recipient is intentionally trusted for the shared copy. The lab does not test malicious recipient behavior or prevent an authorized recipient from copying the report.
- Revoking the hardened URI grant does not repair the separately exported vulnerable provider.
- The fixed custom action and URI values are public lab constants, not secrets or authorization mechanisms.

## Cleanup

Revoke any remaining hardened grant, then uninstall only the two lab application IDs and confirm no lab package or process remains:

```bash
adb -s <serial> uninstall com.example.providerattacker
adb -s <serial> uninstall com.example.providervictim
```

## References

- [Android: Content provider permissions](https://developer.android.com/guide/topics/providers/content-provider-creating#Permissions)
- [Android: Set up FileProvider sharing](https://developer.android.com/training/secure-file-sharing/setup-sharing)
- [Android: FileProvider reference](https://developer.android.com/reference/androidx/core/content/FileProvider)
- [Android: Share a file using a content URI and temporary URI grant](https://developer.android.com/training/secure-file-sharing/share-file)
