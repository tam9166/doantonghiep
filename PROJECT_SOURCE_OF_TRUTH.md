# Mộc Vị Restaurant — Project Source of Truth

## Canonical paths

- Canonical Git/project root: `E:\DoAnTotNghiep`
- Backend: `E:\DoAnTotNghiep\quanlynhahang`
- Frontend: `E:\DoAnTotNghiep\Frontend\nha-hang-frontend`
- Recommended VS Code workspace: `E:\DoAnTotNghiep\MOC_VI.code-workspace`

## Archived backup — do not edit or run

- `E:\DoAnTotNghiep_Archive\backup_20260819_161938`

The two original backups were compared before cleanup and were source-equivalent after
excluding generated dependency/build folders. One source backup is retained outside the
active project tree. The duplicate backup copy and generated archive folders were
removed. The retained backup must not be imported, built, launched, or edited.

## Standard VS Code run

1. Open `E:\DoAnTotNghiep\MOC_VI.code-workspace`.
2. Select the single launch configuration `Run Mộc Vị`.
3. The pre-launch pipeline builds the canonical frontend and compiles the canonical
   backend before Java Debugger starts the application.

The workspace and launch use the same default `dev` profile and ignored backend `.env`
file as the PowerShell workflow. Both resolve SQL Server `localhost:1433`, database
`RestaurantDB`, and application port `8080`. No credentials are stored in this document.

## Standard PowerShell run

From the canonical root:

```powershell
.\run-mocvi.ps1
```

The script performs the same frontend build → Maven compile → Spring Boot run sequence.
It intentionally does not run Maven `clean` during normal development runs because
removing `target/classes` while the application is starting or running can trigger
Spring Boot DevTools classpath deletion restarts.

Use the separate `Clean & Rebuild Mộc Vị` VS Code task only when a manual full rebuild
is required and the application is stopped.

## Cleanup status

Full safe-cleanup findings are recorded in
`E:\DoAnTotNghiep\FULL_PROJECT_CLEANUP_REPORT.md`. The active root has one backend,
one frontend, and one Spring Boot main class. The archive retains one source backup
outside the active tree.

## Individual build and run commands

Backend (Spring Boot, port 8080):

```powershell
cd E:\DoAnTotNghiep\quanlynhahang
.\mvnw.cmd spring-boot:run
```

Frontend development server:

```powershell
cd E:\DoAnTotNghiep\Frontend\nha-hang-frontend
npm run dev
```

Frontend production bundle:

```powershell
cd E:\DoAnTotNghiep\Frontend\nha-hang-frontend
npm run build
```

Vite writes the production bundle to
`E:\DoAnTotNghiep\quanlynhahang\src\main\resources\static`.
Maven copies it to `E:\DoAnTotNghiep\quanlynhahang\target\classes\static`.
Spring Boot on `localhost:8080` serves the classpath copy in `target\classes\static`.

Direct refresh for Vue routes such as `/admin/products`, `/admin/expired-food`, and
`/kitchen/inventory` is handled by the backend SPA fallback. Missing API/static
resources remain JSON 404 responses.

## Operational rule

Edit Vue source only in the canonical frontend, run `npm run build`, then rebuild
or restart the canonical Spring Boot backend before evaluating `localhost:8080`.
Do not use a stale `target\classes\static` bundle to assess a frontend change.
