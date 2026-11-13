"""Tests for Ask Orrery chat tools (agent-context version)."""

from typing import Any
from unittest.mock import MagicMock, Mock, patch

import pytest

from orrery_agent_context.mcp_tools.ask_orrery import (
    _determine_completion_status,
    _extract_text_messages,
    ask_orrery_chat,
    get_orrery_chat,
)

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _user_msg(text: str, time: int) -> dict[str, Any]:
    return {
        "type": "TEXT",
        "time": time,
        "actor": {"type": "USER", "actor": "urn:li:corpuser:alice"},
        "content": {"text": text},
    }


def _agent_msg(text: str, time: int) -> dict[str, Any]:
    return {
        "type": "TEXT",
        "time": time,
        "actor": {"type": "AGENT", "actor": "urn:li:corpuser:orrery-ai"},
        "content": {"text": text},
    }


def _thinking_msg(text: str, time: int) -> dict[str, Any]:
    return {
        "type": "THINKING",
        "time": time,
        "actor": {"type": "AGENT", "actor": "urn:li:corpuser:orrery-ai"},
        "content": {"text": text},
    }


def _mock_graphql_conversation(messages: list[dict[str, Any]]) -> dict[str, Any]:
    return {
        "getOrreryAiConversation": {
            "urn": "urn:li:orreryAiConversation:test1",
            "title": "Test",
            "originType": "MCP",
            "lastMessageTime": 400,
            "messages": messages,
        }
    }


@pytest.fixture
def cloud_graph():
    """Mock graph that reports as Cloud."""
    graph = MagicMock()
    graph.frontend_base_url = "https://mycompany.orrery.dev"
    graph._gms_server = "https://mycompany.orrery.dev/gms"
    return graph


@pytest.fixture
def oss_graph():
    """Mock graph that reports as OSS (no frontend_base_url)."""
    graph = Mock(spec=[])
    return graph


# ---------------------------------------------------------------------------
# _extract_text_messages
# ---------------------------------------------------------------------------


def test_extract_text_messages_filters_non_text() -> None:
    msgs = [
        _user_msg("hello", 100),
        _thinking_msg("hmm...", 200),
        _agent_msg("hi there", 300),
    ]
    result = _extract_text_messages(msgs)
    assert len(result) == 2
    assert result[0]["role"] == "user"
    assert result[1]["role"] == "assistant"


def test_extract_text_messages_empty_list() -> None:
    assert _extract_text_messages([]) == []


# ---------------------------------------------------------------------------
# _determine_completion_status
# ---------------------------------------------------------------------------


def test_completion_status_agent_responded() -> None:
    msgs = [_user_msg("q", 100), _agent_msg("a", 200)]
    is_complete, response = _determine_completion_status(msgs)
    assert is_complete is True
    assert response == "a"


def test_completion_status_still_processing() -> None:
    msgs = [_user_msg("q", 100)]
    is_complete, response = _determine_completion_status(msgs)
    assert is_complete is False


def test_completion_status_follow_up_pending() -> None:
    msgs = [_user_msg("q1", 100), _agent_msg("a1", 200), _user_msg("q2", 300)]
    is_complete, _ = _determine_completion_status(msgs)
    assert is_complete is False


# ---------------------------------------------------------------------------
# get_orrery_chat
# ---------------------------------------------------------------------------


@patch("orrery_agent_context.mcp_tools.ask_orrery.execute_graphql")
@patch("orrery_agent_context.mcp_tools.ask_orrery.get_graph")
def test_get_chat_complete(
    mock_get_graph: MagicMock, mock_gql: MagicMock, cloud_graph: MagicMock
) -> None:
    mock_get_graph.return_value = cloud_graph
    mock_gql.return_value = _mock_graphql_conversation(
        [_user_msg("q", 100), _agent_msg("a", 200)]
    )

    result = get_orrery_chat("urn:li:orreryAiConversation:test1")

    assert result["status"] == "complete"
    assert result["response"] == "a"
    assert result["total_messages"] == 2
    assert result["messages"][0]["role"] == "assistant"


@patch("orrery_agent_context.mcp_tools.ask_orrery.execute_graphql")
@patch("orrery_agent_context.mcp_tools.ask_orrery.get_graph")
def test_get_chat_processing(
    mock_get_graph: MagicMock, mock_gql: MagicMock, cloud_graph: MagicMock
) -> None:
    mock_get_graph.return_value = cloud_graph
    mock_gql.return_value = _mock_graphql_conversation([_user_msg("q", 100)])

    result = get_orrery_chat("urn:li:orreryAiConversation:test1")

    assert result["status"] == "processing"
    assert "response" not in result


@patch("orrery_agent_context.mcp_tools.ask_orrery.execute_graphql")
@patch("orrery_agent_context.mcp_tools.ask_orrery.get_graph")
def test_get_chat_not_found(
    mock_get_graph: MagicMock, mock_gql: MagicMock, cloud_graph: MagicMock
) -> None:
    mock_get_graph.return_value = cloud_graph
    mock_gql.return_value = {"getOrreryAiConversation": None}

    with pytest.raises(RuntimeError, match="not found"):
        get_orrery_chat("urn:li:orreryAiConversation:missing")


