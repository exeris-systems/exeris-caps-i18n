# Security Policy

This capability decides which locale a request is served in, from input an unauthenticated caller
controls: a query parameter and the `Accept-Language` header. A defect here is an input-handling
defect on a request path.

## Supported versions

| Version / branch | Security support |
| --- | --- |
| `main` (default branch) | ✅ Supported |
| tagged releases | none exist yet — see below |
| forks, feature branches | ❌ Not supported |

**No artefact has been published from this repository.** There is no release on GitHub Packages or
Maven Central, and no consumer integrated. A report today is about source on the default branch.
When the first artefact ships (`ROADMAP.md` 0.4.0), this table gains a supported release line.

## Scope

**In scope.** Locale resolution from a query parameter and from `Accept-Language`, language-tag
parsing, the translation registry's lookup and fallback, and the localized-value carrier.

**Out of scope by construction.** This cap reads no socket and writes no response header. A consumer
that applies the resolved locale to the wrong request, or trusts a translated string as markup, has a
bug in that consumer. Report those to the consumer's repository; if the confusion was *invited* by
this cap's API shape, that is in scope here.

## What to report

- Input from an unauthenticated caller — a long or repeated `Accept-Language` value, a crafted
  language tag, an oversized query parameter — causing unbounded work, unbounded memory growth or
  catastrophic regular-expression backtracking.
- A header-keyed cache that an attacker can fill or evict at will.
- Locale-sensitive case folding. Comparison and normalization must use `Locale.ROOT`: under `tr-TR`,
  `"I".toLowerCase()` yields a dotless i.
- A query parameter or header value reflected into a result, diagnostic or exception message in a way
  a consumer is likely to log or return verbatim.
- Cross-locale leakage: a registry lookup that returns another locale's message because of
  fallback order or key normalization.
- Anything that reintroduces a third-party runtime dependency into this jar.
- A committed `cap-manifest.json` that does not correspond to the declaration in the same commit.

## Out of scope

Unless they bypass one of the guarantees above:

- Transport-level, TLS, HTTP-parsing and routing concerns — this cap owns none of them.
- How a consumer renders, escapes or stores a translated message.
- Scanner output with no reachable code path in this repository.

## Severity guidance

**High or critical** — unbounded resource consumption reachable from an unauthenticated request.

**Medium** — cross-locale leakage, reflected input, locale-sensitive comparison.

**Low or maintenance** — diagnostics that could be clearer, or validation that could be stricter.

Severity may be adjusted for exploitability and whether the default configuration is affected.

## Reporting a vulnerability

Use **GitHub private vulnerability reporting** on this repository
(<https://github.com/exeris-systems/exeris-caps-i18n/security/advisories/new>).

**Do not open a public issue, discussion, or pull request for a suspected vulnerability.**

A useful report includes the affected commit or branch, the header and query-parameter values that
trigger it, the result you observed and the one you expected, and a failing test if you have one.
Do not include credentials, tokens, or production data; a synthetic header value is always enough.

## Disclosure

Maintainers aim to acknowledge a report within 72 hours and give an initial triage result within 7
days. A fix carries a regression test and updates the affected javadoc or ADR when it changes an
observable contract.

Please allow a reasonable opportunity to investigate and release a mitigation before public
disclosure. If your organization requires a disclosure deadline, state it in the initial report.

Good-faith research that avoids data destruction, lateral movement, and access to third-party systems
is welcome. This is not legal advice and does not authorize testing against systems you do not own.
