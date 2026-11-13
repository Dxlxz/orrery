import logging

import click

from orrery.cli.specific.agent_cli import register, upsert

logger = logging.getLogger(__name__)


@click.group()
def agent() -> None:
    """Helper commands for Creating and managing Agent on Orrery."""
    pass


agent.add_command(register)
agent.add_command(upsert)


@agent.group()
def create() -> None:
    """Create an agent on Orrery."""
    pass


@create.command(name="snowflake")
@click.option(
    "--sf-account",
    default=None,
    help="Snowflake account identifier (auto-detected if not provided)",
)
@click.option(
    "--sf-user",
    default=None,
    help="Snowflake user name (auto-detected if not provided)",
)
@click.option(
    "--sf-role",
    default=None,
    help="Snowflake role (auto-detected if not provided)",
)
@click.option(
    "--sf-warehouse",
    default=None,
    help="Snowflake warehouse name (auto-detected if not provided)",
)
@click.option(
    "--sf-database",
    default=None,
    help="Snowflake database name (auto-detected if not provided)",
)
@click.option(
    "--sf-schema",
    default=None,
    help="Snowflake schema name (auto-detected if not provided)",
)
@click.option("--orrery-url", required=True, help="Orrery instance URL")
@click.option("--orrery-token", required=True, help="Orrery Personal Access Token")
@click.option(
    "--agent-name", default="ORRERY_SQL_AGENT", help="Agent name in Snowflake"
)
@click.option(
    "--agent-display-name",
    default="Orrery SQL Assistant",
    help="Agent display name in Snowflake UI",
)
@click.option(
    "--agent-color",
    default="blue",
    help="Agent color in Snowflake UI",
    type=click.Choice(["blue", "red", "green", "yellow", "purple", "orange"]),
)
@click.option(
    "--output-dir",
    default="./snowflake_setup",
    help="Output directory for generated SQL files",
)
@click.option(
    "--execute",
    is_flag=True,
    default=False,
    help="Connect to Snowflake and execute the SQL scripts directly",
)
@click.option(
    "--sf-password",
    help="Snowflake password (required if --execute is used with password authentication)",
)
@click.option(
    "--sf-authenticator",
    default="snowflake",
    type=click.Choice(["snowflake", "externalbrowser", "oauth"], case_sensitive=False),
    help="Authentication method: 'snowflake' (password), 'externalbrowser' (SSO), or 'oauth' (token-based). Default: snowflake",
)
@click.option(
    "--enable-mutations/--no-enable-mutations",
    default=True,
    help="Include mutation/write tools (tags, descriptions, owners, etc.). Default: enabled",
)
@click.option(
    "--enable-cloud/--no-enable-cloud",
    default=False,
    help="Include Cloud-only tools (Ask Orrery AI chat). Requires Orrery Cloud. Default: disabled",
)
def create_snowflake(
    sf_account: str | None,
    sf_user: str | None,
    sf_role: str | None,
    sf_warehouse: str | None,
    sf_database: str | None,
    sf_schema: str | None,
    orrery_url: str,
    orrery_token: str,
    agent_name: str,
    agent_display_name: str,
    agent_color: str,
    output_dir: str,
    execute: bool,
    sf_password: str | None,
    sf_authenticator: str,
    enable_mutations: bool,
    enable_cloud: bool,
) -> None:
    """Create a Snowflake agent on Orrery.

    Snowflake connection parameters (account, user, role, warehouse, database, schema)
    will be auto-detected from your current session if not provided explicitly.

    Authentication methods:
    - snowflake (default): Standard password authentication
    - externalbrowser: SSO authentication via browser (no password required)
    - oauth: OAuth token-based authentication

    Examples:
        # Generate SQL files only (default)
        $ orrery agent create snowflake --orrery-url=... --orrery-token=...

        # Execute with password authentication
        $ orrery agent create snowflake --execute --sf-password=secret ...

        # Execute with SSO authentication (browser-based)
        $ orrery agent create snowflake --execute --sf-authenticator=externalbrowser ...
    """
    from orrery_agent_context.snowflake.snowflake import create_snowflake_agent

    ctx = click.get_current_context()
    ctx.invoke(
        create_snowflake_agent,
        sf_account=sf_account,
        sf_user=sf_user,
        sf_role=sf_role,
        sf_warehouse=sf_warehouse,
        sf_database=sf_database,
        sf_schema=sf_schema,
        orrery_url=orrery_url,
        orrery_token=orrery_token,
        agent_name=agent_name,
        agent_display_name=agent_display_name,
        agent_color=agent_color,
        output_dir=output_dir,
        execute=execute,
        sf_password=sf_password,
        sf_authenticator=sf_authenticator,
        enable_mutations=enable_mutations,
        enable_cloud=enable_cloud,
    )
