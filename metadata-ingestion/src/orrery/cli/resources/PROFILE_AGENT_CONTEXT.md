# Orrery profile agent context

## Orrery CLI connection profiles

- Never run `orrery init` without `--profile <name>`. Name the profile after the instance (`acme`, `acme-staging`).
- Never run `orrery profile use`; it changes the default for the user's other shells.
- Pass `--profile <name>` on every command, unless the session already sets `ORRERY_PROFILE`.
- Before any write (`put`, `delete`, `ingest`, upserts), run `orrery profile current --format json` and confirm the server is the one the user asked for.
- If a token has expired, re-run `orrery init --profile <same name>` with the same host. Do not create a new profile.

## Select and verify

Selection order: `--profile`, `ORRERY_PROFILE`, existing GMS connection environment variables,
`~/.orrery/current-profile`, then `~/.orreryenv` (`default`). The global flag works before or after
the subcommand. Unknown profiles stop the command. An explicit profile overrides a conflicting GMS
URL with a warning, and cannot be combined with `ORRERY_SKIP_CONFIG=true` (`ORRERY_PROFILE` is ignored under it).

```bash
orrery profile list --format json
orrery --profile acme profile current --format json
ORRERY_PROFILE=acme orrery profile current --format json
```

`current` returns `name`, `server`, `source`, and `token_expires` (JWT expiry in epoch seconds, or null).
Confirm the server before writing. Writes announce a named target on stderr. For `orrery ingest run`, the recipe's
sink decides the destination: omit the sink to write to the selected profile. `--profile` with a
recipe sink on another server is refused. Discovery output never
includes tokens.

## Maintain profiles

`orrery profile save acme` copies the stored default connection, ignoring environment credentials. It does not copy an OAuth refresh token.
`--from local` copies another profile, and `--force` explicitly replaces an existing destination.
Use `profile show acme` to inspect authentication mode and expiry, `profile rename acme acme-staging`
to rename a profile, and `profile doctor` to diagnose expired tokens, legacy keys, permissions, and
stray files. Doctor changes nothing without `--fix` and never deletes non-config files.

`profile use` changes the machine default and requires `--yes` without a TTY. Agents must not use it.
`profile use --shell <name>` only prints `export ORRERY_PROFILE=<name>` and changes nothing; prefer
setting `ORRERY_PROFILE` or passing `--profile` directly.
`profile remove` deletes a profile directory (resetting to `default` if it was active) and requires `--yes` without a TTY. The reserved `default`
profile cannot be removed or renamed.
