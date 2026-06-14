# Implementation Roadmap

### Phase 0: The Infrastructure (Day 1)
*Think of this as `npm init` and setting up ESLint/Prettier.*
- [x] **Git Setup:** Initialize the Git repository. Create a `.gitignore` file (ignore `target/`, `.idea/`, `*.class`). Make your first commit.
- [x] **Maven Setup:** Create the `pom.xml`. Add dependencies for JavaFX and SonarQube. Set the compiler to Java 25.
- [x] **IDE Setup:** Install the **SonarLint** plugin. Create three Run Configurations in your IDE: `Run Server`, `Run Client 1`, `Run Client 2`.
- [x] **Package Structure:** Create the empty folders: `hr.tvz.darwin.shared`, `hr.tvz.darwin.server`, `hr.tvz.darwin.client`.

### Phase 1: The Domain & DTOs (The "TypeScript Interfaces")
*Build the shared language between Server and Client. No logic yet.*
- [x] **Enums & Reflection:** Create the `Track` and `Island` enums. Create the `@GameRule` annotation and apply it to the Islands.
- [x] **Reflection Utility:** Write a quick `ReflectionHelper.java` that reads the `@GameRule` annotations and prints them to the console (Boom, **Ishod 2** half-done).
- [x] **DTO Records:** Create `WelcomeDTO`, `WorkerDTO`, `PlayerStateDTO`, `GameStateDTO`, `MoveRequestDTO`, and `ErrorDTO`.
- [x] **Serialization Check:** Ensure *every* DTO implements `Serializable` and has a `private static final long serialVersionUID = 1L;`.

### Phase 2: The Server Skeleton (The "Node.js Backend")
*Build the server without any UI. Test it using console logs.*
- [x] **Game Engine Logic:** Create `GameEngine.java`. Implement the `synchronized processMove()` method. Write a simple `System.out.println` test to ensure a Level 1 worker can go to Isabela, but not San Cristobal.
- [x] **TCP Server:** Create `TcpServer.java` with a `ServerSocket` listening on port 8080.
- [x] **Client Handler:** Create the `ClientHandler.java` Virtual Thread. Implement the `switch` statement with Java 25 Pattern Matching to handle incoming DTOs.
- [x] **The Handshake:** Make the server assign Player 1 and Player 2 upon connection, and broadcast the initial `GameStateDTO`.

### Phase 3: The JavaFX Skeleton (The "HTML/DOM")
*Build the UI visually. Do not connect it to the network yet.*
- [x] **Scene Builder:** Open `Game.fxml` in Scene Builder. Drag and drop the UI: 3 Island buttons, 3 Research Track progress bars, and a Chat text area.
- [x] **Controller Mapping:** Create `GameController.java` and link the `@FXML` tags.
- [x] **Helpers:** Create `BindingHelper.java` and `AnimationHelper.java`. Write dummy methods to test updating a progress bar or moving a token.

### Phase 4: Connecting the Pipes (The "Fetch API")
*Connect Phase 2 and Phase 3.*
- [x] **TCP Client:** Create `TcpClient.java`. Connect to `localhost:8080`.
- [x] **Client Listener Thread:** Create a Virtual Thread in the client that listens for Server DTOs.
- [x] **State Binding:** When `GameStateDTO` arrives, route it to `BindingHelper.java`. **CRITICAL:** Use `Platform.runLater(() -> bindingHelper.updateUI(state));`.
- [x] **Sending Moves:** Hook up the UI Island buttons to send `MoveRequestDTO` to the server.
- [x] **Playtest:** You should now be able to click an island on Client A, the server validates it, and Client B's screen updates. (Boom, **Ishod 1 & 3** core done).

### Phase 5: Animations & Polish (The "CSS Transitions")
*Make it look like a game by separating Authoritative State from Visual State.*
- [x] **Chat System:** Implement the `ChatMessageDTO` flow so players can type to each other.
- [x] **Refactor BindingHelper (Coordinate Fix):** Remove the `updateWorkerPositions` logic from `BindingHelper`. Workers should permanently keep their `layoutX/Y` at their base, and we will only animate their `translateX/Y`.
- [x] **Animation Choreography:** In `AnimationHelper`, create a sequence that chains animations together: Travel to Island $\rightarrow$ Change Color (Level Up) $\rightarrow$ Travel Back to Base.
- [x] **Intercept State Updates:** In `GameController`, intercept incoming `GameStateDTO`s. Instead of instantly updating the UI, read `state.lastMove()`. Trigger the animation sequence first, and *only when it finishes*, update the Progress Bars and unlock the UI.
- [x] **UI Locking:** Disable Island buttons *immediately* upon click to prevent network spam. Ensure buttons are correctly re-enabled if the server rejects the move and returns an `ErrorDTO`, or when the turn animation finishes.
- [ ] Add custom UI instead of basic shapes and unstyled things. So finalize UI.

