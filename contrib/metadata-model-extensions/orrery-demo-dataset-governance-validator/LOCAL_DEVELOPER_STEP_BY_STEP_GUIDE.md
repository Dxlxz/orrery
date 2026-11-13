# Local Developer Step-by-Step Guide

## Step 1: Create the Validator Project

First, create a new Gradle project structure:

```bash
mkdir orrery-dataset-governance-validator
cd orrery-dataset-governance-validator
```

## Step 2: Set Up Build Configuration

Create `build.gradle` with the following content:

```gradle
plugins {
    id 'java-library'
    id 'maven-publish'
}

group = 'com.linkedin.orrery'
version = '0.1.0'

repositories {
    mavenCentral()
}

dependencies {
    // Orrery dependencies (using file dependencies that work from contrib/)
    // Check if we're building from within Orrery repo
    def orreryRoot = file('../../../')
    def entityRegistryJar = file("${orreryRoot}/entity-registry/build/libs/entity-registry.jar")
    def liUtilsJar = file("${orreryRoot}/li-utils/build/libs/li-utils.jar")
    def metadataModelsJar = file("${orreryRoot}/metadata-models/build/libs/metadata-models.jar")

    if (entityRegistryJar.exists()) {
        implementation files(entityRegistryJar)
        implementation files("${orreryRoot}/li-utils/build/libs/li-utils-data-template.jar")
        implementation files("${orreryRoot}/metadata-models/build/libs/metadata-models-data-template.jar")
        implementation files("${orreryRoot}/entity-registry/build/libs/entity-registry-data-template.jar")
        implementation files(liUtilsJar)
        implementation files(metadataModelsJar)
    } else {
        // Fallback for standalone usage
        println "Warning: Orrery JARs not found. Build Orrery first or provide JAR files."
    }

    // Core dependencies needed for compilation
    implementation 'com.linkedin.pegasus:data:29.65.7'
    implementation 'org.slf4j:slf4j-api:1.7.36'
    implementation 'com.google.guava:guava:31.1-jre'
    implementation 'javax.annotation:javax.annotation-api:1.3.2'

    // Testing
    testImplementation 'org.junit.jupiter:junit-jupiter:5.9.2'
    testImplementation 'org.mockito:mockito-core:4.11.0'
    testImplementation 'org.mockito:mockito-junit-jupiter:4.11.0'
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

test {
    useJUnitPlatform()
}

// Build plugin JAR
jar {
    archiveBaseName = 'orrery-demo-dataset-governance-validator'
    manifest {
        attributes(
            'Implementation-Title': 'Orrery Demo Dataset Governance Validator',
            'Implementation-Version': project.version
        )
    }
}

// Create model plugin directory structure and deploy
task createModelPlugin(type: Copy, dependsOn: jar) {
    def pluginDir = file("build/model-plugin/dataset-governance-validator/1.0.0")
    pluginDir.mkdirs()

    from jar.archiveFile
    into pluginDir

    doLast {
        // Copy the entity registry to the plugin directory
        copy {
            from 'src/main/resources/entity-registry.yml'
            into pluginDir
        }
        println "Created model plugin in build/model-plugin/"
    }
}

// Deploy to local Orrery plugins directory
task deployPlugin(type: Copy, dependsOn: createModelPlugin) {
    def pluginBaseDir = "${System.getProperty('user.home')}/.orrery/plugins/models"
    def localPluginDir = file("${pluginBaseDir}/dataset-governance-validator/1.0.0")

    from file("build/model-plugin/dataset-governance-validator/1.0.0")
    into localPluginDir

    doLast {
        println "Deployed model plugin to ${localPluginDir}"
        println "Plugin structure:"
        fileTree(localPluginDir).visit { element ->
            if (!element.isDirectory()) {
                println "  ${element.relativePath}"
            }
        }
    }
}
```

## Step 3: Implement the Validator

Create the directory structure:

```bash
mkdir -p src/main/java/com/linkedin/metadata/aspect/plugins/validation
```

Create `src/main/java/com/linkedin/metadata/aspect/plugins/validation/DatasetGovernanceValidator.java`:

