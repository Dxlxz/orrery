# Copyright 2025 Orrery, Inc.
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

"""Orrery Agent Context - MCP Tools for AI Agents."""

from orrery_agent_context._version import __version__
from orrery_agent_context.context import (
    OrreryContext,
    get_graph,
    get_orrery_client,
    reset_client,
    set_client,
)
from orrery_agent_context.utils import create_context_wrapper

__all__ = [
    "__version__",
    "OrreryContext",
    "create_context_wrapper",
    "get_orrery_client",
    "get_graph",
    "set_client",
    "reset_client",
]
