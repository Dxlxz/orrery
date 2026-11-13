import pytest
from click.testing import CliRunner


def test_agent_shim_command_behavior():
    """Test that the agent shim command displays the correct error message."""
    from orrery.cli.cli_utils import make_shim_command

    shim_command = make_shim_command("agent", "run `pip install orrery-agent-context`")

    runner = CliRunner()
    result = runner.invoke(shim_command, [])

    assert result.exit_code == 1
    assert "missing dependencies" in result.output
    assert "run `pip install orrery-agent-context`" in result.output


def test_agent_command_exists():
    """Test that agent command is registered in the CLI."""
    import orrery.entrypoints

    # Verify agent command was added (either real or shim)
    assert "agent" in orrery.entrypoints.orrery.devmands


def test_agent_command_shows_error_or_help():
    """Test that agent command either works or shows helpful error."""
    import orrery.entrypoints

    runner = CliRunner()
    result = runner.invoke(orrery.entrypoints.orrery, ["agent", "--help"])

    # Either the command works (exit code 0) or it's a shim (exit code 1 with helpful message)
    if result.exit_code == 1:
        assert "missing dependencies" in result.output
        assert "pip install orrery-agent-context" in result.output
    else:
        # Real command should show help
        assert result.exit_code == 0


@pytest.mark.parametrize(
    "command_name",
    ["agent", "actions", "lite"],
)
def test_optional_commands_exist(command_name):
    """Test that optional commands (agent, actions, lite) are always registered."""
    import orrery.entrypoints

    # Verify the command exists in the CLI
    assert command_name in orrery.entrypoints.orrery.devmands


@pytest.mark.parametrize(
    "command_name,install_message",
    [
        ("agent", "pip install orrery-agent-context"),
        ("actions", "pip install orrery-actions"),
        ("lite", "pip install 'orrery[orrery-lite]'"),
    ],
)
def test_shim_commands_show_helpful_error(command_name, install_message):
    """Test that shim commands created by make_shim_command show helpful error messages."""
    from orrery.cli.cli_utils import make_shim_command

    shim_command = make_shim_command(command_name, f"run `{install_message}`")

    runner = CliRunner()
    result = runner.invoke(shim_command, [])

    assert result.exit_code == 1
    assert "missing dependencies" in result.output
    assert install_message in result.output
