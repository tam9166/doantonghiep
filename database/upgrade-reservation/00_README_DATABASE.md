# Reservation Upgrade Database Package

Target database: `RestaurantDB` on SQL Server `localhost,1433`.

Run order:

1. `01_backup_database.sql`
2. `02_upgrade_schema.sql`
3. `03_indexes_constraints.sql`
4. `04_seed_sample_data.sql`
5. `05_verify_database.sql`

Rollback script: `06_rollback_schema.sql`.

The upgrade scripts are additive. They do not drop existing business tables and are written to preserve current data.
