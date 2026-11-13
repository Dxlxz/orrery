## Overview

Kafka Connect is a streaming or integration platform. Learn more in the [official Kafka Connect documentation](https://kafka.apache.org/documentation/#connect).

The Orrery integration for Kafka Connect covers streaming/integration entities such as topics, connectors, pipelines, or jobs. It also captures table-level lineage and stateful deletion detection.

## Concept Mapping

| Source Concept                                                                  | Orrery Concept                                                                           | Notes |
| ------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- | ----- |
| `"kafka-connect"`                                                               | [Data Platform](https://docs.orrery.dev/docs/generated/metamodel/entities/dataplatform/) |       |
| [Connector](https://kafka.apache.org/documentation/#connect_connectorsandtasks) | [DataFlow](https://docs.orrery.dev/docs/generated/metamodel/entities/dataflow/)          |       |
| Kafka Topic                                                                     | [Dataset](https://docs.orrery.dev/docs/generated/metamodel/entities/dataset/)            |       |
