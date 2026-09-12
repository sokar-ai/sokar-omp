# 005 — Declare what waiting looks like for this agent

**Priority:** 2
**Opened:** 2026-09-12
**Source:** handed over from Sokar requirement **A11**, 2026-09-12, which I wrote and measured.
The requirement covered all three agents; this is this repository's share.
**Depends on:** Sokar requirement **B47** (the daemon side: the manifest field, the matcher, the
contract). Nothing here can be read by anything until that exists. It also depends on the same
issue in `sokar-claude-code` and `sokar-pi`, because the declaration's shape should be one shape.

## What this agent has to declare

The state is in the **window title**, which is the cheapest thing to read and the hardest to
confuse with the agent quoting itself: `π ⠦ <task>` working, `π > <task>` idle, **`π ! <task>`
waiting**. Its question box also carries `Enter select · n note · ↑/↓ move · Esc cancel`.

## What is already known, measured 2026-09-12

- **Headless: nothing to declare**, and its question tool refuses to run without a terminal at
  all - so an unattended run that wanted to ask ends with a visible error rather than a silent
  success.
- **Attached: the title marker is stable** across repeated samples and returns to `>` the moment
  the question is answered.
- **The `!` under-reports, deliberately.** It is armed for the `ask` tool and for a tool whose
  approval policy resolves to a prompt - not for the model picker, `/login`, plan approval or an
  extension's dialog. Under-reporting is the right direction, and the declaration must say so.
- This agent also carries a switched-off structured event channel that names a question in its own
  words. Enabling it costs one environment variable in `omp.yaml`, and the events survive tmux.
  Whether that is a better route than the title is B47's decision.

## What would close it

- A declaration in `src/main/resources/agent/omp.yaml`, written from a measurement rather than from
  upstream's documentation.
- A test in this repository that drives the agent to a waiting point and asserts the declaration
  still matches - so a version bump that changes the wording fails this build instead of going
  quiet in the field.
- Or, where there is nothing to declare, a test asserting the contract reports *cannot say* rather
  than *not waiting*.

## Why it waits on B47

The manifest field does not exist yet, and the reader for it does not exist yet. A declaration
written now would be text nothing parses. What can be done before B47 lands is the measurement,
and that is done.
