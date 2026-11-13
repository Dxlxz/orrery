import logging
from pathlib import Path

import click
from click_default_group import DefaultGroup

from orrery.api.entities.corpgroup.corpgroup import (
    CorpGroup,
    CorpGroupGenerationConfig,
)
from orrery.cli.specific.file_loader import load_file
from orrery.ingestion.graph.client import get_default_graph
from orrery.ingestion.graph.config import ClientMode
from orrery.upgrade import upgrade

logger = logging.getLogger(__name__)


@click.group(cls=DefaultGroup, default="upsert")
def group() -> None:
    """A group of commands to interact with the Group entity in Orrery."""
    pass


@group.command(
    name="upsert",
)
@click.option("-f", "--file", required=True, type=click.Path(exists=True))
@click.option(
    "--override-editable",
    required=False,
    is_flag=True,
    default=False,
    help="When set, writes to the editable section of the metadata graph, overwriting writes from the UI",
)
@upgrade.check_upgrade
def upsert(file: Path, override_editable: bool) -> None:
    """Create or Update a Group with embedded Users"""

    config_dict = load_file(file)
    group_configs = config_dict if isinstance(config_dict, list) else [config_dict]
    with get_default_graph(ClientMode.CLI) as emitter:
        for group_config in group_configs:
            try:
                orrery_group = CorpGroup.model_validate(group_config)
                for mcp in orrery_group.generate_mcp(
                    generation_config=CorpGroupGenerationConfig(
                        override_editable=override_editable, orrery_graph=emitter
                    )
                ):
                    emitter.emit(mcp)
                click.secho(
                    f"Update succeeded for group {orrery_group.urn}.", fg="green"
                )
            except Exception as e:
                click.secho(
                    f"Update failed for id {group_config.get('id')}. due to {e}",
                    fg="red",
                )
