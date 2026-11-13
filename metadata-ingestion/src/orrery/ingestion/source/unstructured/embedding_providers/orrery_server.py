from typing import List

from orrery.ingestion.graph.client import OrreryGraph
from orrery.ingestion.graph.embeddings import OrreryEmbeddingsClient
from orrery.ingestion.source.unstructured.chunking_config import (
    EmbeddingConfig,
    ResolvedEmbeddingTarget,
)
from orrery.ingestion.source.unstructured.embedding_providers.base import (
    DEFAULT_HTTP_TIMEOUT_SECONDS,
    EmbeddingProvider,
    EmbeddingResult,
    EmbeddingTask,
)
from orrery.ingestion.source.unstructured.embedding_target import (
    ServerEmbeddingModelMismatchError,
    resolve_embedding_target,
)


class OrreryServerEmbeddingProvider(EmbeddingProvider):
    def __init__(
        self, graph: OrreryGraph, *, target: ResolvedEmbeddingTarget, timeout: float
    ) -> None:
        assert target.endpoint_path is not None
        self.target = target
        self.model_embedding_key = target.model_embedding_key
        self.model_id = target.model_id
        self._client = OrreryEmbeddingsClient(
            graph, path=target.endpoint_path, timeout=timeout
        )

    @property
    def semantic_model_version(self) -> str:
        return self.target.semantic_model_version

    def embed(self, texts: List[str], *, task: EmbeddingTask) -> EmbeddingResult:
        if not texts:
            return EmbeddingResult(embeddings=[])
        response = self._client.embed(texts, task_type=task.value)
        if response.model != self.target.model_id:
            raise ServerEmbeddingModelMismatchError(
                "Orrery embeddings model differs from the semantic index model "
                "(appConfig.semanticSearchConfig.embeddingConfig.modelId). "
                "Ask your administrator to align the models before embedding."
            )
        return EmbeddingResult(embeddings=response.embeddings)

    def close(self) -> None:
        self._client.close()


def server_embedding_provider(
    graph: OrreryGraph, *, timeout: float = DEFAULT_HTTP_TIMEOUT_SECONDS
) -> OrreryServerEmbeddingProvider:
    target = resolve_embedding_target(EmbeddingConfig(provider="orrery"), graph)
    return OrreryServerEmbeddingProvider(graph, target=target, timeout=timeout)
