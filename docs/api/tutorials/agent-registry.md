---
description: "Register AI agents, skills, and tools with Orrery — from the Python SDK and CLI, or automatically from LangChain and Google ADK with the agent context kit."
---

# Registering AI Agents, Skills, and Tools

This tutorial shows how to populate the [Agent Registry](../../features/feature-guides/agent-registry.md)
programmatically. There are two ways in:

- **Explicit registration** — describe an agent, its skills, and its tools with the Python SDK or
  the `orrery` CLI. Use this when you catalog agents from your own scripts or CI.
- **Automatic registration** — point the **agent context kit** at an existing LangChain or Google ADK
  agent and it registers the agent, its tools, and its model for you. Use this to catalog agents from
  the framework code you already run.

Everything below works against any Orrery instance — local, Orrery Core, or Orrery Cloud.

## Prerequisites

Install the CLI and authenticate once:

```bash
pip install orrery
orrery init   # writes ~/.orreryenv with your GMS URL and token
```

In Python, the emit target is a graph client:

```python
from orrery.ingestion.graph.client import get_default_graph

graph = get_default_graph()  # reads ~/.orreryenv, or ORRERY_GMS_URL / ORRERY_GMS_TOKEN
```

Entity relationships are typed, so register **tools before the skills that require them, and skills
before the agents that adopt them** — the referencing side validates that URNs are well-formed.

## Register a tool

A tool is an **API** entity (`urn:li:api:<id>`) — a named callable with a typed signature. The same
entity models MCP tools, REST endpoints, gRPC methods, GraphQL operations, and plain functions;
the `subtypes` field says which.

<b>Python SDK</b>

```python
from orrery.api.entities.agent.api import Api, ApiParam

get_orders = Api(
    id="order-svc.get_orders",
    name="get_orders",
    subtypes=["MCP_TOOL"],  # or REST_ENDPOINT, GRPC_METHOD, GRAPHQL_OPERATION, FUNCTION
    description="Fetch orders in a date range.",
    parameters=[
        ApiParam(name="start_date", data_type="string", required=True),
        ApiParam(name="end_date", data_type="string", required=True),
        ApiParam(name="region", data_type="string", description="Optional region filter."),
    ],
    returns=[ApiParam(name="orders", data_type="array<Order>")],
    external_url="https://github.com/acme/order-svc",
)

tool_urn = get_orders.emit(graph)  # urn:li:api:order-svc.get_orders
```

`ApiParam.data_type` accepts friendly hints (`string`, `integer`, `boolean`, `object`,
`array<Order>`, …); the raw value is preserved so a hint like `array<Order>` displays as authored.
For a REST endpoint, add `method="GET"` and `path="/orders"` to emit its HTTP binding.

<b>CLI</b>

```bash
orrery api register --id order-svc.get_orders --name get_orders \
  --subtype MCP_TOOL \
  --param start_date:string:required \
  --param end_date:string:required \
  --param region:string \
  --returns orders:object \
  --description "Fetch orders in a date range."
```

Parameter spec is `NAME:TYPE[:required]`; the returns spec is `NAME:TYPE`.

## Register a skill

A **skill** (`urn:li:agentSkill:<id>`) is a reusable capability bundle — specialized instructions
plus the tools it relies on — defined in git and cataloged in Orrery.

<b>Python SDK</b>

```python
from orrery.api.entities.agent.agent_skill import AgentSkill, SkillSourceRepository

discovery = AgentSkill(
    id="data-discovery",
    name="Data Discovery",
    description="Finds and summarizes relevant datasets.",
    instructions="Use get_orders to retrieve orders, then summarize by region.",
    source_repository=SkillSourceRepository(
        url="https://github.com/acme/skills",
        path="data-discovery/SKILL.md",
    ),
    required_tools=[tool_urn],  # API urns
)

skill_urn = discovery.emit(graph)  # urn:li:agentSkill:data-discovery
```

<b>CLI</b>

```bash
orrery agent-skill register --id data-discovery --name "Data Discovery" \
  --description "Finds and summarizes relevant datasets." \
  --source-repo https://github.com/acme/skills --source-path data-discovery/SKILL.md \
  --requires-tool urn:li:api:order-svc.get_orders
```

## Register an agent

An **agent** (`urn:li:aiAgent:<id>`) ties everything together: the skills it adopts, the tools it
invokes, the model it runs on, and — importantly — the datasets it consumes, which become
**upstream lineage** so the agent appears downstream of its data.

```python
from orrery.api.entities.agent.agent import Agent

agent = Agent(
    id="orders-assistant",
    name="Orders Assistant",
    description="Answers questions about orders.",
    instructions="You are an assistant for the orders team.",
    owners=["urn:li:corpGroup:orders-team"],
    domain="Sales",
    skills=[skill_urn],           # agentSkill urns
    tools=[tool_urn],             # api urns
    models=["urn:li:mlModel:(urn:li:dataPlatform:openai,gpt-4o,PROD)"],
    consumes_datasets=[
        "urn:li:dataset:(urn:li:dataPlatform:snowflake,prod.sales.orders,PROD)",
    ],
    platform="langchain",         # surfaces the framework logo on the profile
)

agent_urn = agent.emit(graph)  # urn:li:aiAgent:orders-assistant
```

