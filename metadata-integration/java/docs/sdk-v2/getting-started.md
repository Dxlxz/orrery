# Getting Started with Java SDK V2

This guide walks you through setting up and using the Orrery Java SDK V2 to interact with Orrery's metadata platform.

## Prerequisites

- Java 8 or higher
- Access to a Orrery instance (Cloud or self-hosted)
- (Optional) A Orrery personal access token for authentication

## Installation

Add the Orrery client library to your project's build configuration.

### Gradle

Add to your `build.gradle`:

```gradle
dependencies {
    implementation 'io.orrery:orrery-client:__version__'
}
```

### Maven

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>io.orrery</groupId>
    <artifactId>orrery-client</artifactId>
    <version>__version__</version>
</dependency>
```

> **Tip:** Find the latest version on [Maven Central](https://mvnrepository.com/artifact/io.orrery/client).

## Creating a Client

The `OrreryClientV2` is your entry point to all SDK operations. Create one by specifying your Orrery server URL:

```java
import orrery.client.v2.OrreryClientV2;

OrreryClientV2 client = OrreryClientV2.builder()
    .server("http://localhost:8080")
    .build();
```

### With Authentication

For Orrery Cloud or secured instances, provide a personal access token:

```java
OrreryClientV2 client = OrreryClientV2.builder()
    .server("https://your-instance.orrery.dev")
    .token("your-personal-access-token")
    .build();
```

> **How to get a token:** In Orrery UI, go to Settings → Access Tokens → Generate Personal Access Token

### Testing the Connection

Verify your client can reach the Orrery server:

```java
try {
    boolean connected = client.testConnection();
    if (connected) {
        System.out.println("Successfully connected to Orrery!");
    } else {
        System.out.println("Failed to connect to Orrery");
    }
} catch (Exception e) {
    System.err.println("Connection error: " + e.getMessage());
}
```

## Creating Your First Entity

Let's create a dataset with some metadata.

### Step 1: Import Required Classes

```java
import orrery.client.v2.OrreryClientV2;
import orrery.client.v2.entity.Dataset;
import com.linkedin.common.OwnershipType;
```

### Step 2: Build a Dataset

Use the fluent builder to construct a dataset:

```java
Dataset dataset = Dataset.builder()
    .platform("snowflake")
    .name("analytics.public.user_events")
    .env("PROD")
    .description("User interaction events from web and mobile")
    .displayName("User Events")
    .build();
```

**Breaking down the builder:**

- `platform` - Data platform identifier (e.g., "snowflake", "bigquery", "postgres")
- `name` - Fully qualified dataset name (database.schema.table or similar)
- `env` - Environment (PROD, DEV, STAGING, etc.)
- `description` - Human-readable description of the dataset
- `displayName` - Friendly name shown in Orrery UI

### Step 3: Add Metadata

Enrich the dataset with tags, owners, and custom properties:

```java
dataset.addTag("pii")
       .addTag("analytics")
       .addOwner("urn:li:corpuser:john_doe", OwnershipType.TECHNICAL_OWNER)
       .addCustomProperty("retention_days", "90")
       .addCustomProperty("team", "data-engineering");
