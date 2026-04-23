# 1. Architecture Blueprint
**Project:** Darwin's Journey (Simplified Vertical Slice)
**Platform:** Java 25, JavaFX
**Architecture:** Authoritative Client-Server (Single Maven Project)

## 1. High-Level System Topology
The application will utilize an **Authoritative Client-Server** topology. 
*   **The Server** is the single source of truth. It holds the game rules, validates all moves, maintains the official game state, and broadcasts updates.
*   **The Clients** are "dumb" terminals. They capture user input, send requests to the server, and reactively render whatever state the server tells them is current.

To avoid the configuration nightmares of multi-module Maven setups, the codebase will be structured as a **Single Maven Project**. Separation of concerns will be handled strictly through Java packages (`hr.tvz.darwin.server`, `hr.tvz.darwin.client`, `hr.tvz.darwin.shared`). The project will have two entry points (two `main` methods): one to launch the Server, and one to launch the Client UI.

## 2. Network Communication Protocol
The system divides network traffic into two distinct channels based on their use cases (TCP for real-time, RMI for global fetching).

### 2.1 Primary Game Loop (TCP Sockets)
All real-time gameplay (worker placement, state updates, and chat) will occur over persistent TCP Socket connections.
*   **Payload Format (DTO Pattern):** We will strictly use Java 25 `records` implementing `Serializable` for all network traffic.
*   **The Handshake:** When a Client connects, the Server immediately sends a `WelcomeDTO(int yourPlayerId)`. The Client saves this ID to know if it is Player 1 or Player 2 (which dictates UI rendering and turn logic).
*   **Data Flow:** The Client sends a `MoveRequestDTO`. The Server validates it. If valid, the Server updates its internal model and broadcasts a new `GameStateDTO` to all connected clients.

### 2.2 Global Services (Java RMI & JNDI)
Remote Method Invocation (RMI) will be isolated to a non-real-time side feature: **The Darwin Archive (Global Highscores)**.
*   The Server will host an RMI service that tracks all-time statistics across all played matches (e.g., "Total Samples Collected Globally").
*   The Server registers this service using **JNDI** (Java Naming and Directory Interface).
*   The Client UI will feature a "Darwin Archive" tab. Clicking it performs a JNDI lookup, connects via RMI, fetches the global stats asynchronously, and displays them.

## 3. UI Architecture & State Management
JavaFX UI code is notoriously verbose. To prevent the main UI controller from exceeding the 200-line limit, we will employ a **God Controller + Helpers** pattern combined with **Reactive Bindings**.

### 3.1 The Controller Split
There will be a single main FXML file (`Game.fxml`) and a single `GameController.java`. `GameController` will contain **zero business logic**. It will act purely as a dependency injector and router.
*   **`GameController`**: Holds the `@FXML` references and passes them to helpers.
*   **`BindingHelper`**: Responsible for linking the UI elements to the reactive state.
*   **`AnimationHelper`**: Responsible for triggering JavaFX Transitions (e.g., moving the worker token).
*   **`NetworkHelper`**: Handles sending button clicks to the TCP output stream.

### 3.2 Reactive State Management (Unpacking DTOs)
JavaFX UI components rely on `IntegerProperty` and `StringProperty` for reactive updates, but these properties are **not serializable** and cannot be sent over TCP. 
*   The `GameStateDTO` sent by the server uses standard primitive `int` values.
*   When a `GameStateDTO` arrives, the `BindingHelper` will "unpack" it and update the local JavaFX properties (e.g., `localBotanyProperty.set(dto.player1().botany())`). The UI will then automatically re-render based on these updated properties.

## 4. Concurrency, Threading & Synchronization
To satisfy Ishod 4, we must handle background processing and protect the server's state from multi-threaded race conditions.

*   **Virtual Threads:** Both the Server and the Client will use Java 25 Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`) for infinite network listening loops and simulating worker travel delays.
*   **UI Thread Safety:** Background threads will strictly use `Platform.runLater()` to dispatch visual updates back to the JavaFX Application Thread.
*   **Server Synchronization:** Because the Server spawns a separate Thread for every connected Client, two clients might try to send a move at the exact same millisecond. The Server's state mutation methods will use the `synchronized` keyword to ensure only one thread can modify the game state at a time.

## 5. Data Persistence & Replay System (XML)
To fulfill the XML requirements (Ishod 5), the application will implement an **Event Sourcing** architecture for the Replay System.

### 5.1 XML Event Logging (DOM - Writing)
The XML file will act as a ledger of actions. 
*   At the end of the game, the Server uses a **DOM Parser** (Document Object Model) to build an XML tree in memory representing the entire match history, which is then written to a `.xml` file.
*   *Example Structure:* `<Turn id="1" player="1" action="PLACE_WORKER" target="ISABELA" />`

### 5.2 Replay Engine (SAX - Reading)
When a user clicks "Replay Last Game" on the Client:
1.  An XSD (XML Schema Definition) file is used to validate the structure.
2.  The Client uses a **SAX Parser** (Simple API for XML) to read the file sequentially. SAX is an event-driven reader, making it perfect for processing turns one by one.
3.  A Virtual Thread processes the SAX events, programmatically triggering the `AnimationHelper` with a 1.5-second delay between each move to visually recreate the match.

## 6. Project Directory Structure Overview
```text
darwin-journey-project/
├── pom.xml
└── src/main/java/hr/tvz/darwin/
    ├── shared/
    │   ├── dto/           (WelcomeDTO, MoveRequestDTO, GameStateDTO)
    │   └── rmi/           (IDarwinArchive remote interface)
    ├── server/
    │   ├── core/          (GameEngine, Validator)
    │   ├── network/       (TCPServer, ClientHandler)
    │   ├── rmi/           (DarwinArchiveImpl)
    │   └── xml/           (DomEventLogger)
    └── client/
        ├── ui/            (GameController, FXML files)
        ├── helpers/       (BindingHelper, AnimationHelper)
        ├── network/       (TCPClient)
        └── replay/        (SaxReplayEngine)
```

---