`source_type` defaults to `EXTERNAL` (a cataloged, externally-managed agent); use `NATIVE` or
`SYSTEM` for Orrery-managed agents.

:::note CLI for agents
The `orrery agent` command registers an agent from flags or a YAML file
(`orrery agent register …` / `orrery agent upsert -f agent.yml`) **only when the agent context kit
is not installed** — with the kit present, `orrery agent` is that package's command instead. When
in doubt, register agents from the Python SDK (shown above) or a YAML file, both of which are always
available. `orrery api` and `orrery agent-skill` are unaffected.
:::

## Automatic registration from LangChain or Google ADK

Instead of describing the agent by hand, let the **agent context kit** read your running agent and
register it — the agent, the tools it exposes, and the model it uses. Registration is **fail-open**:
if Orrery is unreachable it logs and moves on, never breaking your agent.

Install the extra for your framework:

```bash
pip install "orrery-agent-context[langchain]"     # or [google-adk]
```

Annotate tools once with `@orrery_tool` to attach the datasets they touch and the skill they belong
to — these become the agent's lineage and skill links:

```python
from orrery_agent_context.langchain_registration import orrery_tool

@orrery_tool(
    datasets=["urn:li:dataset:(urn:li:dataPlatform:snowflake,prod.sales.orders,PROD)"],
    skill="data-discovery",
)
def get_order_stats(start_date: str, end_date: str) -> str:
    ...
```

### LangChain

```python
from orrery_agent_context._registration_core import AgentRegistrar
from orrery_agent_context.langchain_registration import (
    OrreryCallbackHandler,
    register_langchain_agent,
)

# Pattern 1 — a callback handler that registers when the agent runs
registrar = AgentRegistrar(
    framework="LangChain",
    framework_version=None,
    platform="langchain",
    agent_id="orders-assistant",
    agent_name="Orders Assistant",
    description="Answers questions about orders.",
)
handler = OrreryCallbackHandler(registrar, tools=[get_order_stats])
# attach `handler` to your AgentExecutor's callbacks; it captures the model on the first LLM call

# Pattern 2 — a one-shot snapshot of an already-built executor
register_langchain_agent(
    agent_executor,
    agent_id="orders-assistant",
    agent_name="Orders Assistant",
    description="Answers questions about orders.",
)
```

### Google ADK

```python
from google.adk.agents import Agent
from orrery_agent_context._registration_core import AgentRegistrar
from orrery_agent_context.google_adk_registration import (
    OrreryBeforeModelCallback,
    register_google_adk_agent,
)

# Pattern 1 — a before-model callback
registrar = AgentRegistrar(
    framework="Google ADK",
    framework_version=None,
    platform="google-adk",
    agent_id="order-assistant",
    agent_name="Order Assistant",
    description="Answers questions about orders.",
)
callback = OrreryBeforeModelCallback(registrar, tools=[get_order_summary])
adk_agent = Agent(
    model="gemini-2.0-flash",
    name="order_assistant",
    tools=[get_order_summary],
    before_model_callback=callback,
)

# Pattern 2 — register an already-built agent eagerly
register_google_adk_agent(
    adk_agent,
    agent_id="order-assistant",
    agent_name="Order Assistant",
    description="Answers questions about orders.",
)
```

The kit infers the model's `mlModel` URN and provider from the model identifier (e.g. `gpt-*` →
OpenAI, `claude-*` → Anthropic, `gemini-*` → Google). Runnable end-to-end examples live in the
package under `examples/langchain/autoregister_agent.py` and
`examples/google_adk/autoregister_google_adk.py`.

:::tip Also: use Orrery as your agent's tools
The same package can build tool lists **from** Orrery — `build_langchain_tools(client)` /
`build_google_adk_tools(client)` (with `include_mutations=True` to allow metadata edits, and cloud
variants for Ask Orrery). See the [Orrery MCP Server](../../features/feature-guides/mcp.md) guide
for connecting agents to Orrery's tools.
:::

## Versioning an agent

Set `version` to group successive releases of an agent into a **version set** — the profile then
shows a version picker, and the highest version is flagged as latest. Each version is its own
`aiAgent` entity (distinct `id`):

```python
Agent(
    id="orders-assistant-v2",
    name="Orders Assistant",
    description="Adds refund lookups.",
    version="2.0",
    version_set="orders-assistant",        # the family these versions share
    version_comment="Added refund-lookup tool.",
    tools=[tool_urn, refund_tool_urn],
).emit(graph)
```

Dotted-numeric versions sort correctly out of the box (`1.9` < `1.10` < `2.0`).

## Related

- [Agent Registry](../../features/feature-guides/agent-registry.md) — the feature guide
- [Service Catalog](../../features/feature-guides/service-catalog.md) — services, APIs, and repositories
- [Orrery MCP Server](../../features/feature-guides/mcp.md) — connecting agents to Orrery's tools
