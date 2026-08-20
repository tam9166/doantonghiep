# Regression tests

| ID | Scope | Assertion | Command/status |
|---|---|---|---|
| REG-SEC-001 | External API | No Authorization or X-Captcha-Token on `externalApi`. | `npm test` (existing `api.test.js`) |
| REG-REVIEW-020 | Review rating | Null or value outside 1..5 returns 422. | Add MVC test before changing review flow. |
| REG-SPA-012 | Change password | Direct route and refresh serve the SPA. | Browser/E2E required. |
| REG-BUILD-051 | Backend compilation | Source compiles after integrating fixes. | `mvnw.cmd -DskipTests compile` passed. |
| REG-ORDER-005 | Order atomicity | A late failure leaves no details, totals, inventory mutations or idempotency record. | `OrderCheckoutPersistenceIntegrationTest` |
| REG-STOCK-006 | Inventory shortage | Insufficient stock creates nothing and preserves order totals plus batch/ingredient quantities. | `OrderCheckoutPersistenceIntegrationTest`; maps to HTTP 409 via `GlobalExceptionHandlerTest`. |
| REG-STOCK-007 | Inventory race | Two database transactions requesting the last stock yield exactly one success and stock never becomes negative. | `OrderCheckoutPersistenceIntegrationTest` |
| REG-ORDER-008 | Add-items retry | Reusing the same idempotency key and payload returns the stored result without a second detail or inventory deduction. | `OrderCheckoutPersistenceIntegrationTest` |
| REG-MONEY-009 | Exact numeric types | Inventory, recipe and tax fields use `BigDecimal`/`DECIMAL`; `0.1 × 3` consumes exactly `0.3000` and all six migrated columns are audited at runtime. | V045 and `OrderCheckoutPersistenceIntegrationTest` |
| REG-AI-052 | Availability | Only active tables with sufficient capacity are returned. | `AiAvailabilityToolServiceTest` |
| REG-TIME-053 | Last order | Exact cutoff and later times reject new orders. | `RestaurantBusinessHoursServiceTest` |
| REG-AI-054 | Conversation date | “Tomorrow” uses the injected clock in Vietnam timezone. | `AiDynamicToolServiceTest` |
| REG-AI-055 | Intent | “Sắp hết” means low stock; “sắp hết hạn” means expiry. | `RoleAwareAssistantServiceTest` |
| REG-BOOKING-056 | Legacy booking | Deprecated guest booking returns 410 and does not write PII into order address. | `OrderWorkflowGuardTest` |
| REG-RES-057 | Reservation state | Pending reservation must pass table assignment before payment/confirmation. | `ReservationStateMachineTest`, `ReservationServiceTest` |
| REG-AI-058 | Conversation memory | Messages without a guest count retain a nullable fallback instead of throwing during ternary unboxing. | `AiConversationMemoryServiceTest`; passed in full backend suite. |
| REG-AUTH-059 | Staff login | Authentication loads account authorities and their roles before the repository session closes. | `AccountTokenSecurityTest` plus live staff-login smoke test. |
| REG-ADMIN-060 | Admin data | Staff, popular items and reservations load their required relationships without LAZY/entity-graph failures. | `AdminDataVisibilityRegressionTest` plus authenticated API smoke checks. |
| REG-AUTH-061 | Self-service IDOR | Timekeeping, personal schedule and personal service-zone APIs always use the authenticated principal even if another username is supplied. | `SelfServiceDataIsolationIntegrationTest` |
| REG-SEC-062 | Account response | Direct Account serialization and normal staff/customer/order admin responses contain no password/hash/token-version/internal authority fields. | `SelfServiceDataIsolationIntegrationTest` |
| REG-SEC-063 | JWT startup | Production and staging fail fast when the JWT signing secret is absent; development retains its explicit local-only fallback. | `ApplicationStartupValidatorTest` |
| REG-SEC-064 | Production credentials | Every production/staging account must have a BCrypt hash and cannot use common demo credentials. | `ProductionCredentialValidatorTest` plus tracked configuration audit. |
| REG-ORDER-065 | Explicit order type | Checkout rejects a populated request without `orderType`; delivery/takeaway/dine-in callers must state their flow. | `OrderCheckoutServiceTest` and `OrderCheckoutPersistenceIntegrationTest` |
| REG-RATE-066 | Profile fail-closed | Base/prod/production enable rate limiting; only dev/test/local example disable it by default. | `RateLimitProfileConfigurationTest` |
| REG-RES-067 | Bounded expiry scan | Expiry/no-show processing receives only repository-selected candidates and never reuses the admin full-table listing. | `ReservationServiceTest` plus the candidate query and V050 indexes |
| REG-SESSION-068 | Customer/staff isolation | Staff requests never fall back to customer tokens; each logout preserves the other active namespace. | `api.test.js`, `session.test.js` |
| REG-MIGRATION-069 | Unique preflight | Blank and valid legacy schemas migrate; duplicate review IDs or reservation codes fail before unique-index migrations with actionable messages. | `BlankDatabaseMigrationIntegrationTest`, `UniqueConstraintMigrationPreflightIntegrationTest` |
| REG-UI-070 | Canonical theme | Legacy green/gold/cream palette values cannot reappear in Vue/CSS source. | `themeTokens.test.js` |

Latest verification (2026-08-20): backend 293 tests, 0 failures/errors/skips; frontend 26 tests passed; frontend lint has 0 errors and 8 pre-existing warnings; production build passed; Flyway applied 50 versioned migrations to a blank SQL Server database.
