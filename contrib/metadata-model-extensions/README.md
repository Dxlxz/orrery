# Metadata Model Extensions

This directory contains community-contributed extensions to Orrery's metadata model. These extensions include custom validators, plugins, and other components that extend Orrery's core functionality.

## What Goes Here

- **Custom Validators**: Aspect payload validators that enforce business rules on metadata
- **Model Plugins**: Extensions that add new functionality to the metadata model
- **Custom Aspects**: Additional metadata aspects for specific use cases
- **Entity Extensions**: Modifications to existing entity types

## Structure

Each extension should be in its own subdirectory with:

- `README.md` - Documentation for the extension
- `build.gradle` - Build configuration
- `src/` - Source code
- Tests and examples as appropriate

## Current Extensions

- **[orrery-demo-dataset-governance-validator](./orrery-demo-dataset-governance-validator/)** - **Demo validator** that shows how to validate that logical datasets have required governance metadata (ownership, tags, domain)

## Building Extensions

Extensions in this directory are **self-contained** and work independently. To build an extension:

```bash
# From the extension directory (recommended)
cd orrery-demo-dataset-governance-validator
./gradlew build

# Create deployable plugin structure
./gradlew createModelPlugin

# Deploy to local Orrery instance
./gradlew deployPlugin
```

**Prerequisites:**

- If building from within the Orrery repository, first build required Orrery components:
  ```bash
  # From Orrery root directory
  ./gradlew :entity-registry:build :li-utils:build :metadata-models:build
  ```
- Extensions automatically detect and use Orrery JARs when available

## Contributing

See the main [CONTRIBUTING.md](../../CONTRIBUTING.md) for general contribution guidelines.

For metadata model extensions specifically:

1. **Self-contained design** - Extensions should work independently without modifying core Orrery files
2. **Smart dependency detection** - Use the build pattern that automatically detects Orrery JARs
3. **Follow the existing directory structure pattern**
4. **Include comprehensive documentation**
5. **Add tests for your extension**
6. **Update this README to list your extension**

## Standalone Usage

Extensions can be moved to standalone repositories and still work by:

1. Copying the extension directory
2. Providing required Orrery JAR files in a `libs/` directory
3. Updating the build.gradle to reference the JAR files
