from orrery.ingestion.api.common import PipelineContext
from orrery.ingestion.source.mock_data.orrery_mock_data import (
    LineageConfigGen1,
    OrreryMockDataConfig,
    OrreryMockDataSource,
)


class TestOrreryMockDataReport:
    def test_first_urn_capture(self):
        """Test that the first URN is captured correctly when using the source."""
        config = OrreryMockDataConfig(gen_1=LineageConfigGen1(enabled=True))
        config.gen_1.emit_lineage = True
        config.gen_1.lineage_fan_out = 1
        config.gen_1.lineage_hops = 0  # Only one table to make testing easier

        ctx = PipelineContext(run_id="test")
        source = OrreryMockDataSource(ctx, config)

        assert source.report.first_urn_seen is None

        workunits = list(source.get_workunits())

        assert len(workunits) > 0

        assert source.report.first_urn_seen is not None
        assert source.report.first_urn_seen.startswith(
            "urn:li:dataset:(urn:li:dataPlatform:fake,"
        )

    def test_no_urns_when_lineage_disabled(self):
        """Test that no URNs are captured when lineage is disabled."""
        config = OrreryMockDataConfig(gen_1=LineageConfigGen1(enabled=False))
        config.gen_1.emit_lineage = False

        ctx = PipelineContext(run_id="test")
        source = OrreryMockDataSource(ctx, config)

        assert source.report.first_urn_seen is None

        workunits = list(source.get_workunits())

        assert len(workunits) == 0

        assert source.report.first_urn_seen is None
