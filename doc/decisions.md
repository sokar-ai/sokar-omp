# Decisions

Settled reasoning that outlives the change that produced it. A decision is written here when
somebody would otherwise ask "why is it like this?" and find only code.

Accepted risks live here too. An accepted risk is not a forgotten one: it says what the exposure
is, why it is not being removed, and what would change the answer.

Newest first, and in the order they stand below. The date is when the decision was taken,
not when its row was written - the older ones were found with `git log -S` on the sentence
rather than guessed.

| Date | What was decided |
|---|---|
| 2026-09-13 | [The changelog check is removed, not replaced](#the-changelog-check-is-removed-not-replaced) - requiring an entry returns with Sokar B55, on logchange |
| 2026-09-12 | [Accepted risk: the release binary and its digest share one trust root](#accepted-risk-the-release-binary-and-its-digest-share-one-trust-root) - nothing independent to verify the download against, and why that stays |
| 2026-09-12 | [What the acceptance actually proved, and with what](#what-the-acceptance-actually-proved-and-with-what) - Fedora 44, the installed package, a real credential |
| 2026-09-07 | [A fork is not a promise: how this agent is pointed at the broker](#a-fork-is-not-a-promise-how-this-agent-is-pointed-at-the-broker) - measured 2026-09-07: the extension loads and does not redirect; a file does |
| 2026-09-07 | [Its release cadence is unlike the other two](#its-release-cadence-is-unlike-the-other-two) - 617 versions to Pi's one, so no rule follows from a shared origin |
| 2026-09-07 | [Why the package is small and the download is not](#why-the-package-is-small-and-the-download-is-not) - 6 MB carrying a 200 MB fetch, checked against upstream's digest |

## The changelog check is removed, not replaced

**Decided 2026-09-13 by the operator**, across all Sokar repositories.

`buildtools/check-changelog.py` failed a push whose code change did not touch `CHANGELOG.md`. It is
deleted, and nothing replaces it for now. Sokar is moving to logchange - one YAML file per change,
and a generated `CHANGELOG.md` - and a check for a hand-kept file would have to be rebuilt the moment
that reaches this repository. Requiring an entry returns as Sokar B55, proposed to logchange upstream
first, which keeps the three lessons the script carried: a waiver answers for its own commit only,
documentation is not exempt, and a range that cannot be compared fails.

**Until then** the changelog is still written by hand in the same commit; only the enforcement is gone.

**What would change it:** B55 landing, or logchange being adopted here.

## Accepted risk: the release binary and its digest share one trust root

**Decided:** 2026-09-12, from the security review in `.codex-review.md` (O-03).

This package installs the Oh My Pi binary from that project's GitHub releases, pinned to a version
and checked against a SHA-256 recorded in `agent/omp.yaml`. The digest that pin is compared with
comes from `SHA256SUMS.txt` **in the same release**.

That protects against a corrupted download, a mismatched pin and accidental drift. It does **not**
prove authenticity: anybody who can change both objects - a compromised release account or a
tampered release asset - passes every check this repository makes. The binary then runs in every
task image built with this adapter.

**Why it is accepted rather than fixed:** upstream publishes no signature or provenance
attestation that we could verify against, and both objects come from one authority. A check we
cannot perform cannot be written.

**What would change it:** upstream signing its releases, publishing build provenance, or GitHub
artifact attestations for the release assets. Any of those should be verified in the update job and
recorded in the bill of materials. This is worth re-checking whenever upstream changes its release
workflow, which it does often.

**What reduces it meanwhile:** the version is pinned rather than floating; the digest is checked
before installation in the image layer; the update job opens a pull request rather than publishing
by itself; and since 2026-09-12 the update script refuses a malformed or duplicated digest before
writing it anywhere.

## What the acceptance actually proved, and with what

**Recorded 2026-09-12** from Sokar requirement A05 before it was retired. It is the only record of
what was measured rather than assumed, and the requirement was about to take it with it.

Run whole on **Fedora 44** against the installed `.rpm` with a **real OpenRouter key**:

- both packages install from the package repository rather than from a build tree;
- `sokar` discovers an agent it was never linked against;
- **the agent authenticated against the provider and completed a prompt**;
- the container held no credential but the task-scoped token;
- and the key appeared in no log the run produced.

Also measured: the digest matching upstream's published sum; `omp --version` on a stock
`ubuntu:24.04` with nothing added; the broker path end to end with a deliberately fake key, ending
in the provider's own 401 rather than the proxy's; and coexistence with Pi, whose tree is nearly
identical under a different name and is exactly where a collision would have hidden.

## A fork is not a promise: how this agent is pointed at the broker

**Recorded 2026-09-12** when Sokar requirement A05 was retired into this repository. It is the
finding that requirement got wrong, which is why it is worth keeping.

The requirement reasoned that Oh My Pi inherits Pi's shape, so Pi's extension mechanism would be the
first thing to try. **`registerProvider` does survive the fork** - same name, compatible signature -
**and it does not redirect a built-in provider** in 18.1.13. Measured: the extension loads, runs,
writes its marker, and requests still go to `openrouter.ai`.

What works is `providers.<name>.baseUrl` in `~/.omp/agent/models.yml`, so the container is pointed
at the broker **by a file rather than by a variable or an extension**. The packaging this agent
shares with Pi turned out to matter more than the mechanism it does not.

## Its release cadence is unlike the other two

617 npm versions against Pi's 0.85.0, and three releases on three consecutive days. Nothing in the
update pipeline can be shared with Pi on the grounds of the two being the same project, and any
ageing rule the other agents adopt needs its own answer here - see `issues/003`.

## Why the package is small and the download is not

The package is about 6 MB because it fetches upstream's self-contained 200 MB binary at image-build
time and checks it against the digest upstream publishes, rather than carrying it. The trade is
recorded in the accepted risk above: one trust root for both the binary and its digest.
