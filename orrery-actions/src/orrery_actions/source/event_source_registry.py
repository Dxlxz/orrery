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

from orrery.ingestion.api.registry import PluginRegistry
from orrery_actions.plugin.source.kafka.kafka_event_source import KafkaEventSource
from orrery_actions.plugin.source.orrery.orrery_cloud_event_source import (
    OrreryEventSource,
)
from orrery_actions.plugin.source.pgqueue.pg_queue_event_source import (
    PgQueueEventSource,
)
from orrery_actions.source.event_source import EventSource

event_source_registry = PluginRegistry[EventSource]()
event_source_registry.register_from_entrypoint("orrery_actions.source.plugins")
event_source_registry.register("kafka", KafkaEventSource)
event_source_registry.register("pg_queue", PgQueueEventSource)
event_source_registry.register("orrery-cloud", OrreryEventSource)
