# 009 — Check the pin in a unit test instead of a Python script

**Priority:** 1
**Opened:** 2026-09-13
**Source:** Sokar requirement B53, "The Build In One Language" - agent half handed to this repository on 2026-09-13; the operator made B53 first priority that day

## What

`buildtools/check-pin.py` asks four questions of the **filtered** definition in `target/classes`.
The first three need nothing but the build's own output, so they become a unit test in the agent
module, reading the same filtered file:

1. the version is a version, not an unsubstituted `${...}` - filtering happened at all
2. it is the version `pom.xml` pins
3. the download URL names that same version

The fourth - *the digest is the one upstream lists in the release's `SHA256SUMS.txt`* - needs the
network, so it cannot be a unit test. It belongs to the update pipeline and moves with issue 011.

In passing: the script's header is a copy of the Claude Code adapter's and names `claude.yaml` and
Anthropic, while its code correctly reads `omp.yaml` and Oh My Pi's release. It goes with the script.

## What would close it

- The three questions as a unit test, and each one **proven against the failure it exists for**: an
  unfiltered `${...}`, a pom version different from the definition's, a URL naming another version -
  the test fails on each before it is trusted.
- `build.yml` no longer runs `check-pin.py`. The script itself stays until 011 has taken over the
  digest question, because `update.yml` still asks it.
