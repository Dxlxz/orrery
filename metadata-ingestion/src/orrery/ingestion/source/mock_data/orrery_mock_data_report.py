from dataclasses import dataclass, field
from typing import Optional

from orrery.ingestion.api.source import SourceReport


@dataclass
class OrreryMockDataReport(SourceReport):
    first_urn_seen: Optional[str] = field(
        default=None,
        metadata={"description": "The first URN encountered during ingestion"},
    )
