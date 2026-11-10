---
title: "Review rules for exeris-caps-i18n"
type: reference
visibility: public
owning-repo: exeris-caps-i18n
status: active
last-verified: 2026-11-10
---

# Review rules for `exeris-caps-i18n`

The `repo-routine` extension of `exeris-systems/.github`'s `docs-guardrails-review.md`, applied
**after** its parts and under its severity tags, output format and verdict schema. It adds checks
and raises severities; it lowers nothing and skips nothing. One review, one verdict, one publisher.
A finding cites a rule of this file as `repo-review-rules.md#S<n>`.

**The criteria are not authored here.** They are stated by this repository's `AGENTS.md`,
`README.md`, `CONTRIBUTING.md`, `ROADMAP.md` and `SECURITY.md`, and by the build that enforces them. This file names the
questions a reviewer must reach for and the severity each answer carries; where a rule restates a
sentence of those documents, the document is the one to read and to change.

## What this repository is answerable for

`exeris-caps-i18n` is a single-module Exeris capability: locale resolution, a translation registry and
localized values; licence community (Apache-2.0), a proof of concept with no published artefact.
Other capabilities and the SKUs that compose them compile against its
`@Provides` services, so a contract broken here breaks a composition elsewhere while this build stays
green. The shared `code` part judges whether a change is correct. What it cannot know is which of this
capability's surfaces and invariants its own documents promise, and that is what follows.

## Step S — rules of this repository

S1. **The cap-tier Wall holds** (ADR-055, ADR-006; `AGENTS.md` §2). Only what the build's Wall check
    enforces is asserted here: the check is `CapTierWall` in `exeris-tooling`, run by
    `exeris:verify-capabilities` at `process-classes` over the compiled main classes, and it rejects
    a reference to `org.springframework.*`, `io.netty.*`, `reactor.*` or `jakarta.servlet.*`, to
    kernel internals (`eu.exeris.kernel.**.internal.**`), and to a sibling capability's `internal`
    package; the public (non-`internal`) packages of `exeris-kernel-spi`
    and `exeris-kernel-core` are the kernel this capability may name. In a diff: such
    an import in `src/main`; a dependency in `pom.xml` that brings one in; a rename of the package
    root `eu.exeris.caps.i18n` or an extra segment in it, because the guard derives the
    capability's name from the segment after `eu.exeris.caps.`; `eu.exeris.caps.i18n.internal`
    is private to this capability and `eu.exeris.caps.i18n.api` is the cross-cap surface, and
    an `internal` type in a public signature → `[HARD BLOCK]`. A Spring, servlet, Netty or reactor
    import in `src/test` is outside what the guard scans and is still forbidden by `AGENTS.md` →
    `[CONTRACT]`. A kernel
    driver dependency (`exeris-kernel-community` or an enterprise driver) outside `test` scope →
    `[HARD BLOCK]`: the driver is chosen by the SKU, never by a capability.

S2. **The committed manifest is regenerated, never edited.**
    `src/main/generated/java/cap-manifest.json` is written by `exeris:generate`, and a clean
    `git status` after `mvn verify` shows it matches the declaration. A diff that edits it by hand,
    touches the module class (`@CapabilityModule`, `@Provides`, `@Requires`) without carrying the
    regenerated manifest, carries a manifest change with no declaration change, or leaves a manifest
    that disagrees with the annotations (a `@Provides` version, a `@Requires` range or `optional`
    flag, an `initOrder`) → `[HARD BLOCK]`. A manifest whose `stamp.validated` is not `true` →
    `[HARD BLOCK]`.

