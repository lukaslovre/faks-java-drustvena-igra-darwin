package hr.tvz.darwin.server.core;

import hr.tvz.darwin.shared.Track;
import hr.tvz.darwin.shared.dto.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * The Brain of the server — validates moves and manages game state.
 * <p>
 * CONCURRENCY MODEL (Ishod 4):
 * The `synchronized` keyword on processMove() acts like a lock.
 * Only ONE thread can execute processMove() at a time, even if two
 * clients send moves in the exact same microsecond. This prevents
 * race conditions where two threads simultaneously modify currentState.
 * <p>
 * IMMUTABLE STATE:
 * Java records are immutable (like `const` objects in JS). When state
 * changes, we create a NEW GameStateDTO rather than mutating fields.
 * This is the same pattern as React/Redux reducers.
 */
public class GameEngine {

    /**
     * Turn history determines whose turn it is. Even = Player 1, Odd = Player 2.
     */
    private final List<MoveRequestDTO> moveHistory = new ArrayList<>();

    /**
     * The authoritative game state — broadcast to all clients after every move.
     * volatile ensures cross-thread visibility: when one thread updates the
     * reference inside a synchronized block, other threads see the new value
     * immediately (no stale CPU cache reads).
     */
    private volatile GameStateDTO currentState;

    /**
     * Callback invoked after every valid move with the new game state.
     * The network layer subscribes to this to broadcast state changes.
     */
    private Consumer<GameStateDTO> onStateChanged;

    /**
     * Total samples collected across all games (for RMI Darwin Archive).
     * AtomicInteger handles cross-instance thread safety without needing
     * a static synchronized lock. Two GameEngine instances can safely
     * increment these counters in parallel.
     */
    private static final AtomicInteger totalGlobalSamples = new AtomicInteger(0);

    /**
     * Total games played on this server (for RMI Darwin Archive).
     */
    private static final AtomicInteger totalGamesPlayed = new AtomicInteger(0);

    /**
     * Win condition: first player to reach this level on any track wins.
     */
    private static final int WIN_LEVEL = 5;

    public GameEngine() {
        this.currentState = createInitialState();
    }

