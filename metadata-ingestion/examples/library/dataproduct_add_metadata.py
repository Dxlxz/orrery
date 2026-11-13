from orrery.emitter.mce_builder import (
    make_data_product_urn,
    make_tag_urn,
    make_term_urn,
)
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import (
    GlossaryTermAssociationClass,
    TagAssociationClass,
)
from orrery.specific.dataproduct import DataProductPatchBuilder

rest_emitter = OrreryRestEmitter(gms_server="http://localhost:8080")

data_product_urn = make_data_product_urn("customer_360")

for mcp in (
    DataProductPatchBuilder(data_product_urn)
    .add_tag(TagAssociationClass(tag=make_tag_urn("production")))
    .add_tag(TagAssociationClass(tag=make_tag_urn("pii")))
    .add_term(
        GlossaryTermAssociationClass(urn=make_term_urn("CustomerData.PersonalInfo"))
    )
    .build()
):
    rest_emitter.emit(mcp)
    print(f"Added metadata to Data Product {data_product_urn}")
