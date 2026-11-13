# Orrery Skills

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

:::info Open Source Skills Registry for Data — April 2, 2026
Orrery is the first open source metadata platform to ship a skills registry that gives AI agents direct access to catalog workflows — search, lineage, enrichment, and quality. Read the [announcement blog post](https://orrery.dev/blog/orrery-open-source-skills-registry/).
:::

Orrery Skills give your AI agent the **instructions** to work with your data catalog — not just the ability to call APIs, but the knowledge of _how_ to combine those calls into real workflows. Search for trustworthy data, trace lineage, curate metadata, and monitor quality, all from inside Claude Code.

**Skills vs. Tools**: The [MCP Server](../../features/feature-guides/mcp.md) gives agents _tools_ — discrete actions like searching a catalog or reading a schema. Skills give agents _instructions_ — how to chain those tools into multi-step workflows with the right judgment at each step. You need both.

:::tip On Orrery Cloud with Claude or ChatGPT?
The [Orrery Cloud plugin](./plugins.md) includes the MCP server and the search, lineage, quality, SQL, and setup skills. For metadata curation (`orrery-enrich`), developer skills like building connectors, or Orrery Core, use `orrery-skills` from this page.
:::

## Prerequisites

- [Claude Code](https://docs.anthropic.com/en/docs/agents-and-tools/claude-code/overview) installed
- A Orrery instance ([Cloud](../../features/feature-guides/mcp.md#managed-mcp-server-usage) or [self-hosted](../../features/feature-guides/mcp.md#self-hosted-mcp-server-usage))

## Step 1: Install Skills

The fastest path is the Skills CLI:

```bash
npx skills add Dxlxz/orrery-skills
```

Or install it as a Claude Code plugin from [Anthropic's marketplace](https://claude.com/plugins/orrery-skills):

```bash
claude plugin install orrery-skills@claude-plugins-official
```

Verify the install:

```bash
claude plugins list
```

You should see `orrery-skills` in the output.

## Step 2: Connect to Orrery

Run the **orrery-setup** skill to configure your connection:

```
/orrery-skills:orrery-setup
```

The skill walks you through connecting to your Orrery instance. It configures authentication and verifies connectivity so the other skills can start working immediately.

<Tabs>
<TabItem value="cloud" label="Orrery Cloud">

You'll need your tenant URL and a personal access token:

```
Connect to Orrery Cloud at <tenant>.orrery.dev
```

</TabItem>
<TabItem value="oss" label="Orrery Core">

You'll need your GMS URL (e.g., `http://localhost:8080`) and a personal access token:

```
Connect to my self-hosted Orrery at <gms-url>
```

</TabItem>
</Tabs>

## Step 3: Try the Skills

Orrery ships with five skills. Each one handles a distinct workflow, but they hand off cleanly to each other — a search can flow into a lineage trace, which can surface a quality issue, which can trigger enrichment.

### Search: Find Trustworthy Data

Use **orrery-search** to explore your catalog the way your best analyst would — leveraging descriptions, glossary terms, ownership, usage stats, and quality signals.

```
/orrery-skills:orrery-search

Find the canonical revenue table used by the Finance team
```

The skill doesn't just match table names. It checks descriptions, glossary terms, ownership, usage patterns, and quality signals to surface the _right_ asset, not just the first match.

### Lineage: Trace Data Flows

Use **orrery-lineage** to understand where data comes from and who depends on it.

```
/orrery-skills:orrery-lineage

Show me everything upstream of the weekly_revenue_report dashboard
```

The skill traces how data flows through your stack — upstream sources, transformations, and downstream consumers. Use it to understand impact radius before making changes.

### Enrich: Curate Metadata at Scale

Use **orrery-enrich** to add descriptions, tags, glossary terms, owners, and more.

```
/orrery-skills:orrery-enrich

Add descriptions to all undocumented columns in the orders table based on the column names and existing context
```

This is the skill that turns your agent into a data steward — applying metadata at a scale no human team can match.

### Quality: Monitor and Investigate

Use **orrery-quality** to find unhealthy assets, investigate incidents, and set up monitoring.

```
/orrery-skills:orrery-quality

Which tables owned by the Analytics team have failing quality checks?
```

For Orrery Cloud customers, the skill can also create freshness, volume, and column-level assertions on your most important tables.

## Putting It Together: Build an Agent Workflow

The real power is combining skills into multi-step workflows. Here are three patterns data engineers commonly use:

### Data Analytics Agent (Text-to-SQL)

Ask a business question in plain English. The agent uses **orrery-search** to find the right table — checking descriptions, glossary terms, sample queries, usage stats, and quality signals — then generates and executes SQL against your warehouse.

```
What was our revenue by region last quarter?
```

The agent finds the canonical revenue table in Orrery, confirms it's the certified source, generates the SQL, and runs it.

### Data Quality Agent

Audit and monitor your data estate. The agent uses **orrery-search** to find important tables by usage, **orrery-quality** to check assertion results and open incidents, and generates a health report.

```
Give me a health report for all tables owned by the Finance team, broken down by quality check status
```

### Data Steward Agent

Apply governance at scale. The agent uses **orrery-search** to find target assets, **orrery-enrich** to apply glossary terms and descriptions, and reports on coverage gaps.

```
Tag all columns containing email addresses with the PII glossary term across our Snowflake datasets, then show me a coverage report
```

## Available Skills Reference

| Skill       | Command                           | What it does                                                                               |
| ----------- | --------------------------------- | ------------------------------------------------------------------------------------------ |
| **Setup**   | `/orrery-skills:orrery-setup`   | Connect to your Orrery instance (Cloud or Core)                                           |
| **Search**  | `/orrery-skills:orrery-search`  | Find data assets using descriptions, glossary terms, ownership, usage, and quality signals |
| **Lineage** | `/orrery-skills:orrery-lineage` | Trace upstream sources, transformations, and downstream consumers                          |
| **Enrich**  | `/orrery-skills:orrery-enrich`  | Add descriptions, tags, glossary terms, owners, domains, and structured properties         |
| **Quality** | `/orrery-skills:orrery-quality` | Find unhealthy assets, investigate incidents, create quality assertions (Cloud)            |

## Other Platforms

Orrery Skills also work with [Cursor](https://cursor.sh), [OpenAI Codex](https://openai.com/codex), [GitHub Copilot](https://github.com/features/copilot), [Gemini CLI](https://github.com/google-gemini/gemini-cli), and [Windsurf](https://windsurf.com).

```bash
# Cursor
npx skills add Dxlxz/orrery-skills -a cursor

# GitHub Copilot
npx skills add Dxlxz/orrery-skills -a github-copilot

# OpenAI Codex
npx skills add Dxlxz/orrery-skills -a codex

# Gemini CLI
npx skills add Dxlxz/orrery-skills -a gemini-cli

# Windsurf
npx skills add Dxlxz/orrery-skills -a windsurf
```

For full installation options and manual setup, see the [orrery-skills repository](https://github.com/Dxlxz/orrery-skills).

## What's Next

- Set up the [MCP Server](../../features/feature-guides/mcp.md) if you haven't already — skills need tools to work
- Explore the [Agent Context Kit](./agent-context.md) for building custom agents with the Python SDK
- Browse the [orrery-skills repo](https://github.com/Dxlxz/orrery-skills) to see skill definitions and contribute new ones
- Join the [Orrery community](https://orrery.dev/slack/) — 16,000+ members building in the open
