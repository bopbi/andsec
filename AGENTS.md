# AndSec Agent Guide

## Purpose

AndSec is an educational Android security repository whose primary goal is to teach developers how to design, implement, and verify secure apps. Controlled demonstrations of vulnerability classes and attack techniques provide the evidence for defensive lessons, hardened variants, and their limitations. Each top-level topic directory covers a different approach. The first topic is accessibility abuse; more topics will be added over time.

## Repository layout

- `accessibility/` — accessibility-related labs and documentation.
- `accessibility/plan/` — implementation plans and execution records for accessibility demonstrations.
- `accessibility/victim/` — standalone Victim Lab Android project.
- `accessibility/attacker/` — standalone Attacker Lab Android project.
- `deep-links/` — deep-link, App Link, and browser-login redirect labs and documentation.
- `deep-links/plan/` — implementation plans and execution records for deep-link demonstrations.
- `deep-links/victim/` — standalone browser-login Victim Lab Android project.
- `deep-links/attacker/` — standalone custom-scheme interception Attacker Lab Android project.
- `deep-links/mock-provider/` — local dependency-free Python authorization and profile server.
- `intents/` — intent and inter-process communication labs and documentation.
- `intents/plan/` — implementation plans and execution records for intent demonstrations.
- `intents/victim/` — standalone implicit/explicit sender Victim Lab Android project.
- `intents/attacker/` — standalone matching-action Attacker Lab Android project.
- Future vulnerability classes should live in their own descriptive top-level directories with a topic `README.md` and a `plan/` directory when implementation planning is needed.
- `AGENTS.md` is the canonical agent guide; provider-specific guide files are relative symlinks to it.

The accessibility demo is implemented as two independent Android projects, `accessibility/victim/` and `accessibility/attacker/`, using Kotlin and traditional Android Views. Its setup and API 37 verification results are in `accessibility/README.md`. The browser-login redirect demo is implemented as two independent Android projects plus a local mock provider under `deep-links/`, comparing a vulnerable custom-scheme token redirect with an authorization-code flow protected by PKCE. Its setup and API 36 verification results are in `deep-links/README.md`. The implicit activity intent demo is implemented as two independent Android projects under `intents/`, comparing implicit routing with explicit component targeting. Its setup and API 37 verification results are in `intents/README.md`. Read the relevant plan and topic README before changing a lab.

## Topic roadmap

The root `README.md` tracks candidate topics and their status. Accessibility, deep links/app links, and the first implicit intent interception lab have implemented demonstrations. Other candidates include additional intents and inter-process communication scenarios, content providers and file sharing, WebView and JavaScript bridges, data storage and backup, network security, remote configuration and client-side trust, UI redressing and overlays, and permissions and confused-deputy scenarios. The deep-links topic specifically covers Chrome Custom Tabs and external-browser login redirects; its first lab isolates custom-scheme interception and PKCE while documenting `state`, callback validation, OpenID Connect `nonce`, and verified App Links as related concerns.

Treat candidates as possible directions rather than approved implementations. Before starting a new topic, create or update its topic overview and plan, define a minimal threat model and verification matrix, and record the resulting status in the root roadmap. Do not create all candidate topic directories at once. Prefer a narrowly scoped lab with vulnerable and hardened variants over a broad collection of loosely related examples.

## Working conventions

- Keep each demonstration self-contained, visibly labeled as a lab, and limited to synthetic apps and harmless data.
- Do not target third-party apps, real accounts, credentials, payments, messages, or personal data.
- Preserve explicit user consent and Android's system-mediated permission flow. Do not add permission bypasses, stealth, persistence, obfuscation, anti-analysis behavior, or data exfiltration.
- Distinguish abuse of a legitimately authorized capability from an Android platform vulnerability; tie security claims to observed behavior and the tested Android version.
- Make secure development the learning outcome. Each topic README should explain the vulnerable app decision, provide an actionable secure-development checklist, describe what each defense does not solve, and link to authoritative guidance.
- Prefer the smallest demonstration that proves the concept. Reuse the project's existing Android/Gradle template versions and conventions rather than introducing unrelated libraries or architecture.
- Add tests only where the containing project already has tests; otherwise use its existing build, lint, and documented manual verification.
- Keep plans in the relevant topic's `plan/` directory and keep application code out of plan directories.

## Build and verification

There is no root build or test command. Use each Android project's own Gradle wrapper from its project directory. For the accessibility apps, the standard checks are:

```bash
cd accessibility/victim && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
cd accessibility/attacker && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

For the browser-login redirect apps, use the equivalent project-local checks:

```bash
cd deep-links/victim && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
cd deep-links/attacker && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

For the implicit intent interception apps, use the equivalent project-local checks:

```bash
cd intents/victim && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
cd intents/attacker && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Device-dependent security behavior must be checked on a dedicated emulator or non-personal test device. Follow the lab's verification matrix, record actual outcomes and API levels, and disable or uninstall test services and apps during cleanup. Do not claim device scenarios are verified when only static builds have run.
