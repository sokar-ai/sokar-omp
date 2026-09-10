# sokar-omp

The [Sokar](https://github.com/sokar-ai/sokar) adapter for
[Oh My Pi](https://github.com/can1357/oh-my-pi) — published on npm as
`@oh-my-pi/pi-coding-agent`, shipped as a standalone binary called `omp`, and
homed at [omp.sh](https://omp.sh).

**Not [Pi](https://github.com/earendil-works/pi)**, which
[sokar-pi](https://github.com/sokar-ai/sokar-pi) adapts. That is a different
project by a different author, published as `@earendil-works/pi-coding-agent`
and run as `pi`. Oh My Pi is a fork of a *third* project also called Pi. Three
codebases, two names: the link is the identity, and the two packages are checked
against each other rather than assumed apart — see
[Installed beside Pi](#installed-beside-pi).

## Install

Needs [Sokar](https://github.com/sokar-ai/sokar) itself - this package declares `Depends: sokar`, and both
come from the same repository.

Set the package repository up once, as the flavour's guide describes —
[Debian and Ubuntu](https://github.com/sokar-ai/sokar/blob/main/doc/getting-started-debian.md)
or [Fedora and RHEL](https://github.com/sokar-ai/sokar/blob/main/doc/getting-started-fedora.md)
— then:

```
sudo apt install sokar-agent-omp      # or: sudo dnf install sokar-agent-omp
```

**The package is 6 MB and the tool is not in it.** Upstream publishes one
self-contained Linux binary per release with a `SHA256SUMS.txt` beside it, so the
image build fetches and verifies it rather than the package carrying it. That
binary is 200 MB, because `omp` embeds the Bun runtime it needs — it is not a
Node script, and no Node runtime is installed for it.

Nothing has to be registered. Sokar scans `/usr/libexec/sokar/agents` and asks
whatever it finds to describe itself:

```
$ sokar agents
NAME   BINARY   LABEL      FROM
omp    omp      Oh My Pi   /usr/libexec/sokar/agents/sokar-agent-omp
```

See [build](build.md) to build it yourself.

Two different things get called "the agent", and the difference matters when
something goes wrong:

|                   | Where it lives                                     | What it is                          |
|-------------------|----------------------------------------------------|-------------------------------------|
| `sokar-agent-omp` | on the **host**, in `/usr/libexec/sokar/agents`    | this adapter, 15 MB, one file       |
| `omp`             | inside the **task image**, at `~/.local/bin/omp`   | the CLI itself, one 200 MB file     |

## Storing the credential

Oh My Pi has one credential kind here, an OpenRouter API key, so there is no
`--credential-type` to state. Get one from
[openrouter.ai/keys](https://openrouter.ai/keys). It is stored under the
**provider's** name, not this agent's, so a second agent reaching OpenRouter uses
the same entry instead of a second copy of the same secret:

```
sokar vault unlock
printf '%s' 'sk-or-…' | sokar vault put openrouter
```

Unlock **first**: `vault put` reads the credential from standard input, so it has
nothing left to read a passphrase from. Use `printf`, not `echo`, or a newline
becomes part of your key.

Oh My Pi's own `/login` also reaches subscription-routed services, GitHub Copilot
among them. **None of those is brokered here.** `omp` keeps them in a SQLite
store of its own (`~/.omp/agent/agent.db`), which is why this adapter declares no
`config_dir` and `sokar vault import` has nothing to read. Whether any of them
can be brokered at all is untested.

## Naming a model

`--model` is passed straight through, and omp resolves it against its bundled
catalogue. One trap, measured against 18.1.13:

```
--model z-ai/glm-4.6                   → provider openrouter, which is the one with a credential
--model openai/gpt-4o-mini             → provider openai, which has none: "No API key found"
--model openrouter/openai/gpt-4o-mini  → provider openrouter
```

A bare id resolves to whichever provider has a credential, **unless its first
segment is itself a provider's name** — then that provider wins and the run fails
looking like a missing key. Prefix the provider when the id starts with one.

## What the container actually gets

Not your credential, and not an environment variable either. **One file:**

```
/home/agent/.omp/agent/models.yml
```

```yaml
providers:
  "openrouter":
    baseUrl: "http://127.0.0.1:9419/api/v1"
    apiKey: "sokar_pt_…"
```

`sokar_pt_…` is a phantom token for this task only. omp sends it to the broker,
which checks it, swaps in your real key and reissues the request to
`https://openrouter.ai`. Your key never enters the container, and the token stops
working when the task ends.

Three things about that file are load-bearing:

- **It is a file, not a variable, and not an extension.** omp has no variable for
  an endpoint. Its extension API looks like it should do the job — extensions are
  auto-discovered from `~/.omp/agent/extensions/*.ts` and `registerProvider(name,
  { baseUrl })` is documented for exactly this — but measured against 18.1.13 the
  extension loads, runs, and the requests still go to `openrouter.ai`. The
  models file redirects them; the extension does not.
- **`/api/v1` belongs to the provider, not to omp.** OpenRouter serves the OpenAI
  dialect there, and omp appends its own `/responses` to whatever base URL it is
  given. A base URL without `/api/v1` answers 404.
- **The address is literal, not `localhost`.** Node and Bun resolve `localhost`
  to `::1` first and the relay binds IPv4, so `localhost` is a connection
  refused.

The file is written `0600` and owned by the agent user, because it holds a token.

## Why there is a relay

omp can only be given a URL. A host-side listener is unreachable from a rootless
container, so the **listening end** moves into the task's network namespace:
`sokar vault relay` binds `127.0.0.1:9419` there and forwards to the broker's
unix socket on the host. The broker itself stays on the host, where it keeps the
host's DNS and egress and holds the real credential.

## What it is allowed to reach

```
$ sokar agents --verbose
NAME   BINARY   LABEL      FROM
omp    omp      Oh My Pi   /usr/libexec/sokar/agents/sokar-agent-omp
       domains:
       speaks:  native
       provider: anthropic -> api.anthropic.com (the credential is swapped in on the way out)
       provider: openrouter (default) -> openrouter.ai (the credential is swapped in on the way out)
       refused: catalog.stencil.so, api.kilo.ai, api.venice.ai, zenmux.ai, coding-intl.dashscope.aliyuncs.com, registry.npmjs.org
       resume:  yes
```

Two providers because it speaks `native` — whichever dialect its provider does —
so any provider Sokar knows can serve it. Only OpenRouter has been run.

**No domain of its own.** Unlike Claude Code, omp needs nothing to resolve before
it will start — the provider's host is declared by the provider, and everything
else it asks for is refused. Those six names are what 18.1.13 resolved during a
headless run: its own model catalogue, live model lists for providers this task
has no credential for, and the registry it was published to. Every one was
answered NXDOMAIN and the run still reached its endpoint, which is why they are
refused rather than allowed.

## Installed beside Pi

The two are near-identical trees under different names, which is where a
collision would hide. Measured on Ubuntu 26.04 with both packages installed:

| | `sokar-agent-omp` | `sokar-agent-pi` |
|---|---|---|
| agent name | `omp` | `pi` |
| binary in the container | `omp` | `pi` |
| adapter on the host | `…/agents/sokar-agent-omp` | `…/agents/sokar-agent-pi` |
| what the package installs elsewhere | `/usr/share/sokar/sbom/sokar-agent-omp.cdx.json` | `/usr/share/sokar/agents/pi/pi-tree.tar.gz` |
| where the tool lands in the image | `/home/agent/.local/bin/omp` | `/opt/pi`, `/opt/node`, `/usr/local/bin/pi` |
| file Sokar writes into the container | `/home/agent/.omp/agent/models.yml` | `/home/agent/.pi/agent/extensions/sokar-route.ts` |

`comm -12` over the two `dpkg -L` listings finds no file claimed by both, and
`sokar agents` lists each under its own name from its own package.

**One shared name remains, and it is an environment variable.** omp still honours
`PI_CONFIG_DIR` and `PI_PROFILE` from its ancestry. Either one moves its config
directory away from `~/.omp/agent`, so the file Sokar writes is never read and
the task talks straight to the provider — measured. Sokar sets neither, and
nothing in a task image sets them; do not add them.

## Bumping the Oh My Pi version

Two edits, both in files this repository owns:

| file | what |
|---|---|
| `pom.xml` | `agent.cli.version`, filtered into the URL in `omp.yaml` and into the package description |
| `src/main/resources/agent/omp.yaml` | the `sha256`, from that release's own `SHA256SUMS.txt` |

```
curl -sSL https://github.com/can1357/oh-my-pi/releases/download/v<version>/SHA256SUMS.txt \
    | grep omp-linux-x64
```

Take the `omp-linux-x64` line — the glibc build, which is what a task image on
`ubuntu:24.04` runs. The digest is the publisher's, so this is verifiable rather
than trusted, and `OmpAgentTest` fails if the URL and the version stop agreeing.

**The releases are frequent.** npm lists 617 versions of
`@oh-my-pi/pi-coding-agent` up to and including 18.1.13, three of them on three
consecutive days. Being a fork of Pi says nothing about its cadence: Pi was on
0.85.0 at the same moment.

`sokar agents --supply-chain` reports what is pinned, so "which version ran" is
answerable from the installed adapter rather than from a build log.

## When it will not authenticate

Check what actually reached the proxy — `vault.log` in the task's state
directory, `/run/user/<uid>/sokar/<container>/`:

```
request   POST /api/v1/responses -> 401 from the provider
```

- **`401 from the provider`** — the request reached OpenRouter and it rejected the
  credential. The plumbing works; the key is wrong or out of credit. OpenRouter
  words this one confusingly: a *bad* bearer token comes back as
  `401 Missing Authentication header`, while *no* header at all comes back as
  `No cookie auth credentials found`. The first means your key was sent and
  refused.
- **`401 token not accepted`** — the proxy rejected the phantom token. It is from
  another task, or the task has outlived `--token-hours`.
- **no `request` lines at all** — omp never used the endpoint. Check `relay.log`
  in the same directory, that `models.yml` exists in the container, and that no
  `PI_CONFIG_DIR` or `PI_PROFILE` is set.
- **`No API key found for <provider>`** — the model id resolved to a provider the
  models file says nothing about. See [Naming a model](#naming-a-model).

## What has been checked

`buildtools/broker-check.sh` runs a real task with a **deliberately fake**
OpenRouter key and asserts the whole path around the credential: the models file
is in the container, `0600`, carrying a phantom token; the real key is in neither
the environment nor the file; and the request reached the provider through the
broker. The provider's 401 is the proof — it is OpenRouter's rejection of a bad
key, not the proxy's rejection of the token, so every hop ran.

```
$ buildtools/broker-check.sh
  PASS  the agent's own setup file is in the container (/home/agent/.omp/agent/models.yml)
  PASS  it carries a task-scoped token
  PASS  it is readable only by the agent user (600 agent)
  PASS  the real credential is not in the container's environment or its setup file
  PASS  omp's requests went through the broker
        | request   GET /api/v1/models -> 200 from the provider
        | request   POST /api/v1/responses -> 401 from the provider
```

Sokar's own `buildtools/e2e-tier1.sh` drives this agent with
`SOKAR_E2E_AGENT=omp SOKAR_E2E_AGENT_MODULE=<path to this checkout>`, and covers
what this script does not: the image build, the pinned version, container
hardening, and domain coverage. **Its `credential exchange` check cannot pass
here**, and that is a gap in the check rather than in the agent: it looks for a
socket or base-URL variable in the container, and an agent pointed at a broker by
a file has neither. `sokar-agent-pi` fails the same check for the same reason.

What is **not** checked anywhere yet: that a valid key gets a 200. That needs an
OpenRouter account, and it is the one thing above the fake credential cannot
stand in for.
