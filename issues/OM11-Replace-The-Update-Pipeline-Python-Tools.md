# OM11 — Replace the update pipeline's Python tools with Sokar's shared tool

**Priority:** 1
**Opened:** 2026-09-13
**Source:** Sokar requirement B53, "The Build In One Language" - agent half handed to this repository on 2026-09-13; the operator made B53 first priority that day
**Depends on:** Sokar B53, which publishes the shared tool

## What

`update.yml` runs four Python files: `upstream-version.py`, `update.py`, `compare-bills.py` and
`check-pin.py`. The first two have already drifted between the three agent repositories; the bill
comparison has not. They are replaced by the tool Sokar publishes, with this agent's release shape
as configuration.

**`check-pin.py` is only half gone.** The version, the pom and the download URL agreeing is a unit
test now (`PinAgreementTest`). What remains is the one question a unit test cannot ask: whether the
pinned digest is the one upstream lists in the release's `SHA256SUMS.txt`. `build.yml` runs the
script **on every push** for that question, not only the update job - it is what catches a
hand-made version bump that forgot the digest. Its header is still a copy of the Claude Code
adapter's and names `claude.yaml` and Anthropic; its code correctly checks Oh My Pi. It goes with
the script.

Whatever OM03 settles about the update rules applies to the replacement unchanged.

## The minimum regression matrix, carried over from the retired issue about the Python
tooling having no test harness

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

- No `python3` left in `update.yml` or `build.yml`, and all four files deleted.
- **The digest check still runs on every push**, not only when the update job runs.
- Each replaced check **proven against the failure it exists for**, reproduced first - the matrix
  above, an older upstream than the pinned one stopping red instead of rolling back, and a bill that
  changed in a component the update did not name failing the comparison.
