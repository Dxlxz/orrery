import { Image, Typography } from 'antd';
import React from 'react';
import { Trans, useTranslation } from 'react-i18next';
import styled, { useTheme } from 'styled-components';

import { Text } from '@components/components/Text';

import orreryLogo from '@images/orrerylogo.png';

const Section = styled.div`
    width: auto;
    margin-top: 40px;
    margin-left: 60px;
`;

const TitleText = styled(Typography.Text)`
    font-size: 18px;
    margin-left: 8px;
    color: ${(props) => props.theme.colors.textSecondary};
`;

const TitleSection = styled.div`
    display: flex;
    align-items: center;
    margin-bottom: 20px;
`;

const MessageSection = styled.div`
    margin-bottom: 20px;
`;

const DetailParagraph = styled(Text)`
    font-size: 14px;
`;

const ResourceList = styled.ul`
    font-size: 14px;
`;

const ResourceListItem = styled.li`
    margin-bottom: 4px;
`;

const resources = [
    {
        /* untranslated-text */
        label: 'Orrery Project',
        path: 'https://docs.orrery.dev',
        shouldOpenInNewTab: true,
        description: 'Orrery Project website',
    },
    {
        /* untranslated-text */
        label: 'Orrery Docs',
        path: 'https://docs.orrery.dev/docs',
        shouldOpenInNewTab: true,
    },
    {
        /* untranslated-text */
        label: 'Orrery GitHub',
        path: 'https://github.com/Dxlxz/orrery',
        shouldOpenInNewTab: true,
    },
];

export const ErrorSection = (): JSX.Element => {
    const { t } = useTranslation('shared.error');
    const themeConfig = useTheme();
    const themeLogo = themeConfig.assets.logoUrl || orreryLogo;

    return (
        <Section>
            <div>
                <TitleSection>
                    <Image src={themeLogo} preview={false} style={{ width: 40 }} />
                    <TitleText strong>{themeConfig.content.title}</TitleText>
                </TitleSection>
                <MessageSection>
                    <Typography.Title level={2}>{t('section.title')}</Typography.Title>
                    <DetailParagraph color="textSecondary">{t('section.description')}</DetailParagraph>
                </MessageSection>
                <div>
                    <DetailParagraph color="textSecondary">
                        <Trans t={t} i18nKey="section.needSupport" components={{ bold: <b /> }} />
                    </DetailParagraph>
                    <ResourceList>
                        {resources.map((resource) => (
                            <ResourceListItem key={resource.path}>
                                <a href={resource.path}>{resource.label}</a>
                            </ResourceListItem>
                        ))}
                    </ResourceList>
                </div>
            </div>
        </Section>
    );
};
