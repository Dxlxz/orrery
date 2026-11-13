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

"""
Python wrapper for Orrery test connection with secret masking support.

Reads a JSON envelope from stdin containing __recipe_yaml__, __secrets__,
and __report_out_file__. Forwards to `orrery ingest -c - --test-source-connection`
via stdin. Secrets never touch environment variables or disk.
"""

import json
import sys
from pathlib import Path

from orrery.executor.execution.wrapper_common import (
    activate_venv,
    build_orrery_stdin,
    check_cli_flag_support,
    read_stdin_envelope,
    register_secrets_for_masking,
    run_orrery_subprocess,
    setup_memory_limit,
    supports_stdin_envelope,
    validate_venv,
)


def create_unsupported_report(report_out_file: str) -> None:
    """Create a report indicating test-source-connection is not supported."""
    report = {
        "internal_failure": True,
        "internal_failure_reason": (
            "orrery library doesn't have test_connection feature. "
            "You are likely running an old version."
        ),
    }

    with open(report_out_file, "w") as f:
        json.dump(report, f, indent=2)

    print(
        "orrery ingest doesn't seem to have test_connection feature. "
        "You are likely running an old version",
        file=sys.stderr,
    )


def main():
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <venv_path>", file=sys.stderr)
        sys.exit(1)

    venv_path = sys.argv[1]

    raw_envelope, envelope = read_stdin_envelope()
    recipe_yaml = envelope["__recipe_yaml__"]
    secrets = envelope.get("__secrets__", {})
    report_out_file = envelope["__report_out_file__"]

    venv_python, venv_orrery = validate_venv(venv_path)
    activate_venv(venv_path)
    setup_memory_limit()
    register_secrets_for_masking(secrets)

    has_test_connection = check_cli_flag_support(venv_orrery, "test-source-connection")
    if not has_test_connection:
        create_unsupported_report(report_out_file)
        sys.exit(0)

    print(
        "This version of orrery supports test-source-connection functionality",
        file=sys.stderr,
    )
    report_path = Path(report_out_file)
    if report_path.exists():
        report_path.unlink()

    # Forward the envelope so the CLI registers the secrets for its own
    # masking; older CLIs get the recipe pre-resolved.
    if supports_stdin_envelope(venv_python):
        orrery_stdin = raw_envelope
    else:
        print(
            "Warning: venv CLI predates the stdin secrets envelope; "
            "its own output will not be masked",
            file=sys.stderr,
        )
        orrery_stdin = build_orrery_stdin(recipe_yaml, secrets)

    cmd = [
        str(venv_orrery),
        "ingest",
        "-c",
        "-",
        "--test-source-connection",
        "--report-to",
        report_out_file,
    ]

    sys.exit(run_orrery_subprocess(cmd, orrery_stdin))


if __name__ == "__main__":
    main()
