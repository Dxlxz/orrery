"""REMOVE_OWNERS UDF generator."""

from orrery_agent_context.snowflake.udfs.base import generate_python_udf_code


def generate_remove_owners_udf() -> str:
    """Generate REMOVE_OWNERS UDF using orrery-agent-context.

    This UDF wraps orrery_agent_context.mcp_tools.remove_owners() to remove
    owners from multiple Orrery entities.

    Parameters:
        owner_urns (STRING): JSON array of owner URNs to remove
        entity_urns (STRING): JSON array of entity URNs to remove ownership from
        ownership_type_urn (STRING): Optional ownership type URN (use NULL for all types)

    Returns:
        VARIANT: Dictionary with success status and message
    """
    function_body = """from orrery_agent_context.mcp_tools import remove_owners
import json
try:
    orrery_url = _snowflake.get_generic_secret_string('orrery_url_secret')
    orrery_token = _snowflake.get_generic_secret_string('orrery_token_secret')
    orrery_url = orrery_url.rstrip('/')

    client = OrreryClient(server=orrery_url, token=orrery_token)

    owner_urn_list = json.loads(owner_urns) if isinstance(owner_urns, str) else owner_urns
    entity_urn_list = json.loads(entity_urns) if isinstance(entity_urns, str) else entity_urns

    with OrreryContext(client):
        return remove_owners(
            owner_urns=owner_urn_list,
            entity_urns=entity_urn_list,
            ownership_type_urn=ownership_type_urn if ownership_type_urn else None
        )

except Exception as e:
    return {
        'success': False,
        'error': str(e)
    }"""

    return generate_python_udf_code(
        function_name="REMOVE_OWNERS",
        parameters=[
            ("owner_urns", "STRING"),
            ("entity_urns", "STRING"),
            ("ownership_type_urn", "STRING"),
        ],
        return_type="VARIANT",
        function_body=function_body,
    )