### Phase 6: The "Side Quests" (XML, RMI & Serialization)
*These features are architecturally isolated. Implementing them in this order ensures you can test each feature instantly.*

#### 1. Binary Serialization: "Save Game"
- [ ] **Save Logic (Client-side):** Add a "Save Game" button to the Client UI. Write a utility class `BinarySerializer.java` that uses `ObjectOutputStream` to write the current `GameStateDTO` to a file named `savegame.bin` in a local directory.
- [ ] **Load Logic (Client-side):** Add a "Load Game" button. It uses `ObjectInputStream` to read `savegame.bin` and passes the deserialized `GameStateDTO` to your `BindingHelper` to instantly update the UI.
- [ ] **Test:** Play a turn $\rightarrow$ Save $\rightarrow$ Play another turn $\rightarrow$ Load. The game should jump back to the saved state.

#### 2. XML Replay System - Part 1: Schema & Writing
*We must define the schema and write the XML ledger from the Server before we can replay it.*
- [ ] **XSD Schema:** Create `replay.xsd` in `src/main/resources/`. Define the strict types for `<Move>` attributes (e.g., checking that `playerId` is an integer).
- [ ] **DOM Writer (Server-side):** Create `DomXmlWriter.java` in the server package. 
    *   When the Server's `GameEngine` detects a game-over condition, pass the `moveHistory` list to this writer.
    *   Use `DocumentBuilderFactory` to write a validated `match_replay.xml` file.
- [ ] **Test:** Run a quick game to completion. Verify that `match_replay.xml` is successfully generated in your project root and that it matches the XSD schema.

#### 3. XML Replay System - Part 2: Reading & Replaying
*Now we parse the generated XML file on the Client and drive the UI animations.*
- [ ] **SAX Parser (Client-side):** Create `SaxReplayParser.java` which extends `DefaultHandler`. Override `startElement` to capture each `<Move>` tag and push it into a Java `Queue<MoveRequestDTO>`.
- [ ] **XSD Validation Helper:** Before parsing, use `SchemaFactory` to validate `match_replay.xml` against your `replay.xsd`.
- [ ] **Replay Engine (Client-side):** Add a "Watch Replay" button on the UI.
    *   When clicked, run the SAX Parser to populate the queue.
    *   Spawn a Virtual Thread that pulls moves from the Queue one-by-one, calls `Platform.runLater()` to trigger your Phase 5 slide animations, and pauses (`Thread.sleep(1500)`) between each move.
- [ ] **Test:** Click "Watch Replay" and watch your last game play itself back visually.

#### 4. The Darwin Archive: RMI & JNDI 
*Completely isolated from the game loop.*
- [ ] **RMI Interface:** Create `IDarwinArchive.java` extending `Remote` in your `shared` package. Define methods like `getTotalGamesPlayed()`.
- [ ] **RMI Implementation (Server-side):** Create `DarwinArchiveImpl.java` extending `UnicastRemoteObject`. It should read/write these simple counters to a local `global_stats.txt` file.
- [ ] **JNDI Binding (Server-side):** On server startup, create the RMI registry on port `1099` and bind your implementation using JNDI:
    ```java
    Context ctx = new InitialContext();
    ctx.rebind("rmi://localhost:1099/DarwinArchive", archiveService);
    ```
- [ ] **JNDI Lookup (Client-side):** Add a "Global Archive" tab or button on the Client UI. On click, spawn a background thread to look up the service and fetch the numbers:
    ```java
    IDarwinArchive archive = (IDarwinArchive) ctx.lookup("rmi://localhost:1099/DarwinArchive");
    ```
    Update the UI with the fetched statistics.
- [ ] **Test:** Run a game to completion (which increments the server's counter). Click "Global Archive" on the client and verify that the stats fetch and display correctly.

### Phase 7: Final QA & Defense Prep
- [ ] **Line Count Check:** Scan all files. If any file is >200 lines, extract a method into a new Helper class.
- [ ] **SonarQube Sweep:** Look at SonarLint warnings. Fix any remaining code smells.
