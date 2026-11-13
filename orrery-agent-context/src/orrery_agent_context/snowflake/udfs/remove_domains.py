"""REMOVE_DOMAINS UDF generator."""

from orrery_agent_context.snowflake.udfs.base import generate_python_udf_code


def generate_remove_domains_udf() -> str:
    """Generate REMOVE_DOMAINS UDF using orrery-agent-context.

    This UDF wraps orrery_agent_context.mcp_tools.remove_domains() to remove
    domain assignments from multiple Orrery entities.

    Parameters:
        entity_urns (STRING): JSON array of entity URNs to remove domain from

    Returns:
        VARIANT: Dictionary with success status and message
    """
    function_body = """from orrery_agent_context.mcp_tools import remove_domains
import json
try:
    orrery_url = _snowflake.get_generic_secret_string('orrery_url_secret')
    orrery_token = _snowflake.get_generic_secret_string('orrery_token_secret')
    orrery_url = orrery_url.rstrip('/')

    client = OrreryClient(server=orrery_url, token=orrery_token)

    entity_urn_list = json.loads(entity_urns) if isinstance(entity_urns, str) else entity_urns

    with OrreryContext(client):
        return remove_domains(entity_urns=entity_urn_list)

except Exception as e:
    return {
        'success': False,
        'error': str(e)
    }"""

    return generate_python_udf_code(
        function_name="REMOVE_DOMAINS",
        parameters=[("entity_urns", "STRING")],
        return_type="VARIANT",
        function_body=function_body,
    )