```java
package com.linkedin.metadata.aspect.plugins.validation;

import com.linkedin.common.urn.Urn;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.aspect.AspectRetriever;
import com.linkedin.metadata.aspect.RetrieverContext;
import com.linkedin.metadata.aspect.SystemAspect;
import com.linkedin.metadata.aspect.batch.BatchItem;
import com.linkedin.metadata.aspect.batch.ChangeMCP;
import com.linkedin.metadata.aspect.plugins.config.AspectPluginConfig;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom validator that ensures dataset URNs have ownership, tags, and domain set
 * either in the current batch of MCPs or already existing in GMS.
 */
public class DatasetGovernanceValidator extends AspectPayloadValidator {

  private static final Logger logger = LoggerFactory.getLogger(DatasetGovernanceValidator.class);

  private static final String DATASET_ENTITY_TYPE = "dataset";
  private static final Set<String> REQUIRED_ASPECTS = Set.of(
      Constants.OWNERSHIP_ASPECT_NAME,      // "ownership"
      Constants.GLOBAL_TAGS_ASPECT_NAME,    // "globalTags"
      Constants.DOMAINS_ASPECT_NAME         // "domains"
  );

  private AspectPluginConfig config;

  @Override
  protected Stream<AspectValidationException> validateProposedAspects(
      @Nonnull Collection<? extends BatchItem> mcpItems,
      @Nonnull RetrieverContext retrieverContext) {

    logger.debug("Validating {} MCP items", mcpItems.size());

    AspectRetriever aspectRetriever = retrieverContext.getAspectRetriever();

    // Group all dataset URNs in the batch
    Set<Urn> datasetUrns = mcpItems.stream()
        .map(BatchItem::getUrn)
        .filter(urn -> DATASET_ENTITY_TYPE.equals(urn.getEntityType()))
        .collect(Collectors.toSet());

    if (datasetUrns.isEmpty()) {
      return Stream.empty();
    }

    // Create map for batch fetching existing aspects
    Map<Urn, Set<String>> urnAspects = datasetUrns.stream()
        .collect(Collectors.toMap(
            urn -> urn,
            urn -> REQUIRED_ASPECTS
        ));

    // Batch fetch existing aspects from GMS
    Map<Urn, Map<String, SystemAspect>> existingAspects =
        aspectRetriever.getLatestSystemAspects(urnAspects);

    // Track which aspects are being set in this batch
    Map<Urn, Set<String>> batchAspects = mcpItems.stream()
        .filter(item -> DATASET_ENTITY_TYPE.equals(item.getUrn().getEntityType()))
        .filter(item -> REQUIRED_ASPECTS.contains(item.getAspectName()))
        .collect(Collectors.groupingBy(
            BatchItem::getUrn,
            Collectors.mapping(BatchItem::getAspectName, Collectors.toSet())
        ));

    // Validate each dataset URN
    return datasetUrns.stream()
        .map(urn -> validateDatasetGovernance(urn, existingAspects.get(urn),
                                              batchAspects.get(urn), mcpItems))
        .filter(Objects::nonNull);
  }

  @Override
  protected Stream<AspectValidationException> validatePreCommitAspects(
      @Nonnull Collection<ChangeMCP> changeMCPs,
      @Nonnull RetrieverContext retrieverContext) {

    // Use same validation logic for pre-commit
    return validateProposedAspects(changeMCPs, retrieverContext);
  }

  private AspectValidationException validateDatasetGovernance(
      Urn datasetUrn,
      Map<String, SystemAspect> existingAspects,
      Set<String> batchAspects,
      Collection<? extends BatchItem> mcpItems) {

    Set<String> missingAspects = REQUIRED_ASPECTS.stream()
        .filter(aspectName -> {
          boolean existsInGMS = existingAspects != null &&
                               existingAspects.containsKey(aspectName);
          boolean existsInBatch = batchAspects != null &&
                                 batchAspects.contains(aspectName);
          return !existsInGMS && !existsInBatch;
        })
        .collect(Collectors.toSet());

    if (!missingAspects.isEmpty()) {
      String message = String.format(
          "Dataset %s is missing required governance aspects: %s. " +
          "All datasets must have ownership, tags, and domain set.",
          datasetUrn,
          String.join(", ", missingAspects)
      );

      logger.warn("Validation failed: {}", message);

      // Find a BatchItem for this dataset to include in the exception
      BatchItem relevantItem = mcpItems.stream()
          .filter(item -> datasetUrn.equals(item.getUrn()))
          .findFirst()
          .orElse(null);

      return AspectValidationException.forItem(relevantItem, message);
    }

    return null;
  }

  @Nonnull
  @Override
  public AspectPluginConfig getConfig() {
    return config;
  }

  @Override
  public DatasetGovernanceValidator setConfig(@Nonnull AspectPluginConfig config) {
    this.config = config;
    return this;
  }
}
```

