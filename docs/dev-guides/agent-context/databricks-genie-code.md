# Databricks Genie Code

Give [Genie Code](https://docs.databricks.com/en/genie/genie-code.html) access to your enterprise data context in Orrery — find trustworthy data, understand lineage, look up ownership, and generate better SQL queries, all without leaving your notebook.

:::tip OAuth MCP alternative (Orrery Cloud v1.0.2+)
On Orrery Cloud v1.0.2+, you can register `https://<tenant>.orrery.dev/mcp` directly with Databricks as an external MCP tool using **OAuth 2.0** — Orrery supports Dynamic Client Registration so Databricks registers itself and stores refresh tokens automatically, eliminating the PAT in the Unity Catalog connection below. Your tenant URL is required here; the global `https://mcp.orrery.dev/mcp` endpoint is not yet supported by Databricks. See the [OAuth + DCR section of the MCP guide](../../features/feature-guides/mcp.md#oauth2-with-dynamic-client-registration-recommended).
:::

<p align="center">
  <img width="70%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/ai/agent-context/databricks/genie-code-orrery-query.png" alt="Genie Code querying Orrery catalog"/>
</p>

## Prerequisites

- A Databricks workspace with the **Managed MCP Servers** preview enabled
- A Orrery instance with the [MCP server](../../features/feature-guides/mcp.md) enabled
- A Orrery access token (personal access token or service account token)
- `CREATE CONNECTION` privilege on your Unity Catalog metastore

## Step 1: Register Orrery as an External MCP Server

This creates a Unity Catalog HTTP connection to your Orrery [MCP server](../../features/feature-guides/mcp.md). You only need to do this once — the same connection is used by [Databricks Agent Bricks](./databricks-agent-bricks.md).

1. In your Databricks workspace, go to **Catalog > Connections > Create connection**.

2. Configure the connection:

   - **Name**: `orrery` (or your preferred name)
   - **Connection type**: HTTP
   - **Auth type**: Bearer Token
   - **Bearer token**: Your Orrery access token
   - **Host**: Your Orrery MCP server URL:
     - Orrery Cloud: `https://<tenant>.orrery.dev`
     - Self-hosted: `http://<gms-host>:8080`
   - **Base path**: `/mcp`

3. Click **Create connection**.

4. Grant access to users who need it: Go to **Catalog > Connections > orrery > Permissions** and grant `USE CONNECTION` to the appropriate users or groups.

Once created, the connection shows up under **Agents > MCP Servers** in your workspace.

## Step 2: Add Orrery to Genie Code

1. Open **Genie Code** and click the **Settings** gear icon.

2. Under **MCP Servers**, click **Add Server**.

3. Select **External MCP servers** and choose the `orrery` connection you created above.

<p align="center">
  <img width="70%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/ai/agent-context/databricks/genie-code-add-mcp-servers.png" alt="Add MCP Servers dialog showing External MCP: orrery"/>
</p>

4. Click **Save**.

## Try It Out

Switch to **Agent mode** in Genie Code and try:

```
Find all datasets related to "customer churn" and show me their owners
```

```
Search Orrery for datasets in the Marketing domain, then query the customer_segments
table to show me the top 10 segments by size
```

> **Tip**: MCP servers require Genie Code **Agent mode**. There's a limit of 20 tools across all connected MCP servers.

## Troubleshooting

- **Orrery not showing up?** Check the connection under **Catalog > Connections**, verify the MCP URL and base path, and confirm the bearer token is still valid.
- **`401 Unauthorized`?** Regenerate the Orrery access token and update the UC connection.
- **No tools discovered?** Make sure Orrery's [MCP server](../../features/feature-guides/mcp.md) is enabled. Genie Code caps at 20 tools across all MCP servers. The base path is `/mcp`.

**Links:** [MCP on Databricks](https://docs.databricks.com/en/generative-ai/agent-framework/mcp.html) · [Orrery MCP Server](../../features/feature-guides/mcp.md) · [Agent Context Kit](./agent-context.md)
