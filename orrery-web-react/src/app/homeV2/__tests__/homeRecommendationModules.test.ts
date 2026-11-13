import { describe, expect, it } from 'vitest';

import {
    HOME_V2_RECOMMENDATION_MODULE_IDS,
    collectHomeRecommendationModuleIds,
    pageModuleTypeToRecommendationModuleId,
    sortRecommendationModuleIds,
} from '@app/homeV2/homeRecommendationModules';
import { DEFAULT_TEMPLATE } from '@app/homeV3/modules/constants';

import { PageTemplateFragment } from '@graphql/template.generated';
import { OrreryPageModuleType, EntityType, PageModuleScope, RecommendationModuleId } from '@types';

function createModule(type: OrreryPageModuleType, urn = `urn:li:orreryPageModule:${type}`) {
    return {
        urn,
        type: EntityType.OrreryPageModule,
        exists: true,
        properties: {
            name: type,
            type,
            visibility: { scope: PageModuleScope.Global },
            params: {},
        },
    };
}

function createTemplate(moduleTypes: OrreryPageModuleType[]): PageTemplateFragment {
    return {
        urn: 'urn:li:orreryPageTemplate:test',
        type: EntityType.OrreryPageTemplate,
        properties: {
            rows: [
                {
                    modules: moduleTypes.map((type) => createModule(type)),
                },
            ],
        },
    } as PageTemplateFragment;
}

describe('pageModuleTypeToRecommendationModuleId', () => {
    it('maps Domains and Platforms only', () => {
        expect(pageModuleTypeToRecommendationModuleId(OrreryPageModuleType.Domains)).toBe(
            RecommendationModuleId.Domains,
        );
        expect(pageModuleTypeToRecommendationModuleId(OrreryPageModuleType.Platforms)).toBe(
            RecommendationModuleId.Platforms,
        );
        expect(pageModuleTypeToRecommendationModuleId(OrreryPageModuleType.OwnedAssets)).toBeUndefined();
        expect(pageModuleTypeToRecommendationModuleId(OrreryPageModuleType.Link)).toBeUndefined();
        expect(pageModuleTypeToRecommendationModuleId(undefined)).toBeUndefined();
    });
});

describe('sortRecommendationModuleIds', () => {
    it('dedupes and sorts for a stable Apollo cache key', () => {
        expect(
            sortRecommendationModuleIds([
                RecommendationModuleId.Platforms,
                RecommendationModuleId.Domains,
                RecommendationModuleId.Platforms,
            ]),
        ).toEqual([RecommendationModuleId.Domains, RecommendationModuleId.Platforms]);
    });
});

describe('collectHomeRecommendationModuleIds', () => {
    it('returns only Domains for the default Home V3 template', () => {
        expect(collectHomeRecommendationModuleIds(DEFAULT_TEMPLATE)).toEqual([RecommendationModuleId.Domains]);
    });

    it('includes Platforms when present, sorted independently of template order', () => {
        expect(
            collectHomeRecommendationModuleIds(
                createTemplate([OrreryPageModuleType.Platforms, OrreryPageModuleType.Domains]),
            ),
        ).toEqual([RecommendationModuleId.Domains, RecommendationModuleId.Platforms]);
    });

    it('ignores modules that do not use listRecommendations', () => {
        expect(
            collectHomeRecommendationModuleIds(
                createTemplate([
                    OrreryPageModuleType.OwnedAssets,
                    OrreryPageModuleType.Link,
                    OrreryPageModuleType.RichText,
                ]),
            ),
        ).toEqual([]);
    });

    it('returns an empty list for a missing template so the query can skip', () => {
        expect(collectHomeRecommendationModuleIds(null)).toEqual([]);
        expect(collectHomeRecommendationModuleIds(undefined)).toEqual([]);
    });
});

describe('HOME_V2_RECOMMENDATION_MODULE_IDS', () => {
    it('requests Domains, Platforms, and Top Terms in sorted order', () => {
        expect(HOME_V2_RECOMMENDATION_MODULE_IDS).toEqual([
            RecommendationModuleId.Domains,
            RecommendationModuleId.Platforms,
            RecommendationModuleId.TopTerms,
        ]);
    });
});
