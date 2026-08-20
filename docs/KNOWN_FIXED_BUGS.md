# Known bug audit

Statuses: **FIXED** means source/test evidence is present; **STILL EXISTS** means source evidence confirms it; **NOT VERIFIED** needs an executable environment or deeper scenario testing; **NOT APPLICABLE** means the feature is absent.

| ID | Severity | Flow | Status | Evidence / next action |
|---|---|---|---|---|
| BUG-001 | P0 | External API | FIXED | `src/services/api.js` has separate `externalApi`; `api.test.js` checks no auth/captcha headers. |
| BUG-002 | P0 | Timekeeping | FIXED | `SelfServiceDataIsolationIntegrationTest` proves check-in and personal attendance ignore a forged username and use the authenticated principal. |
| BUG-003 | P0 | Staff | FIXED | Two-user integration coverage proves personal schedules and service zones cannot be read via a forged username; V044 supplies the required service-zone table. |
| BUG-004 | P0 | Account API | FIXED | Sensitive `Account` fields are ignored/write-only and regression coverage audits direct serialization plus staff, customer and order responses. |
| BUG-005 | P0 | Order | FIXED | A database integration test forces a failure at the final activity-log step and proves details, totals, inventory and idempotency data all roll back. |
| BUG-006 | P0 | Inventory | FIXED | Database-backed shortage coverage proves no detail/operation is created and order totals plus batch/ingredient stock remain unchanged. |
| BUG-007 | P0 | Inventory | FIXED | Concurrent database requests for the last unit yield exactly one success and one shortage conflict; pessimistic locks prevent negative stock. |
| BUG-008 | P0 | Order | FIXED | Repeating the same add-items request and idempotency key returns the stored result, creates one detail/operation and consumes inventory once. |
| BUG-009 | P0 | Money | FIXED | Money, inventory quantities, recipe amounts and tax rates use `BigDecimal`; V045 converts the six remaining live `FLOAT` columns to exact `DECIMAL` types. |
| BUG-010 | P0 | JWT | FIXED | Production and staging profiles fail fast without a configured JWT secret; `ApplicationStartupValidatorTest` covers both profile families. |
| BUG-011 | P0 | Credentials | FIXED | Production/staging startup rejects plaintext hashes and common weak/demo passwords for every account; tracked deployment configuration contains placeholders only. |
| BUG-012 | P1 | Password route | FIXED | `/change-password` is routed by frontend and SPA fallback. |
| BUG-013 | P1 | Authorization | FIXED | Frontend admin-route matrix now matches backend operational roles; frontend and backend matrix regressions pass. |
| BUG-014 | P1 | Error handling | FIXED | Global/security/filter errors use the stable JSON contract with explicit `application/json` and correlation IDs. |
| BUG-015 | P1 | Rate limit | FIXED | Policies match exact method/path; public chatbot's tighter rule is ordered before the broad AI matcher and covered by regression. |
| BUG-016 | P1 | Rate limit | FIXED | Client identity uses the servlet remote address and ignores attacker-controlled forwarding headers; bypass regression passes. |
| BUG-017 | P1 | Rate limit | FIXED | Production uses the bounded/expiring SQL Server store from V047; two independent service instances share one quota in the database regression. |
| BUG-018 | P1 | Review | FIXED | Public projection excludes reservation ID/code, phone and moderation fields; admin projection remains role-protected. |
| BUG-019 | P1 | Reservation lookup | FIXED | Public lookup is POST body with code + phone. |
| BUG-020 | P1 | Review | FIXED | Null/out-of-range rating returns 422 in controller. |
| BUG-021 | P1 | Review | FIXED | Ratings, phone, URL and text limits/formats are validated; V046 enforces one review per reservation under concurrency. |
| BUG-022 | P1 | Booking | FIXED | Booking codes use a date plus random UUID suffix, are checked before persistence, returned from the saved entity, and V048 enforces database uniqueness. |
| BUG-023 | P1 | Booking | FIXED | Legacy guest-booking is gone; dine-in data uses normalized `order_type`/`table_id` and no longer stores table metadata in address. |
| BUG-024 | P1 | Reservation table | FIXED | Checkout, open-order lookup, table status, refund, merge and split use exact `tableId`/FK paths with no address substring inference. |
| BUG-025 | P1 | Migration | FIXED | V001/V002 provide the official baseline; the blank-database regression created a temporary SQL Server database and applied all 50 versioned migrations successfully. |
| BUG-026 | P1 | Payment config | FIXED | Demo values are dev-only; production requires external payment values, disables demo mode and fails startup for unsafe configuration. |
| BUG-027 | P1 | Recruitment upload | FIXED | Private UUID storage validates extension/MIME/magic and DOCX structure, blocks active content/traversal/ZIP bombs, restricts download, and purges expired CVs. |
| BUG-028 | P2 | Timekeeping | NOT VERIFIED | Check-out preservation needs scenario test. |
| BUG-029 | P2 | Date parsing | NOT VERIFIED | Partial Java-time conversion observed; complete audit pending. |
| BUG-030 | P2 | Timezone | NOT VERIFIED | Vietnam zone exists in timekeeping; cross-flow verification pending. |
| BUG-031 | P2 | Reservation | FIXED | `RestaurantBusinessHoursService` centralizes configured hours. |
| BUG-032 | P2 | Persistence | NOT VERIFIED | Query profiling pending. |
| BUG-033 | P2 | Lists | NOT VERIFIED | Admin endpoint pagination audit pending. |
| BUG-034 | P2 | Auth | STILL EXISTS | Access token is stored in `localStorage`; architecture decision/refactor needed. |
| BUG-035 | P2 | CSRF | NOT VERIFIED | Verify cookie/token architecture and endpoints. |
| BUG-036 | P2 | Roles | NOT VERIFIED | Route/controller permission duplication audit pending. |
| BUG-037 | P2 | SPA | FIXED | Explicit SPA routes include change-password and event booking; API/assets excluded. |
| BUG-038 | P2 | Spring config | STILL EXISTS | Deprecated `WebMvcConfigurer` converter/path APIs emit compiler warnings. |
| BUG-039 | P2 | CI | NOT VERIFIED | Linux execute bit must be checked in a Linux checkout. |
| BUG-040 | P2 | Repository hygiene | STILL EXISTS | Runtime artifacts/logs/backups/zip are present locally; do not delete without owner approval. |
| BUG-041 | P2 | Documentation | NOT VERIFIED | README and deployment docs require source comparison. |
| BUG-042 | P2 | Frontend | NOT VERIFIED | Large components exist; no safe refactor selected. |
| BUG-043 | P2 | Frontend tests | STILL EXISTS | Only limited unit tests were found; broaden coverage. |
| BUG-044 | P2 | UX | STILL EXISTS | Many production views call native alert/confirm/prompt. |
| BUG-045 | P2 | Frontend API | STILL EXISTS | Many views call authenticated Axios directly instead of shared service layer. |
| BUG-046 | P2 | JPA | NOT VERIFIED | `@Data` appears in entities; relation/logging audit pending. |
| BUG-047 | P2 | Account roles | FIXED | Authentication repository query fetches authorities and roles explicitly while the general entity relation remains LAZY. |
| BUG-048 | P2 | Errors | NOT VERIFIED | Raw exception exposure audit pending. |
| BUG-049 | P2 | PII | NOT VERIFIED | Retention/access policy not documented. |
| BUG-050 | P2 | Performance | NOT VERIFIED | Build code-splits views; dashboard/query measurement pending. |

