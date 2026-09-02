# Workspace Duplicate and Java Diagnostics Report

Date: 2026-09-02

## 1. Workspace root before cleanup

The opened root was `E:\DoAnTotNghiep`. It contains the canonical project and two
complete project backups, so opening the root without exclusions allowed the Java
language server to discover all three Maven applications.

## 2. Duplicate project roots before cleanup

- `E:\DoAnTotNghiep\backup_20260819_161938`
- `E:\DoAnTotNghiep\backup_20260819_162005`

Each backup contains a complete `quanlynhahang` Maven backend and
`Frontend\nha-hang-frontend` Vue application. Neither backup contains a Git repository,
and `backup_*/` is already ignored by the canonical repository.

Both backup trees were compared against the canonical source before cleanup and then
moved out of the active root. They were also compared to each other after excluding
generated dependency/build folders.

- `E:\DoAnTotNghiep_Archive\backup_20260819_161938`

`backup_20260819_161938` is retained as the single archive source copy. The duplicate
`backup_20260819_162005` copy was removed from the archive after source hashes matched.
Generated archive folders such as `node_modules` and `target` were removed from the
retained backup. An empty leftover backup directory shell with 0 files was removed from
the active root after the archived copy was verified.

## 3. All discovered main classes

Before cleanup, three filesystem copies of
`poly.edu.quanlynhahang.QuanlynhahangApplication` were found:

1. `E:\DoAnTotNghiep\quanlynhahang\src\main\java\poly\edu\quanlynhahang\QuanlynhahangApplication.java`
2. `E:\DoAnTotNghiep\backup_20260819_161938\quanlynhahang\src\main\java\poly\edu\quanlynhahang\QuanlynhahangApplication.java`
3. `E:\DoAnTotNghiep\backup_20260819_162005\quanlynhahang\src\main\java\poly\edu\quanlynhahang\QuanlynhahangApplication.java`

After cleanup, the active project tree contains only item 1.

## 4. Canonical source

- Git/project root: `E:\DoAnTotNghiep`
- Backend: `E:\DoAnTotNghiep\quanlynhahang`
- Frontend: `E:\DoAnTotNghiep\Frontend\nha-hang-frontend`
- Branch at verification: `appmod/java-upgrade-20260818181401`
- HEAD at verification: `080f79b fix: complete invoice inventory AI and admin UX`

This is the only root with `.git`, the current migrations, tests, product-image work,
and the latest Admin Product pagination implementation.

## 5. Backup source

One `backup_20260819_*` source tree is retained as owner data under
`E:\DoAnTotNghiep_Archive`. It was not edited, built, staged, or added to Git. It must
not be used as a runtime or edit target unless the owner explicitly requests it.

## 6. Cause of the duplicate main class

VS Code/JDT imported three complete Maven projects from the broad workspace root. All
three declare the same fully qualified main class. The old Java launch configuration
also included an ambiguous empty `projectName`, so the debugger could not choose one
application reliably.

## 7. Duplicate exclusion and fix

- `.vscode/settings.json` now uses the Java extension's supported
  `java.import.exclusions` setting for `backup_*`, `node_modules`, and `target`.
- Backup trees are also excluded from file display, search, and file watching.
- `MOC_VI.code-workspace` contains only the canonical backend and frontend folders.
- The recommended launch selects `projectName: quanlynhahang` explicitly.
- The ambiguous empty-project launch entry was removed from the local launch file.

After opening `E:\DoAnTotNghiep\MOC_VI.code-workspace`, run
`Java: Clean Java Language Server Workspace` once and allow VS Code to reload. This
clears any diagnostic cache created before the exclusions. A scan of the recommended
workspace resolves exactly one application main class.

## 8. Source served by localhost:8080

Vite builds the canonical frontend into
`E:\DoAnTotNghiep\quanlynhahang\src\main\resources\static`. Maven copies those files to
`E:\DoAnTotNghiep\quanlynhahang\target\classes\static`, and the packaged canonical JAR
serves that classpath directory.

A smoke run of the canonical Spring Boot application confirmed:

- process source: `E:\DoAnTotNghiep\quanlynhahang\target\classes`;
- `/`, `/admin`, `/admin/products`, `/staff-login`, `/admin/expired-food`, and
  `/kitchen/inventory` returned the built SPA shell with HTTP 200;
- the served Admin Product chunk contained the current pagination text and page size 10;
- the served root HTML matched `target\classes\static\index.html` exactly.

Direct refresh for `/admin/products`, `/admin/expired-food`, and `/kitchen/inventory`
now returns the SPA shell with HTTP 200. Anonymous API access remains blocked with 401
and is not converted into SPA HTML.

