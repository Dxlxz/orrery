# CLAUDE.md

Java SDK V2 integration tests - guidance for Claude Code.

## Running Integration Tests

**Prerequisites:** Running Orrery instance (start with `./gradlew quickstartDebug`)

```bash
# Run all integration tests
ADMIN_USERNAME=orrery ADMIN_PASSWORD=orrery ./gradlew :metadata-integration:java:orrery-client:test --tests "*Integration*"

# Run specific entity tests
./gradlew :metadata-integration:java:orrery-client:test --tests "DatasetIntegrationTest"

# Run single test method
./gradlew :metadata-integration:java:orrery-client:test --tests "DatasetIntegrationTest.testDatasetCreateMinimal"

# With verbose output
./gradlew :metadata-integration:java:orrery-client:test --tests "*Integration*" --info
```

## Environment Variables

| Variable         | Required | Default                 | Description                                    |
| ---------------- | -------- | ----------------------- | ---------------------------------------------- |
| `ORRERY_SERVER` | Yes      | `http://localhost:8080` | Orrery GMS server URL                         |
| `ORRERY_TOKEN`  | No       | _auto-generated_        | Personal access token (skips auth if provided) |
| `ADMIN_USERNAME` | No       | `orrery`               | Admin username for authentication              |
| `ADMIN_PASSWORD` | No       | `orrery`               | Admin password for authentication              |

**Note:** `BaseIntegrationTest` automatically generates tokens from admin credentials if `ORRERY_TOKEN` not provided.

## Test Organization

Integration tests cover 8 entity types (Dataset, Chart, Dashboard, DataJob, DataFlow, Container, MLModel, MLModelGroup), plus emit modes (`EmitModeIntegrationTest`) and `EntityClient.get()` (`EntityClientGetIntegrationTest`). Name new test classes `*IntegrationTest`: CI selects them with `--tests "*Integration*"`.

All tests extend `BaseIntegrationTest` which handles authentication, token generation, client initialization, and cleanup.

## Adding New Integration Tests

```java
public class MyEntityIntegrationTest extends BaseIntegrationTest {
  @Test
  public void testMyEntity() throws Exception {
    // Use inherited 'client' field
    MyEntity entity = MyEntity.builder()
        .id("test_entity_" + System.currentTimeMillis())  // Use timestamps for unique IDs
        .build();
    client.entities().upsert(entity);
    assertNotNull(entity.getUrn());
  }
}
```

**Important:** Use timestamp-based IDs to avoid conflicts. No cleanup needed - `BaseIntegrationTest` handles lifecycle.

## Troubleshooting

**Tests skipped:** Verify Orrery running and environment set:

```bash
curl http://localhost:8080/health
export ORRERY_SERVER=http://localhost:8080
```

**Authentication failures:** Check credentials:

```bash
export ADMIN_USERNAME=orrery
export ADMIN_PASSWORD=orrery
```

**Connection issues:** Check services:

```bash
docker ps | grep orrery
curl http://localhost:8080/health
docker logs orrery-gms  # View GMS logs
```
