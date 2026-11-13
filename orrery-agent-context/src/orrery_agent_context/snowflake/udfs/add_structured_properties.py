"""ADD_STRUCTURED_PROPERTIES UDF generator."""

from orrery_agent_context.snowflake.udfs.base import generate_python_udf_code


def generate_add_structured_properties_udf() -> str:
    """Generate ADD_STRUCTURED_PROPERTIES UDF using orrery-agent-context.

    This UDF wraps orrery_agent_context.mcp_tools.add_structured_properties() to
    add structured properties with values to multiple Orrery entities.

    Parameters:
        property_values (STRING): JSON object mapping property URNs to value arrays
                                 (e.g., '{"urn:li:structuredProperty:retentionTime": ["90"]}')
        entity_urns (STRING): JSON array of entity URNs to assign properties to

    Returns:
        VARIANT: Dictionary with success status and message

    Example:
        - Add property: ADD_STRUCTURED_PROPERTIES('{"urn:li:structuredProperty:retentionTime": ["90"]}', '["urn:li:dataset:(...)"]')
    """
    function_body = """from orrery_agent_context.mcp_tools import add_structured_properties
import json
try:
    orrery_url = _snowflake.get_generic_secret_string('orrery_url_secret')
    orrery_token = _snowflake.get_generic_secret_string('orrery_token_secret')
    orrery_url = orrery_url.rstrip('/')

    client = OrreryClient(server=orrery_url, token=orrery_token)

    property_values_dict = json.loads(property_values) if isinstance(property_values, str) else property_values
    entity_urn_list = json.loads(entity_urns) if isinstance(entity_urns, str) else entity_urns

    with OrreryContext(client):
        return add_structured_properties(
            property_values=property_values_dict,
            entity_urns=entity_urn_list
        )

except Exception as e:
    return {
        'success': False,
        'error': str(e)
    }"""

    return generate_python_udf_code(
        function_name="ADD_STRUCTURED_PROPERTIES",
        parameters=[
            ("property_values", "STRING"),
            ("entity_urns", "STRING"),
        ],
        return_type="VARIANT",
        function_body=function_body,
    )
