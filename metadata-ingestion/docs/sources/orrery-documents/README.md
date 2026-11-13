## Overview

Orrery Documents is a documentation or collaboration platform. Learn more in the [official Orrery Documents documentation](https://orrery.dev/docs/).

The Orrery integration for Orrery Documents covers document/workspace entities and hierarchy context for knowledge assets. It also captures stateful deletion detection.

Process Document entities from Orrery and generate semantic embeddings for semantic search.

## Concept Mapping

While the specific concept mapping is still pending, this shows the generic concept mapping in Orrery.

| Source Concept                                           | Orrery Concept              | Notes                                                            |
| -------------------------------------------------------- | ---------------------------- | ---------------------------------------------------------------- |
| Platform/account/project scope                           | Platform Instance, Container | Organizes assets within the platform context.                    |
| Core technical asset (for example table/view/topic/file) | Dataset                      | Primary ingested technical asset.                                |
| Schema fields / columns                                  | SchemaField                  | Included when schema extraction is supported.                    |
| Ownership and collaboration principals                   | CorpUser, CorpGroup          | Emitted by modules that support ownership and identity metadata. |
| Dependencies and processing relationships                | Lineage edges                | Available when lineage extraction is supported and enabled.      |
