# Orrery's Reporting Framework for Ingestion Job Telemetry

The Orrery's reporting framework allows for configuring reporting providers with the ingestion pipelines to send
telemetry about the ingestion job runs to external systems for monitoring purposes. It is powered by the Orrery's
stateful ingestion framework. The `orrery` reporting provider comes with the standard client installation,
and allows for reporting ingestion job telemetry to the orrery backend as the destination.

**_NOTE_**: This feature requires the server to be `statefulIngestion` capable.
This is a feature of metadata service with version >= `0.8.20`.

To check if you are running a stateful ingestion capable server:

```console
curl http://<orrery-gms-endpoint>/config

{
models: { },
statefulIngestionCapable: true, # <-- this should be present and true
retention: "true",
noCode: "true"
}
```

## Config details

The ingestion reporting providers are a list of reporting provider configurations under the `reporting` config
param of the pipeline, each reporting provider configuration begin a type and config pair object. The telemetry data will
be sent to all the reporting providers in this list.

Note that a `.` is used to denote nested fields, and `[idx]` is used to denote an element of an array of objects in the YAML recipe.

| Field                     | Required | Default                                                                                                                                                                                                                                        | Description                                                                                                                                              |
| ------------------------- | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `reporting[idx].type`     | ✅       | `orrery`                                                                                                                                                                                                                                      | The type of the ingestion reporting provider registered with orrery.                                                                                    |
| `reporting[idx].config`   |          | The `orrery_api` config if set at pipeline level. Otherwise, the default `OrreryClientConfig`. See the [defaults](https://github.com/Dxlxz/orrery/blob/master/metadata-ingestion/src/orrery/ingestion/graph/client.py#L19) here. | The configuration required for initializing the orrery reporting provider.                                                                              |
| `reporting[idx].required` |          | `false`                                                                                                                                                                                                                                        | When false, configuration and reporting errors are treated as warnings rather than failing the run.                                                      |
| `pipeline_name`           | ✅       |                                                                                                                                                                                                                                                | The name of the ingestion pipeline. This is used as a part of the identifying key for the telemetry data reported by each job in the ingestion pipeline. |

#### Supported sources

- All sql based sources.
- snowflake_usage.

#### Sample configuration

```yaml
source:
  type: "snowflake"
  config:
    username: <user_name>
    password: <password>
    role: <role>
    host_port: <host_port>
    warehouse: <ware_house>
    # Rest of the source specific params ...
# This is mandatory. Changing it will cause old telemetry correlation to be lost.
pipeline_name: "my_snowflake_pipeline_1"

# Pipeline-level orrery_api configuration.
orrery_api: # Optional. But if provided, this config will be used by the "orrery" ingestion state provider.
  server: "http://localhost:8080"

sink:
  type: "orrery-rest"
  config:
    server: "http://localhost:8080"

reporting:
  - type: "orrery" # Required
    config: # Optional.
      orrery_api: # default value
        server: "http://localhost:8080"
```

## Reporting Ingestion State Provider (Developer Guide)

An ingestion reporting state provider is responsible for saving and retrieving the ingestion telemetry
associated with the ingestion runs of various jobs inside the source connector of the ingestion pipeline.
The data model used for capturing the telemetry is [OrreryIngestionRunSummary](https://github.com/Dxlxz/orrery/blob/master/metadata-models/src/main/pegasus/com/linkedin/datajob/orrery/OrreryIngestionRunSummary.pdl).
A reporting ingestion state provider needs to implement the IngestionReportingProviderBase.
interface and register itself with orrery by adding an entry under `orrery.ingestion.reporting_provider.plugins`
key of the entry_points section in [setup.py](https://github.com/Dxlxz/orrery/blob/master/metadata-ingestion/setup.py)
with its type and implementation class as shown below.

```python
entry_points = {
    # <snip other keys>"
    "orrery.ingestion.reporting_provider.plugins": [
        "orrery = orrery.ingestion.reporting.orrery_ingestion_run_summary_provider:OrreryIngestionRunSummaryProvider",
        "file = orrery.ingestion.reporting.file_reporter:FileReporter",
    ],
}
```

### Orrery Reporting Ingestion State Provider

This is the reporting state provider implementation that is available out of the box in orrery. Its type is `orrery` and it is implemented on top
of the `orrery_api` client and the timeseries aspect capabilities of the orrery-backend.

#### Config details

Note that a `.` is used to denote nested fields in the YAML recipe.

| Field    | Required | Default                                                                                                                                                                                                                                        | Description                                                                 |
| -------- | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| `type`   | ✅       | `orrery`                                                                                                                                                                                                                                      | The type of the ingestion reporting provider registered with orrery.       |
| `config` |          | The `orrery_api` config if set at pipeline level. Otherwise, the default `OrreryClientConfig`. See the [defaults](https://github.com/Dxlxz/orrery/blob/master/metadata-ingestion/src/orrery/ingestion/graph/client.py#L19) here. | The configuration required for initializing the orrery reporting provider. |
