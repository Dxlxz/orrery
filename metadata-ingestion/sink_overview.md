---
description: "Overview of metadata sinks in Orrery ingestion, the destinations where extracted metadata events are written."
---

# Sinks

Sinks are **destinations for metadata**.

<p align="center">
  <img width="70%"  src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/sources-sinks.png"/>
</p>

In general, the sink will be defined in the [recipe](./recipe_overview.md) after the [source](./source_overview.md) like below.

```yaml
source: ...

sink:
  type: <sink_type>
  config: ...
```

## Types of Sinks

When configuring ingestion for Orrery, you're likely to be sending the metadata to Orrery over either one of the following.

- [REST (orrery-rest)](sink_docs/orrery.md#orrery-rest)
- [Kafka (orrery-kafka)](sink_docs/orrery.md#orrery-kafka)

For debugging purposes or troubleshooting, the following sinks can be useful:

- [Metadata File](sink_docs/metadata-file.md)
- [Console](sink_docs/console.md)

## Default Sink

Since `orrery` version `>=0.8.33.2`, the default sink is assumed to be a `orrery-rest` endpoint.
