import FilterRendererRegistry from '@app/searchV2/filters/render/FilterRendererRegistry';
import { renderers as orreryRenderers } from '@app/searchV2/filters/render/orreryRenderers';

/**
 * Configure the render registry.
 */
const RENDERERS = [...orreryRenderers];
const REGISTRY = new FilterRendererRegistry();
RENDERERS.forEach((renderer) => REGISTRY.register(renderer));

/**
 * Used to render custom filter views for a particular facet field.
 */
export const useFilterRendererRegistry = () => {
    return REGISTRY;
};
