## Overview

Salesforce is a Orrery utility or metadata-focused integration. Learn more in the [official Salesforce documentation](https://www.salesforce.com/).

The Orrery integration for Salesforce covers metadata entities and operational objects relevant to this connector. It also captures table-level lineage, data profiling, tags, and stateful deletion detection.

## Concept Mapping

| Source Concept  | Orrery Concept                                           | Notes                     |
| --------------- | --------------------------------------------------------- | ------------------------- |
| `Salesforce`    | [Data Platform](../../metamodel/entities/dataPlatform.md) |                           |
| Standard Object | [Dataset](../../metamodel/entities/dataset.md)            | subtype "Standard Object" |
| Custom Object   | [Dataset](../../metamodel/entities/dataset.md)            | subtype "Custom Object"   |
