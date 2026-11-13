# Copyright 2021 Orrery, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
import importlib
import json
import logging
import sys
from typing import Any, List, Optional, cast

from pydantic import BaseModel

from orrery.executor.dispatcher.default_dispatcher import DefaultDispatcher
from orrery.executor.execution.executor import Executor
from orrery.executor.execution.reporting_executor import (
    ReportingExecutor,
    ReportingExecutorConfig,
)
from orrery.executor.execution.task import TaskConfig
from orrery.executor.request.execution_request import ExecutionRequest
from orrery.executor.request.signal_request import SignalRequest
from orrery.metadata.schema_classes import MetadataChangeLogClass
from orrery.secret.orrery_secret_store import OrrerySecretStoreConfig
from orrery.secret.secret_store import SecretStoreConfig
from orrery_actions.action.action import Action
from orrery_actions.event.event_envelope import EventEnvelope
from orrery_actions.event.event_registry import METADATA_CHANGE_LOG_EVENT_V1_TYPE
from orrery_actions.pipeline.pipeline_context import PipelineContext

logger = logging.getLogger(__name__)

ORRERY_EXECUTION_REQUEST_ENTITY_NAME = "orreryExecutionRequest"
ORRERY_EXECUTION_REQUEST_INPUT_ASPECT_NAME = "orreryExecutionRequestInput"
ORRERY_EXECUTION_REQUEST_SIGNAL_ASPECT_NAME = "orreryExecutionRequestSignal"
APPLICATION_JSON_CONTENT_TYPE = "application/json"


def _is_importable(path: str) -> bool:
    return "." in path or ":" in path


def import_path(path: str) -> Any:
    """
    Import an item from a package, where the path is formatted as 'package.module.submodule.ClassName'
    or 'package.module.submodule:ClassName.classmethod'. The dot-based format assumes that the bit
    after the last dot is the item to be fetched. In cases where the item to be imported is embedded
    within another type, the colon-based syntax can be used to disambiguate.
    """
    assert _is_importable(path), "path must be in the appropriate format"

    if ":" in path:
        module_name, object_name = path.rsplit(":", 1)
    else:
        module_name, object_name = path.rsplit(".", 1)

    item = importlib.import_module(module_name)
    for attr in object_name.split("."):
        item = getattr(item, attr)
    return item


class ExecutorConfig(BaseModel):
    executor_id: Optional[str] = None
    task_configs: Optional[List[TaskConfig]] = None


# Listens to new Execution Requests & dispatches them to the appropriate handler.
class ExecutorAction(Action):
    @classmethod
    def create(cls, config_dict: dict, ctx: PipelineContext) -> "Action":
        config = ExecutorConfig.model_validate(config_dict or {})
        return cls(config, ctx)

    def __init__(self, config: ExecutorConfig, ctx: PipelineContext):
        self.ctx = ctx

        executors: List[Executor] = []

        executor_config = self._build_executor_config(config, ctx)
        executors.append(ReportingExecutor(executor_config))

        # Construct execution request dispatcher
        self.dispatcher = DefaultDispatcher(executors)

    def act(self, event: EventEnvelope) -> None:
        """This method listens for ExecutionRequest changes to execute in schedule and trigger events"""
        if event.event_type is METADATA_CHANGE_LOG_EVENT_V1_TYPE:
            orig_event = cast(MetadataChangeLogClass, event.event)
            if (
                orig_event.get("entityType") == ORRERY_EXECUTION_REQUEST_ENTITY_NAME
                and orig_event.get("changeType") == "UPSERT"
            ):
                if (
                    orig_event.get("aspectName")
                    == ORRERY_EXECUTION_REQUEST_INPUT_ASPECT_NAME
                ):
                    logger.debug("Received execution request input. Processing...")
                    self._handle_execution_request_input(orig_event)
                elif (
                    orig_event.get("aspectName")
                    == ORRERY_EXECUTION_REQUEST_SIGNAL_ASPECT_NAME
                ):
                    logger.debug("Received execution request signal. Processing...")
                    self._handle_execution_request_signal(orig_event)

    def _handle_execution_request_input(self, orig_event):
        entity_urn = orig_event.get("entityUrn")
        entity_key = orig_event.get("entityKeyAspect")

        # Get the run id to use.
        exec_request_id = None
        if entity_key is not None:
            exec_request_key = json.loads(
                entity_key.get("value")
            )  # this becomes the run id.
            exec_request_id = exec_request_key.get("id")
        elif entity_urn is not None:
            urn_parts = entity_urn.split(":")
            exec_request_id = urn_parts[len(urn_parts) - 1]

        # Decode the aspect json into something more readable :)
        exec_request_input = json.loads(orig_event.get("aspect").get("value"))

        # Build an Execution Request
        exec_request = ExecutionRequest(
            executor_id=exec_request_input.get("executorId"),
            exec_id=exec_request_id,
            name=exec_request_input.get("task"),
            args=exec_request_input.get("args"),
        )

        # Try to dispatch the execution request
        try:
            self.dispatcher.dispatch(exec_request)
        except Exception:
            logger.error("ERROR", exc_info=sys.exc_info())

    def _handle_execution_request_signal(self, orig_event):
        entity_urn = orig_event.get("entityUrn")

        if (
            orig_event.get("aspect").get("contentType") == APPLICATION_JSON_CONTENT_TYPE
            and entity_urn is not None
        ):
            # Decode the aspect json into something more readable :)
            signal_request_input = json.loads(orig_event.get("aspect").get("value"))

            # Build a Signal Request
            urn_parts = entity_urn.split(":")
            exec_id = urn_parts[len(urn_parts) - 1]
            signal_request = SignalRequest(
                executor_id=signal_request_input.get("executorId"),
                exec_id=exec_id,
                signal=signal_request_input.get("signal"),
            )

            # Try to dispatch the signal request
            try:
                self.dispatcher.dispatch_signal(signal_request)
            except Exception:
                logger.error("ERROR", exc_info=sys.exc_info())

    def _build_executor_config(
        self, config: ExecutorConfig, ctx: PipelineContext
    ) -> ReportingExecutorConfig:
        if config.task_configs:
            task_configs = config.task_configs
        else:
            # Build default task config
            task_configs = [
                TaskConfig(
                    name="RUN_INGEST",
                    type="orrery.executor.execution.sub_process_ingestion_task.SubProcessIngestionTask",
                    configs=dict({}),
                ),
                TaskConfig(
                    name="TEST_CONNECTION",
                    type="orrery.executor.execution.sub_process_test_connection_task.SubProcessTestConnectionTask",
                    configs={},
                ),
            ]

        if not ctx.graph:
            raise Exception(
                "Invalid configuration provided to action. Orrery Graph Client Required. Try including the 'orrery' block in your configuration."
            )

        graph = ctx.graph.graph

        # Build default executor config
        local_executor_config = ReportingExecutorConfig(
            id=config.executor_id or "default",
            task_configs=task_configs,
            secret_stores=[
                SecretStoreConfig(type="env", config=dict({})),
                SecretStoreConfig(
                    type="orrery",
                    # TODO: Once SecretStoreConfig is updated to accept arbitrary types
                    # and not just dicts, we can just pass in the OrrerySecretStoreConfig
                    # object directly.
                    config=OrrerySecretStoreConfig(graph_client=graph).model_dump(),
                ),
            ],
            graph_client=graph,
        )

        return local_executor_config

    def close(self) -> None:
        # TODO: Handle closing action ingestion processing.
        pass
