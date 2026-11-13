# Orrery init agent context

Always pass `--profile <name>` when initializing a connection.

## Orrery CLI connection profiles

- Never run `orrery init` without `--profile <name>`. Name the profile after the instance (`acme`, `acme-staging`).
- Never run `orrery profile use`; it changes the default for the user's other shells.
- Pass `--profile <name>` on every command, unless the session already sets `ORRERY_PROFILE`.
- Before any write (`put`, `delete`, `ingest`, upserts), run `orrery profile current --format json` and confirm the server is the one the user asked for.
- If a token has expired, re-run `orrery init --profile <same name>` with the same host. Do not create a new profile.

## Initialize and inspect a connection

```bash
orrery init --profile local --host http://localhost:8080 --username orrery --password orrery
orrery --profile local profile current --format json

orrery init --profile acme --host https://acme.example.com/gms --token "<token>"
orrery init --profile acme --oauth --host https://acme.example.com/gms
orrery init --profile acme --sso --host https://acme.example.com/gms
```

Initialization writes `~/.orrery/profiles/<name>/.orreryenv`. It never changes the active profile.
All existing authentication modes work with profiles. `--oauth` and `--sso` each exclude token and
username/password flags. SSO requires the SSO package extra and a Playwright Chromium installation.
Support login uses `--support` with `--sso` or `--oauth`. OAuth support login also requires `--ticket-id`.

## Defaults and guardrails

| Situation                                   | Behavior                                                        |
| ------------------------------------------- | --------------------------------------------------------------- |
| Host omitted with credentials               | Uses `http://localhost:8080`                                    |
| Token duration omitted for localhost        | `ONE_MONTH`                                                     |
| Token duration omitted for a remote host    | `ONE_HOUR`                                                      |
| Existing profile, non-TTY, same server      | Refreshes credentials without prompting                         |
| Existing profile, non-TTY, different server | Refuses, prints a separate-profile command. `--force` overrides |
| Existing profile, TTY                       | Prompts for overwrite confirmation                              |

Pass the same host when refreshing a remote profile. CLI arguments override the existing init input
variables `ORRERY_GMS_URL`, `ORRERY_GMS_TOKEN`, `ORRERY_USERNAME`, and `ORRERY_PASSWORD`.
Connection environment variables are inputs to init, not its destination. Without an explicit name,
init writes the profile selected by `ORRERY_PROFILE`, then `current-profile`, then `default`.

Durations: `ONE_HOUR`, `ONE_DAY`, `ONE_WEEK`, `ONE_MONTH`, `THREE_MONTHS`, `SIX_MONTHS`,
`ONE_YEAR`, `NO_EXPIRY`. Server configuration determines which durations it allows. OAuth manages
access token lifetime and refresh automatically.
