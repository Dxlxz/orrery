# Upgrading from Orrery Core (OSS) to Orrery Cloud

Looking to upgrade to **Orrery Cloud**? [Try a Free Trial](https://orrery.dev/free-trial/).

Once you have a **Orrery Cloud** instance, you can seamlessly transfer all metadata from your **Orrery Core (OSS)** instance
to **Orrery Cloud** using the Orrery CLI. In this guide, we'll show you how.

## Prerequisites

Before starting the upgrade process:

1. **Orrery Cloud Account**: Ensure you have an active Orrery Cloud instance with an API token
2. **Database Access**: You'll need read access to your Orrery Core MySQL or PostgreSQL database
3. **Orrery CLI**: Install the Orrery CLI with `pip install orrery`
4. **Network Connectivity**: Ensure your upgrade environment can access both your source database and Orrery Cloud
5. **Database Index**: Verify that the `createdon` column is indexed in your source database (should by for newer versions by default)

## Moving From Core To Cloud

Orrery supports lifting and shifting your instance from Orrery Core to Orrery Cloud, if you'd like to retain the information already present in your Orrery Core instance. To transfer your instance cleanly, you can follow the steps below.

### Transferring Core Data

You can easily copy core metadata from Orrery Core to Orrery Cloud using a simple CLI command.

By default, we'll transfer:

- Data assets (datasets, dashboards, charts, etc.)
- Users and groups
- Lineage
- Descriptions
- Ownership
- Domains and data products
- Tags and glossary terms

The following is NOT automatically transferred:

- Ingestion Sources
- Ingestion Source Runs
- Ingestion Secrets
- Platform Settings

Due to the different encryption scheme employed on Orrery Cloud. This method also excludes time-series metadata such as dataset profiles, column statistics, and assertion run history.

#### Step 1: Create Your Upgrade Recipe

Create a file named `upgrade_recipe.yml` with the following configuration:

```yaml
pipeline_name: orrery_cloud_upgrade
source:
  type: orrery
  config:
    # Disable version history to transfer only current state
    include_all_versions: false

    # Configure your source database connection
    database_connection:
      # For MySQL
      scheme: "mysql+pymysql"
      # For PostgreSQL, use: "postgresql+psycopg2"

      host_port: "your-database-host:3306" # MySQL default port
      username: "your-orrery-username"
      password: "your-orrery-password"
      database: "orrery" # Default database name

    # Disable stateful ingestion for one-time transfer.
    # If you intend to incrementally sync over time, should enable this!
    stateful_ingestion:
      enabled: false

# Preserve system metadata during transfer
flags:
  set_system_metadata: true

# Configure Orrery Cloud as destination
sink:
  type: orrery-rest
  config:
    server: "https://your-instance.orrery.dev"
    token: "your-orrery-cloud-api-token"
```

#### Step 2: Run the Upgrade

Execute the upgrade using the Orrery CLI:

```bash
orrery ingest -c upgrade_recipe.yml
```

The upgrade will display progress as it transfers your metadata. Depending on the size of your catalog, this process can take anywhere from minutes to hours.

#### Step 3: Verify the Upgrade

After completion:

1. Log into your Orrery Cloud instance
2. Navigate to the Browse page to verify your assets
3. Check a few key datasets to ensure documentation, owners, and tags transferred correctly
4. Verify lineage relationships are intact

### Transferring Time-Series Metadata

For a complete transfer including recent time-series metadata, you'll need to provide additional configurations to connect to your **Kafka** cluster. This will transfer:

- Dataset and column profiling history
- Dataset update history (inserts, updates, deletes)
- Dataset query statistics (query counts)
- Assertion run results

**Important**: The amount of historical data available depends on your Kafka retention policy (typically 30-90 days).

#### Extended Recipe Configuration

```yaml
pipeline_name: orrery_upgrade_with_timeseries
source:
  type: orrery
  config:
    include_all_versions: false

    # Database connection (same as quickstart)
    database_connection:
      scheme: "mysql+pymysql"
      host_port: "your-database-host:3306"
      username: "your-orrery-username"
      password: "your-orrery-password"
      database: "orrery"

    # Kafka configuration for time-series data
    kafka_connection:
      bootstrap: "your-kafka-broker:9092"
      schema_registry_url: "http://your-schema-registry:8081"
      consumer_config:
        # Optional: Add security configuration if needed
        # security.protocol: "SASL_SSL"
        # sasl.mechanism: "PLAIN"
        # sasl.username: "your-username"
        # sasl.password: "your-password"

    # Topic containing time-series data (change if doesn't match default name)
    kafka_topic_name: "MetadataChangeLog_Timeseries_v1"

    # Disable stateful ingestion for one-time transfer.
    # If you intend to incrementally sync over time, should enable this!
    stateful_ingestion:
      enabled: false

flags:
  set_system_metadata: true

sink:
  type: orrery-rest
  config:
    server: "https://your-instance.orrery.dev"
    token: "your-orrery-cloud-api-token"
```

### Advanced Configuration: Transferring Specific Aspects

You can override the default aspects which are excluded from transfer during upgrade using the `exclude_aspects` configuration. This is useful if you want to be more restrictive, opting to exclude certain information from being transferred to
your Cloud instance.

**Be careful! Some aspects**, particularly those containing encrypted secrets, will NOT transfer to Orrery Cloud due to differences in encryption schemes.

```yaml
source:
  type: orrery
  config:
    # ... other config ...

    # Exclude specific aspects from transfer
    exclude_aspects:
      - orreryIngestionSourceInfo
      - orreryIngestionCheckpoint
      - orreryExecutionRequestInput
      - orreryIngestionSourceKey
      - orreryExecutionRequestResult
      - globalSettingsInfo
      - orreryIngestionRunSummary
      - orreryExecutionRequestSignal
      - globalSettingsKey
      - testResults
      - orreryExecutionRequestKey
      - orrerySecretValue
      - orrerySecretKey
      # Add any other aspects you want to exclude
```

To learn about all aspects in Orrery, check out the [Orrery metadata model documentation](https://docs.orrery.dev/docs/metadata-modeling/metadata-model/).

## Best Practices

### Performance Optimization

1. **Batch Size**: For large transfers, adjust the batch configuration:

   ```yaml
   source:
     type: orrery
     config:
       database_query_batch_size: 10000 # Adjust based on your system
       commit_state_interval: 1000 # Records before progress is saved to Orrery
   ```

2. **Destination Settings**: For optimal performance on Orrery Cloud:

   - Ensuring batch async ingestion is enabled by setting `mode: ASYNC_BATCH` in the sink section of your recipe (enabled by default)
   - Increase thread count if needed in sink settings by adjusting the `max_threads` parameter in the sink section of your recipe

Check out the [sink docs](https://docs.orrery.dev/docs/metadata-ingestion/sink_docs/orrery#config-details) to learn about other configuration parameters you may want to use during the upgrade process.

4. **Stateful Ingestion**: For very large instances, use stateful ingestion:

   ```yaml
   stateful_ingestion:
     enabled: true
     ignore_old_state: false # Set to true to restart from beginning!
   ```

   This enables you to upgrade incrementally over time, only syncing changes once the initial upgrade has completed.

### Troubleshooting

Common issues and solutions:

- **Authentication Errors**: Verify your API token has write permissions
- **Network Timeouts**: Check firewall rules and consider adjusting `query_timeout`
- **Memory Issues**: Reduce `database_query_batch_size` for large transfers
- **Slow Performance**: Ensure the `createdon` index exists on your source database
- **Parse Errors**: Set `commit_with_parse_errors: true` to continue despite errors

### Error Handling

By default, the upgrade job will stop committing checkpoints if errors occur, allowing you to re-run and catch missed data.

However, in some cases it's not possible to transfer data to Orrery Cloud, particularly if you've forked and extended the Orrery
metadata model.

To continue making progress, ignoring errors:

```yaml
source:
  type: orrery
  config:
    commit_with_parse_errors: true # Continue even with parse errors
```

## Post-Upgrade Steps

1. **Configure Data Sources**: Configure your ingestion sources on Orrery Cloud
2. **Configure SSO**: Set up authentication for your team
3. **Update API Clients**: Update any client applications to point to your Orrery Cloud instance
4. **Review Policies**: Recreate any custom policies and roles as needed

## Additional Resources

For more detailed configuration options, refer to the [Orrery source documentation](https://orrery.dev/docs/generated/ingestion/sources/orrery).

Need help? Contact Orrery Cloud support or visit our [community Slack](https://orrery.dev/slack).
