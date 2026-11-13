import json
import logging

import click
from click_default_group import DefaultGroup

from orrery.ingestion.graph.client import get_default_graph
from orrery.ingestion.graph.config import ClientMode
from orrery.upgrade import upgrade

logger = logging.getLogger(__name__)


@click.group(cls=DefaultGroup, default="urn")
def state() -> None:
    """Managed state stored in Orrery by stateful ingestion."""
    pass


@state.command()
@click.option("--pipeline-name", required=True, type=str)
@click.option("--platform", required=True, type=str)
@upgrade.check_upgrade
def inspect(pipeline_name: str, platform: str) -> None:
    """
    Get the latest stateful ingestion state for a given pipeline.
    Only works for state entity removal for now.
    """

    orrery_graph = get_default_graph(ClientMode.CLI)
    checkpoint = orrery_graph.get_latest_pipeline_checkpoint(pipeline_name, platform)
    if not checkpoint:
        click.secho("No ingestion state found.", fg="red")
        exit(1)

    logger.info(f"Found ingestion state with {len(checkpoint.state.urns)} URNs.")
    click.echo(json.dumps(checkpoint.state.urns, indent=2))
