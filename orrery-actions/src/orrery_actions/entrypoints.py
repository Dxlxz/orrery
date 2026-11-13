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

import logging
import platform
import sys

import click
import stackprinter
from prometheus_client import start_http_server

import orrery_actions._version as actions_version
from orrery.cli.env_utils import get_boolean_env_variable
from orrery_actions.cli.actions import actions

logger = logging.getLogger(__name__)

# Configure logger.
BASE_LOGGING_FORMAT = (
    "[%(asctime)s] %(levelname)-8s {%(name)s:%(lineno)d} - %(message)s"
)
logging.basicConfig(format=BASE_LOGGING_FORMAT)

MAX_CONTENT_WIDTH = 120


class _BelowErrorFilter(logging.Filter):
    """Restrict a handler to DEBUG / INFO / WARNING so ERROR+ use a separate stream."""

    def filter(self, record: logging.LogRecord) -> bool:
        return record.levelno < logging.ERROR


@click.group(
    context_settings=dict(
        # Avoid truncation of help text.
        # See https://github.com/pallets/click/issues/486.
        max_content_width=MAX_CONTENT_WIDTH,
    )
)
@click.option(
    "--enable-monitoring",
    type=bool,
    is_flag=True,
    default=False,
    help="Enable prometheus monitoring endpoint. You can set the portnumber with --monitoring-port.",
)
@click.option(
    "--monitoring-port",
    type=int,
    default=8000,
    help="""Prometheus monitoring endpoint will be available on :<PORT>/metrics.
    To enable monitoring use the --enable-monitoring flag
    """,
)
@click.option("--debug/--no-debug", default=False)
@click.version_option(
    version=actions_version.nice_version_name(),
    prog_name=actions_version.__package_name__,
)
@click.option(
    "-dl",
    "--detect-memory-leaks",
    type=bool,
    is_flag=True,
    default=False,
    help="Run memory leak detection.",
)
@click.pass_context
def orrery_actions(
    ctx: click.Context,
    enable_monitoring: bool,
    monitoring_port: int,
    debug: bool,
    detect_memory_leaks: bool,
) -> None:
    # Insulate 'orrery_actions' and all child loggers from inadvertent changes to the
    # root logger by the external site packages that we import.
    # (Eg: https://github.com/reata/sqllineage/commit/2df027c77ea0a8ea4909e471dcd1ecbf4b8aeb2f#diff-30685ea717322cd1e79c33ed8d37903eea388e1750aa00833c33c0c5b89448b3R11
    #  changes the root logger's handler level to WARNING, causing any message below
    #  WARNING level to be dropped  after this module is imported, irrespective
    #  of the logger's logging level! The lookml source was affected by this).

    # 1. Create 'orrery' parent logger.
    orrery_logger = logging.getLogger("orrery_actions")
    # 2. Console: stdout for < ERROR, stderr for ERROR+ (single default StreamHandler would send all levels to stderr).
    formatter = logging.Formatter(BASE_LOGGING_FORMAT)
    stdout_handler = logging.StreamHandler(sys.stdout)
    stdout_handler.setLevel(logging.DEBUG)
    stdout_handler.addFilter(_BelowErrorFilter())
    stdout_handler.setFormatter(formatter)
    stderr_handler = logging.StreamHandler(sys.stderr)
    stderr_handler.setLevel(logging.ERROR)
    stderr_handler.setFormatter(formatter)
    orrery_logger.addHandler(stdout_handler)
    orrery_logger.addHandler(stderr_handler)
    # 3. Turn off propagation to the root handler.
    orrery_logger.propagate = False
    # 4. Adjust log-levels.
    if debug or get_boolean_env_variable("ORRERY_DEBUG", False):
        logging.getLogger().setLevel(logging.INFO)
        orrery_logger.setLevel(logging.DEBUG)
    else:
        logging.getLogger().setLevel(logging.WARNING)
        orrery_logger.setLevel(logging.INFO)
    if enable_monitoring:
        start_http_server(monitoring_port)
    # Setup the context for the memory_leak_detector decorator.
    ctx.ensure_object(dict)
    ctx.obj["detect_memory_leaks"] = detect_memory_leaks


def main(**kwargs):
    # This wrapper prevents click from suppressing errors.
    try:
        sys.exit(orrery_actions(standalone_mode=False, **kwargs))
    except click.exceptions.Abort:
        # Click already automatically prints an abort message, so we can just exit.
        sys.exit(1)
    except click.ClickException as error:
        error.show()
        sys.exit(1)
    except Exception as exc:
        logger.error(
            stackprinter.format(
                exc,
                line_wrap=MAX_CONTENT_WIDTH,
                truncate_vals=10 * MAX_CONTENT_WIDTH,
                suppressed_paths=[r"lib/python.*/site-packages/click/"],
                show_vals=False,
            )
        )
        logger.info(
            f"Orrery Actions version: {actions_version.__version__} at {actions_version.__file__}"
        )
        logger.info(
            f"Python version: {sys.version} at {sys.executable} on {platform.platform()}"
        )
        sys.exit(1)


orrery_actions.add_command(actions)
