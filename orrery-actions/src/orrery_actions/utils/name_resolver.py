from abc import abstractmethod
from typing import Optional

from orrery.ingestion.graph.client import OrreryGraph
from orrery.metadata.schema_classes import (
    ChartInfoClass,
    ContainerPropertiesClass,
    CorpUserEditableInfoClass,
    CorpUserInfoClass,
    DashboardInfoClass,
    DatasetPropertiesClass,
    DomainPropertiesClass,
    GlossaryTermInfoClass,
    SubTypesClass,
    TagPropertiesClass,
)
from orrery.utilities.urns.data_flow_urn import DataFlowUrn
from orrery.utilities.urns.data_job_urn import DataJobUrn
from orrery.utilities.urns.dataset_urn import DatasetUrn
from orrery.utilities.urns.urn import Urn
from orrery_actions.utils.orrery_util import ORRERY_SYSTEM_ACTOR_URN


class NameResolver:
    @abstractmethod
    def get_entity_name(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        pass

    @abstractmethod
    def get_specialized_type(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        pass


class DefaultNameResolver(NameResolver):
    """A Default name resolver for Entities"""

    def get_entity_name(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        default_entity_name = (
            entity_urn.get_entity_id_as_string()
            if str(entity_urn) != ORRERY_SYSTEM_ACTOR_URN
            else "System"
        )
        return default_entity_name

    def get_specialized_type(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        entity_type = entity_urn.entity_type
        entity_type = {"schemaField": "column"}.get(
            entity_urn.entity_type, entity_urn.entity_type
        )

        if orrery_graph:
            subtypes: Optional[SubTypesClass] = orrery_graph.get_aspect(
                entity_urn=str(entity_urn), aspect_type=SubTypesClass
            )
            subtype = subtypes.typeNames[0] if subtypes and subtypes.typeNames else None
            return f"{subtype or entity_type}"

        return entity_type


class DataFlowNameResolver(DefaultNameResolver):
    def get_entity_name(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        dataflow_urn = DataFlowUrn.from_string(str(entity_urn))
        dataflow_name = dataflow_urn.get_flow_id()
        return dataflow_name

    def get_specialized_type(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        """We prefix the type of the dataset with the platform it is part of"""
        dataflow_urn = DataFlowUrn.from_string(str(entity_urn))
        dataflow_orch = dataflow_urn.get_orchestrator_name()
        return f"{dataflow_orch} DAG"


class DataJobNameResolver(DefaultNameResolver):
    def get_entity_name(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        datajob_urn = DataJobUrn.from_string(str(entity_urn))
        datajob_name = datajob_urn.get_job_id()
        return datajob_name

    def get_specialized_type(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        """We prefix the type of the datajob with the platform it is part of"""
        datajob_urn = DataJobUrn.from_string(str(entity_urn))
        dataflow_urn = datajob_urn.get_data_flow_urn()
        dataflow_orch = dataflow_urn.get_orchestrator_name()
        return f"{dataflow_orch} Task"


class DatasetNameResolver(DefaultNameResolver):
    def get_entity_name(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        dataset_urn = DatasetUrn.from_string(str(entity_urn))
        dataset_name = dataset_urn.name
        if orrery_graph:
            properties: Optional[DatasetPropertiesClass] = orrery_graph.get_aspect(
                entity_urn=str(entity_urn), aspect_type=DatasetPropertiesClass
            )
            if properties and properties.name:
                dataset_name = properties.name

        return f"{dataset_name}"

    def get_specialized_type(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        """We prefix the type of the dataset with the platform it is part of"""
        dataset_urn = DatasetUrn.from_string(str(entity_urn))
        dataset_platform = dataset_urn.get_data_platform_urn().get_entity_id_as_string()
        specialized_type = super().get_specialized_type(entity_urn, orrery_graph)
        return f"{dataset_platform} {specialized_type}"


class SchemaFieldNameResolver(DefaultNameResolver):
    def get_entity_name(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        return DatasetUrn.get_simple_field_path_from_v2_field_path(
            entity_urn.entity_ids[1]
        )

    def get_specialized_type(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        return "column"


class ContainerNameResolver(DefaultNameResolver):
    def get_entity_name(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        if orrery_graph:
            container_props: Optional[ContainerPropertiesClass] = (
                orrery_graph.get_aspect(
                    entity_urn=str(entity_urn), aspect_type=ContainerPropertiesClass
                )
            )
            if container_props and container_props.name:
                return container_props.name

        # fall-through to default
        return super().get_entity_name(entity_urn, orrery_graph)


class TagNameResolver(DefaultNameResolver):
    def get_entity_name(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        if orrery_graph:
            tag_properties: Optional[TagPropertiesClass] = orrery_graph.get_aspect(
                entity_urn=str(entity_urn), aspect_type=TagPropertiesClass
            )
            if tag_properties and tag_properties.name:
                return tag_properties.name

        # fall-through to default
        return super().get_entity_name(entity_urn, orrery_graph)


class GlossaryTermResolver(DefaultNameResolver):
    def get_entity_name(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        if orrery_graph:
            term_properties: Optional[GlossaryTermInfoClass] = orrery_graph.get_aspect(
                entity_urn=str(entity_urn), aspect_type=GlossaryTermInfoClass
            )
            if term_properties and term_properties.name:
                return term_properties.name

        # fall-through to default
        return super().get_entity_name(entity_urn, orrery_graph)


class CorpUserNameResolver(DefaultNameResolver):
    def get_entity_name(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        entity_name = super().get_entity_name(entity_urn, orrery_graph)

        if orrery_graph:
            user_properties: Optional[CorpUserInfoClass] = orrery_graph.get_aspect(
                str(entity_urn), CorpUserInfoClass
            )
            if user_properties and user_properties.displayName:
                entity_name = user_properties.displayName

            editable_properties: Optional[CorpUserEditableInfoClass] = (
                orrery_graph.get_aspect(str(entity_urn), CorpUserEditableInfoClass)
            )
            if editable_properties and editable_properties.displayName:
                entity_name = editable_properties.displayName

        return entity_name


class ChartNameResolver(DefaultNameResolver):
    def get_entity_name(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        if orrery_graph:
            chart_properties: Optional[ChartInfoClass] = orrery_graph.get_aspect(
                str(entity_urn), ChartInfoClass
            )
            if chart_properties and chart_properties.title:
                return chart_properties.title

        # fall-through to default
        return super().get_entity_name(entity_urn, orrery_graph)


class DashboardNameResolver(DefaultNameResolver):
    def get_entity_name(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        if orrery_graph:
            dashboard_properties: Optional[DashboardInfoClass] = (
                orrery_graph.get_aspect(str(entity_urn), DashboardInfoClass)
            )
            if dashboard_properties and dashboard_properties.title:
                return dashboard_properties.title

        # fall-through to default
        return super().get_entity_name(entity_urn, orrery_graph)


class DomainNameResolver(DefaultNameResolver):
    def get_entity_name(
        self, entity_urn: Urn, orrery_graph: Optional[OrreryGraph]
    ) -> str:
        if orrery_graph:
            domain_properties: Optional[DomainPropertiesClass] = (
                orrery_graph.get_aspect(str(entity_urn), DomainPropertiesClass)
            )
            if domain_properties and domain_properties.name:
                return domain_properties.name

        # fall-through to default
        return super().get_entity_name(entity_urn, orrery_graph)


class NameResolverRegistry:
    def __init__(self):
        self.registry = {
            "schemaField": SchemaFieldNameResolver(),
            "dataset": DatasetNameResolver(),
            "glossaryTerm": GlossaryTermResolver(),
            "chart": ChartNameResolver(),
            "dashboard": DashboardNameResolver(),
            "corpuser": CorpUserNameResolver(),
            "container": ContainerNameResolver(),
            "dataJob": DataJobNameResolver(),
            "dataFlow": DataFlowNameResolver(),
            "domain": DomainNameResolver(),
        }
        self.default_resolver = DefaultNameResolver()

    def get_resolver(self, entity_urn: Urn) -> NameResolver:
        return self.registry.get(entity_urn.entity_type, self.default_resolver)


_name_resolver_registry = NameResolverRegistry()


def get_entity_name_from_urn(
    entity_urn_str: str, orrery_graph: Optional[OrreryGraph]
) -> str:
    entity_urn = Urn.from_string(entity_urn_str)
    return _name_resolver_registry.get_resolver(entity_urn).get_entity_name(
        entity_urn, orrery_graph
    )


def get_entity_qualifier_from_urn(
    entity_urn_str: str, orrery_graph: Optional[OrreryGraph]
) -> str:
    entity_urn = Urn.from_string(entity_urn_str)
    return _name_resolver_registry.get_resolver(entity_urn).get_specialized_type(
        entity_urn, orrery_graph
    )
