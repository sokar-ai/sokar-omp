# Oh My Pi for Sokar

Runs [Oh My Pi](https://github.com/can1357/oh-my-pi) (`omp`) inside
[Sokar](https://github.com/sokar-ai/sokar): in a hardened container, reaching only what it needs,
with your credential kept on the machine and its work waiting for your review. It is not
[Pi](https://github.com/earendil-works/pi), which [sokar-pi](https://github.com/sokar-ai/sokar-pi)
adapts.

## Install

Set Sokar's package repository up once, as Sokar's
[getting started](https://github.com/sokar-ai/sokar/blob/main/doc/getting-started.md) describes,
then:

```
sudo apt install sokar-agent-omp      # or: sudo dnf install sokar-agent-omp
```

Setting a machine up from Sokar's interface offers it too. `sokar agents` lists it at once; nothing
has to be registered. The package is the adapter only: Oh My Pi itself, one 200 MB binary, is fetched
into the task's image at a pinned version and checked against its digest. It installs beside
`sokar-agent-pi` without a shared file.

## Sign in

With an **OpenRouter API key**, the default provider:

```
sokar vault put openrouter --type api-key
```

It asks for the key without showing it. The entry carries the provider's name, so another agent
reaching OpenRouter uses the same one.

With a **GitHub Copilot subscription**, granted once on this machine: an `oauth-device` entry named
`github-copilot`, then `sokar vault authorize github-copilot`, and tasks started with
`--provider github-copilot`. Sokar's
[credentials guide](https://github.com/sokar-ai/sokar/blob/main/doc/credentials.md#a-subscription-granted-once-github-copilot)
has the commands. Oh My Pi's other `/login` services are not brokered.

## Start a task

```
sokar project default add <address>
sokar task start <name> -p default -r <repository> --agent omp
```

No project file is needed. The address is where the repository is cloned from and where its approved
work goes; `sokar project default list` shows the name `-r` takes. Started inside a checkout,
`sokar task start` adds that checkout's repository by itself. A project of your own, with a
`project.yml`, is for settings beyond that. A bare `--model` goes to the task's provider.

## What the task holds and reaches

- **Not your credential.** The container gets a token that works for this task only, in
  `~/.omp/agent/models.yml`, and Sokar's broker swaps the real credential in on the way out.
- **Reachable:** the task's provider only (`openrouter.ai`, or `api.githubcopilot.com` with
  `--provider github-copilot`). Oh My Pi needs no host of its own to start. Nothing else resolves.
- **Refused on purpose:** its model catalogue, model lists of providers the task has no credential
  for, and `registry.npmjs.org`. It works without them.
- **Python eval needs `python3` in the task's image.** The image Sokar builds has it, where the base
  has a package manager; `omp setup python --check` in the task says "Python execution is ready". A
  project whose own image lacks it gets Oh My Pi's notice at start, and eval runs JavaScript only.

`sokar agents --verbose` shows all of it for the installed version.

## When it does not work

The task's broker logs every request in `vault.log`, in `/run/user/<uid>/sokar/<container>/`:

| What it says | What it means |
|---|---|
| `401 from the provider` | the request reached the provider, which refused the credential: wrong, out of credit, or a Copilot grant revoked at GitHub |
| `401 token not accepted` | a request came without this task's token |
| `401 this task's token expired at …` | the task outlived `--token-hours` |
| `503` | the vault was locked or the entry removed: `sokar vault unlock` |
| no `request` line at all | omp never used the broker: see `relay.log` beside it |

Why it works the way it does is in [the decisions](decisions.md).
