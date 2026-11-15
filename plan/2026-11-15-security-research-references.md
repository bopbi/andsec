# Android Security Research References

- **Status:** done
- **Created:** 2026-11-15
- **Source:** user request

## Goal
Create a central, curated reference catalog for Android security research so contributors can find durable technical guidance and current security reporting, understand each source's relevance and limitations, and keep entries reviewable over time.

## Context
The root roadmap covers implemented and candidate Android security topics (`README.md:47`), and its learning principles require claims to distinguish platform behavior, app design, and authorized capabilities (`README.md:94`). References currently live within individual topic guides: accessibility has three official Android references (`accessibility/README.md:103`), deep links lists Android, Chrome, OAuth, and PKCE guidance (`deep-links/README.md:136`), and intents links to two Android platform documents (`intents/README.md:124`). There is no repository-wide catalog yet.

The supplied Malwarebytes article is accessible dated reporting about accessibility restrictions associated with Android 17.2 and Advanced Protection Mode. It is a useful current-events lead, but secondary reporting must not by itself establish platform behavior or replace lab verification. The existing contribution guidance likewise requires security claims to be tied to observed behavior and API levels (`CONTRIBUTING.md:12`). An unrelated untracked `content-providers/` directory is present and must remain untouched.

## Non-goals
- Do not change application code, lab behavior, verification results, or topic threat models.
- Do not build an exhaustive link dump, scrape or automate collection from websites, or add feed-processing dependencies.
- Do not replace the focused references in topic READMEs or treat news articles as proof of Android behavior.
- Do not create a recurring task or promise continuous monitoring as part of this documentation change.

## Approach
Add one root-level `REFERENCES.md` and link it from `README.md`. Organize the catalog by source role—official platform and policy guidance, standards, ongoing security research/news sources, and dated articles—and tag entries with relevant AndSec topics. Seed it with the existing topic-guide references and the supplied Malwarebytes article, using consistent metadata and concise relevance notes.

This central index makes recurring research sources and new articles discoverable without scattering general research guidance across labs. Keeping authoritative documentation distinct from secondary reporting preserves the repository's evidence standards. A wiki, database, or automated aggregator was rejected because the current need is a small, reviewable Markdown catalog with no new tooling.

## Implementation quality
- **Responsibility and integration:** This is documentation-only work. `REFERENCES.md` owns cross-topic research discovery and maintenance guidance; topic READMEs remain responsible for citations directly supporting their labs. Follow the repository's existing plain-Markdown headings, bullet lists, and relative links.
- **Reuse and boundaries:** Reuse the verified URLs and topic associations already present in the three topic guides rather than inventing replacement citations. Do not add scripts, dependencies, schemas, or a new documentation framework.
- **Testability and behavior:** No production behavior or automated test seam is involved. Observable results are a navigable catalog, accurate source metadata, clear primary/secondary source labels, and a working link from the root README.
- **Not applicable:** Code ownership, runtime collaborators, extraction boundaries, and behavior-focused automated tests do not apply because only Markdown documentation changes.

