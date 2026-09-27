# OM14 — Record the fetched CLI's license in the bill of materials

**Priority:** 2
**Opened:** 2026-09-27
**Source:** measured while carrying the third-party comparison into the pull request, 2026-09-27;
narrowed the same day, when the component itself had landed

## What

**The bill names the fetched binary now** - measured on the bill published at
2026-09-27T17:52:53Z: `omp` with its version, the URL it is fetched from and the pinned SHA-256,
recorded by Sokar's release tool at package time. On a version bump `compare-bills` reports it as
`moved` rather than as one added and one removed, which `--expect-moved omp` does and which was
proven against a bill with the version moved by hand.

**What it still lacks is a license.** The component carries none, so the comparison prints `NO
LICENSE DECLARED` for it and has nothing to compare: Oh My Pi relicensing itself between two releases
reaches a package with nothing asking. The license gate in `update.yml` covers the Java libraries
and not the thing this package exists to install.

**Where a license could come from, measured 2026-09-27:**

    SHA256SUMS.txt          digests only
    GitHub license API      MIT for the repository - /repos/can1357/oh-my-pi/license, which takes
                            a ref, so it can be asked for the release's tag

## What would close it

- The component carries the license upstream declares for the pinned release.
- A release whose license changed stops the update, proven by running `compare-bills` against a
  bill with the license altered by hand - it already compares a moved component's licenses.
- `sokar-claude-code` gets the same, since both fetch a single binary.

## Open question

**How the license reaches the bill.** Recommended: the way the digest does - `update` reads it from
the per-release source above and writes it into the definition beside `sha256`, `add-fetched-cli`
records it from there, and the build itself stays free of network calls for it. That needs Sokar's
release tool to read and write it, so it is a question for Agent Sokar before it is work here.
