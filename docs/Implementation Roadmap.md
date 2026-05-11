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
- [ ] **Playtest:** You should now be able to click an island on Client A, the server validates it, and Client B's screen updates. (Boom, **Ishod 1 & 3** core done).

### Phase 5: Animations & Polish (The "CSS Transitions")
*Make it look like a game.*
- [ ] **Worker Animations:** In `AnimationHelper.java`, use `TranslateTransition` to slide the worker token to the island.
- [ ] **Async Wait:** Use a Virtual Thread to `Thread.sleep(1000)` after the animation, then update the worker's color (level up), then slide it back. (Boom, **Ishod 4** done).
- [ ] **Chat System:** Implement the `ChatMessageDTO` flow so players can type to each other.

### Phase 6: The "Side Quests" (XML & RMI)
*These are isolated features. Do them last so they don't break the core game.*
- [ ] **XML XSD:** Write `replay.xsd`.
- [ ] **XML DOM Writer (Server):** Write `DomXmlWriter.java`. Trigger it when a player reaches Level 5. Check if `replay.xml` generates correctly.
- [ ] **XML SAX Reader (Client):** Write `SaxReplayParser.java` and the Producer-Consumer queue. Link it to a "Load Replay" button on the UI. (Boom, **Ishod 5** done).
- [ ] **RMI Server:** Create `IDarwinArchive` and `DarwinArchiveImpl`. Bind it to JNDI on port 1099.
- [ ] **RMI Client:** Add a "Global Stats" button to the UI. On click, do a JNDI lookup and fetch the stats. (Boom, **Ishod 3** RMI done).

### Phase 7: Final QA & Defense Prep
- [ ] **Line Count Check:** Scan all files. If any file is >190 lines, extract a method into a new Helper class.
- [ ] **SonarQube Sweep:** Look at SonarLint warnings. Fix any remaining code smells.
- [ ] **Network Test:** Run the Server on your laptop. Run Client A on your laptop. Find a second PC (or a friend's laptop), connect to the same Wi-Fi, change `localhost` to your laptop's IP address, and run Client B. *This is exactly how you will demonstrate it to the professor.*
- [ ] **Git Check:** Ensure you have at least 15-20 commits with meaningful messages (e.g., "Added SAX parser for replay", "Fixed UI thread crash").