## Step 4: Configure the Model Plugin

Create the directory:

```bash
mkdir -p src/main/resources
```

Create `src/main/resources/entity-registry.yml`:

```yaml
id: "dataset-governance-validator"
entities:
  - name: dataset
    category: core
    aspects:
      - name: schemaMetadata
    plugins:
      aspectPayloadValidators:
        - className: "com.linkedin.metadata.aspect.plugins.validation.DatasetGovernanceValidator"
          supportedOperations: ["CREATE", "UPSERT"]
          supportedEntityAspectNames:
            - entityName: "dataset"
              aspectName: "*"
```

## Step 5: Build and Deploy the Validator

```bash
# Build the validator JAR
./gradlew build

# Deploy as a Orrery model plugin
./gradlew deployPlugin
```

This creates the plugin structure at:

```
~/.orrery/plugins/models/
└── dataset-governance-validator/
    └── 1.0.0/
        ├── orrery-demo-dataset-governance-validator-0.1.0.jar
        └── entity-registry.yml
```

## Step 6: Load the Plugin in Orrery

For local development with `quickstartDebug`, Orrery automatically discovers plugins from `~/.orrery/plugins/models`.

For production deployments, mount the plugin directory into your Orrery GMS container:

```yaml
# docker-compose.override.yml
services:
  orrery-gms:
    volumes:
      - ~/.orrery/plugins:/etc/orrery/plugins/models
```

## Step 7: Restart Orrery GMS

```bash
# For local development
docker restart orrery-orrery-gms-debug-1

# Monitor logs to confirm plugin loaded
docker logs -f orrery-orrery-gms-debug-1 | grep -i "validator"
```

You should see:

```
Enabled 1 plugins. [com.linkedin.metadata.aspect.plugins.validation.DatasetGovernanceValidator]
```

## Testing the Validator

### Method 1: Using Orrery Python Client

Create `test_batch_validator.py`:

```python
#!/usr/bin/env python3

from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.metadata.schema_classes import (
    DatasetPropertiesClass,
    OwnershipClass,
    OwnerClass,
    OwnershipTypeClass,
    GlobalTagsClass,
    TagAssociationClass,
    DomainsClass
)
from orrery.metadata.com.linkedin.pegasus2avro.common import AuditStamp

# Configure emitter
emitter = OrreryRestEmitter(gms_server="http://localhost:8080")

# Test 1: Invalid dataset (missing governance) - should fail
print("Testing invalid dataset...")
dataset_urn = "urn:li:dataset:(urn:li:dataPlatform:mysql,db.test_dataset,PROD)"

try:
    mcp = MetadataChangeProposalWrapper(
        entityUrn=dataset_urn,
        aspect=DatasetPropertiesClass(
            name="test_dataset",
            description="Dataset without governance - should be blocked"
        )
    )
    emitter.emit_mcps([mcp])
    print("❌ ERROR: Expected validation failure!")
except Exception as e:
    print(f"✅ SUCCESS: Validator blocked dataset: {str(e)}")

# Test 2: Valid dataset (has all governance) - should succeed
print("\nTesting valid dataset...")
audit_stamp = AuditStamp(time=1640995200000, actor="urn:li:corpuser:orrery")

mcps = [
    MetadataChangeProposalWrapper(
        entityUrn=dataset_urn,
        aspect=DatasetPropertiesClass(name="test_dataset")
    ),
    MetadataChangeProposalWrapper(
        entityUrn=dataset_urn,
        aspect=OwnershipClass(
            owners=[OwnerClass(
                owner="urn:li:corpuser:orrery",
                type=OwnershipTypeClass.DATAOWNER
            )],
            lastModified=audit_stamp
        )
    ),
    MetadataChangeProposalWrapper(
        entityUrn=dataset_urn,
        aspect=GlobalTagsClass(
            tags=[TagAssociationClass(tag="urn:li:tag:Production")]
        )
    ),
    MetadataChangeProposalWrapper(
        entityUrn=dataset_urn,
        aspect=DomainsClass(domains=["urn:li:domain:engineering"])
    )
]

try:
    emitter.emit_mcps(mcps)
    print("✅ SUCCESS: Valid dataset accepted!")
except Exception as e:
    print(f"❌ ERROR: {str(e)}")
```

