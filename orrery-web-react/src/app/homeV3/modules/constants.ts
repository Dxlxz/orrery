import { Brain } from '@phosphor-icons/react/dist/csr/Brain';
import { ChartBar } from '@phosphor-icons/react/dist/csr/ChartBar';
import { Database } from '@phosphor-icons/react/dist/csr/Database';
import { Globe } from '@phosphor-icons/react/dist/csr/Globe';
import { Graph } from '@phosphor-icons/react/dist/csr/Graph';
import { LinkSimple } from '@phosphor-icons/react/dist/csr/LinkSimple';
import { Rows } from '@phosphor-icons/react/dist/csr/Rows';
import { SortAscending } from '@phosphor-icons/react/dist/csr/SortAscending';
import { Stack } from '@phosphor-icons/react/dist/csr/Stack';
import { Storefront } from '@phosphor-icons/react/dist/csr/Storefront';
import { Table } from '@phosphor-icons/react/dist/csr/Table';
import { TextT } from '@phosphor-icons/react/dist/csr/TextT';
import i18next from 'i18next';
import React from 'react';

import { PageTemplateFragment } from '@graphql/template.generated';
import { OrreryPageModuleType, EntityType, PageModuleScope, PageTemplateScope, PageTemplateSurfaceType } from '@types';

// TODO: remove these description once descriptions in modules are implemented
export const MODULE_TYPE_TO_DESCRIPTION: Map<OrreryPageModuleType, string> = new Map([
    [OrreryPageModuleType.AssetCollection, 'A curated list of assets of your choosing'],
    [OrreryPageModuleType.Domains, 'Most used domains in your organization'],
    [OrreryPageModuleType.Hierarchy, 'Top down view of assets'],
    [OrreryPageModuleType.Link, 'Choose links that are important'],
    [OrreryPageModuleType.OwnedAssets, 'Assets the current user owns'],
    [OrreryPageModuleType.RichText, 'Pin docs for your Orrery users'],
    [OrreryPageModuleType.Columns, 'View the columns of this dataset'],
    [OrreryPageModuleType.SemanticModelDatasets, 'Datasets referenced by this semantic model'],
    [OrreryPageModuleType.SemanticModelMetrics, 'Metrics defined within this semantic model'],
    [OrreryPageModuleType.SemanticModelRelationships, 'Relationships defined in this semantic model'],
    [OrreryPageModuleType.SemanticModelDimensions, 'Dimensions defined in this semantic model'],
    [OrreryPageModuleType.AiContext, 'AI context including synonyms, instructions, and examples'],
    [OrreryPageModuleType.MetricSql, 'SQL expression for this metric'],
    [OrreryPageModuleType.RelatedMetrics, 'Related metrics'],
    [OrreryPageModuleType.SubDataProducts, 'Child data products of this data product'],
]);

export const MODULE_TYPE_TO_ICON: Map<OrreryPageModuleType, React.ComponentType<any>> = new Map([
    [OrreryPageModuleType.AssetCollection, Stack],
    [OrreryPageModuleType.Domains, Globe],
    [OrreryPageModuleType.Hierarchy, SortAscending],
    [OrreryPageModuleType.Link, LinkSimple],
    [OrreryPageModuleType.OwnedAssets, Database],
    [OrreryPageModuleType.RichText, TextT],
    [OrreryPageModuleType.Columns, Table],
    [OrreryPageModuleType.SemanticModelDatasets, Database],
    [OrreryPageModuleType.SemanticModelMetrics, ChartBar],
    [OrreryPageModuleType.SemanticModelRelationships, Graph],
    [OrreryPageModuleType.SemanticModelDimensions, Rows],
    [OrreryPageModuleType.AiContext, Brain],
    [OrreryPageModuleType.MetricSql, TextT],
    [OrreryPageModuleType.RelatedMetrics, ChartBar],
    [OrreryPageModuleType.SubDataProducts, Storefront],
]);

export const DEFAULT_MODULE_ICON = Database;

