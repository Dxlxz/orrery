import logging

from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter

# Imports for metadata model classes
from orrery.metadata.schema_classes import (
    PropertyValueClass,
    StructuredPropertyDefinitionClass,
)
from orrery.metadata.urns import StructuredPropertyUrn

log = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)

# Create rest emitter
rest_emitter = OrreryRestEmitter(gms_server="http://localhost:8080")

# first, let's make an open ended structured property that allows one text value
text_property_urn = StructuredPropertyUrn("io.orrery.openTextProperty")
text_property_definition = StructuredPropertyDefinitionClass(
    qualifiedName="io.orrery.openTextProperty",
    displayName="Open Text Property",
    valueType="urn:li:dataType:orrery.string",
    cardinality="SINGLE",
    entityTypes=[
        "urn:li:entityType:orrery.dataset",
        "urn:li:entityType:orrery.container",
    ],
    description="This structured property allows a signle open ended response as a value",
    immutable=False,
)

event_prop_1: MetadataChangeProposalWrapper = MetadataChangeProposalWrapper(
    entityUrn=str(text_property_urn),
    aspect=text_property_definition,
)
rest_emitter.emit(event_prop_1)

# next, let's make a property that allows for multiple orrery entity urns as values
# This example property could be used to reference other users or groups in orrery
urn_property_urn = StructuredPropertyUrn("io.orrery.dataManagement.dataSteward")
urn_property_definition = StructuredPropertyDefinitionClass(
    qualifiedName="io.orrery.dataManagement.dataSteward",
    displayName="Data Steward",
    valueType="urn:li:dataType:orrery.urn",
    cardinality="MULTIPLE",
    entityTypes=["urn:li:entityType:orrery.dataset"],
    description="The data stewards of this asset are in charge of ensuring data cleanliness and governance",
    immutable=True,
    typeQualifier={
        "allowedTypes": [
            "urn:li:entityType:orrery.corpuser",
            "urn:li:entityType:orrery.corpGroup",
        ]
    },  # this line ensures only user or group urns can be assigned as values
)

event_prop_2: MetadataChangeProposalWrapper = MetadataChangeProposalWrapper(
    entityUrn=str(urn_property_urn),
    aspect=urn_property_definition,
)
rest_emitter.emit(event_prop_2)

# finally, let's make a single select number property with a few allowed options
number_property_urn = StructuredPropertyUrn("io.orrery.dataManagement.replicationSLA")
number_property_definition = StructuredPropertyDefinitionClass(
    qualifiedName="io.orrery.dataManagement.replicationSLA",
    displayName="Retention Time",
    valueType="urn:li:dataType:orrery.number",
    cardinality="SINGLE",
    entityTypes=[
        "urn:li:entityType:orrery.dataset",
        "urn:li:entityType:orrery.dataFlow",
    ],
    description="SLA for how long data can be delayed before replicating to the destination cluster",
    immutable=False,
    allowedValues=[
        PropertyValueClass(
            value=30,
            description="30 days, usually reserved for datasets that are ephemeral and contain pii",
        ),
        PropertyValueClass(
            value=90,
            description="Use this for datasets that drive monthly reporting but contain pii",
        ),
        PropertyValueClass(
            value=365,
            description="Use this for non-sensitive data that can be retained for longer",
        ),
    ],
)

event_prop_3: MetadataChangeProposalWrapper = MetadataChangeProposalWrapper(
    entityUrn=str(number_property_urn),
    aspect=number_property_definition,
)
rest_emitter.emit(event_prop_3)
