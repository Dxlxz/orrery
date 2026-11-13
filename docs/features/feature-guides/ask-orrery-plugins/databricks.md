import FeatureAvailability from '@site/src/components/FeatureAvailability';

# Databricks Plugin

<FeatureAvailability saasOnly />

> **Note**: Ask Orrery Plugins is currently in **Private Beta**. To enable this feature, please reach out to your Orrery customer support representative.

The **Databricks Plugin** connects Ask Orrery to your Databricks workspace via the [Databricks MCP server](https://docs.databricks.com/aws/en/generative-ai/mcp/connect-external-services), enabling conversational analytics directly from the chat. Like the Snowflake plugin, Orrery acts as the **semantic layer** — finding the right data — while Databricks serves as the **data layer** — executing queries and returning results.

## Why Connect Databricks?

With the Databricks plugin enabled, Ask Orrery can:

- **Conversational analytics** — ask questions in plain language and get answers backed by real data. Orrery finds the right tables using metadata, then Databricks executes the SQL query.
- **Data exploration** — understand dataset structure, preview sample data, and inspect column values across your Unity Catalog.
- **Data debugging** — investigate data issues detected by Orrery assertions by querying the actual data at the source.

Because the plugin uses **OAuth**, each user authenticates with their own Databricks account. Users only see data they are authorized to access.

**Example prompts:**

- _"Query the top 10 customers by lifetime value"_
- _"How many rows were added to the events table today?"_
- _"Show me 5 sample rows from the orders table"_
- _"Check for null values in the email column of the users table"_

## Prerequisites

- A Databricks workspace with [Managed MCP Servers](https://docs.databricks.com/aws/en/generative-ai/mcp/connect-external-services) enabled
- Admin access to create OAuth app connections in Databricks (**Settings > App connections**)
- Orrery Cloud with Ask Orrery Plugins enabled
- Platform admin access in Orrery to configure the plugin

## Admin Setup

The Databricks plugin uses **User OAuth** authentication — each user authenticates with their own Databricks account. Setup involves creating an OAuth app connection in Databricks, then configuring the plugin in Orrery.

### Step 1: Create an OAuth App Connection in Databricks

1. In Databricks, navigate to **Settings > App connections**
2. Click **Add connection** and fill in the details:

| Field                        | Value                                                    |
| ---------------------------- | -------------------------------------------------------- |
| **Application Name**         | `Orrery` (or any name you'll recognize)                 |
| **Redirect URLs**            | `https://<your-orrery-url>/integrations/oauth/callback` |
| **Access scopes**            | Check **SQL**                                            |
| **Generate a client secret** | Checked                                                  |

:::caution Redirect URL
The **Redirect URL** must match the OAuth Callback URL shown in the Orrery plugin creation form exactly (e.g. `https://your-org.orrery.dev/integrations/oauth/callback`). You can copy this URL from the Orrery form when creating the plugin in Step 3.
:::

3. Click **Add**

<p align="center">
  <img width="70%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/saas/ai/plugins/databricks_add_oauth_connection.png"/>
</p>

### Step 2: Collect Credentials

After creating the connection, a dialog will show your **Client ID** and **Client Secret**. Copy both immediately — the client secret won't be shown again.

<p align="center">
  <img width="70%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/saas/ai/plugins/databricks_oauth_credentials.png"/>
</p>

### Step 3: Create Plugin in Orrery

1. Navigate to **Settings > AI > Plugins** in Orrery
2. Click **+ Create** and select **Custom MCP**
3. Fill in the plugin details:

| Field                   | Value                                               |
| ----------------------- | --------------------------------------------------- |
| **Name**                | `Databricks`                                        |
| **Description**         | A description for the plugin                        |
| **MCP Server URL**      | `https://<your-workspace-hostname>/api/2.0/mcp/sql` |
| **Authentication Type** | `User OAuth (Each user authenticates)`              |

4. Orrery will automatically discover the OAuth configuration from Databricks, including the authorization URL, token URL, and available scopes. Provide the following:

| Field              | Value                    |
| ------------------ | ------------------------ |
| **Client ID**      | From Step 2              |
| **Client Secret**  | From Step 2              |
| **Default Scopes** | `sql` (minimum required) |

<p align="center">
  <img width="70%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/saas/ai/plugins/databricks_plugin_config.png"/>
</p>

:::info Access Scopes
The `sql` scope is the minimum required for Ask Orrery to execute queries. If you selected **All APIs** when creating the Databricks app connection, additional scopes will be available — select only what you need.
:::

5. Optionally add **Instructions for the AI Assistant**, ensure **Enable for Ask Orrery** is on, and click **Create**

## User Setup

Navigate to **Settings > My AI Settings**, find the **Databricks** plugin, and click **Connect**. You'll be redirected to Databricks to authenticate, then back to Orrery. See the [overview](./overview.md#user-setup-enabling-plugins) for more details.

## Troubleshooting

### OAuth Fails

- Verify the **Redirect URL** in Databricks matches the OAuth Callback URL shown in Orrery exactly
- Ensure the Client ID and Client Secret are entered correctly
- If your workspace has [IP access restrictions](https://docs.databricks.com/aws/en/security/network/front-end/ip-access-list), ensure Orrery's outbound IPs are allowlisted

### Query Failures

- Verify the user has access to the tables they're querying in Unity Catalog
- Ensure the `sql` scope was selected when creating the app connection in Databricks
- Check that a SQL warehouse is available and running in the workspace
