# 011 — Replace the update pipeline's Python tools with Sokar's shared tool

**Priority:** 1
**Opened:** 2026-09-13
**Source:** Sokar requirement B53, "The Build In One Language" - agent half handed to this repository on 2026-09-13; the operator made B53 first priority that day
**Depends on:** Sokar B53, which publishes the shared tool

## What

`update.yml` runs four Python files: `upstream-version.py`, `update.py`, `check-pin.py` (for the
digest, the one question issue 009 cannot move) and `compare-bills.py`. The first two have already
drifted between the three agent repositories; the bill comparison has not. They are replaced by the
tool Sokar publishes, with this agent's release shape as configuration.

Whatever issue 003 settles about the update rules applies to the replacement unchanged.

## The minimum regression matrix, carried over from issue 002

Run by hand against `digest_in(body, version)` on 2026-09-12. The replacement's digest parser must
reproduce every row:

| input | expected |
|---|---|
| one valid digest for the asset | accepted |
| the asset absent from the body | refused, exit 1 |
| two different digests for the asset | refused, exit 1 |
| `abc123` | refused - not a digest |
| 64 uppercase hex characters | refused - not lowercase |
| the same digest twice | accepted |

## What would close it

- No `python3` left in `update.yml`, and all four files deleted.
- Each replaced check **proven against the failure it exists for**, reproduced first - the matrix
  above, an older upstream than the pinned one stopping red instead of rolling back, and a bill that
  changed in a component the update did not name failing the comparison.