Run the test:

```bash
# Using Orrery's metadata-ingestion venv
cd /path/to/orrery/metadata-ingestion
source venv/bin/activate
python test_batch_validator.py
```

### Method 2: Using Python SDK

Test with Python SDK (individual proposal - should fail):

```python
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import DatasetPropertiesClass

# Configure emitter
emitter = OrreryRestEmitter(gms_server="http://localhost:8080", token="your-token")

# Test dataset without governance metadata - should fail
dataset_urn = "urn:li:dataset:(urn:li:dataPlatform:mysql,db.test_api,PROD)"

mcp = MetadataChangeProposalWrapper(
    entityUrn=dataset_urn,
    aspect=DatasetPropertiesClass(
        name="test_api",
        description="Test dataset"
    )
)

try:
    emitter.emit(mcp)
    print("❌ UNEXPECTED: Should have failed validation")
except Exception as e:
    print(f"✅ EXPECTED: Validation failed - {e}")
```

### Expected Results

1. **Invalid Dataset Test**: Returns HTTP 422 with message:

   ```
   Dataset urn:li:dataset:(urn:li:dataPlatform:mysql,db.test_dataset,PROD) is missing
   required governance aspects: ownership, domains, globalTags. All datasets must have
   ownership, tags, and domain set.
   ```

2. **Valid Dataset Test**: Successfully ingests the dataset with all governance metadata.

## How It Works

### Validation Flow

1. **Batch Reception**: The validator receives all MCPs in a batch through `validateProposedAspects`
2. **Dataset Filtering**: Filters MCPs to only process dataset entity types
3. **Dual Checking**: For each dataset URN, checks if required aspects exist in:
   - **GMS**: Already stored aspects (via `AspectRetriever.getLatestSystemAspects`)
   - **Current Batch**: Aspects being set in this batch of changes
4. **Validation Decision**: If any required aspect is missing from both sources, validation fails
5. **Error Reporting**: Returns specific error messages indicating which aspects are missing

### Key Design Decisions

- **Batch-Level Validation**: Validates entire batches to handle related changes together
- **Efficient Fetching**: Uses batch fetching from GMS to minimize database calls
- **Clear Errors**: Provides actionable error messages listing missing aspects
- **Dataset-Only**: Only validates datasets, allowing other entities to pass through

## Production Considerations

### Performance

- Uses batch fetching to minimize GMS queries
- Only processes dataset entities
- Caches nothing - always checks current state

### Extensibility

- Easy to add more required aspects
- Can be extended to other entity types
- Configuration could be externalized

### Compatibility

- Works with Orrery 0.13.2+
- No core Orrery modifications required
- Deployable as external JAR

## Troubleshooting

### Validator Not Loading

Check GMS logs for plugin loading:

```bash
docker logs orrery-orrery-gms-debug-1 | grep -i "plugin"
```

### Validation Not Triggering

1. Verify entity-registry.yml has correct `supportedOperations`
2. Check that the JAR is in the correct plugin directory
3. Ensure GMS was restarted after deploying the plugin

### Authentication Issues

For testing, you may need to disable authentication:

```yaml
# application.yaml
authentication:
  enabled: false
```

## Next Steps

- **Configuration**: Add external configuration for required aspects
- **Exemptions**: Support exempting specific datasets via tags
- **Metrics**: Add monitoring for validation failures
- **Expansion**: Create similar validators for other entity types
