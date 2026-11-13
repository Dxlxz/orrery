"""Shared document import mode for context document ingestion sources."""

from orrery.utilities.str_enum import StrEnum


class DocumentImportMode(StrEnum):
    """Whether ingested documents are native (editable) or external (read-only references)."""

    NATIVE = "NATIVE"
    EXTERNAL = "EXTERNAL"
