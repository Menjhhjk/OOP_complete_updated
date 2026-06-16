# Iskollect Session Updates

This document summarizes the changes made in the latest development session. It is written as a focused changelog for advisor review.

## 2026-06-10 Supabase Alignment and Documentation Refresh

### Session Scope

This session aligned the backend code and reference documentation with the live Supabase PostgreSQL database. Supabase is now treated as the source of truth for table and column names.

### Database and Schema Findings

- Confirmed that the live Supabase database is reachable through JDBC.
- Confirmed all expected tables are individually reachable:
  - `badges`
  - `bottle_records`
  - `coupons`
  - `inout_logs`
  - `points_ledger`
  - `redemptions`
  - `streaks`
  - `user_badges`
  - `users`
- Confirmed `coupons` only has:
  - `coupon_id`
  - `coupon_name`
  - `points_required`
- Removed code assumptions for non-existent `coupons.description` and `coupons.coupon_type`.
- Confirmed `users` uses `password_hash`, not `password`.
- Confirmed `users` does not currently contain `name`, `course`, or `year_level`.
- Confirmed `inout_logs` uses:
  - `log_id`
  - `user_id`
  - `action`
  - `performed_at`
  - `ip_address`
  - `notes`
- Removed SQL assumptions around legacy in/out columns such as `event_type`, `entry_method`, `timestamp`, `staff_note`, and `status`.

### Java Files Updated

- `CouponDAO.java`
  - Now inserts and maps only `coupon_name` and `points_required`.
  - No longer reads or writes `description` or `coupon_type`.
- `Coupon.java`
  - Removed coupon description and coupon type fields.
- `UserDAO.java`
  - Registration now writes to `users.password_hash`.
  - User mapping reads `password_hash`.
  - Profile update now updates `username` only, because Supabase does not contain `name`, `course`, or `year_level`.
- `InOutLogDAO.java`
  - SQL now targets `action`, `performed_at`, and `notes`.
  - Date filtering now uses `performed_at::date`.
  - Duplicate checks now compare `action`.
- `InOutLog.java`
  - Updated to represent the Supabase-backed fields while keeping compatibility accessors for existing service code.
- `RedemptionDAO.java`
  - Redemption status values now use lowercase `pending` and `claimed`.
- `ReportService.java`
  - Removed references to missing `users.name`.
  - Redemption report filters now use lowercase status values.
- `WeeklyResetScheduler.java`
  - Removed dependency on missing `system_config`.
  - Last reset date is currently kept in memory during the application session.
- `TestDatabaseConnection.java`
  - Expanded from a basic connection test into a read-only Supabase diagnostics runner.
  - It checks each table, prints column metadata, queries sample rows, and masks credentials in the printed JDBC URL.

### SQL Reference Files Updated

- `sql/00_create_core_schema_postgresql.sql`
- `sql/01_create_inout_logs.sql`

These files are kept as Supabase-aligned references. They are not the source of truth if they ever disagree with the live Supabase database.

### Documentation Updated

- `ISKOLLECT - System Architecture.docx`
  - Updated from the older MySQL/Student/Reward/Transaction terminology to the current JavaFX + Java services/DAOs + JDBC + Supabase PostgreSQL architecture.
  - Added the current Supabase tables, DAO/service mappings, diagnostics runner, known gaps, and revised workflows.
  - A backup was saved as `ISKOLLECT - System Architecture.backup-20260610.docx`.
- `README.md`
  - Updated to reflect the current source tree, modules, Supabase schema notes, diagnostics runner, and known integration notes.
- `SESSION_UPDATES.md`
  - This section was added to document the new Supabase alignment work.

### Current Live Data Observed

- `coupons` contains 4 rows:
  - School Supplies, 10 points
  - Snack Voucher V1, 30 points
  - Snack Voucher V2, 50 points
  - Lunch Voucher, 100 points
- `badges` contains 5 rows:
  - Bronze, 5 points
  - Silver, 10 points
  - Emerald, 20 points
  - Gold, 35 points
  - Constellation, 50 points
- Other checked tables were reachable but empty at the time of diagnostics.

### Verification Performed

The project was compiled and tested successfully after the changes:

```bash
mvn test
```

### Remaining Work

- Replace `UserValidator` stub with real `UserDAO` validation for in/out logging.
- Review the mismatch between badge bonus logic in Java and the larger bonus values currently stored in Supabase.
- Decide whether profile fields such as course and year level should be added to Supabase or removed from the UI.
- Add focused DAO/service integration tests against a test database.
- Keep `resources/config.properties` out of public sharing because it contains database credentials.

## Session Scope

The session implemented the PostgreSQL-oriented backend and controller scaffolding for the Iskollect bottle-based recycling rewards system. The work followed the provided SAD instructions while adapting the database layer to PostgreSQL instead of MySQL.

