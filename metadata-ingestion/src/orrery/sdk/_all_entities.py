from typing import Dict, List, Type

from orrery.sdk.chart import Chart
from orrery.sdk.container import Container
from orrery.sdk.dashboard import Dashboard
from orrery.sdk.dataflow import DataFlow
from orrery.sdk.datajob import DataJob
from orrery.sdk.dataset import Dataset
from orrery.sdk.document import Document
from orrery.sdk.entity import Entity
from orrery.sdk.glossary_node import GlossaryNode
from orrery.sdk.glossary_term import GlossaryTerm
from orrery.sdk.metric import Metric
from orrery.sdk.mlmodel import MLModel
from orrery.sdk.mlmodelgroup import MLModelGroup
from orrery.sdk.semantic_model import SemanticModel
from orrery.sdk.tag import Tag

# Base entity classes that don't have circular dependencies
# Those that do are imported in the EntityClient where needed
# TODO: Is there a better way to declare this?
ENTITY_CLASSES_LIST: List[Type[Entity]] = [
    Container,
    Dataset,
    Document,
    MLModel,
    MLModelGroup,
    DataFlow,
    DataJob,
    Dashboard,
    Chart,
    GlossaryNode,
    GlossaryTerm,
    Tag,
    SemanticModel,
    # NOTE: SemanticModelDataset is intentionally NOT registered here. It is a
    # Dataset subtype sharing the "dataset" entity type, so registering it would
    # overwrite Dataset in the ENTITY_CLASSES map below (last write wins) and
    # every dataset URN would hydrate as a SemanticModelDataset. Reading logical
    # datasets back as Dataset is the correct behavior.
    Metric,
]

# Create the mapping of entity types to classes
ENTITY_CLASSES: Dict[str, Type[Entity]] = {
    cls.get_urn_type().ENTITY_TYPE: cls for cls in ENTITY_CLASSES_LIST
}
