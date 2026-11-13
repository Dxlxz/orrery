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

__package_name__ = "orrery-agent-context"
__version__ = "0.1.0.dev0"
# Stable orrery release this package is built and tested against. The CLI
# version is borrowed from the server version and does not follow semantic
# versioning, so we pin a known-good stable release rather than self-pinning to
# this package's own (server-derived) version. Bump explicitly when newer CLI
# functionality is required.
__orrery_orrery_pin__ = "1.6.0.6"
