# 3. Network Protocol & API Spec
**Project:** Darwin's Journey (Simplified Vertical Slice)
**Protocols:** TCP (Real-time Game Loop), RMI (Global Stats)

## 1. TCP Connection Lifecycle & Flow
The TCP architecture uses persistent sockets. The connection remains open for the duration of the match.

### 1.1 The Handshake Phase
1.  **Server Start:** The Server opens `ServerSocket` on port `8080` and waits.
2.  **Client Connects:** Client A connects. The Server spawns a `ClientHandler` Virtual Thread.
3.  **Identification:** The Server immediately sends `WelcomeDTO(1)` to Client A. Client A disables its UI (waiting for Player 2).
4.  **Game Start:** Client B connects. Server sends `WelcomeDTO(2)`. The Server then broadcasts the initial `GameStateDTO` to both clients. Client A's UI unlocks.

### 1.2 The Turn Loop Phase
1.  **Action:** Player 1 clicks an Island. The Client sends `MoveRequestDTO(playerId=1, workerId=0, targetIsland=ISABELA)`.
2.  **Validation:** The Server's `synchronized processMove()` checks if it's Player 1's turn and if Worker 0 has the required level.
3.  **Broadcast:** 
    *   *If Valid:* Server updates state and broadcasts the new `GameStateDTO` to *both* clients.
    *   *If Invalid:* Server sends `ErrorDTO("Worker level too low")` *only* to Player 1.
4.  **UI Update:** Clients receive the `GameStateDTO`, update their reactive bindings, and trigger the worker movement animation.

### 1.3 Disconnects & Edge Cases (Devil's Advocate Prevention)
*   **The Problem:** If Player 2 closes their window, Player 1 is stuck forever.
*   **The Solution:** The `ClientHandler` thread uses a `try-catch (IOException)`. If the catch block triggers, the Server immediately broadcasts an `ErrorDTO("Opponent disconnected. Game Over.")` to the remaining player and safely shuts down the game session.

## 2. TCP Payload Schema (DTOs)
All DTOs must declare a `serialVersionUID`. Without this, if you compile the Server and Client independently, Java might assume they are different classes and throw an `InvalidClassException`.

```java
// 1. Handshake
public record WelcomeDTO(int playerId) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
}

// 2. Updated Game State (Added winnerId to handle End Game logic)
// winnerId is 0 if the game is ongoing, 1 if P1 wins, 2 if P2 wins.
public record GameStateDTO(
    PlayerStateDTO player1, 
    PlayerStateDTO player2,
    int winnerId 
) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
}

// 3. Move Request
public record MoveRequestDTO(int playerId, int workerId, Island targetIsland) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
}
```

## 3. RMI & JNDI API Spec (The Darwin Archive)
RMI (Remote Method Invocation) is used exclusively for fetching all-time global statistics. It operates on a separate port (`1099` by default).

### 3.1 JNDI Registry Configuration
*   **Server-side:** On startup, the Server instantiates `DarwinArchiveImpl` and binds it to the RMI registry using JNDI: 
    `Context ctx = new InitialContext(); ctx.rebind("rmi://localhost:1099/DarwinArchive", archiveService);`
*   **Client-side:** When the user clicks the "Global Stats" tab, the Client performs a lookup:
    `IDarwinArchive archive = (IDarwinArchive) ctx.lookup("rmi://localhost:1099/DarwinArchive");`

### 3.2 Interface Definition
```java
public interface IDarwinArchive extends Remote {
    // Fetches the sum of all Botany, Zoology, and Geology points ever scored
    int getTotalGlobalResearchPoints() throws RemoteException;
    
    // Fetches the total number of completed matches on this server
    int getTotalGamesPlayed() throws RemoteException;
}
```
*Implementation Detail:* The Server's `DarwinArchiveImpl` will read/write these values to a simple `global_stats.txt` file whenever a game concludes, ensuring the data persists even if the Server restarts.
