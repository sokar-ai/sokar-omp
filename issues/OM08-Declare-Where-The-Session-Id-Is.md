# OM08 — Declare where this agent's session id is

**Priority:** 3
**Opened:** 2026-09-13
**Source:** Sokar requirement B46, agent half handed to this repository on 2026-09-13
**Depends on:** Sokar B46 - there is no manifest field to declare it in until B46 builds one. Also
the same issue in `sokar-claude-code` and `sokar-pi`, because the declaration should be one shape.

## What

A task that comes back should continue the conversation it was having. Sokar records the session id
and resumes with the declared `resume_flag`, which for Oh My Pi is `--session`; where the id *is* is
a fact about this agent, so this package declares it, in `omp.yaml`.

## What is known

Nothing yet. Oh My Pi resumes with `--session`, and nobody has looked at where the value that flag
takes appears: in its headless JSON events, in files under its agent directory, or both.

## What would close it

- Both routes measured on the pinned Oh My Pi version: where a headless run (`--print --mode json`)
  names its session, and where an attached run keeps it.
- The declaration in `omp.yaml`, in whatever shape B46 settles.
- A test that drives a short session and asserts the declared route still finds the id.

## Open questions

- Whether headless output names the session at all.
- Whether the headless and the attached id are the same string, which decides whether one
  declaration covers both modes.
