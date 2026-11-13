"""ASK_ORRERY_CHAT UDF generator."""

from orrery_agent_context.snowflake.udfs.base import generate_python_udf_code


def generate_ask_orrery_chat_udf() -> str:
    """Generate ASK_ORRERY_CHAT UDF using orrery-agent-context.

    This UDF wraps orrery_agent_context.mcp_tools.ask_orrery_chat() to send
    questions to the Orrery AI assistant from Snowflake. Blocks until the
    agent responds. Each call creates a new conversation.

    Cloud-only: requires a Orrery Cloud instance.

    Parameters:
        message (STRING): The question to ask the AI agent.

    Returns:
        VARIANT: Dictionary with conversation_urn and response.

    Examples:
        - ASK_ORRERY_CHAT('What are the most queried datasets?')
        - ASK_ORRERY_CHAT('Show me the schema of the orders table')
    """
    function_body = """from orrery_agent_context.mcp_tools import ask_orrery_chat
try:
    orrery_url = _snowflake.get_generic_secret_string('orrery_url_secret')
    orrery_token = _snowflake.get_generic_secret_string('orrery_token_secret')
    orrery_url = orrery_url.rstrip('/')

    client = OrreryClient(server=orrery_url, token=orrery_token)

    with OrreryContext(client):
        return ask_orrery_chat(
            message=message,
        )

except Exception as e:
    return {
        'success': False,
        'error': str(e),
        'message': message
    }"""

    return generate_python_udf_code(
        function_name="ASK_ORRERY_CHAT",
        parameters=[("message", "STRING")],
        return_type="VARIANT",
        function_body=function_body,
    )
