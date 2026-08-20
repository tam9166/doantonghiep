# Release checklist

- [ ] Cleanly review `git status` and scoped diff; no secrets/logs/uploads/build output.
- [ ] Frontend lint, unit tests and production build pass.
- [ ] Backend tests and compile pass.
- [ ] Fresh Flyway migration and seed verified.
- [ ] P0/P1 audit has no unaddressed release blocker.
- [ ] IDOR, inventory race, payment retry and SPA deep-link tests pass.
- [ ] UI status semantics and responsive critical screens checked.
- [ ] Production environment has JWT, DB, CORS, payment, upload and proxy settings.
