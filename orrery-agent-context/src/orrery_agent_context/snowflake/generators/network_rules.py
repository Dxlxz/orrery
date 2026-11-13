"""Generate Snowflake network rules SQL."""


def generate_network_rules_sql(orrery_domain: str) -> str:
    """Generate network rules SQL that uses configuration variables.

    Args:
        orrery_domain: The domain extracted from the Orrery URL (e.g., 'fieldeng.orrery.dev')
    """
    return f"""-- ============================================================================
-- Step 1: Network Rules and External Access Integration for Orrery API
-- ============================================================================
-- This script creates the network rule and external access integration
-- required for UDFs to make outbound calls to Orrery
--
-- Prerequisites:
-- - Run 00_configuration.sql first to set variables
-- - You must have ACCOUNTADMIN role or equivalent privileges
-- ============================================================================

-- Use the configured database and schema
USE DATABASE IDENTIFIER($SF_DATABASE);
USE SCHEMA IDENTIFIER($SF_SCHEMA);
USE WAREHOUSE IDENTIFIER($SF_WAREHOUSE);

-- Step 1: Create network rule for Orrery API
-- For EGRESS mode with external access, use HOST_PORT type with domain name
-- Note: Snowflake resolves the domain to IPs automatically
CREATE OR REPLACE NETWORK RULE orrery_api_rule
  MODE = EGRESS
  TYPE = HOST_PORT
  VALUE_LIST = ('{orrery_domain}')
  COMMENT = 'Network rule for {orrery_domain} Orrery API access';

-- Step 2: Create external access integration
CREATE OR REPLACE EXTERNAL ACCESS INTEGRATION orrery_access
  ALLOWED_NETWORK_RULES = (orrery_api_rule)
  ALLOWED_AUTHENTICATION_SECRETS = (orrery_url, orrery_token)
  ENABLED = TRUE
  COMMENT = 'External access integration for Orrery API calls';

-- Step 3: Grant usage on integration to the specified role
GRANT USAGE ON INTEGRATION orrery_access TO ROLE IDENTIFIER($SF_ROLE);

-- Verify the setup
SHOW NETWORK RULES LIKE 'orrery_api_rule';
SHOW INTEGRATIONS LIKE 'orrery_access';

SELECT
    'Network rule and external access integration created successfully!' AS status,
    '{orrery_domain}' AS orrery_domain,
    $SF_DATABASE || '.' || $SF_SCHEMA AS location;
"""
