# Technical debt

1. Replace localStorage access-token architecture only with a full refresh-token/CSRF/revocation design.
2. Migrate native browser dialogs and scattered direct Axios calls gradually through shared UI/API services.
3. Replace deprecated Spring MVC configuration APIs.
4. Add DB-backed integration coverage for order, inventory, payment, reservation, staff and migration flows.
5. Remove or ignore local runtime artifacts only after repository owner approves their disposal.
