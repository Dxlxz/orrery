"""Builder for LangChain tools from Orrery MCP tools."""

from typing import TYPE_CHECKING

from orrery_agent_context.mcp_tools import get_me
from orrery_agent_context.mcp_tools.ask_orrery import ask_orrery_chat, get_orrery_chat
from orrery_agent_context.mcp_tools.assertions import get_dataset_assertions
from orrery_agent_context.mcp_tools.documents import grep_documents, search_documents
from orrery_agent_context.mcp_tools.domains import remove_domains, set_domains
from orrery_agent_context.mcp_tools.structured_properties import (
    add_structured_properties,
    remove_structured_properties,
)
from orrery_agent_context.utils import create_context_wrapper

if TYPE_CHECKING:
    from orrery.sdk.main_client import OrreryClient

try:
    from langchain_core.tools import tool  # type: ignore[import-not-found]
    from langchain_core.tools.base import BaseTool  # type: ignore[import-not-found]
except ImportError as e:
    raise ImportError(
        "langchain-core is required for LangChain tools. "
        "Install with: pip install 'orrery-agent-context[langchain]'"
    ) from e

from orrery_agent_context.mcp_tools.descriptions import update_description
from orrery_agent_context.mcp_tools.entities import get_entities, list_schema_fields
from orrery_agent_context.mcp_tools.incidents import (
    list_incidents,
    raise_incident,
    resolve_incident,
)
from orrery_agent_context.mcp_tools.lineage import (
    get_lineage,
    get_lineage_paths_between,
)
from orrery_agent_context.mcp_tools.owners import add_owners, remove_owners
from orrery_agent_context.mcp_tools.queries import get_dataset_queries
from orrery_agent_context.mcp_tools.save_document import save_document
from orrery_agent_context.mcp_tools.search import search
from orrery_agent_context.mcp_tools.tags import add_tags, remove_tags
from orrery_agent_context.mcp_tools.terms import (
    add_glossary_terms,
    remove_glossary_terms,
)


def build_langchain_tools(
    client: "OrreryClient",
    include_mutations: bool = False,
) -> list[BaseTool]:
    """Build LangChain tools with automatic context management.

    Each tool is wrapped to automatically set the OrreryGraph in context
    before execution, allowing tools to retrieve it using get_graph().

    Args:
        client: OrreryClient instance
        include_mutations: Whether to include mutation tools (default: False)

    Returns:
        List of LangChain BaseTool instances

    Example:
        from orrery.sdk.main_client import OrreryClient
        from orrery_agent_context.langchain_tools import build_langchain_tools

        client = OrreryClient(...)
        tools = build_langchain_tools(client, include_mutations=True)

        # Use with LangChain agents - context is managed automatically
        agent = create_react_agent(llm, tools, prompt)
        result = agent.invoke({"input": "search for datasets"})
    """
    tools = []

    # Wrap each tool to manage context automatically
    tools.append(tool(create_context_wrapper(search_documents, client)))
    tools.append(tool(create_context_wrapper(grep_documents, client)))

    tools.append(tool(create_context_wrapper(get_entities, client)))
    tools.append(tool(create_context_wrapper(list_schema_fields, client)))
    tools.append(tool(create_context_wrapper(get_me, client)))
    tools.append(tool(create_context_wrapper(get_lineage, client)))
    tools.append(tool(create_context_wrapper(get_lineage_paths_between, client)))

    tools.append(tool(create_context_wrapper(get_dataset_queries, client)))
    tools.append(tool(create_context_wrapper(get_dataset_assertions, client)))
    tools.append(tool(create_context_wrapper(list_incidents, client)))
    tools.append(tool(create_context_wrapper(search, client)))

    if include_mutations:
        tools.append(tool(create_context_wrapper(update_description, client)))
        tools.append(tool(create_context_wrapper(set_domains, client)))
        tools.append(tool(create_context_wrapper(remove_domains, client)))
        tools.append(tool(create_context_wrapper(add_owners, client)))
        tools.append(tool(create_context_wrapper(remove_owners, client)))
        tools.append(tool(create_context_wrapper(add_structured_properties, client)))
        tools.append(tool(create_context_wrapper(remove_structured_properties, client)))
        tools.append(tool(create_context_wrapper(add_tags, client)))
        tools.append(tool(create_context_wrapper(remove_tags, client)))
        tools.append(tool(create_context_wrapper(add_glossary_terms, client)))
        tools.append(tool(create_context_wrapper(remove_glossary_terms, client)))
        tools.append(tool(create_context_wrapper(save_document, client)))
        tools.append(tool(create_context_wrapper(raise_incident, client)))
        tools.append(tool(create_context_wrapper(resolve_incident, client)))

    return tools


def build_langchain_cloud_tools(
    client: "OrreryClient",
    *,
    ask_orrery: bool = True,
) -> list[BaseTool]:
    """Build LangChain tools for Orrery Cloud features.

    Returns tools that are only available on Orrery Cloud instances.
    Use alongside ``build_langchain_tools`` to get the full suite::

        tools = build_langchain_tools(client, include_mutations=True)
        tools += build_langchain_cloud_tools(client, ask_orrery=True)

    Args:
        client: OrreryClient instance (must be connected to a Cloud instance)
        ask_orrery: Include the Ask Orrery AI chat tools (default: True)

    Returns:
        List of LangChain BaseTool instances for Cloud-only features
    """
    tools: list[BaseTool] = []

    if ask_orrery:
        tools.append(tool(create_context_wrapper(ask_orrery_chat, client)))
        tools.append(tool(create_context_wrapper(get_orrery_chat, client)))

    return tools
