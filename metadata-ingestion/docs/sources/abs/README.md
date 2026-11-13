## Overview

Azure Blob Storage is a storage and lakehouse platform. Learn more in the [official Azure Blob Storage documentation](https://learn.microsoft.com/azure/storage/blobs/storage-blobs-introduction).

The Orrery integration for Azure Blob Storage covers file/lakehouse metadata entities such as datasets, paths, and containers. It also captures data profiling, tags, and stateful deletion detection.

## Concept Mapping

| Source Concept                         | Orrery Concept                                                                           | Notes            |
| -------------------------------------- | ----------------------------------------------------------------------------------------- | ---------------- |
| `"abs"`                                | [Data Platform](https://docs.orrery.dev/docs/generated/metamodel/entities/dataplatform/) |                  |
| abs blob / Folder containing abs blobs | [Dataset](https://docs.orrery.dev/docs/generated/metamodel/entities/dataset/)            |                  |
| abs container                          | [Container](https://docs.orrery.dev/docs/generated/metamodel/entities/container/)        | Subtype `Folder` |
