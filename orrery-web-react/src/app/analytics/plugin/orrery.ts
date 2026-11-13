import analyticsConfig from '@conf/analytics';
import { resolveRuntimePath } from '@utils/runtimeBasePath';

const { orrery } = analyticsConfig;
const isEnabled: boolean = (orrery && orrery.enabled) || false;

const track = (payload) => {
    fetch(resolveRuntimePath('/openapi/v1/tracking/track'), {
        method: 'POST',
        cache: 'no-cache',
        credentials: 'same-origin',
        headers: {
            'Content-Type': 'application/json',
        },
        referrerPolicy: 'no-referrer',
        body: JSON.stringify(payload),
    });
};

const orreryPlugin = () => {
    return {
        /* All plugins require a name */
        name: 'orrery',
        initialize: () => {},
        identify: () => {},
        loaded: () => {
            return true;
        },
        page: ({ payload }) => {
            track(payload.properties);
        },
        track: ({ payload }) => {
            track(payload.properties);
        },
    };
};

export default {
    isEnabled,
    plugin: isEnabled && orreryPlugin(),
};
