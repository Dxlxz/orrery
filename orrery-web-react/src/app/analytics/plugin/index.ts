import amplitude from '@app/analytics/plugin/amplitude';
import orrery from '@app/analytics/plugin/orrery';
import googleAnalytics from '@app/analytics/plugin/googleAnalytics';
import logger from '@app/analytics/plugin/logger';
import mixpanel from '@app/analytics/plugin/mixpanel';

export default [googleAnalytics, mixpanel, amplitude, orrery, logger];
