## Overview

Fivetran is a streaming or integration platform. Learn more in the [official Fivetran documentation](https://www.fivetran.com/).

The Orrery integration for Fivetran covers streaming/integration entities such as topics, connectors, pipelines, or jobs. It also captures column-level lineage and stateful deletion detection.

## Concept Mapping

| Fivetran        | Orrery                                                                                               |
| --------------- | ----------------------------------------------------------------------------------------------------- |
| `Connector`     | [DataJob](https://docs.orrery.dev/docs/generated/metamodel/entities/datajob/)                        |
| `Source`        | [Dataset](https://docs.orrery.dev/docs/generated/metamodel/entities/dataset/)                        |
| `Destination`   | [Dataset](https://docs.orrery.dev/docs/generated/metamodel/entities/dataset/)                        |
| `Connector Run` | [DataProcessInstance](https://docs.orrery.dev/docs/generated/metamodel/entities/dataprocessinstance) |

Source and destination are mapped to Dataset as an Input and Output of Connector.
