# Google Gemini CLI

Give [Gemini CLI](https://github.com/google-gemini/gemini-cli) access to your enterprise data context in Orrery — find trustworthy data, trace lineage, and look up ownership right from the terminal.

## Prerequisites

- [Gemini CLI](https://github.com/google-gemini/gemini-cli) installed
- A Orrery instance ([Cloud](../../features/feature-guides/mcp.md#managed-mcp-server-usage) v0.3.12+ or [self-hosted](../../features/feature-guides/mcp.md#self-hosted-mcp-server-usage)) and a [personal access token](../../authentication/personal-access-tokens.md)

## Orrery Cloud

Gemini CLI natively supports streamable HTTP with custom headers.

```bash
gemini mcp add --transport http \
  --header "Authorization: Bearer <token>" \
  orrery-cloud \
  "https://<tenant>.orrery.dev/mcp"
```

Replace `<tenant>` with your Orrery Cloud tenant name and `<token>` with your personal access token.

Or add it directly to your `settings.json` (`~/.gemini/settings.json` for user-level, or `.gemini/settings.json` for project-level):

```json
{
  "mcpServers": {
    "orrery-cloud": {
      "httpUrl": "https://<tenant>.orrery.dev/mcp",
      "headers": {
        "Authorization": "Bearer <token>"
      }
    }
  }
}
```

## Orrery Core

Install [`uv`](https://github.com/astral-sh/uv) (`curl -LsSf https://astral.sh/uv/install.sh | sh`), then:

```bash
gemini mcp add \
  -e ORRERY_GMS_URL="<your-orrery-url>" \
  -e ORRERY_GMS_TOKEN="<your-orrery-token>" \
  orrery \
  uvx mcp-server-orrery@latest
```

## Verify

Run `gemini mcp list` or use the `/mcp` command inside a session to confirm the Orrery server is connected.

For general troubleshooting, see the [MCP server guide](../../features/feature-guides/mcp.md#troubleshooting).
