# Iskollect

Iskollect is a Java desktop application for a bottle-based recycling rewards system. It uses a 3-tier architecture:

- Presentation layer: JavaFX controllers
- Business logic layer: service classes and model/result objects
- Data access layer: JDBC DAOs connected to Supabase PostgreSQL

Supabase PostgreSQL is the database source of truth. The SQL files in this repository are reference files that should match the live Supabase schema, but the live Supabase schema takes priority when there is a mismatch.

## Technology Stack

- Java 17 target, commonly run with JDK 21
- JavaFX 21.0.5
- Maven
- JDBC
- PostgreSQL JDBC driver 42.7.4
- Supabase PostgreSQL
- jBCrypt 0.4

## Database

Connection settings are read from:

```text
resources/config.properties
```

Expected keys:

```properties
db.url=jdbc:postgresql://<host>:5432/<database>
db.user=<username>
db.password=<password>
```

The current Supabase-aligned reference schema files are:

```text
sql/00_create_core_schema_postgresql.sql
sql/01_create_inout_logs.sql
```

Current expected Supabase tables:

- `users`
- `badges`
- `bottle_records`
- `coupons`
- `inout_logs`
- `points_ledger`
- `redemptions`
- `streaks`
- `user_badges`

Important schema notes:

- `coupons` has only `coupon_id`, `coupon_name`, and `points_required`.
- `coupons.description` and `coupons.coupon_type` do not exist.
- `users` stores hashed passwords in `password_hash`, not `password`.
- `users` does not currently store `name`, `course`, or `year_level`.
- `inout_logs` uses `action`, `performed_at`, `ip_address`, and `notes`.
- Legacy in/out columns such as `event_type`, `entry_method`, `timestamp`, `staff_note`, and `status` are not in the live Supabase table.

## Project Structure

```text
Iskollect/
|-- pom.xml                                      [Build Configuration]
|-- README.md                                    [Project Documentation]
|-- SESSION_UPDATES.md                           [Advisor Session Documentation]
|-- resources/
|   `-- config.properties                        [Database Configuration]
|-- sql/
|   |-- 00_create_core_schema_postgresql.sql     [Supabase Reference Schema]
|   `-- 01_create_inout_logs.sql                 [Ingress/Egress Reference Schema]
|-- src/com/iskollect/
|   |-- TestDatabaseConnection.java              [Supabase Diagnostics]
|   |-- controller/
|   |   |-- ActivityHistoryController.java       [Activity History]
|   |   |-- BottleSubmitController.java          [Bottle Submission and Points]
|   |   |-- CouponsController.java               [Coupons Catalog]
|   |   |-- DashboardController.java             [Dashboard, Points, Badges, Streaks]
|   |   |-- InOutController.java                 [Ingress/Egress Logging]
|   |   |-- ProfileController.java               [User Profile]
|   |   `-- RedeemController.java                [Coupon Redemption]
|   |-- dao/
|   |   |-- BottleRecordDAO.java                 [Bottle Submission and Activity History]
|   |   |-- CouponDAO.java                       [Coupons Catalog]
|   |   |-- InOutLogDAO.java                     [Ingress/Egress Logging]
|   |   |-- PointsLedgerDAO.java                 [Points Ledger and Audit Trail]
|   |   |-- RedemptionDAO.java                   [Coupon Redemption and History]
|   |   `-- UserDAO.java                         [Authentication, Session, User Points]
|   |-- exception/
|   |   |-- AuthException.java                   [Authentication]
|   |   |-- DatabaseException.java               [Shared Database Error Handling]
|   |   |-- DuplicateLogException.java           [Ingress/Egress Logging]
|   |   |-- InsufficientPointsException.java     [Coupon Redemption]
|   |   |-- InvalidInputException.java           [Validation]
|   |   `-- NavigationException.java             [Navigation]
|   |-- model/
|   |   |-- ActivityHistory.java                 [Activity History]
|   |   |-- BottleRecord.java                    [Bottle Submission and Points]
|   |   |-- Coupon.java                          [Coupons Catalog]
|   |   |-- InOutLog.java                        [Ingress/Egress Logging]
|   |   |-- LogResult.java                       [Ingress/Egress Logging]
|   |   |-- RedeemResult.java                    [Coupon Redemption]
|   |   |-- Redemption.java                      [Coupon Redemption and History]
|   |   |-- ReportResult.java                    [Reports]
|   |   |-- SubmitResult.java                    [Bottle Submission and Points]
|   |   `-- User.java                            [Authentication, Session, User Profile]
|   |-- scheduler/
|   |   `-- WeeklyResetScheduler.java            [Weekly Reset]
|   |-- service/
|   |   |-- ActivityHistoryService.java          [Activity History]
|   |   |-- AuthService.java                     [Authentication]
|   |   |-- BadgeService.java                    [Badges and Weekly Rewards]
|   |   |-- BottleService.java                   [Bottle Submission and Points]
|   |   |-- CouponService.java                   [Coupons and Redemption]
|   |   |-- InOutService.java                    [Ingress/Egress Logging]
|   |   |-- PointsService.java                   [Points Calculation and Balance]
|   |   |-- ReportService.java                   [Reports]
|   |   |-- SecurityCheck.java                   [Session Security]
|   |   `-- StreakService.java                   [Streak Bonuses]
|   `-- util/
|       |-- AlertUtil.java                       [JavaFX Alerts]
|       |-- CouponGenerator.java                 [Coupon Redemption]
|       |-- DBConnection.java                    [Supabase JDBC Connection]
|       |-- PasswordUtil.java                    [Authentication Security]
|       |-- RedirectUtil.java                    [Navigation]
|       |-- SessionManager.java                  [Session Management]
|       `-- UserValidator.java                   [Ingress/Egress User Validation Stub]
```

