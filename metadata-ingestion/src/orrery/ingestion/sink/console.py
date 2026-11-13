import logging

from orrery.configuration.common import ConfigModel
from orrery.ingestion.api.common import RecordEnvelope
from orrery.ingestion.api.sink import Sink, SinkReport, WriteCallback

logger = logging.getLogger(__name__)


class ConsoleSink(Sink[ConfigModel, SinkReport]):
    def write_record_async(
        self, record_envelope: RecordEnvelope, write_callback: WriteCallback
    ) -> None:
        print(f"{record_envelope}")
        if write_callback:
            self.report.report_record_written(record_envelope)
            write_callback.on_success(record_envelope, {})
