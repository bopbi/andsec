# AndSec

AndSec is an educational collection of small, controlled Android security labs. Its primary goal is to help developers **design, implement, and verify secure Android apps**. Each lab uses a vulnerable example to explain a failure mode, then identifies hardened behavior, practical mitigations, and the limits of those defenses.

Understanding attack techniques and their preconditions supports that defensive goal. These reproducible demonstrations are not tools for attacking real apps or users.

## How to learn from a lab

1. **Define the security boundary.** Read the threat model and identify the asset, untrusted input or component, attacker preconditions, and intended security property.
2. **Reproduce the failure.** Run the vulnerable variant and its negative controls. Separate an unsafe app decision from intended Android behavior rather than treating every surprising result as a platform vulnerability.
3. **Compare the hardened variant.** Identify exactly which control changes the outcome and which risks remain. A routing change, for example, is not necessarily caller authentication or authorization.
4. **Apply the topic checklist.** Review your own app's manifest, component boundaries, input validation, authentication and authorization, secret handling, and user experience as applicable.
5. **Verify both directions.** Test that attacks, replay, malformed input, and unauthorized calls fail as expected while legitimate accessibility, sharing, login, and navigation continue to work.

Use each topic's documentation as a learning guide, not as a complete production implementation. Confirm current platform behavior and recommendations in the linked official Android and standards documentation.

## Project structure

Each vulnerability class or approach has its own top-level directory with a topic overview and, when needed, implementation plans:

```text
andsec/
├── accessibility/       # Accessibility-related labs and documentation
│   ├── README.md        # Accessibility topic overview and verification results
│   ├── victim/          # Standalone Victim Lab Android project
│   ├── attacker/        # Standalone Attacker Lab Android project
│   └── plan/            # Implementation plans and execution records
├── deep-links/          # Deep-link, App Link, and browser-login redirect labs
│   ├── README.md        # Topic overview and verification results
│   ├── victim/          # Standalone browser-login Victim Lab Android project
│   ├── attacker/        # Standalone custom-scheme interception Attacker Lab project
│   ├── mock-provider/   # Local synthetic authorization and profile server
│   └── plan/            # Implementation plans and execution records
├── intents/             # Intent and inter-process communication labs
│   ├── README.md        # Topic overview and verification results
│   ├── victim/          # Standalone implicit/explicit sender Victim Lab project
│   ├── attacker/        # Standalone matching-action Attacker Lab project
│   └── plan/            # Implementation plans and execution records
├── <future-topic>/      # Added when work on another topic begins
│   ├── README.md
│   └── plan/
└── README.md
```

Each implemented lab should include its own setup, build, verification, and cleanup instructions. Candidate topics below are research directions, not commitments to implement every item.

## Topic roadmap

| Topic | Possible demonstrations | Status |
| --- | --- | --- |
| **Accessibility abuse** | Cross-app UI interaction after explicit service authorization, visibility controls, and negative controls | **Implemented; verified on Android 17 / API 37** |
| **Deep links and app links** | Browser and Chrome Custom Tab login redirects, custom-scheme interception, unvalidated callback parameters, `state` binding, authorization codes with PKCE, and verified versus unverified app links | **Implemented; verified on Android 16 / API 36** |
| **Intents and inter-process communication** | Implicit-intent interception, unsafe extras, intent redirection, mutable `PendingIntent`, and exported activities, services, or broadcast receivers | **Implemented; verified on Android 17 / API 37** |
| **Content providers and file sharing** | Overly broad provider access, path handling mistakes, `FileProvider` configuration, and URI permission grants | Candidate |
| **Network security** | Cleartext traffic, incorrect certificate validation, permissive trust managers, and Network Security Configuration using a local test server | Candidate |
| **Remote configuration and client-side trust** | Feature flags versus authorization, remotely controlled allowlists, configurable endpoint URLs, configuration integrity, and server-side enforcement | Candidate |
| **UI redressing and overlays** | Tapjacking, obscured touches, overlay preconditions, and defensive filtering of synthetic input | Candidate |
| **Permissions and confused deputies** | Implicit capability grants, unsafe URI permissions, and least-privilege component design | Candidate |

Topics can be split or combined as needed to make each lesson clear. Every lab should compare vulnerable and hardened behavior where practical and identify whether the issue comes from app configuration, app code, platform behavior, or an explicitly authorized capability.

## Implemented topic: accessibility

The first demonstration examines how a deliberately enabled Android accessibility service can interact with another app. It uses two synthetic, separately installed apps:

- **Victim Lab** exposes a harmless counter button.
- **Attacker Lab** requests one explicit, short-lived action against that button after the user enables its accessibility service.

The experiment includes negative controls and an accessibility-visibility comparison. It demonstrates the abuse potential of a legitimately authorized, high-privilege capability; it does not demonstrate an Android platform vulnerability or a permission bypass.

