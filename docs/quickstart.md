import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Orrery Quickstart Guide

:::tip Orrery Cloud

Want a fully managed Orrery? **[Try Orrery Cloud free](https://orrery.dev/free-trial/)**.

:::

> **Already running Orrery?** If you're upgrading an existing install, check [Managing Your Local Instance](#managing-your-local-instance) before running quickstart — some versions need upgrade steps first.

## Prerequisites

- Install **Docker** and **Docker Compose** v2 for your platform.

  | Platform | Application                                                                                                                                     |
  | -------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
  | Windows  | [Docker Desktop](https://www.docker.com/products/docker-desktop/)                                                                               |
  | Mac      | [Docker Desktop](https://www.docker.com/products/docker-desktop/)                                                                               |
  | Linux    | [Docker for Linux](https://docs.docker.com/desktop/install/linux-install/) and [Docker Compose](https://docs.docker.com/compose/install/linux/) |

- **Launch the Docker engine** from command line or the desktop app.
- Ensure you have **Python 3.10+** installed & configured. (Check using `python3 --version`).

:::note Docker Resource Allocation

Make sure to allocate enough hardware resources for Docker engine. <br />
Tested & confirmed config: 2 CPUs, 8GB RAM, 2GB Swap area, and 13GB disk space.

:::

## Install the Orrery CLI

<Tabs>
<TabItem value="brew" label="Homebrew">

```bash
brew install Dxlxz/tap/orrery
orrery version
```

Homebrew manages an isolated Python environment for `orrery`, so there's no venv to activate. The formula installs the **core CLI only** — to add ingestion connectors, install them into the brew-managed environment:

```bash
"$(brew --prefix orrery)/libexec/bin/pip" install 'orrery[snowflake,bigquery]'
```

</TabItem>
<TabItem value="pip" label="pip">

```bash
python3 -m pip install --upgrade pip wheel setuptools
python3 -m pip install --upgrade orrery
orrery version
```

:::note Command Not Found

If you see `command not found`, try running cli commands like `python3 -m orrery version`. <br />
Note that Orrery CLI does not support Python 2.x.

:::

</TabItem>
</Tabs>

## Start Orrery

Run the following CLI command from your terminal.

```bash
orrery docker quickstart
```

This will deploy a Orrery instance using [docker-compose](https://docs.docker.com/compose/).
If you are curious, the `docker-compose.yml` file is downloaded to your home directory under the `.orrery/quickstart` directory.

If things go well, you should see messages like the ones below:

```shell-session
Fetching docker-compose file https://raw.githubusercontent.com/Dxlxz/orrery/master/docker/quickstart/docker-compose.quickstart-profile.yml from GitHub
Pulling docker images...
Finished pulling docker images!

Starting up Orrery...
[+] Running 14/14
 ✔ Network orrery_network                         Created                                                                                              0.0s
 ✔ Volume "orrery_broker"                         Created                                                                                              0.0s
 ✔ Volume "orrery_mysqldata"                      Created                                                                                              0.0s
 ✔ Volume "orrery_osdata"                         Created                                                                                              0.0s
 ✔ Container orrery-mysql-1                       Healthy                                                                                             11.6s
 ✔ Container orrery-opensearch-1                  Healthy                                                                                             11.6s
 ✔ Container orrery-kafka-broker-1               Healthy                                                                                              6.0s
 ✔ Container orrery-system-update-quickstart-1    Exited                                                                                              26.6s
 ✔ Container orrery-orrery-gms-quickstart-1      Healthy                                                                                             42.1s
 ✔ Container orrery-frontend-quickstart-1         Started                                                                                             26.6s
 ✔ Container orrery-orrery-actions-quickstart-1  Started                                                                                             42.1s

✔ Orrery is now running
Load sample data: run `orrery init` then `orrery datapack load showcase-ecommerce`,
or head to http://localhost:9002 (username: orrery, password: orrery) to play around with the frontend.
Need support? Get in touch on Slack: https://orrery.dev/slack/
```

### Sign In

Upon completion of this step, you should be able to navigate to the Orrery UI at [http://localhost:9002](http://localhost:9002) in your browser.
You can sign in using the default credentials below.

```json
username: orrery
password: orrery
```

To change the default credentials, please refer to [Change the default user orrery in quickstart](authentication/changing-default-credentials.md#quickstart).

### Load Sample Data

First, configure the CLI to talk to your local Orrery instance:

```bash
orrery init --username orrery --password orrery
```

Then load the **showcase-ecommerce** data pack — a rich set of ~1,050 entities across Snowflake, Looker, PowerBI, and Tableau with lineage, governance, glossary terms, domains, and data products:

```bash
orrery datapack load showcase-ecommerce
```

:::note

The `orrery datapack` command is currently experimental — its command surface and behavior may change in future releases.

:::

That's it! Now feel free to play around with Orrery!

## Next Steps

- [Quickstart Debugging Guide](./troubleshooting/quickstart.md)
- [Ingest metadata through the UI](./ui-ingestion.md)
- [Ingest metadata through the CLI](../metadata-ingestion/README.md)
- [Add Users to Orrery](authentication/guides/add-users.md)
- [Configure OIDC Authentication](authentication/guides/sso/configure-oidc-react.md)
- [Configure JaaS Authentication](authentication/guides/jaas.md)
- [Configure authentication in Orrery's backend](authentication/introducing-metadata-service-authentication.md#configuring-metadata-service-authentication).
- [Change the default user orrery in quickstart](authentication/changing-default-credentials.md#quickstart)

## Managing Your Local Instance

Once Orrery is running, use the commands below to manage your local instance. These are for the local quickstart deployment only — for production operations, see [Deploying Orrery with Kubernetes](./deploy/kubernetes.md).

:::note (Optional) Set a stable signing key

By default, Orrery generates a random signing key and salt for authentication tokens on first run and reuses them on later runs (saved to `~/.orrery/quickstart/.local-secrets.env`). Most users don't need to change this.

If you want tokens to stay stable across environments, set your own values **before** running `orrery docker quickstart`:

```bash
export ORRERY_TOKEN_SERVICE_SIGNING_KEY=<value>
export ORRERY_TOKEN_SERVICE_SALT=<value>
```

Generate a value with `openssl rand -base64 32`.

If upgrading from a CLI older than v1.5: the signing key is now generated randomly instead of using a hardcoded default, so any personal access tokens (PATs) created before upgrading are invalidated and must be regenerated.

:::

:::note Upgrading from a CLI older than v1.2

From version 1.2 onwards, the `orrery docker quickstart` command uses a docker-compose file that is incompatible with Orrery installed using earlier versions of the CLI. If you already have Orrery installed with an older CLI, upgrade with these steps:

1. Back up your data (recommended): `orrery docker quickstart --backup`. This can be skipped if the data does not need to be preserved.
2. Remove the old installation: `orrery docker nuke`
3. Start a fresh installation: `orrery docker quickstart`

⚠️ Without a backup, all existing data will be lost.

For the full list of breaking changes across versions, see [Breaking Changes](how/breaking-changes.md).

:::

<details>
<summary><b>Stop Orrery</b></summary>

To stop Orrery's quickstart, you can issue the following command.

```bash
orrery docker quickstart --stop
```

</details>

<details>
<summary><b>Reset Orrery</b></summary>

To cleanse Orrery of all of its state (e.g. before ingesting your own), you can use the CLI `nuke` command.

```bash
orrery docker nuke
```

</details>

<details>
<summary><b>Upgrade Orrery</b></summary>

If you have been testing Orrery locally, a new version of Orrery got released and you want to try the new version then you can just issue the quickstart command again. It will pull down newer images and restart your instance without losing any data.

```bash
orrery docker quickstart
```

By default, quickstart will install the latest released version of orrery. You can pick a specific version by specifying a release explicitly.

```bash
orrery docker quickstart --version v1.6.0
```

You can see the releases available on the [github releases](https://github.com/Dxlxz/orrery/releases) page
You can also specify `head` or `quickstart` as the version to get the latest coordinated development images from `master` (compose from `master`, images tagged `quickstart`). For a specific commit build, use `sha-<short_sha>` (registry-only, not a git tag).

If you pass an unrecognized `--version` that is not a release tag (for example a typo), the CLI prompts before falling back to the default quickstart configuration. Omitting `--version` uses the default without prompting. Release-like tags (`v1.2.0`) and `sha-*` tags are used as-is without prompting. Four-part hotfix tags (`v1.5.0.6`) use that tag for Orrery images and the shorter release (`v1.5.0`) for compose and other non-image settings. For scripts, pass `--accept-version-default` to accept the suggested configuration without an interactive prompt.

</details>

<details>
<summary><b>Customize installation</b></summary>

If you would like to customize the Orrery installation further, please download the [docker-compose file](https://raw.githubusercontent.com/Dxlxz/orrery/master/docker/quickstart/docker-compose.quickstart-profile.yml) used by the CLI tool, modify it as necessary and deploy Orrery by passing the downloaded docker-compose file:

```bash
orrery docker quickstart --quickstart-compose-file <path to compose file>
```

</details>

<!-- Back up Orrery and Restore Orrery are intentionally plain headings, not <details> collapsibles like the sections above, because other docs (how/restore-indices.md) and the CLI source (docker_cli.py) link directly to their #back-up-orrery and #restore-orrery anchors. Plain headings give stable auto-generated anchors that reliably resolve and scroll. Do not convert these two to <details>. -->

### Back up Orrery

The quickstart image is not recommended for use as a production instance. <br />
However, in case you want to take a backup of your current quickstart state (e.g. you have a demo to your company coming up and you want to create a copy of the quickstart data so you can restore it at a future date), you can supply the `--backup` flag to quickstart.

```bash
orrery docker quickstart --backup
```

This will take a backup of your MySQL image and write it by default to your `~/.orrery/quickstart/` directory as the file `backup.sql`.

You can customize the backup file path by passing a `--backup-file` argument:

```bash
orrery docker quickstart --backup --backup-file <path to backup file>
```

:::caution

Note that the Quickstart backup does not include any timeseries data (dataset statistics, profiles, etc.), so you will lose that information if you delete all your indexes and restore from this backup.

:::

### Restore Orrery

As you might imagine, these backups are restore-able. The following section describes a few different options you have to restore your backup.

#### General Restoring

To restore a previous backup, run the following command:

```bash
orrery docker quickstart --restore
```

This command will pick up the `backup.sql` file located under `~/.orrery/quickstart` and restore your primary database as well as the elasticsearch indexes with it.

To supply a specific backup file, use the `--restore-file` option.

```bash
orrery docker quickstart --restore --restore-file /home/my_user/orrery_backups/quickstart_backup_2002_22_01.sql
```

#### Restore Only Index

Another situation that can come up is the index can get corrupt, or be missing some update. In order to re-bootstrap the index from the primary store, you can run this command to sync the index with the primary store.

```bash
orrery docker quickstart --restore-indices
```

#### Restore Only Primary

Sometimes, you might want to just restore the state of your primary database (MySQL), but not re-index the data. To do this, you have to explicitly disable the restore-indices capability.

```bash
orrery docker quickstart --restore --no-restore-indices
```

## Move To Production

:::caution

Quickstart is not intended for a production environment. We recommend deploying Orrery to production using Kubernetes.
We provide helpful [Helm Charts](https://artifacthub.io/packages/helm/orrery/orrery) to help you quickly get up and running.
Check out [Deploying Orrery to Kubernetes](./deploy/kubernetes.md) for a step-by-step walkthrough.

:::

The `quickstart` method of running Orrery is intended for local development and a quick way to experience the features that Orrery has to offer.
It is not intended for a production environment. This recommendation is based on the following points.

### Default Credentials

`quickstart` uses docker compose configuration which includes default credentials for both Orrery, and it's underlying
prerequisite data stores, such as MySQL. Additionally, other components are unauthenticated out of the box. This is a
design choice to make development easier and is not best practice for a production environment.

### Exposed Ports

Orrery's services, and it's backend data stores use the docker default behavior of binding to all interface addresses.
This makes it useful for development but is not recommended in a production environment.

### Performance & Management

`quickstart` is limited by the resources available on a single host, there is no ability to scale horizontally.
Rollout of new versions often requires downtime and the configuration is largely pre-determined and not easily managed.
Lastly, by default, `quickstart` follows the most recent builds forcing updates to the latest released and unreleased builds.
