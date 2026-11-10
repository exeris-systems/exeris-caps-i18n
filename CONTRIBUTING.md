# Contributing to exeris-caps-i18n

Thank you for contributing. This repository is Apache-2.0 (`community` under ADR-023) and open to
outside contributions.

It parses input an unauthenticated caller controls, so a change to resolution or parsing needs an
argument about worst-case work, not just a green build. `AGENTS.md` carries the full set of
invariants and the reasoning behind them; it is written for agent sessions but it is the most
complete account, and worth reading before a non-trivial change.

## Build & test

```bash
mvn compile -Dexeris.codegen.skip=true   # pass 1 — seed the capability metadata
mvn verify                               # pass 2 — generate, validate, scan the Wall
```

Both passes are required after a `clean`; `README.md` explains why. Requires **JDK 25** and Maven
3.9+.

## What a change has to preserve

- **The `api` / `internal` split.** `..i18n.api` is the entire cross-cap surface. No `internal` type
  may appear in an `api` signature, and the package root `eu.exeris.caps.i18n` must not be renamed or
  given an extra segment — the Wall guard derives this cap's identity from it (ADR-055).
- **No third-party runtime dependency.** No new `compile`-scope third-party dependency, and no Spring,
  servlet, Netty or reactor type anywhere. The SDK annotations stay `provided`; the processor stays in
  `annotationProcessorPaths`.
- **The committed manifest.** `src/main/generated/java/cap-manifest.json` is tracked, and
  `exeris:generate` writes it into `src/`, not `target/`. If your change touches the module
  declaration, commit the regenerated manifest in the same commit. A clean `git status` after
  `mvn verify` is the check.
- **The JDK baseline.** `maven.compiler.release` stays 25 and no preview flag appears anywhere
  (kernel ADR-066, SDK ADR-069).
- **Locale-independent comparison.** Case folding uses `Locale.ROOT`.

## Tests

JUnit 5 + AssertJ only. **No mocking framework** (ADR-058). Doubles are written, never mocked. A bug
fix lands with the test that fails without it. If you are fixing a conformance issue, say which part of
RFC 9110 or BCP 47 you are conforming to.

## Conventions

- Code, identifiers, comments, commit messages, PR titles and bodies, and documentation files are
  **English**.
- Conventional-commit prefixes (`feat:`, `fix:`, `build:`, `ci:`, `docs:`).
- Comments explain *why*, not *what*, and state the invariant rather than the history.

## Architecture and ADR discipline

The Exeris ecosystem has **one ADR number space** across ~20 repositories, indexed in
`exeris-docs/adr-index.md`. If your change needs a decision record:

1. **Reserve the number in that index first**, in its own pull request, then write the content.
2. A cross-repo decision leaves a `docs/adr/*.link.md` stub in every affected repository; see the
   three already in `docs/adr/`.
3. Refactor-only rationale belongs in the pull-request description, not in an ADR.

## Pull-request checklist

- [ ] `mvn verify` green, and `git status` clean afterwards
- [ ] The `api` / `internal` split and the package root are intact
- [ ] No new third-party `compile`-scope dependency; no Spring / servlet / transport type
- [ ] Tests added or updated; JUnit 5 + AssertJ only
- [ ] Regenerated `cap-manifest.json` committed, if the declaration changed
- [ ] `README.md` / `AGENTS.md` / `ROADMAP.md` updated if a documented claim changed

## Security

Do not open a public issue for a suspected vulnerability. `SECURITY.md` describes what is in scope
and how to report privately.
