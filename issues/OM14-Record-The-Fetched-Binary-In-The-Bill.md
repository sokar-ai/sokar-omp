# OM14 — Record the fetched CLI in the bill of materials

**Priority:** 2
**Opened:** 2026-09-27
**Source:** measured while carrying the third-party comparison into the pull request, 2026-09-27

## What

The published bill of materials names **5 components** — this module's own Java dependencies. The
thing this package exists to install, the pinned Oh My Pi binary, is not one of them. Measured on
2026-09-27 against
`https://fuinorg.jfrog.io/artifactory/sokar-dist-deb/pool/main/s/sokar-agent-omp/sokar-agent-omp.cdx.json`:
5 components, against `sokar-pi`'s 140, because that repository builds an npm tree from a lockfile
and this one fetches one file.

So `buildtools/compare-bills.py` compares five Java libraries that an upstream release cannot
change, and the licence gate in `update.yml` is one the update can hardly ever trip. Its own step
says so: *"A no-op today: this bill does not record the fetched binary, so nothing moves."* That was
written as a statement of the present, and the present has not moved since.

**What it costs today:** an Oh My Pi release that changes its own licence, or that bundles something
new, reaches a package with nothing asking. The gate that would catch it is the one place a person
was meant to look, and for this repository it looks at the wrong five things.

**Why the shape is already decided:** `update.yml` passes `--expect-moved omp`, which exists so that
the pinned CLI moving version does not stop every update. **That argument is only needed once the
binary is in the bill**, so the design anticipated this and the recording was never built.

## What would close it

- The bill names the pinned CLI as a component: its version, the URL it came from, the SHA-256 the
  definition pins, and the licence upstream declares.
- `compare-bills.py` reports it as `moved` on a version bump rather than as one added and one
  removed, which `--expect-moved omp` already does once the component exists.
- A release whose licence changed stops the update, proven by running the comparison against a bill
  with the licence altered by hand.
- `sokar-claude-code` has the same gap as `CC17`; whatever is written here is written there, because
  both fetch a single binary rather than building a tree.

## Open question

Where the licence comes from. The definition pins a URL and a digest, not a licence, and reading it
from the release page would be a network call in a build that has one already for the digest.
Whether upstream's `package.json` in the tarball is the source, or the repository's declared licence,
or a value in the agent definition, is not decided here.
