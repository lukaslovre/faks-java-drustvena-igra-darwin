# Darwin's Journey

Simplified two-player Java 25 and JavaFX implementation of Darwin's Journey.

## Requirements

- JDK 25
- Maven

## Running the application

Open three terminals in the project directory.

Start the TCP/RMI server first:

```bash
mvn compile exec:java -Dexec.mainClass=hr.tvz.darwin.server.ServerApp
```

Then start two client processes, one in each remaining terminal:

```bash
mvn javafx:run
```

The server listens for gameplay on TCP port 8080 and exposes the Darwin
Archive through RMI/JNDI on port 1099.

The small `hr.tvz.darwin.client.Main` launcher is intentional. It delegates to
the JavaFX `Application` class so Maven can construct the JavaFX module path
before the UI starts.

## Verification

```bash
mvn test
```
