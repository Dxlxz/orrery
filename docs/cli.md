---
description: "Install and use the Orrery CLI to ingest metadata, manage entities, run quickstart deployments, and perform common operations."
# Display to h4 headings
# toc_min_heading_level: 2
toc_max_heading_level: 4
---

# Orrery CLI

Orrery comes with a friendly cli called `orrery` that allows you to perform a lot of common operations using just the command line. [Orrery](https://orrery.dev) maintains the [pypi package](https://pypi.org/project/orrery/) for `orrery`.

## Installation

### Using Homebrew (macOS / Linux)

The simplest way to install the Orrery CLI on macOS or Linux:

```shell
brew install Dxlxz/tap/orrery
orrery version
orrery init
# authenticate your orrery CLI with your orrery instance
```

Homebrew manages an isolated Python environment for `orrery`, so there's no venv to activate and no PATH conflicts with system Python. To upgrade later, run `brew upgrade orrery`.

**Note:** The Homebrew formula installs the **core CLI only**. To add ingestion connectors (Snowflake, BigQuery, Looker, etc.), install them into the brew-managed environment:

```shell
"$(brew --prefix orrery)/libexec/bin/pip" install 'orrery[snowflake,bigquery]'
```

The full list of available connector extras is shown in the [Installing Plugins](#installing-plugins) section below.

### Using pip

We recommend Python virtual environments (venv-s) to namespace pip modules. Here's an example setup:

```shell
python3 -m venv venv             # create the environment
source venv/bin/activate         # activate the environment
```

**_NOTE:_** If you install `orrery` in a virtual environment, that same virtual environment must be re-activated each time a shell window or session is created.

Once inside the virtual environment, install `orrery` using the following commands

```shell
# Requires Python 3.10+
python3 -m pip install --upgrade pip wheel setuptools
python3 -m pip install --upgrade orrery
# validate that the install was successful
orrery version
# If you see "command not found", try running this instead: python3 -m orrery version
orrery init
# authenticate your orrery CLI with your orrery instance
```

If you run into an error, try checking the [_common setup issues_](../metadata-ingestion/developing.md#common-setup-issues).

Other installation options such as installation from source and running the cli inside a container are available further below in the guide [here](#alternate-installation-options).

## Profiles

Use named profiles when you work with more than one Orrery instance. Each named connection lives
in `~/.orrery/profiles/<name>/.orreryenv`. Your existing `~/.orreryenv` remains the reserved
`default` profile. Profiles created by overriding `HOME` under that directory already work.
Telemetry, browser discovery, recipes, and caches continue to use your normal home directory.

```bash
orrery init --profile local --host http://localhost:8080 --username orrery --password orrery
orrery init --profile acme --host https://acme.example.com/gms --token "<token>" --description "Acme instance"
orrery search orders --profile acme
orrery --profile acme profile current --format json
```

The global `--profile` flag works before or after the subcommand. Names match
`[a-z0-9][a-z0-9._-]{0,62}`. Initialization supports token, username/password, SSO, OAuth, and support
authentication. It creates or refreshes a profile without changing the active connection.

### Selection order

The first applicable source wins:

1. `--profile <name>` for one invocation.
2. `ORRERY_PROFILE` for a shell, agent session, directory environment, or CI job.
3. Existing GMS connection environment variables: `ORRERY_GMS_URL`, `ORRERY_GMS_TOKEN`, and the existing host, port, and protocol variables.
4. `~/.orrery/current-profile` for the machine default.
5. `~/.orreryenv`, the `default` profile.

An unknown profile is an error, including a stale active pointer. The CLI lists the profiles and
servers available and never falls back to another instance. If an explicit profile conflicts with
the environment's GMS URL, the profile wins and stderr identifies both servers. `--profile` with
`ORRERY_SKIP_CONFIG=true` is an error; under `ORRERY_SKIP_CONFIG=true` an inherited `ORRERY_PROFILE` is ignored. Without an explicit profile, the skip-config behavior
and connection environment precedence stay unchanged. Environment-configured OAuth
(`ORRERY_AUTH_TYPE`) keeps its precedence over static tokens, but is not applied to an explicit
profile while `ORRERY_GMS_URL` names another server, because its tokens belong to that server.

A profile selects a server together with its credential. Unlike `AWS_PROFILE`, which exported
AWS credentials can still override, `ORRERY_PROFILE` deliberately outranks `ORRERY_GMS_URL` and
`ORRERY_GMS_TOKEN`, so a token exported for one instance is never sent to another. Recipe
references to `${ORRERY_GMS_URL}` and `${ORRERY_GMS_TOKEN}` follow the same rule.

The active pointer (`profile use`) is a convenience for interactive use and is shared by every
shell and agent on the machine. To switch one shell only, run
`eval "$(orrery profile use --shell acme)"`, which sets `ORRERY_PROFILE` there. Scripts and
agents should pin a profile with `--profile` or `ORRERY_PROFILE` rather than rely on the pointer;
`profile current` and the write-target line say when the pointer made the choice.

`orrery profile current --format json` reports `name`, `server`, `source`, and `token_expires`
(JWT expiry in epoch seconds, or null). Check the server before a write. Write commands announce
named targets on stderr. `orrery ingest run` writes through the recipe's sink, so it announces
that destination instead. A recipe whose `orrery-rest` sink names a different server than an
explicit `--profile` is refused before anything runs. Under `ORRERY_PROFILE` or the active pointer
the recipe's sink wins with a warning naming both servers. A recipe without a sink writes to the
selected profile. The Python SDK uses the same rules through `load_client_config(profile=...)`,
including clients that load their default connection through `OrreryGraph`.

### Manage connections

| Command                                              | Behavior                                                                                                                                                                                                                      |
| ---------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `orrery profile list [--format json]`               | Lists names, servers, token status, descriptions, and an active marker. Never displays tokens.                                                                                                                                |
| `orrery profile current [--format json]`            | Reports the selected profile and why it was chosen.                                                                                                                                                                           |
| `orrery profile use acme [--shell]`                 | Changes the machine default. Requires `--yes` without a terminal. `use default` removes the active pointer. `--shell` instead prints `export ORRERY_PROFILE=acme` for the current shell.                                     |
| `orrery profile save acme [--from local] [--force]` | Copies a stored connection, defaulting to `~/.orreryenv`. Ignores environment credentials and refuses an existing destination without `--force`. An OAuth session is not copied; run `init --oauth` on the copy for its own. |
| `orrery profile show acme`                          | Displays server, expiry, auth mode, and path with the token redacted. `--show-token` reveals it explicitly.                                                                                                                   |
| `orrery profile remove acme`                        | Deletes the profile directory after confirmation; if it was active, `default` becomes active. Requires `--yes` without a terminal and refuses `default`.                                                                      |
| `orrery profile rename acme acme-staging`           | Moves the directory and updates the active pointer if needed.                                                                                                                                                                 |
| `orrery profile doctor [--fix]`                     | Reports expired tokens, legacy keys, loose permissions, and stray files. Only `--fix` applies repairs. Never deletes stray files.                                                                                             |

Profiles store one credential each, in plaintext YAML with directory permissions `0700` and file
permissions `0600`. OAuth refresh updates the profile it read from. OAuth sessions are per
profile: two profiles for the same identity provider each need their own `init --oauth` login. New descriptions live under
the top-level `profile.description` key, preserving compatibility with older configuration readers.

### Initialize safely from scripts

Without `--profile`, init chooses its destination from `ORRERY_PROFILE`, then the machine default,
then `default`. `ORRERY_GMS_URL` supplies the new host, not a destination profile. Without a terminal,
init refuses to change an existing profile to another server and suggests creating a separate
profile. Refreshing credentials against the same server still succeeds. Pass `--force` only when
you intend to replace the connection. Token-duration defaults stay `ONE_MONTH` for localhost and
`ONE_HOUR` for remote hosts.

### Orrery CLI connection profiles

- Never run `orrery init` without `--profile <name>`. Name the profile after the instance (`acme`, `acme-staging`).
- Never run `orrery profile use`; it changes the default for the user's other shells.
- Pass `--profile <name>` on every command, unless the session already sets `ORRERY_PROFILE`.
- Before any write (`put`, `delete`, `ingest`, upserts), run `orrery profile current --format json` and confirm the server is the one the user asked for.
- If a token has expired, re-run `orrery init --profile <same name>` with the same host. Do not create a new profile.

## Starter Commands

The `orrery` cli allows you to do many things, such as quick-starting a Orrery docker instance locally, ingesting metadata from your sources into a Orrery server or a Orrery lite instance, as well as retrieving, modifying and exploring metadata.
Like most command line tools, `--help` is your best friend. Use it to discover the capabilities of the cli and the different commands and sub-commands that are supported.

```console
orrery --help
Usage: orrery [OPTIONS] COMMAND [ARGS]...

Options:
  --debug / --no-debug            Enable debug logging.
  --log-file FILE                 Enable debug logging.
  --debug-vars / --no-debug-vars  Show variable values in stack traces. Implies --debug. While we try to avoid
                                  printing sensitive information like passwords, this may still happen.
  --version                       Show the version and exit.
  -dl, --detect-memory-leaks      Run memory leak detection.
  --help                          Show this message and exit.

Commands:
  actions       <disabled due to missing dependencies>
  check         Helper commands for checking various aspects of Orrery.
  container     A group of commands to interact with containers in Orrery.
  dataproduct   A group of commands to interact with the DataProduct entity in Orrery.
  dataset       A group of commands to interact with the Dataset entity in Orrery.
  delete        Delete metadata from Orrery.
  docker        Helper commands for setting up and interacting with a local Orrery instance using Docker.
  exists        A group of commands to check existence of entities in Orrery.
  forms         A group of commands to interact with forms in Orrery.
  get           A group of commands to get metadata from Orrery.
  graphql       Execute GraphQL queries and mutations against Orrery.
  group         A group of commands to interact with the Group entity in Orrery.
  ingest        Ingest metadata into Orrery.
  init          Configure which orrery instance to connect to
  lite          A group of commands to work with a Orrery Lite instance
  migrate       Helper commands for migrating metadata within Orrery.
  properties    A group of commands to interact with structured properties in Orrery.
  put           A group of commands to put metadata in Orrery.
  state         Managed state stored in Orrery by stateful ingestion.
  telemetry     Toggle telemetry.
  timeline      Get timeline for an entity based on certain categories
  user          A group of commands to interact with the User entity in Orrery.
  version       Print version number and exit.
```

The following top-level commands listed below are here mainly to give the reader a high-level picture of what are the kinds of things you can accomplish with the cli.
We've ordered them roughly in the order we expect you to interact with these commands as you get deeper into the `orrery`-verse.

### docker

The `docker` command allows you to start up a local Orrery instance using `orrery docker quickstart`. You can also check if the docker cluster is healthy using `orrery docker check`.

### version

The `version` command allows you to get version number of CLI that you are using.

```console
orrery version
Orrery CLI version: 1.2.0.9
Models: bundled
Python version: 3.11.11 (main, Mar 17 2025, 21:33:08) [Clang 20.1.0 ]
```

You can pass `--include-server` flag to include server information too. This is helpful to share cli and server version at once for debugging purposes. This does require `orrery init` to be run before that so there is a server that CLI is able to connect to.

```console
orrery version --include-server
Orrery CLI version: 1.2.0.9
Models: bundled
Python version: 3.11.11 (main, Mar 17 2025, 21:33:08) [Clang 20.1.0 ]
Server config: {'models': {}, 'managedIngestion': {'defaultCliVersion': '1.2.0.9', 'enabled': True}, 'timeZone': 'GMT', 'datasetUrnNameCasing': False, 'orrery': {'serverEnv': 'cloud', 'serverType': 'prod'}, 'baseUrl': 'https://xyz.orrery.dev', 'patchCapable': True, 'versions': {'dxlxz/orrery': {'version': 'v0.3.14rc0', 'commit': '464d94926bb21a596f3d9d81164b272c74872ce7'}}, 'statefulIngestionCapable': True, 'remoteExecutorBackend': {'revision': 2}, 'supportsImpactAnalysis': True, 'telemetry': {'enabledCli': False, 'enabledMixpanel': False, 'enabledServer': True, 'enabledIngestion': True}, 'retention': 'true', 'noCode': 'true'}
```

### ingest

The `ingest` command allows you to ingest metadata from your sources using ingestion configuration files, which we call recipes.
Source specific crawlers are provided by plugins and might sometimes need additional extras to be installed. See [installing plugins](#installing-plugins) for more information.
[Removing Metadata from Orrery](./how/delete-metadata.md) contains detailed instructions about how you can use the ingest command to perform operations like rolling-back previously ingested metadata through the `rollback` sub-command and listing all runs that happened through `list-runs` sub-command.

```console
Usage: orrery [orrery-options] ingest [command-options]

Command Options:
  -c / --config             Config file in .toml or .yaml format
  -n / --dry-run            Perform a dry run of the ingestion, essentially skipping writing to sink
  --preview                 Perform limited ingestion from the source to the sink to get a quick preview
  --preview-workunits       The number of workunits to produce for preview
  --strict-warnings         If enabled, ingestion runs with warnings will yield a non-zero error code
  --test-source-connection  When set, ingestion will only test the source connection details from the recipe
  --no-progress             If enabled, mute intermediate progress ingestion reports
```

#### ingest --dry-run

The `--dry-run` option of the `ingest` command performs all of the ingestion steps, except writing to the sink. This is useful to validate that the
ingestion recipe is producing the desired metadata events before ingesting them into orrery.

```shell
# Dry run
orrery ingest -c ./examples/recipes/example_to_orrery_rest.dhub.yaml --dry-run
# Short-form
orrery ingest -c ./examples/recipes/example_to_orrery_rest.dhub.yaml -n
```

#### ingest list-source-runs

The `list-source-runs` option of the `ingest` command lists the previous runs, displaying their run ID, source name,
start time, status, and source URN. This command allows you to filter results using the --urn option for URN-based
filtering or the --source option to filter by source name (partial or complete matches are supported).

```shell
# List all ingestion runs
orrery ingest list-source-runs
# Filter runs by a source name containing "demo"
orrery ingest list-source-runs --source "demo"
```

#### ingest --preview

The `--preview` option of the `ingest` command performs all of the ingestion steps, but limits the processing to only the first 10 workunits produced by the source.
This option helps with quick end-to-end smoke testing of the ingestion recipe.

```shell
# Preview
orrery ingest -c ./examples/recipes/example_to_orrery_rest.dhub.yaml --preview
# Preview with dry-run
orrery ingest -c ./examples/recipes/example_to_orrery_rest.dhub.yaml -n --preview
```

By default `--preview` creates 10 workunits. But if you wish to try producing more workunits you can use another option `--preview-workunits`

```shell
# Preview 20 workunits without sending anything to sink
orrery ingest -c ./examples/recipes/example_to_orrery_rest.dhub.yaml -n --preview --preview-workunits=20
```

#### ingest --no-default-report

By default, the cli sends an ingestion report to Orrery, which allows you to see the result of all cli-based ingestion in the UI. This can be turned off with the `--no-default-report` flag.

```shell
# Running ingestion with reporting to Orrery turned off
orrery ingest -c ./examples/recipes/example_to_orrery_rest.dhub.yaml --no-default-report
```

The reports include the recipe that was used for ingestion. This can be turned off by adding an additional section to the ingestion recipe.

```yaml
source:
  # source configs

sink:
  # sink configs

# Add configuration for the orrery reporter
reporting:
  - type: orrery
    config:
      report_recipe: false

# Optional log to put failed JSONs into a file
# Helpful in case you are trying to debug some issue with specific ingestion failing
failure_log:
  enabled: false
  log_config:
    filename: ./path/to/failure.json
```

#### ingest --record (Beta)

:::note Beta Feature
Recording and replay is currently in beta. The feature is stable for debugging purposes but the archive format may change in future releases.
:::

The `--record` option enables recording of all HTTP requests and database queries during ingestion. This creates an encrypted archive that can be replayed offline for debugging.

```shell
# Record an ingestion run with password protection
orrery ingest -c ./recipe.yaml --record --record-password mysecret

# Record to a specific local directory
export INGESTION_ARTIFACT_DIR=/path/to/recordings
orrery ingest -c ./recipe.yaml --record --record-password mysecret --no-s3-upload

# Record and upload directly to S3
orrery ingest -c ./recipe.yaml --record --record-password mysecret \
    --record-output-path s3://my-bucket/recordings/my-run.zip
```

Recording options:

| Option                  | Description                                                                                                |
| ----------------------- | ---------------------------------------------------------------------------------------------------------- |
| `--record`              | Enable recording of the ingestion run                                                                      |
| `--record-password`     | Password for encrypting the archive. Can also be set via `ORRERY_RECORDING_PASSWORD` environment variable |
| `--record-output-path`  | Path to save the recording archive. Use local path or S3 URL (`s3://bucket/path/file.zip`)                 |
| `--no-s3-upload`        | Disable S3 upload (save locally only)                                                                      |
| `--no-secret-redaction` | Keep actual credentials in recording (use with caution, for local debugging only)                          |

The recording creates an encrypted ZIP archive containing HTTP cassettes, database query recordings, and a redacted recipe. This archive can be replayed using `orrery ingest replay`.

**Installation:** Recording requires the `debug-recording` plugin:

```shell
pip install 'orrery[debug-recording]'
```

➡️ [Learn more about recording and debugging ingestion](./how/debug-ingestion-recording.md)

### ingest replay (Beta)

The `ingest replay` command replays a recorded ingestion run for debugging. This allows you to reproduce issues in an air-gapped environment without network access.

```shell
# Replay from local file
orrery ingest replay ./recording.zip --password mysecret

# Replay from S3
orrery ingest replay s3://bucket/recordings/run-id.zip --password mysecret

# Replay with live sink (emit to real Orrery instance)
orrery ingest replay ./recording.zip --password mysecret --live-sink --server http://localhost:8080
```

Replay options:

| Option        | Description                                                                                                |
| ------------- | ---------------------------------------------------------------------------------------------------------- |
| `--password`  | Password for decrypting the archive. Can also be set via `ORRERY_RECORDING_PASSWORD` environment variable |
| `--live-sink` | Emit to real GMS server instead of using recorded responses                                                |
| `--server`    | GMS server URL when using `--live-sink`                                                                    |
| `--report-to` | Path to write the report file                                                                              |

### recording (Beta)

The `recording` command group provides utilities for working with recording archives.

```shell
# View archive metadata
orrery recording info recording.zip --password mysecret

# Extract archive contents
orrery recording extract recording.zip --password mysecret --output-dir ./extracted

# List archive contents
orrery recording list recording.zip --password mysecret
```

#### recording info

Display metadata about a recording archive including run ID, source type, creation time, and whether an exception was captured.

```shell
orrery recording info recording.zip --password mysecret
```

Use `--json` for machine-readable output.

#### recording extract

Extract a recording archive to inspect its contents:

```shell
orrery recording extract recording.zip --password mysecret --output-dir ./extracted
```

#### recording list

List the files contained in a recording archive:

```shell
orrery recording list recording.zip --password mysecret
```

### ingest deploy

The `ingest deploy` command instructs the cli to upload an ingestion recipe to Orrery to be run by Orrery's [UI Ingestion](./ui-ingestion.md).
This command can also be used to schedule the ingestion while uploading or even to update existing sources. It will upload to the remote instance the
CLI is connected to, not the sink of the recipe. Use `orrery init` to set the remote if not already set.

This command will automatically create a new recipe if it doesn't exist, or update it if it does.
Note that this is a complete update, and will remove any options that were previously set.
I.e: Not specifying a schedule in the cli update command will remove the schedule from the recipe to be updated.

#### Command Options

```console
Usage: orrery ingest deploy [OPTIONS]

Options:
  -n, --name TEXT     Recipe Name
  -c, --config FILE   Config file in .toml or .yaml format.  [required]
  --urn TEXT          Urn of recipe to update. If not specified here or in the recipe's pipeline_name,
                      this will create a new ingestion source.
  --executor-id TEXT  Executor id to route execution requests to. Do not use this unless you have
                      configured a custom executor.
  --cli-version TEXT  Provide a custom CLI version to use for ingestion. By default will use server
                      default.
  --schedule TEXT     Cron definition for schedule. If none is provided, ingestion recipe will not be
                      scheduled
  --time-zone TEXT    Timezone for the schedule in 'America/New_York' format. Uses UTC by default.
  --debug BOOLEAN     Should we debug.
  --extra-pip TEXT    Extra pip packages. e.g. ["memray"]
  --extra-env TEXT    Environment variables as comma-separated KEY=VALUE pairs.
                      e.g. "VAR1=value1,VAR2=value2"
```

#### Examples

**Schedule a recipe with default executor:**

```shell
orrery ingest deploy --name "Snowflake Integration" --schedule "0 5 * * *" --time-zone "Europe/London" -c recipe.yaml
```

**Deploy to a specific remote executor:**

```shell
orrery ingest deploy --name "Remote Snowflake Integration" --executor-id "remote-executor-pool-1" --schedule "0 5 * * *" -c recipe.yaml
```

**Update an existing recipe:**

```shell
orrery ingest deploy --urn "urn:li:orreryIngestionSource:deploy-12345678" --schedule "0 6 * * *" -c updated_recipe.yaml
```

**Deploy with environment variables:**

```shell
orrery ingest deploy --name "Snowflake Integration" --extra-env "SNOWFLAKE_WAREHOUSE=COMPUTE_WH,SNOWFLAKE_ROLE=ANALYST" -c recipe.yaml
```

By default, the ingestion recipe's identifier is generated by hashing the name.
You can override the urn generation by passing the `--urn` flag to the CLI.

#### Remote Executors

The `--executor-id` option allows you to route ingestion execution to specific executors:

- **Default executor** (`"default"`): Uses the managed executor provided by Orrery Cloud or your configured default executor
- **Remote executors**: Route to custom remote executors you've deployed in your environment

:::note
Use executor IDs other than "default" only if you have configured custom remote executors.
:::

**Examples with remote executors:**

```shell
# Deploy to default executor (can be configured as remote)
orrery ingest deploy --name "My Integration" -c recipe.yaml

# Deploy to specific remote executor pool
orrery ingest deploy --name "Private Network Integration" --executor-id "private-network-pool" -c recipe.yaml

# Deploy to region-specific executor
orrery ingest deploy --name "EU Region Integration" --executor-id "eu-west-executor" -c recipe.yaml
```

For more information on setting up remote executors, see the [Remote Executor Setup Guide](managed-orrery/operator-guide/setting-up-remote-ingestion-executor.md).

#### Using deployment section

As an alternative to configuring settings from the CLI, they can also be set in the `deployment` field of the recipe.

```yml
# deployment_recipe.yml
deployment:
  name: "Snowflake Integration"
  schedule: "0 5 * * *"
  time_zone: "Europe/London"
  executor_id: "remote-executor-pool-1" # Optional: specify remote executor
  cli_version: "0.15.0.1" # Optional: specify CLI version
  extra_pip: '["polars==1.35.2"]'
  extra_env: "VAR1=value1,VAR2=value2"

source: ...
```

```shell
orrery ingest deploy -c deployment_recipe.yml
```

CLI options will override corresponding values in the deployment section.

#### Deployment Configuration Options

These deployment options that can be specified via CLI flags can also be configured in the `deployment` section:

| Field         | CLI Option      | Description                     | Default            |
| ------------- | --------------- | ------------------------------- | ------------------ |
| `name`        | `--name`        | Recipe name displayed in the UI | Required           |
| `schedule`    | `--schedule`    | Cron expression for scheduling  | None (manual only) |
| `time_zone`   | `--time-zone`   | Timezone for scheduled runs     | `"UTC"`            |
| `executor_id` | `--executor-id` | Target executor for ingestion   | `"default"`        |
| `cli_version` | `--cli-version` | CLI version for ingestion       | Server default     |
| `extra_pip`   | `--extra-pip`   | Extra pip packages              | None               |
| `extra_env`   | `--extra-env`   | Extra environment variables     | None               |

#### Batch Deployment

Deploy multiple recipes from version control:

```shell
# Deploy every yml recipe in a directory
ls recipe_directory/*.yml | xargs -n 1 -I {} orrery ingest deploy -c {}

# Deploy with consistent executor across all recipes
ls recipe_directory/*.yml | xargs -n 1 -I {} orrery ingest deploy --executor-id "production-executor" -c {}
```

### recipe

The `recipe` commands help you author and verify an ingestion recipe before you run it. They
answer two kinds of question: _what does this source type accept?_ (offline, no connection)
and _what does this source actually contain, given my recipe?_ (live, needs working
credentials).

They are designed to be driven by an AI coding assistant as well as by hand — every command
prints JSON, and every failure uses a distinct exit code so a caller can tell "your input was
wrong" from "I could not reach the source".

#### Offline commands

These need only a source type or a recipe file — no connection, no credentials.

```shell
# What configuration fields does this source accept, and what does the connector
# declare it can do? Reports each field's type, whether it is required or a secret,
# and for AllowDenyPattern fields which hierarchy level it filters.
orrery recipe describe snowflake

# Emit a starter recipe for a source type
orrery recipe scaffold snowflake

# Validate a recipe's configuration without connecting
orrery recipe validate my_recipe.yml
```

#### Live commands

These connect using the recipe's own credentials. Secrets are resolved in-process and
redacted from all output.

```shell
# Verify the credentials work
orrery recipe test-connection --recipe my_recipe.yml
```

**Recipe variables** resolve exactly as `orrery ingest` resolves them: `${X}`, a `$X` that starts
the value, `${X:-default}` and `${X:=default}`; `$$` is kept as written. A dotted name such as
`${gms.server}` is not a path into `~/.orreryenv`: it reads as `${gms}` and becomes an empty
string, and `recipe validate` warns about it. Use `${ORRERY_GMS_URL}` and `${ORRERY_GMS_TOKEN}`.
A reference that takes part of a value (`${X:0:8}`, `${#X}`) is refused with exit code 2, since
only whole values can be masked in the output.

**Exploring what a source contains.** A source that can be probed lists `Probe` among its
capabilities in `orrery recipe describe <source_type>`, and in its connector docs. Start with
`probe methods`, which is connection-free and lists what this connector offers — each command's
parameters and what it returns:

```shell
orrery recipe probe methods --recipe my_recipe.yml

# Call one; a command's parameters imply the nesting
orrery recipe probe run columns --recipe my_recipe.yml --schema public --table orders
orrery recipe probe run topics --recipe my_recipe.yml --limit 50
```

Schema, table and view names are matched exactly as `containers`, `tables` and `views` list them;
a table argument also matches views and materialized views. Any other name, such as a foreign
table or one that differs only in case, is refused with exit code 2 before the database sees it.
When a listed name differs only in case, the error names it.

SQL sources expose a `sql` command for catalog queries, usually faster. It is an ordinary
command in the `probe methods` list — there is no separate subcommand to learn:

```shell
orrery recipe probe run sql --recipe my_recipe.yml --limit 50 \
  --query "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
```

Only a single `SELECT` over catalog metadata is permitted: `information_schema`, and beyond it
only the catalog relations the source names one by one. On Postgres that admits `pg_class` but not
`pg_stats`, `pg_statistic` or `pg_shadow`, which hold sampled column values or password hashes;
the refusal says what the source permits. Anything else — a user table, a second statement, a
vendor-specific function, a comment or an optimizer hint — is refused with exit code 2 before the
database sees it. A query the database rejects as the caller's mistake (SQLSTATE class 42, such
as a misspelled column) also exits 2. The check narrows what a query can reach; the credential
decides what each relation shows, so point the recipe at a read-only role.

**Checking what your filters would do.** `probe filter` judges names you already have, with no
connection:

```shell
orrery recipe probe filter --recipe my_recipe.yml \
  --kind Table --parent public --name orders --name users --name audit_log_v2

# Try a different pattern without editing the recipe
orrery recipe probe filter --recipe my_recipe.yml --kind Table --parent public \
  --name orders --name users --try-allow '^public\.ord.*'
```

Each result reports the `target` the pattern was matched against — which is usually the
qualified identifier, not the bare name. That matters: `AllowDenyPattern` is start-anchored, so
`^orders.*` matches nothing when ingestion evaluates `public.orders`. The output also names the
`pattern_field` that actually decided, which is not always the one named after the kind (MySQL
copies `table_pattern` into `view_pattern`).

The `--parent` containers are judged too: a table under a schema or database the recipe
excludes is reported excluded by that container's pattern, because ingestion never reaches it.
A `--name` or `--parent` that comes back masked (`***`, because it equals a secret) is refused
with exit code 2 rather than judged.

To judge a whole listing, write it with `probe run ... --report-to run.json` and pass
`--from-run run.json` instead of `--name`; its kind and parent are used unless you pass `--kind`
or `--parent`. The file must be a regular file of at most 50 MB.

`filtering` in the output says how the kind is filtered: `by_pattern` (`pattern_field` names the
field, as a dotted path when it sits in a nested block), `by_rule` (rules that are not a pattern,
such as `path_specs`, decide; `--try-allow`/`--try-deny` do not apply), `unfiltered`, or
`unresolved` (usually a kind this source does not have).

Probe output is **metadata only** — names, types, constraints, DDL, counts. No table rows,
no column values, no message payloads.

#### Exit codes

| Code | Meaning                                                                                                   |
| ---- | --------------------------------------------------------------------------------------------------------- |
| 0    | success                                                                                                   |
| 1    | a defect in Orrery or the connector, or a missing Python package (named in the error)                    |
| 2    | invalid input — a bad recipe or URL, an unknown command, a name the source does not list, a refused query |
| 3    | could not connect to the source, the source returned an error, or a read was recorded as failed           |

Errors are a JSON line on stderr, `{"error": "..."}`. An error raised by a driver, an SDK or the
connector is shown by label only, its class and at most one code, because its text is where
connection strings and query literals leak from: `'tables' failed (ProgrammingError; SQLSTATE
42P01)`. To see the full text while debugging locally, rerun with `ORRERY_PROBE_VERBOSE_LOGS=1`;
this also turns log scrubbing off, so do not use it where the output is shared or read by an agent.
stderr also carries the connector's log lines at INFO and above, scrubbed of the recipe's secrets
and of credential shapes, with tracebacks dropped.

When a source cannot be opened and the driver's exception chain holds a standard network error,
the label is followed by a reason and a fixed hint, for example `opening source 'mysql' failed
(OperationalError; errno 2003): ConnectionRefused - nothing is listening at the recipe's host and
port; ...`. The reason is read from the exception's type, never its text, and the exit code is
still 3:

| Reason              | Meaning and fix                                                                               |
| ------------------- | --------------------------------------------------------------------------------------------- |
| `HostNotResolved`   | The host name did not resolve. Check its spelling, and the DNS or VPN in use.                 |
| `ConnectionRefused` | Nothing listens at the host and port. Check the port, and that the server is running.         |
| `Timeout`           | The host did not answer. A firewall, allowlist or private network is likely dropping traffic. |
| `HostUnreachable`   | There is no network route to the host from this machine.                                      |
| `TlsVerifyFailed`   | The server's TLS certificate failed verification. Check the recipe's CA or TLS settings.      |

Drivers built on libpq (Postgres and its relatives) report no reason, since libpq is C and leaves no
Python network error to read.

A non-empty `warnings` list alongside an empty or partial result means _part of the source
could not be read_ — not that the source is empty. Treat the two differently.

### init

The init command is used to tell `orrery` about where your Orrery instance is located. The CLI will point to localhost Orrery by default.

**_Note_**: Provide your GMS instance's host when the prompt asks you for the Orrery host.

#### Interactive Mode

```shell
# Interactive mode - prompts for input
orrery init
/Users/user/.orreryenv already exists. Overwrite? [y/N]: y
Configure which orrery instance to connect to
Enter your Orrery host [http://localhost:8080]: http://localhost:8080
Enter your Orrery access token []:
```

#### Non-Interactive Mode with Username/Password

The CLI can automatically generate tokens from your username and password credentials. This is useful for quickstart instances, automation, and CI/CD pipelines.

```shell
# Quickstart (local instance with default credentials)
orrery init --profile local --username orrery --password orrery

# Custom credentials with longer token duration
orrery init --profile local --username orrery --password orrery --token-duration ONE_MONTH

# For long-running jobs or CI/CD
orrery init --profile local --username orrery --password orrery --token-duration ONE_WEEK
```

#### Orrery Cloud Example

For Orrery Cloud instances, you can use an existing token:

```shell
# Interactive
orrery init
Enter your Orrery host [http://localhost:8080]: https://<your-instance-id>.orrery.dev/gms
Enter your Orrery access token []: <token generated from https://<your-instance-id>.orrery.dev/settings/tokens>

# Non-interactive
orrery init --profile acme --host https://acme.example.com/gms --token "<token>"
```

#### Environment variables supported

Connection environment variables take precedence over the saved machine default. An explicit
`--profile` or `ORRERY_PROFILE` takes precedence over connection environment variables, as described
in [Selection order](#selection-order).

- `ORRERY_PROFILE` - Select a named connection for this shell or session.
- `ORRERY_SKIP_CONFIG` (default `false`) - Set to `true` to skip loading saved connection files. Cannot be combined with `--profile`; `ORRERY_PROFILE` is ignored when it is set.
- `ORRERY_GMS_URL` (default `http://localhost:8080`) - Set to a URL of GMS instance
- `ORRERY_GMS_HOST` (default `localhost`) - Set to a host of GMS instance. Prefer using `ORRERY_GMS_URL` to set the URL.
- `ORRERY_GMS_PORT` (default `8080`) - Set to a port of GMS instance. Prefer using `ORRERY_GMS_URL` to set the URL.
- `ORRERY_GMS_PROTOCOL` (default `http`) - Set to a protocol like `http` or `https`. Prefer using `ORRERY_GMS_URL` to set the URL.
- `ORRERY_GMS_TOKEN` (default `None`) - Used for communicating with Orrery Cloud.
- `ORRERY_CLIENT_CERT_PATH` (default `None`) - Path to a PEM file containing the client certificate to present when GMS sits behind a proxy that requires mutual TLS (mTLS). The file may be a combined PEM (cert + private key) if `ORRERY_CLIENT_KEY_PATH` is not set. Applies to **all** outbound HTTPS from the CLI / Python SDK — `orrery init`, `orrery ingest`, the `orrery-rest` sink, and any `OrreryGraph` client.
- `ORRERY_CLIENT_KEY_PATH` (default `None`) - Path to a PEM file containing the private key matching `ORRERY_CLIENT_CERT_PATH`. Required only when the cert and key live in separate files (e.g. the kubernetes `tls`-secret layout, where `tls.crt` and `tls.key` are mounted side by side).
- `ORRERY_USERNAME` (default `None`) - Username for generating access tokens via `orrery init`. Used with `ORRERY_PASSWORD` for non-interactive authentication.
- `ORRERY_PASSWORD` (default `None`) - Password for generating access tokens via `orrery init`. Used with `ORRERY_USERNAME` for non-interactive authentication.
- `ORRERY_TELEMETRY_ENABLED` (default `true`) - Set to `false` to disable telemetry. If CLI is being run in an environment with no access to public internet then this should be disabled.
- `ORRERY_TELEMETRY_TIMEOUT` (default `10`) - Set to a custom integer value to specify timeout in secs when sending telemetry.
- `ORRERY_DEBUG` (default `false`) - Set to `true` to enable debug logging for CLI. Can also be achieved through `--debug` option of the CLI. This exposes sensitive information in logs, enabling on production instances should be avoided especially if UI ingestion is in use as logs can be made available for runs through the UI.
- `ORRERY_VERSION` (default `quickstart`) - Set to a specific version to run quickstart with the particular version of docker images (`quickstart`, a release `v*`, or a commit pin `sha-<short_sha>`).
- `ORRERY_MAX_TRANSFORM_OPERATION_LENGTH` (default unset) - Only for cases where column-level lineage aspects grow too large. Set it to N (at least `1`) to cut `transformOperation` in fine-grained lineage to its first N characters followed by `...`; values of at most N + 3 characters are kept unchanged. With `incremental_lineage`, changing the value leaves entries stored under the old value until a run with `incremental_lineage: false`.
- `ACTIONS_VERSION` (default `quickstart`) - Set to a specific version to run quickstart with that image tag of `orrery-actions` container.
- `ORRERY_ACTIONS_IMAGE` (default `dxlxz/orrery-actions`) - Set to `-slim` to run a slimmer actions container without pyspark/deequ features.
- `ORRERY_RECORDING_PASSWORD` - Password for encrypting/decrypting recording archives. Used by `--record` and `--replay` commands.
- `INGESTION_ARTIFACT_DIR` - Directory to save recordings when S3 upload is disabled. If not set, recordings are saved to a temp directory.
- `ORRERY_REPORT_FAILURE_SAMPLE_SIZE` / `ORRERY_REPORT_WARNING_SAMPLE_SIZE` / `ORRERY_REPORT_INFO_SAMPLE_SIZE` (default `10`) - How many failure/warning/info entries to retain and show in the **final** report. Recipe flags: `report_failure_sample_size`, `report_warning_sample_size`, `report_info_sample_size`.
- `ORRERY_PROGRESS_REPORT_MAX_FAILURES` / `ORRERY_PROGRESS_REPORT_MAX_WARNINGS` / `ORRERY_PROGRESS_REPORT_MAX_INFOS` (default `10`) - How many entries to display in the **interim** 60-second progress reports. The final report is unaffected. Recipe flags: `progress_report_max_failures`, `progress_report_max_warnings`, `progress_report_max_infos`.
- `ORRERY_CALLER` (default: auto-detected) - Override the caller label sent in the User-Agent header to GMS. The CLI auto-detects common callers (e.g. coding agents, CI systems), but you can set this explicitly for custom tools or to distinguish environments (e.g. `airflow-prod-etl`).

```shell
ORRERY_SKIP_CONFIG=false
ORRERY_GMS_URL=http://localhost:8080
ORRERY_GMS_TOKEN=
ORRERY_TELEMETRY_ENABLED=true
ORRERY_TELEMETRY_TIMEOUT=10
ORRERY_DEBUG=false
ORRERY_REPORT_FAILURE_SAMPLE_SIZE=10
ORRERY_REPORT_WARNING_SAMPLE_SIZE=10
```

### container

A group of commands to interact with containers in Orrery.

e.g. You can use this to apply a tag to all datasets recursively in this container.

```shell
orrery container tag --container-urn "urn:li:container:0e9e46bd6d5cf645f33d5a8f0254bc2d" --tag-urn "urn:li:tag:tag1"
orrery container domain --container-urn "urn:li:container:3f2effd1fbe154a4d60b597263a41e41" --domain-urn  "urn:li:domain:ajsajo-b832-4ab3-8881-7ed5e991a44c"
orrery container owner --container-urn "urn:li:container:3f2effd1fbe154a4d60b597263a41e41" --owner-urn  "urn:li:corpGroup:eng@example.com"
orrery container term --container-urn "urn:li:container:3f2effd1fbe154a4d60b597263a41e41" --term-urn  "urn:li:term:PII"
```

### check

The orrery package is composed of different plugins that allow you to connect to different metadata sources and ingest metadata from them.
The `check` command allows you to check if all plugins are loaded correctly as well as validate an individual MCE-file.

#### restore-indices

This command allows you to restore indices for one or more `urn`.

```shell
orrery --debug check restore-indices --urn "URN"
```

It can also take `--file` argument that points to a file that has list of urns like

```shell
orrery check restore-indices --file ./urn.txt
```

where urn.txt is like this

```urn.txt
urn:li:dataset:(urn:li:dataPlatform:snowflake,test_db.schema1.test_all_nulls,PROD)
urn:li:dataset:(urn:li:dataPlatform:platform1,test_db.schema2.test_complex_types,PROD)
urn:li:dataset:(urn:li:dataPlatform:redshift,test_db.schema3.test_few_rows,PROD)
```

#### get-kafka-consumer-offsets

This required Orrery Cloud `0.3.12.x` or above version.

```shell
orrery check get-kafka-consumer-offsets
```

which can give can print out the details of lag in kafka topics.

```
{'mcl': {'generic-mae-consumer-job-client': {'MetadataChangeLog_Versioned_v1': {'metrics': {'avgLag': 36,
                                                                                            'maxLag': 36,
                                                                                            'medianLag': 36,
                                                                                            'totalLag': 36},
                                                                                'partitions': {'0': {'lag': 36,
                                                                                                     'offset': 257318}}}}},
 'mcl-timeseries': {'generic-mae-consumer-job-client': {'MetadataChangeLog_Timeseries_v1': {'metrics': {'avgLag': 0,
                                                                                                        'maxLag': 0,
                                                                                                        'medianLag': 0,
                                                                                                        'totalLag': 0},
                                                                                            'partitions': {'0': {'lag': 0,
                                                                                                                 'offset': 113}}}}},
 'mcp': {'generic-mce-consumer-job-client': {'MetadataChangeProposal_v1': {'metrics': {'avgLag': 7222149,
                                                                                       'maxLag': 7222149,
                                                                                       'medianLag': 7222149,
                                                                                       'totalLag': 7222149},
                                                                           'partitions': {'0': {'lag': 7222149,
                                                                                                'offset': 250254}}}}}}
```

### delete

The `delete` command allows you to delete metadata from Orrery.

The [metadata deletion guide](./how/delete-metadata.md) covers the various options for the delete command.

### exists

The exists command can be used to check if an entity exists in Orrery.

```shell
> orrery exists --urn "urn:li:dataset:(urn:li:dataPlatform:hive,SampleHiveDataset,PROD)"
true
> orrery exists --urn "urn:li:dataset:(urn:li:dataPlatform:hive,NonExistentHiveDataset,PROD)"
false
```

### get

The `get` command allows you to easily retrieve metadata from Orrery, by using the REST API. This works for both versioned aspects and timeseries aspects. For timeseries aspects, it fetches the latest value.
For example the following command gets the ownership aspect from the dataset `urn:li:dataset:(urn:li:dataPlatform:hive,SampleHiveDataset,PROD)`

```shell-session
$ orrery get --urn "urn:li:dataset:(urn:li:dataPlatform:hive,SampleHiveDataset,PROD)" --aspect ownership
{
  "ownership": {
    "lastModified": {
      "actor": "urn:li:corpuser:jdoe",
      "time": 1680210917580
    },
    "owners": [
      {
        "owner": "urn:li:corpuser:jdoe",
        "source": {
          "type": "SERVICE"
        },
        "type": "NONE"
      }
    ]
  }
}
```

### graphql

The `graphql` command allows you to execute GraphQL queries and mutations against Orrery's GraphQL API. This provides full access to Orrery's metadata through its native GraphQL interface.

```shell
# Execute a GraphQL query
orrery graphql --query "query { me { username } }"

# Use named operations from Orrery's schema
orrery graphql --operation searchAcrossEntities --variables '{"input": {"query": "users"}}'

# List available operations
orrery graphql --list-operations

# Get help for a specific operation
orrery graphql --describe searchAcrossEntities

# Explore types recursively
orrery graphql --describe SearchInput --recurse

# Load queries and variables from files
orrery graphql --query ./search-tags.graphql --variables ./search-params.json

# Get JSON output for LLM integration
orrery graphql --list-operations --format json
```

The GraphQL command supports both raw GraphQL queries/mutations and operation-based execution using Orrery's introspected schema. It automatically detects whether `--query` and `--variables` arguments are file paths or literal content, enabling seamless use of both inline GraphQL and file-based queries.

Key features:

- **Schema discovery**: List and describe all available operations and types
- **File support**: Load queries and variables from `.graphql` and `.json` files
- **LLM-friendly output**: JSON format with complete type information
- **Recursive exploration**: Deep-dive into complex GraphQL types

➡️ [Learn more about the GraphQL command](./cli-commands/graphql.md)

### search

The `search` command allows you to search across all Orrery entities from the command line with support for both keyword and semantic search.

```shell
# Basic keyword search
orrery search "users"

# Semantic search
orrery search --semantic "financial reports"

# Search with filters
orrery search "*" --filter platform=snowflake --filter entity_type=dataset

# Get results in table format
orrery search "*" --table
```

Key features:

- **Dual search modes**: Keyword search (default) and semantic search
- **Flexible filtering**: Simple key=value filters and complex JSON filters with AND/OR/NOT logic
- **Multiple output formats**: JSON (default), table, and URNs-only
- **Discovery features**: List available filters and describe specific filters
- **Faceted search**: Explore data distribution with aggregation counts
- **Pagination & sorting**: Control result size and ordering

➡️ [Learn more about the search command](./cli-commands/search.md)

### put

The `put` group of commands allows you to write metadata into Orrery. This is a flexible way for you to issue edits to metadata from the command line.

#### put aspect

The **put aspect** (also the default `put`) command instructs `orrery` to set a specific aspect for an entity to a specified value.
For example, the command shown below sets the `ownership` aspect of the dataset `urn:li:dataset:(urn:li:dataPlatform:hive,SampleHiveDataset,PROD)` to the value in the file `ownership.json`.
The JSON in the `ownership.json` file needs to conform to the [`Ownership`](https://github.com/Dxlxz/orrery/blob/master/metadata-models/src/main/pegasus/com/linkedin/common/Ownership.pdl) Aspect model as shown below.

```json
{
  "owners": [
    {
      "owner": "urn:li:corpuser:jdoe",
      "type": "DEVELOPER"
    },
    {
      "owner": "urn:li:corpuser:jdub",
      "type": "DATAOWNER"
    }
  ]
}
```

```console
orrery --debug put --urn "urn:li:dataset:(urn:li:dataPlatform:hive,SampleHiveDataset,PROD)" --aspect ownership -d ownership.json

[DATE_TIMESTAMP] DEBUG    {orrery.cli.cli_utils:340} - Attempting to emit to Orrery GMS; using curl equivalent to:
curl -X POST -H 'User-Agent: python-requests/2.26.0' -H 'Accept-Encoding: gzip, deflate' -H 'Accept: */*' -H 'Connection: keep-alive' -H 'X-RestLi-Protocol-Version: 2.0.0' -H 'Content-Type: application/json' --data '{"proposal": {"entityType": "dataset", "entityUrn": "urn:li:dataset:(urn:li:dataPlatform:hive,SampleHiveDataset,PROD)", "aspectName": "ownership", "changeType": "UPSERT", "aspect": {"contentType": "application/json", "value": "{\"owners\": [{\"owner\": \"urn:li:corpuser:jdoe\", \"type\": \"DEVELOPER\"}, {\"owner\": \"urn:li:corpuser:jdub\", \"type\": \"DATAOWNER\"}]}"}}}' 'http://localhost:8080/aspects/?action=ingestProposal'
Update succeeded with status 200
```

#### put platform

**🤝 Version Compatibility:** `orrery>0.8.44.4`

The **put platform** command instructs `orrery` to create or update metadata about a data platform. This is very useful if you are using a custom data platform, to set up its logo and display name for a native UI experience.

```shell
orrery put platform --name longtail_schemas --display_name "Long Tail Schemas" --logo "https://flink.apache.org/img/logo/png/50/color_50.png"
✅ Successfully wrote data platform metadata for urn:li:dataPlatform:longtail_schemas to Orrery (OrreryRestEmitter: configured to talk to https://longtailcompanions.orrery.dev/api/gms with token: eyJh**********Cics)
```

### timeline

The `timeline` command allows you to view a version history for entities. Currently only supported for Datasets. For example,
the following command will show you the modifications to tags for a dataset for the past week. The output includes a computed semantic version,
relevant for schema changes only currently, the target of the modification, and a description of the change including a timestamp.
The default output is sanitized to be more readable, but the full API output can be obtained by passing the `--verbose` flag and
to get the raw JSON difference in addition to the API output you can add the `--raw` flag. For more details about the feature please see [the main feature page](dev-guides/timeline.md)

```console
orrery timeline --urn "urn:li:dataset:(urn:li:dataPlatform:mysql,User.UserAccount,PROD)" --category TAG --start 7daysago
2022-02-17 14:03:42 - 0.0.0-computed
 MODIFY TAG dataset:mysql:User.UserAccount : A change in aspect editableSchemaMetadata happened at time 2022-02-17 20:03:42.0
2022-02-17 14:17:30 - 0.0.0-computed
 MODIFY TAG dataset:mysql:User.UserAccount : A change in aspect editableSchemaMetadata happened at time 2022-02-17 20:17:30.118
```

## Entity Specific Commands

### dataset (Dataset Entity)

The `dataset` command allows you to interact with Dataset entities in Orrery, including creating, updating, retrieving, and validating Dataset metadata.

```shell
# Get a dataset and write to YAML file
orrery dataset get --urn "urn:li:dataset:(urn:li:dataPlatform:hive,example_table,PROD)" --to-file dataset.yaml

# Create or update dataset from YAML file
orrery dataset upsert -f dataset.yaml
```

➡️ [Learn more about the dataset command](./cli-commands/dataset.md)

### user (User Entity)

The `user` command allows you to interact with the User entity in Orrery. It supports two main operations:

#### upsert

Create or update users from a YAML file. For detailed information, please refer to [Creating Users and Groups with Orrery CLI](/docs/api/tutorials/owners.md#upsert-users).

```shell
orrery user upsert -f users.yaml
```

An example of `users.yaml` would look like as in [bar.user.dhub.yaml](https://github.com/Dxlxz/orrery/blob/master/metadata-ingestion/examples/cli_usage/user/bar.user.dhub.yaml) file for the complete code.

```yaml
- id: bar@orrery.dev
  first_name: The
  last_name: Bar
  email: bar@orrery.dev
  slack: "@the_bar_raiser"
  description: "I like raising the bar higher"
  groups:
    - foogroup@orrery.dev
- id: orrery
  slack: "@orreryproject"
  phone: "1-800-GOT-META"
  description: "The Orrery Project"
  picture_link: "https://raw.githubusercontent.com/Dxlxz/orrery/master/orrery-web-react/src/images/orrery-logo-color-stable.svg"
```

#### add

Create a native Orrery user with email/password authentication. This command creates users who can log in directly to Orrery using their email and password.

```shell
# Create a user with a role
orrery user add --email user@example.com --display-name "John Doe" --password --role Admin

# Create a user without a role
orrery user add --email user@example.com --display-name "Jane Smith" --password
```

**Options:**

- `--email` (required): User's email address, which will be used as their login ID
- `--display-name` (required): User's full display name
- `--password` (required): Flag to prompt for password input (password will be hidden during entry)
- `--role` (optional): Role to assign to the user. Valid values are `Admin`, `Editor`, or `Reader` (case-insensitive)

**Notes:**

- The command will check if a user with the specified email already exists and exit if found
- Passwords are entered securely via a hidden prompt and require confirmation
- If role assignment fails, the user will still be created but without the role
- Requires admin permissions to execute

### group (Group Entity)

The `group` command allows you to interact with the Group entity.
It currently supports the `upsert` operation, which can be used to create a new group or update an existing one with embedded Users.
For more information, please refer to [Creating Users and Groups with Orrery CLI](/docs/api/tutorials/owners.md#upsert-users).

```shell
orrery group upsert -f group.yaml
```

An example of `group.yaml` would look like as in [foo.group.dhub.yaml](https://github.com/Dxlxz/orrery/blob/master/metadata-ingestion/examples/cli_usage/group/foo.group.dhub.yaml) file for the complete code.

```yaml
id: foogroup@orrery.dev
display_name: Foo Group
owners:
  - orrery
members:
  - bar@orrery.dev # refer to a user either by id or by urn
  - id: joe@orrery.dev # inline specification of user
    slack: "@joe_shmoe"
    display_name: "Joe's Hub"
```

### dataproduct (Data Product Entity)

**🤝 Version Compatibility:** `orrery>=0.10.2.4`

The dataproduct group of commands allows you to manage the lifecycle of a DataProduct entity on Orrery.
See the [Data Products](./dataproducts.md) page for more details on what a Data Product is and how Orrery represents it.

```shell
orrery dataproduct --help
Commands:
  upsert*          Upsert attributes to a Data Product in Orrery
  update           Create or Update a Data Product in Orrery.
  add_asset        Add an asset to a Data Product
  add_owner        Add an owner to a Data Product
  delete           Delete a Data Product in Orrery.
  diff             Diff a Data Product file with its twin in Orrery
  get              Get a Data Product from Orrery
  remove_asset     Add an asset to a Data Product
  remove_owner     Remove an owner from a Data Product
  set_description  Set description for a Data Product in Orrery
```

Here we detail the sub-commands available under the dataproduct group of commands:

#### upsert

Use this to upsert a data product yaml file into Orrery. This will create the data product if it doesn't exist already. Remember, this will upsert all the fields that are specified in the yaml file and will not touch the fields that are not specified. For example, if you do not specify the `description` field in the yaml file, then `upsert` will not modify the description field on the Data Product entity in Orrery. To keep this file sync-ed with the metadata on Orrery use the [diff](#diff) command. The format of the yaml file is available [here](./dataproducts.md#creating-a-data-product-yaml--git).

```shell
# Usage
> orrery dataproduct upsert -f data_product.yaml

```

#### update

Use this to fully replace a data product's metadata in Orrery from a yaml file. This will create the data product if it doesn't exist already. Remember, this will update all the fields including ones that are not specified in the yaml file. For example, if you do not specify the `description` field in the yaml file, then `update` will set the description field on the Data Product entity in Orrery to empty. To keep this file sync-ed with the metadata on Orrery use the [diff](#diff) command. The format of the yaml file is available [here](./dataproducts.md#creating-a-data-product-yaml--git).

```shell
# Usage
> orrery dataproduct upsert -f data_product.yaml

```

:::note

❗**Pro-Tip: upsert versus update**

Wondering which command is right for you? Use `upsert` if there are certain elements of metadata that you don't want to manage using the yaml file (e.g. owners, assets or description). Use `update` if you want to manage the entire data product's metadata using the yaml file.

:::

#### diff

Use this to keep a data product yaml file updated from its server-side version in Orrery.

```shell
# Usage
> orrery dataproduct diff -f data_product.yaml --update
```

#### get

Use this to get a data product entity from Orrery and optionally write it to a yaml file

```shell
# Usage
> orrery dataproduct get --urn urn:li:dataProduct:pet_of_the_week --to-file pet_of_the_week_dataproduct.yaml
{
  "id": "urn:li:dataProduct:pet_of_the_week",
  "domain": "urn:li:domain:dcadded3-2b70-4679-8b28-02ac9abc92eb",
  "assets": [
    "urn:li:dataset:(urn:li:dataPlatform:snowflake,long_tail_companions.analytics.pet_details,PROD)",
    "urn:li:dashboard:(looker,dashboards.19)",
    "urn:li:dataFlow:(airflow,snowflake_load,prod)"
  ],
  "display_name": "Pet of the Week Campaign",
  "owners": [
    {
      "id": "urn:li:corpuser:jdoe",
      "type": "BUSINESS_OWNER"
    }
  ],
  "description": "This campaign includes Pet of the Week data.",
  "tags": [
    "urn:li:tag:adoption"
  ],
  "terms": [
    "urn:li:glossaryTerm:ClientsAndAccounts.AccountBalance"
  ],
  "properties": {
    "lifecycle": "production",
    "sla": "7am every day"
  }
}
Data Product yaml written to pet_of_the_week_dataproduct.yaml
```

#### add_asset

Use this to add a data asset to a Data Product.

```shell
# Usage
> orrery dataproduct add_asset --urn "urn:li:dataProduct:pet_of_the_week"  --asset "urn:li:dataset:(urn:li:dataPlatform:hive,fct_users_deleted,PROD)"
```

#### remove_asset

Use this to remove a data asset from a Data Product.

```shell
# Usage
> orrery dataproduct remove_asset --urn "urn:li:dataProduct:pet_of_the_week"  --asset "urn:li:dataset:(urn:li:dataPlatform:hive,fct_users_deleted,PROD)"
```

#### add_owner

Use this to add an owner to a Data Product.

```shell
# Usage
> orrery dataproduct add_owner --urn "urn:li:dataProduct:pet_of_the_week"  --owner "jdoe@longtail.com" --owner-type BUSINESS_OWNER
```

#### remove_owner

Use this to remove an owner from a Data Product.

```shell
# Usage
> orrery dataproduct remove_owner --urn "urn:li:dataProduct:pet_of_the_week"  --owner "urn:li:corpUser:jdoe@longtail.com"
```

#### set_description

Use this to attach rich documentation for a Data Product in Orrery.

```shell
> orrery dataproduct set_description --urn "urn:li:dataProduct:pet_of_the_week" --description "This is the pet dataset"
# For uploading rich documentation from a markdown file, use the --md-file option
# > orrery dataproduct set_description --urn "urn:li:dataProduct:pet_of_the_week" --md-file ./pet_of_the_week.md
```

#### delete

Use this to delete a Data Product from Orrery. Default to `--soft` which preserves metadata, use `--hard` to erase all metadata associated with this Data Product.

```shell
> orrery dataproduct delete --urn "urn:li:dataProduct:pet_of_the_week"
# For Hard Delete see below:
# > orrery dataproduct delete --urn "urn:li:dataProduct:pet_of_the_week" --hard
```

## Miscellaneous Admin Commands

### lite (experimental)

The lite group of commands allow you to run an embedded, lightweight Orrery instance for command line exploration of your metadata. This is intended more for developer tool oriented usage rather than as a production server instance for Orrery. See [Orrery Lite](./orrery_lite.md) for more information about how you can ingest metadata into Orrery Lite and explore your metadata easily.

### telemetry

To help us understand how people are using Orrery, we collect anonymous usage statistics on actions such as command invocations via Mixpanel.
We do not collect private information such as IP addresses, contents of ingestions, or credentials.
The code responsible for collecting and broadcasting these events is open-source and can be found [within our GitHub](https://github.com/Dxlxz/orrery/blob/master/metadata-ingestion/src/orrery/telemetry/telemetry.py).

Telemetry is enabled by default, and the `telemetry` command lets you toggle the sending of these statistics via `telemetry enable/disable`.

### migrate

The `migrate` group of commands allows you to perform certain kinds of migrations.

All migration commands share the same engine, so they behave consistently:

- **All aspects are migrated.** The set of aspects carried to the new URN is sourced from the
  entity registry, so every user-authored aspect (ownership, tags, glossary terms, documentation,
  structured properties, and so on) is transferred — not a fixed subset. Timeseries aspects (usage
  statistics, profiles) and system-managed aspects (browse paths, incidents summary) are not
  migrated.
- **All incoming references are repointed.** Every entity that references a migrated URN via a
  modeled relationship — lineage, dashboard/chart references, **assertions**, etc. — is updated to
  point at the new URN, table- and column-level.

#### dataplatform2instance

The `dataplatform2instance` migration command allows you to migrate your entities from an instance-agnostic platform identifier to an instance-specific platform identifier. If you have ingested metadata in the past for this platform and would like to transfer any important metadata over to the new instance-specific entities, then you should use this command. For example, if your users have added documentation or added tags or terms to your datasets, then you should run this command to transfer this metadata over to the new entities. For further context, read the Platform Instance Guide [here](./platform-instances.md).

This command migrates the following entity types: datasets, charts, dashboards, dataflows, datajobs, and containers — together with all of their (registry-defined) aspects.

Options:

- `--platform` (required): The data platform to migrate (e.g. `hive`, `snowflake`, `powerbi`).
- `--instance` (required): The target platform instance name.
- `--dry-run` / `-n`: Preview what will be migrated without making changes.
- `--force` / `-F`: Skip the confirmation prompt.
- `--keep`: Preserve old entities instead of soft-deleting them.
- `--hard`: Hard-delete old entities instead of soft-delete.
- `--env`: The environment/fabric to filter on (default: `PROD`).
- `--on-conflict`: How to handle entities that already exist at the target. One of `overwrite` (default), `patch`, `prompt`, or `preserve` (see [Conflict strategies](#conflict-strategies)).
- `--skip-on-error`: Continue migrating remaining entities when one fails, instead of aborting.
- `--entity-types`: Comma-separated list of entity types to migrate (default: all). Available: `dataset`, `chart`, `dashboard`, `dataFlow`, `dataJob`.
- `--checkpoint-file`: Path to a checkpoint file for resumable migrations. Already-migrated source URNs are read from this file and skipped; newly migrated URNs are appended on success. The file is created automatically on first write. Not written during `--dry-run`; errored pairs (under `--skip-on-error`) are not checkpointed, so they retry on resume.

**_Note_**: Timeseries aspects such as Usage Statistics and Dataset Profiles are not migrated over to the new entity instances, you will get new data points created when you re-run ingestion using the `usage` or sources with profiling turned on.

##### Dry Run

```console
orrery migrate dataplatform2instance --platform powerbi --instance my_instance --dry-run --skip-on-error
Starting migration: platform:powerbi, instance=my_instance, force=False, dry-run=True
This command will migrate DATASET, CHART, DASHBOARD, DATAFLOW, DATAJOB and CONTAINERS.
No dataset entities found without instance, skipping.
No chart entities found without instance, skipping.
No dashboard entities found without instance, skipping.
No dataFlow entities found without instance, skipping.
No dataJob entities found without instance, skipping.
100% (398 of 398) |##############| Elapsed Time: 0:00:00
[Dry Run] Migration Report:
--------------
[Dry Run] Migration Run Id: container-migrate-f324880f-...
[Dry Run] Num entities created = 0
[Dry Run] Num entities affected = 0
[Dry Run] Num entities migrated = 0
```

##### Real Migration (with soft-delete)

```console
orrery migrate dataplatform2instance --platform hive --instance warehouse
Starting migration: platform:hive, instance=warehouse, force=False, dry-run=False
This command will migrate DATASET, CHART, DASHBOARD, DATAFLOW, DATAJOB and CONTAINERS.
Will migrate 4 urns such as [...]
New urns will look like ['urn:li:dataset:(urn:li:dataPlatform:hive,warehouse.SampleHiveDataset,PROD)', ...]
Ok to proceed? [y/N]: y
...
Migration Report:
--------------
Migration Run Id: migrate-f5ae7201-...-dataset
Num entities created = 4
Num entities affected = 0
Num entities migrated = 4
```

#### instance2instance

The `instance2instance` migration command allows you to migrate entities from one platform instance to another. This is useful when you need to rename or consolidate platform instances, for example when merging two Orrery environments or renaming an instance after an infrastructure change.

Like `dataplatform2instance`, this command migrates datasets, charts, dashboards, dataflows, datajobs, and containers. It rewrites URNs by replacing the old instance prefix with the new one and updates all incoming relationships (lineage, dashboard references, etc.) to point to the new entities.

When the target entity already exists, the command uses a strategy controlled by `--on-conflict`
(see [Conflict strategies](#conflict-strategies) below).

**Merge limitation**: Full aspect-level merge (via the Patch API) is only supported for **dataset** entities. For charts, dashboards, dataflows, and datajobs, the merge path falls back to overwrite when the target entity already exists.

**Container limitation**: Containers are migrated via a separate legacy code path that always overwrites the target. `--on-conflict`, `--skip-on-error`, and `--checkpoint-file` apply only to the entity migration (datasets, charts, dashboards, dataflows, datajobs) — not to containers. In practice this is rarely an issue: containers are a small bounded set per migration and are typically re-created by ingestion.

##### Conflict strategies

`--on-conflict` controls what happens when the target URN already exists. It is available on all
`migrate` commands.

- `patch`: Additively merge ownership, tags, terms, and lineage. Skip scalar fields (description, custom properties) that conflict.
- `overwrite`: Replace conflicting target aspects with source values.
- `prompt`: Ask interactively for each conflict.
- `preserve`: Leave the existing target **completely untouched** (no additive merge, no overwrite). Incoming references are still repointed to the existing target and the source is still deleted (unless `--keep`) — i.e. the existing target is _adopted_ in place of the source. Use this when the target is authoritative and you only want to redirect references and retire the source.

Defaults differ by command: `overwrite` for `dataplatform2instance`, `patch` for `instance2instance`, `overwrite` for `urns-mapping`.

Regardless of the chosen strategy, the following aspects are **never overwritten** during a merge (i.e. when the target already exists):

- **`status`**: The target's soft-delete state is authoritative. A soft-deleted source must not remove a live target.
- **`container`**: The target's parent container is preserved. For `urns-mapping`, containers are not migrated, so the source's container reference may point at an entity that is about to be deleted. For `dataplatform2instance` / `instance2instance`, the separate container migration step repoints container references via incoming-relationship rewriting.

These aspects are still cloned normally when the target does not exist (the clone path).

Options:

- `--platform` (required): The data platform (e.g. `snowflake`, `powerbi`).
- `--old-instance` (required): The source platform instance name.
- `--new-instance` (required): The target platform instance name.
- `--dry-run` / `-n`: Preview what will be migrated without making changes.
- `--force` / `-F`: Skip the confirmation prompt.
- `--keep`: Preserve old entities instead of soft-deleting them.
- `--hard`: Hard-delete old entities instead of soft-delete.
- `--env`: The environment/fabric to filter on (default: `PROD`).
- `--on-conflict`: Conflict resolution strategy: `patch` (default), `overwrite`, `prompt`, or `preserve` (see [Conflict strategies](#conflict-strategies)).
- `--skip-on-error`: Continue migrating remaining entities when one fails, instead of aborting.
- `--entity-types`: Comma-separated list of entity types to migrate (default: all). Available: `dataset`, `chart`, `dashboard`, `dataFlow`, `dataJob`.
- `--checkpoint-file`: Path to a checkpoint file for resumable migrations (see `dataplatform2instance` above).

**⚠️ Note**: `dataFlow` and `dataJob` should always be migrated together. DataJob URNs embed their parent DataFlow URN — migrating one without the other creates orphaned references. The CLI will prompt for confirmation if you attempt to separate them.

##### When to Use Which Command

| Scenario                                                   | Command                                            |
| ---------------------------------------------------------- | -------------------------------------------------- |
| First time adding a platform instance to existing entities | `dataplatform2instance`                            |
| Renaming or consolidating platform instances               | `instance2instance`                                |
| Partial migration (specific entity types only)             | Either command with `--entity-types=dataset,chart` |

The key difference: `dataplatform2instance` migrates entities that have **no** platform instance to a new instance (prepending the instance prefix). `instance2instance` migrates entities from one **existing** instance to another (replacing the instance prefix). The default `--on-conflict` also differs: `overwrite` for p2i, `patch` for i2i.

##### Dry Run

```console
orrery migrate instance2instance \
  --platform powerbi \
  --old-instance old_inst \
  --new-instance new_inst \
  --dry-run \
  --skip-on-error
Starting migration: platform:powerbi, old-instance=old_inst, new-instance=new_inst, force=False, dry-run=True, on-conflict=patch
This command will migrate DATASET, CHART, DASHBOARD, DATAFLOW, DATAJOB and CONTAINERS.
Found 689 dataset entities to migrate.
100% (689 of 689) |##############| Elapsed Time: 0:04:55
[Dry Run] Migration Report:
--------------
[Dry Run] Migration Run Id: migrate-i2i-cba165cc-...-dataset
[Dry Run] Num entities created = 689
[Dry Run] Num entities affected = 181
[Dry Run] Num entities migrated = 688
[Dry Run] Entities errored = 1
[Dry Run]   urn:li:dataset:(...,%_of_sales,PROD): URLDecoder: Illegal hex characters in escape (%) pattern
No chart entities found, skipping.
No dashboard entities found, skipping.
No dataFlow entities found, skipping.
No dataJob entities found, skipping.
```

##### Real Migration with Merge

```console
orrery migrate instance2instance \
  --platform powerbi \
  --old-instance old_inst \
  --new-instance new_inst \
  --on-conflict patch \
  --skip-on-error
```

##### Safe Exploration Workflow

When migrating between instances, a safe approach is:

1. **Dry run** to see what will happen:
   ```console
   orrery migrate instance2instance --platform powerbi --old-instance old --new-instance new --dry-run
   ```
2. **Migrate with --keep** to preserve the old entities as a safety net:
   ```console
   orrery migrate instance2instance --platform powerbi --old-instance old --new-instance new --keep
   ```
3. **Verify** the new entities look correct in the Orrery UI.
4. **Clean up** old entities manually with `orrery delete` if everything looks good.

#### urns-mapping

The `urns-mapping` command migrates entities using an **explicit source → target URN mapping** that
you supply, rather than deriving targets from a platform-instance strategy. Use it for arbitrary URN
moves that `dataplatform2instance` / `instance2instance` can't express — for example a bulk rename
computed by an external process. It runs the same engine (clone all aspects, rewrite self- and
incoming references, delete the source), so behavior matches the other commands.

Options:

- `--mapping-file` (required): Path to a JSON file of source → target pairs. Either a list of
  objects, `[{"source": "urn:...", "target": "urn:..."}, ...]`, or a flat object,
  `{"urn:src": "urn:tgt", ...}`.
- `--dry-run` / `-n`, `--force` / `-F`, `--keep`, `--hard`, `--skip-on-error`, `--checkpoint-file`: as for the other commands.
- `--on-conflict`: `overwrite` (default), `patch`, `prompt`, or `preserve` (see [Conflict strategies](#conflict-strategies)).

Each pair's source and target must be the **same entity type**. Unlike the instance-migration
commands, `urns-mapping` does **not** stamp a `dataPlatformInstance` (a platform instance cannot be
recovered from a target URN), does not migrate containers (see note below), and does not enforce
dataFlow/dataJob parent-URN consistency — you own the exact mapping.

**Containers are not migrated.** Container URNs are GUID-based (derived from a container key), so
they cannot be included in a simple source → target mapping. The next ingestion run against the new
names will re-create the containers automatically, but any user-authored governance metadata on the
old containers (tags, terms, documentation, ownership) will be lost. `dataplatform2instance` and
`instance2instance` handle container migration via a dedicated code path; adding container support
to `urns-mapping` is a potential future enhancement.

**Ordering across cross-referenced entities**: when a single mapping migrates entities that
reference one another (e.g. a dashboard that consumes a dataset you are also migrating), list the
**referenced** entity before the **referrer**. Incoming references are repointed using the
relationship index, which only reflects references that existed before the run — so migrating the
referenced entity first ensures the referrer is repointed and then carries the updated reference
into its own migrated copy.

**Large or complex migrations**: for big migrations, or reference graphs where clean
referenced-before-referrer ordering isn't possible (bidirectional references, cycles), migrate in
stages — e.g. one entity type at a time — with `--keep`, verifying (in the UI or via the graph API)
that references have settled before running the next stage. Because a later stage's
incoming-reference rewrite reads the relationship index, letting it catch up between stages lets that
stage see the entities migrated earlier. Note this only helps references _across_ stages; references
between two entities migrated in the **same** stage still rely on the ordering above, so keep
mutually-referencing entities dependency-ordered within a stage.

**Nested URNs — user responsibility**: some entity URNs embed another entity's URN as part of their
identity. The migration engine does **not** automatically derive child mappings from parent mappings:

- **dataFlow / dataJob**: a `dataJob` URN embeds its parent `dataFlow` URN
  (`urn:li:dataJob:(urn:li:dataFlow:(...), jobId)`). When migrating a dataFlow, you **must** also
  include mappings for its child dataJob URNs; otherwise the dataJob key aspects become inconsistent
  with the entity URN. Automatic derivation of child URN mappings may be added in a future release.
- **schemaField**: column-level metadata stored on the dataset's `editableSchemaMetadata` aspect
  (tags, terms, descriptions applied via the UI) **is** migrated along with the dataset. `schemaField`
  entities — which carry their own `globalTags`, `glossaryTerms`, `documentation`, and structured
  property aspects — are **not** repointed by `urns-mapping`; they are typically re-created by the next
  ingestion run against the new dataset name. When only the **column casing** changes (the dataset URN
  stays the same), use [`schema-field-case`](#schema-field-case) instead, which re-anchors those
  `schemaField` aspects and `editableSchemaMetadata` entries onto the new field paths.

Example `mapping.json` (list form):

```json
[
  {
    "source": "urn:li:dataset:(urn:li:dataPlatform:snowflake,old_db.schema.events,PROD)",
    "target": "urn:li:dataset:(urn:li:dataPlatform:snowflake,new_db.schema.events,PROD)"
  },
  {
    "source": "urn:li:dashboard:(looker,old.sales)",
    "target": "urn:li:dashboard:(looker,new.sales)"
  }
]
```

The equivalent flat-object form maps each source URN directly to its target:

```json
{
  "urn:li:dataset:(urn:li:dataPlatform:snowflake,old_db.schema.events,PROD)": "urn:li:dataset:(urn:li:dataPlatform:snowflake,new_db.schema.events,PROD)"
}
```

```console
orrery migrate urns-mapping --mapping-file ./mapping.json --dry-run
```

#### schema-field-case

The `schema-field-case` command re-anchors column-level metadata after a connector changes the
**casing** of column names — for example after enabling Snowflake's
[`preserve_column_case`](https://docs.orrery.dev/docs/generated/ingestion/sources/snowflake), or on
any connector that historically lowercased identifiers (e.g. Oracle). The dataset URN is unchanged;
only the field paths move (`product2id` → `Product2Id`), which strands the UI/API-authored metadata
that was keyed on the old paths. Matching is purely case-insensitive, so it works in **either
direction** — enabling case preservation (`product2id` → `Product2Id`) or the reverse, turning
lowercasing on for a source whose fields were mixed-case (`Product2Id` → `product2id`).

> **Not for dataset-URN casing changes.** Toggling `convert_urns_to_lowercase` re-cases the **dataset
> URN itself** (and the dataset portion of every `schemaField` URN), not the field paths — the whole
> dataset moves to a new URN. That is a dataset-level remap; use
> [`urns-mapping`](#urns-mapping) for it, not this command.

Unlike the other `migrate` commands, this does **not** rewrite entity URNs or use the migration
engine. It reconciles, per dataset, against the freshly re-ingested `schemaMetadata` (the source of
truth for the new casing):

- **`editableSchemaMetadata`** entries (descriptions, tags, terms edited in the UI) are rewritten in
  place onto the matching current field path.
- **`schemaField` entity** aspects (`globalTags`, `glossaryTerms`, `documentation`,
  `structuredProperties`, `businessAttributes`, and the other user-authored column aspects) are
  copied onto the correctly-cased `schemaField` URN. The old entity is soft-deleted by default (use
  `--keep-source-fields` to keep it).

> **Not re-anchored:** entities that _reference_ a column rather than living on it — native column
> assertions and incidents — are not repointed and will dangle on the soft-deleted source. Use
> `--keep-source-fields` if a dataset has those. Connector-emitted references (lineage,
> `fineGrainedLineage`, foreign keys) don't need repointing: the prerequisite re-ingestion re-emits
> them onto the new field path.

**Run order:** re-ingest the source with the new casing **first**, then run this command.

Matching is case-insensitive and handles both v1 and v2 field paths. It is conservative by default:

- **Ambiguous case-only collisions are never guessed.** If a single stranded field maps to two
  current fields that differ only by case (e.g. a historically lowercased `col` now split into `Col`
  and `COL`), it is reported for manual review and left untouched.
- **Existing destination metadata is preserved.** Tags, glossary terms, and structured properties are
  merged with whatever already sits on the correctly-cased field: tags/terms are unioned by URN
  (preserving source/immutable attribution), and structured properties are unioned by property URN.
  The same structured property carrying different values on each side, or any other aspect
  (`documentation`, `deprecation`, `businessAttributes`, …) that already differs on the destination,
  is reported rather than overwritten, and the source is kept so nothing is lost.
- **Re-running is safe** (idempotent): matched entries converge and merged aspects do not duplicate.

Use `--interactive` to resolve those clashes at the prompt instead of skipping them — you choose the
target for an ambiguous collision, and decide per-aspect whether to overwrite the destination on a
conflict. Without it, clashes are reported under "needing manual review" and left for you to handle.

Options:

- `--platform`: Platform to discover datasets for (e.g. `snowflake`, `oracle`). Ignored when
  `--urn` / `--urn-file` is given.
- `--platform-instance`: Platform instance to filter discovery on, if any. Works with or without a
  platform instance — everything is derived from the actual dataset URN.
- `--env`: Env (e.g. `PROD`) to filter discovery on, if any.
- `--urn` / `--urn-file`: Explicit dataset URN(s) to reconcile instead of discovery (repeatable /
  one URN per line).
- `--keep-source-fields` / `--delete-source-fields`: Keep or soft-delete the old `schemaField` entity
  after copying its aspects (default: delete). `editableSchemaMetadata` entries are always rewritten
  in place.
- `--include-soft-deleted` / `--exclude-soft-deleted`: Include soft-deleted `schemaField` entities
  when discovering stranded fields (default: exclude).
- `--interactive` / `--no-interactive`: Prompt to resolve each clash (ambiguous collision target,
  aspect-conflict overwrite) instead of skipping it (default: no). Ignored under `--dry-run`.
- `--dry-run` / `-n`: Report what would change without writing.
- `--force` / `-F`: Skip the confirmation prompt.

```console
# Preview against a single dataset
orrery migrate schema-field-case \
  --urn 'urn:li:dataset:(urn:li:dataPlatform:snowflake,my_db.my_schema.orders,PROD)' \
  --dry-run

# Reconcile every Snowflake dataset after enabling preserve_column_case
orrery migrate schema-field-case --platform snowflake --force
```

#### Known limitations

These apply to all `migrate` commands.

- **Non-relationship references are not repointed.** URN values stored in non-relationship fields
  (e.g. a URN _value_ of a structured property on another entity) are not graph-indexed and
  therefore not discovered or rewritten by the migration engine.

- **Optimistic cross-pair rewriting under `--skip-on-error`.** The batch URN rewriter is built from
  _all_ pairs before the migration loop starts. If pair A fails and a later pair B's aspects
  reference A's source URN, that reference is still rewritten to A's (never-created) target URN.
  This is intentional: when the user retries failed pairs via `--checkpoint-file`, A's target is
  created and B's reference becomes correct. Removing failed pairs from the map would leave B
  pointing at A's source (which may be deleted), making retry harder.

- **`--on-conflict patch` and multi-downstream fine-grained lineage.** The Patch API keys
  fine-grained lineage entries on `(transformOperation, downstream, query)` and requires exactly one
  downstream per entry. FGL entries with 0 or 2+ downstreams will raise an error during merge. This
  is a pre-existing constraint of the Patch API, not specific to migration. In practice, field-level
  lineage is almost always N upstreams → 1 downstream, so this is unlikely to trigger. Use
  `--on-conflict overwrite` instead of `patch` if your data contains multi-downstream FGL entries.

## Alternate Installation Options

### Using docker

[![Docker Hub](https://img.shields.io/docker/pulls/dxlxz/orrery-ingestion?style=plastic)](https://hub.docker.com/r/dxlxz/orrery-ingestion)

If you don't want to install locally, you can alternatively run metadata ingestion within a Docker container.
We have prebuilt images available on [Docker hub](https://hub.docker.com/r/dxlxz/orrery-ingestion). All plugins will be installed and enabled automatically.

You can use the `orrery-ingestion` docker image as explained in [Docker Images](../docker/README.md). In case you are using Kubernetes you can start a pod with the `orrery-ingestion` docker image, log onto a shell on the pod and you should have the access to orrery CLI in your kubernetes cluster.

_Limitation: the orrery_docker.sh convenience script assumes that the recipe and any input/output files are accessible in the current working directory or its subdirectories. Files outside the current working directory will not be found, and you'll need to invoke the Docker image directly._

```shell
# Assumes the Orrery repo is cloned locally.
./metadata-ingestion/scripts/orrery_docker.sh ingest -c ./examples/recipes/example_to_orrery_rest.yml
```

### Install from source

If you'd like to install from source, see the [developer guide](../metadata-ingestion/developing.md).

## Installing Plugins

We use a plugin architecture so that you can install only the dependencies you actually need. Click the plugin name to learn more about the specific source recipe and any FAQs!

### Sources

Please see our [Integrations page](https://docs.orrery.dev/integrations) if you want to filter on the features offered by each source.

| Plugin Name                                                                                    | Install Command                                            | Provides                                |
| ---------------------------------------------------------------------------------------------- | ---------------------------------------------------------- | --------------------------------------- |
| [metadata-file](./generated/ingestion/sources/metadata-file.md)                                | _included by default_                                      | File source and sink                    |
| [athena](./generated/ingestion/sources/athena.md)                                              | `pip install 'orrery[athena]'`                      | AWS Athena source                       |
| [bigquery](./generated/ingestion/sources/bigquery.md)                                          | `pip install 'orrery[bigquery]'`                    | BigQuery source                         |
| [csv-enricher](./generated/ingestion/sources/csv-enricher.md)                                  | _no additional dependencies_                               | CSV Enricher source                     |
| [orrery-lineage-file](./generated/ingestion/sources/file-based-lineage.md)                    | _no additional dependencies_                               | Lineage File source                     |
| [orrery-business-glossary](./generated/ingestion/sources/business-glossary.md)                | _no additional dependencies_                               | Business Glossary File source           |
| [dbt](./generated/ingestion/sources/dbt.md)                                                    | `pip install 'orrery[dbt]'`                         | dbt source                              |
| [dremio](./generated/ingestion/sources/dremio.md)                                              | `pip install 'orrery[dremio]'`                      | Dremio Source                           |
| [druid](./generated/ingestion/sources/druid.md)                                                | `pip install 'orrery[druid]'`                       | Druid Source                            |
| [feast](./generated/ingestion/sources/feast.md)                                                | `pip install 'orrery[feast]'`                       | Feast source (0.26.0)                   |
| [glue](./generated/ingestion/sources/glue.md)                                                  | `pip install 'orrery[glue]'`                        | AWS Glue source                         |
| [hana](./generated/ingestion/sources/hana.md)                                                  | `pip install 'orrery[hana]'`                        | SAP HANA source                         |
| [hive](./generated/ingestion/sources/hive.md)                                                  | `pip install 'orrery[hive]'`                        | Hive source                             |
| [kafka](./generated/ingestion/sources/kafka.md)                                                | `pip install 'orrery[kafka]'`                       | Kafka source                            |
| [kafka-connect](./generated/ingestion/sources/kafka-connect.md)                                | `pip install 'orrery[kafka-connect]'`               | Kafka connect source                    |
| [ldap](./generated/ingestion/sources/ldap.md)                                                  | `pip install 'orrery[ldap]'` ([extra requirements]) | LDAP source                             |
| [looker](./generated/ingestion/sources/looker.md)                                              | `pip install 'orrery[looker]'`                      | Looker source                           |
| [lookml](./generated/ingestion/sources/looker.md#module-lookml)                                | `pip install 'orrery[lookml]'`                      | LookML source                           |
| [metabase](./generated/ingestion/sources/metabase.md)                                          | `pip install 'orrery[metabase]'`                    | Metabase source                         |
| [mode](./generated/ingestion/sources/mode.md)                                                  | `pip install 'orrery[mode]'`                        | Mode Analytics source                   |
| [mongodb](./generated/ingestion/sources/mongodb.md)                                            | `pip install 'orrery[mongodb]'`                     | MongoDB source                          |
| [mssql](./generated/ingestion/sources/mssql.md)                                                | `pip install 'orrery[mssql]'`                       | SQL Server source                       |
| [mysql](./generated/ingestion/sources/mysql.md)                                                | `pip install 'orrery[mysql]'`                       | MySQL source                            |
| [mariadb](./generated/ingestion/sources/mariadb.md)                                            | `pip install 'orrery[mariadb]'`                     | MariaDB source                          |
| [openapi](./generated/ingestion/sources/openapi.md)                                            | `pip install 'orrery[openapi]'`                     | OpenApi Source                          |
| [oracle](./generated/ingestion/sources/oracle.md)                                              | `pip install 'orrery[oracle]'`                      | Oracle source                           |
| [postgres](./generated/ingestion/sources/postgres.md)                                          | `pip install 'orrery[postgres]'`                    | Postgres source                         |
| [redash](./generated/ingestion/sources/redash.md)                                              | `pip install 'orrery[redash]'`                      | Redash source                           |
| [redshift](./generated/ingestion/sources/redshift.md)                                          | `pip install 'orrery[redshift]'`                    | Redshift source                         |
| [sagemaker](./generated/ingestion/sources/sagemaker.md)                                        | `pip install 'orrery[sagemaker]'`                   | AWS SageMaker source                    |
| [salesforce](./generated/ingestion/sources/salesforce.md)                                      | `pip install 'orrery[salesforce]'`                  | Salesforce source                       |
| [snowflake](./generated/ingestion/sources/snowflake.md)                                        | `pip install 'orrery[snowflake]'`                   | Snowflake source                        |
| [sqlalchemy](./generated/ingestion/sources/sqlalchemy.md)                                      | `pip install 'orrery[sqlalchemy]'`                  | Generic SQLAlchemy source               |
| [superset](./generated/ingestion/sources/superset.md)                                          | `pip install 'orrery[superset]'`                    | Superset source                         |
| [tableau](./generated/ingestion/sources/tableau.md)                                            | `pip install 'orrery[tableau]'`                     | Tableau source                          |
| [trino](./generated/ingestion/sources/trino.md)                                                | `pip install 'orrery[trino]'`                       | Trino source                            |
| [starburst-trino-usage](./generated/ingestion/sources/trino.md)                                | `pip install 'orrery[starburst-trino-usage]'`       | Starburst Trino usage statistics source |
| [nifi](./generated/ingestion/sources/nifi.md)                                                  | `pip install 'orrery[nifi]'`                        | NiFi source                             |
| [powerbi](./generated/ingestion/sources/powerbi.md#module-powerbi)                             | `pip install 'orrery[powerbi]'`                     | Microsoft Power BI source               |
| [powerbi-report-server](./generated/ingestion/sources/powerbi.md#module-powerbi-report-server) | `pip install 'orrery[powerbi-report-server]'`       | Microsoft Power BI Report Server source |

### Debug/Utility Plugins

| Plugin Name     | Install Command                                | Provides                                              |
| --------------- | ---------------------------------------------- | ----------------------------------------------------- |
| debug-recording | `pip install 'orrery[debug-recording]'` | Record and replay ingestion runs for debugging (Beta) |

### Sinks

| Plugin Name                                                       | Install Command                              | Provides                   |
| ----------------------------------------------------------------- | -------------------------------------------- | -------------------------- |
| [metadata-file](../metadata-ingestion/sink_docs/metadata-file.md) | _included by default_                        | File source and sink       |
| [console](../metadata-ingestion/sink_docs/console.md)             | _included by default_                        | Console sink               |
| [orrery-rest](../metadata-ingestion/sink_docs/orrery.md)        | `pip install 'orrery[orrery-rest]'`  | Orrery sink over REST API |
| [orrery-kafka](../metadata-ingestion/sink_docs/orrery.md)       | `pip install 'orrery[orrery-kafka]'` | Orrery sink over Kafka    |

These plugins can be mixed and matched as desired. For example:

```shell
pip install 'orrery[bigquery,orrery-rest]'
```

### Check the active plugins

```shell
orrery check plugins
```

[extra requirements]: https://www.python-ldap.org/en/python-ldap-3.3.0/installing.html#build-prerequisites

## Release Notes and CLI versions

The server release notes can be found in [github releases](https://github.com/Dxlxz/orrery/releases). These releases are done approximately every week on a regular cadence unless a blocking issue or regression is discovered.

CLI release is made through a different repository and release notes can be found in [dxlxz releases](https://github.com/dxlxz/orrery/releases). At least one release which is tied to the server release is always made alongwith the server release. Multiple other bigfix releases are made in between based on amount of fixes that are merged between the server release mentioned above.

If server with version `0.8.28` is being used then CLI used to connect to it should be `0.8.28.x`. Tests of new CLI are not ran with older server versions so it is not recommended to update the CLI if the server is not updated.
