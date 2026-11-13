import { ArgsProps } from 'antd/lib/notification';

import { ORRERY_CLASS_NAME } from '@components/components/Notification/constants';

export const defaults: Partial<ArgsProps> = {
    placement: 'topRight',
    className: ORRERY_CLASS_NAME,
};