    /**
     * Registers a callback to be notified when the game state changes.
     * This decouples the domain layer from the network layer.
     */
    public void setOnStateChanged(Consumer<GameStateDTO> onStateChanged) {
        this.onStateChanged = onStateChanged;
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
                0,
                null
        );
    }

    /**
     * Returns which player's turn it is.
     * Reads directly from the immutable GameStateDTO (which tracks activePlayerId
     * after every state transition) rather than deriving it from moveHistory size.
     * This is a true Redux pattern — the state object IS the source of truth.
     */
    public int getActivePlayerId() {
        return currentState.activePlayerId();
    }

    /**
     * Returns the current game state (for testing).
     */
    public GameStateDTO getCurrentState() {
        return currentState;
    }

    /**
     * Returns an unmodifiable snapshot of the move history.
     * Used by DomXmlWriter (Phase 6) to generate XML replay files.
     * <p>
     * DEFENSIVE COPY:
     * We synchronize on 'this' to ensure we don't copy while a move is being
     * added inside processMove(). The returned wrapper prevents the XML writer
     * from accidentally mutating the server's source-of-truth list.
     */
    public List<MoveRequestDTO> getMoveHistory() {
        synchronized (this) {
            return Collections.unmodifiableList(new ArrayList<>(moveHistory));
        }
    }

    /**
     * THE MAIN GAME LOGIC — validates and applies a move.
     * <p>
     * CONCURRENCY DESIGN (Ishod 4):
     * The synchronized block is NARROWLY scoped to only protect memory mutation
     * (state update, history append). The network callback onStateChanged is
     * called OUTSIDE the lock. This prevents a slow network client from
     * blocking the entire game engine (the "alien method" anti-pattern).
     *
     * @param request The move request from a client
     * @throws InvalidMoveException if the move violates game rules
     */
    public void processMove(MoveRequestDTO request) throws InvalidMoveException {
        // Capture the new state reference to pass to the callback AFTER releasing the lock
        GameStateDTO stateToBroadcast;

        synchronized (this) {
            // If game over don't process further requests
            if (currentState.winnerId() != 0) {
                throw new InvalidMoveException("The game is already over!");
            }

            // 1. TURN VALIDATION: Is it this player's turn?
            if (request.playerId() != getActivePlayerId()) {
                throw new InvalidMoveException("It is not your turn!");
            }

            // 2. WORKER VALIDATION: Get the worker's current level from state
            WorkerDTO worker = getWorker(request.playerId(), request.workerId());
            if (worker == null) {
                throw new InvalidMoveException("Invalid worker ID.");
            }

            // 3. ISLAND VALIDATION: Can this worker level reach this island?
            if (worker.level() < request.targetIsland().requiredLevel) {
                throw new InvalidMoveException("Worker level too low for this island! Required: "
                        + request.targetIsland().requiredLevel + ", Your level: " + worker.level());
            }

            // 4. UPDATE STATE: Create a new GameStateDTO with the changes applied
            currentState = generateNewState(currentState, request);
            moveHistory.add(request);

            // Capture the reference for broadcast outside the lock
            stateToBroadcast = currentState;

            // 5. WIN CONDITION CHECK: Did someone reach Level 5?
            if (currentState.winnerId() != 0) {
                // AtomicInteger is thread-safe across instances — no static lock needed
                totalGamesPlayed.incrementAndGet();
                totalGlobalSamples.addAndGet(calculateTotalSamples(currentState));
                System.out.println("Game over! Winner: Player " + currentState.winnerId());
                // XML logging will be triggered here in Phase 6
            }
        }

        // 6. CALLBACK: Notify the network layer OUTSIDE the lock.
        // Network I/O (socket writes) can block. Holding the lock during I/O
        // would freeze the game engine.
        if (onStateChanged != null) {
            onStateChanged.accept(stateToBroadcast);
        }
    }

    /**
     * Derives a NEW GameStateDTO from the old one after applying a move.
     * Like a Redux reducer: (oldState, action) => newState
     */
    private GameStateDTO generateNewState(GameStateDTO oldState, MoveRequestDTO move) {
        boolean isPlayer1 = move.playerId() == 1;

        // 1. Calculate the new player states.
        // The player who moved gets an updated state via applyWorkerAction.
        // The other player's state stays exactly the same.
        PlayerStateDTO newP1 = isPlayer1 ? applyWorkerAction(oldState.player1(), move) : oldState.player1();
        PlayerStateDTO newP2 = !isPlayer1 ? applyWorkerAction(oldState.player2(), move) : oldState.player2();

        // 2. Determine whose turn is next
        int nextPlayerId = isPlayer1 ? 2 : 1;

        // 3. Check if this move resulted in a win
        int winnerId = checkWinner(newP1, newP2);

        // 4. Assemble and return the final immutable state
        return new GameStateDTO(newP1, newP2, nextPlayerId, winnerId, move);
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
     * Checks if either player has won (reached Level 5 on any track).
     *
     * @return 0 if no winner, 1 if Player 1 wins, 2 if Player 2 wins
     */
    private int checkWinner(PlayerStateDTO p1, PlayerStateDTO p2) {
        // Check Player 1
        if (p1.botany() >= WIN_LEVEL || p1.zoology() >= WIN_LEVEL || p1.geology() >= WIN_LEVEL) {
            return 1;
        }
        // Check Player 2
        if (p2.botany() >= WIN_LEVEL || p2.zoology() >= WIN_LEVEL || p2.geology() >= WIN_LEVEL) {
            return 2;
        }
        return 0;
    }

    /**
     * Gets a specific worker from the current state.
     *
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

    /**
     * Calculates total samples (sum of all track points) for RMI Darwin Archive.
     */
    private static int calculateTotalSamples(GameStateDTO state) {
        return state.player1().botany() + state.player1().zoology() + state.player1().geology()
                + state.player2().botany() + state.player2().zoology() + state.player2().geology();
    }

    /**
     * Returns total global samples (for RMI).
     */
    public static int getTotalGlobalSamples() {
        return totalGlobalSamples.get();
    }

    /**
     * Returns total games played (for RMI).
     */
    public static int getTotalGamesPlayed() {
        return totalGamesPlayed.get();
    }

    /**
     * Resets the game state for a new match.
     * Notice we do NOT reset totalGlobalSamples or totalGamesPlayed,
     * as those must persist across multiple matches for the RMI Archive.
     */
    public synchronized void reset() {
        moveHistory.clear();
        currentState = createInitialState();
        System.out.println("GameEngine state has been reset.");
    }
}