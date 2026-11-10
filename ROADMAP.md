---
title: "exeris-caps-i18n — Roadmap to implemented"
type: roadmap
visibility: public
owning-repo: exeris-caps-i18n
status: active
last-verified: 2026-11-10
---

# exeris-caps-i18n — Roadmap to `implemented`

This repository is one of the four commodity `community` capabilities in the Exeris ecosystem
(alongside `exeris-caps-cors-policy`, `exeris-caps-token-issuer` and `exeris-caps-observability-bridge`), licensed under
**Apache-2.0** per [ADR-023](https://github.com/exeris-systems/exeris-docs/blob/main/adr/ADR-023-capability-licensing-taxonomy.md)
and [HLA §3.2](https://github.com/exeris-systems/exeris-docs/blob/main/high-level-architecture.md).

It carries two obligations:
1. **Be a complete cap.** Reach `implemented` in [`cap-license-registry.md`](https://github.com/exeris-systems/exeris-docs/blob/main/cap-license-registry.md)
   — "feature-complete against its `@Provides` contract, with tests".
2. **Be the reference i18n engine for SKUs.** Serve as the standard localization, locale resolution,
   and translated message engine for the PIM and Headless CMS API SKUs without pulling heavyweight external dependencies.

Milestones are versions, not dates. Dates appear only on what has shipped.

> **`1.0.0` is not on this roadmap.** It is a gate, not a milestone — see the last section. Freezing
> a service contract that no consumer has ever called is how you freeze the wrong shape.

---

## Where this sits

**Primary consumer SKUs:** PIM SKU and Headless CMS API SKU (Whitepaper §4, HLA §3.3). Both SKUs
require multi-lingual attributes (`LocalizedValue`), request locale resolution (`LocaleResolver`),
and localized user-facing messages (`TranslationRegistry`).

**Standing risk:** No running consumer SKU exists yet to exercise the composition end-to-end.
Freezing the SPI too early would make subsequent integration expensive.

**Upstream constraints this repo does not own** (from the [Capability Author Guide](https://github.com/exeris-systems/exeris-docs/blob/main/cap-author-guide.md) (CAG) §8):

| Constraint | Effect here | Owner |
|---|---|---|
| `compositionVersion` never wired — every build stamps `0.0.0` | Cosmetic; the content binding is the half that carries weight | `exeris-tooling` U3 |
| Manifest emitted to a *source* root, not the runtime classpath | Delivering it to a running SKU is a SKU scaffold concern | SKU scaffold, Phase 5 |
| No cross-service resolution (CAG §8) | A hard cross-cap `@Requires` fails if resolved in isolation. Mitigated via `optional = true` or decoupled functional adapters | `exeris-tooling` T12/T17 |
| No `composition.json` reader | Configuration cannot arrive from a SKU manifest yet | SKU scaffold, Phase 5 |

---

## 0.1.0 — import & repository hygiene

> **Current status:** `main` carries the organisation shell only. The capability code arrives in one
> import pull request, built on kernel 0.13.1, `exeris-sdk` 0.13.3 and `exeris-tooling` 0.11.0.
> Closing 0.1.0 requires that import and a green CI.

### Delivered by the import
- [ ] `@CapabilityModule` + `@CapabilityLifecycle` + `@Provides(service = LocaleResolver.class, version = "0.1.0")` + `@Provides(service = TranslationRegistry.class, version = "0.1.0")`.
- [ ] Package root `eu.exeris.caps.i18n` with strict `api` / `internal` split.
- [ ] Clean **JDK 25 LTS** baseline: **strictly preview-clean** (no `--enable-preview` per CAG §5).
- [ ] Initial `LocaleResolver` adhering to RFC 9110 §12.5.4 (`Accept-Language` parsing with `q`-weighting and query param precedence).
- [ ] Thread-safe in-memory `DefaultTranslationRegistry` and immutable `LocalizedValue` record carrier.
- [ ] Two-pass codegen build wired (`generate` + `verify-capabilities` in `exeris-codegen-maven-plugin`).
- [ ] `cap-manifest.json` committed with `validated: true` and a SHA-256 `contentBinding`.
- [ ] Unit tests with JUnit 5 + AssertJ; zero external logging or runtime dependencies.

### Tasks to close 0.1.0 (Repository Hygiene & Guardrails)
- [x] **`AGENTS.md` and `CLAUDE.md`** — Documenting repository invariants:
  - Two-pass build rule (CAG §5): never run `mvn clean compile` in one shot on a clean tree; first seed metadata with `mvn compile -Dexeris.codegen.skip=true`, then `mvn verify` (without `clean`).
  - Strict enforcement of Cap-Tier Wall (no `org.springframework.*`, no `io.netty.*`, no sibling `internal` access).
  - Explicit rule forbidding `--enable-preview`: ScopedValue is final in JDK 25 LTS.
- [x] **`README.md`** — High-level architecture, service contract, API usage examples (`LocaleResolver`, `TranslationRegistry`, `LocalizedValue`), and licensing statement.
- [x] **`SECURITY.md` & `CONTRIBUTING.md`** — Vulnerability disclosure policy for public Apache-2.0 repo, header parsing DoS protection guidelines, and PR conventions.
- [x] **ADR link stubs** — `docs/adr/ADR-024.link.md` (composition model), `docs/adr/ADR-053.link.md` (manifest format) and `docs/adr/ADR-055.link.md` (Wall guard).
- [ ] **`.github/workflows/build.yml`** — Multi-JDK CI (JDK 25 & 26), credential-free build per CAG §5:
  - Clean local repo: `-Dmaven.repo.local=.m2/repository`.
  - Resolve kernel 0.13.1, `exeris-sdk` 0.13.3 and `exeris-tooling` 0.11.0 as released artefacts; no upstream is built from source.
  - Verification pass running `mvn verify` enforcing Cap-Tier Wall and manifest checks.

**Exit gate for 0.1.0:** Hygiene files committed, CI green on JDK 25/26 without credentials, two-pass build cleanly passing on clean `-Dmaven.repo.local`.

---

## 0.2.0 — the contract & context

> **Goal:** Settle the public contract and runtime propagation before real consumers bind to it.
> Keep the `version = "0.1.0"` attribute in `@Provides` while the capability is unreleased — keeping it
> stable protects the deterministic SHA-256 `contentBinding` from spurious composition drift.

- [ ] **`I18nContext` using JDK 25 `ScopedValue<Locale>`:**
  - Provide safe, allocation-free, immutable context propagation across Virtual Threads.
  - Zero-preview requirement: ScopedValue is final in JDK 25 LTS; strictly no `--enable-preview`.
  - API methods: `I18nContext.currentLocale()`, `I18nContext.runWith(Locale, Runnable)`, `I18nContext.callWith(Locale, CallableOp)`.
  - Safe fallback to `LocaleResolver.defaultLocale()` when unbound.
- [ ] **`LocaleAdmissionFilter` (Service Boundary Integration):**
  - Implement `AdmissionFilter` running in `AdmissionPhase.PRE_AUTH`.
  - Resolve client `Locale` from incoming `HttpExchange` via `LocaleResolver`.
  - Synchronize resolved locale with `RequestContext.attribute("exeris.locale", locale)`.
  - **CAG §8 compliance:** Handle the absence of cross-service resolution in `exeris-tooling` by declaring
    `@Requires(service = ApiSurfaceRegistry.class, optional = true)` or providing a pure functional adapter,
    ensuring standalone and batch builds compile cleanly without mandatory Service Boundary runtime.
- [ ] **Public construction path (`LocaleResolvers`):**
  - Introduce `LocaleResolvers.builder()` in `api` to configure custom header evaluation, fallback locales,
    and optional cookie extraction (e.g. `locale` / `lang` cookies).
  - Keep internal state implementations (`DefaultLocaleResolver`) strictly encapsulated.
- [ ] **Message interpolation in `TranslationRegistry`:**
  - Add parameterized message resolution:
    - Named token substitution: `resolve(Locale, key, Map<String, Object> args)` supporting `{name}`, `{tenant}`.
    - Indexed substitution: `resolve(Locale, key, Object... args)` delegating to JDK standard `java.text.MessageFormat` for numbers/dates.
  - Ensure zero external dependencies: no ICU4J or heavy template engines.
- [ ] **Manifest regeneration:**
  - Run two-pass build (`mvn compile -Dexeris.codegen.skip=true && mvn verify`) to update `cap-manifest.json`.

**Exit gate for 0.2.0:** All unit and context tests passing; `ScopedValue` propagation verified across virtual thread forks; manifest clean and valid.

---

## 0.3.0 — conformance & resource loaders

> **Goal:** High-assurance specification conformance and structured resource loading without external dependencies.

- [ ] **RFC 9110 & BCP 47 Conformance Corpus:**
  - Parameterized test suite covering RFC 9110 §12.5.4 edge cases:
    - Complex `q`-value sorting and normalization (`q=0.000` to `q=1.000`).
    - Wildcard handling (`*`, `*;q=0.1`).
    - Malformed and obfuscated header strings (resilience against regex catastrophic backtracking / ReDoS).
    - BCP 47 language tag parsing (case-insensitivity, scripts, regions, variants).
- [ ] **`BundleLoader` SPI:**
  - Extensible SPI for bulk-loading translations into `TranslationRegistry`:
    - `PropertiesBundleLoader`: Standard UTF-8 `.properties` reader.
    - `JsonBundleLoader`: Zero-dependency, lightweight JSON translation bundle reader for CMS/PIM data.
- [ ] **Quality gates in Maven build:**
  - **JaCoCo gate:** Enforce 85% line and instruction coverage across `api` and `internal`.
  - **Mutation testing (PIT):** Validate that subtle fallback or precedence mutations fail the build.

**Exit gate for 0.3.0:** Conformance suite passes 100%; JaCoCo threshold met; PIT mutation score clean; zero third-party runtime dependencies.

---

## 0.4.0 — from example to template, and something to resolve

> **Goal:** Turn the capability into a distributable, resolvable building block ready for SKU composition.

- [ ] **GitHub Packages release workflow:**
  - Automated release workflow triggered on release tags (`v0.4.0`).
  - Release credentials isolated strictly to the release job (`build.yml` stays credential-free per CAG §5).
- [ ] **Packaging compliance:**
  - Produce `-sources.jar` and `-javadoc.jar` alongside the primary jar.
  - Include license and notice in `META-INF/`.
- [ ] **`cap-license-registry.md` status flip:**
  - Update `cap-license-registry.md` in `exeris-docs`: flip `exeris-caps-i18n` from `scaffolded` to `implemented`.
  - Ensure HLA §3.2 and `cap-license-registry.md` are in 100% synchronization.

**Exit gate for 0.4.0:** Resolvable `0.4.0` artifact in GitHub Packages; registry marked `implemented`.

---

## 1.0.0 — a gate, not a milestone

**Entry condition: a real consumer SKU has integrated.**

Freezing the public SPI before a consumer has exercised it in production composition guarantees freezing the wrong shape.
`1.0.0` will be declared when:

1. **Consumer Validation:** The PIM SKU or Headless CMS API SKU includes `exeris-caps-i18n` in its composition DAG,
   successfully boots via kernel conductor, and passes end-to-end multi-lingual integration tests.
2. **SemVer Enforcement:** Introduce `revapi` or `japicmp` Maven plugins to verify binary and source backward compatibility
   against the published baseline.
3. **Freeze:** The `@Provides` contract versions are raised to `1.0.0` and formally frozen.

---

## Deliberately out of scope

- **ICU4J / Heavy CLDR dependencies:** Exeris community caps must stay lightweight, fast to compile, and zero-dependency.
  Advanced pluralization rules (beyond standard basic tokens) belong in dedicated optional enterprise extension modules.
- **Direct HTTP response manipulation:** `LocaleResolver` inspects requests; applying language response headers (`Content-Language`)
  remains the responsibility of the Service Boundary response pipeline, keeping transport semantics decoupled from capability logic.
- **Host runtime bindings:** No imports or bindings to Spring, Netty, or Jakarta Servlet (enforced by the Cap-Tier Wall).
