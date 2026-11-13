---
title: "Docker Container Architecture"
---

# Docker Container Architecture

When running Orrery via docker-compose. or helm, the following is a diagram of the containers involved
with running Orrery and their relationships with each other. The helm chart uses helm hooks to determine
the proper ordering of the components whereas docker-compose relies on a series of health checks.

```text
                orrery-frontend-react  orrery-actions
                                     \   /
                                       |   orrery-upgrade (NoCodeDataMigration, helm only)
                                       |   /
                                orrery-gms (healthy)
                                       |
                                orrery-upgrade (SystemUpdate completed)
            /--------------------/   |      \------------------------------------------------\
           /                         |                                                         \
    mysql (healthy)         elasticsearch (healthy)  kafka-setup (completed)      (if apply) neo4j (healthy)
```
