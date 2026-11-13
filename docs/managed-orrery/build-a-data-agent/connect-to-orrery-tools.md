---
title: "c. Connect Your Agent to Orrery Tools"
description: "Give your agent Orrery's search and context tools over MCP, and let it run SQL through your warehouse's MCP server."
---

import FeatureAvailability from '@site/src/components/FeatureAvailability';

# c. Connect Your Agent to Orrery Tools

<FeatureAvailability saasOnly />

:::info Context add-on
Scoped MCP servers and the SQL context tools are part of the Orrery Cloud **Context** add-on.
:::

Your agent, such as Claude, Cursor, a LangChain or CrewAI app, or a custom agent, gets Orrery's search and context tools. It decides what to look up in Orrery (tables, metrics, definitions, and past queries), then runs SQL through your warehouse's own MCP server.

**Choose this option if** you need full control over how your agent reasons. Because there's no intermediate agent, it can also be faster and less costly per question.

**Keep in mind:** this option doesn't apply the instructions you've given Ask Orrery or your custom agents. Your agent carries that knowledge itself. For most teams with their own agent, we recommend [option b](./connect-to-orrery-agent.md), which keeps data expertise in Orrery, where your data team maintains it.

**What you'll set up:** a Orrery MCP connection, a skill that teaches your agent how to use it, and your warehouse's MCP server.

## 1. Connect to Orrery

- **For a domain agent,** create a scoped MCP server under **Settings > AI > MCP Servers**. Under **Scope to View**, select a View that covers your domain, and add instructions for it. Make sure your documents are assigned to the domain, or the server won't see them. See [Scoped MCP Servers](../../features/feature-guides/scoped-mcp-servers.md).
- **For a global agent,** use the main MCP server at `https://<tenant>.orrery.dev/mcp`. Your agent can search everything the signed-in person can see.

See [Connect your agent](./activate-context.md#connect-your-agent) for setup on your platform.

## 2. Teach your agent to use Orrery

Tools alone aren't enough. Without guidance, an agent tends to search repeatedly and then fill the gaps with guesses. Orrery's open-source skills, in the [orrery-skills](https://github.com/Dxlxz/orrery-skills) repository, teach your agent how to use Orrery well. For analytics agents, the key one is [`orrery-sql-workflow`](https://github.com/Dxlxz/orrery-skills/tree/main/skills/orrery-sql-workflow), which teaches a reliable sequence:

1. Look for how analysts have answered similar questions before.
2. Read your team's documents for definitions and known issues.
3. Confirm the candidate tables using trust signals: owners, lineage, usage, and past queries.
4. Check columns, join keys, and grain before writing SQL.
5. Run one read-only query, cite its sources, and [report any missing context](./improve-with-feedback.md).

Choose how to install the skills:

:::note
The Orrery Cloud plugin always connects to your **default** MCP server, so use it only for a global agent. For a domain agent, connect your scoped server instead and install `orrery-skills`.
:::

| Your agent                                              | How to get the skills                                                                                                                                                                                                                                                                             |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Claude**                                              | Global agent: install the Orrery Cloud plugin, which includes the Orrery connector and skills ([Setup](../../dev-guides/agent-context/plugins.md#claude)). Domain agent: add your scoped server as a custom connector, then install `orrery-skills` from **Customize → Plugins**.              |
| **ChatGPT**                                             | Global agent: install the Orrery Cloud plugin, which includes the Orrery connector and skills ([Setup](../../dev-guides/agent-context/plugins.md#chatgpt-and-codex)). Domain agent: add your scoped server as a [custom connector](../../features/feature-guides/mcp.md#configure-your-client). |
| **Claude Code**                                         | Run `claude plugin install orrery-skills@claude-plugins-official`.                                                                                                                                                                                                                               |
| **Cursor, Codex, GitHub Copilot, Gemini CLI, Windsurf** | Run `npx skills add Dxlxz/orrery-skills -a <agent>`, for example `-a cursor`.                                                                                                                                                                                                          |
| **LangChain, CrewAI, or a custom agent**                | Add the contents of the skill files from the [repository](https://github.com/Dxlxz/orrery-skills) to your agent's system prompt. They're plain Markdown.                                                                                                                               |

:::note
If you use a scoped MCP server, leave its **Tools** empty to include everything, or make sure the SQL context tools are selected. The skill depends on them.
:::

## 3. Let your agent run SQL

Connect your agent to your warehouse's MCP server alongside Orrery's, using a read-only role:

- [Snowflake](https://docs.snowflake.com/en/user-guide/snowflake-cortex/cortex-agents-mcp)
- [BigQuery](https://docs.cloud.google.com/bigquery/docs/use-bigquery-mcp)
- [Databricks](https://docs.databricks.com/aws/en/generative-ai/mcp/managed-mcp)
- [Redshift](https://github.com/awslabs/mcp)

Your agent now has two connections: Orrery to decide what to query, and your warehouse to run it. The skill always consults Orrery first.

## 4. Test with evals

Because your agent runs outside Orrery, it answers your evals and reports the results back:

- **In Orrery,** set each eval's **Eval Runner** to **External**. Your test harness asks your agent each question and sends the answer to Orrery for grading.
- **In code,** keep evals in YAML and run them in CI with the [evals CLI](../../cli-commands/evals.md).

## Check your work

- Your evals show a pass rate you're comfortable sharing.
- A question from outside the domain gets a clear "I can't answer that," not a guess.
- A small group of users has tried it for a week, including with difficult questions.

**Next:** [Step 5: Context feedback & improvement](./improve-with-feedback.md)