S3. **Service contracts are `0.1.0` while the capability is unreleased**
    (`standards/capability-conventions.md` rule 9). While no artefact of this capability has been
    published, every `@Provides` declares `version = "0.1.0"` and every `@Requires` on an unreleased
    contract declares `[0.1.0,0.2.0)`; a declaration at `1.0.0` or higher, or a range that is not
    `[0.1.0,0.2.0)`, → `[CONTRACT]`. A pre-release breaking change does not bump the version: it
    keeps `0.1.0` and its Compatibility impact line is `breaking (ADR-099)` (accepted 2026-10-07),
    or `breaking (ADR-NNN)` of the accepted ADR that governs this capability. **An unreleased
    capability keeps no `accepted-api-changes.json`; the first released version starts it.** The
    reviewer therefore does not ask for that file, an entry in it, or a japicmp baseline on a
    pre-release pull request, and does not read the absence of a version bump as a finding. A pull
    request that publishes the first release is the one in which the register starts, and rule 24 of
    the shared routine then applies in full.

S4. **A community capability registers no entitlement.** This capability is `community` (Apache-2.0,
    ADR-023), so it ships no
    `META-INF/services/eu.exeris.kernel.spi.contract.EntitlementRequirement` and no
    `EntitlementRequirement` class; one added → `[HARD BLOCK]`. `LICENSE`, the POM `<licenses>`
    block and the README claim move together; a change to one alone → `[CONTRACT]`.

S5. **The jar has no third-party runtime dependency.** Every Exeris annotation is `@Retention(SOURCE)`:
    `exeris-sdk-annotations` stays at `provided` scope and `exeris-processor` stays in
    `annotationProcessorPaths`. A new third-party `compile`-scope dependency (an ICU or CLDR library
    included), a Spring, servlet or transport type, a logging implementation (use `System.Logger`,
    ADR-060), or code that reads an Exeris annotation at runtime (reflection over `SOURCE` retention
    never works) → `[HARD BLOCK]`.

S6. **The build baseline and the test contract hold** (`AGENTS.md` §2, `CONTRIBUTING.md`).
    `maven.compiler.release` stays 25 and no `--enable-preview` appears in any scope →
    `[HARD BLOCK]`. Tests use JUnit 5 and AssertJ only, with hand-written doubles and no mocking
    framework (ADR-058) → `[CONTRACT]`. A bug fix lands with the test that fails without it →
    `[CONTRACT]` when absent.

S7. **Verification names what ran.** CI runs `mvn verify` on JDK 25 and 26 on every head, so the
    pull request's Verification need not repeat it. A local run may be cited as
    `target/surefire-reports` at `<sha>`, and the reviewer accepts that as the evidence of what ran
    locally. A Verification that claims a pass without a command, report path or sha → `[STYLE]`.

S11. **Case folding and comparison use `Locale.ROOT`.** `toLowerCase()` or `toUpperCase()` without
    `Locale.ROOT` in `internal`, or a language tag normalized under the default locale → `[HARD BLOCK]`
    (under `tr-TR`, `"I".toLowerCase()` is a dotless i).

S16. **The roadmap's order holds.** `ROADMAP.md` diagnoses the known gaps and sequences them, and
    says `1.0.0` is a release gate, not a milestone. A change that implements a later milestone's
    item ahead of the earlier milestone's decisions, or declares `1.0.0`, with `ROADMAP.md`
    unchanged → `[DOC DEBT]`.

## Where this does not apply, and what it costs

The shared `code` and `pr` parts judge correctness, scope, the pull request's body and its commits, and the rules above are not a second copy of them. Whether a change to `internal` is correct is the `code` part's; what this file adds is the list of invariants this capability's own documents state, which a generic review cannot know to look for. Not to the pull request's body and commits, which the shared `pr` part judges, nor to comments
and Javadoc, which the shared `code-docs` part and the Javadoc gate judge.

The cost is that these rules are prose a reviewer applies, not a program. This repository hands the review no `repo-checks` output, so the Wall scan, the manifest comparison and the tests named above are evidence only through the build checks on the pull request, which the reviewer cannot read. A rule the reviewer could only check by running the build is reported as unchecked, not as passing.
