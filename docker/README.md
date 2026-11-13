---
title: "Deploying with Docker"
hide_title: true
---

# Docker Images

## Prerequisites

You need to install [docker](https://docs.docker.com/install/) and
[docker-compose](https://docs.docker.com/compose/install/) (if using Linux; on Windows and Mac compose is included with
Docker Desktop).

Make sure to allocate enough hardware resources for Docker engine. Tested & confirmed config: 2 CPUs, 8GB RAM, 2GB Swap
area.

_If you prefer not to use Docker Desktop (which requires a license for commercial use), you can opt for free and open-source alternatives such as [Podman Desktop](https://podman-desktop.io/) or [Rancher Desktop](https://rancherdesktop.io/). To configure them, you can add the following aliases to your `~/.bashrc` file:_

```bash
# podman
alias docker=podman
alias docker-compose="podman compose"

# Rancher (or nerdctl)
alias docker=nerdctl
alias docker-compose="nerdctl compose"
```

## Quickstart

The easiest way to bring up and test Orrery is using Orrery [Docker](https://www.docker.com) images
which are continuously deployed to [Docker Hub](https://hub.docker.com/u/dxlxz) with every commit to repository.

You can easily download and run all these images and their dependencies with our
[quick start guide](../docs/quickstart.md).

**Compose layout:** Quickstart and local development use [Docker Compose profiles](profiles/docker-compose.yml) under
`docker/profiles/`. The CLI downloads a flattened compose file at
`docker/quickstart/docker-compose.quickstart-profile.yml`. Legacy root-level `docker-compose*.yml` files and shell
scripts (`quickstart.sh`, `dev.sh`, `nuke.sh`) have been removed.

Orrery Docker Images:

Do not use `latest` or `debug` tags for any of the image as those are not supported and present only due to legacy reasons. For local Compose / quickstart, use the coordinated `quickstart` tag or a version-specific tag like `v0.8.40`. For production and Kubernetes, pin a release tag (`v*`) or an immutable commit tag (`sha-<short_sha>`). Do not use `quickstart` in production clusters.

- [dxlxz/orrery-ingestion](https://hub.docker.com/r/dxlxz/orrery-ingestion/)
- [dxlxz/orrery-gms](https://hub.docker.com/repository/docker/dxlxz/orrery-gms/)
- [dxlxz/orrery-frontend-react](https://hub.docker.com/repository/docker/dxlxz/orrery-frontend-react/)
- [dxlxz/orrery-mae-consumer](https://hub.docker.com/repository/docker/dxlxz/orrery-mae-consumer/)
- [dxlxz/orrery-mce-consumer](https://hub.docker.com/repository/docker/dxlxz/orrery-mce-consumer/)
- [dxlxz/orrery-upgrade](https://hub.docker.com/r/dxlxz/orrery-upgrade/) (runs SystemUpdate; performs SQL and search index setup when `ORRERY_SQL_SETUP_ENABLED=true`)
- [dxlxz/orrery-actions](https://hub.docker.com/r/dxlxz/orrery-actions). Do not use `dxlxz/orrery-actions` as that is deprecated and no longer used.

## Image Variants

`orrery-ingestion` and `orrery-actions` are available as **full**, **slim**, and **locked**. **`orrery-ingestion`** is Ubuntu 24.04–based. **`orrery-actions`** uses the **default base image** set in its Dockerfile; override with Docker build arg **`BASE_IMAGE`** when needed.

Java runtime images (**`orrery-gms`**, **`orrery-mce-consumer`**, **`orrery-mae-consumer`**, **`orrery-upgrade`**, **`orrery-frontend-react`**) follow the same pattern: each Dockerfile declares a default **base image**; override with **`BASE_IMAGE`**. Optional **`APK_REPOSITORY_URL`** overrides the apk repository line used at image build time (see each Dockerfile for the default). When building with Gradle, use **`-PdockerBaseImage=...`** and **`-PapkRepositoryUrl=...`** instead of any legacy mirror property you may have used for older image builds. Those five images share **`docker/snippets/setup_java_runtime.sh`**, which ensures **`java`** is on **`PATH`** for entrypoints (apk OpenJDK JRE packages here do not add **`/usr/bin/java`** by default). All Java service images run **Java 25 LTS** at runtime (set via **`JAVA_MAJOR=25`** in the setup script); the script’s **`JAVA_MAJOR=…`** assignment is the single bump point for the apk JRE major. **`orrery-actions`** reads that line when installing OpenJDK for the full variant. **`orrery-ingestion`** images also use **`openjdk-25-jre-headless`**.

| Variant          | Image size | Use case                                |
| ---------------- | ---------- | --------------------------------------- |
| `full` (default) | Largest    | All connectors, maximum compatibility   |
| `slim`           | Medium     | Common connectors, good balance         |
| `locked`         | Medium     | Air-gapped environments (pypi disabled) |

### Variant Tag Format

```
dxlxz/orrery-ingestion:v0.x.y          # full (default)
dxlxz/orrery-ingestion:v0.x.y-slim     # slim
dxlxz/orrery-ingestion:v0.x.y-locked   # locked
```

### orrery-ingestion Feature Matrix

| Feature               | full | slim | locked |
| --------------------- | :--: | :--: | :----: |
| Core CLI & REST/Kafka | Yes  | Yes  |  Yes   |
| S3 / GCS / Azure Blob | Yes  | Yes  |  Yes   |
| Snowflake             | Yes  | Yes  |   -    |
| BigQuery              | Yes  | Yes  |   -    |
| Redshift              | Yes  | Yes  |   -    |
| MySQL / PostgreSQL    | Yes  | Yes  |   -    |
| ClickHouse            | Yes  | Yes  |   -    |
| dbt                   | Yes  | Yes  |   -    |
| Looker / LookML       | Yes  | Yes  |   -    |
| Tableau / PowerBI     | Yes  | Yes  |   -    |
| Superset              | Yes  | Yes  |   -    |
| Glue                  | Yes  | Yes  |   -    |
| Spark lineage (JRE)   | Yes  |  -   |   -    |
| Oracle client         | Yes  |  -   |   -    |
| MSSQL ODBC driver     | Yes  |  -   |   -    |
| Runtime `pip install` | Yes  | Yes  |   -    |

### orrery-actions Feature Matrix

| Feature                       | full | slim | locked |
| ----------------------------- | :--: | :--: | :----: |
| Core actions                  | Yes  | Yes  |  Yes   |
| Kafka / Executor              | Yes  | Yes  |  Yes   |
| Slack / Teams                 | Yes  | Yes  |  Yes   |
| Tag / Term / Doc propagation  | Yes  | Yes  |  Yes   |
| Snowflake tag propagation     | Yes  | Yes  |  Yes   |
| Bundled CLI venvs             | Yes  | Yes  |  Yes   |
| Oracle client (full only)     | Yes  |  -   |   -    |
| MSSQL ODBC driver (full only) | Yes  |  -   |   -    |
| Runtime `pip install`         | Yes  | Yes  |   -    |

### CI Testing Coverage

| Image Variant | Tested in CI | Notes                       |
| ------------- | :----------: | --------------------------- |
| `full`        |     Yes      | Smoke tests run on every PR |
| `slim`        |     Yes      | Smoke tests run on every PR |
| `locked`      |  Build only  |                             |

### Choosing the Right Variant

- **`full`**: Use when you need maximum connector coverage or aren't sure what you'll need
- **`slim`**: Recommended for most production deployments with standard cloud data stacks
- **`locked`**: Required for air-gapped environments where runtime package installation is prohibited

Dependencies:

- [Elasticsearch](elasticsearch) (or OpenSearch)
- [MySQL](mysql) (or PostgreSQL)
- [(Optional) Neo4j](neo4j)

SQL and search index setup are performed by the system update job (orrery-upgrade with `-u SystemUpdate`) when backing services are healthy; no separate setup containers are required.

### Ingesting demo data.

If you want to test ingesting some data once Orrery is up, run `orrery init` then `orrery datapack load showcase-ecommerce`. See the [quickstart guide](../docs/quickstart.md) for more details.

## Using Docker Images During Development

See [Using Docker Images During Development](../docs/docker/development.md).

## Building And Deploying Docker Images

We use GitHub Actions to build and continuously deploy our images. There should be no need to do this manually; a
successful release on Github will automatically publish the images.

### Building images

> This is **not** our recommended development flow and most developers should be following the
> [Using Docker Images During Development](../docs/docker/development.md) guide.

To build the full images (that we are going to publish), you need to run the following:

```
COMPOSE_DOCKER_CLI_BUILD=1 DOCKER_BUILDKIT=1 docker compose -p orrery build
```

This is because we're relying on builtkit for multistage builds. It does not hurt also set `ORRERY_VERSION` to
something unique.

### Community Built Images

As the open source project grows, community members would like to contribute additions to the docker images. Not all contributions to the images can be accepted because those changes are not useful for all community members, it will increase build times, add dependencies and possible security vulns. In those cases this section can be used to point to `Dockerfiles` hosted by the community which build on top of the images published by the Orrery core team along with any container registry links where the result of those images are maintained.

# Orrery Docker Nuke Tasks

This document describes the nuke task system for cleaning up Orrery Docker containers and volumes.

## Overview

The nuke tasks provide a way to completely remove Orrery containers and volumes for different project namespaces. This is useful for:

- Cleaning up test environments
- Resetting development setups
- Isolating different project instances
- Troubleshooting container issues

## Available Tasks

### All Configurations Have Nuke Tasks

Every quickstart configuration automatically gets a nuke task for targeted cleanup:

- **`quickstartNuke`** - Removes containers and volumes for the default project namespace (`orrery`)
- **`quickstartDebugNuke`** - Removes containers and volumes for the debug configuration (`orrery`)
- **`quickstartDebugMinNuke`** - Removes containers and volumes for the debug-min configuration (`orrery`)
- **`quickstartDebugConsumersNuke`** - Removes containers and volumes for the debug-consumers configuration (`orrery`)
- **`quickstartPgNuke`** - Removes containers and volumes for the quickstart-postgres configuration (`orrery`)
- **`quickstartPgConsumersNuke`** - Removes containers and volumes for the quickstart-postgres-consumers configuration (`orrery`)
- **`quickstartPgDebugNuke`** - Removes containers and volumes for the debug-postgres configuration (`orrery`)
- **`quickstartOS2Nuke`** - Removes containers and volumes for the quickstart-opensearch2 configuration (`orrery`)
- **`quickstartOS2DebugNuke`** - Removes containers and volumes for the debug-opensearch2 configuration (`orrery`)
- **`quickstartOS3Nuke`** - Removes containers and volumes for the quickstart-opensearch3 configuration (`orrery`)
- **`quickstartOS3DebugNuke`** - Removes containers and volumes for the debug-opensearch3 configuration (`orrery`)
- **`quickstartSlimNuke`** - Removes containers and volumes for the backend configuration (`orrery`)
- **`quickstartSparkNuke`** - Removes containers and volumes for the spark configuration (`orrery`)
- **`quickstartStorageNuke`** - Removes containers and volumes for the storage configuration (`orrery`)
- **`quickstartBackendDebugNuke`** - Removes containers and volumes for the backend-debug configuration (`orrery`)

### Project Namespace Behavior

- **Default project namespace (`orrery`)**: Most configurations use this, so their nuke tasks will clean up containers in the same namespace

## Usage

### Basic Usage

```bash
# Remove containers and volumes for specific configurations
./gradlew quickstartDebugNuke      # For debug configuration
./gradlew quickstartDebugMinNuke   # For debug-min configuration
./gradlew quickstartPgNuke           # For quickstart-postgres (slim)
./gradlew quickstartPgConsumersNuke  # For quickstart-postgres-consumers (with MAE/MCE)

# For general cleanup of all containers
./gradlew quickstartDown
```

### When to Use Each Task

- **Use specific nuke tasks when:**

  - You want to clean up a specific configuration environment
  - You need targeted cleanup without affecting other configurations
  - You're working with a particular development setup (debug, postgres, etc.)

- **Use `quickstartDown` when:**
  - You want to stop all running containers
  - You need a general cleanup option
  - You want to ensure all environments are stopped

## How It Works

1. **Volume Management**: Each nuke task sets `removeVolumes = true` for relevant configurations
2. **Container Cleanup**: Tasks are finalized by appropriate `ComposeDownForced` operations
3. **Project Isolation**: Each task operates within its own Docker Compose project namespace
4. **Configuration Respect**: Tasks respect the `projectName` settings in `quickstart_configs`

## Configuration

The nuke tasks are automatically generated based on the `quickstart_configs` in `docker/build.gradle`. To add a new nuke task:

1. Add a configuration to `quickstart_configs`:

   ```gradle
   'quickstartCustom': [
       profile: 'debug',
       modules: [...],
       // Optional: custom project name for isolation
       additionalConfig: [
           projectName: 'dh-custom'
       ]
   ]
   ```

2. The task `quickstartCustomNuke` will be automatically created

## Troubleshooting

### Task Not Found

- Ensure the configuration exists in `quickstart_configs`
- Check that the task name follows the pattern `{configName}Nuke`

### Containers Not Removed

- Verify the project namespace is correct
- Check that the configuration has the right `projectName` setting
- Ensure the task is targeting the correct `ComposeDownForced` operations

### Volume Persistence

- Check if `preserveVolumes` is set to `true` in the configuration
- Verify the `removeVolumes` setting is properly applied

## Related Commands

- **Start services**: `./gradlew quickstartDebug`
- **Stop services**: `./gradlew quickstartDown`
- **Reload services**: `./gradlew debugReload`

## Examples

### Complete Development Environment Reset

```bash
# Clean up specific debug environment
./gradlew quickstartDebugNuke

# Start fresh
./gradlew quickstartDebug
```

### Mixed Environment Management

```bash
# Clean up only debug environment (leaving other environments intact)
./gradlew quickstartDebugNuke

# Clean up only postgres environment (leaving others intact)
./gradlew quickstartPgNuke
# Or the postgres + consumers variant:
./gradlew quickstartPgConsumersNuke
```
