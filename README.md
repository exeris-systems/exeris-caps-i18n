# exeris-caps-i18n

Locale resolution, a translation registry and localized values as an Exeris capability.

**Licence:** Apache-2.0 — one of the four `community` caps under [ADR-023](https://github.com/exeris-systems/exeris-docs/blob/main/adr/ADR-023-capability-licensing-taxonomy.md). Free for any use, production included, with no subscription.

**Status:** proof of concept. Both services are provided at contract version `0.1.0`, and no artefact is published yet. [`ROADMAP.md`](ROADMAP.md) sequences the work.

---

## What the capability is

It provides the localization primitives a service needs without a heavyweight dependency:

- **Locale resolution** — `LocaleResolver` picks the client's `Locale` from an explicit `lang` / `locale` query parameter, then the `Accept-Language` header with quality weighting (RFC 9110 §12.5.4), then a configured default.
- **Translation registry** — `TranslationRegistry` is a thread-safe, in-memory registry of localized messages keyed by locale and message key, falling back from a regional tag to its base language.
- **Localized values** — `LocalizedValue` is an immutable carrier for a multi-lingual attribute, with graceful fallback when the requested language is missing.

The primary consumers are SKUs that carry multi-lingual attributes: PIM and Headless CMS API ([HLA §3.3](https://github.com/exeris-systems/exeris-docs/blob/main/high-level-architecture.md)).

## What it deliberately does not do

- **It writes no response header.** It inspects requests; applying `Content-Language` stays with the Service Boundary response pipeline.
- **It carries no ICU4J or CLDR dependency.** The cap stays zero third-party dependency; advanced plural rules belong to a dedicated extension.
- **It binds to no host runtime.** No Spring, Netty or Jakarta Servlet type, enforced by the cap-tier Wall.

## What it provides

| Service | Contract |
|---|---|
| `eu.exeris.caps.i18n.api.LocaleResolver` | `0.1.0` |
| `eu.exeris.caps.i18n.api.TranslationRegistry` | `0.1.0` |

`LocalizedValue` is a value type in the same `api` package. The capability declares no `@Requires` and, being `community`, registers no entitlement.

## Layout

The package root is `eu.exeris.caps.i18n`, and the split is load-bearing:

| Package | Visibility |
|---|---|
| `eu.exeris.caps.i18n` | the `@CapabilityModule` declaration |
| `eu.exeris.caps.i18n.api` | **visible to other caps** — the service contracts and value types |
| `eu.exeris.caps.i18n.internal` | **private** — a sibling cap reaching in fails the build |

`CapTierWall` derives the set of cap names a build owns from the segment after `eu.exeris.caps.`, and that set is what licenses the build to read its own `internal` packages.

## Building

Requires **JDK 25+** and Maven 3.9+, built against kernel 0.13.1, `exeris-sdk` 0.13.3 and `exeris-tooling` 0.11.0. Build in **two passes on a fresh checkout**:

```bash
mvn compile -Dexeris.codegen.skip=true   # seed: the processor writes capability metadata
mvn verify                               # generate, validate the graph, scan the Wall
```

A successful build produces `src/main/generated/java/cap-manifest.json`, carrying a validated stamp and a SHA-256 content binding over the resolved cap set. The file is committed; `git status` is clean after `mvn verify`.

## Reference

[Capability Author Guide](https://github.com/exeris-systems/exeris-docs/blob/main/cap-author-guide.md) · [ADR-024](https://github.com/exeris-systems/exeris-docs/blob/main/adr/ADR-024-capability-composition-model.md) (composition model) · [ADR-055](https://github.com/exeris-systems/exeris-tooling/blob/main/docs/adr/ADR-055-cap-tier-wall-guard.md) (Wall guard)
