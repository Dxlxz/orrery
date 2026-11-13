import orrery.emitter.mce_builder as builder
import orrery.metadata.schema_classes as models
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.ingestion.graph.client import OrreryClientConfig, OrreryGraph

gms_endpoint = "http://localhost:8080"
emitter = OrreryRestEmitter(gms_server=gms_endpoint, extra_headers={})
graph = OrreryGraph(OrreryClientConfig(server=gms_endpoint))

feature_urn = builder.make_ml_feature_urn(
    feature_table_name="user_features",
    feature_name="email_address",
)

current_tags = graph.get_aspect(
    entity_urn=feature_urn, aspect_type=models.GlobalTagsClass
)

tag_to_add = builder.make_tag_urn("PII")
term_to_add = builder.make_term_urn("CustomerData")

if current_tags:
    if tag_to_add not in [tag.tag for tag in current_tags.tags]:
        current_tags.tags.append(models.TagAssociationClass(tag=tag_to_add))
else:
    current_tags = models.GlobalTagsClass(
        tags=[models.TagAssociationClass(tag=tag_to_add)]
    )

emitter.emit(
    MetadataChangeProposalWrapper(
        entityUrn=feature_urn,
        aspect=current_tags,
    )
)

current_terms = graph.get_aspect(
    entity_urn=feature_urn, aspect_type=models.GlossaryTermsClass
)

if current_terms:
    if term_to_add not in [term.urn for term in current_terms.terms]:
        current_terms.terms.append(models.GlossaryTermAssociationClass(urn=term_to_add))
else:
    current_terms = models.GlossaryTermsClass(
        terms=[models.GlossaryTermAssociationClass(urn=term_to_add)],
        auditStamp=models.AuditStampClass(time=0, actor="urn:li:corpuser:orrery"),
    )

emitter.emit(
    MetadataChangeProposalWrapper(
        entityUrn=feature_urn,
        aspect=current_terms,
    )
)
