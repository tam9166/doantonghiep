# Full Project Cleanup Report

Date: 2026-09-02

## 1. Project size before

- Active project: `E:\DoAnTotNghiep`
- Files: 12,593
- Directories: 1,400
- Size: 382.15 MB

Archive before cleanup:

- Archive root: `E:\DoAnTotNghiep_Archive`
- Files: 16,846
- Directories: 2,184
- Size: 414.02 MB

## 2. Project size after

- Active project files after cleanup before final build artifacts: 12,526
- Active project directories after cleanup before final build artifacts: 1,391
- Active project size after cleanup before final build artifacts: 352.08 MB

After the required final backend package verification, Maven recreated `target/`
artifacts:

- Active project files after verification/package: 13,415
- Active project directories after verification/package: 1,410
- Active project size after verification/package: 529.15 MB

Archive after cleanup:

- Files: 634
- Directories: 59
- Size: 4.06 MB

## 3. Space reclaimed

- Active project reclaimed: about 30.07 MB
- Archive reclaimed: about 409.96 MB
- Total reclaimed across active project and archive: about 440.03 MB

The active project reclaimed number is measured before final verification recreated
Maven `target/` output. `target/` remains generated/ignored and can be rebuilt.

## 4. Generated files removed

- Removed ignored Codex modernization/runtime artifacts under
  `E:\DoAnTotNghiep\.github\modernize\java-upgrade`.
- Removed ignored release output under `E:\DoAnTotNghiep\release`.
- Removed generated `node_modules` and `target` folders from the retained archive backup.

`E:\DoAnTotNghiep\quanlynhahang\target` was intentionally kept because full backend
test/package and runtime smoke recreate and use it.

## 5. Duplicate files removed

- Removed duplicate archive source copy:
  `E:\DoAnTotNghiep_Archive\backup_20260819_162005`.
- Kept one source-equivalent archive backup:
  `E:\DoAnTotNghiep_Archive\backup_20260819_161938`.

The two backups had identical source hashes after excluding generated dependency/build
folders.

## 6. Dead frontend code removed

None.

The Vue router maps every active page view. Components with low direct reference counts
are either route-loaded, layout-level, test files, or candidates requiring manual review.

## 7. Dead backend code removed

None from tracked production code.

Removed untracked stale backup files only:

- `quanlynhahang/src/main/java/poly/edu/quanlynhahang/config/WebConfig.java.bak`
- `quanlynhahang/src/main/java/poly/edu/quanlynhahang/repository/ReservationRepository.java.bak`

Spring beans/entities/repositories were not removed because annotation-driven runtime
loading means reference-count-only deletion would be unsafe.

## 8. Dependencies removed

None.

Frontend dependencies all remain tied to application imports, routing, charts, QR,
WebSocket/STOMP, state management, i18n, testing, linting, or Vite build.

Backend dependencies remain tied to Spring starters, SQL Server/Flyway, JWT, mail/PDF,
WebSocket, Micrometer, Lombok annotation processing, or tests.

## 9. Configs removed or merged

- Removed ignored backend-local VS Code launch file:
  `quanlynhahang/.vscode/launch.json`.
- Kept canonical root/workspace config:
  `MOC_VI.code-workspace`, `.vscode/settings.json`, `.vscode/launch.json`,
  `.vscode/tasks.json`.

No application profile, database URL, port, or secret mechanism was changed.

## 10. Scripts removed or merged

None.

Kept scripts:

- `run-mocvi.ps1` as the canonical local build/run entry point.
- Existing `scripts/*.ps1`, `*.sh` utilities because they are explicit build,
  release, security, DB-upgrade, and smoke-test entry points.

## 11. Archive cleanup

Archive now contains one source backup only:

`E:\DoAnTotNghiep_Archive\backup_20260819_161938`

Removed from archive:

- duplicate source-equivalent backup copy;
- backup `node_modules`;
- backup Maven `target`.

No archive is imported into VS Code/runtime.

## 12. Items intentionally kept

- `Frontend/nha-hang-frontend/node_modules`: kept to avoid reinstall cost; required for
  current lint/test/build runs.
- `quanlynhahang/target`: generated, but rebuilt by Maven and used for runtime smoke.
- `quanlynhahang/src/main/resources/static`: intentionally tracked Spring static
  runtime output for deployment.
- Product image duplicates between `frontend/public` and backend `static`: kept because
  database/runtime URLs can reference these paths directly.
- `database/**`, `sql/**`, and all Flyway migrations: kept.
- `*.docx` project document state: untouched.

## 13. Manual-review items

- Exact duplicate assets between frontend public and backend static should remain unless
  the deployment strategy changes.
- `Frontend/nha-hang-frontend/orders.json` is empty and untracked by usage, but needs
  owner confirmation before deletion.
