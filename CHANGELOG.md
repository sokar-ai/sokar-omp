# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Headings name this package's version; Oh My Pi's version is what the package installs and
appears as an entry rather than a heading. One sentence per change - `git log` has the detail.

## [Unreleased]

### Removed

- The Python release tools, `acceptance.sh` and `broker-check.sh`, which had drifted from or duplicated the other agent repositories.
- The changelog check in CI; requiring an entry returns later, built on logchange.

### Changed

- Oh My Pi pinned to 18.4.1 (was 18.1.13).
- The build and the update job call Sokar's release tool instead of Python: recording the fetched binary in the bill, the upstream lookup, the pin move, the digest check on every push and the bill comparison.
- The acceptance run is the Cucumber scenarios alone; they refuse to run as root, bring a vault of their own, check that the adapter installs what its bill names, and prove the brokering path with a fake key.
- The pull request carries the third-party comparison itself, with each component's licence, rather than leaving it in the run log.
- Issues carry their repository's letter: `OM07` rather than `007`, so a number says which set it belongs to.
- The acceptance suite makes its project by following a local repository and names it on every task start; its cleanup asks Sokar to remove the project instead of deleting Sokar's directories.
- Every task the acceptance suite starts names its repository, which Sokar now requires.
- The pinned version, the pom and the download URL are checked by a unit test on the filtered definition the package ships.

### Security

- omp no longer checks for a newer version at start inside a task; the check could only fail there.
- The acceptance suite no longer puts the test credential on a command line; the pattern reaches `grep` on a file descriptor.
- The check that no log holds the credential also searches with line breaks removed, so a value split across a newline is found rather than reported as absent.
- The acceptance run passes the model name to the container as data rather than inside a shell command.
- The update script refuses a malformed or duplicated upstream digest before writing it anywhere.
- The CI no longer installs the unpinned Hetzner Python client; nothing had used it since the Java machine tooling replaced it.

### Added

- The build refuses a main package that is not null-marked, so NullAway cannot skip one in silence.
- The compile checks the package's nullness contract with NullAway, so returning null where a type promises a value fails the build.
- The bill records the pinned Oh My Pi's license, read per release from GitHub, so a release that changes it stops the update.
- The build refuses an issue number that names no issue, so a citation in prose cannot outlive the issue it points at.
- The build refuses a link to an issue file from anywhere but the index, so a pointer cannot outlive the issue it names.
- The packages provide `sokar-agent`, so the setup script and the daemon list this agent as one a person can choose.
- The Oh My Pi adapter: definition, credential handling, headless commands. Its log is shown as omp writes it; there is no formatter for it yet.
- `.deb` and `.rpm` packages, published to Artifactory from `main`.
- Oh My Pi 18.1.13, pinned by version and SHA-256 against the `SHA256SUMS.txt` upstream publishes.
- A CycloneDX bill of materials in every package.
- An acceptance suite against the published packages on Ubuntu and Fedora, with a tier that authenticates for real.
- Weekly automated updates, verifying a new version on both distributions before anything is published.
- `buildtools/upstream-version.py`, `buildtools/update.py` and `buildtools/check-pin.py` for that pipeline.
- `buildtools/compare-bills.py`, so an update stops when the third-party set or a license changes.
- `buildtools/check-changelog.py`, failing a code change that does not say what changed.
- This changelog.

### Fixed

- An attended task starts at Oh My Pi's prompt instead of its five-step setup wizard.
- The native binary starts on any x86-64 CPU; it needed AVX2, so on a pre-Haswell host or a VM with a conservative CPU model the package installed and then would not start.
- The unit suite no longer pins the upstream version in an assertion, which made every automated update fail with "expected 18.1.13 but was 18.2.7".
- The packages declare the glibc (2.34) and zlib the native binary links against, and the build fails when the binary needs more.
- The build's index check follows Artifactory's redirect to cloud storage; it had read every package as not indexed.
- The models file is written only when there is a token to put in it, not merely an endpoint.
- The bill records the CLI the image fetches, with its publisher's digest, so the update gate can see it change.
- The update job stops instead of rolling back when upstream offers an older version than is pinned, and compares versions as numbers rather than text.
- The changelog waiver answers for the commit it is written on, rather than for everything pushed with it.
- The changelog check asks whether an entry was added, not whether `CHANGELOG.md` was touched; a rewritten link line used to satisfy it.
- The changelog check reads the waiver after fetching the base commit, not before; in a shallow clone it missed `[no changelog]` and failed the build anyway.
- The changelog check no longer exempts documentation; a typo says `[no changelog]` like any other change that ships nothing observable.
- The acceptance suite refuses a run as root, rather than passing every check but the one that needs the broker.

[Unreleased]: https://github.com/sokar-ai/sokar-omp/commits/main