See the [accessibility topic documentation](accessibility/README.md) for setup, build instructions, and verified API 37 results, or the [accessibility lab plan](accessibility/plan/2026-11-14-android-accessibility-demo.md) for the design and execution record.

## Implemented topic: browser login redirects

The deep-links topic covers native apps that open a synthetic login in Chrome Custom Tabs or the user's external browser and return through an Android redirect URI. Using a browser or Custom Tab is not itself a vulnerability; the lab examines how redirect responses are routed, validated, and redeemed.

The first experiment compares two custom-scheme responses using two synthetic apps and a local mock identity provider:

- **Vulnerable variant:** A synthetic access token is returned directly in the redirect. A second app claiming the same custom scheme can use the token if Android routes the callback to it.
- **Hardened variant:** A one-time authorization code is protected by PKCE. The second app may still intercept the code but cannot redeem it without the victim's code verifier.

Both variants use fresh `state` values and exact callback validation. The topic documentation also distinguishes related App Link routing, unvalidated callback, OpenID Connect `nonce`, and request-binding concerns without combining them into the first experiment.

See the [deep-links topic documentation](deep-links/README.md) for setup and verified API 36 results, or the [browser login redirect lab plan](deep-links/plan/2026-11-14-browser-login-redirect-demo.md) for the threat model, limitations, and execution record.

## Implemented topic: implicit intent interception

The intents topic uses two synthetic, separately installed apps to compare an implicit activity intent with an explicit-component intent. Both variants carry only the fixed synthetic report `ANDSEC-DEMO-REPORT-001`. Attacker Lab claims the same custom action and demonstrates disclosure if Android routes the vulnerable intent to it; explicit selection in a chooser is not evidence of guaranteed automatic interception.

The hardened variant names Victim `ReportActivity` directly. This selects its destination without authenticating callers or preventing direct invocation of that exported activity.

See the [intents topic documentation](intents/README.md) for setup, build instructions, the verification matrix, and verified API 37 results, or the [implicit intent interception lab plan](intents/plan/2026-11-15-implicit-intent-interception.md) for the design and execution record.

## Learning principles

- Understand the preconditions and security boundaries behind each demonstration.
- Use the smallest harmless scenario that illustrates the concept.
- Record actual observations, including the Android version and negative results.
- Distinguish intended platform capabilities, unsafe app design, and genuine platform vulnerabilities.
- Turn every observation into concrete defensive guidance, including the control's limitations and its effect on legitimate accessibility or other user-facing functionality.

## Safety and scope

All labs must remain limited to synthetic apps and harmless data on a dedicated emulator or non-personal test device. They must not target third-party apps, real accounts, credentials, messages, payments, or personal data.

Do not add permission bypasses, stealth, persistence, obfuscation, anti-analysis behavior, or data exfiltration. Android's system-mediated permission flows and explicit user consent must remain intact.

## Getting started

### Prerequisites

- **JDK 17** for running Gradle. The Android projects compile Java/Kotlin sources for Java 17 and include their own Gradle wrappers; a separate Gradle installation is not needed.
- **Android SDK**, including Platform 37.0 for the accessibility and intents labs and Platform 36.1 for the deep-links labs, plus Platform Tools (`adb`). Install Build Tools and an emulator/system image when using the device instructions. Android Studio's SDK Manager can install these packages.
- **Python 3** for the deep-links mock provider.
- **Android CLI** for the `android` emulator, installation, and layout commands in the topic guides. It is not required for Gradle builds; equivalent Android Studio and `adb` workflows can be used.

To install Android CLI on macOS, run the command for your CPU architecture:

```bash
# Apple silicon
curl -fsSL https://dl.google.com/android/cli/latest/darwin_arm64/install.sh | bash

# Intel
curl -fsSL https://dl.google.com/android/cli/latest/darwin_x86_64/install.sh | bash
```

For Linux or Windows, use the [official Android CLI installation instructions](https://developer.android.com/tools/agents/android-cli). Then initialize and inspect the environment:

```bash
android init
android info
android sdk install platform-tools emulator platforms/android-37.0 platforms/android-36.1
```

Set `ANDROID_HOME` to the SDK location or create an ignored `local.properties` containing `sdk.dir=/path/to/sdk` in each Android project, and put `adb` on `PATH`. Use `android sdk list --all` to inspect packages and `android emulator create --help` when creating a dedicated test AVD.

### Build and verify

There is no root build command. Start with the topic documentation, then use the Gradle wrapper and instructions supplied within each Android project's directory.

Android Studio users should open each app's project directory separately—for example, `accessibility/victim/` or `deep-links/attacker/`—rather than opening the repository root as a single Android project. If the repository root is already open, use **File > Open** to select the relevant nested project directory and let Android Studio configure its Gradle project.

See [CONTRIBUTING.md](CONTRIBUTING.md) for lab-safety rules and contribution checks, and [SECURITY.md](SECURITY.md) for reporting unintended security issues.

## License

AndSec is available under the [MIT License](LICENSE).
