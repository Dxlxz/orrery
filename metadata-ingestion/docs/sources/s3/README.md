## Overview

Amazon S3 is a storage and lakehouse platform. Learn more in the [official Amazon S3 documentation](https://aws.amazon.com/s3/).

The Orrery integration for Amazon S3 covers file/lakehouse metadata entities such as datasets, paths, and containers. It also captures data profiling, tags, and stateful deletion detection.

## Concept Mapping

| Source Concept                           | Orrery Concept                                                                           | Notes               |
| ---------------------------------------- | ----------------------------------------------------------------------------------------- | ------------------- |
| `"s3"`                                   | [Data Platform](https://docs.orrery.dev/docs/generated/metamodel/entities/dataplatform/) |                     |
| s3 object / Folder containing s3 objects | [Dataset](https://docs.orrery.dev/docs/generated/metamodel/entities/dataset/)            |                     |
| s3 bucket                                | [Container](https://docs.orrery.dev/docs/generated/metamodel/entities/container/)        | Subtype `S3 bucket` |
| s3 folder                                | [Container](https://docs.orrery.dev/docs/generated/metamodel/entities/container/)        | Subtype `Folder`    |