@patch("orrery_agent_context.mcp_tools.ask_orrery.execute_graphql")
@patch("orrery_agent_context.mcp_tools.ask_orrery.get_graph")
def test_get_chat_pagination(
    mock_get_graph: MagicMock, mock_gql: MagicMock, cloud_graph: MagicMock
) -> None:
    mock_get_graph.return_value = cloud_graph
    messages = []
    for i in range(3):
        messages.append(_user_msg(f"q{i}", 100 + i * 200))
        messages.append(_agent_msg(f"a{i}", 200 + i * 200))
    mock_gql.return_value = _mock_graphql_conversation(messages)

    page1 = get_orrery_chat(
        "urn:li:orreryAiConversation:test1", message_limit=2, offset=0
    )
    assert len(page1["messages"]) == 2
    assert page1["total_messages"] == 6
    assert page1["messages"][0]["text"] == "a2"


# ---------------------------------------------------------------------------
# get_orrery_chat — OSS guard
# ---------------------------------------------------------------------------


@patch("orrery_agent_context.mcp_tools.ask_orrery.get_graph")
def test_get_chat_raises_on_oss(mock_get_graph: MagicMock, oss_graph: Mock) -> None:
    mock_get_graph.return_value = oss_graph

    with pytest.raises(RuntimeError, match="Cloud"):
        get_orrery_chat("urn:li:orreryAiConversation:test1")


# ---------------------------------------------------------------------------
# ask_orrery_chat
# ---------------------------------------------------------------------------


@patch("orrery_agent_context.mcp_tools.ask_orrery._fetch_last_agent_response")
@patch("orrery_agent_context.mcp_tools.ask_orrery._consume_chat_stream")
@patch("orrery_agent_context.mcp_tools.ask_orrery._create_conversation")
@patch("orrery_agent_context.mcp_tools.ask_orrery.get_graph")
def test_ask_chat_new_conversation(
    mock_get_graph: MagicMock,
    mock_create: MagicMock,
    mock_stream: MagicMock,
    mock_fetch: MagicMock,
    cloud_graph: MagicMock,
) -> None:
    mock_get_graph.return_value = cloud_graph
    mock_create.return_value = "urn:li:orreryAiConversation:new1"
    mock_fetch.return_value = "The answer"

    result = ask_orrery_chat(message="What datasets?")

    assert result["conversation_urn"] == "urn:li:orreryAiConversation:new1"
    assert result["response"] == "The answer"
    mock_create.assert_called_once()
    mock_stream.assert_called_once_with(
        "urn:li:orreryAiConversation:new1", "What datasets?"
    )


@patch("orrery_agent_context.mcp_tools.ask_orrery._fetch_last_agent_response")
@patch("orrery_agent_context.mcp_tools.ask_orrery._consume_chat_stream")
@patch("orrery_agent_context.mcp_tools.ask_orrery.get_graph")
def test_ask_chat_existing_conversation(
    mock_get_graph: MagicMock,
    mock_stream: MagicMock,
    mock_fetch: MagicMock,
    cloud_graph: MagicMock,
) -> None:
    mock_get_graph.return_value = cloud_graph
    mock_fetch.return_value = "Follow-up answer"

    existing = "urn:li:orreryAiConversation:existing1"
    result = ask_orrery_chat(message="Follow up", conversation_urn=existing)

    assert result["conversation_urn"] == existing
    assert result["response"] == "Follow-up answer"
    mock_stream.assert_called_once_with(existing, "Follow up")


@patch("orrery_agent_context.mcp_tools.ask_orrery._consume_chat_stream")
@patch("orrery_agent_context.mcp_tools.ask_orrery._create_conversation")
@patch("orrery_agent_context.mcp_tools.ask_orrery.get_graph")
def test_ask_chat_stream_error(
    mock_get_graph: MagicMock,
    mock_create: MagicMock,
    mock_stream: MagicMock,
    cloud_graph: MagicMock,
) -> None:
    mock_get_graph.return_value = cloud_graph
    mock_create.return_value = "urn:li:orreryAiConversation:err1"
    mock_stream.side_effect = RuntimeError("Connection failed")

    with pytest.raises(RuntimeError, match="Connection failed"):
        ask_orrery_chat(message="Boom")


@patch("orrery_agent_context.mcp_tools.ask_orrery._fetch_last_agent_response")
@patch("orrery_agent_context.mcp_tools.ask_orrery._consume_chat_stream")
@patch("orrery_agent_context.mcp_tools.ask_orrery._create_conversation")
@patch("orrery_agent_context.mcp_tools.ask_orrery.get_graph")
def test_ask_chat_no_response_found(
    mock_get_graph: MagicMock,
    mock_create: MagicMock,
    mock_stream: MagicMock,
    mock_fetch: MagicMock,
    cloud_graph: MagicMock,
) -> None:
    mock_get_graph.return_value = cloud_graph
    mock_create.return_value = "urn:li:orreryAiConversation:empty1"
    mock_fetch.return_value = None

    with pytest.raises(RuntimeError, match="no response was found"):
        ask_orrery_chat(message="Hello?")


# ---------------------------------------------------------------------------
# ask_orrery_chat — OSS guard
# ---------------------------------------------------------------------------


@patch("orrery_agent_context.mcp_tools.ask_orrery.get_graph")
def test_ask_chat_raises_on_oss(mock_get_graph: MagicMock, oss_graph: Mock) -> None:
    mock_get_graph.return_value = oss_graph

    with pytest.raises(RuntimeError, match="Cloud"):
        ask_orrery_chat(message="Hello")
