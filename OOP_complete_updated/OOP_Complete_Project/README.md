# ISKOllect

ISKOllect is a JavaFX desktop application for a school-based garbage recycling rewards system. It allows students to register using their PUP webmail, submit collected bottles, earn points and badges, redeem rewards, and view their bottle and transaction history.

The system was developed as an Object-Oriented Programming project using a layered Java architecture with PostgreSQL/Supabase as the database.

## Features

- User registration and login using PUP webmail
- Password hashing with BCrypt
- Bottle submission with point calculation
- Streak and badge rewards
- Rewards catalog and coupon redemption
- Transaction history with filters
- Bottle collection records
- Profile management
- Password update validation
- PostgreSQL/Supabase database connection

## Technology Stack

- Java 21
- JavaFX 21
- Maven
- JDBC
- PostgreSQL / Supabase
- jBCrypt
- FXML and CSS for the interface

## Project Structure

```text
OOP_Complete_Project/
|-- pom.xml
|-- mvnw
|-- mvnw.cmd
|-- README.md
|-- sql/
|   `-- 00_create_core_schema_postgresql.sql
|-- src/
|   `-- main/
|       |-- java/
|       |   |-- module-info.java
|       |   `-- com/
|       |       `-- iskollect/
|       |           |-- Main.java
|       |           |-- TestDatabaseConnection.java
|       |           |-- controller/
|       |           |-- dao/
|       |           |-- exception/
|       |           |-- model/
|       |           |-- scheduler/
|       |           |-- service/
|       |           `-- util/
|       `-- resources/
|           |-- config.properties
|           `-- com/
|               `-- iskollect/
|                   |-- fxml/
|                   |-- assets/
|                   `-- style.css
```

## Prerequisites

Before running the project, install:

- JDK 21 or later
- Git, if cloning from GitHub
- Internet connection for Supabase database access
- An IDE such as IntelliJ IDEA, VS Code, or NetBeans

Maven does not need to be installed separately because the project includes the Maven Wrapper.

Check Java with:

```bash
java -version
```

## Database Setup

ISKOllect uses PostgreSQL through Supabase.

The database configuration file is:

```text
src/main/resources/config.properties
```

Use this format:

```properties
db.url=jdbc:postgresql://<host>:5432/<database>
db.user=<database_username>
db.password=<database_password>
```

The database schema reference is located at:

```text
sql/00_create_core_schema_postgresql.sql
```

Expected tables include:

- `users`
- `badges`
- `bottle_records`
- `coupons`
- `inout_logs`
- `points_ledger`
- `redemptions`
- `streaks`
- `user_badges`

The current schema reference follows `ISKOllect_Schema_6-14-2026.docx` for the core table and column names. It also includes the documented foreign-key delete behavior, `display_name VARCHAR(50)`, and badge bonus values of `0`, `1`, `3`, `5`, and `10`.

Important: Do not commit real database credentials to a public GitHub repository. Use placeholder values or a private configuration file when sharing the project publicly.

## Installation

Clone the repository or download the project folder.

Open PowerShell or Command Prompt inside the project folder:

```text
OOP_Complete_Project
```

Install dependencies and build the project:

```bash
.\mvnw.cmd clean install
```

On macOS or Linux:

```bash
./mvnw clean install
```

## Running the Application

Run the JavaFX application with:

```bash
.\mvnw.cmd javafx:run
```

On macOS or Linux:

```bash
./mvnw javafx:run
```

The login screen should appear after the application starts.

## Running from an IDE

1. Open `OOP_Complete_Project` as a Maven project.
2. Wait for Maven dependencies to finish importing.
3. Run the main class:

```text
src/main/java/com/iskollect/Main.java
```

Main class:

```text
com.iskollect.Main
```

If the IDE shows a JavaFX runtime error, run the application through Maven:

```bash
.\mvnw.cmd javafx:run
```

## Testing the Database Connection

The project includes a database diagnostic class:

```text
src/main/java/com/iskollect/TestDatabaseConnection.java
```

Class name:

```text
com.iskollect.TestDatabaseConnection
```

Run this class from the IDE to verify that the application can connect to the database and access the required tables.

## Build and Test

Compile without running tests:

```bash
.\mvnw.cmd -q -DskipTests package
```

Run tests:

```bash
.\mvnw.cmd -q test
```

## Key Implementation Details

### CRUD Operations

The system uses DAO classes to perform database operations:

- `UserDAO` handles user records, profile updates, passwords, and session tokens.
- `BottleRecordDAO` handles bottle submission records.
- `CouponDAO` handles available coupon data.
- `RedemptionDAO` handles coupon redemption history.
- `PointsLedgerDAO` records point changes.

### Validation

Validation is handled in the controllers and service layer. The system validates:

- Required login and registration fields
- PUP webmail format
- Password length and required characters
- Bottle submission limits
- Age format
- Username length and allowed characters
- Matching password confirmation fields

### Database Connection

Database access is handled through:

```text
src/main/java/com/iskollect/util/DBConnection.java
```

The application uses JDBC prepared statements in DAO classes to reduce SQL injection risk and safely pass user input to database queries.

## Recent Bug Fixes

- Added a maximum bottle submission limit.
- Fixed transaction date filters.
- Separated display name and username updates.
- Added visible validation messages for profile and password updates.
- Improved PUP webmail validation during registration and login.
- Reduced repeated login/logout memory buildup by loading scenes fresh and stopping detached screen clocks.
- Aligned Maven compiler settings with Java 21.

## Known Limitations

- Account deletion and record deletion are not available through the user interface.
- Database credentials must be configured before running the application.
- The schema document defines `inout_logs.action` for account/session events such as `LOGIN`, `LOGOUT`, `SESSION_TIMEOUT`, and `LOCK`. The current system now records `LOGIN`, `LOGOUT`, and `SESSION_TIMEOUT`, but it also keeps the existing ingress/egress monitoring values `INGRESS` and `EGRESS` because that module is already implemented in the codebase.

## Authors

Developed by the ISKOllect project team for an Object-Oriented Programming course.