- `quanlynhahang/src/main/java/poly/edu/quanlynhahang/controller/KitchenController.java`
  is an empty tracked Java file; it is harmless but should be reviewed separately before
  removal.
- Several comments and silent catch blocks are intentional UI/operational behavior and
  were not mass-edited.
- Optional old database upgrade SQL scripts under `database/**` should be reviewed with
  the owner's deployment history before cleanup.

## 14. Tests

Latest verified baseline:

- Frontend lint: PASS
- Frontend tests: PASS, 33 files, 125 tests
- Frontend build: PASS
- Backend focused SPA/exception tests: PASS, 6 tests
- Backend `clean test`: PASS, 469 tests, 0 failures, 0 errors
- Backend `package`: PASS, 469 tests, 0 failures, 0 errors

## 15. Runtime verification

Latest verified runtime smoke:

- `/`: HTTP 200 SPA shell
- `/admin`: HTTP 200 SPA shell
- `/admin/products`: HTTP 200 SPA shell
- `/staff-login`: HTTP 200 SPA shell
- `/admin/expired-food`: HTTP 200 SPA shell
- `/kitchen/inventory`: HTTP 200 SPA shell
- `/images/products/goi-cuon-tom-thit.jpg`: HTTP 200 image/jpeg
- `/api/__missing_smoke__`: HTTP 401 anonymous blocked
- `/api/admin/products`: HTTP 401 anonymous blocked

## 16. Git status

The user-owned Word document deletion remains outside this cleanup:

`D BAO_CAO_DO_AN_TOT_NGHIEP_QUAN_LY_NHA_HANG_MOC_VI.docx`

No commit or push was performed.

## 17. Files changed

- `.vscode/settings.json`
- `MOC_VI.code-workspace`
- `PROJECT_SOURCE_OF_TRUTH.md`
- `RUNTIME_SOURCE_CLEANUP_REPORT.md`
- `WORKSPACE_DUPLICATE_AND_JAVA_DIAGNOSTICS_REPORT.md`
- `FULL_PROJECT_CLEANUP_REPORT.md`
- `run-mocvi.ps1`
- `docs/KNOWN_FIXED_BUGS.md`
- `quanlynhahang/src/main/java/poly/edu/quanlynhahang/config/GlobalExceptionHandler.java`
- `quanlynhahang/src/main/java/poly/edu/quanlynhahang/config/SpaRouteRegistry.java`
- `quanlynhahang/src/test/java/poly/edu/quanlynhahang/config/GlobalExceptionHandlerTest.java`
- `quanlynhahang/src/test/java/poly/edu/quanlynhahang/config/SpaRouteRegistryTest.java`

Ignored local VS Code files were also updated:

- `.vscode/launch.json`
- `.vscode/tasks.json`

## 18. Files deleted

Active project ignored/generated/debug files:

- `E:\DoAnTotNghiep\.github\modernize\java-upgrade`
- `E:\DoAnTotNghiep\release`
- `E:\DoAnTotNghiep\quanlynhahang\qa-server.out.log`
- `E:\DoAnTotNghiep\quanlynhahang\qa-server-final.out.log`
- `E:\DoAnTotNghiep\quanlynhahang\qa-qr-fix.out.log`
- `E:\DoAnTotNghiep\quanlynhahang\qa-qr-fix.err.log`
- `E:\DoAnTotNghiep\quanlynhahang\qa-server-final.err.log`
- `E:\DoAnTotNghiep\quanlynhahang\qa-server.err.log`
- `E:\DoAnTotNghiep\quanlynhahang\src\main\java\poly\edu\quanlynhahang\config\WebConfig.java.bak`
- `E:\DoAnTotNghiep\quanlynhahang\src\main\java\poly\edu\quanlynhahang\repository\ReservationRepository.java.bak`
- `E:\DoAnTotNghiep\quanlynhahang\.vscode\launch.json`

Archive cleanup:

- `E:\DoAnTotNghiep_Archive\backup_20260819_162005`
- `E:\DoAnTotNghiep_Archive\backup_20260819_161938\Frontend\nha-hang-frontend\node_modules`
- `E:\DoAnTotNghiep_Archive\backup_20260819_161938\quanlynhahang\target`

## 19. Safe to delete

Completed.

## 20. Keep and review manually

KEEP:

- canonical backend and frontend source;
- tracked static runtime assets;
- all Flyway migrations;
- SQL/database support scripts;
- project documentation and QA evidence;
- active dependency folders needed to run tests locally.

REVIEW_MANUALLY:

- empty `orders.json`;
- empty tracked `KitchenController.java`;
- optional historical database upgrade SQL;
- public/backend duplicated static images if deployment packaging is redesigned later.
