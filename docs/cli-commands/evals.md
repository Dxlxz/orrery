---
description: "Use the orrery-cloud evals command to manage, run, and report Orrery Context Evals from the terminal or from CI."
---

import FeatureAvailability from '@site/src/components/FeatureAvailability';

# orrery-cloud evals

<FeatureAvailability saasOnly stage="private-beta" />

The `evals` command manages **Context Evals** — question-and-answer checks that measure whether
Orrery answers questions about your metadata correctly. An eval pairs a question with one or more
pass conditions; running it records a verdict you can track over time or gate a CI job on.

Context Evals are in private beta, so the command surface can change between releases.

## Installation

The command lives in the `orrery-cloud` package only — it is not part of the open source
`orrery` CLI. It needs Python 3.10 or later.

```shell
pip install 'orrery-cloud[orrery-evals]>=2.1.4rc1'
```

Install it into a virtualenv or with `pipx`: the package pins its own `orrery` version, so a
shared environment can end up with a different `orrery` CLI than the one you had. Connection details
come from `~/.orreryenv` (written by `orrery init`) or from `ORRERY_GMS_URL` and
`ORRERY_GMS_TOKEN`.

## Quick Start

```shell
# Create or update evals from a definition file
orrery-cloud evals upsert -f evals.yml

# List what exists
orrery-cloud evals list

# Run one eval and wait for the verdict
orrery-cloud evals run urn:li:eval:my-eval

# Run everything for one agent and fail the shell on any failed eval
orrery-cloud evals run --all --agent-urn urn:li:aiAgent:my-agent --fail-on-fail
```

`upsert` is the default subcommand, so `orrery-cloud evals -f evals.yml` works too. Run any
subcommand with `--help` for its full option list.

## Defining Evals

`upsert` reads a YAML or JSON file containing either one eval object or a list of them. An entry with
a `urn` updates that eval; an entry without one creates a new eval.

```yaml
- name: Revenue table owner
  description: Ask Orrery who owns the revenue table.
  question: Who owns the revenue table?
  evalType: METADATA
  origin: CONTEXT_HUB
  executor:
    type: NATIVE
  conditions:
    - type: LLM_JUDGE
      llmJudge:
        guidelines: The answer names the owning team and links the dataset.
    - type: ASSET_REFERENCE
      assetReference:
        mustReference:
          - urn:li:dataset:(urn:li:dataPlatform:snowflake,my_db.my_schema.revenue,PROD)
        mustNotReference:
          - urn:li:dataset:(urn:li:dataPlatform:snowflake,my_db.my_schema.revenue_staging,PROD)
```

| Field             | Required | Description                                                      |
| ----------------- | -------- | ---------------------------------------------------------------- |
| `name`            | Yes      | Display name.                                                    |
| `question`        | Yes      | The question put to Orrery.                                     |
| `conditions`      | Yes      | At least one pass condition.                                     |
| `executor`        | Yes      | `NATIVE` for Orrery to answer, `EXTERNAL` for your own harness. |
| `origin`          | Yes      | `CONTEXT_HUB` or `CUSTOM_AGENT`.                                 |
| `evalType`        | No       | `METADATA` or `SQL`.                                             |
| `description`     | No       | What the eval covers.                                            |
| `referenceOutput` | No       | An example of a good answer, used as judging context.            |
| `agentUrn`        | No       | The agent the eval belongs to.                                   |
| `urn`             | No       | Present means update, absent means create.                       |

A condition is either an `LLM_JUDGE` — requiring `llmJudge.guidelines` — or an `ASSET_REFERENCE`,
requiring at least one Orrery URN across `assetReference.mustReference` and
`assetReference.mustNotReference`. `origin` and `agentUrn` are fixed once an eval is created, so an
exported definition can be re-applied unchanged.

## Commands

### list

```shell
orrery-cloud evals list
orrery-cloud evals list --agent-urn urn:li:aiAgent:my-agent --eval-type METADATA
orrery-cloud evals list --start 20 --limit 20
```

Filter with `--agent-urn` or `--base-agent-only` (not both), `--eval-type`, `--eval-executor`, and
`--query`. Pages are `--limit` results (default 10) from the zero-based `--start` offset.

### get

```shell
orrery-cloud evals get urn:li:eval:my-eval
orrery-cloud evals get urn:li:eval:my-eval --history 5
orrery-cloud evals get urn:li:eval:my-eval --format upsert > my-eval.yml
```

`--format upsert` drops the read-only run fields, so the output can be edited and fed back to
`upsert`.

### upsert

```shell
orrery-cloud evals upsert -f evals.yml
orrery-cloud evals upsert -f evals.yml --dry-run
```

`--dry-run` validates the file and reports create-versus-update per entry without writing. If one
entry of a multi-eval file fails, the CLI prints the entries that already succeeded with their URNs
and exits non-zero — reuse those URNs when retrying, since a repeated create makes a duplicate eval.

### delete

```shell
orrery-cloud evals delete urn:li:eval:my-eval
```

### run

```shell
orrery-cloud evals run urn:li:eval:my-eval urn:li:eval:my-other-eval
orrery-cloud evals run --all --agent-urn urn:li:aiAgent:my-agent
orrery-cloud evals run --all --wait 600 --fail-on-fail
```

Pass either URNs or `--all`, not both, and wait up to `--wait` seconds (default 300) for every
selected eval to reach a terminal event.

With `--fail-on-fail`, the command exits `1` when any selected eval ends up **FAIL**, **ERROR**,
**stale** (queued or running for more than 15 minutes), or **incomplete** (no terminal event within
`--wait`). Without the flag all four still exit `0`, so CI jobs should always pass it.

Other things worth knowing:

- `--all` without `--agent-urn` covers only the Ask Orrery base agent, whereas `list` without a
  filter spans every agent, so the two can return different counts.
- A run selects at most 1000 evals. A larger `--all` selection is refused before anything is queued.
- `--eval-executor EXTERNAL` is rejected — starting a run always queues native execution, which would
  answer the eval itself and race your harness. Run those in your harness and use `report`.

### report

Submit an answer produced outside Orrery, using the run ID returned by `run`.

```shell
orrery-cloud evals report urn:li:eval:my-eval \
  --run-id "$RUN_ID" \
  --answer "The revenue table is owned by the analytics team."

# Read a long answer from stdin
my-harness answer | orrery-cloud evals report urn:li:eval:my-eval --run-id "$RUN_ID" --answer -
```

Omit `--type` to let Orrery judge the submitted answer. To report your own verdict, pass `--type`
(`PASS`, `FAIL`, or `ERROR`) — it is required alongside the verdict options `--condition-results`,
`--judge-model`, `--judge-reasoning`, and `--error`. `--agent-model`, `--external-client`, and
`--session-id` record where the answer came from. Reporting twice against a run ID that is already
terminal is deduplicated rather than duplicated.

### history

```shell
orrery-cloud evals history urn:li:eval:my-eval
orrery-cloud evals history urn:li:eval:my-eval --limit 50
```

Returns `--limit` run events (default 10). Add `--include-proposal-runs`, or `--action-request-urn`
for one change proposal, but not both.

## CI Example

```shell
pip install 'orrery-cloud[orrery-evals]>=2.1.4rc1'

export ORRERY_GMS_URL=https://your-instance.orrery.dev/gms
export ORRERY_GMS_TOKEN=your-token

orrery-cloud evals upsert -f evals.yml
orrery-cloud evals run --all --agent-urn "$AGENT_URN" --wait 900 --fail-on-fail
```

The run exits non-zero on any failed eval, which fails the job.
