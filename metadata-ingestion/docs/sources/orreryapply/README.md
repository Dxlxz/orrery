## Overview

Orrery Apply is a Orrery utility or metadata-focused integration. Learn more in the [official Orrery Apply documentation](https://orrery.dev/docs/).

The Orrery integration for Orrery Apply covers metadata entities and operational objects relevant to this connector. It applies metadata from YAML/JSON files to existing Orrery entities.

## Concept Mapping

| Source Concept                             | Orrery Concept                                      | Notes                                                    |
| ------------------------------------------ | ---------------------------------------------------- | -------------------------------------------------------- |
| Apply operation input                      | Metadata Change Proposal (MCP) updates               | Input drives metadata updates rather than discovery.     |
| Asset target list                          | Dataset / Container (and other supported entities)   | Targets are selected explicitly in recipe configuration. |
| Ownership / domain / tag / term assignment | Ownership, Domain, GlobalTags, GlossaryTerms aspects | Applied directly to existing Orrery entities.           |
