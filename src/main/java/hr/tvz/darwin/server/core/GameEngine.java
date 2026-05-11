package hr.tvz.darwin.server.core;

import hr.tvz.darwin.server.network.ClientHandler;
import hr.tvz.darwin.server.network.TcpServer;
import hr.tvz.darwin.shared.Track;
import hr.tvz.darwin.shared.dto.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The Brain of the server — validates moves and manages game state.
 *
 * CONCURRENCY MODEL (Ishod 4):
 * The `synchronized` keyword on processMove() acts like a lock.
 * Only ONE thread can execute processMove() at a time, even if two
 * clients send moves in the exact same microsecond. This prevents
 * race conditions where two threads simultaneously modify currentState.
 *
 * IMMUTABLE STATE:
 * Java records are immutable (like `const` objects in JS). When state
 * changes, we create a NEW GameStateDTO rather than mutating fields.
 * This is the same pattern as React/Redux reducers.
 */
public class GameEngine {

    /** Turn history determines whose turn it is. Even = Player 1, Odd = Player 2. */
    private final List<MoveRequestDTO> moveHistory = new ArrayList<>();

    /** The authoritative game state — broadcast to all clients after every move. */
    private GameStateDTO currentState;

    /** Reference back to TcpServer so we can broadcast state changes. */
    private TcpServer server;

    /** Total samples collected across all games (for RMI Darwin Archive). */
    private static int totalGlobalSamples = 0;

    /** Total games played on this server (for RMI Darwin Archive). */
    private static int totalGamesPlayed = 0;

    /** Win condition: first player to reach this level on any track wins. */
    private static final int WIN_LEVEL = 5;

    public GameEngine() {
        this.currentState = createInitialState();
    }

    /** Sets the server reference so the engine can broadcast to all clients. */
    public void setServer(TcpServer server) {
        this.server = server;
    }

    /**
     * Creates the starting state — both players at 0 points, both workers at level 1.
     * Records are immutable, so we create a new instance for the initial state.
     */
    private static GameStateDTO createInitialState() {
        return new GameStateDTO(
                new PlayerStateDTO(0, 0, 0,
                        new WorkerDTO(0, 1, null),
                        new WorkerDTO(1, 1, null)),
                new PlayerStateDTO(0, 0, 0,
                        new WorkerDTO(0, 1, null),
                        new WorkerDTO(1, 1, null)),
                1,
                0
        );
    }

    /**
     * Returns which player's turn it is based on moveHistory size.
     * Even number of moves → Player 1's turn (moves.size() % 2 == 0)
     * Odd number of moves  → Player 2's turn
     */
    public int getActivePlayerId() {
        return (moveHistory.size() % 2 == 0) ? 1 : 2;
    }

    /** Returns the current game state (for testing). */
    public GameStateDTO getCurrentState() {
        return currentState;
    }

    /**
     * THE MAIN GAME LOGIC — synchronized to prevent race conditions.
     *
     * Java synchronized acts like a mutex lock. If Thread A (Player 1)
     * is inside this method, Thread B (Player 2) must wait outside until
     * Thread A finishes. This ensures only one thread modifies currentState
     * at a time, preventing memory corruption.
     *
     * @param request The move request from a client
     * @param sender  The ClientHandler that sent this (to send errors back)
     */
    public synchronized void processMove(MoveRequestDTO request, ClientHandler sender) {
        // TODO: Is this missing game over validation? Check if winner exists? Anything else?

        // 1. TURN VALIDATION: Is it this player's turn?
        // If Player 1 sends a move but it's Player 2's turn, reject it.
        if (request.playerId() != getActivePlayerId()) {
            sender.send(new ErrorDTO("It is not your turn!"));
            return;
        }

        // 2. WORKER VALIDATION: Get the worker's current level from state
        WorkerDTO worker = getWorker(request.playerId(), request.workerId());
        if (worker == null) {
            sender.send(new ErrorDTO("Invalid worker ID."));
            return;
        }

        // 3. ISLAND VALIDATION: Can this worker level reach this island?
        // Island.requiredLevel is the minimum level needed to perform the action.
        if (worker.level() < request.targetIsland().requiredLevel) {
            sender.send(new ErrorDTO("Worker level too low for this island! Required: "
                    + request.targetIsland().requiredLevel + ", Your level: " + worker.level()));
            return;
        }

        // 4. UPDATE STATE: Create a new GameStateDTO with the changes applied
        // This follows the immutable state pattern (like Redux reducers).
        // We derive the NEW state from the OLD state rather than mutating.
        currentState = generateNewState(currentState, request);
        moveHistory.add(request);

        // 5. WIN CONDITION CHECK: Did someone reach Level 5?
        if (currentState.winnerId() != 0) {
            // Game is over — increment global stats for Darwin Archive (RMI)
            totalGamesPlayed++;
            totalGlobalSamples += calculateTotalSamples(currentState);
            System.out.println("Game over! Winner: Player " + currentState.winnerId());
            // XML logging will be triggered here in Phase 6
        }

        // 6. BROADCAST: Send the new state to ALL connected clients
        if (server != null) {
            server.broadcast(currentState);
        }
    }

