"""GET_ME UDF generator."""

from orrery_agent_context.snowflake.udfs.base import generate_python_udf_code


def generate_get_me_udf() -> str:
    """Generate GET_ME UDF using orrery-agent-context.

    This UDF wraps orrery_agent_context.mcp_tools.get_me() to get information
    about the currently authenticated user.

    Returns user profile information, platform privileges, group memberships,
    and user settings.

    Parameters:
        None

    Returns:
        VARIANT: Dictionary with:
                - success: Boolean indicating if operation succeeded
                - data: User information including corpUser details
                - message: Success or error message

    Example:
        - Get current user: SELECT GET_ME()
    """
    function_body = """from orrery_agent_context.mcp_tools import get_me
try:
    orrery_url = _snowflake.get_generic_secret_string('orrery_url_secret')
    orrery_token = _snowflake.get_generic_secret_string('orrery_token_secret')
    orrery_url = orrery_url.rstrip('/')

    client = OrreryClient(server=orrery_url, token=orrery_token)

    with OrreryContext(client):
        return get_me()

except Exception as e:
    return {
        'success': False,
        'error': str(e)
    }"""

    return generate_python_udf_code(
        function_name="GET_ME",
        parameters=[],
        return_type="VARIANT",
        function_body=function_body,
    )
