# Orrery Generalized Metadata Store (GMS) Docker Image

[![orrery-gms docker](https://github.com/Dxlxz/orrery/workflows/orrery-gms%20docker/badge.svg)](https://github.com/Dxlxz/orrery/actions?query=workflow%3A%22orrery-gms+docker%22)

Refer to [Orrery GMS Service](../../metadata-service) to have a quick understanding of the architecture and
responsibility of this service for the Orrery.

## Other Database Platforms

While GMS defaults to using MySQL as its storage backend, it is possible to switch to any of the
[database platforms](https://ebean.io/docs/database/) supported by Ebean.

For example, use Docker Compose profiles under `docker/profiles/`:

```shell
# PostgreSQL instead of MySQL
./gradlew quickstartPgDebug

# Or directly with compose
cd docker/profiles && docker compose --profile quickstart-postgres up
```

See `docker/profiles/README.md` in the repository for all profile options.
