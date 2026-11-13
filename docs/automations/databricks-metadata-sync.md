---
description: "Sync Orrery tags, terms, owners, and descriptions to Databricks Unity Catalog using Orrery Cloud's Databricks metadata sync automation."
---

import FeatureAvailability from '@site/src/components/FeatureAvailability';

# Databricks Metadata Sync Automation

<FeatureAvailability saasOnly />

:::info

This feature is currently in Public Beta in Orrery Cloud. Reach out to your Orrery Cloud representative if you face any issues configuring or validating the capabilities outlined below.

:::

## Overview

Databricks Metadata Sync is an automation feature that enables seamless synchronization of Orrery Tags and Descriptions with Databricks Unity Catalog. This automation ensures consistent metadata governance across both platforms, automatically propagating Orrery governance artifacts to Unity Catalog tables, columns, catalogs, and schemas. Typically, this will be used in conjunction with the [Databricks ingestion source](https://docs.orrery.dev/docs/generated/ingestion/sources/databricks), which enables ingesting Tags & descriptions from Databricks into Orrery.

This automation is exclusively available in Orrery Cloud.

## Use Cases

- Maintain consistent metadata across Orrery and Databricks
- Improve data discovery by propagating descriptions back to Databricks
- Unity data governance by managing Tag application directly within Orrery

## Sync Capabilities

The Databricks Metadata Sync automation provides comprehensive metadata synchronization with the following features:

- **Automated Tag Propagation**: Seamlessly sync Orrery Tags to Unity Catalog tables, columns, catalogs, and schemas
- **Description Synchronization**: Automatically propagate Orrery descriptions to Unity Catalog objects as comments
- **Logical Model Descriptions**: Optionally sync the descriptions that tables and columns get from a logical model (see [Logical model descriptions](#logical-model-descriptions))
- **Bidirectional Updates**: Maintain consistency by automatically removing Tags and descriptions from Unity Catalog when they are removed in Orrery
- **Selective Propagation**: Configure specific Tags for propagation, or sync all Tags
- **Historical Backfill**: Initialize Tags and Descriptions for assets on Databricks with current Orrery Tags & Descriptions.

> **A note about legacy Hive Metastore**: Bi-directional sync for _descriptions_ is supported for Hive Metastore Schemas & Tables, but Tag sync is _not_. This is because Databricks does not support applying of Tags to these assets on Hive Metastore.

## Prerequisites

Before enabling Databricks Metadata Sync, ensure the following permissions and configurations are in place:

### Required Unity Catalog Permissions

#### Basic Access Permissions (Required for Both Tags and Descriptions)

- **USE CATALOG**: Access to the Unity Catalog containing target objects
- **USE SCHEMA**: Permission to access schemas within the catalog

#### Permissions for Tag Synchronization

Based on Unity Catalog requirements, to add tags to objects you need:

- **APPLY TAG**: Required on each object where tags will be applied (catalogs, schemas, tables, columns)
- **USE SCHEMA**: Required on the object's parent schema (already covered in basic access)
- **USE CATALOG**: Required on the object's parent catalog (already covered in basic access)

_Note: If using governed tags, you may also need ASSIGN permission on the tag policy._

#### Permissions for Description Synchronization

- **MODIFY**: Required to update comments/descriptions on Unity Catalog objects (tables, columns, catalogs, schemas)
- **SELECT**: Required alongside MODIFY for table and column description operations

### Example Permission Configuration

Configure the necessary permissions for your Orrery automation service principal based on your sync requirements:

#### For Tags Only

```sql
-- Basic access permissions
GRANT USE CATALOG ON CATALOG your_catalog TO `orrery-automation@your-domain.com`;
GRANT USE SCHEMA ON SCHEMA your_catalog.your_schema TO `orrery-automation@your-domain.com`;

-- Tag application permissions
GRANT APPLY TAG ON CATALOG your_catalog TO `orrery-automation@your-domain.com`;
GRANT APPLY TAG ON SCHEMA your_catalog.your_schema TO `orrery-automation@your-domain.com`;
GRANT APPLY TAG ON ALL TABLES IN SCHEMA your_catalog.your_schema TO `orrery-automation@your-domain.com`;
```

#### For Descriptions Only

```sql
-- Basic access permissions
GRANT USE CATALOG ON CATALOG your_catalog TO `orrery-automation@your-domain.com`;
GRANT USE SCHEMA ON SCHEMA your_catalog.your_schema TO `orrery-automation@your-domain.com`;

-- Description modification permissions (both MODIFY and SELECT are required)
GRANT MODIFY ON SCHEMA your_catalog.your_schema TO `orrery-automation@your-domain.com`;
GRANT SELECT ON SCHEMA your_catalog.your_schema TO `orrery-automation@your-domain.com`;
```

#### For Both Tags and Descriptions

```sql
-- Basic access permissions
GRANT USE CATALOG ON CATALOG your_catalog TO `orrery-automation@your-domain.com`;
GRANT USE SCHEMA ON SCHEMA your_catalog.your_schema TO `orrery-automation@your-domain.com`;

-- Tag application permissions
GRANT APPLY TAG ON CATALOG your_catalog TO `orrery-automation@your-domain.com`;
GRANT APPLY TAG ON SCHEMA your_catalog.your_schema TO `orrery-automation@your-domain.com`;
GRANT APPLY TAG ON ALL TABLES IN SCHEMA your_catalog.your_schema TO `orrery-automation@your-domain.com`;

-- Description modification permissions (both MODIFY and SELECT are required)
GRANT MODIFY ON SCHEMA your_catalog.your_schema TO `orrery-automation@your-domain.com`;
GRANT SELECT ON SCHEMA your_catalog.your_schema TO `orrery-automation@your-domain.com`;
```

### Connection Requirements

Ensure your Orrery instance has:

- Valid Databricks workspace credentials
- Network connectivity to your Databricks Unity Catalog environment
- Appropriate service principal or user authentication configured
- Databricks warehouse id for executing operations

## Configuration Guide

### Step 1: Access Automations

Navigate to the Automations section in your Orrery Cloud interface:

1. Select **Automations** from the dropdown menu, Under the **Govern** section

<p align="center">
  <img width="20%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/automation/saas/automations-nav-link.png" alt="Navigate to Automations"/>
</p>

### Step 2: Create Databricks Automation

Configure the automation:

1. Click the **Create** button
2. Select **Databricks Metadata Sync** from the available automation types

<p align="center">
  <img width="60%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/automation/saas/databricks-metadata-sync/automation-selection.png" alt="Select Databricks Metadata Sync"/>
</p>

### Step 3: Configure Sync Options

Choose the types of information to synchronize:

#### Select Action

Choose between:

- **Tags**: Sync Tags for Tables, Columns, Catalogs, & Schemas (Unity Catalog only)
- **Descriptions**: Sync descriptions for Tables, Columns, Catalogs & Schemas as comments (Unity Catalog & legacy Hive Metastore)
  - **Sync descriptions from logical models**: Shown when **Descriptions** is selected. Off by default. See [Logical model descriptions](#logical-model-descriptions).

<p align="center">
  <img width="60%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/automation/saas/databricks-metadata-sync/select-action.png" alt="Select Sync Action"/>
</p>

#### Configure Tag Selection (if Tags selected)

When syncing Tags, you can choose:

- **All Tags**: Propagate all Orrery Tags to Unity Catalog
- **Specific Tags**: Select only specific Tags for synchronization

### Step 4: Configure Connection Settings

Complete the Databricks connection configuration:

#### Required Connection Details

- **Workspace URL**: Your Databricks workspace URL (e.g., `https://abcsales.cloud.databricks.com`)
- **Warehouse ID**: The SQL warehouse ID used for executing for metadata operations (e.g., `fab3e5fg0bcbfc56`)
- **Token**: Databricks personal access token or service principal token

<p align="center">
  <img width="60%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/automation/saas/databricks-metadata-sync/connection-config.png" alt="Connection Configuration"/>
</p>

#### Test Connection

Click **Test Connection** to verify your configuration before proceeding.

> **Note:** If you use a **remote executor** for your automation (can be selected under "Advanced"), the connection test may fail with _"Unauthorized network access to workspace"_. This may be expected, since the test runs directly from Orrery Cloud and not from your remote executor. The actual metadata sync will still work correctly via the remote executor.

### Step 5: Configure Automation Details

Provide automation metadata:

- **Name**: Descriptive name for your automation (e.g., "Databricks Metadata Sync")
- **Description**: Details about the automation's purpose and scope
- **Category**: Select an appropriate category for organization

Click **Save and Run** to activate the automation and begin real-time synchronization.

## Historical Data Synchronization

### Initializing Existing Assets

For environments with existing Orrery metadata, you can perform a one-time backfill to ensure all current Tags and Descriptions from Orrery are propagated to Unity Catalog. Depending on the number of assets, this might take a while!

#### Initialization Process

1. Navigate to your created Databricks Metadata Sync automation
2. Click the three-dot **More** menu next to the automation

<p align="left">
  <img width="20%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/automation/saas/automation-more-menu.png" alt="Automation More Menu"/>
</p>

3. Select **Initialize** from the dropdown menu

<p align="left">
  <img width="20%" src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/automation/saas/automation-initialize.png" alt="Initialize Automation"/>
</p>

:::note Initialization Timeline

The initialization process duration depends on the volume of Unity Catalog assets in your environment. Large catalogs with extensive metadata may require significant processing time.

:::

## Validating the Integration

### Viewing Synced Metadata

Confirm successful metadata syncing by examining Unity Catalog objects:

1. **Access Databricks UI**: Navigate to your Databricks workspace
2. **Browse Catalog**: Open the Unity Catalog explorer
3. **Inspect Objects**: Select tables or columns to view applied tags and comments

## Logical model descriptions

When you sync Descriptions, you can also select **Sync descriptions from logical models** (off by default, since it replaces existing comments). Descriptions that a table or column gets from its logical model are then synced as comments, unless it has a description edited in Orrery, which always wins. Descriptions that arrive through lineage are not synced. Initialize writes the logical-model description of every linked table and column.

:::note Limitations

- Removing a logical-model description does not clear the comment in Databricks. Clear it there too, or the next ingestion imports it back.
- Clearing a description edited in Orrery leaves the Databricks comment unchanged instead of restoring the logical-model description. Run Initialize to write it again.

:::

## Troubleshooting

### Common Issues and Solutions

#### Permission Errors

- Verify service principal has all required Unity Catalog permissions
- Confirm catalog and schema access rights
- Check tag creation and application privileges

#### Connection Issues

- Validate Databricks workspace URL format
- Ensure access token is valid and not expired
- Verify warehouse ID is correct and accessible
- Check network connectivity between Orrery and Databricks

#### Synchronization Failures

- Check Unity Catalog object permissions
- Verify target objects exist and are accessible
- Ensure warehouse is running and available

### Support Resources

For additional assistance with Databricks Metadata Sync, contact your Orrery Cloud representative.

## FAQ

1. **Where should I manage Tags & Descriptions?**

In general, we recommend centrally authoring Tags and Descriptions within Orrery. This allows you to maintain a clear and consistent governance posture across _all_ of your data sources and data products - there is always data outside of Databricks! Authoring this critical information in Orrery also improves the experience for your data practitioners trying to find the right data.

This automation is intended to enable this style of management, allowing you to "push down" metadata from the central catalog into Databricks, where your data is stored and queried.

2. **How does Orrery represent key-value tags from Databricks?**

During ingestion from [Databricks](https://docs.orrery.dev/docs/generated/ingestion/sources/databricks), Orrery can ingest tags and descriptions that were originally authored within Databricks. Orrery converts key-value formatted tags in Databricks into Orrery tags of the format: `key:value`. For example, if you have a tag with key `has_pii` and value `true` in Databricks, this will be ingested as a single combined tag named `has_pii: true` in Orrery.

After ingestion into Orrery, you can apply this tag to tables or columns and sync it back to Databricks using this automation. Any tag with the format `key:value` that is applied on Orrery will be synced back to Databricks in proper key, value form.

If you apply a tag without a separator colon in Orrery (e.g. `has_pii`), it will be synced back to Databricks with the key being `has_pii` and value being empty.

3. **I updated a table description in _Databricks_, but I don't see it reflecting after ingestion into Orrery. Why not?**

This is usually because you've already overridden the description inside Orrery for this table. Orrery assumes that _it_ will be the source of truth for documentation, which means that any edits that have taken place in the Orrery UI (or via API) will take precedent over changes provided in Databricks. When you change the description in Orrery, the description change will overwrite the latest description in Databricks if this automation is enabled.

But fear not - you can always view the original underlying Databricks description underneath the Orrery description in the Orrery UI, even when it changes.

4. **Can I sync Orrery Structured Properties or Glossary Terms back to Databricks as Tags?**

Currently, no. Sync back is limited to Tags, to keep the concepts aligned more simply across both platforms. Reach out if you'd benefit from this capability!
