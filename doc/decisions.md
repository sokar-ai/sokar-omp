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
| The broker | [A fork is not a promise: how this agent is pointed at the broker](#a-fork-is-not-a-promise-how-this-agent-is-pointed-at-the-broker) - the extension loads and does not redirect; a file does |
| The task | [omp does not check for a newer version in a task](#omp-does-not-check-for-a-newer-version-in-a-task) - the check could only fail, and never installs anything |
| Updates | [Its release cadence is unlike the other two](#its-release-cadence-is-unlike-the-other-two) - many releases to Pi's few, so no rule follows from a shared origin |
| The build | [The release tooling is Sokar's, configured from the pom](#the-release-tooling-is-sokars-configured-from-the-pom) - data beside the pin, and the digest parser's matrix it must keep |
| The build | [NullAway is configured in this pom, not in the shared parent](#nullaway-is-configured-in-this-pom-not-in-the-shared-parent) - the parent is not this repository's to change |
| The build | [The changelog is written by hand, and nothing enforces it](#the-changelog-is-written-by-hand-and-nothing-enforces-it) - requiring an entry comes with Sokar B55, on logchange |
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

## A fork is not a promise: how this agent is pointed at the broker

Oh My Pi is a fork of Pi, and sharing an origin does not mean sharing a mechanism.
**`registerProvider` survives the fork** - same name, compatible signature - **and it does not
redirect a built-in provider** in 18.1.13. Measured: the extension loads, runs, writes its marker,
and requests still go to `openrouter.ai`.

What works is `providers.<name>.baseUrl` in `~/.omp/agent/models.yml`, so the container is pointed
at the broker **by a file rather than by a variable or an extension**. The packaging this agent
shares with Pi matters more than the mechanism it does not.

## omp does not check for a newer version in a task

**Read in the pinned 18.1.13, not run:** with `startup.checkUpdate` on, which is the default, omp
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

## Its release cadence is unlike the other two

Hundreds of npm versions against Pi's pre-1.0 handful, often a release a day. Nothing in the
update pipeline can be shared with Pi on the grounds of the two being the same project, and any
ageing rule the other agents adopt needs its own answer here - see OM03 in the
[issue index](../issues/README.md).

## The release tooling is Sokar's, configured from the pom

The release tooling is the shared `sokar-release`, not a copy in each agent repository: copies of
one tool drift apart.

**What differs between agents is data**, and it lives in `pom.xml` as `sokar.release.*`, beside
`agent.cli.version`: the label, the source definition, the newest GitHub release as upstream, and
the `SHA256SUMS.txt` the digest is read from. Flags on each call would put the same facts on every
workflow line, and the second copy is the one that goes stale. At package time the tool is a
plugin dependency of the exec plugin, not of the project, so it never reaches the bill of materials
or the native image's classpath.

**The digest parser must keep this matrix**, each row served as a `SHA256SUMS.txt` to the tool's
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
with Sokar B55, proposed to logchange upstream, which keeps three lessons: a waiver answers for its
own commit only, documentation is not exempt, and a range that cannot be compared fails.

**What would change it:** B55 landing, or logchange being adopted here.

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
artifact attestations for the release assets. Any of those should be verified in the update job and
recorded in the bill of materials. This is worth re-checking whenever upstream changes its release
workflow, which it does often.

**What reduces it meanwhile:** the version is pinned rather than floating; the digest is checked
before installation in the image layer; the update job opens a pull request rather than publishing
by itself; and the release tool refuses a malformed or duplicated digest before writing it anywhere.
