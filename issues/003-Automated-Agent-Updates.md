# 003 — Automated agent updates: what is still undecided

**Priority:** 2
**Opened:** 2026-09-12
**Source:** handed over from Sokar requirement **A02**, 2026-09-12. The requirement covered all
three agent repositories; this is this repository's share of it.
**Depends on:** the same issue in `sokar-claude-code` and `sokar-pi` - any rule agreed here should be the same rule there,
or the divergence should be deliberate.

## What is already built here

Detect, apply, verify, publish all exist: `buildtools/upstream-version.py` reads the newest GitHub
release, `buildtools/update.py` moves the pin - and since 2026-09-12 refuses a malformed or
duplicated upstream digest before writing it - `buildtools/check-pin.py` refuses a disagreement,
and `buildtools/compare-bills.py` stops a release that changes what third-party code ships.

So this issue is not "build the pipeline". It is the set of questions the pipeline still answers by
convention rather than by a stated rule.

## What is still open

- **Can the verifying tier's credential live in CI?** Without it the automation checks less than a
  person does by hand, which is a worse gate wearing the appearance of a better one.
- **What version does the agent package take when only the tool it installs moved?** The two were
  separated deliberately. A bot needs a stated rule rather than a guess.
- **Is the pointer this repository follows the right one?** It follows the newest GitHub release and has no channel at all. Nothing states how old a release must be before it is picked up.
- **How do the CI snapshots get refreshed when GraalVM or the base image moves?** The machines pin
  GraalVM by digest and pre-pull base images, so following an upstream release there means
  rebuilding an image rather than editing a version.
- **The cadence here is unlike the other two.** 617 npm versions against Pi's 0.85.0, three
  releases on three consecutive days. Whatever ageing rule the others adopt, this one needs its own
  answer or the job opens a pull request most days.
- **The fetched CLI is not in the package's bill of materials.** The bill names this adapter's
  dependencies; the 200 MB binary the package downloads is pinned by digest in the agent definition
  instead. Whether a bill should cover something fetched at build time, and what it could honestly
  say about it, is unsettled.

## What would close it

Each question answered in `doc/decisions.md` with its reasoning, and where the answer is a rule the
pipeline must keep, a check that fails when it is broken. An answer that lives only in a person's
head is what this issue exists to remove.
