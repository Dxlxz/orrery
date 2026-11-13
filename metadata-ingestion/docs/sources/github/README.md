## Overview

GitHub hosts source files, documentation, and knowledge bases in git repositories. Learn more in the [official GitHub documentation](https://docs.github.com/).

The Orrery `github-documents` integration ingests markdown and text files from a GitHub repository as Document entities. Folder structure in the repository is preserved as parent-child document relationships in Orrery.

## Concept Mapping

| Source Concept       | Orrery Concept | Notes                                                          |
| -------------------- | --------------- | -------------------------------------------------------------- |
| Repository folder    | Document        | Optional folder documents for navigation.                      |
| Markdown / text file | Document        | Native (editable) or external (read-only) depending on config. |
| Repository + branch  | Custom metadata | Stored on documents for traceability.                          |