```

### Step 4: Upsert to Orrery

Send the dataset to Orrery:

```java
try {
    client.entities().upsert(dataset);
    System.out.println("Successfully created dataset: " + dataset.getUrn());
} catch (IOException | ExecutionException | InterruptedException e) {
    System.err.println("Failed to create dataset: " + e.getMessage());
}
```

## Complete Example

Here's a complete, runnable example:

```java
import orrery.client.v2.OrreryClientV2;
import orrery.client.v2.entity.Dataset;
import com.linkedin.common.OwnershipType;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class OrreryQuickStart {
    public static void main(String[] args) {
        // Create client
        OrreryClientV2 client = OrreryClientV2.builder()
            .server("http://localhost:8080")
            .token("your-token-here")  // Optional
            .build();

        try {
            // Test connection
            if (!client.testConnection()) {
                System.err.println("Cannot connect to Orrery");
                return;
            }

            // Build dataset
            Dataset dataset = Dataset.builder()
                .platform("snowflake")
                .name("analytics.public.user_events")
                .env("PROD")
                .description("User interaction events")
                .displayName("User Events")
                .build();

            // Add metadata
            dataset.addTag("pii")
                   .addTag("analytics")
                   .addOwner("urn:li:corpuser:datateam", OwnershipType.TECHNICAL_OWNER)
                   .addCustomProperty("retention_days", "90");

            // Upsert to Orrery
            client.entities().upsert(dataset);
            System.out.println("Created dataset: " + dataset.getUrn());

        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

For more complete examples, see the [Dataset Entity Guide](./dataset-entity.md#examples).

## Reading Entities

Load an existing entity from Orrery:

```java
import com.linkedin.common.urn.DatasetUrn;

DatasetUrn urn = new DatasetUrn(
    "snowflake",
    "analytics.public.user_events",
    "PROD"
);

try {
    Dataset loaded = client.entities().get(urn);
    if (loaded != null) {
        System.out.println("Dataset description: " + loaded.getDescription());
        System.out.println("Is read-only: " + loaded.isReadOnly());  // true
    }
} catch (IOException | ExecutionException | InterruptedException e) {
    e.printStackTrace();
}
```

> **Important:** Entities fetched from the server are **read-only by default**. Additional aspects are lazy-loaded on demand.

### Understanding Read-Only Entities

When you fetch an entity from Orrery, it's immutable to prevent accidental modifications:

```java
Dataset dataset = client.entities().get(urn);

// Reading works fine
String description = dataset.getDescription();
List<String> tags = dataset.getTags();

// But mutation throws ReadOnlyEntityException
// dataset.addTag("pii");  // ERROR: Cannot mutate read-only entity!
```

**Why?** Immutability-by-default makes mutation intent explicit, prevents accidental changes when passing entities between functions, and enables safe entity sharing.

## Updating Entities with Patches

To modify a fetched entity, create a mutable copy first:

```java
// 1. Load existing dataset (read-only)
Dataset dataset = client.entities().get(urn);

// 2. Get mutable copy
Dataset mutable = dataset.mutable();

// 3. Add new tags and owners (patch operations)
mutable.addTag("gdpr")
       .addOwner("urn:li:corpuser:new_owner", OwnershipType.TECHNICAL_OWNER);

// 4. Apply patches to Orrery
client.entities().update(mutable);
```

The `update()` method sends only the changes (patches) to Orrery, not the full entity. This is more efficient and safer for concurrent updates.

### Entity Lifecycle

Understanding when entities are mutable vs read-only:

**Builder-created entities** - Mutable from creation:

```java
Dataset dataset = Dataset.builder()
    .platform("snowflake")
    .name("my_table")
    .build();

dataset.isMutable();  // true - can mutate immediately
dataset.addTag("test");  // Works without .mutable()
```

**Server-fetched entities** - Read-only by default:

```java
Dataset dataset = client.entities().get(urn);

dataset.isReadOnly();  // true
// dataset.addTag("test");  // ERROR!

Dataset mutable = dataset.mutable();  // Get writable copy
mutable.addTag("test");  // Now works
```

See the [Patch Operations Guide](./patch-operations.md) for details.

## Upserting vs Updating

SDK V2 provides two methods for persisting entities:

### `upsert(entity)`

- **Use for:** New entities or full replacements
- **Sends:** All aspects from the entity
- **Behavior:** Creates if doesn't exist, replaces if exists

```java
client.entities().upsert(dataset);
```

### `update(entity)`

- **Use for:** Incremental changes to existing entities
- **Sends:** Only pending patches accumulated since the entity was loaded or created
- **Behavior:** Applies surgical updates to specific fields

```java
client.entities().update(dataset);
```

## Working with Other Entities

SDK V2 supports multiple entity types beyond datasets:

### Charts

```java
import orrery.client.v2.entity.Chart;

Chart chart = Chart.builder()
    .tool("looker")
    .id("my_sales_chart")
    .title("Sales Performance by Region")
    .description("Monthly sales broken down by geographic region")
    .build();

client.entities().upsert(chart);
```

See the [Chart Entity Guide](./chart-entity.md) for details.

### Dashboards

Coming soon! Dashboard entity support is planned for a future release.

## Configuration Options

Customize the client for your environment:

```java
OrreryClientV2 client = OrreryClientV2.builder()
    .server("https://your-instance.orrery.dev")
    .token("your-access-token")

    // Configure operation mode
    .operationMode(OrreryClientConfigV2.OperationMode.SDK)  // or INGESTION

    // Customize underlying REST emitter
    .restEmitterConfig(config -> config
        .timeoutSec(30)
        .maxRetries(5)
        .retryIntervalSec(2)
    )

    .build();
```

### Operation Modes

SDK V2 supports two operation modes:

- **SDK Mode** (default): For interactive applications, provides patch-based updates and lazy loading
- **INGESTION Mode**: For ETL pipelines, optimizes for high-throughput batch operations

```java
// SDK mode (default) - interactive use
OrreryClientV2 sdkClient = OrreryClientV2.builder()
    .server("http://localhost:8080")
    .operationMode(OrreryClientConfigV2.OperationMode.SDK)
    .build();

// Ingestion mode - ETL pipelines
OrreryClientV2 ingestionClient = OrreryClientV2.builder()
    .server("http://localhost:8080")
    .operationMode(OrreryClientConfigV2.OperationMode.INGESTION)
    .build();
```

See [OrreryClientV2 Configuration](./client.md) for all available options.

## Error Handling

Handle errors gracefully:

```java
try {
    client.entities().upsert(dataset);
} catch (IOException e) {
    // Network or serialization errors
    System.err.println("I/O error: " + e.getMessage());
} catch (ExecutionException e) {
    // Server-side errors
    System.err.println("Server error: " + e.getCause().getMessage());
} catch (InterruptedException e) {
    // Operation cancelled
    Thread.currentThread().interrupt();
}
```

## Resource Management

Always close the client when done to release resources:

```java
try (OrreryClientV2 client = OrreryClientV2.builder()
        .server("http://localhost:8080")
        .build()) {

    // Use client here
    client.entities().upsert(dataset);

} // Client automatically closed
```

Or close explicitly:

```java
try {
    // Use client
} finally {
    client.close();
}
```

## Next Steps

Now that you've created your first entity, explore more advanced topics:

- **[Design Principles](./design-principles.md)** - Understand the architecture behind SDK V2
- **[Dataset Entity Guide](./dataset-entity.md)** - Comprehensive dataset operations
- **[Chart Entity Guide](./chart-entity.md)** - Working with chart entities
- **[Patch Operations](./patch-operations.md)** - Deep dive into incremental updates
- **[Client Configuration](./client.md)** - Advanced client setup and options

Or check out complete examples in the entity guides:

- [Dataset Examples](./dataset-entity.md#examples)
- [Chart Examples](./chart-entity.md#examples)
- [Dashboard Examples](./dashboard-entity.md#examples)
- [DataJob Examples](./datajob-entity.md#examples)
