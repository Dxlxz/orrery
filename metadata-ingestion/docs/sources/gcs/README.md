## Overview

Google Cloud Storage is a storage and lakehouse platform. Learn more in the [official Google Cloud Storage documentation](https://cloud.google.com/storage).

The Orrery integration for Google Cloud Storage covers file/lakehouse metadata entities such as datasets, paths, and containers. It also captures stateful deletion detection.

## Concept Mapping

| Source Concept                             | Orrery Concept                                                                           | Notes                |
| ------------------------------------------ | ----------------------------------------------------------------------------------------- | -------------------- |
| `"Google Cloud Storage"`                   | [Data Platform](https://docs.orrery.dev/docs/generated/metamodel/entities/dataplatform/) |                      |
| GCS object / Folder containing GCS objects | [Dataset](https://docs.orrery.dev/docs/generated/metamodel/entities/dataset/)            |                      |
| GCS bucket                                 | [Container](https://docs.orrery.dev/docs/generated/metamodel/entities/container/)        | Subtype `GCS bucket` |
| GCS folder                                 | [Container](https://docs.orrery.dev/docs/generated/metamodel/entities/container/)        | Subtype `Folder`     |