## 9. Root cause of the apparent Admin Product UI regression

The canonical `AdminProduct.vue` still has the wider table and 10-item pagination.
Commit `38e1d3a` is the latest commit that introduced that pagination. Commit `080f79b`
did not revert the component; it only contains rebuilt static output for this area.

The two backups contain older Admin Product components and old UI text. Therefore the
observed old UI could be produced by either importing/launching a backup or serving an
old `target\classes\static` copy after editing Vue source without rebuilding and
restarting Maven/Spring Boot. No service worker or PWA cache was found. There is no Git
evidence that the canonical component was restored to the old version.

## 10. Actual Java errors after source cleanup

No Java compilation or test error remains in the canonical project. The reported
private-field access errors were emitted from backup copies of
`RestaurantBusinessHoursServiceTest`, not from the canonical test source.

Toolchain versions are aligned on Java 21:

- `java`: 21.0.8
- `javac`: 21.0.8
- Maven wrapper: 3.9.15 on Java 21.0.8
- Maven target (`java.version`): 21
- VS Code Java language server runtime: bundled Java 21.0.11

## 11. RestaurantBusinessHoursServiceTest resolution

Production fields remain private. The canonical test already configures
`openingTime`, `closingTime`, and `lastOrderTime` with
`ReflectionTestUtils.setField(...)`. No production visibility or test change was
required. The direct assignments that caused the severity-8 diagnostics exist only in
the excluded backup trees.

Focused result: 11 tests run, 0 failures, 0 errors, 0 skipped.

## 12. Maven test result

Command: `./mvnw.cmd clean test`

- Tests run: 469
- Failures: 0
- Errors: 0
- Skipped: 0
- Result: BUILD SUCCESS

## 13. Maven package result

Command: `./mvnw.cmd clean package`

- Tests run: 469
- Failures: 0
- Errors: 0
- Skipped: 0
- Result: BUILD SUCCESS
- Artifact: `target\quanlynhahang-0.0.1-SNAPSHOT.jar`

Frontend verification also passed:

- lint: PASS
- tests: 33 files, 125 tests PASS
- build: PASS (285 modules)

## 14. Warning groups intentionally not changed

- Field-injection recommendations across controllers/services.
- Unnecessary `@Repository` suggestions on Spring Data interfaces.
- Deprecated JPA `@Temporal`/`TemporalType` and legacy date models.
- Deprecated Spring MVC/Jackson APIs in `WebConfig`.
- Raw/unchecked map warnings and isolated unused-import/unused-member suggestions.
- Framework upgrade suggestions.

These warnings are not current build blockers and were not mass-refactored.

## 15. Recommended technical debt

1. Plan Java-time/JPA migration with schema, serialization, timezone, and Flyway tests.
2. Migrate deprecated Spring MVC converter/path APIs in a focused compatibility change.
3. Convert dependency injection incrementally only when touching affected classes.
4. Investigate the scheduled reservation expiry transaction rollback observed during
   runtime smoke in a separate business-flow task.

## 16. Files changed

- `.vscode/settings.json` — excludes backup/build trees from Java import and workspace scanning.
- `.vscode/launch.json` — local launch now selects only the canonical Maven project
  (the repository ignores this local file).
- `.vscode/tasks.json` — local-only pipeline for frontend build, backend clean, and
  backend run (the repository ignores this local file).
- `MOC_VI.code-workspace` — canonical two-folder workspace and unambiguous Java launch.
- `run-mocvi.ps1` — canonical PowerShell build/run entry point.
- `PROJECT_SOURCE_OF_TRUTH.md` — canonical paths and build/run operating rules.
- `WORKSPACE_DUPLICATE_AND_JAVA_DIAGNOSTICS_REPORT.md` — this audit report.
- `RUNTIME_SOURCE_CLEANUP_REPORT.md` — final runtime/source cleanup report.
- `docs/KNOWN_FIXED_BUGS.md` — records the duplicate-workspace diagnosis and resolution.
- `quanlynhahang/src/main/java/poly/edu/quanlynhahang/config/SpaRouteRegistry.java` —
  adds tested SPA routes for direct refresh.
- `quanlynhahang/src/main/java/poly/edu/quanlynhahang/config/GlobalExceptionHandler.java` —
  keeps missing API/static resources as JSON 404.
- `quanlynhahang/src/test/java/poly/edu/quanlynhahang/config/SpaRouteRegistryTest.java`
  and `GlobalExceptionHandlerTest.java` — regression coverage for the route fix.

The pre-existing deletion of
`BAO_CAO_DO_AN_TOT_NGHIEP_QUAN_LY_NHA_HANG_MOC_VI.docx` was not modified, restored,
staged, or included in this work.
