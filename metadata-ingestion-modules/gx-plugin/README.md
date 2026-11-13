<!-- PyPI long description. Keep concise, feature-discovery-first. -->

# Orrery Great Expectations (GX) Plugin

**Send Great Expectations data quality results into Orrery** — surface assertion outcomes alongside your dataset metadata so teams can see data health at a glance.

## What you can do

- **Emit assertion results** from GX Checkpoints directly into Orrery as data quality assertions
- **Link quality checks to datasets** — results appear on the dataset's profile in Orrery
- **Track pass/fail history** over time for every expectation suite
- **Works with any Orrery deployment** — self-hosted or Orrery Cloud

## Compatibility

| GX version      | Module                                                |
| --------------- | ----------------------------------------------------- |
| 0.17.x / 0.18.x | `orrery_gx_plugin.action.OrreryValidationAction`    |
| 1.x             | `orrery_gx_plugin.action_v1.OrreryValidationAction` |

Both APIs are supported additively in the same package. Use the module that matches your installed GX major version.

## Installation

```bash
pip install orrery-gx-plugin
```

## Quickstart (GX 0.17 / 0.18)

Add the Orrery action to your GX Checkpoint:

```python
from orrery_gx_plugin.action import OrreryValidationAction
from great_expectations.checkpoint import Checkpoint

checkpoint = Checkpoint(
    name="my_checkpoint",
    data_context=context,
    action_list=[
        {
            "name": "orrery",
            "action": {
                "class_name": "OrreryValidationAction",
                "module_name": "orrery_gx_plugin.action",
                "server_url": "http://localhost:8080",
            },
        }
    ],
)
```

## Quickstart (GX Core 1.x)

```python
import great_expectations as gx
from orrery_gx_plugin.action_v1 import OrreryValidationAction

context = gx.get_context()
# ... create validation_definitions ...

checkpoint = context.checkpoints.add(
    gx.Checkpoint(
        name="my_checkpoint",
        validation_definitions=validation_definitions,
        actions=[
            OrreryValidationAction(
                name="orrery",
                server_url="http://localhost:8080",
                # Optional when batch_spec meta is incomplete:
                # platform="postgres",
                # dataset_name="public.my_table",
            )
        ],
    )
)
checkpoint.run()
```

Results from every Checkpoint run will appear in Orrery under the dataset's **Validation** tab.

## Links

- [Full documentation](https://docs.orrery.dev/docs/metadata-ingestion/integration_docs/great-expectations)
- [Great Expectations](https://greatexpectations.io/)
- [GitHub](https://github.com/Dxlxz/orrery)
- [Slack community](https://orrery.dev/slack)
