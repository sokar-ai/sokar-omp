# OM18 — Prove GitHub Copilot in acceptance

**Status:** later.

**What must be true.** A task started with the GitHub Copilot provider works, and holds no GitHub
token, as CI shows on every build.

## Why

The GitHub Copilot path is documented and was measured by hand on 18.4.1, but no scenario runs it. The
credential scenarios use an OpenRouter key, and CI fails when that key is missing.

## Acceptance

- A Copilot token is a CI secret, and the acceptance kit puts an `oauth-device` grant into the vault
  without a person.
- A scenario starts a task with `--provider github-copilot` and checks that the task's files,
  environment and logs hold no GitHub token; a token planted in the task is seen to fail it.
