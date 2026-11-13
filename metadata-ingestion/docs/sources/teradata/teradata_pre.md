### Overview

The `teradata` module ingests metadata from Teradata into Orrery. It is intended for production ingestion workflows and module-specific capabilities are documented below.

This plugin extracts the following:

- Metadata for databases, schemas, views, and tables
- Column types associated with each table
- Table, row, and column statistics via optional SQL profiling

### Prerequisites

1. Create a user which has access to the database you want to ingest.
   ```sql
   CREATE USER orrery FROM <database> AS PASSWORD = <password> PERM = 20000000;
   ```
2. Create a user with the following privileges:

   ```sql
   GRANT SELECT ON dbc.columns TO orrery;
   GRANT SELECT ON dbc.databases TO orrery;
   GRANT SELECT ON dbc.tables TO orrery;
   GRANT SELECT ON DBC.All_RI_ChildrenV TO orrery;
   GRANT SELECT ON DBC.ColumnsV TO orrery;
   GRANT SELECT ON DBC.IndicesV TO orrery;
   GRANT SELECT ON dbc.TableTextV TO orrery;
   GRANT SELECT ON dbc.TablesV TO orrery;
   GRANT SELECT ON dbc.dbqlogtbl TO orrery; -- if lineage or usage extraction is enabled
   ```

   If you want to run profiling, you need to grant select permission on all the tables you want to profile.

3. **For lineage/usage extraction**: Enable query logging and set an appropriate query text size (default is 200 chars, may be insufficient).

   To set for all users:

   ```sql
   REPLACE QUERY LOGGING LIMIT SQLTEXT=2000 ON ALL;
   ```

   See more here about query logging:
   [https://docs.teradata.com/r/Lake-Database-Reference/Database-Administration/Tracking-Query-Behavior-with-Database-Query-Logging-Operational-DBAs/SQL-Statements-to-Control-Logging/LIMIT-Logging-Options](https://docs.teradata.com/r/Lake-Database-Reference/Database-Administration/Tracking-Query-Behavior-with-Database-Query-Logging-Operational-DBAs/SQL-Statements-to-Control-Logging/LIMIT-Logging-Options)
