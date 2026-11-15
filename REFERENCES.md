# Security Research References

This catalog curates cross-topic sources for designing, implementing, and reviewing AndSec labs. It supplements the references in each topic guide: those guides remain the authoritative citations for their specific demonstrations and verification results.

Official documentation and standards support technical and policy claims. Research and news sources help identify developments to investigate, but secondary reporting does not by itself establish Android platform behavior or replace version-specific lab verification.

## Contribution format

Keep entries curated and group them by source role. Each entry must include:

- A linked title and its publisher or standards body.
- A source type: `official`, `standard`, `research`, or `news`.
- Relevant AndSec topic tags and a one-sentence relevance note.
- A `last checked` date in `YYYY-MM-DD` format.

Dated articles must also include their publication date when available. Do not infer publication dates for undated living documents.

## Official Android, Chrome, and Play guidance

- [Accessibility service guide](https://developer.android.com/guide/topics/ui/accessibility/service)
  - **Publisher:** Android Developers
  - **Source type:** `official`
  - **Topics:** `accessibility`
  - **Relevance:** Explains service declaration, configuration, UI inspection, and user-directed actions that underpin the accessibility lab's authorized-capability threat model.
  - **Last checked:** 2026-11-15
- [`AccessibilityNodeInfo.performAction`](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo#performAction(int))
  - **Publisher:** Android Developers
  - **Source type:** `official`
  - **Topics:** `accessibility`
  - **Relevance:** Documents the node-action API used for the accessibility lab's single synthetic button interaction.
  - **Last checked:** 2026-11-15
- [Accessibility service configuration](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo)
  - **Publisher:** Android Developers
  - **Source type:** `official`
  - **Topics:** `accessibility`
  - **Relevance:** Documents service configuration and capabilities relevant to package and event scoping, declared capabilities, and authorized UI access.
  - **Last checked:** 2026-11-15
- [Android App Links](https://developer.android.com/training/app-links)
  - **Publisher:** Android Developers
  - **Source type:** `official`
  - **Topics:** `deep-links`, `intents`
  - **Relevance:** Explains deep-link and verified App Link routing, which must be considered separately from authorization-request binding and callback validation.
  - **Last checked:** 2026-11-15
- [Chrome Custom Tabs](https://developer.chrome.com/docs/android/custom-tabs)
  - **Publisher:** Chrome for Developers
  - **Source type:** `official`
  - **Topics:** `deep-links`
  - **Relevance:** Describes the browser-powered in-app web flow used by the synthetic login lab and how it differs from launching an external browser.
  - **Last checked:** 2026-11-15
- [Implicit intent hijacking](https://developer.android.com/privacy-and-security/risks/implicit-intent-hijacking)
  - **Publisher:** Android Developers
  - **Source type:** `official`
  - **Topics:** `intents`
  - **Relevance:** Describes the interception risk and explicit-targeting mitigations demonstrated by the implicit intent lab.
  - **Last checked:** 2026-11-15
- [Intents and intent filters](https://developer.android.com/guide/components/intents-filters)
  - **Publisher:** Android Developers
  - **Source type:** `official`
  - **Topics:** `intents`, `deep-links`
  - **Relevance:** Defines explicit and implicit routing, intent-filter matching, component exposure, and related platform behavior needed to interpret the routing labs.
  - **Last checked:** 2026-11-15

## Protocol standards

- [RFC 8252: OAuth 2.0 for Native Apps](https://www.rfc-editor.org/rfc/rfc8252)
  - **Standards body:** Internet Engineering Task Force (IETF)
  - **Source type:** `standard`
  - **Topics:** `deep-links`
  - **Relevance:** Establishes best current practice for browser-based native-app authorization, redirect mechanisms, and their security considerations.
  - **Last checked:** 2026-11-15
- [RFC 7636: Proof Key for Code Exchange by OAuth Public Clients](https://www.rfc-editor.org/rfc/rfc7636)
  - **Standards body:** Internet Engineering Task Force (IETF)
  - **Source type:** `standard`
  - **Topics:** `deep-links`
  - **Relevance:** Specifies how PKCE protects an intercepted authorization code at the token endpoint without preventing the callback interception itself.
  - **Last checked:** 2026-11-15
- [RFC 9700: OAuth 2.0 Security Best Current Practice](https://www.rfc-editor.org/rfc/rfc9700)
  - **Standards body:** Internet Engineering Task Force (IETF)
  - **Source type:** `standard`
  - **Topics:** `deep-links`
  - **Relevance:** Provides current OAuth security guidance on redirect-based flows, authorization-code protection, PKCE, and related attacks and mitigations.
  - **Last checked:** 2026-11-15

## Ongoing research and news sources

- [Malwarebytes Blog](https://www.malwarebytes.com/blog)
  - **Publisher:** Malwarebytes
  - **Source type:** `news`
  - **Topics:** `cross-topic`
  - **Relevance:** Provides recurring security reporting that can identify Android developments for investigation, with individual claims still requiring primary-source confirmation.
  - **Last checked:** 2026-11-15

## Dated articles

- [Google cracks down on Android apps abusing accessibility](https://www.malwarebytes.com/blog/mobile/2026/03/google-cracks-down-on-android-apps-abusing-accessibility)
  - **Publisher:** Malwarebytes
  - **Source type:** `news`
  - **Topics:** `accessibility`
  - **Published:** 2026-03-17
  - **Relevance:** Secondary reporting about accessibility restrictions associated with Android 17.2 and Advanced Protection Mode; any platform or policy claim used in a lab requires confirmation against primary documentation and version-specific testing.
  - **Last checked:** 2026-11-15

## Maintenance

- Review relevant sources when starting or updating a lab.
- Prefer primary documentation for technical and policy claims, and use secondary reporting as a research lead rather than proof.
- Record publication and last-checked dates separately; a recent link check does not revalidate all of an article's claims.
- Remove or replace broken links, preferring canonical replacements from the same publisher or standards body.
- Update an entry's relevance note when newer authoritative information materially changes it, and keep shared canonical URLs consistent with the topic guides.