## Implemented Modules

### Authentication and Session Management

- PUP webmail-only registration validation
- BCrypt password hashing
- Login password verification against `users.password_hash`
- Session token persistence in `users.session_token`
- Last-activity tracking in `users.last_activity`
- `SecurityCheck` validates idle timeout and token symmetry

### Bottle Submission and Points

- Bottle count validation
- Base point calculation: `bottles * 0.5`
- Streak bonus calculation
- Weekly badge bonus calculation
- Bottle submission persistence through `bottle_records`
- Auditable point deltas through `points_ledger`
- User point and bottle total updates through `users`

### Coupons and Redemption

- Supabase coupon catalog retrieval from `coupons`
- Atomic redemption flow using JDBC transactions
- Unique coupon code generation
- Negative point ledger entry on redemption
- Redemption history retrieval from `redemptions`
- Redemption status values currently use `pending` and `claimed`

### Ingress and Egress Logging

- Manual ingress and egress event logging
- Duplicate event detection by `user_id`, `action`, and `performed_at`
- Daily log retrieval
- Supabase-aligned `inout_logs` columns:
  - `log_id`
  - `user_id`
  - `action`
  - `performed_at`
  - `ip_address`
  - `notes`

### Activity History

- Bottle submissions and redemptions merged into one history
- Filtering by today, current week, current month, or current year

### Reports

`ReportService` supports:

- Bottle summary by user and date range
- Weekly leaderboard
- Points ledger
- Redemption report
- System summary

Report methods return `ReportResult` and convert database errors into failure results.

### Weekly Reset

`WeeklyResetScheduler` runs every 7 days and delegates weekly reset behavior to `BadgeService`.

The live Supabase schema does not contain `system_config`, so the scheduler currently keeps its last-reset date in memory during the running application session.

## Supabase Diagnostics

`src/com/iskollect/TestDatabaseConnection.java` is a read-only diagnostics runner for the live Supabase database.

It:

- Tests the JDBC connection.
- Masks `user` and `password` values in the printed JDBC URL.
- Checks each expected table individually.
- Lists table columns from `information_schema.columns`.
- Runs `SELECT * FROM public.<table> LIMIT 5`.
- Prints sample rows as console pseudo-tables.
- Prints `(no rows)` when a table exists and is reachable but empty.

Run it from the IDE as:

```text
com.iskollect.TestDatabaseConnection
```

## JavaFX Prototypes

The repository includes two JavaFX UI prototypes based on the UI team's low-fidelity PDF:

- `com.iskollect.prototype.HighFidelityPrototype`
  - Current default for `mvn javafx:run`
  - Uses the existing Supabase/JDBC backend where data is available
  - Shows `NIL` placeholders when tables are empty
  - Includes a `Bypass Login for Testing` button
  - Uses a green-grey/white palette with muted accent colors
- `com.iskollect.prototype.LowFidelityPrototype`
  - Earlier wireframe-style prototype
  - Useful for comparing against the original low-fidelity layout

Run the high-fidelity prototype:

```bash
mvn javafx:run
```

If your IDE reports `JavaFX runtime components are missing`, use the Maven command above or configure the IDE to launch with the JavaFX Maven plugin/module path.

## Build and Test

Compile:

```bash
mvn -q -DskipTests compile
```

Run tests:

```bash
mvn -q test
```

## IDE Notes

VS Code or another Java IDE should import the Maven project automatically.

If JavaFX imports show as unresolved, clean/reload the Java language server and allow Maven dependencies to be imported.

## Known Integration Notes

- Supabase PostgreSQL is the source of truth for schema decisions.
- `resources/config.properties` contains database credentials and should not be shared publicly.
- `UserValidator` is still a stub and currently returns `true`; in/out logging should eventually validate real users.
- `ProfileController` still exposes course/year-level UI fields, but Supabase does not persist those fields.
- `InOutLog` keeps compatibility enums for entry method/status, but those values are not stored in Supabase.
- Badge bonus values in code and Supabase seed data should be reviewed for product consistency.
- Additional DAO/service integration tests are still needed.

## Architecture Document

The backend system architecture document was refreshed to match the current Java/Supabase implementation:

```text
C:\Users\Rommel\Downloads\ISKOLLECT - System Architecture.docx
```

A backup of the previous version was saved as:

```text
C:\Users\Rommel\Downloads\ISKOLLECT - System Architecture.backup-20260610.docx
```

## Advisor Session Notes

A focused advisor-readable summary of development changes is available in:

```text
SESSION_UPDATES.md
```
