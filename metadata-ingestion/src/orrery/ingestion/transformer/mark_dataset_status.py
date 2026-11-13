from typing import Optional, cast

import orrery.emitter.mce_builder as builder
from orrery.configuration.common import ConfigModel
from orrery.ingestion.api.common import PipelineContext
from orrery.ingestion.transformer.dataset_transformer import DatasetStatusTransformer
from orrery.metadata.schema_classes import StatusClass


class MarkDatasetStatusConfig(ConfigModel):
    removed: bool


class MarkDatasetStatus(DatasetStatusTransformer):
    """Transformer that marks status of each dataset."""

    ctx: PipelineContext
    config: MarkDatasetStatusConfig

    def __init__(self, config: MarkDatasetStatusConfig, ctx: PipelineContext):
        super().__init__()
        self.ctx = ctx
        self.config = config

    @classmethod
    def create(cls, config_dict: dict, ctx: PipelineContext) -> "MarkDatasetStatus":
        config = MarkDatasetStatusConfig.model_validate(config_dict)
        return cls(config, ctx)

    def transform_aspect(
        self, entity_urn: str, aspect_name: str, aspect: Optional[builder.Aspect]
    ) -> Optional[builder.Aspect]:
        assert aspect is None or isinstance(aspect, StatusClass)
        status_aspect: StatusClass = aspect or StatusClass(removed=None)
        status_aspect.removed = self.config.removed
        return cast(Optional[builder.Aspect], status_aspect)
