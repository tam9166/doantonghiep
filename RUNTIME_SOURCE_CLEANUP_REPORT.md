# Runtime Source Cleanup Report

Date: 2026-09-02

## 1. Canonical root

- Project root: `E:\DoAnTotNghiep`
- Backend: `E:\DoAnTotNghiep\quanlynhahang`
- Frontend: `E:\DoAnTotNghiep\Frontend\nha-hang-frontend`
- Branch: `appmod/java-upgrade-20260818181401`
- HEAD during verification: `080f79b fix: complete invoice inventory AI and admin UX`

## 2. Duplicate roots before cleanup

- `E:\DoAnTotNghiep\backup_20260819_161938`
- `E:\DoAnTotNghiep\backup_20260819_162005`

Each backup contained a complete old backend and frontend tree.

## 3. Backup moved and excluded

The backup folders were compared, then moved intact outside the active repository tree:

- `E:\DoAnTotNghiep_Archive\backup_20260819_161938`

The duplicate archive copy `backup_20260819_162005` was removed after source hashes
matched the retained backup. Generated archive folders such as `node_modules` and
`target` were removed from the retained backup. Active workspace exclusions still
ignore backup/build folders to prevent accidental Java import if a backup is copied
back later.

An empty leftover directory shell under `E:\DoAnTotNghiep\backup_20260819_161938`
contained 0 files and was removed after the archived copy was verified.

## 4. Main class count

- Before cleanup: 3 copies of `poly.edu.quanlynhahang.QuanlynhahangApplication`
- After cleanup: 1 active copy

Active copy:

`E:\DoAnTotNghiep\quanlynhahang\src\main\java\poly\edu\quanlynhahang\QuanlynhahangApplication.java`

## 5. VS Code workspace

Recommended workspace:

`E:\DoAnTotNghiep\MOC_VI.code-workspace`

It contains only:

- `Moc Vi Backend`: `E:\DoAnTotNghiep\quanlynhahang`
- `Moc Vi Frontend`: `E:\DoAnTotNghiep\Frontend\nha-hang-frontend`

Open this workspace, then run `Java: Clean Java Language Server Workspace` once to
clear stale Java diagnostics previously created from the backup folders.

## 6. Launch configuration

The active launch path is now explicit:

- launch name: `Run Mộc Vị`
- main class: `poly.edu.quanlynhahang.QuanlynhahangApplication`
- project name: `quanlynhahang`
- backend cwd: `E:\DoAnTotNghiep\quanlynhahang`
- env file: `E:\DoAnTotNghiep\quanlynhahang\.env`

The ambiguous launch entry with empty `projectName` was removed.

## 7. Tasks pipeline

The run pipeline is:

1. `Mộc Vị: Frontend Build`
2. `Mộc Vị: Backend Clean`
3. `Mộc Vị: Backend Run`

The same sequence is also available through `E:\DoAnTotNghiep\run-mocvi.ps1`.

## 8. Frontend build output path

Vite writes the production bundle to:

`E:\DoAnTotNghiep\quanlynhahang\src\main\resources\static`

## 9. Spring static path

Spring Boot serves static files from the backend classpath:

`classpath:/static/`

## 10. Target static path

Maven copies frontend static files to:

`E:\DoAnTotNghiep\quanlynhahang\target\classes\static`

## 11. PowerShell runtime path

The verified runtime command was executed from:

`E:\DoAnTotNghiep\quanlynhahang`

Command:

```powershell
.\mvnw.cmd spring-boot:run
```

Spring Boot started from:

`E:\DoAnTotNghiep\quanlynhahang\target\classes`

## 12. VS Code runtime path

VS Code should run the same backend cwd:

`E:\DoAnTotNghiep\quanlynhahang`

It should not import or run any archived backup path.

## 13. Database and profile comparison

- Active profile: `dev`
- SQL Server host: `localhost:1433`
- Database: `RestaurantDB`
- Flyway current version in runtime smoke: `098`

The VS Code and PowerShell paths both use the backend `.env` file and the same Spring
configuration chain.

## 14. SPA fallback fix

Added tested SPA deep links:

- `/admin/products`
- `/admin/expired-food`
- `/kitchen/inventory`

Missing API/static resources remain JSON 404 responses and are not converted into SPA
HTML.

## 15. Browser/runtime verification

Runtime HTTP smoke against `localhost:8080` passed:

- `/`: HTTP 200 SPA HTML
- `/admin`: HTTP 200 SPA HTML
- `/admin/products`: HTTP 200 SPA HTML
- `/staff-login`: HTTP 200 SPA HTML
- `/admin/expired-food`: HTTP 200 SPA HTML
- `/kitchen/inventory`: HTTP 200 SPA HTML
- `/images/products/goi-cuon-tom-thit.jpg`: HTTP 200 image/jpeg
- `/api/__missing_smoke__`: HTTP 401 anonymous blocked
- `/api/admin/products`: HTTP 401 anonymous blocked

## 16. Admin Product bundle and hash

Source and target static bundles match:

- AdminProduct JS: `AdminProduct-BiYUbH1t.js`
- SHA-256: `ADA3A0E0CAF06BDEDA68B50271B758BB0CC1D050D0A93E6573AB78B8A571AEBE`
- AdminProduct CSS: `AdminProduct-C-ILft0J.css`
- SHA-256: `2094C87513A9D91145788CF150C947A571987BD3D957CB33B6CA57D773187CA0`
- `index.html` source/target/HTTP SHA-256:
  `D722693083B5E13532C0EBF421E1FBB9554D8CC1533CD3E5BEC9C264CF3A8574`

## 17. Tests

Frontend:

- `npm run lint`: PASS
- `npm test`: PASS, 33 files, 125 tests
- `npm run build`: PASS

Backend:

- focused SPA/exception tests: PASS, 6 tests
- `.\mvnw.cmd clean test`: PASS, 469 tests, 0 failures, 0 errors
- `.\mvnw.cmd package`: PASS, 469 tests, 0 failures, 0 errors

## 18. Files changed

- `.vscode/settings.json`
- `.vscode/launch.json` local ignored file
- `.vscode/tasks.json` local ignored file
- `MOC_VI.code-workspace`
- `PROJECT_SOURCE_OF_TRUTH.md`
- `WORKSPACE_DUPLICATE_AND_JAVA_DIAGNOSTICS_REPORT.md`
- `RUNTIME_SOURCE_CLEANUP_REPORT.md`
- `run-mocvi.ps1`
- `docs/KNOWN_FIXED_BUGS.md`
- `quanlynhahang/src/main/java/poly/edu/quanlynhahang/config/SpaRouteRegistry.java`
- `quanlynhahang/src/main/java/poly/edu/quanlynhahang/config/GlobalExceptionHandler.java`
- `quanlynhahang/src/test/java/poly/edu/quanlynhahang/config/SpaRouteRegistryTest.java`
- `quanlynhahang/src/test/java/poly/edu/quanlynhahang/config/GlobalExceptionHandlerTest.java`

## 19. Things not touched

- No database reset.
- No Flyway repair.
- No Git reset, checkout, clean, amend, rebase, or force push.
- No commit or push was made for this cleanup task.
- The user-owned deleted Word file remains outside this work:
  `BAO_CAO_DO_AN_TOT_NGHIEP_QUAN_LY_NHA_HANG_MOC_VI.docx`
