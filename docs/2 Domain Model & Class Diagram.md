# 2. Domain Model & Class Diagram
**Project:** Darwin's Journey (Simplified Vertical Slice)
**Architecture:** Nested DTOs, Enum-driven Rules, Derived State

## 1. Static Game Rules & Reflection (Ishod 2)
Instead of hardcoding game logic in methods, the rules of the board are defined as Java `Enums`. To satisfy the Reflection API requirement, we will create a custom annotation `@GameRule` that tags these enums. A dedicated utility class will use Reflection to read these tags and generate a "Help Manual" in the console.

```java
// 1. The Custom Annotation
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GameRule {
    String description();
}

// 2. The Tracks
public enum Track { BOTANY, ZOOLOGY, GEOLOGY }

// 3. The Islands (The Rules Engine)
public enum Island {
    @GameRule(description = "Requires Level 1. Rewards +1 Botany.")
    ISABELA(1, Track.BOTANY),
    
    @GameRule(description = "Requires Level 2. Rewards +1 Zoology.")
    SANTA_CRUZ(2, Track.ZOOLOGY),
    
    @GameRule(description = "Requires Level 3. Rewards +1 Geology.")
    SAN_CRISTOBAL(3, Track.GEOLOGY);

    public final int requiredLevel;
    public final Track reward;

    Island(int requiredLevel, Track reward) {
        this.requiredLevel = requiredLevel;
        this.reward = reward;
    }
}
```

## 2. Network DTOs (Data Transfer Objects)

All network communication over TCP will use Java 25 `records` that implement `Serializable`. The state is deeply nested for clean OOP representation.
### 2.1 Server -> Client (State & Handshake)
```java
// Handshake: Sent immediately upon connection so the client knows its identity
public record WelcomeDTO(int playerId) implements Serializable {}

// Worker knows its own location (Null/None if at base)
public record WorkerDTO(int id, int level, Island currentIsland) implements Serializable {}

// Player holds their track progress and their two workers
public record PlayerStateDTO(
    int botany, 
    int zoology, 
    int geology, 
    WorkerDTO worker0, 
    WorkerDTO worker1
) implements Serializable {}

// The Master State Object
// winnerId: 0 = ongoing, 1 = Player 1 wins, 2 = Player 2 wins
public record GameStateDTO(
    PlayerStateDTO player1, 
    PlayerStateDTO player2,
    int winnerId
) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
}
```

### 2.2 Client -> Server (Requests)
```java
// Client -> Server: "I want to move this worker to this island"
public record MoveRequestDTO(int playerId, int workerId, Island targetIsland) implements Serializable {}

// Client -> Server -> Clients: Chat system
public record ChatMessageDTO(int playerId, String message) implements Serializable {}

// Server -> Client: Feedback when InvalidMoveException is thrown
public record ErrorDTO(String errorMessage) implements Serializable {}
```

## 3. Network Multiplexing (Java 25 Pattern Matching)
Because we send different records over the same TCP stream, the receiver (both Client and Server) will use Java 25 Pattern Matching in their listener threads to route the data without messy casting.

```java
// Example of the Client's incoming network loop
Object payload = objectInputStream.readObject();

switch (payload) {
    case WelcomeDTO welcome -> this.myPlayerId = welcome.playerId();
    case GameStateDTO state -> Platform.runLater(() -> bindingHelper.updateUI(state));
    case ChatMessageDTO chat -> Platform.runLater(() -> chatHelper.appendMessage(chat));
    case ErrorDTO error -> Platform.runLater(() -> uiHelper.showErrorPopup(error.errorMessage()));
    default -> System.err.println("Unknown payload received");
}
```

## 4. Derived State, Turn Management & Synchronization
The Server does not store a `currentTurn` variable. Instead, it maintains a `List<MoveRequestDTO> moveHistory`. This list acts as the data source for the XML Logger and mathematically determines whose turn it is. 

To satisfy **Ishod 4 (Synchronization)**, the `processMove` method uses the `synchronized` keyword. This guarantees that if Player 1 and Player 2 send a network payload at the exact same millisecond, the Server will process them sequentially, preventing array corruption or race conditions.

```java
public class GameEngine {
    private final List<MoveRequestDTO> moveHistory = new ArrayList<>();
    private GameStateDTO currentState; // Updated after every valid move

    // Derived Turn Logic
    public int getActivePlayerId() {
        // If 0 moves have been made, it's Player 1's turn (Even = P1, Odd = P2)
        return (moveHistory.size() % 2 == 0) ? 1 : 2;
    }

    // SYNCHRONIZATION: Prevents race conditions from multi-threaded TCP ClientHandlers
    public synchronized void processMove(MoveRequestDTO request) throws InvalidMoveException {
        if (request.playerId() != getActivePlayerId()) {
            throw new InvalidMoveException("It is not your turn!");
        }
        // ... validate worker level against Island enum ...
        // ... update state, add to history ...
    }
}
```

## 5. RMI Interface (Ishod 3)
The RMI service is strictly separated from the game loop. It uses primitive return types to fetch global statistics from the Server's "Darwin Archive".

```java
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IDarwinArchive extends Remote {
    // Returns the total number of samples collected across all games ever played
    int getTotalGlobalSamples() throws RemoteException;
    
    // Returns the total number of matches played on this server
    int getTotalGamesPlayed() throws RemoteException;
}
```

## 6. Class Responsibility Map (Defeating the 200-Line Limit)
By structuring the domain this way, we guarantee no class exceeds 200 lines. 

*   **Data Classes (Records):** ~1-5 lines each.
*   **Enums:** ~20 lines each.
*   **`GameEngine.java`**: Handles validation and state updates. Because rules are in the `Island` enum, validation is just a few lines. (~80 lines).
*   **`TcpServer.java`**: Handles accepting sockets and routing them to `ClientHandler` threads. (~50 lines).
*   **`ClientHandler.java`**: The `switch` statement for incoming server requests. (~60 lines).
*   **`GameController.java` (Client):** Holds `@FXML` tags and passes them to helpers. (~50 lines).
*   **`BindingHelper.java` (Client):** Unpacks the nested `GameStateDTO` into JavaFX properties. (~100 lines).