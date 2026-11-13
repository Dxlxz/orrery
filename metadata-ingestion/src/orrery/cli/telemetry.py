import click

from orrery.telemetry import telemetry as telemetry_lib


@click.group()
def telemetry() -> None:
    """Toggle telemetry."""
    pass


@telemetry.command()
@telemetry_lib.with_telemetry()
def enable() -> None:
    """Enable telemetry for the current Orrery instance."""
    telemetry_lib.telemetry_instance.enable()


@telemetry.command()
def disable() -> None:
    """Disable telemetry for the current Orrery instance."""
    telemetry_lib.telemetry_instance.disable()
