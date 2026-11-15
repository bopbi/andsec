# Contributing to AndSec

AndSec welcomes small, well-documented educational security labs and fixes to existing ones. By contributing, you agree that your work is licensed under the repository's [MIT License](LICENSE).

## Lab safety and scope

- Use only synthetic apps, local services, and harmless data on a dedicated emulator or non-personal test device.
- Do not target third-party apps, real accounts, credentials, payments, messages, or personal data.
- Preserve explicit user consent and Android's system-mediated permission flows.
- Do not add permission bypasses, stealth, persistence, obfuscation, anti-analysis behavior, or data exfiltration.
- Avoid undocumented third-party network access. A lab that needs networking must use a documented local provider or another explicitly approved, narrowly scoped design.
- Distinguish abuse of an authorized capability from an Android platform vulnerability, and tie security claims to observed behavior and Android API levels.

Prefer the smallest demonstration that proves the concept, with vulnerable and hardened variants where practical. Reuse the existing project templates and dependencies rather than introducing unrelated architecture or libraries.

## Making changes

1. Read the root [README](README.md), the relevant topic README, and any implementation plan before editing a lab.
2. For a new topic, first create its topic overview and plan with a minimal threat model and verification matrix. Update the root roadmap; do not scaffold every candidate topic at once.
3. Keep application code out of `plan/` directories and keep every lab self-contained and visibly labeled.
4. Add automated tests only where the containing project already has tests. Otherwise, use its existing build, lint, and documented manual verification.
5. Run the relevant project-local checks. There is no root build command:

   ```bash
   cd accessibility/victim && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
   cd accessibility/attacker && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
   cd deep-links/victim && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
   cd deep-links/attacker && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
   ```

6. Exercise device-dependent behavior using the topic's complete verification matrix. Record actual outcomes, API levels, negative results, and any checks that were not run. Never present static checks as device verification.
7. Disable or uninstall test services and apps during cleanup. Do not commit APKs, logs, build output, `local.properties`, IDE state, credentials, environment files, or signing keys.

See [SECURITY.md](SECURITY.md) for reporting unintended security issues.