## New defects fixed this audit

| ID | Severity | Root cause | Fix | Regression |
|---|---|---|---|---|
| BUG-051 | P0 | Uncommitted edits removed imports/methods and used invalid Java identifiers, leaving backend uncompilable. | Restored compatible imports, service wiring, method signatures and repository contract. | `mvnw.cmd -DskipTests compile` passes. |
| BUG-052 | P1 | AI availability queried every table instead of the repository contract for active, ordered tables. | Use `findByActiveTrueOrderByAreaIdAscIdAsc()`. | FIXED: `AiAvailabilityToolServiceTest`; full backend suite passed. |
| BUG-053 | P2 | Last-order comparison included the exact cutoff despite the documented “before cutoff” rule and did not handle overnight hours. | Require a strict cutoff and handle same-day/overnight windows explicitly. | FIXED: `RestaurantBusinessHoursServiceTest`; full backend suite passed. |
| BUG-054 | P1 | Conversation date parsing accepted an injected clock but called `LocalDate.now(zoneId)`. | Derive today with `LocalDate.ofInstant(now, zoneId)`. | FIXED: fixed-clock regressions; full backend suite passed. |
| BUG-055 | P2 | Expiry intent used broad “sắp hết” keywords and unordered intent iteration, so low stock could be misclassified as expiration. | Reserve expiry intent for explicit expiry phrases and define deterministic intent precedence. | FIXED: `RoleAwareAssistantServiceTest`; full backend suite passed. |
| BUG-056 | P1 | The guest-count ternary mixed primitive parsing with a nullable fallback, causing Java to unbox `null` when a message contained no guest count. | Box the parsed branch explicitly so a nullable fallback remains valid. | FIXED: `AiConversationMemoryServiceTest`; full backend suite passed. |
| BUG-057 | P0 | Staff login loaded a LAZY authority collection after the repository session closed, so valid credentials returned HTTP 500. | Add a dedicated authentication query with an entity graph for authorities and roles. | FIXED: `AccountTokenSecurityTest` plus real `admin` login against SQL Server. |
| BUG-058 | P0 | Staff, popular-item and reservation admin screens queried LAZY data incorrectly; the reservation graph also named unmapped attributes. | Add scoped fetch queries for staff/order details and correct the reservation entity graph. | FIXED: `AdminDataVisibilityRegressionTest` plus authenticated API smoke checks. |
| BUG-059 | P1 | The in-memory rate limiter was process-local and had no production-wide quota or durable cleanup bound. | Add a transactional SQL Server quota store with expiry cleanup and a 100,000-row cap while retaining the bounded Caffeine store for local use. | FIXED: `RateLimitDatabaseIntegrationTest`, `RateLimitServiceTest`, and `RateLimitingFilterTest`. |
| BUG-060 | P1 | Sequential booking codes could race across requests and schema uniqueness was not guaranteed by an official migration. | Generate high-entropy date-prefixed codes, verify nonexistence, return the saved value, and add V048's unique index. | FIXED: 100-code uniqueness regression and successful V048 migration. |
| BUG-061 | P1 | Flyway began at V003, so a new database depended on destructive manual setup scripts and also lacked `reservation_waitlist`. | Add non-destructive V001 schema and V002 seed baselines including the missing waitlist schema. | FIXED: `BlankDatabaseMigrationIntegrationTest` applies V001-V048 to a real temporary SQL Server database and verifies core tables/data. |
| BUG-062 | P1 | Recruitment files trusted shallow metadata, lived under a weak path contract, and had no retention lifecycle. | Introduce private safe-name storage, layered content/ZIP validation, attachment-only authorized downloads, generic errors, and scheduled retention. | FIXED: `CvFileStorageServiceTest` and `CvRetentionServiceTest`. |

