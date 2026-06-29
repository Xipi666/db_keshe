/*
  PSM-Smart Oracle 21c initialization entrypoint.

  Run this script from SQL*Plus / SQLcl / compatible database tools in the
  Core/src/main/resources/db directory.

  Script layout:
  - oracle21c-schema.sql: database framework, including tables, constraints,
    comments, sequences, and indexes.
  - oracle21c-base-data.sql: deployment baseline data, including users,
    box transformer metadata, circuits, measure points, and thresholds.
  - oracle21c-mock-data.sql: demo/mock runtime data, including second-level
    samples, alarms, maintenance tasks, and cabinet door logs.
*/

@@oracle21c-schema.sql
@@oracle21c-base-data.sql
@@oracle21c-mock-data.sql
@@oracle21c-business.sql