// Shared list sizing for home-page list modules, kept here so caps aren't hardcoded per
// module (prevents them drifting out of sync across modules).
export const MODULE_LIST_PAGE_SIZE = 10; // page size for infinite-scroll list modules
// Client-side cap for top-N slice modules (e.g. Domains). Keep in sync with the recommendation
// source's getMaxContent() on the backend (DomainsCandidateSource.java) to avoid FE/BE drift.
export const MODULE_LIST_MAX_ITEMS = 25;

// keep this in sync with PageModuleService.java
export const DEFAULT_MODULE_URNS = [
    'urn:li:orreryPageModule:your_assets',
    'urn:li:orreryPageModule:your_subscriptions',
    'urn:li:orreryPageModule:top_domains',
    'urn:li:orreryPageModule:assets',
    'urn:li:orreryPageModule:output_ports',
    'urn:li:orreryPageModule:child_hierarchy',
    'urn:li:orreryPageModule:data_products',
    'urn:li:orreryPageModule:sub_data_products',
    'urn:li:orreryPageModule:related_terms',
    'urn:li:orreryPageModule:platforms',
    'urn:li:orreryPageModule:lineage',
    'urn:li:orreryPageModule:columns',
    'urn:li:orreryPageModule:semantic_model_datasets',
    'urn:li:orreryPageModule:semantic_model_metrics',
    'urn:li:orreryPageModule:semantic_model_relationships',
    'urn:li:orreryPageModule:semantic_model_dimensions',
    'urn:li:orreryPageModule:ai_context',
    'urn:li:orreryPageModule:sql',
    'urn:li:orreryPageModule:related_metrics',
];

export const DEFAULT_TEMPLATE_URN = 'urn:li:orreryPageTemplate:home_default_1';

export const CUSTOM_MODULE_TYPES: OrreryPageModuleType[] = [
    OrreryPageModuleType.Link,
    OrreryPageModuleType.RichText,
    OrreryPageModuleType.Hierarchy,
    OrreryPageModuleType.AssetCollection,
];

export const LARGE_MODULE_TYPES: OrreryPageModuleType[] = [
    OrreryPageModuleType.OwnedAssets,
    OrreryPageModuleType.Domains,
    OrreryPageModuleType.AssetCollection,
    OrreryPageModuleType.Hierarchy,
    OrreryPageModuleType.RichText,
    OrreryPageModuleType.Assets,
    OrreryPageModuleType.OutputPorts,
    OrreryPageModuleType.ChildHierarchy,
    OrreryPageModuleType.RelatedTerms,
    OrreryPageModuleType.DataProducts,
    OrreryPageModuleType.SubDataProducts,
    OrreryPageModuleType.Columns,
    OrreryPageModuleType.SemanticModelDatasets,
    OrreryPageModuleType.SemanticModelMetrics,
    OrreryPageModuleType.SemanticModelRelationships,
    OrreryPageModuleType.SemanticModelDimensions,
    OrreryPageModuleType.AiContext,
    OrreryPageModuleType.MetricSql,
    OrreryPageModuleType.RelatedMetrics,
];

export const SMALL_MODULE_TYPES: OrreryPageModuleType[] = [OrreryPageModuleType.Link];

export const DEFAULT_TEMPLATE: PageTemplateFragment = {
    urn: DEFAULT_TEMPLATE_URN,
    type: EntityType.OrreryPageTemplate,
    properties: {
        visibility: {
            scope: PageTemplateScope.Global,
        },
        surface: {
            surfaceType: PageTemplateSurfaceType.HomePage,
        },
        rows: [
            {
                modules: [
                    {
                        urn: 'urn:li:orreryPageModule:your_assets',
                        type: EntityType.OrreryPageModule,
                        properties: {
                            get name() {
                                return i18next.t('modules:yourAssets.moduleName');
                            },
                            type: OrreryPageModuleType.OwnedAssets,
                            visibility: {
                                scope: PageModuleScope.Global,
                            },
                            params: {},
                        },
                    },
                    {
                        urn: 'urn:li:orreryPageModule:top_domains',
                        type: EntityType.OrreryPageModule,
                        properties: {
                            get name() {
                                return i18next.t('modules:domains.moduleName');
                            },
                            type: OrreryPageModuleType.Domains,
                            visibility: {
                                scope: PageModuleScope.Global,
                            },
                            params: {},
                        },
                    },
                ],
            },
        ],
    },
};
