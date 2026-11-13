import { DocumentTreeNode } from '@app/document/DocumentTreeContext';
import { ORRERY_PLATFORM_URN } from '@app/document/utils/documentTreeFilters';

import { DataPlatform, FacetMetadata } from '@types';

/**
 * Resolves a user-facing label for a source platform.
 *
 * Prefers the backend-populated `properties.displayName` ("Google Docs", "Confluence").
 * Falls back to title-casing the raw platform name when display name is missing —
 * `google_docs` becomes `Google Docs`, `confluence` becomes `Confluence`. This is
 * a stopgap so the sidebar reads cleanly even when a platform's metadata aspect
 * hasn't been bootstrapped server-side.
 *
 * Returns an empty string when neither a display name nor a raw name is present —
 * callers treat that as "platform name unresolvable".
 *
 * @param platform - Source platform
 * @returns Display label suitable for a section header, or '' when unresolvable
 */
export function formatPlatformLabel(platform: DataPlatform): string {
    const displayName = platform.properties?.displayName || platform.displayName;
    if (displayName) return displayName;

    const name = platform.name || '';
    return name
        .split('_')
        .filter(Boolean)
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
}

export interface DocumentSourceGroup {
    platform: DataPlatform;
    /** Resolved, human-readable section label (see {@link formatPlatformLabel}). */
    label: string;
    nodes: DocumentTreeNode[];
}

/** Raised facet cap so every source section can render without waiting on root pagination. */
export const SOURCE_SECTION_FACET_MAX = 100;

/**
 * One root per missing platform is enough to render its header. A full page
 * plus a child-count query per row ran on every Source load, sort, and View change.
 */
export const SOURCE_SECTION_HEADER_COUNT = 1;

export type DocumentSourceSection =
    | { kind: 'native' }
    | { kind: 'platform'; platformUrn: string; platform: DataPlatform; label: string };

/**
 * Which sidebar section a root belongs in.
 * Platform wins over NATIVE/EXTERNAL — a GitHub doc imported as native still
 * sits under GitHub, matching {@link partitionRootNodesByLayer}.
 */
export function resolveDocumentSourceSection(node: Pick<DocumentTreeNode, 'platform'>): DocumentSourceSection {
    const { platform } = node;
    const platformUrn = platform?.urn;
    if (!platform || !platformUrn || platformUrn === ORRERY_PLATFORM_URN) {
        return { kind: 'native' };
    }

    const label = formatPlatformLabel(platform);
    if (!label) {
        return { kind: 'native' };
    }

    return { kind: 'platform', platformUrn, platform, label };
}

/**
 * Platform URNs whose section is absent from the roots loaded so far.
 * The Orrery section is present when any root resolves as native, because
 * null, Orrery, and unlabeled platforms share that header.
 */
export function sourcePlatformsMissingFromRoots(
    platformUrns: string[],
    roots: Array<Pick<DocumentTreeNode, 'platform'>>,
): string[] {
    const hasNative = roots.some((root) => resolveDocumentSourceSection(root).kind === 'native');
    const presentPlatformUrns = new Set(
        roots.flatMap((root) => {
            const section = resolveDocumentSourceSection(root);
            return section.kind === 'platform' ? [section.platformUrn] : [];
        }),
    );

    const missing: string[] = [];
    const seen = new Set<string>();
    platformUrns.forEach((platformUrn) => {
        if (!platformUrn || seen.has(platformUrn)) return;
        seen.add(platformUrn);
        const represented = platformUrn === ORRERY_PLATFORM_URN ? hasNative : presentPlatformUrns.has(platformUrn);
        if (!represented) missing.push(platformUrn);
    });
    return missing;
}

/** Positive facet bucket values (count > 0), in aggregation order. */
export function positiveFacetAggregationValues(
    aggregations: FacetMetadata['aggregations'] | null | undefined,
): string[] {
    return (aggregations ?? []).flatMap((aggregation) =>
        aggregation && aggregation.count > 0 && aggregation.value ? [aggregation.value] : [],
    );
}

export interface PartitionedRootNodes {
    /**
     * Root documents that belong to Orrery itself — rendered under the
     * "Orrery" section. A document lands here when its resolved platform is the
     * Orrery platform, when it carries no platform at all, or when the source
     * platform's name can't be resolved into a section label.
     */
    native: DocumentTreeNode[];
    /**
     * Root documents grouped by source platform — one section per platform
     * (GitHub, Notion, Confluence, …). Order follows first appearance in the
     * input, so callers can sort upstream to control display order.
     */
    sourcesByPlatform: DocumentSourceGroup[];
}

/**
 * Splits a flat list of root tree nodes into the sections the sidebar renders,
 * keyed on the document's resolved source platform (independent of whether the
 * document was imported as NATIVE or EXTERNAL).
 *
 * The backend resolves a document's `platform` from its data-platform-instance
 * aspect, defaulting to the Orrery platform when no such aspect exists. So:
 *
 *   - if the document resolves to a non-Orrery platform whose name we can turn
 *     into a label, it gets that platform's source section
 *   - otherwise (Orrery platform, no platform, or an unresolvable name) it
 *     falls back to the curated "Orrery" section
 *
 * Keying on platform — rather than the NATIVE/EXTERNAL source type — means a
 * GitHub document imported as NATIVE still appears under a "GitHub" section
 * (its resolved platform is `github`) instead of the Orrery section.
 *
 * @param rootNodes - Root nodes from the document tree
 * @returns Orrery-section nodes alongside per-platform source groups
 */
export function partitionRootNodesByLayer(rootNodes: DocumentTreeNode[]): PartitionedRootNodes {
    const native: DocumentTreeNode[] = [];
    const groupsByUrn = new Map<string, DocumentSourceGroup>();

    rootNodes.forEach((node) => {
        const section = resolveDocumentSourceSection(node);
        if (section.kind === 'native') {
            native.push(node);
            return;
        }

        const existing = groupsByUrn.get(section.platformUrn);
        if (existing) {
            existing.nodes.push(node);
        } else {
            groupsByUrn.set(section.platformUrn, {
                platform: section.platform,
                label: section.label,
                nodes: [node],
            });
        }
    });

    return { native, sourcesByPlatform: [...groupsByUrn.values()] };
}
