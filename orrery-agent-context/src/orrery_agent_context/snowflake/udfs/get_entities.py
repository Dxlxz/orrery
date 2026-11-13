"""GET_ENTITIES UDF generator."""

from orrery_agent_context.snowflake.udfs.base import generate_python_udf_code


def generate_get_entities_udf() -> str:
    """Generate GET_ENTITIES UDF using orrery-agent-context.

    This UDF wraps orrery_agent_context.mcp_tools.get_entities() to retrieve
    detailed information about entities by their Orrery URNs from Snowflake.

    The underlying function accepts arrays of URNs for efficient batch retrieval,
    but this UDF is simplified to accept a single URN string.

    Parameters:
        entity_urn (STRING): Entity URN (e.g., "urn:li:dataset:(...)")

    Returns:
        VARIANT: Dictionary with entity details including schema metadata, ownership,
                tags, glossary terms, and other metadata aspects
    """
    function_body = """from orrery_agent_context.mcp_tools import get_entities
try:
    orrery_url = _snowflake.get_generic_secret_string('orrery_url_secret')
    orrery_token = _snowflake.get_generic_secret_string('orrery_token_secret')
    orrery_url = orrery_url.rstrip('/')

    client = OrreryClient(server=orrery_url, token=orrery_token)

    with OrreryContext(client):
        results = get_entities([entity_urn])
        return results[0]

except Exception as e:
    return {
        'success': False,
        'error': str(e),
        'urn': entity_urn
    }"""

    return generate_python_udf_code(
        function_name="GET_ENTITIES",
        parameters=[("entity_urn", "STRING")],
        return_type="VARIANT",
        function_body=function_body,
    )
