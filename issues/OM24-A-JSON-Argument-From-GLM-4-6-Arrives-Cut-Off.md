# OM24 — A JSON argument from GLM-4.6 arrives cut off

**Status:** later.

**What must be true.** Whoever runs omp with `z-ai/glm-4.6` can have the agent write a file whose
content is JSON - a mailbox message among them - or is told plainly that this model cannot.

## Why

Measured headless on the VM, the agent told to `Write` a one-line JSON message into a file: with
`z-ai/glm-4.6` the `write` call's `content` arrived as `"to` or `to`, everything from the first `":` on
gone, and the model retried it 105 times in three minutes. In the mailbox the same cut leaves
`content` empty, and omp refuses "content is required" until the task ends. With `z-ai/glm-5.3-flash`
the same call carried the whole JSON, once. CI runs `z-ai/glm-5.3-flash` for that reason.

## Acceptance

- Where the argument is cut - in what the model emits, in the provider's translation, or in omp's
  reading of it - is measured from the raw stream.
- The cut is fixed where it happens, reported upstream, or `doc/` names the model as unable to send a
  message; a scenario with that model writing a JSON file is seen to fail before and pass after a fix.

## To be checked

- Whether other models of the same family cut the same argument.
