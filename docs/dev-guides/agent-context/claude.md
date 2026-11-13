# Claude

Give Claude ([web, desktop](https://claude.ai/download), Cowork, and [Claude Code](https://docs.anthropic.com/en/docs/agents-and-tools/claude-code/overview)) access to your enterprise data context in Orrery — find trustworthy data, trace lineage, look up ownership, and reference documentation while you work.

## Prerequisites

- A Claude account, or Claude Code installed
- A Orrery instance: [Cloud](../../features/feature-guides/mcp.md#managed-mcp-server-usage) (OAuth on v1.0.2+, PAT on v0.3.12+) or [self-hosted](../../features/feature-guides/mcp.md#self-hosted-mcp-server-usage)

:::tip Quickest start: the Orrery Cloud plugin
On Orrery Cloud, install the [Orrery Cloud plugin](./plugins.md#claude). It includes the MCP server and skills, and you sign in with your Orrery account. Use the steps below if you need a scoped MCP server, an access token, or Orrery Core.
:::

## Claude Code

### Orrery Cloud — OAuth (Recommended, v1.0.2+)

On Orrery Cloud v1.0.2+, Claude Code can use OAuth2 with Dynamic Client Registration — no token to mint or paste.

```bash
claude mcp add --transport http orrery https://mcp.orrery.dev/mcp
```

The first Orrery call returns `401 Unauthorized`, which Claude Code flags as needing authentication. Run `/mcp` inside Claude Code, select the Orrery server, and choose **Authenticate** — a browser opens for the Orrery OAuth flow. Enter your Orrery domain (e.g. `<tenant>` for `https://<tenant>.orrery.dev`) and sign in. Tokens are stored and refreshed automatically.

Prefer your tenant URL directly? Swap the URL for `https://<tenant>.orrery.dev/mcp`.

### Orrery Cloud — Personal Access Token (Legacy)

For service accounts or Orrery Cloud versions prior to v1.0.2, use a [personal access token](../../authentication/personal-access-tokens.md):

```bash
claude mcp add --transport http \
  --header "Authorization: Bearer <token>" \
  orrery-cloud \
  "https://<tenant>.orrery.dev/mcp"
```

:::tip Keep Your Token Secret
To avoid putting the token in your shell history, set it as an environment variable first:

```bash
export ORRERY_TOKEN="<your-token>"
claude mcp add --transport http \
  --header "Authorization: Bearer $ORRERY_TOKEN" \
  orrery-cloud \
  "https://<tenant>.orrery.dev/mcp"
```

:::

### Orrery Core

Install [`uv`](https://github.com/astral-sh/uv) (`curl -LsSf https://astral.sh/uv/install.sh | sh`), then:

```bash
claude mcp add orrery \
  -e ORRERY_GMS_URL="<your-orrery-url>" \
  -e ORRERY_GMS_TOKEN="<your-orrery-token>" \
  -- uvx mcp-server-orrery@latest
```

### Verify

Run `claude mcp list` to confirm the Orrery server appears.

## Claude Desktop

### Orrery Cloud — OAuth Connector (Recommended, v1.0.2+)

Custom remote MCP connectors are available on **Free, Pro, Max, Team, and Enterprise** plans (Free is limited to one custom connector; Team/Enterprise restricts adding to Owners).

1. Open **Customize → Connectors**. On Team/Enterprise, an owner adds it under **Organization settings → Connectors**.
2. Click **Add custom connector**.
3. Name: `Orrery`. Remote MCP server URL: `https://mcp.orrery.dev/mcp`. Leave **Advanced settings** empty — Orrery registers the client via DCR automatically.
4. Click **Add**, then **Connect**. A browser window opens for the Orrery OAuth flow.
5. Enter your Orrery domain (e.g. `<tenant>`), sign in, and approve. The Orrery tools appear in Claude's tool menu (hammer icon).

Prefer your tenant URL directly? Use `https://<tenant>.orrery.dev/mcp` as the connector URL instead.

:::note
Remote MCP connectors are configured via the Claude Desktop UI, not `claude_desktop_config.json` — that file is for local stdio servers only.
:::

### Orrery Cloud — `mcp-remote` Bridge (Legacy)

For older Claude Desktop versions without native remote MCP support, use [`mcp-remote`](https://github.com/geelen/mcp-remote) as a local bridge.

Open **Claude Desktop → Settings → Developer → Edit Config** and update `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "orrery-cloud": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-remote",
        "https://<tenant>.orrery.dev/mcp",
        "--header",
        "Authorization: Bearer <token>"
      ]
    }
  }
}
```

### Orrery Core

1. Run `which uvx` to find the full path to `uvx`.
2. Open **Claude Desktop → Settings → Developer → Edit Config** and update `claude_desktop_config.json`:

```js
{
  "mcpServers": {
    "orrery": {
      "command": "<full-path-to-uvx>",  // e.g. /Users/you/.local/bin/uvx
      "args": ["mcp-server-orrery@latest"],
      "env": {
        "ORRERY_GMS_URL": "<your-orrery-url>",
        "ORRERY_GMS_TOKEN": "<your-orrery-token>"
      }
    }
  }
}
```

### Verify

Restart Claude Desktop. The Orrery tools should appear in the tools menu (hammer icon).

## Troubleshooting

For general troubleshooting (authentication errors, empty results, `uvx` not found), see the [MCP server guide](../../features/feature-guides/mcp.md#troubleshooting).
