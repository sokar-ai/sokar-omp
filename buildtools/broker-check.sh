#!/usr/bin/env bash
#
# Tier 2 minus an account: does the file-routed brokering path work end to end?
#
# The vault holds a deliberately fake OpenRouter key, so the provider answers 401. That 401 is
# the proof, not the failure: it is the PROVIDER's rejection of a bad key rather than the
# proxy's rejection of the token, so the relay carried the request, the broker accepted the
# phantom token and swapped the real credential in on the way out. Only a 200 needs an account.
#
# OpenRouter words a bad bearer as "Missing Authentication header" and no header at all as
# "No cookie auth credentials found", so the two are told apart by the message, not the code.
#
# Needs sokar and its agent installed, podman, and 'sokar setup' already run. It uses a vault of
# its own and removes everything it created.
set -uo pipefail

WORK="$(mktemp -d)"
export SOKAR_VAULT="$WORK/vault.bin"
# Not a real key, and it must not look like one: this string is searched for in the container.
FAKE_CREDENTIAL="sk-or-omp-check-not-a-real-key"
CONTAINER=""
FAILURES=0

pass() { printf '  PASS  %s\n' "$1"; }
fail() { printf '  FAIL  %s\n' "$1"; FAILURES=$((FAILURES + 1)); }
info() { printf '        %s\n' "$1"; }

cleanup() {
    [ -n "$CONTAINER" ] && podman rm -f "$CONTAINER" >/dev/null 2>&1
    sokar vault unlock --forget >/dev/null 2>&1
    podman rmi -f sokar/omp-check >/dev/null 2>&1
    # State directories outlive the container - nothing removes the files - and they hold this
    # run's dead token.
    rm -rf "$WORK" "${XDG_RUNTIME_DIR:-/run/user/$(id -u)}/sokar/sokar-omp-check-"*
    rm -rf "${XDG_DATA_HOME:-$HOME/.local/share}/sokar/build/omp-check"
    rm -rf "${XDG_DATA_HOME:-$HOME/.local/share}/sokar/mirrors/omp-check.git"
    :
}
trap cleanup EXIT

command -v podman >/dev/null || { echo "podman is not installed"; exit 2; }

# A vault of this run's own, never the operator's. Reading theirs would make the result depend
# on what they happen to have stored, and a real key would quietly test something else.
sokar vault unlock --passphrase-command "printf omp-check" >/dev/null 2>&1
printf '%s' "$FAKE_CREDENTIAL" | sokar vault put openrouter --type api-key >/dev/null 2>&1 \
    || { echo "could not prepare this run's own vault at $SOKAR_VAULT"; exit 2; }

cat > "$WORK/project.yml" <<'EOF'
project:
  name: "omp-check"
  security_class: "guarded"
image:
  base_image: "ubuntu:24.04"
egress:
  sets: [git-hosting]
EOF

echo "== the brokering path, with a fake credential =="

START="$WORK/start.log"
# No --raw: it turns the machine-readable flags off, and omp then starts its terminal interface
# and is killed by the hangup it gets instead of answering.
# --clearance deny: a check must not raise a prompt on somebody's desktop and then wait for it.
(cd "$WORK" && timeout 1200 sokar task start --repository omp-check --agent omp --model z-ai/glm-4.6 \
    -P "reply with the single word SOKARLIVE" --clearance deny > "$START" 2>&1)
CONTAINER="$(grep '^container ' "$START" | awk '{print $2}')"
STATE="$(grep '^sidecar ' "$START" | awk '{print $2}' | xargs dirname 2>/dev/null)"
info "container ${CONTAINER:-none}, state ${STATE:-none}"

if [ -z "$CONTAINER" ]; then
    fail "the task did not start"
    grep -v SLF4J "$START" | tail -20
    exit 1
fi

MODELS=/home/agent/.omp/agent/models.yml
if podman exec "$CONTAINER" test -f "$MODELS" 2>/dev/null; then
    pass "the agent's own setup file is in the container ($MODELS)"
    CONTENT="$(podman exec "$CONTAINER" cat "$MODELS" 2>/dev/null)"
    echo "$CONTENT" | sed 's/^/        | /'
    if echo "$CONTENT" | grep -q 'sokar_pt_'; then
        pass "it carries a task-scoped token"
    else
        fail "it carries no phantom token"
    fi
    PERMS="$(podman exec "$CONTAINER" stat -c '%a %U' "$MODELS" 2>/dev/null)"
    case "$PERMS" in
        600*) pass "it is readable only by the agent user ($PERMS)" ;;
        *)    fail "it holds a token but is $PERMS" ;;
    esac
else
    fail "no setup file in the container - omp was never pointed at the broker"
fi

if podman exec "$CONTAINER" sh -c "env; cat $MODELS 2>/dev/null" 2>/dev/null \
        | grep -q "$FAKE_CREDENTIAL"; then
    fail "the real credential reached the container"
else
    pass "the real credential is not in the container's environment or its setup file"
fi

if [ -n "$STATE" ] && [ -f "$STATE/vault.log" ]; then
    REQUESTS="$(grep '^request ' "$STATE/vault.log" 2>/dev/null)"
    if [ -n "$REQUESTS" ]; then
        pass "omp's requests went through the broker"
        echo "$REQUESTS" | sort -u | head -4 | sed 's/^/        | /'
    else
        fail "nothing reached the broker - omp did not use the endpoint it was given"
        info "relay.log:"
        tail -5 "$STATE/relay.log" 2>/dev/null | sed 's/^/        | /'
    fi
else
    fail "no vault.log at ${STATE:-<no state directory>}"
fi

echo
echo "== $FAILURES check(s) failed =="
[ "$FAILURES" -eq 0 ]
