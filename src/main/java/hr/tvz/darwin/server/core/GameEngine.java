package hr.tvz.darwin.server.core;

import hr.tvz.darwin.shared.Track;
import hr.tvz.darwin.shared.dto.GameStateDTO;
import hr.tvz.darwin.shared.dto.MoveRequestDTO;
import hr.tvz.darwin.shared.dto.PlayerStateDTO;
import hr.tvz.darwin.shared.dto.WorkerDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/** Validates moves and owns the server's authoritative game state. */
public class GameEngine {
    private static final Logger LOGGER = Logger.getLogger(GameEngine.class.getName());
    private static final int WIN_LEVEL = 5;

    private final List<MoveRequestDTO> moveHistory = new ArrayList<>();

    // State is replaced, not mutated. volatile makes unsynchronized reads see
    // the latest state published by processMove() or reset().
    private volatile GameStateDTO currentState;
    private Consumer<GameStateDTO> onStateChanged;

    public GameEngine() {
        currentState = createInitialState();
    }

    public void setOnStateChanged(Consumer<GameStateDTO> onStateChanged) {
        this.onStateChanged = onStateChanged;
    }

    public int getActivePlayerId() {
        return currentState.activePlayerId();
    }

    public GameStateDTO getCurrentState() {
        return currentState;
    }

    /** Returns a stable snapshot while the live history remains private. */
    public synchronized List<MoveRequestDTO> getMoveHistory() {
        return List.copyOf(moveHistory);
    }

    /**
     * Applies one move atomically. The callback runs after releasing the lock
     * because socket writes may block other client threads.
     */
    public void processMove(MoveRequestDTO request) throws InvalidMoveException {
        GameStateDTO stateToBroadcast;
        synchronized (this) {
            validateMove(request);
            currentState = generateNewState(currentState, request);
            moveHistory.add(request);
            stateToBroadcast = currentState;
            if (currentState.winnerId() != 0) {
                LOGGER.info(() -> "Game over! Winner: Player " + currentState.winnerId());
            }
        }

        if (onStateChanged != null) {
            onStateChanged.accept(stateToBroadcast);
        }
    }

    private void validateMove(MoveRequestDTO request) throws InvalidMoveException {
        if (currentState.winnerId() != 0) {
            throw new InvalidMoveException("The game is already over!");
        }
        if (request.playerId() != getActivePlayerId()) {
            throw new InvalidMoveException("It is not your turn!");
        }

        WorkerDTO worker = getWorker(request.playerId(), request.workerId());
        if (worker.level() < request.targetIsland().requiredLevel) {
            throw new InvalidMoveException("Worker level too low for this island! Required: "
                    + request.targetIsland().requiredLevel + ", Your level: " + worker.level());
        }
    }

    private GameStateDTO generateNewState(GameStateDTO oldState, MoveRequestDTO move) {
        boolean isPlayer1 = move.playerId() == 1;
        PlayerStateDTO newP1 = isPlayer1
                ? applyWorkerAction(oldState.player1(), move) : oldState.player1();
        PlayerStateDTO newP2 = isPlayer1
                ? oldState.player2() : applyWorkerAction(oldState.player2(), move);
        int nextPlayerId = isPlayer1 ? 2 : 1;
        int winnerId = checkWinner(newP1, newP2);
        return new GameStateDTO(newP1, newP2, nextPlayerId, winnerId, move);
    }

    private PlayerStateDTO applyWorkerAction(PlayerStateDTO player, MoveRequestDTO move) {
        WorkerDTO worker0 = player.worker0();
        WorkerDTO worker1 = player.worker1();
        WorkerDTO targetWorker = move.workerId() == 0 ? worker0 : worker1;
        WorkerDTO updatedWorker = new WorkerDTO(
                targetWorker.id(), Math.min(targetWorker.level() + 1, 3), null);

        Track reward = move.targetIsland().reward;
        return new PlayerStateDTO(
                player.botany() + (reward == Track.BOTANY ? 1 : 0),
                player.zoology() + (reward == Track.ZOOLOGY ? 1 : 0),
                player.geology() + (reward == Track.GEOLOGY ? 1 : 0),
                move.workerId() == 0 ? updatedWorker : worker0,
                move.workerId() == 0 ? worker1 : updatedWorker
        );
    }

    private int checkWinner(PlayerStateDTO player1, PlayerStateDTO player2) {
        if (hasWinningTrack(player1)) {
            return 1;
        }
        return hasWinningTrack(player2) ? 2 : 0;
    }

    private boolean hasWinningTrack(PlayerStateDTO player) {
        return player.botany() >= WIN_LEVEL
                || player.zoology() >= WIN_LEVEL
                || player.geology() >= WIN_LEVEL;
    }

    /** Returns a worker or throws when workerId is outside the protocol range. */
    private WorkerDTO getWorker(int playerId, int workerId) {
        PlayerStateDTO player = playerId == 1 ? currentState.player1() : currentState.player2();
        return switch (workerId) {
            case 0 -> player.worker0();
            case 1 -> player.worker1();
            default -> throw new IllegalArgumentException("Invalid workerId: " + workerId);
        };
    }

    public synchronized void reset() {
        moveHistory.clear();
        currentState = createInitialState();
        LOGGER.info("GameEngine state has been reset.");
    }

    private static GameStateDTO createInitialState() {
        return new GameStateDTO(createInitialPlayer(), createInitialPlayer(), 1, 0, null);
    }

    private static PlayerStateDTO createInitialPlayer() {
        return new PlayerStateDTO(0, 0, 0,
                new WorkerDTO(0, 1, null), new WorkerDTO(1, 1, null));
    }
}
