<!-- PyPI long description. Keep concise, feature-discovery-first. -->

# Orrery Agent Context

**MCP tools for AI agents to search and query your Orrery metadata catalog** — works with Claude, Cursor, Copilot, and any MCP-compatible AI assistant.

## What you can do

- **Search** datasets, dashboards, pipelines, and other data assets by name or description
- **Retrieve entity details** — schema, lineage, ownership, tags, glossary terms, and more
- **Trace lineage** upstream and downstream across your data assets
- **Mutate metadata** — update descriptions, tags, owners, domains, and glossary terms
- **Build LangChain or Google ADK agents** with pre-built tool bindings
- **Set up Snowflake AI agents** with one CLI command

## Installation

```bash
pip install orrery-agent-context

# With LangChain support
pip install "orrery-agent-context[langchain]"
```

## Quickstart

### LangChain agent

```python
from orrery.sdk.main_client import OrreryClient
from orrery_agent_context.langchain_tools import build_langchain_tools

client = OrreryClient.from_env()

# Read-only tools (search, lineage, entity details)
tools = build_langchain_tools(client, include_mutations=False)

# Include write tools (tags, descriptions, owners, etc.)
tools = build_langchain_tools(client, include_mutations=True)

# Orrery Cloud: add Ask Orrery AI assistant
from orrery_agent_context.langchain_tools import build_langchain_cloud_tools
tools += build_langchain_cloud_tools(client, ask_orrery=True)
```

### Snowflake AI agent setup

```bash
orrery agent create snowflake \
  --orrery-url https://your-orrery-instance \
  --orrery-token your-token
```

## Available tools

**Search** — `search()`, `search_documents()`, `grep_documents()`

**Entities** — `get_entities()`, `list_schema_fields()`

**Lineage** — `get_lineage()`, `get_lineage_paths_between()`

**Queries** — `get_dataset_queries()`

**Incidents** — `list_incidents()`

**Mutations** — `add_tags()`, `remove_tags()`, `update_description()`, `set_domains()`, `add_owners()`, `add_glossary_terms()`, `add_structured_properties()`, `save_document()`, `raise_incident()`, `resolve_incident()`

**Cloud-only** — `ask_orrery_chat()` (Orrery Cloud AI assistant)

## Links

- [Documentation](https://docs.orrery.dev/)
- [GitHub](https://github.com/Dxlxz/orrery)
- [Slack community](https://orrery.dev/slack)
