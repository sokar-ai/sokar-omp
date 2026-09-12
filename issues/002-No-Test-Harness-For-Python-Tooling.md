# 002 — The Python build tooling has no test harness

**Priority:** 3
**Opened:** 2026-09-12
**Source:** work on `.codex-review.md` (the negative tests the review asked for)

## What

`buildtools/` holds several Python tools that decide what gets shipped - the update script, the pin
check, the changelog guard, the bill comparison. None of them has a test, and nothing in CI runs
one: there is no pytest, no unittest, no runner.

## Why it matters

The review asked for negative tests around metadata parsing, which is the right ask: these scripts
are the last thing between an upstream mistake and a published package. Writing one test today
means introducing a framework, a dependency and a CI step - a decision about how this repository is
built, not a fix.

Until then, guards in these scripts are verified by hand, which is not repeatable and leaves no
record.

## The minimum regression matrix, until there is a harness

These were run by hand against `digest_in(body, version)` on 2026-09-12 and are the cases a change
to that parser has to reproduce. Written down here rather than left in the review answer, because
a matrix nobody can find is a matrix that gets repeated from memory and shrinks each time.

| input | expected |
|---|---|
| one valid digest for the asset | accepted |
| the asset absent from the body | refused, exit 1 |
| two different digests for the asset | refused, exit 1 |
| `abc123` | refused - not a digest |
| 64 uppercase hex characters | refused - not lowercase |
| the same digest twice | accepted |

Whoever changes the parser either automates these or runs them and records the result in the
commit. "It still works" is not one of the two.

## What would close it

- Decide on a runner (pytest is the obvious one) and where its dependency is pinned.
- A CI step that runs it, failing the build like the Java tests do.
- Tests for the guards that already exist, starting with the update script's metadata rules.

Where a guard is worth testing today, the logic is kept in a function that takes its input as an
argument rather than fetching it - so a test needs no network when the harness arrives.
