# Snowflake Cortex Agents

Give [Snowflake Cortex Agents](https://www.snowflake.com/en/developers/guides/getting-started-with-snowflake-intelligence) access to your enterprise data context in Orrery — business definitions, ownership, lineage, and quality signals — so they can generate better SQL and answer data questions accurately.

Snowflake connects to the [Orrery MCP Server](../../features/feature-guides/mcp.md) as an **External MCP Server**, using OAuth2 with [Dynamic Client Registration (DCR)](https://datatracker.ietf.org/doc/html/rfc7591). Each user signs in with their own Orrery credentials (including SSO), and tokens are scoped per-user and refreshed automatically.

:::note
Snowflake Intelligence was renamed **Snowflake CoWork** in June 2026. The Cortex Agent and MCP connector objects are unchanged — only the end-user interface was rebranded.
:::

## Prerequisites

- Orrery Cloud v1.0.2+ (required for OAuth2 + DCR on the MCP endpoint)
- A Snowflake account with Cortex Agents / Snowflake CoWork enabled
- A Snowflake user with the `ACCOUNTADMIN` role for the initial setup. `CREATE API INTEGRATION` requires it; the MCP server object needs `CREATE EXTERNAL MCP SERVER` on the target schema, which by default only account admins hold.
- A database and schema to hold the external MCP server object

## Setup

### 1. Create the API integration

Run as `ACCOUNTADMIN`:

```sql
CREATE API INTEGRATION orrery_mcp_api_integration
  API_PROVIDER = external_mcp
  API_ALLOWED_PREFIXES = ('https://mcp.orrery.dev')
  API_USER_AUTHENTICATION = (
    TYPE = OAUTH_DYNAMIC_CLIENT,
    OAUTH_RESOURCE_URL = 'https://mcp.orrery.dev'
  )
  ENABLED = TRUE;
```

`OAUTH_RESOURCE_URL` is where Snowflake looks for Orrery's OAuth metadata. It must match the `resource` value Orrery advertises at `/.well-known/oauth-protected-resource`, which is the **origin only** — no `/mcp` path. The MCP server `URL` in the next step still uses the full endpoint.

To point at your tenant instead of the global endpoint, use `https://<tenant>.orrery.dev` for both `API_ALLOWED_PREFIXES` and `OAUTH_RESOURCE_URL`, and `https://<tenant>.orrery.dev/mcp` for the MCP server `URL`.

### 2. Create the External MCP Server

The MCP server is a schema-level object. Create it in a database and schema your agent users can reach, and note the fully qualified name — later steps need it.

```sql
CREATE EXTERNAL MCP SERVER <db>.<schema>.orrery_mcp_server
  WITH DISPLAY_NAME = 'Orrery'
  URL = 'https://mcp.orrery.dev/mcp'
  API_INTEGRATION = orrery_mcp_api_integration;
```

### 3. Grant access to your agent users

**Don't skip this step.** By default only account admins can use the MCP server, and Snowflake hides objects a role isn't authorized for rather than raising an error — so without these grants the connector simply never appears in CoWork, with no error message anywhere.

Grant `USAGE` on both the MCP server and its API integration to every role that will use the agent:

```sql
GRANT USAGE ON EXTERNAL MCP SERVER <db>.<schema>.orrery_mcp_server TO ROLE <role>;
GRANT USAGE ON INTEGRATION orrery_mcp_api_integration TO ROLE <role>;
```

### 4. Add the connector to your agent

In Snowsight, navigate to **AI & ML → Agents**, select your agent, choose **MCP Connectors**, select Orrery from **Available Connectors**, and select **Add to agent**. Customize the agent's prompt, model, and tools as needed.

Or via SQL:

```sql
ALTER AGENT <agent_name> MODIFY LIVE VERSION SET SPECIFICATION $$
    <previous_spec>
    mcp_servers:
      - server_spec:
         name: "<db>.<schema>.orrery_mcp_server"
$$;
```

### 5. Connect and sign in

Open [Snowflake CoWork](https://ai.snowflake.com/) and select your agent. Open the sources panel, select **Connectors**, then select **Connect** next to Orrery. Snowflake walks each user through the Orrery OAuth flow once, then reuses the credential on subsequent calls.

The connector shows as **Connected** when it's ready. Connectors that aren't in the connected state are excluded from the agent's orchestration.

<p align="center">
  <img width="70%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/ai/agent-context/snowflake/snowflake-cortex-agent.png"/>
</p>

## Troubleshooting

- **Permission denied creating the objects?** `CREATE API INTEGRATION` requires `ACCOUNTADMIN`; `CREATE EXTERNAL MCP SERVER` requires the matching privilege on the target schema.
- **Orrery doesn't appear in the CoWork connectors list?** This is almost always the missing `GRANT USAGE` from step 3 — Snowflake hides unauthorized objects instead of returning an error. Sign in as the role your agent users actually use and confirm the object is visible:

  ```sql
  SHOW EXTERNAL MCP SERVERS IN ACCOUNT;
  ```

- **Connector listed, but the OAuth flow never starts?** Check that Snowflake can resolve Orrery's OAuth metadata:

  ```sql
  SELECT SYSTEM$START_USER_OAUTH_FLOW('orrery_mcp_api_integration');
  ```

  This returns an authorization URL. If it errors instead, `OAUTH_RESOURCE_URL` is wrong — see step 1.

- **Does Orrery need to allowlist Snowflake's callback URL?** No. Orrery's OAuth server supports Dynamic Client Registration, so Snowflake registers itself and supplies `https://identity.snowflake.com/oauth2/callback` automatically. There is no redirect URI allowlist to configure on the Orrery side.
- **OAuth flow fails?** Confirm your Orrery Cloud instance is on v1.0.2+.
- **Agent not using Orrery tools?** Update the agent system prompt to explicitly mention Orrery tools (search, lineage, schema lookup, etc.).
- **Empty results?** The signed-in user's Orrery permissions apply — check that they can see the entities in the Orrery UI.

**Links:** [Cortex Agents Docs](https://www.snowflake.com/en/developers/guides/getting-started-with-snowflake-intelligence) · [Agent Context Kit](./agent-context.md) · [MCP Server Guide](../../features/feature-guides/mcp.md)

---

## UDF-Based Setup

This integration works through UDFs created by the Orrery CLI. Once set up, your Cortex Agent calls Orrery tools alongside your Snowflake tables.

### Prerequisites

- `pip install orrery-agent-context[snowflake]`
- A Orrery Cloud instance URL and [personal access token](../../authentication/personal-access-tokens.md)
- A Snowflake user with the `ACCOUNTADMIN` role (for initial setup)

### Setup

You can either let the CLI execute the SQL directly, or generate the SQL files and run them yourself.

**Option A: Execute directly**

```bash
orrery agent create snowflake \
  --sf-account YOUR_ACCOUNT \
  --sf-user YOUR_USER \
  --sf-password YOUR_PASSWORD \
  --sf-role YOUR_ROLE \
  --sf-warehouse YOUR_WAREHOUSE \
  --sf-database YOUR_DATABASE \
  --sf-schema YOUR_SCHEMA \
  --orrery-url https://your-orrery.orrery.dev \
  --orrery-token YOUR_TOKEN \
  --enable-mutations \
  --execute
```

Use `--sf-authenticator externalbrowser` for SSO instead of `--sf-password`.

<p align="center">
  <img width="70%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/ai/agent-context/snowflake/snowflake-execute-generator.png"/>
</p>

**Option B: Generate SQL**

Drop `--execute` and `--sf-password` to generate SQL files instead. Then run them in order in a Snowflake worksheet:

```sql
@00_configuration.sql;
@01_network_rules.sql;
@02_orrery_udfs.sql;
@03_stored_procedure.sql;
@04_cortex_agent.sql;
```

### Configure and Use

Customize the agent's prompt, model, and tools in the Snowflake UI, then open [Snowflake CoWork](https://ai.snowflake.com/) and select the Orrery Agent.

### Updating UDFs

When new tools are released, re-run the UDF and agent SQL:

```sql
@02_orrery_udfs.sql;
@04_cortex_agent.sql;
```

### Troubleshooting (UDF setup)

- **Permission denied?** Initial setup requires `ACCOUNTADMIN`. After that, `SNOWFLAKE_INTELLIGENCE_ADMIN` is sufficient.
- **UDFs not found?** Run `SHOW USER FUNCTIONS LIKE 'orrery%';` to verify.
- **Agent not using Orrery tools?** Update the agent system prompt to explicitly mention Orrery tools.
- **Connection errors?** Verify the Orrery URL is reachable and the token hasn't expired (should start with `eyJ`).
- **Empty results?** Check token permissions and that entities exist in the Orrery UI.