    /**
     * Derives a NEW GameStateDTO from the old one after applying a move.
     * Like a Redux reducer: (oldState, action) => newState
     *
     * Rules of our simplified game:
     * 1. Worker goes to the island (currently working — track increases by 1)
     * 2. Worker levels up (level 1→2, 2→3)
     * 3. If worker was already level 3, it stays at level 3
     * 4. Player's track for that island's reward increases by 1
     */
    private GameStateDTO generateNewState(GameStateDTO oldState, MoveRequestDTO move) {
        boolean isPlayer1 = move.playerId() == 1;
        PlayerStateDTO oldPlayer = isPlayer1 ? oldState.player1() : oldState.player2();
        PlayerStateDTO newPlayer = applyWorkerAction(oldPlayer, move);

        int nextPlayer = (move.playerId() == 1) ? 2 : 1;
        int winnerId = checkWinner(newState(move, isPlayer1, newPlayer, oldState), move.playerId());

        return new GameStateDTO(
                isPlayer1 ? newPlayer : oldState.player1(),
                isPlayer1 ? oldState.player2() : newPlayer,
                nextPlayer,
                winnerId
        );
    }

    private GameStateDTO newState(MoveRequestDTO move, boolean isPlayer1, PlayerStateDTO newPlayer, GameStateDTO oldState) {
        return isPlayer1
                ? new GameStateDTO(newPlayer, oldState.player2(), 0, 0)
                : new GameStateDTO(oldState.player1(), newPlayer, 0, 0);
    }

    /**
     * Applies a worker action to a player's state.
     * Returns a NEW PlayerStateDTO (records are immutable, no mutation).
     */
    private PlayerStateDTO applyWorkerAction(PlayerStateDTO player, MoveRequestDTO move) {
        // Determine which worker is being used
        WorkerDTO worker0 = player.worker0();
        WorkerDTO worker1 = player.worker1();
        WorkerDTO targetWorker = (move.workerId() == 0) ? worker0 : worker1;

        // Calculate new worker level (level up, cap at 3)
        int newLevel = Math.min(targetWorker.level() + 1, 3);

        // Create the updated worker
        WorkerDTO newWorker = new WorkerDTO(targetWorker.id(), newLevel, null);

        // Determine which track to increment based on the island's reward
        Track reward = move.targetIsland().reward;
        int newBotany = player.botany() + (reward == Track.BOTANY ? 1 : 0);
        int newZoology = player.zoology() + (reward == Track.ZOOLOGY ? 1 : 0);
        int newGeology = player.geology() + (reward == Track.GEOLOGY ? 1 : 0);

        // Return new PlayerStateDTO with updated values (replace the moved worker)
        return new PlayerStateDTO(
                newBotany,
                newZoology,
                newGeology,
                (move.workerId() == 0) ? newWorker : worker0,
                (move.workerId() == 0) ? worker1 : newWorker
        );
    }

    /**
     * Checks if the active player has won (reached Level 5 on any track).
     * @return 0 if no winner, playerId (1 or 2) if someone won
     */
    private int checkWinner(GameStateDTO state, int activePlayerId) {
        // Check all three tracks for Player who just moved
        if (state.player1().botany() >= WIN_LEVEL
                || state.player1().zoology() >= WIN_LEVEL
                || state.player1().geology() >= WIN_LEVEL) {
            return 1;
        }
        if (state.player2().botany() >= WIN_LEVEL
                || state.player2().zoology() >= WIN_LEVEL
                || state.player2().geology() >= WIN_LEVEL) {
            return 2;
        }
        return 0;
    }

    /**
     * Gets a specific worker from the current state.
     * @param playerId 1 or 2
     * @param workerId 0 or 1
     * @return The WorkerDTO or null if invalid
     */
    private WorkerDTO getWorker(int playerId, int workerId) {
        PlayerStateDTO player = (playerId == 1) ? currentState.player1() : currentState.player2();
        return switch (workerId) {
            case 0 -> player.worker0();
            case 1 -> player.worker1();
            default -> null;
        };
    }

    /** Calculates total samples (sum of all track points) for RMI Darwin Archive. */
    private static int calculateTotalSamples(GameStateDTO state) {
        return state.player1().botany() + state.player1().zoology() + state.player1().geology()
                + state.player2().botany() + state.player2().zoology() + state.player2().geology();
    }

    /** Returns total global samples (for RMI). */
    public static int getTotalGlobalSamples() {
        return totalGlobalSamples;
    }

    /** Returns total games played (for RMI). */
    public static int getTotalGamesPlayed() {
        return totalGamesPlayed;
    }
}