"""ThoughtSpot Orrery connector."""

from orrery.ingestion.source.thoughtspot.config import (
    ThoughtSpotConfig,
    ThoughtSpotConnectionConfig,
)
from orrery.ingestion.source.thoughtspot.source import ThoughtSpotSource

__all__ = ["ThoughtSpotSource", "ThoughtSpotConfig", "ThoughtSpotConnectionConfig"]
