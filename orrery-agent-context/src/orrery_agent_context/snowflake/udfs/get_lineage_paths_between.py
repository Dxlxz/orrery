"""GET_LINEAGE_PATHS_BETWEEN UDF generator."""

from orrery_agent_context.snowflake.udfs.base import generate_python_udf_code


def generate_get_lineage_paths_between_udf() -> str:
    """Generate GET_LINEAGE_PATHS_BETWEEN UDF using orrery-agent-context.

    This UDF wraps orrery_agent_context.mcp_tools.get_lineage_paths_between() to get
    detailed lineage paths between two specific entities or columns.

    Returns the paths array showing the exact transformation chain(s) including
    intermediate entities and transformation query URNs.

    Parameters:
        source_urn (STRING): URN of the source dataset
        target_urn (STRING): URN of the target dataset
        source_column (STRING): Optional column name in source dataset (use NULL for dataset-level)
        target_column (STRING): Optional column name in target dataset (use NULL for dataset-level)

    Returns:
        VARIANT: Dictionary with:
                - source: Source entity/column info
                - target: Target entity/column info
                - paths: Array of path objects showing transformation chains
                - pathCount: Number of paths found
                - metadata: Query metadata including direction and path type

    Examples:
        - Dataset-level: GET_LINEAGE_PATHS_BETWEEN(source_urn, target_urn, NULL, NULL)
        - Column-level: GET_LINEAGE_PATHS_BETWEEN(source_urn, target_urn, 'user_id', 'customer_id')
    """
    function_body = """from orrery_agent_context.mcp_tools import get_lineage_paths_between
try:
    orrery_url = _snowflake.get_generic_secret_string('orrery_url_secret')
    orrery_token = _snowflake.get_generic_secret_string('orrery_token_secret')
    orrery_url = orrery_url.rstrip('/')

    client = OrreryClient(server=orrery_url, token=orrery_token)

    with OrreryContext(client):
        return get_lineage_paths_between(
            source_urn=source_urn,
            target_urn=target_urn,
            source_column=source_column if source_column else None,
            target_column=target_column if target_column else None
        )

except Exception as e:
    return {
        'success': False,
        'error': str(e),
        'source_urn': source_urn,
        'target_urn': target_urn
    }"""

    return generate_python_udf_code(
        function_name="GET_LINEAGE_PATHS_BETWEEN",
        parameters=[
            ("source_urn", "STRING"),
            ("target_urn", "STRING"),
            ("source_column", "STRING"),
            ("target_column", "STRING"),
        ],
        return_type="VARIANT",
        function_body=function_body,
    )
