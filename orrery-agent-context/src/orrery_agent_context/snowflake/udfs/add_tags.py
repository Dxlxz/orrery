"""ADD_TAGS UDF generator."""

from orrery_agent_context.snowflake.udfs.base import generate_python_udf_code


def generate_add_tags_udf() -> str:
    """Generate ADD_TAGS UDF using orrery-agent-context.

    This UDF wraps orrery_agent_context.mcp_tools.add_tags() to add tags to
    multiple Orrery entities or their columns in a single operation.

    Parameters:
        tag_urns (STRING): JSON array of tag URNs (e.g., '["urn:li:tag:PII", "urn:li:tag:Sensitive"]')
        entity_urns (STRING): JSON array of entity URNs to tag
        column_paths (STRING): Optional JSON array of column names (use NULL for entity-level tags)

    Returns:
        VARIANT: Dictionary with success status and message

    Examples:
        - Tag datasets: ADD_TAGS('["urn:li:tag:PII"]', '["urn:li:dataset:(...)"]', NULL)
        - Tag columns: ADD_TAGS('["urn:li:tag:PII"]', '["urn:li:dataset:(...)"]', '["email"]')
    """
    function_body = """from orrery_agent_context.mcp_tools import add_tags
import json
try:
    orrery_url = _snowflake.get_generic_secret_string('orrery_url_secret')
    orrery_token = _snowflake.get_generic_secret_string('orrery_token_secret')
    orrery_url = orrery_url.rstrip('/')

    client = OrreryClient(server=orrery_url, token=orrery_token)

    tag_urn_list = json.loads(tag_urns) if isinstance(tag_urns, str) else tag_urns
    entity_urn_list = json.loads(entity_urns) if isinstance(entity_urns, str) else entity_urns
    column_path_list = json.loads(column_paths) if column_paths and isinstance(column_paths, str) else None

    with OrreryContext(client):
        return add_tags(
            tag_urns=tag_urn_list,
            entity_urns=entity_urn_list,
            column_paths=column_path_list
        )

except Exception as e:
    return {
        'success': False,
        'error': str(e)
    }"""

    return generate_python_udf_code(
        function_name="ADD_TAGS",
        parameters=[
            ("tag_urns", "STRING"),
            ("entity_urns", "STRING"),
            ("column_paths", "STRING"),
        ],
        return_type="VARIANT",
        function_body=function_body,
    )
