#!/usr/bin/env python3
"""
Says whether Oh My Pi has moved on from the version this module pins.

The first step of the update pipeline, and the one that differs most between agents: what
"the current version" is depends on how the vendor publishes. Oh My Pi publishes GitHub
releases, so this reads the latest one; another agent reads a registry or a pointer file.
What every agent's copy of this script shares is the OUTPUT, so the job that calls it does
not have to know which:

    pinned=18.1.13       what this module installs today
    upstream=18.1.16     the newest release upstream
    update=yes           'yes' when newer, 'no' when the same, 'rollback' when older
    source=newest release  which pointer 'upstream' was read from
    major=same           'moved' when the major version changed, which nothing may decide alone

The same lines are appended to $GITHUB_OUTPUT when it is set.

Exit codes:

    0   the question was answered, whatever the answer is
    2   it could not be answered

'update=yes' is deliberately NOT a non-zero exit. A scheduled job that goes red every time
there is something to do teaches whoever watches it to ignore red.

THERE IS NO STABLE CHANNEL HERE, unlike Claude Code, which publishes 'stable' beside
'latest'. Upstream's newest release is the only thing on offer, so this follows it and the
verification below the detect step is what has to carry the weight instead.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

RELEASES = "https://api.github.com/repos/can1357/oh-my-pi/releases/latest"

VERSION = re.compile(r"^\d+\.\d+\.\d+$")

POM = Path(__file__).resolve().parents[1] / "pom.xml"


def pinned() -> str:
    """Reads the version this module installs, from the one place it is written by hand."""
    found = re.search(r"<agent\.cli\.version>([^<]+)</agent\.cli\.version>",
                      POM.read_text(encoding="utf-8"))
    if not found:
        unanswerable(f"{POM} declares no agent.cli.version")
    version = found.group(1).strip()
    if not VERSION.match(version):
        unanswerable(f"{POM} pins {version!r}, which is not a version")
    return version


def upstream() -> str:
    """
    Reads the tag of the newest release, without its leading 'v'.

    :return: A bare version, as the pom property holds it.
    """
    request = urllib.request.Request(RELEASES, headers={"Accept": "application/vnd.github+json"})
    # Unauthenticated GitHub allows 60 requests an hour per address, which a shared runner can
    # exhaust for reasons that have nothing to do with this repository.
    token = os.environ.get("GITHUB_TOKEN")
    if token:
        request.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            tag = json.load(response).get("tag_name") or ""
    except urllib.error.HTTPError as failure:
        unanswerable(f"{RELEASES}: HTTP {failure.code}")
    except Exception as failure:  # noqa: BLE001 - anything at all here means "do not know"
        unanswerable(f"{RELEASES}: {failure}")
    version = tag[1:] if tag.startswith("v") else tag
    if not VERSION.match(version):
        unanswerable(f"the latest release is tagged {tag!r}, which is not a version")
    return version


def unanswerable(reason: str):
    """
    Stops with the code that means "could not tell", which is not the code for "up to date".

    An update job that reads a failed fetch as "nothing new" is quietly switched off, and looks
    exactly like one that is working.
    """
    print(f"could not tell whether there is a new version - {reason}", file=sys.stderr)
    sys.exit(2)


def order(version: str) -> tuple[int, ...]:
    """
    Returns a version as something that sorts the way versions do.

    As text, 2.1.9 sorts after 2.1.10, and a gate that calls a legitimate update a rollback gets
    switched off. VERSION has already refused anything but three numbers, so this is the whole
    comparison rather than a parser for one.

    :param version: A version that matched VERSION.
    :return: Its numbers, in order.
    """
    return tuple(int(part) for part in version.split("."))


def verdict(have: str, there: str, named: bool) -> str:
    """
    Decides what the job does about the two versions.

    Older than the pin is a rollback, which automation may not choose: a withdrawn release and
    two pointers that disagree look the same from here. Only a version a person named may take
    the pin backwards.

    :param have: The pinned version.
    :param there: The version upstream offers.
    :param named: Whether a person named that version rather than a pointer.
    :return: 'yes', 'no' or 'rollback'.
    """
    if order(there) > order(have):
        return "yes"
    if order(there) == order(have):
        return "no"
    return "yes" if named else "rollback"


def main() -> int:
    parser = argparse.ArgumentParser(description="Compares the pinned Oh My Pi version against "
                                                 "the newest release upstream.")
    parser.add_argument("--upstream", default=None, metavar="VERSION",
                        help="answer as if this were the newest release, and ask nothing")
    args = parser.parse_args()

    have = pinned()
    there = args.upstream or upstream()
    if args.upstream and not VERSION.match(there):
        print(f"{there!r} is not a version", file=sys.stderr)
        return 1

    answer = {
        "pinned": have,
        "upstream": there,
        "source": "a version named by hand" if args.upstream else "newest release",
        "update": verdict(have, there, bool(args.upstream)),
        # A major move changes flags and configuration by definition; answered here rather than
        # by the caller, so every agent's copy answers it the same way.
        "major": "moved" if there.split(".")[0] != have.split(".")[0] else "same",
    }
    for key, value in answer.items():
        print(f"{key}={value}")

    output = os.environ.get("GITHUB_OUTPUT")
    if output:
        with open(output, "a", encoding="utf-8") as handle:
            for key, value in answer.items():
                handle.write(f"{key}={value}\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
