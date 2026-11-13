import logging
import os
from typing import Union

from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.ingestion.api.common import RecordEnvelope
from orrery.ingestion.api.sink import Sink, SinkReport, WriteCallback
from orrery.lite.lite_util import LiteLocalConfig, get_orrery_lite
from orrery.metadata.com.linkedin.pegasus2avro.mxe import (
    MetadataChangeEvent,
    MetadataChangeProposal,
)

logger = logging.getLogger(__name__)


class OrreryLiteSinkConfig(LiteLocalConfig):
    type: str = "sqlite"
    config: dict = {"file": os.path.expanduser("~/.orrery/lite/orrery.db")}


class OrreryLiteSink(Sink[OrreryLiteSinkConfig, SinkReport]):
    def __post_init__(self) -> None:
        self.orrery_lite = get_orrery_lite(self.config.model_dump())

    def write_record_async(
        self,
        record_envelope: RecordEnvelope[
            Union[
                MetadataChangeEvent,
                MetadataChangeProposal,
                MetadataChangeProposalWrapper,
            ]
        ],
        write_callback: WriteCallback,
    ) -> None:
        record = record_envelope.record
        if not isinstance(record, (MetadataChangeEvent, MetadataChangeProposalWrapper)):
            self.report.report_warning(f"orrery-local does not support {type(record)}")
            return

        try:
            self.orrery_lite.write(record)
            self.report.report_record_written(record_envelope)
        except Exception as e:
            self.report.report_failure(f"{record_envelope.metadata}: {type(e)}: {e}")
            if write_callback:
                write_callback.on_failure(record_envelope, e, {})
        else:
            if write_callback:
                write_callback.on_success(record_envelope, success_metadata={})

    def close(self):
        if self.orrery_lite:
            self.orrery_lite.close()
