---
description: "Overview of Orrery Telemetry, the anonymous usage data the project collects, and instructions for opting out."
---

# Orrery Telemetry

## Overview of Orrery Telemetry

To effectively build and maintain the Orrery Project, we must understand how end-users work within Orrery. Beginning in version 0.8.35, Orrery collects anonymous usage statistics and errors to inform our roadmap priorities and to enable us to proactively address errors.

Both the Orrery backend and the ingestion framework collect telemetry.

## Orrery Backend Telemetry

Deployments are assigned a UUID which is sent along with event details, Java version, OS, and timestamp.
The source code is available [here](../../metadata-service/factories/src/main/java/com/linkedin/gms/factory/telemetry/TelemetryUtils.java).

## Ingestion Framework Telemetry

The ingestion framework collects telemetry including CLI invocations, source/sink types, error types, versions, and timestamps. If you run with `orrery --debug`, all telemetry calls will be logged.

On first invocation, the CLI will generate a randomized UUID, which will be sent alongside every telemetry event. This config is stored in `~/.orrery/telemetry-config.json`.

The source code is available [here](../../metadata-ingestion/src/orrery/telemetry/telemetry.py).

## Disabling Telemetry

Telemetry is enabled by default. While we are careful to anonymize all telemetry data and encourage users to keep it enabled so that we can improve Orrery, we understand that some users may wish to disable it.

You can disable backend telemetry by setting the `ORRERY_TELEMETRY_ENABLED` environment variable to `false`. You'll need to set this on both the orrery-gms and orrery-actions containers.

If you're using the Orrery CLI, ingestion framework telemetry will be disabled when the `ORRERY_TELEMETRY_ENABLED` environment variable is set to `false`. To persist this change, set `ORRERY_TELEMETRY_ENABLED=false` in your shell profile.