## Major Decisions

- The project was changed to PostgreSQL-specific JDBC configuration.
- Maven was configured as the build system for Java 17, JavaFX, and the PostgreSQL JDBC driver.
- Existing ingress and egress logic was preserved, but its DAO date queries were adjusted for PostgreSQL.
- JavaFX controllers were kept thin: they call services and update UI fields only.
- Registration and authentication were not fully implemented because they are identified as a separate module.

## Files Added

### Models

- `Student.java`
- `Transaction.java`
- `Reward.java`
- `RedeemedReward.java`
- `SubmitResult.java`
- `RedeemResult.java`
- `TransactionHistory.java`
- `ReportResult.java`

These classes represent the main database-backed entities and immutable result objects used by services and controllers.

### DAOs

- `StudentDAO.java`
- `TransactionDAO.java`
- `RewardDAO.java`
- `RedeemedRewardDAO.java`

The DAO classes use `PreparedStatement` and `DBConnection.getInstance().getConnection()`. SQL was written for PostgreSQL compatibility.

### Services

- `PointsService.java`
- `StreakService.java`
- `BadgeService.java`
- `BottleService.java`
- `RewardService.java`
- `TransactionService.java`
- `ReportService.java`

These services implement point calculation, bottle submission, badge and streak handling, redemption, transaction history, and reporting workflows.

### Utilities

- `SessionManager.java`
- `CouponGenerator.java`

`SessionManager` stores the current student in memory. `CouponGenerator` creates 12-character uppercase coupon codes from UUID values.

### Scheduler

- `WeeklyResetScheduler.java`

The scheduler reads and writes `system_config.last_weekly_reset`, resets weekly student stats, and uses PostgreSQL `ON CONFLICT` for the configuration upsert.

### Controllers

- `DashboardController.java`
- `BottleSubmitController.java`
- `RewardsController.java`
- `RedeemController.java`
- `TransactionController.java`
- `ProfileController.java`
- `InOutController.java`

The controllers are JavaFX-facing classes. They contain no SQL and delegate business workflows to the service or DAO layer.

### Exceptions

- `InsufficientPointsException.java`
- `AuthException.java`

`AuthException` is intentionally a stub for the future registration and authentication module.

### Configuration and Build Files

- `pom.xml`
- `.vscode/settings.json`
- `resources/config.properties`
- `sql/00_create_core_schema_postgresql.sql`

The Maven file now includes JavaFX and PostgreSQL dependencies. VS Code settings were added so the Java language server imports Maven dependencies automatically.

## Files Updated

### `DBConnection.java`

Updated from MySQL driver loading to PostgreSQL driver loading:

```java
Class.forName("org.postgresql.Driver");
```

The expected JDBC URL is now:

```text
jdbc:postgresql://localhost:5432/iskollect_db
```

### `InOutLogDAO.java`

Updated date filtering queries to PostgreSQL-compatible syntax:

```sql
timestamp::date
```

The table documentation was also adjusted away from MySQL-specific types such as `AUTO_INCREMENT` and `DATETIME`.

### `InOutServiceTest.java` (Removed)

This was an earlier empty test placeholder. It has since been deleted along with its test folder because it had no active test coverage or use.

Earlier in the project, its package declaration had been fixed from:

```java
package test.com.iskollect;
```

to:

```java
package com.iskollect;
```

This matches the test source root and folder path.

## PostgreSQL Schema

The new PostgreSQL schema file is:

```text
sql/00_create_core_schema_postgresql.sql
```

It creates:

- `students`
- `transactions`
- `rewards`
- `redeemed_rewards`
- `inout_logs`
- `system_config`

It also seeds the reward catalog:

- Supplies Coupon
- Snack V1 Coupon
- Snack V2 Coupon
- Lunch Coupon

## Verification Performed

The following commands were run successfully:

```bash
mvn -q -DskipTests compile
mvn -q test
```

A search was also performed for common MySQL leftovers such as:

- `jdbc:mysql`
- `com.mysql`
- `AUTO_INCREMENT`
- `DATETIME`
- `TINYINT`

No remaining matches were found in the checked source, SQL, resource, or Maven files.

## Remaining Work

- Build or connect the JavaFX FXML views.
- Replace `StudentValidator` with real student lookup logic after the registration module is available.
- Implement the full authentication and registration module separately.
- Confirm local PostgreSQL credentials in `resources/config.properties`.
- Run the PostgreSQL schema against the actual development database.
- Add broader unit and integration tests for DAO and service behavior.

## Notes

The session produced a compiling backend-oriented implementation. The database direction is now PostgreSQL-specific, and the Maven build verifies that the source compiles with the declared JavaFX and PostgreSQL dependencies. The main intentional gap is authentication and registration, which remains separate by project instruction.
