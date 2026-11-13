"""SET_DOMAINS UDF generator."""

from orrery_agent_context.snowflake.udfs.base import generate_python_udf_code


def generate_set_domains_udf() -> str:
    """Generate SET_DOMAINS UDF using orrery-agent-context.

    This UDF wraps orrery_agent_context.mcp_tools.set_domains() to assign a domain
    to multiple Orrery entities.

    Parameters:
        domain_urn (STRING): Domain URN to assign (e.g., 'urn:li:domain:marketing')
        entity_urns (STRING): JSON array of entity URNs to assign to the domain

    Returns:
        VARIANT: Dictionary with success status and message

    Example:
        - Set domain: SET_DOMAINS('urn:li:domain:marketing', '["urn:li:dataset:(...)"]')
    """
    function_body = """from orrery_agent_context.mcp_tools import set_domains
import json
try:
    orrery_url = _snowflake.get_generic_secret_string('orrery_url_secret')
    orrery_token = _snowflake.get_generic_secret_string('orrery_token_secret')
    orrery_url = orrery_url.rstrip('/')

    client = OrreryClient(server=orrery_url, token=orrery_token)

    entity_urn_list = json.loads(entity_urns) if isinstance(entity_urns, str) else entity_urns

    with OrreryContext(client):
        return set_domains(
            domain_urn=domain_urn,
            entity_urns=entity_urn_list
        )

except Exception as e:
    return {
        'success': False,
        'error': str(e)
    }"""

    return generate_python_udf_code(
        function_name="SET_DOMAINS",
        parameters=[
            ("domain_urn", "STRING"),
            ("entity_urns", "STRING"),
        ],
        return_type="VARIANT",
        function_body=function_body,
    )
