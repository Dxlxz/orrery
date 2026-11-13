import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { getRolePolicies, getRoleUsers } from '@app/permissions/roles/roles.utils';

import { OrreryRole } from '@types';

function roleWithPolicyEntities(entities: Array<{ urn: string; name: string } | null>): OrreryRole {
    return {
        policies: {
            relationships: entities.map((entity) => ({ entity })),
        },
    } as unknown as OrreryRole;
}

function roleWithUserEntities(entities: Array<{ urn: string } | null>): OrreryRole {
    return {
        users: {
            relationships: entities.map((entity) => ({ entity })),
        },
    } as unknown as OrreryRole;
}

describe('getRolePolicies', () => {
    let warnSpy: ReturnType<typeof vi.spyOn>;

    beforeEach(() => {
        warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    });

    afterEach(() => {
        warnSpy.mockRestore();
    });

    it('returns the policy entities for a role', () => {
        const role = roleWithPolicyEntities([
            { urn: 'urn:li:orreryPolicy:admin-platform-policy', name: 'Platform' },
            { urn: 'urn:li:orreryPolicy:admin-metadata-policy', name: 'Metadata' },
        ]);

        const policies = getRolePolicies(role);

        expect(policies.map((p) => p.urn)).toEqual([
            'urn:li:orreryPolicy:admin-platform-policy',
            'urn:li:orreryPolicy:admin-metadata-policy',
        ]);
        expect(warnSpy).not.toHaveBeenCalled();
    });

    it('drops null entities (non-policy edges) and warns', () => {
        const role = roleWithPolicyEntities([
            { urn: 'urn:li:orreryPolicy:admin-platform-policy', name: 'Platform' },
            null,
            { urn: 'urn:li:orreryPolicy:admin-metadata-policy', name: 'Metadata' },
        ]);

        const policies = getRolePolicies(role);

        expect(policies.map((p) => p.urn)).toEqual([
            'urn:li:orreryPolicy:admin-platform-policy',
            'urn:li:orreryPolicy:admin-metadata-policy',
        ]);
        expect(warnSpy).toHaveBeenCalledTimes(1);
    });

    it('drops every edge when all policy entities are null', () => {
        const role = roleWithPolicyEntities([null, null]);

        expect(getRolePolicies(role)).toEqual([]);
        expect(warnSpy).toHaveBeenCalledTimes(1);
    });

    it('returns an empty array when the role has no policies relationship', () => {
        expect(getRolePolicies({} as OrreryRole)).toEqual([]);
        expect(getRolePolicies(undefined)).toEqual([]);
        expect(warnSpy).not.toHaveBeenCalled();
    });
});

describe('getRoleUsers', () => {
    let warnSpy: ReturnType<typeof vi.spyOn>;

    beforeEach(() => {
        warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    });

    afterEach(() => {
        warnSpy.mockRestore();
    });

    it('returns the member entities for a role', () => {
        const role = roleWithUserEntities([{ urn: 'urn:li:corpuser:alice' }, { urn: 'urn:li:corpGroup:admins' }]);

        expect(getRoleUsers(role).map((u) => u.urn)).toEqual(['urn:li:corpuser:alice', 'urn:li:corpGroup:admins']);
        expect(warnSpy).not.toHaveBeenCalled();
    });

    it('drops null member entities and warns', () => {
        const role = roleWithUserEntities([{ urn: 'urn:li:corpuser:alice' }, null]);

        expect(getRoleUsers(role).map((u) => u.urn)).toEqual(['urn:li:corpuser:alice']);
        expect(warnSpy).toHaveBeenCalledTimes(1);
    });

    it('returns an empty array when the role has no users relationship', () => {
        expect(getRoleUsers({} as OrreryRole)).toEqual([]);
        expect(getRoleUsers(undefined)).toEqual([]);
        expect(warnSpy).not.toHaveBeenCalled();
    });
});
