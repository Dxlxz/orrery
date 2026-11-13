## Overview

SQL Queries is a Orrery utility or metadata-focused integration. Learn more in the [official SQL Queries documentation](https://orrery.dev/docs/).

The Orrery integration for SQL Queries covers metadata entities and operational objects relevant to this connector. It also captures table- and column-level lineage.

## Concept Mapping

While the specific concept mapping is still pending, this shows the generic concept mapping in Orrery.

| Source Concept                                           | Orrery Concept              | Notes                                                            |
| -------------------------------------------------------- | ---------------------------- | ---------------------------------------------------------------- |
| Platform/account/project scope                           | Platform Instance, Container | Organizes assets within the platform context.                    |
| Core technical asset (for example table/view/topic/file) | Dataset                      | Primary ingested technical asset.                                |
| Schema fields / columns                                  | SchemaField                  | Included when schema extraction is supported.                    |
| Ownership and collaboration principals                   | CorpUser, CorpGroup          | Emitted by modules that support ownership and identity metadata. |
| Dependencies and processing relationships                | Lineage edges                | Available when lineage extraction is supported and enabled.      |
