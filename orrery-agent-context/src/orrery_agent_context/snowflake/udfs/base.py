"""Base utilities for generating Snowflake UDFs."""

import textwrap


def generate_python_udf_code(
    function_name: str,
    parameters: list[tuple[str, str]],
    return_type: str,
    function_body: str,
) -> str:
    """
    Generate the SQL CREATE FUNCTION statement for a Python UDF.

    Args:
        function_name: Name of the UDF to create
        parameters: List of (param_name, param_type) tuples
        return_type: Return type of the function (e.g., 'VARIANT', 'STRING')
        function_body: Python code for the function body (without def statement)

    Returns:
        Complete SQL CREATE FUNCTION statement
    """
    param_signature = ", ".join(f"{name} {type_}" for name, type_ in parameters)
    py_param_names = ", ".join(name for name, _ in parameters)

    udf_template = f"""CREATE OR REPLACE FUNCTION {function_name}({param_signature})
RETURNS {return_type}
LANGUAGE PYTHON
RUNTIME_VERSION = '3.11'
ARTIFACT_REPOSITORY = snowflake.snowpark.pypi_shared_repository
PACKAGES = ('orrery-agent-context==1.4.0.5')
SECRETS = ('orrery_url_secret' = orrery_url, 'orrery_token_secret' = orrery_token)
EXTERNAL_ACCESS_INTEGRATIONS = (orrery_access)
HANDLER = '{function_name.lower()}'
AS $$
import _snowflake
from orrery.sdk.main_client import OrreryClient
from orrery_agent_context.context import OrreryContext

def {function_name.lower()}({py_param_names}):
{textwrap.indent(function_body, "    ")}
$$;"""

    return udf_template
