import React, { useMemo } from 'react';

import { usePageTemplateContext } from '@app/homeV3/context/PageTemplateContext';
import AssetCollectionModal from '@app/homeV3/modules/assetCollection/AssetCollectionModal';
import DocumentationModal from '@app/homeV3/modules/documentation/DocumentationModal';
import HierarchyViewModal from '@app/homeV3/modules/hierarchyViewModule/HierarchyViewModal';
import LinkModal from '@app/homeV3/modules/link/LinkModal';

import { OrreryPageModuleType } from '@types';

export default function ModuleModalMapper() {
    const {
        moduleModalState: { moduleType },
    } = usePageTemplateContext();

    const ModuleModalComponent = useMemo(() => {
        switch (moduleType) {
            // TODO: add support of other module types
            case OrreryPageModuleType.AssetCollection:
                return AssetCollectionModal;
            case OrreryPageModuleType.Link:
                return LinkModal;
            case OrreryPageModuleType.RichText:
                return DocumentationModal;
            case OrreryPageModuleType.Hierarchy:
                return HierarchyViewModal;
            default:
                return null;
        }
    }, [moduleType]);

    if (moduleType === undefined) return null;
    if (!ModuleModalComponent) return null;

    return <ModuleModalComponent />;
}
