from orrery.ingestion.api.registry import PluginRegistry
from orrery.ingestion.glossary.classifier import Classifier

# The built-in `orrery` classifier (OrreryClassifier) has been removed because it
# depended on the unmaintained `orrery-classify` package, which pinned
# numpy<2 and an outdated spaCy stack and blocked dependency upgrades. The
# classification framework (this registry, the `Classifier` interface, and the
# orchestration mixin) is intentionally retained so custom classifiers can still be
# registered. No classifier is registered by default.
DEFAULT_CLASSIFIER_TYPE = "orrery"

BUILTIN_CLASSIFIER_REMOVED_MESSAGE = (
    "The built-in 'orrery' column-level classifier has been removed from "
    "orrery because it depended on the unmaintained `orrery-classify` "
    "package (which pinned numpy<2). To keep using it, install the last release that "
    "supports it: `pip install 'orrery==1.6.0.5'`. Otherwise, register your "
    "own classifier implementation or disable classification "
    "(`classification.enabled: false`)."
)

classifier_registry = PluginRegistry[Classifier]()
