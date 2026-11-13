"""MCP tools for interacting with Orrery metadata."""

from orrery_agent_context.mcp_tools.ask_orrery import ask_orrery_chat, get_orrery_chat
from orrery_agent_context.mcp_tools.assertions import get_dataset_assertions
from orrery_agent_context.mcp_tools.descriptions import update_description
from orrery_agent_context.mcp_tools.documents import grep_documents, search_documents
from orrery_agent_context.mcp_tools.domains import remove_domains, set_domains
from orrery_agent_context.mcp_tools.entities import get_entities, list_schema_fields
from orrery_agent_context.mcp_tools.get_me import get_me
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
from orrery_agent_context.mcp_tools.structured_properties import (
    add_structured_properties,
    remove_structured_properties,
)
from orrery_agent_context.mcp_tools.tags import add_tags, remove_tags
from orrery_agent_context.mcp_tools.terms import (
    add_glossary_terms,
    remove_glossary_terms,
)

__all__ = [
    "search",
    "get_entities",
    "list_schema_fields",
    "get_lineage",
    "get_lineage_paths_between",
    "get_dataset_queries",
    "get_dataset_assertions",
    "list_incidents",
    "search_documents",
    "grep_documents",
    "add_tags",
    "remove_tags",
    "update_description",
    "set_domains",
    "remove_domains",
    "add_owners",
    "remove_owners",
    "add_glossary_terms",
    "remove_glossary_terms",
    "add_structured_properties",
    "remove_structured_properties",
    "get_me",
    "save_document",
    "raise_incident",
    "resolve_incident",
    # Cloud-only tools
    "ask_orrery_chat",
    "get_orrery_chat",
]
