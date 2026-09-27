# Building

Nothing here needs a checkout of [Sokar](https://github.com/sokar-ai/sokar). This
repository compiles against the **published agent contract** - `sokar-agent-api`
and `sokar-wire` - resolved from Maven, and that is the property worth keeping: if
a build here ever needs the Sokar sources, the split has been undone without
anyone deciding to undo it.

```
./mvnw -s settings.xml verify                     # unit tests
./mvnw -s settings.xml -Pnative,dist verify       # + the binary, the .deb and the .rpm
```

`-s settings.xml` is not optional while the contract is a snapshot: it declares the
repository the snapshot comes from. The native build needs GraalVM as `JAVA_HOME`.

What the second command leaves in `target/`:

```
sokar-agent-omp                              the adapter, 15 MB, run by Sokar on the host
sokar-agent-omp_1.0.0~SNAPSHOT_amd64.deb     6.2 MB
sokar-agent-omp-1.0.0~SNAPSHOT-1.x86_64.rpm  6.2 MB
```

Measured on one machine: 2 s for the unit build, 25 s with the native image and
both packages. **No container runtime is needed to build this** — the tool is not
carried in the package, so there is no tree to assemble. That is the whole
difference from [sokar-pi](https://github.com/sokar-ai/sokar-pi), which takes
minutes and needs podman: Pi is 162 npm packages with no single URL to pin, while
Oh My Pi publishes one self-contained 200 MB binary per release with a
`SHA256SUMS.txt` beside it. Where a publisher offers a digest, the image build
fetches and checks it and the package stays small.

The trade that buys: a task image build for this agent needs network access to
`github.com`, where a Pi image build needs none.

**The package version is this agent's own**, not Sokar's and not Oh My Pi's. An
agent released against an unchanged CLI is still an upgrade, and a Sokar release
does not move it. `~` rather than `-` before `SNAPSHOT` because dpkg and rpm sort
`~` below everything; left as `-SNAPSHOT` it would sort *above* the release and
apt would refuse the upgrade.

The Oh My Pi version the image installs is carried in the package description
instead, so `dpkg -s sokar-agent-omp` and `rpm -qi` still answer it.

## Bumping the Oh My Pi version

Two edits — see [the README](README.md#bumping-the-oh-my-pi-version) for the
command that produces the digest. `OmpAgentTest` fails if the URL and the version
stop agreeing, so the pair cannot drift silently; the digest itself is only
checked where it is used, at image-build time, where a mismatch fails the layer.

## Checking it

Three things, in the order they are worth running.

1. **`./mvnw -s settings.xml verify`** — 14 unit tests. They cover the shape of
   the command line, the definition, and the file the adapter writes into a
   container, including a token containing quotes and newlines.

2. **The `broker.feature` scenario** — a real task on an installed Sokar, with a
   deliberately fake OpenRouter key. It asserts everything around the credential:
   the file is in the container and `0600`, it carries a phantom token, the real
   key is in neither the environment nor the file, and the request reached the
   provider through the broker. The provider's 401 is what proves the chain ran.
   Needs no account; it uses a vault of its own and removes what it created.

3. **Sokar's own `buildtools/e2e-tier1.sh`**, pointed here:

   ```
   SOKAR_E2E_AGENT=omp SOKAR_E2E_AGENT_MODULE=<path to this checkout> \
       buildtools/e2e-tier1.sh
   ```

   It expects a Sokar build tree - `app/target/sokar` and
   `<module>/target/sokar-agent-omp` relative to its own parent directory - so a
   directory with those two paths and the installed binaries copied into them is
   enough to run it against packages rather than a checkout.

   It covers what (2) does not: that the CLI reaches the image at the pinned
   version, that the firewall is applied at create time, and **domain coverage** -
   which names omp resolves that its definition does not declare. That check is
   why `registry.npmjs.org` is in `refused_domains`; nothing else found it.

   Its `credential exchange` check cannot pass for this agent. It looks for a
   socket or base-URL variable in the container, and an agent pointed at a broker
   by a file has neither; `sokar-agent-pi` fails it identically.

What none of them checks is that a **valid** key gets a 200. That needs an
OpenRouter account.

## The changelog

`CHANGELOG.md`, in [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format.
**One sentence per change** - it is a compressed summary of the commits it covers, and
`git log` is where anyone who wants the reasoning goes.

A version bump is not written by hand: the `update` of Sokar's release tool writes its own line and replaces
the one it wrote last time. Everything else is by hand, and since 2026-09-13 nothing checks for
it; requiring an entry returns with Sokar B55 - see [`doc/decisions.md`](doc/decisions.md).

## Following Oh My Pi without watching it

`.github/workflows/update.yml` runs once a week, on Monday. It asks GitHub for the newest
release and, if that is not what this module pins, does what a person would: the release tool's
`update`, rebuild, and prove the result on a real Ubuntu machine and a real Fedora one **before anything
is published**.

**There is no stable channel here.** Claude Code publishes `stable` beside `latest`, so that
pipeline can follow somebody else's judgement about what is ready. Upstream's newest release is
all this one has, which puts the whole weight on the verification below.

**The point is the stopping.** The run refuses to publish when an acceptance suite failed, when
the third-party component set or a license changed, or when the upstream **major** version
moved. Whatever the outcome it opens a pull request; merging is what publishes. Auto-merge is
off behind `SOKAR_UPDATE_AUTO_MERGE`, and needs `SOKAR_UPDATE_TOKEN` as well - a push made with
the workflow's own token triggers no build, so merging with it would publish nothing and report
success.

**A fact newly recorded in the bill stops the next update once.** The published bill does not have it
yet, so the comparison sees it change from nothing - the license of the fetched CLI, added on
2026-09-27, read as *relicensed* until a push published a bill that carried it. Expected, and cleared
by publishing, not by approving the update.

`PinAgreementTest` fails the unit tests when the filtered definition's version, the pom and
the download URL disagree. The license is written beside the digest, read from GitHub's license API at the release's tag, and
lands in the bill, where the update gate stops on a change to it. The digest and the license need the
network, so the release tool's `check-pin` still checks both on every push, against the
`SHA256SUMS.txt` and the license upstream publishes.

## Publishing

A push to `main` uploads the two packages to Artifactory, into the **same repositories
Sokar itself publishes to** - `sokar-dist-deb` and `sokar-dist-rpm`. They belong
together: this package declares `Depends: sokar`, so split across repositories an
operator would have to configure both for the dependency to resolve.

A pull request builds and packages but publishes nothing.

Two repository settings are needed, the same ones Sokar uses: the variable `JF_URL`
(the platform url, **without** `/artifactory`) and the secret `JF_ACCESS_TOKEN`. The
token needs Read, Deploy/Cache, **Annotate** and **Delete** on both repositories -
Annotate because the Debian index is driven by properties, Delete because the snapshot
file name is stable and every build overwrites it.

Uploading is not the same as being installable: indexing is asynchronous, and a
package with missing or wrong properties is stored happily and never listed. The
workflow polls the Debian index for its own package name afterwards and fails the
run if it never appears.

## Acceptance, as a person at a terminal

`src/acceptance` holds Cucumber scenarios that drive a real machine over ssh - a pty for what a
person sees, no pty for what a script gets - through Sokar's published acceptance kit. They are
off unless a host is named, so an ordinary build neither resolves the kit nor compiles them:

```
./mvnw -s settings.xml verify \
    -Dsokar.acceptance.host=<machine with sokar and this agent's package installed> \
    -Dsokar.acceptance.user=acceptance \
    -Dsokar.acceptance.key=$HOME/.ssh/id_ed25519
```

The report lands in `target/acceptance.html`. The scenarios tagged `@credential` need
`SOKAR_E2E_OPENROUTER_API_KEY` and `SOKAR_E2E_MODEL` in the environment of the machine running the
suite - typed into the vault at a terminal, never on a command line - and are **skipped**, not
passed, without them. There is no glue class here: every step is the kit's, which is what keeps
this repository free of test code that knows about ssh.

**A scenario's vault passphrase must be a string no terminal prints.** Each scenario that needs a
vault brings its own - a temporary file and a passphrase the scenario states - and the kit then checks
the passphrase never appears on screen. The rented machine's account is named `acceptance`, and
Ubuntu 26.04's shell integration prints `user=acceptance` in its escape sequences, so a passphrase of
`acceptance` reads as echoed. That failed every such scenario on Hetzner on 2026-09-27 while the VMs,
whose account is `claude`, stayed green.

In CI the same suite runs from the runner against the rented machine on every push to `main`.
A run that produces no scenarios fails rather than passing quietly.
