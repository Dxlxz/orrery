import React from 'react';

import { OrreryPageModuleType } from '@types';

export type ModuleInfo = {
    key: string;
    urn?: string; // Filled in a case of working with an existing module (e.g. admin created modules)
    type: OrreryPageModuleType;
    name: string;
    description?: string;
    icon: React.ComponentType<any>;
};
