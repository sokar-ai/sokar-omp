# OM19 — Declare a thinking level and a read-only mode

**Status:** later; blocked by sokar B101.

**What must be true.** A task start can set omp's thinking level, and can ask for a read-only mode by
name, without being able to pass any other flag through.

## Why

omp has `--thinking=<level>`, `--tools=<list>` (only the named tools are enabled) and `--max-time`,
but a task can use none of them: `sokar task start` gives an agent only what its definition declares,
and `omp.yaml`'s `headless:` declares only `--model` and the output flags. That is by design - a start
that could pass any flag through could undo `--auto-approve` and the rest the definition holds. A long
review that should only read can today be held to that only by its prompt.

## Acceptance

- A `headless:` entry for the thinking level, so a start sets it the way it sets `--model`.
- A read-only mode declared in `omp.yaml` (read and search tools plus one write), which a start asks
  for by name; in that mode a write outside it is seen to be refused.
