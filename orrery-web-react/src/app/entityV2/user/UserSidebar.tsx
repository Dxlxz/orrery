import { toast } from '@components';
import React from 'react';
import { useTranslation } from 'react-i18next';

import { useUserContext } from '@app/context/useUserContext';
import { Content, SideBar } from '@app/entityV2/shared/SidebarStyledComponents';
import { AboutSidebarSection } from '@app/entityV2/shared/sidebarSection/AboutSidebarSection';
import { UserGroupSideBarSection } from '@app/entityV2/shared/sidebarSection/UserGroupSidebarSection';
import { UserOwnershipSidebarSection } from '@app/entityV2/shared/sidebarSection/UserOwnershipSideBarSection';
import { SidebarData, UserProfileInfoCard } from '@app/entityV2/user/UserProfileInfoCard';

import { useUpdateCorpUserPropertiesMutation } from '@graphql/user.generated';
import { OrreryRole } from '@types';

type Props = {
    sidebarData: SidebarData;
    refetch: () => void;
};

/**
 * UserSidebar- Sidebar section for users profiles.
 */
export default function UserSidebar({ sidebarData, refetch }: Props) {
    const { t } = useTranslation('entity.types');
    const { t: tf } = useTranslation('common.feedback');
    const { aboutText, groupsDetails, orreryRoles, urn, ownershipResults } = sidebarData;

    const [updateCorpUserPropertiesMutation] = useUpdateCorpUserPropertiesMutation();

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const me = useUserContext();
    const isProfileOwner = me?.user?.urn === urn;

    // About Text save
    const onSaveAboutMe = (inputString) => {
        updateCorpUserPropertiesMutation({
            variables: {
                urn: urn || '',
                input: {
                    aboutMe: inputString,
                },
            },
        })
            .then(() => {
                toast.success(tf('changesSaved'), { duration: 3 });
                refetch();
            })
            .catch((e) => {
                toast.destroy();
                toast.error(t('shared.saveChangesError', { error: e.message || '' }), { duration: 3 });
            });
    };

    const orreryRoleName =
        orreryRoles && orreryRoles.length > 0 ? (orreryRoles[0]?.entity as OrreryRole).name : '';

    return (
        <SideBar>
            <UserProfileInfoCard
                sidebarData={sidebarData}
                refetch={refetch}
                orreryRoleName={orreryRoleName}
                isProfileOwner={isProfileOwner}
            />
            <Content>
                <AboutSidebarSection
                    aboutText={aboutText || ''}
                    isProfileOwner={isProfileOwner}
                    onSaveAboutMe={onSaveAboutMe}
                />
                <UserOwnershipSidebarSection ownershipResults={ownershipResults} />
                <UserGroupSideBarSection groupsDetails={groupsDetails} />
            </Content>
        </SideBar>
    );
}
