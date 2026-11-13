# Microsoft Copilot Studio

Build [Copilot Studio](https://copilotstudio.microsoft.com/) agents that can find trustworthy data, trace lineage, look up ownership, and answer data questions grounded in your enterprise context from Orrery.

## Prerequisites

- A [Microsoft Copilot Studio](https://copilotstudio.microsoft.com/) account
- A Orrery instance: [Cloud](../../features/feature-guides/mcp.md#managed-mcp-server-usage) (OAuth on v1.0.2+, PAT on v0.3.12+) or [self-hosted](../../features/feature-guides/mcp.md#self-hosted-mcp-server-usage) with the MCP server running

## Setup

### 1. Create or Open an Agent

In [Copilot Studio](https://copilotstudio.microsoft.com/), click **+ Create a blank agent** or open an existing one.

<p align="center">
  <img width="80%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/ai/agent-context/copilot-studio/copilot_studio_agents_page.png"/>
</p>

### 2. Add an MCP Tool

From your agent's overview, scroll to **Tools** and click **+ Add tool**. Select **Model Context Protocol** under "Create new".

<p align="center">
  <img width="70%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/ai/agent-context/copilot-studio/copilot_studio_add_tool.png"/>
</p>

### 3. Configure the MCP Connection

#### Orrery Cloud — OAuth (Recommended, v1.0.2+)

On Orrery Cloud v1.0.2+, Copilot Studio can connect with OAuth2 and [Dynamic Client Registration (DCR)](https://datatracker.ietf.org/doc/html/rfc7591). Each agent user signs in with their own Orrery account (including SSO such as Okta or Azure AD). No personal access token to mint or paste.

1. Fill in the MCP server fields:

   | Field              | Value                                 |
   | ------------------ | ------------------------------------- |
   | **Server name**    | `Orrery MCP Server`                  |
   | **Server URL**     | `https://mcp.orrery.dev/mcp`         |
   | **Authentication** | **OAuth 2.0** · **Dynamic discovery** |

2. Click **Create**. Copilot Studio discovers Orrery's OAuth metadata and registers itself via DCR — leave Client ID / Client secret empty.
3. On **Add tool**, click **Create a new connection** (or **Connect**). A browser window opens for the Orrery OAuth flow.
4. Enter your Orrery domain when prompted (e.g. `<tenant>` for `https://<tenant>.orrery.dev`), sign in, and approve the connection. Copilot Studio can only retrieve Orrery's tools after this step completes.
5. Click **Add to agent**.

Prefer your tenant URL directly? Use `https://<tenant>.orrery.dev/mcp` as the Server URL instead — both endpoints support OAuth2 + DCR. With the tenant URL you skip the domain prompt and go straight to login.

:::tip Fallback OAuth modes
If **Dynamic discovery** fails, try **Dynamic** and enter the Authorization and Token URLs from the auth server's `/.well-known/oauth-authorization-server` document. Prefer Dynamic discovery when it works — you should not need a Client ID or Client secret.
:::

#### Orrery Cloud — Personal Access Token (v0.3.12+)

For service accounts, unattended agents, or Orrery Cloud versions prior to v1.0.2, use a [personal access token](../../authentication/personal-access-tokens.md):

| Field              | Value                                                 |
| ------------------ | ----------------------------------------------------- |
| **Server name**    | `Orrery MCP Server`                                  |
| **Server URL**     | `https://<tenant>.orrery.dev/mcp`                       |
| **Authentication** | API key · Header · `Authorization` · `Bearer <token>` |

<p align="center">
  <img width="70%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/ai/agent-context/copilot-studio/copilot_studio_mcp_config.png"/>
</p>

Include the `Bearer ` prefix in the API key value. Click **Create**, create the connection with your token, then **Add to agent**.

:::note Orrery Core
For Orrery Core instances, OAuth DCR for the managed MCP path is a Orrery Cloud capability. Expose the [MCP server](../../features/feature-guides/mcp.md#self-hosted-mcp-server-usage) via a publicly accessible URL, use that as the Server URL, and authenticate with a personal access token (API key · Header · `Authorization` · `Bearer <token>`).
:::

### 4. Enable Tools

After the connection succeeds, Copilot Studio discovers Orrery's tools automatically. Click **Add and configure**, then toggle on the tools you want.

<p align="center">
  <img width="80%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/ai/agent-context/copilot-studio/copilot_studio_tools_list.png"/>
</p>

If no tools appear, go back and complete **Connect** / **Create a new connection** — tool discovery requires an authenticated connection.

### 5. Test

Click **Test** in the top-right corner and try:

- _"What datasets does the analytics team own?"_
- _"Show me the lineage for the revenue dashboard"_

<p align="center">
  <img width="50%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/ai/agent-context/copilot-studio/copilot_studio_test_agent.png"/>
</p>

## Tips

- Use the **Instructions** field to guide behavior — e.g., _"Always search Orrery before answering data questions."_
- **Publish** your agent to Teams, your website, or other channels when ready.
- Prefer OAuth for interactive Copilot agents so each user acts as themselves in Orrery. Keep PATs for service accounts and unattended workflows.

## Troubleshooting

- **Can't connect with OAuth?** Confirm the tenant is on Orrery Cloud v1.0.2+, the Server URL is `https://mcp.orrery.dev/mcp` (or your tenant MCP URL), and authentication is **OAuth 2.0** with **Dynamic discovery**. Click **Create a new connection** / **Connect**, enter your Orrery domain (e.g. `<tenant>`) when prompted, sign in, and approve.
- **Can't connect with a PAT?** Verify the Orrery URL, check the token hasn't expired, and confirm auth is set to **API key** (not OAuth). Include the `Bearer ` prefix.
- **Tools not appearing?** Finish the **Connect** step first — Copilot Studio only lists Orrery tools after the connection authenticates. Then refresh the Tools page. Verify the [MCP server](../../features/feature-guides/mcp.md) is running and the connection has the right permissions.
- **Empty results?** Check that your Orrery instance has ingested metadata. Try broader search terms.

**Links:** [Copilot Studio Docs](https://learn.microsoft.com/en-us/microsoft-copilot-studio/mcp-add-existing-server-to-agent) · [Agent Context Kit](./agent-context.md) · [MCP Server Guide](../../features/feature-guides/mcp.md)
