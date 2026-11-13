// Kept out of OrreryMentionsExtension so read-only markdown rendering can recognize
// mention spans without loading Remirror.
export const ORRERY_MENTION_ATTRS = {
    urn: 'data-orrery-mention-urn',
    /** Stable label for markdown serialization — node textContent can include icon glyphs. */
    name: 'data-orrery-mention-name',
} as const;
