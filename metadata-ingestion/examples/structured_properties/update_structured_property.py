import logging
from typing import Union

from orrery.configuration.kafka import KafkaProducerConnectionConfig
from orrery.emitter.kafka_emitter import KafkaEmitterConfig, OrreryKafkaEmitter
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.urns import StructuredPropertyUrn
from orrery.specific.structured_property import StructuredPropertyPatchBuilder

log = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)


# Get an emitter, either REST or Kafka, this example shows you both
def get_emitter() -> Union[OrreryRestEmitter, OrreryKafkaEmitter]:
    USE_REST_EMITTER = True
    if USE_REST_EMITTER:
        gms_endpoint = "http://localhost:8080"
        return OrreryRestEmitter(gms_server=gms_endpoint)
    else:
        kafka_server = "localhost:9092"
        schema_registry_url = "http://localhost:8081"
        return OrreryKafkaEmitter(
            config=KafkaEmitterConfig(
                connection=KafkaProducerConnectionConfig(
                    bootstrap=kafka_server, schema_registry_url=schema_registry_url
                )
            )
        )


# input your unique structured property ID
property_urn = StructuredPropertyUrn("io.orrery.dataManagement.dataSteward")

with get_emitter() as emitter:
    for patch_mcp in (
        StructuredPropertyPatchBuilder(str(property_urn))
        .set_display_name("test display name")
        .set_cardinality("MULTIPLE")
        .add_entity_type("urn:li:entityType:orrery.dataJob")
        .build()
    ):
        emitter.emit(patch_mcp)
