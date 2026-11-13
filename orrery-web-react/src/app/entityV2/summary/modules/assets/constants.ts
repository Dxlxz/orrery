import { EntityType } from '@types';

export const ENTITIES_TO_ADD_TO_ASSETS = [EntityType.DataProduct, EntityType.Domain, EntityType.GlossaryTerm];

export const CONTENTS_MODULE_URN = 'urn:li:orreryPageModule:contents';
export const DATA_SOURCES_MODULE_URN = 'urn:li:orreryPageModule:data_sources';

export function isDataSourcesModule(moduleUrn?: string): boolean {
    return moduleUrn === DATA_SOURCES_MODULE_URN;
}
