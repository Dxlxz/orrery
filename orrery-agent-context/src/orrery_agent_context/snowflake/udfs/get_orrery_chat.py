"""GET_ORRERY_CHAT UDF generator."""

from orrery_agent_context.snowflake.udfs.base import generate_python_udf_code


def generate_get_orrery_chat_udf() -> str:
    """Generate GET_ORRERY_CHAT UDF using orrery-agent-context.

    This UDF wraps orrery_agent_context.mcp_tools.get_orrery_chat() to
    retrieve messages and status from an Ask Orrery conversation.

    Cloud-only: requires a Orrery Cloud instance.

    Parameters:
        conversation_urn (STRING): URN of the conversation to retrieve.
        message_limit (NUMBER): Maximum number of messages to return. Default 10.
        offset (NUMBER): Number of most-recent messages to skip. Default 0.

    Returns:
        VARIANT: Dictionary with conversation_urn, status, messages, and response.

    Examples:
        - Get latest: GET_ORRERY_CHAT('urn:li:orreryAiConversation:abc', 10, 0)
        - Page older: GET_ORRERY_CHAT('urn:li:orreryAiConversation:abc', 10, 10)
    """
    function_body = """from orrery_agent_context.mcp_tools import get_orrery_chat
try:
    orrery_url = _snowflake.get_generic_secret_string('orrery_url_secret')
    orrery_token = _snowflake.get_generic_secret_string('orrery_token_secret')
    orrery_url = orrery_url.rstrip('/')

    client = OrreryClient(server=orrery_url, token=orrery_token)

    with OrreryContext(client):
        return get_orrery_chat(
            conversation_urn=conversation_urn,
            message_limit=int(message_limit),
            offset=int(offset),
        )

except Exception as e:
    return {
        'success': False,
        'error': str(e),
        'conversation_urn': conversation_urn
    }"""

    return generate_python_udf_code(
        function_name="GET_ORRERY_CHAT",
        parameters=[
            ("conversation_urn", "STRING"),
            ("message_limit", "NUMBER"),
            ("offset", "NUMBER"),
        ],
        return_type="VARIANT",
        function_body=function_body,
    )
