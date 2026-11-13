## Overview

Dremio is a Orrery utility or metadata-focused integration.

The Orrery integration for Dremio covers metadata entities and operational objects relevant to this connector. It also captures table- and column-level lineage, usage statistics, data profiling, ownership, and stateful deletion detection.

## Concept Mapping

| Source Concept             | Orrery Concept | Notes                                                      |
| -------------------------- | --------------- | ---------------------------------------------------------- |
| **Physical Dataset/Table** | `Dataset`       | Subtype: `Table`                                           |
| **Virtual Dataset/Views**  | `Dataset`       | Subtype: `View`                                            |
| **Spaces**                 | `Container`     | Mapped to Orrery’s `Container` aspect. Subtype: `Space`   |
| **Folders**                | `Container`     | Mapped as a `Container` in Orrery. Subtype: `Folder`      |
| **Sources**                | `Container`     | Represented as a `Container` in Orrery. Subtype: `Source` |
