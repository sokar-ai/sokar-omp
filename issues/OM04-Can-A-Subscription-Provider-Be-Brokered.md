# OM04 — Can any of this agent's subscription providers be brokered

**Priority:** 3
**Opened:** 2026-09-12
**Source:** handed over from Sokar requirement **A05**, 2026-09-12. Everything else in that
requirement is built, met and measured - including the last criterion, with a real credential.
This is the question it left open, and it is the only reason this agent was worth building beyond
being a third one.
**Depends on:** Sokar requirement **A01**, which owns the vault and the host-side browser sign-in.

## What is open

This agent advertises 60+ providers, including the kind A01 was chosen to exercise: a subscription
reached through a browser sign-in, GitHub Copilot and Cursor among them.

**The two that matter authenticate into omp's own store.** `/login` writes into its SQLite
database, which is not a shape Sokar can stand in front of. So the open question is not "does it
support Copilot" - it is whether *any* of its subscription providers can be reached through a
broker at all, or whether all of them route through the vendor's own service with a credential the
container would have to hold.

If the answer is none, that is a finding rather than a failure: it would mean a forge subscription
needs a different mechanism than an endpoint and a token, and A01's assumption moves rather than
being met.

## What would close it

One provider of that kind, tried for real. Either a request leaves through the broker with a
task-scoped credential, or it does not and this file records which part refuses - the sign-in, the
store, or the endpoint.

## Why it is priority 3

The agent is shipped and verified against a provider that works. Nothing depends on this answer
except A01, which is not being built now.
