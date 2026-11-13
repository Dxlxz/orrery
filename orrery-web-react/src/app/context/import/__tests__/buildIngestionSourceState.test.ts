import { describe, expect, it } from 'vitest';

import { buildIngestionSourceState } from '@app/context/import/buildIngestionSourceState';

describe('buildIngestionSourceState', () => {
    it('parses recipe yaml into builder state', () => {
        const state = buildIngestionSourceState({
            sourceType: 'github-documents',
            displayName: 'GitHub',
            recipeYaml: `source:
  type: github-documents
  config:
    repository: acme/docs
sink:
  type: orrery-rest
  config:
    server: "\${ORRERY_GMS_URL}"
`,
        });

        expect(state.type).toBe('github-documents');
        expect(state.name).toBe('GitHub');
        const parsedRecipe = JSON.parse(state.config!.recipe!);
        expect(parsedRecipe.source).toEqual({
            type: 'github-documents',
            config: {
                repository: 'acme/docs',
            },
        });
        expect(parsedRecipe.sink.type).toBe('orrery-rest');
        expect(parsedRecipe.sink.config.server).toContain('ORRERY_GMS_URL');
    });
});