## Master Prompt V2 continuation audit (2026-08-20)

These labels are prefixed with `V2-` because the supplied prompt reused IDs already present above.

| ID | Severity | Status | Evidence / disposition |
|---|---|---|---|
| V2-BUG-051 | P1 | FIXED | V049 repaired 149 existing table-linked orders from `TAKEAWAY` to `DINE_IN`, removed the database default, and checkout now rejects a missing `orderType` with 422. |
| V2-BUG-052 | P1 | FIXED | Base/prod/production rate limiting defaults to enabled; only explicit dev/test/local-example profiles default to disabled. |
| V2-BUG-053 | P1 | FIXED | The scheduled reservation expiry job now queries at most 200 matching candidate IDs, fetches only that batch, and V050 adds the supporting indexes. |
| V2-BUG-054 | P1 | FIXED | Customer (`token`/`user`) and staff (`staff_token`/`staff_user`) namespaces have separate readers/logout operations; staff screens no longer fall back to customer credentials. |
| V2-BUG-055 | P2 | INCOMPLETE_FEATURE | `contact_task_status` exists only in V041 and the entity mapping. The implemented contact workflow uses `contact_status`, call note, actor, and timestamp fields from V036. Do not invent a second workflow until product requirements define its states, transitions, ownership, and UI. |
| V2-BUG-056 | P1 | FIXED | A Flyway `beforeMigrate` callback gives actionable duplicate-data failures before V046/V048; integration tests cover blank, valid legacy, duplicate review, and duplicate booking-code databases. |
| V2-BUG-057 | P1 | FIXED | Kitchen status/toggle/group/recipe/AI failures use the shared safe backend error extractor with user-safe fallbacks. |
| V2-BUG-058 | P1 | FIXED | Remaining legacy green/gold/cream literals were moved to canonical burgundy/navy and semantic tokens; a source regression prevents the legacy palette from returning. |
