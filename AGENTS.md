---
title: "exeris-caps-i18n: agent operating contract"
type: reference
visibility: public
owning-repo: exeris-caps-i18n
status: active
last-verified: 2026-11-10
---

# exeris-caps-i18n

Operating contract for AI agents working in this repository.

## 1. Mission and scope

`exeris-caps-i18n` is a single-module Exeris capability. It provides locale resolution
(`LocaleResolver`), a translation registry (`TranslationRegistry`) and an immutable localized-value
carrier (`LocalizedValue`). Licence: Apache-2.0, one of the four `community` caps under ADR-023.

- **The capability code has not landed in this repository yet.** `main` carries the organisation shell;
  the code arrives in one import pull request. Do not assume a `pom.xml` or `src/` exists.
- **The jar has no third-party runtime dependency.** Every Exeris annotation is `@Retention(SOURCE)`:
  `exeris-sdk-annotations` stays at `provided` scope and `exeris-processor` stays in
  `annotationProcessorPaths`, never on the compile classpath.
- **Its registry status is `specified`.** Read [`ROADMAP.md`](ROADMAP.md) before proposing a design
  change: the known gaps are diagnosed and sequenced there, with the reasoning.

## 2. Operating contract and boundaries

- **The cap-tier Wall (ADR-055), build-enforced.** `eu.exeris.caps.i18n.api` is the whole cross-cap
  surface; `..i18n.internal` is private. The guard derives the capability's own name (`i18n`) from the
  segment after `eu.exeris.caps.`, so renaming the package root or adding a segment revokes the
  build's right to read its own `internal`. Never put an `internal` type in an `api` signature.
- **No host-runtime type.** No Spring, servlet, Netty or reactor type, no new third-party `compile`
  dependency, and no reading an Exeris annotation at runtime: reflection over `SOURCE` retention never
  works. Public kernel SPI and Core types may be named; kernel `internal` packages and drivers may not.
- **The committed manifest.** `src/main/generated/java/cap-manifest.json` is tracked, and
  `exeris:generate` writes it into `src/`. A diff touching the module declaration carries the
  regenerated manifest: `stamp.validated: true`, a `sha256:<64 hex>` `contentBinding`, an `initOrder`
  matching the declared graph. Regeneration is byte-stable, so a dirty `git status` after `mvn verify`
  means the declaration and the manifest disagree.
- **Contract versions.** While no artefact is published, every `@Provides` declares `0.1.0`.
  `@Provides(version = …)` is a semver contract: a behavioural change to a service bumps it once a
  release exists.
- **JDK 25, no preview.** `maven.compiler.release` stays 25 and no `--enable-preview` appears
  anywhere (kernel ADR-066, SDK ADR-069): a preview-stamped class re-pins the jar to one JDK major.
- **Tests (ADR-058).** JUnit 5 and AssertJ only, no mocking framework.
- **Licence.** Apache-2.0 is an ADR-023 classification: `LICENSE`, the POM `<licenses>` block and
  the README claim move together.
- **Language.** Code, identifiers, comments, commit messages, pull-request bodies and documentation
  are English.
- **Safety.** Do not push, tag, publish or change repository settings unless the user asks. The vendored
  bundle policy `bundle:agent-safety-and-autonomy` applies.

### Input handling

Locale resolution reads values an unauthenticated caller controls. These are easy to undo by accident.

- **Work is bounded by the input.** A long or repeated `Accept-Language` value never causes
  unbounded work, and no header-keyed cache grows without a bound.
- **Parsing is not regex-backtracking.** A crafted header must not trigger catastrophic backtracking.
- **Case folding uses `Locale.ROOT`** (under `tr-TR`, `"I".toLowerCase()` is a dotless i).
- **Resolution always yields a locale.** A missing or unparseable input falls back to the configured
  default; it never throws to signal "no match".

Report vulnerabilities through [`SECURITY.md`](SECURITY.md), never a public issue.

## 3. Architecture and documentation entry points

- [`README.md`](README.md): what the capability does and does not do, building, the manifest.
- [`ROADMAP.md`](ROADMAP.md) and [`CONTRIBUTING.md`](CONTRIBUTING.md).
- Composition model: [`docs/adr/ADR-024.link.md`](docs/adr/ADR-024.link.md). Manifest and stamp:
  [`docs/adr/ADR-053.link.md`](docs/adr/ADR-053.link.md) and `exeris-sdk-composition-spec` in
  exeris-sdk.
- The Wall: [`docs/adr/ADR-055.link.md`](docs/adr/ADR-055.link.md), then `CapTierWall` in
  exeris-tooling's `exeris-codegen-java`. The guard is what runs; read it before arguing the rule.
- [Capability Author Guide](https://github.com/exeris-systems/exeris-docs/blob/main/cap-author-guide.md);
  ADRs indexed in [`adr-index.md`](https://github.com/exeris-systems/exeris-docs/blob/main/adr-index.md).

## 4. `.agents/` discovery

- `.agents/manifest.yaml` imports the organisation bundle `exeris-agents`, pinned by version, commit
  and digest.
- `.agents/vendor/exeris-agents-2.1.0/` is the vendored bundle: policies, base schemas and the hook
  dispatcher. Never edit it in place; bumping it means re-vendoring.

This repository authors no profiles, skills, workflows or hooks of its own.

## 5. Verification and reporting

Once the code has landed, built against kernel 0.13.1, exeris-sdk 0.13.3 and exeris-tooling 0.11.0:

```bash
mvn compile -Dexeris.codegen.skip=true   # pass 1 — seed capability metadata
mvn verify                               # pass 2 — generate, validate the graph, scan the Wall
git status --porcelain                   # must be empty: the committed manifest matches the build
```

Both passes are needed after a `clean`: the processor writes metadata only during `compile`. The
build log must show `own cap(s): [i18n]`. Report what ran and its result; do not claim a check
that was not run.

## 6. Provider adapters

`CLAUDE.md` is a pointer to this file. No other provider adapter is rendered in this repository.
