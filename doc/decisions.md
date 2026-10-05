# Decisions

Settled reasoning that outlives the change that produced it. A decision is written here when
somebody would otherwise ask "why is it like this?" and find only code.

Accepted risks live here too. An accepted risk is not a forgotten one: it says what the exposure
is, why it is not being removed, and what would change the answer.

Ordered by what each decision covers: what the package is, how the agent is pointed at the broker,
what it may do in a task, how it is built and updated, and the risks accepted on the way.

| What it covers | What holds |
|---|---|
| The package | [Why the package is small and the download is not](#why-the-package-is-small-and-the-download-is-not) - about 6 MB carrying a 200 MB fetch, checked against upstream's digest |
| The package | [What the acceptance proves, and with what](#what-the-acceptance-proves-and-with-what) - Fedora 44, the installed package, a real credential |
| The package | [Installed beside Pi, nothing is shared but two variables](#installed-beside-pi-nothing-is-shared-but-two-variables) - no file claimed by both packages; `PI_CONFIG_DIR` and `PI_PROFILE` stay unset |
| The broker | [A fork is not a promise: how this agent is pointed at the broker](#a-fork-is-not-a-promise-how-this-agent-is-pointed-at-the-broker) - the extension loads and does not redirect; a file does |
| The broker | [A relay in the task's namespace, at a literal address](#a-relay-in-the-tasks-namespace-at-a-literal-address) - omp takes only a URL; `127.0.0.1:9419`, the provider's `/api/v1`, a file at `0600` |
| The broker | [A model id that starts with a provider's name goes to that provider](#a-model-id-that-starts-with-a-providers-name-goes-to-that-provider) - measured on 18.1.13; prefix the provider |
| The broker | [GitHub Copilot is brokered by one header, because omp exchanges no token](#github-copilot-is-brokered-by-one-header-because-omp-exchanges-no-token) - a person's grant on the host, a task token in the container, the provider ranked first |
| The task | [omp does not check for a newer version in a task](#omp-does-not-check-for-a-newer-version-in-a-task) - the check could only fail, and never installs anything |
| The task | [No host of its own, and six refused](#no-host-of-its-own-and-six-refused) - what 18.1.13 resolved and did not need |
| The task | [omp starts at its prompt: the setup wizard is marked done](#omp-starts-at-its-prompt-the-setup-wizard-is-marked-done) - a five-step wizard opens even with a credential; setupVersion: 2 answers it |
| The task | [Reaching work is checked on the rendered screen, because omp draws its prompt first](#reaching-work-is-checked-on-the-rendered-screen-because-omp-draws-its-prompt-first) - a declared marker, three seconds on screen; the byte stream passes wrongly |
| The task | [Waiting for a person is read from the question's own last line](#waiting-for-a-person-is-read-from-the-questions-own-last-line) - `↑/↓ move · ⎋ cancel` in the last 5 lines, attended only |
| The task | [A task that comes back continues its conversation](#a-task-that-comes-back-continues-its-conversation) - `--resume` with the id from the session record |
| Updates | [Its release cadence is unlike the other two](#its-release-cadence-is-unlike-the-other-two) - many releases to Pi's few, so no rule follows from a shared origin |
| Updates | [What an update takes, and when](#what-an-update-takes-and-when) - three days old and still the newest, a patch bump, a real model call |
| The build | [The release tooling is Sokar's, configured from the pom](#the-release-tooling-is-sokars-configured-from-the-pom) - data beside the pin, and the matrix its digest parser was measured on |
| The build | [Actions run from a commit, and Dependabot moves them](#actions-run-from-a-commit-and-dependabot-moves-them) - every `uses:` at a commit, `check-actions` refuses the rest, GraalVM by Sokar's pin |
| The build | [NullAway is configured in this pom, not in the shared parent](#nullaway-is-configured-in-this-pom-not-in-the-shared-parent) - the parent is not this repository's to change |
| The build | [The changelog is written by hand, and nothing enforces it](#the-changelog-is-written-by-hand-and-nothing-enforces-it) - requiring an entry belongs to Sokar's changelog check, on logchange |
| Risk | [Accepted risk: the release binary and its digest share one trust root](#accepted-risk-the-release-binary-and-its-digest-share-one-trust-root) - nothing independent to verify the download against, and why that stays |

## Why the package is small and the download is not

The package is about 6 MB because it fetches upstream's self-contained 200 MB binary at image-build
time and checks it against the digest upstream publishes, rather than carrying it. The trade is
recorded in the accepted risk below: one trust root for both the binary and its digest.

## What the acceptance proves, and with what

Run whole on **Fedora 44** against the installed `.rpm` with a **real OpenRouter key**:

- both packages install from the package repository rather than from a build tree;
- `sokar` discovers an agent it was never linked against;
- **the agent authenticates against the provider and completes a prompt**;
- the container holds no credential but the task-scoped token;
- and the key appears in no log the run produces.

Also measured: the digest matching upstream's published sum; `omp --version` on a stock
`ubuntu:24.04` with nothing added; the broker path end to end with a deliberately fake key, ending
in the provider's own 401 rather than the proxy's; and coexistence with Pi, whose tree is nearly
identical under a different name and is exactly where a collision would hide.

## Installed beside Pi, nothing is shared but two variables

Oh My Pi and Pi are near-identical trees under different names, which is where a collision would hide.
Measured on Ubuntu 26.04 with both packages installed:

| | `sokar-agent-omp` | `sokar-agent-pi` |
|---|---|---|
| agent name | `omp` | `pi` |
| binary in the container | `omp` | `pi` |
| adapter on the host | `…/agents/sokar-agent-omp` | `…/agents/sokar-agent-pi` |
| what the package installs elsewhere | `/usr/share/sokar/sbom/sokar-agent-omp.cdx.json` | `/usr/share/sokar/agents/pi/pi-tree.tar.gz` |
| where the tool lands in the image | `/home/agent/.local/bin/omp` | `/opt/pi`, `/opt/node`, `/usr/local/bin/pi` |
| file Sokar writes into the container | `/home/agent/.omp/agent/models.yml` | `/home/agent/.pi/agent/extensions/sokar-route.ts` |

`comm -12` over the two `dpkg -L` listings finds no file claimed by both, and `sokar agents` lists
each under its own name from its own package.

**One shared name remains, and it is an environment variable.** omp honours `PI_CONFIG_DIR` and
`PI_PROFILE` from its ancestry. Either one moves its config directory away from `~/.omp/agent`, so the
file Sokar writes is never read and the task talks straight to the provider - measured. Sokar sets
neither, and nothing in a task image sets them.

## A fork is not a promise: how this agent is pointed at the broker

Oh My Pi is a fork of Pi, and sharing an origin does not mean sharing a mechanism.
**`registerProvider` survives the fork** - same name, compatible signature - **and it does not
redirect a built-in provider** in 18.1.13. Measured: the extension loads, runs, writes its marker,
and requests still go to `openrouter.ai`.

What works is `providers.<name>.baseUrl` in `~/.omp/agent/models.yml`, so the container is pointed
at the broker **by a file rather than by a variable or an extension**. The packaging this agent
shares with Pi matters more than the mechanism it does not.

## A relay in the task's namespace, at a literal address

omp can only be given a URL. A host-side listener is unreachable from a rootless container, so the
**listening end** moves into the task's network namespace: `sokar vault relay` binds `127.0.0.1:9419`
there and forwards to the broker's unix socket on the host. The broker itself stays on the host, where
it keeps the host's DNS and egress and holds the real credential.

What the container gets is one file, `/home/agent/.omp/agent/models.yml`, and no variable:

```yaml
providers:
  "openrouter":
    baseUrl: "http://127.0.0.1:9419/api/v1"
    apiKey: "sokar_pt_…"
```

`sokar_pt_…` is a token for this task only; the broker checks it, swaps in the real key and reissues
the request to `https://openrouter.ai`. Three things about the file are load-bearing:

- **`/api/v1` belongs to the provider, not to omp.** OpenRouter serves the OpenAI dialect there, and
  omp appends its own `/responses` to whatever base URL it is given. A base URL without `/api/v1`
  answers 404.
- **The address is literal, not `localhost`.** Node and Bun resolve `localhost` to `::1` first and the
  relay binds IPv4, so `localhost` is a connection refused.
- **It is written `0600` and owned by the agent user**, because it holds a token.

A task whose `vault.log` shows no request at all never used the endpoint: `relay.log` in the same
directory, the file in the container and the two variables in
[Installed beside Pi](#installed-beside-pi-nothing-is-shared-but-two-variables) are where to look.

## A model id that starts with a provider's name goes to that provider

`--model` is passed straight through, and omp resolves it against its bundled catalogue. Measured on
18.1.13:

```
--model z-ai/glm-4.6                   -> provider openrouter, which is the one with a credential
--model openai/gpt-4o-mini             -> provider openai, which has none: "No API key found"
--model openrouter/openai/gpt-4o-mini  -> provider openrouter
```

A bare id resolves to the provider with a credential **unless its first segment is itself a
provider's name** - then that provider wins and the run fails looking like a missing key. Prefixing
the provider answers it. Whether the provider order the adapter writes (see GitHub Copilot below)
changes this on 18.4.1 is not measured.

## GitHub Copilot is brokered by one header, because omp exchanges no token

A subscription reached through a browser sign-in is what this agent is for, and Copilot is the one
that matters: an organisation that has approved only Copilot can run Oh My Pi only if Copilot
can be brokered. **It can, measured on 18.4.1** with a real Copilot account, through Sokar on a VM:
`--provider github-copilot` answered, and the container held only the task's token - no GitHub token
in its environment or its files.

**Why one header is enough:** omp sends the GitHub OAuth token itself to Copilot as `Authorization:
Bearer`, with no exchange for a short-lived Copilot token - its source says so, and its refresh hands the
same token back. So the broker mints nothing; it replaces one header. The token comes from GitHub's
device flow - OpenCode's OAuth app on github.com, scope `read:user` - which a person grants once on the
host with `sokar vault authorize github-copilot`. The token never expires, so Sokar keeps it as it is,
and removing it here cannot revoke it: that is done at GitHub, under Authorized OAuth Apps. An
organisation that restricts third-party OAuth apps has to approve OpenCode's, which is a different
approval from Copilot's.

**omp's own store is not in the way.** `/login` writes into `agent.db`, but a key in `models.yml`
ranks above it, and the adapter writes the endpoint and the task's token there for Copilot as for any
provider. A base that is not Copilot's own is kept as given, with Copilot's headers and per-model paths
under it, and omp does not then ask `api.github.com` for an endpoint; none was asked in the run.
The same store is why this adapter declares no `config_dir`: `sokar vault import` has nothing to read,
and Oh My Pi's other `/login` services are not brokered.

**The task's provider is ranked first.** Several providers offer the same model id - `gpt-4.1` is
`openai`'s too - and without an order omp picks `openai`, which the task has no key for. So the
adapter writes `modelProviderOrder: [<the task's provider>]` into its settings, and a bare model goes
to the provider the task was started with. Proven by `OmpContainerSetupTest` and the run above.

## omp does not check for a newer version in a task

**Read in 18.1.13, not run:** with `startup.checkUpdate` on, which is the default, omp
asks the npm registry or GitHub for a newer release at start and shows a notice. It installs nothing
by itself. Neither host is reachable from a task, so the check can only fail.

**How it is stopped:** `startup.checkUpdate: false` in `~/.omp/agent/config.yml`, written into every
container. Measured on 18.1.13: `omp config get startup.checkUpdate` answers `false` with the file
and `true` without it.

**What is not covered:** `omp update` has no switch, and it would replace
`/home/agent/.local/bin/omp`, which the agent can write. What keeps it from doing that is that the
task reaches neither host.

**What would change it:** an omp release that installs updates by itself, or a switch for
`omp update`.

## No host of its own, and six refused

omp needs nothing to resolve before it will start: the provider's host is declared by the provider,
so `omp.yaml` has no `allowed_domains`. Its `refused_domains` are what 18.1.13 resolved during a
headless run - `catalog.stencil.so`, `api.kilo.ai`, `api.venice.ai`, `zenmux.ai`,
`coding-intl.dashscope.aliyuncs.com`, `registry.npmjs.org`: its own model catalogue, live model lists
for providers the task has no credential for, and the registry it was published to. Every one was
answered NXDOMAIN and the run still reached its endpoint and completed, which is why they are refused
rather than allowed. Listing them lets a reviewer, and the domain-coverage check, tell a refusal from
an oversight.

Every provider Sokar knows is declared for it, because it speaks `native` - whichever dialect its
provider does. OpenRouter and GitHub Copilot are the ones that have been run.

## omp starts at its prompt: the setup wizard is marked done

**A fresh omp opens at a five-step setup wizard** - providers to sign in to, the default model, glyph
mode, composer layout, theme - **even with a credential in place.** Attended, the task then waits at
a menu. Skipping every step writes `setupVersion: 2` to `~/.omp/agent/config.yml` and nothing else
that changes behavior; the rest is session state. So the config file this adapter writes carries
that line, and omp starts at its prompt.

Measured on 18.1.13 at a terminal, a fresh task per run: without the line, the wizard; with it, the
prompt - with a credential, and without one (omp started by hand in a task with a shell attached,
since Sokar does not start the agent without a credential; it then warns that no model is
available). **Headless**, a prompt run answers and exits either way: the wizard is attended only.

## Reaching work is checked on the rendered screen, because omp draws its prompt first

**The acceptance run fails when anything comes before omp's prompt**, known or not. The definition
declares `Welcome back!`, and the kit's step waits for it attended, typing nothing; a second scenario
requires an unattended run to end within its bound. **omp draws its welcome box before a dialog and
the dialog over it**: in the bytes of a start with the setup wizard back, `Welcome back!` comes at
3,509 and `Setup step` at 468,241. So a check that reads the byte stream passes while the wizard is
up - measured, it did. The step reads the screen as the task's tmux renders it, and needs the marker
there for three seconds on end: with `setupVersion: 2` taken out of the build it fails, and the screen
it reports shows the wizard. Both scenarios pass on 18.4.1.

## Its release cadence is unlike the other two

Hundreds of npm versions against Pi's pre-1.0 handful, often a release a day. Nothing in the
update pipeline can be shared with Pi on the grounds of the two being the same project. The ageing
rule is the same as the other two's, and it is what answers the cadence - see below.

## Waiting for a person is read from the question's own last line

Sokar tells a person the agent is waiting for them from `session.waiting` in the agent's YAML: literal
lines on the attached screen that only a question draws. Measured on 18.4.1 at a terminal, a
question Oh My Pi puts ends with `⏎ select · n note · ↑/↓ move · ⎋ cancel` - symbols, not the words "Enter" and "Esc". The rule is `↑/↓ move · ⎋ cancel` in the last 5 lines. Its working line starts with `⎋` too, which is why the rule is not the symbol alone. The window title's `π !` says the same, but Sokar reads the screen; like the title, the box is drawn for its ask tool and approval prompts, not the model picker or /login - it under-reports, the safe direction.

**Proven three ways.** `WaitingDeclarationTest` reads the declaration against the measured screens, and
fails without it. By hand on the VM, Sokar read the agent as not waiting at rest and as waiting once it
asked. `waiting.feature` drives it to a question at the version it pins and asks Sokar, never the
screen - so a release that words its questions differently fails the build instead of going quiet. The scenario enters the prompt with the kit's `I enter`, text and Enter together, which Oh My Pi takes as typed and submits.

**Unattended there is nothing to declare**: a run that wanted to ask ends, and Sokar says what it said
last rather than that it waits.

**What would change the answer:** a release that draws its questions differently, which the scenario is
there to catch.

## A task that comes back continues its conversation

Sokar records the session a task's agent ran and passes it back with `--resume` when the task starts
again. **`--resume`, not `--session`:** 18.4.1 has no `--session`, and a task started again with it fails
on an unknown option. Where the id is, `omp.yaml` declares under `session.session_id`. Measured on
18.4.1: an unattended run's first record, `{"type":"session"}`, carries it as `id`. An attached session is
kept as `~/.omp/agent/sessions/--workspace--/<timestamp>_<id>.jsonl`, with the same `{"type":"session"}`
record inside it after a title record; Sokar reads the id from inside the newest session file.

**Proven at the version it pins.** `SessionIdDeclarationTest` reads the declaration against the measured
records and file names. `session.feature` drives it through Sokar on a VM: a word to remember in one run,
the task started again, Sokar reporting that it continues the session, and the word given back although
the second prompt never names it.
The attached half is proven the same way: a message, the task stopped and started
again, and the earlier message back on its screen.

## The release tooling is Sokar's, configured from the pom

The release tooling is the shared `sokar-release`, not a copy in each agent repository: copies of
one tool drift apart.

**What differs between agents is data**, and it lives in `pom.xml` as `sokar.release.*`, beside
`agent.cli.version`: the label, the source definition, the newest GitHub release as upstream, and
the `SHA256SUMS.txt` the digest is read from. Flags on each call would put the same facts on every
workflow line, and the second copy is the one that goes stale. At package time the tool is a
plugin dependency of the exec plugin, not of the project, so it never reaches the bill of materials
or the native image's classpath.

**The digest parser was measured on this matrix**, each row served as a `SHA256SUMS.txt` to the tool's
`check-pin`:

| input | expected |
|---|---|
| one valid digest for the asset | accepted |
| the asset absent from the body | refused, exit 1 |
| two different digests for the asset | refused, exit 1 |
| `abc123` | refused - not a digest |
| 64 uppercase hex characters | refused - not lowercase |
| the same digest twice | accepted |

**Also measured of the tool:** `add-fetched-cli` writes the bill entry for the fetched binary;
`upstream-version` answers `rollback` for a newest release older than the pin; `check-pin` fails on
a digest one character off; `compare-bills` stops on a component the update did not name.

## Actions run from a commit, and Dependabot moves them

Every `uses:` names a full commit with its release beside it, `@<commit> # vX.Y.Z`. A tag is a name its
owner may point anywhere, and these jobs hold the publishing token, the machine credentials and a
token that merges pull requests. GitHub's own actions are held to the same rule: the argument does not
depend on who publishes the action.

**Dependabot keeps the pins current**, in every repository. A pin nobody moves rots, and a stale action with a known flaw is not safer than the current tag. Each
release arrives as a pull request with the new commit and its version, and nothing merges it
automatically: the review is what the pin buys.

**`sokar-release check-actions` fails the build on a step that names a tag, a branch, a bare hash or a
line of releases like `# v7`**, and says how to pin it, so a new workflow cannot bring a tag back. It is
Sokar's one check for this rule, run in the build job, and this repository keeps no second test for it.
A step of this repository and an image by digest pass. Measured against this repository's workflows,
with a tag, a `# v7`, a `setup-graalvm` at a commit and a Dependabot without `/.github/actions/*` each
put in: every one refused.

**Dependabot watches the local actions too, and waits three days.** `dependabot.yml` lists
`/.github/actions/*` beside `/`, because the pinned-JDK action pins its cache, and a Dependabot that
watches only the workflows never moves that pin. A release is taken after three days, as every other
pin here, and the week's moves come as one grouped pull request. `check-actions` fails when the local
actions are not watched.

**`mvnw` checks the Maven it downloads**: `distributionSha256Sum` in `.mvn/wrapper/maven-wrapper.properties`,
for 3.9.15 the digest Apache's own SHA-512 confirms. A wrong one stops the wrapper before Maven runs.

**Nothing else in these workflows is fetched by a name**: the only `curl` is `jf rt curl` reading this
product's own Artifactory. The setup actions download their tools themselves, and there the two differ:

- **The JFrog CLI is fixed by the action's commit.** `setup-jfrog-cli` 5.2.0 defaults to `jf` 2.124.0
  and asks for the newest only when told to, so a Dependabot bump moves both together, under review. It
  checks no digest, which is the same publisher's trust as the action.
- **GraalVM is the one Sokar pins, checked against its digest.** `setup-graalvm` with `'25'` resolves
  the newest 25.x at run time and checks nothing for community builds - and that JDK compiles the
  native binary this repository publishes. So every job uses `./.github/actions/pinned-jdk`: the
  runner's own Java 25 runs `sokar-machines jdk --github` once, which installs the GraalVM
  `sokar-machines` pins (`machines.graalvm.*`, moved by Sokar's Machines workflow under the same
  three-day rule), checks its digest before unpacking, and sets `JAVA_HOME`. The build and the
  acceptance machines use the same pin, by construction. `check-actions` refuses `setup-graalvm` and
  `setup-java` even at a pinned commit, because the commit fixes the action, not the JDK it fetches.
  Measured on the VM as a stand-in runner - an empty home, Temurin 25 in place of the runner's Java:
  GraalVM 25.0.2 installed and checked, and this repository's native build and packages made with it.

## What an update takes, and when

Sokar's release tool applies these rules; `UpdateRulesTest` fails when this
repository stops asking for them.

**A release is taken once it is three days old, and still the newest**: `sokar.release.min-age` is
`3d`. A release withdrawn or patched within days never becomes a pull request, and the people who
install on the first day get the days to report what breaks. A younger release is not skipped for the
one before it: the job waits and says until when. A release whose date cannot be read is never old
enough.

**A pin move bumps the package's patch version**, `0.4.1-SNAPSHOT` to `0.4.2-SNAPSHOT`, so a package
version names what it installs and `apt` sees an upgrade.

**The verifying tier makes a real model call**, with its key in the workflow's secrets - intended, not
temporary. Published metadata proves the download is intact; only a real request proves the new
version still starts without a question, reads its credential variable, routes through the broker and
gets an answer.

**The CI machines are not kept current here.** GraalVM and the pre-pulled base images are Sokar's
machine tooling, under the same rule, once for every repository.

**The three-day rule answers this agent's cadence**: a release superseded within three days is never
opened as a pull request.

**What would change the answer:** a release that cannot wait three days. Dispatching the job with the
version named takes it at once - that is the way round the rule, not a change to it.

## NullAway is configured in this pom, not in the shared parent

`org.fuin:pom` would make it true in every repository at once, but it is not this repository's to
change, and waiting for it would leave the `@NullMarked` promise unchecked. So the compiler
configuration, the two versions and `.mvn/jvm.config` are here, identical in `sokar-claude-code` and
`sokar-pi`. Three copies of one block is the shape `AGENTS.md` warns about; it is accepted because
the parent is the one place that removes it, and **moving it there is the answer the day the parent
takes it** - then all three copies go in the same change.

Only the `default-compile` execution runs it: tests pass `null` on purpose, and Error Prone never
sees them. `.mvn/jvm.config` exists because Error Prone runs inside javac in Maven's own JVM, and
from JDK 16 on that JVM refuses it the compiler's internals - measured on JDK 25, an
`IllegalAccessError` on `com.sun.tools.javac.api` before a single file is checked.

## The changelog is written by hand, and nothing enforces it

The changelog is written by hand in the same commit as the change, and no check requires it. A
check for a hand-kept file would have to be rebuilt as soon as Sokar's move to logchange - one YAML
file per change, and a generated `CHANGELOG.md` - reaches this repository. Requiring an entry comes
with Sokar's changelog check, proposed to logchange upstream, which keeps three lessons: a waiver answers for its
own commit only, documentation is not exempt, and a range that cannot be compared fails.

**What would change it:** Sokar's changelog check landing, or logchange being adopted here.

## Accepted risk: the release binary and its digest share one trust root

This package installs the Oh My Pi binary from that project's GitHub releases, pinned to a version
and checked against a SHA-256 recorded in `agent/omp.yaml`. The digest that pin is compared with
comes from `SHA256SUMS.txt` **in the same release**.

That protects against a corrupted download, a mismatched pin and accidental drift. It does **not**
prove authenticity: anybody who can change both objects - a compromised release account or a
tampered release asset - passes every check this repository makes. The binary then runs in every
task image built with this adapter.

**Why it is accepted rather than fixed:** upstream publishes no signature or provenance
attestation that we could verify against, and both objects come from one authority. A check we
cannot perform cannot be written.

**What would change it:** upstream signing its releases, publishing build provenance, or GitHub
artifact attestations for the release assets. Upstream changes its release workflow often.

**What reduces it meanwhile:** the version is pinned rather than floating; the digest is checked
before installation in the image layer; the update job opens a pull request rather than publishing
by itself; and the release tool refuses a malformed or duplicated digest before writing it anywhere.
