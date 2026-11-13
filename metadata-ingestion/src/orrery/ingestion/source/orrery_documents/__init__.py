"""Orrery Documents Source - Process Document entities and generate embeddings."""

from orrery.ingestion.source.orrery_documents.orrery_documents_config import (
    OrreryDocumentsSourceConfig,
)
from orrery.ingestion.source.orrery_documents.orrery_documents_source import (
    OrreryDocumentsSource,
)

__all__ = ["OrreryDocumentsSource", "OrreryDocumentsSourceConfig"]
