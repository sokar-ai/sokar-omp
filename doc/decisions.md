# Decisions

Settled reasoning that outlives the change that produced it. A decision is written here when
somebody would otherwise ask "why is it like this?" and find only code.

Accepted risks live here too. An accepted risk is not a forgotten one: it says what the exposure
is, why it is not being removed, and what would change the answer.

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
