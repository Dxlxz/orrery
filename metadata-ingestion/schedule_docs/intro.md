---
description: "Overview of options for scheduling Orrery metadata ingestion recipes, including Airflow, Kubernetes CronJobs, and crontab."
---

# Introduction to Scheduling Metadata Ingestion

Given a recipe file `/home/ubuntu/orrery_ingest/mysql_to_orrery.yml`.

```
source:
  type: mysql
  config:
    # Coordinates
    host_port: localhost:3306
    database: dbname

    # Credentials
    username: root
    password: example

sink:
 type: orrery-rest
 config:
  server: http://localhost:8080
```

We can do ingestion of our metadata using [Orrery CLI](../../docs/cli.md) as follows

```
orrery ingest -c /home/ubuntu/orrery_ingest/mysql_to_orrery.yml
```

This will ingest metadata from the `mysql` source which is configured in the recipe file. This does ingestion once. As the source system changes we would like to have the changes reflected in Orrery. To do this someone will need to re-run the ingestion command using a recipe file.

An alternate to running the command manually we can schedule the ingestion to run on a regular basis. In this section we give some examples of how scheduling ingestion of metadata into Orrery can be done.
