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
