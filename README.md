# AttendX

AttendX is a JavaFX desktop application for managing academic attendance. It includes student and faculty management, attendance tracking, dashboards, and report exports.

## Requirements

- JDK 21
- Apache Maven 3.9 or later

Check that Java and Maven are available:

```powershell
java -version
mvn -version
```

## Run

From the project directory, start the application with:

```powershell
mvn javafx:run
```

The application connects to the configured MySQL database when available and falls back to a local SQLite database (`attendx.db`) when MySQL cannot be reached.

## Database configuration

Database settings are read from `src/main/resources/config/database.properties`. Configure the MySQL URL, username, and password for your environment. Keep real credentials private; do not commit them to source control.

The application initializes its tables and seed data at startup. SQL setup and seed scripts are also available in the `sql/` directory.

## Build and test

Run the test suite:

```powershell
mvn test
```

Build the executable package:

```powershell
mvn clean package
```

The shaded JAR is written to `target/attendx-1.0.0.jar`.

## Technology

- Java 21 and JavaFX
- MySQL with SQLite fallback
- Maven
- Apache POI for Excel exports and OpenPDF for PDF exports

