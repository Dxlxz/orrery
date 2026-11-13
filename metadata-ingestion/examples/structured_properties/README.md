# Extended Properties

## Expected Capabilities

### structured_properties command

```yaml
- id: io.orrery.privacy.retentionTime
  # urn: urn:li:structuredProperty:<>
  # fullyQualifiedName: io.orrery.privacy.retentionTime
  type: STRING
  cardinality: MULTIPLE
  entityTypes:
    - dataset # or urn:li:logicalEntity:metamodel.orrery.dataset
    - dataflow
  description: "Retention Time is used to figure out how long to retain records in a dataset"
  allowedValues:
    - value: 30 days
      description: 30 days, usually reserved for datasets that are ephemeral and contain pii
    - value: 3 months
      description: Use this for datasets that drive monthly reporting but contain pii
    - value: 2 yrs
      description: Use this for non-sensitive data that can be retained for longer
- id: io.orrery.dataManagement.replicationSLA
  type: NUMBER
  description: "SLA for how long data can be delayed before replicating to the destination cluster"
  entityTypes:
    - dataset
- id: io.orrery.dataManagement.deprecationDate
  type: DATE
  entityTypes:
    - dataset
    - dataFlow
    - dataJob
```

```
orrery properties upsert -f structured_properties.yaml
```

```
orrery properties create --name io.orrery.privacy.retentionTime --type STRING --cardinality MULTIPLE --entity_type DATASET --entity_type DATAFLOW
```

### dataset command

```
orrery dataset upsert -f dataset.yaml
```

See example in `dataproduct`.
