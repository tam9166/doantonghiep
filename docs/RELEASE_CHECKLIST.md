# Release checklist

- [x] Reviewed `git status`/diff; secret scan passes and release manifest excludes `.env`, logs, uploads, dependency/build output. The pre-existing deleted report DOCX remains intentionally uncommitted pending owner confirmation.
- [x] Frontend clean install/audit, lint, 61 unit tests and production build pass.
- [x] Backend clean package passes with 415 tests, 0 failures/errors/skips.
- [x] Fresh V001-V059 and legacy V045-V059 Flyway migrations pass against SQL Server.
- [x] Table release concurrency and payment-callback-versus-release SQL Server integration tests pass.
- [x] Final reviewed P0/P1 source scope has no known unresolved defect; production CAPTCHA and redundant waiter transfer mutations were corrected.
- [x] Existing IDOR, inventory race, reservation race/idempotency, payment retry and SPA deep-link regression suites pass.
- [x] All six demo roles authenticate through the correct login gate; 11 protected staff/admin screens pass packaged-app desktop/tablet/mobile route, console, request and horizontal-overflow smoke checks.
- [ ] Critical state-mutating browser E2E (reservation handoff, order/kitchen/cashier, cancellation/refund and merge/split/transfer/release) still needs isolated disposable fixtures/provider sandbox; backend workflow/concurrency regressions pass.
- [ ] Real production environment is not verified. JWT, DB, CORS, payment, CAPTCHA, mail, upload and AI configuration are externalized and production startup is fail-fast for JWT/payment/CAPTCHA secrets.
- [x] Release ZIP contains no `.env`, `.git`, `node_modules`, `target`, backup, log or nested ZIP entries.
- [x] Release-manifest secret scan passes for tracked and untracked files selected for packaging.
- [x] Maven and shell wrappers retain Git executable mode; build scripts use `npm ci`.
- [x] Verified release commit pushed to `tam9166/doantonghiep` `main`; pre-existing local report-DOCX deletion was excluded from the commit.
