## Project: Darwin's Journey

Java 25, JavaFX, Maven, Authoritative Client-Server.

## Entry Points

- `hr.tvz.darwin.server.ServerApp` - TCP 8080, RMI 1099
- `hr.tvz.darwin.client.ClientApp` - JavaFX UI

## Run

```bash
mvn compile javafx:run -Dexec.mainClass=hr.tvz.darwin.server.ServerApp  # server
mvn compile javafx:run -Dexec.mainClass=hr.tvz.darwin.client.ClientApp  # client x2
```

## Docs (read before each phase)

| Doc                                      | When to read                                                 |
|------------------------------------------|--------------------------------------------------------------|
| `docs/Game Simplification.md`            | Before starting - understand the simplified game rules       |
| `docs/1 Architecture Blueprint.md`       | Phase 1 - system topology, network protocol, UI architecture |
| `docs/2 Domain Model & Class Diagram.md` | Phase 1 - DTOs, enums, reflection, serialization             |
| `docs/3 Network Protocol & API Spec.md`  | Phase 2-4 - TCP/RMI implementation details                   |
| `docs/4 XML Replay System Spec.md`       | Phase 6 - DOM/SAX implementation                             |
| `docs/Implementation Roadmap.md`         | Throughout - ordered task checklist                          |

## Critical Rules

- **200 line limit per class**
- **All DTOs need `serialVersionUID`**
- Use `synchronized` on `GameEngine.processMove()`
- Background threads use `Platform.runLater()` for UI