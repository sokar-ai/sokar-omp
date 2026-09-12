#!/usr/bin/env python3
"""
Moves this module to a new Oh My Pi version, doing exactly what a person would.

    update.py <version> [--dry-run]

Three things are written by hand today and all three are written here:

    pom.xml                                  <agent.cli.version>
    src/main/resources/agent/omp.yaml        the SHA-256 of the linux-x64 binary
    CHANGELOG.md                             what moved, under Unreleased

Everything else that names the version - the definition's install section, the download URL,
the package description in both the deb and the rpm - is FILTERED from that property, so it
cannot be left behind.

The digest is never taken from the caller. It is read from the SHA256SUMS.txt upstream
publishes beside every release, so the digest recorded is the publisher's rather than one
computed here, and a release without a linux-x64 build stops rather than becoming a package
whose image build fails at 'sha256sum -c'.

THE MODULE'S OWN VERSION IS NOT BUMPED WHILE IT IS A SNAPSHOT. 'agent.snapshot.run' - the CI
run number - already makes every build a strictly newer package than the last, which is what
apt and dnf sort on, and 1.0.1-SNAPSHOT would invent a successor to a 1.0.0 that was never
released. Once a release exists, a CLI move is a patch bump of the module, and this does that.

Exit codes:

    0   the working tree now pins the requested version
    1   the request cannot be carried out - no such release, no linux-x64 build, nothing to write
    2   upstream could not be read, which is not the same as "no such version"
"""
from __future__ import annotations

import argparse
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

SUMS = "https://github.com/can1357/oh-my-pi/releases/download/v{version}/SHA256SUMS.txt"

# The platform the definition installs; a second one would be a package change, not a version one.
ASSET = "omp-linux-x64"

VERSION = re.compile(r"^\d+\.\d+\.\d+$")

# What upstream must publish for the asset: 64 lowercase hex characters and nothing else.
DIGEST = re.compile(r"[0-9a-f]{64}")

ROOT = Path(__file__).resolve().parents[1]
POM = ROOT / "pom.xml"
DEFINITION = ROOT / "src" / "main" / "resources" / "agent" / "omp.yaml"
CHANGELOG = ROOT / "CHANGELOG.md"

ENTRY = re.compile(r"^- Oh My Pi pinned to (\S+?)\.?(?: \(was (\S+?)\.?\))?\.$", re.M)


def digest_in(body: str, version: str) -> str:
    """
    Picks our asset's digest out of a published SHA256SUMS.txt, refusing anything malformed.

    Kept apart from the download so the rules below can be exercised without the network.

    :param body: Content of the published file.
    :param version: The release it belongs to, for the message.
    :return: The 64-character digest.
    """
    # Every line for our asset, not the first: a file naming it twice is malformed, and taking
    # whichever came first would pin whatever an attacker appended.
    found = [parts[0] for parts in (line.split() for line in body.splitlines())
             if len(parts) == 2 and parts[1] == ASSET]
    if not found:
        print(f"{version} publishes no {ASSET} in its SHA256SUMS.txt", file=sys.stderr)
        sys.exit(1)
    if len(set(found)) > 1:
        print(f"{version} publishes {len(found)} different digests for {ASSET}", file=sys.stderr)
        sys.exit(1)
    digest = found[0]
    # Checked here rather than by the pin check after the build has been prepared: a value that is
    # not a digest must never reach a file.
    if not DIGEST.fullmatch(digest):
        print(f"{version} publishes '{digest}' for {ASSET}, which is not a SHA-256 digest",
              file=sys.stderr)
        sys.exit(1)
    return digest


def published_digest(version: str) -> str:
    """
    Reads the digest upstream published for the linux-x64 asset of one release.

    :param version: The release to look up.
    :return: The 64-character digest.
    """
    url = SUMS.format(version=version)
    try:
        with urllib.request.urlopen(url, timeout=60) as response:
            body = response.read().decode("utf-8")
    except urllib.error.HTTPError as failure:
        if failure.code == 404:
            print(f"there is no release {version} - {url} answers 404", file=sys.stderr)
            sys.exit(1)
        unreadable(f"{url}: HTTP {failure.code}")
    except Exception as failure:  # noqa: BLE001
        unreadable(f"{url}: {failure}")

    return digest_in(body, version)


def unreadable(reason: str):
    """Stops with the code that means "could not ask", distinct from "the answer is no"."""
    print(f"could not read the published digests - {reason}", file=sys.stderr)
    print("This is not the same as 'there is no such version'.", file=sys.stderr)
    sys.exit(2)


