# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Headings name this package's version; Oh My Pi's version is what the package installs and
appears as an entry rather than a heading. One sentence per change - `git log` has the detail.

## [Unreleased]

### Added

- The Oh My Pi adapter: definition, credential handling, headless commands, log formatting.
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

- The models file is written only when there is a token to put in it, not merely an endpoint.
- The changelog check asks whether an entry was added, not whether `CHANGELOG.md` was touched; a rewritten link line used to satisfy it.
- The changelog check reads the waiver after fetching the base commit, not before; in a shallow clone it missed `[no changelog]` and failed the build anyway.
- The changelog check no longer exempts documentation; a typo says `[no changelog]` like any other change that ships nothing observable.
- The acceptance suite refuses a run as root, rather than passing every check but the one that needs the broker.

[Unreleased]: https://github.com/sokar-ai/sokar-omp/commits/main
