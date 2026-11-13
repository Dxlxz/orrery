#!/usr/bin/env python3
"""
Orrery Ingestion Source for RDF.

This module provides a Orrery ingestion source that allows RDF to be used
as a native Orrery ingestion plugin.
"""

from orrery.ingestion.source.rdf.ingestion.rdf_source import (
    RDFSource,
    RDFSourceConfig,
    RDFSourceReport,
)

__all__ = ["RDFSource", "RDFSourceConfig", "RDFSourceReport"]
