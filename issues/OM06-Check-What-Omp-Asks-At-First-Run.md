# OM06 — Check what Oh My Pi asks at first run inside a task

**Priority:** 1
**Opened:** 2026-09-13
**Source:** Sokar requirement B24, handed to this repository on 2026-09-13

## What

Never checked. What is declared is `--auto-approve`, which turns its approval prompts off. Whether a
fresh task shows anything before work - a first-run wizard, a login menu, a trust question, a
telemetry question - nobody has looked.

## Why it matters

A question nobody answers is a task that starts and then waits. Sokar's suite cannot notice it,
because its stub agent asks nothing.

## What would close it

- Measured at a real terminal in `/workspace`, on a machine where omp never ran: with a credential,
  without one, and headless.
- Each dialog found either answered by a file this adapter declares, or recorded as not answerable
  with the reason.
- The result in `doc/decisions.md`.

## Open question

Whether Oh My Pi has any first-run dialog at all.
