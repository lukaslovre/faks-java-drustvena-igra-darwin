# Project: Darwin's Journey (Java College Project)

## Context & Goal

This is a university project graded strictly on 5 learning outcomes: JavaFX, TCP/RMI Networking, Multithreading, XML (
DOM/SAX), and Reflection/Serialization.
**Do not over-engineer.** The goal is a simplified "Vertical Slice" demo. Use vanilla Java 25 and JavaFX.

## Educational Guidelines (JS/TS -> Java)

The developer is highly experienced in JavaScript/TypeScript but a beginner in Java.

- **Act as a mentor.** Explain Java concepts (e.g., Threads, Generics, strict OOP) using JS/TS analogies (e.g.,
  Promises, Event Loop, Interfaces).
- **Comment heavily.** Add educational comments to the code explaining *why* something is done the Java way.

## Critical Rules

- **< 200 lines per class:** Strictly enforced. Extract logic into Helper/Utility classes if approaching the limit.
- **No external libraries:** Use only standard Java (`java.net`, `java.rmi`, `javax.xml`) and JavaFX. No Spring, Gson,
  Jackson, etc.
- **SonarQube compliant:** Write clean code immediately (strict `private`/`public` access modifiers, no deep nesting,
  standard naming).
- **UI Thread Safety:** Background network/logic threads *must* use `Platform.runLater()` to touch JavaFX components.
- **Serialization:** All network DTOs must implement `Serializable` and declare `serialVersionUID`.
- **Concurrency:** Use `synchronized` on server state-mutating methods (e.g., `GameEngine.processMove()`).

## Entry Points & Run Commands

- `hr.tvz.darwin.server.ServerApp` - TCP 8080, RMI 1099
- `hr.tvz.darwin.client.ClientApp` - JavaFX UI

```bash
mvn compile javafx:run -Dexec.mainClass=hr.tvz.darwin.server.ServerApp  # server
mvn compile javafx:run -Dexec.mainClass=hr.tvz.darwin.client.ClientApp  # client x2
```

## Progressive Context (Pre-implementation Docs)

Read these files *only* when working on their respective phases.

| Document                                 | When to read                                                 |
|:-----------------------------------------|:-------------------------------------------------------------|
| `docs/Game Simplification.md`            | Before starting - understand the simplified game rules       |
| `docs/1 Architecture Blueprint.md`       | Phase 1 - system topology, network protocol, UI architecture |
| `docs/2 Domain Model & Class Diagram.md` | Phase 1 - DTOs, enums, reflection, serialization             |
| `docs/3 Network Protocol & API Spec.md`  | Phase 2-4 - TCP/RMI implementation details                   |
| `docs/4 XML Replay System Spec.md`       | Phase 6 - DOM/SAX implementation                             |
| `docs/Implementation Roadmap.md`         | Throughout - ordered task checklist                          |