## Steps
1. Create `REFERENCES.md` with a short purpose statement and contribution format. Each entry must include a linked title, publisher or standards body, source type (`official`, `standard`, `research`, or `news`), relevant topic tags, a one-sentence relevance note, and a `last checked` date. Dated articles must also show their publication date when available; undated living documents must not be assigned an inferred publication date.
2. Populate separate sections for official Android/Chrome/Play guidance, protocol standards, ongoing research and news sources, and dated articles. Carry over the existing citations from `accessibility/README.md:103`, `deep-links/README.md:136`, and `intents/README.md:124`, retaining their original destinations and associating them with the appropriate roadmap topics. Include source-homepage links only when they are useful for recurring monitoring and are distinct from individual-article citations.
3. Add the supplied article, [Google cracks down on Android apps abusing accessibility](https://www.malwarebytes.com/blog/mobile/2026/03/google-cracks-down-on-android-apps-abusing-accessibility), as a March 2026 `news` entry tagged for accessibility. Describe it as secondary reporting about Android 17.2 and Advanced Protection Mode, and state that any platform or policy claim used in a lab requires confirmation against primary documentation and version-specific testing.
4. Add a short maintenance section to `REFERENCES.md`: review relevant sources when starting or updating a lab; prefer primary documentation for technical and policy claims; record publication and last-checked dates separately; remove or replace broken links; and update an entry's relevance note when newer authoritative information materially changes it. Do not imply that a recent link check revalidates all article claims.
5. Add a concise **Security research references** link to `REFERENCES.md` near the root README's learning principles (`README.md:94`). Explain that the catalog supplements, rather than replaces, the references in each topic guide.

## Assumptions
- A single root-level Markdown catalog is the default because the request spans current and future topics; if maintainers later need topic-only catalogs, move topic-specific entries into the relevant guides while retaining the root index for shared sources.
- Existing topic-guide URLs are the initial authoritative set; if one no longer resolves during execution, locate its canonical replacement from the same publisher or standards body and record that substitution rather than silently dropping the reference.
- The Malwarebytes URL's `/2026/03/` path supports a March 2026 publication month, but if the page exposes a more precise date, use that date instead.

## Needs your decision
None.

## Risks
- Links and platform guidance can become stale; visible last-checked dates and the maintenance rules mitigate this but do not guarantee ongoing freshness.
- Secondary reporting can oversimplify Android behavior. Keep source labels and primary-source/testing caveats explicit.
- Duplicating topic citations in a central index can cause drift. Preserve topic READMEs as lab-specific evidence and update both locations when a shared canonical URL changes.
- An undifferentiated list can grow without being useful. Keep entries curated, tagged, and accompanied by a specific reason they matter to AndSec.

## Verification
- Confirm `README.md` links to `REFERENCES.md`, the relative link resolves, and every catalog entry follows the required metadata format.
- Open every external URL and check that its title, publisher, topic, and available publication date match the entry. For the Malwarebytes entry, confirm it remains labeled as dated secondary reporting with the primary-source/testing caveat.
- Confirm all citations currently present in the three implemented topic guides are represented or intentionally covered by the catalog, and that none of those topic guides was edited.
- Review the final diff for clear separation of official guidance, standards, ongoing sources, and dated articles, with no unsupported platform-security claims.
- No Gradle, emulator, or application test run is required for this documentation-only change. The final diff should contain only `README.md` and `REFERENCES.md`; leave the unrelated untracked `content-providers/` directory unchanged.

## Model recommendation
- **Overall tier:** standard — the file edits are simple, but curating sources and summarizing security claims without overstating secondary reporting require research judgment.
- **Compatible model range:** `claude-sonnet-5-5`; `gpt-6.1-terra`; `gemini-3.8-flash`; `kimi-k3.1-pro` with `reasoning_effort: "high"`; `qwen3.7-plus`; `grok-4.6`.
- **Execution compatibility:** Any listed model, or a verified higher-tier model, is acceptable even when it differs from the model used to create the plan. Record the actual provider, model, and tier in the execution log.

## How to execute
Run `/run-plan` with this file when you are ready — or just say "run the plan" and `/run-plan` picks the newest draft in `plan/`.

## Execution log
- **Planned tier / compatible model range:** standard / `claude-sonnet-5-5`; `gpt-6.1-terra`; `gemini-3.8-flash`; `kimi-k3.1-pro` with `reasoning_effort: "high"`; `qwen3.7-plus`; `grok-4.6`
- **Executed:** 2026-11-15 on Kimi (Moonshot) / `kimi-k3.1-pro` with `reasoning_effort: "high"` (standard)
- **Provider switch:** none
- **Steps:** 1 ✅, 2 ✅, 3 ✅, 4 ✅, 5 ✅
- **Verification:** Opened all 12 external URLs and confirmed their titles, publishers, topics, and available publication dates. Python catalog validation passed for metadata, citation coverage, the README link, and the dated-article caveat; an initial validator-only URL-regex failure on `performAction(int)` was corrected before the successful rerun. `git diff --exit-code -- accessibility/README.md deep-links/README.md intents/README.md` passed. Final Markdown diff review confirmed clear source-role separation and no unsupported platform-security claims. No Gradle, emulator, or application tests were required.
- **Quality review:** Passed: `REFERENCES.md` owns cross-topic discovery and maintenance guidance, existing topic citations remain unchanged, source labels and secondary-reporting caveats are explicit, and no tooling or dependencies were added. Automated behavior tests are not applicable to this documentation-only change.
- **Deviations / decisions made:** Used the Malwarebytes page's precise publication date, 2026-03-17, as directed by the plan's assumption. `content-providers/` was not listed as an unrelated untracked directory during execution; it was left unchanged.
- **Follow-ups:** none