def replace_once(text: str, pattern: re.Pattern, replacement, what: str) -> str:
    """
    Substitutes exactly one occurrence, refusing zero and refusing several.

    A bot that writes nothing and reports success is the failure this pipeline exists to avoid,
    and one that writes a second occurrence it did not know about is worse.
    """
    rewritten, count = pattern.subn(replacement, text)
    if count != 1:
        print(f"expected exactly one {what}, found {count} - refusing to guess", file=sys.stderr)
        sys.exit(1)
    return rewritten


def note_the_change(text: str, version: str, was: str) -> str:
    """
    Records the new version under Unreleased, replacing this script's own earlier entry.

    A weekly job would otherwise add a line every time it ran. One line survives, and it keeps
    the version of the LAST RELEASE as its "was" - the reader wants the net move since something
    shipped, not the last hop.
    """
    existing = ENTRY.search(text)
    since = existing.group(2) if existing and existing.group(2) else was
    line = f"- Oh My Pi pinned to {version} (was {since})."

    if existing:
        return text[:existing.start()] + line + text[existing.end():]

    lines = text.split("\n")
    try:
        at = next(i for i, one in enumerate(lines) if one.rstrip() == "## [Unreleased]")
    except StopIteration:
        print("CHANGELOG.md has no '## [Unreleased]' heading to write under", file=sys.stderr)
        sys.exit(1)

    # Only this release's own block: a Changed heading under an older release is not ours.
    ends = next((i for i in range(at + 1, len(lines)) if lines[i].startswith("## ")), len(lines))
    heading = next((i for i in range(at, ends) if lines[i].rstrip() == "### Changed"), None)
    if heading is None:
        lines[at + 1:at + 1] = ["", "### Changed", "", line]
    else:
        first = next((i for i in range(heading + 1, ends) if lines[i].startswith("- ")), None)
        lines.insert(first if first is not None else heading + 2, line)
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description="Pins a new Oh My Pi version.")
    parser.add_argument("version", help="the release to pin, e.g. 18.1.16")
    parser.add_argument("--dry-run", action="store_true",
                        help="say what would change and write nothing")
    args = parser.parse_args()

    if not VERSION.match(args.version):
        print(f"{args.version!r} is not a version", file=sys.stderr)
        return 1

    digest = published_digest(args.version)

    pom = POM.read_text(encoding="utf-8")
    was = re.search(r"<agent\.cli\.version>([^<]+)</agent\.cli\.version>", pom)
    if not was:
        print(f"{POM} declares no agent.cli.version", file=sys.stderr)
        return 1
    if was.group(1).strip() == args.version:
        print(f"already pinned to {args.version} - nothing to do")
        return 0

    pom = replace_once(pom, re.compile(r"<agent\.cli\.version>[^<]+</agent\.cli\.version>"),
                       f"<agent.cli.version>{args.version}</agent.cli.version>",
                       "agent.cli.version in pom.xml")

    definition = replace_once(DEFINITION.read_text(encoding="utf-8"),
                              re.compile(r'(\bsha256:\s*")[0-9a-f]{64}(")'),
                              lambda m: f"{m.group(1)}{digest}{m.group(2)}",
                              "pinned sha256 in omp.yaml")

    module = re.search(r"<artifactId>sokar-agent-omp</artifactId>\s*<version>([^<]+)</version>", pom)
    bumped = None
    if module and not module.group(1).endswith("-SNAPSHOT"):
        major, minor, patch = module.group(1).split(".")
        bumped = f"{major}.{minor}.{int(patch) + 1}"
        pom = replace_once(
            pom,
            re.compile(r"(<artifactId>sokar-agent-omp</artifactId>\s*<version>)"
                       r"[^<]+(</version>)"),
            lambda m: f"{m.group(1)}{bumped}{m.group(2)}", "the module's own version")

    changelog = note_the_change(CHANGELOG.read_text(encoding="utf-8"),
                                args.version, was.group(1).strip())

    print(f"  oh my pi      {was.group(1).strip()} -> {args.version}")
    print(f"  sha256        {digest}  ({ASSET}, from the published SHA256SUMS.txt)")
    if bumped:
        print(f"  this module   {module.group(1)} -> {bumped}")
    elif module:
        print(f"  this module   {module.group(1)}, unchanged - the CI run number already orders "
              f"snapshot packages")
    print(f"  changelog     {ENTRY.search(changelog).group(0)}")

    if args.dry_run:
        print("\n--dry-run: nothing written")
        return 0

    POM.write_text(pom, encoding="utf-8")
    DEFINITION.write_text(definition, encoding="utf-8")
    CHANGELOG.write_text(changelog, encoding="utf-8")
    print(f"\nwritten. Review the diff, then: Pin Oh My Pi {args.version}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
