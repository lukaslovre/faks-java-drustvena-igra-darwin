package hr.tvz.darwin.server.core;

import hr.tvz.darwin.server.network.ClientHandler;
import hr.tvz.darwin.server.network.TcpServer;
import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameEngine — validates game logic, turn management, and win detection.
 * <p>
 * TDD APPROACH:
 * We write these tests BEFORE knowing if the code works correctly.
 * Red: Tests fail initially.
 * Green: We implement just enough to pass.
 * Refactor: Clean up the implementation.
 * <p>
 * MOCKING STRATEGY:
 * We use a simple TestClientHandler instead of Mockito. This captures
 * what DTOs the engine tries to send back to clients, letting us verify
 * error cases without complex mock setups.
 */
class GameEngineTest {

    private GameEngine engine;
    private TestClientHandler player1Handler;
    private TestClientHandler player2Handler;

    /**
     * A real ClientHandler for testing — captures sent DTOs instead of sending over TCP.
     */
    static class TestClientHandler extends ClientHandler {
        private Object lastSentPayload;

        TestClientHandler(int playerId, GameEngine engine, TcpServer server) throws IOException {
            super(null, playerId, engine, server);
        }

        @Override
        public synchronized void send(Object payload) {
            this.lastSentPayload = payload;
        }

        public Object getLastSentPayload() {
            return lastSentPayload;
        }

        public ErrorDTO getLastError() {
            return (lastSentPayload instanceof ErrorDTO) ? (ErrorDTO) lastSentPayload : null;
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        engine = new GameEngine();
        TcpServer server = new TcpServer();
        engine.setServer(server);
        player1Handler = new TestClientHandler(1, engine, server);
        player2Handler = new TestClientHandler(2, engine, server);
    }

    // === Turn Management Tests ===

    @Test
    void gameStartsWithPlayer1Turn() {
        assertEquals(1, engine.getActivePlayerId());
        assertEquals(1, engine.getCurrentState().activePlayerId());
    }

    @Test
    void afterPlayer1Moves_itsPlayer2Turn() {
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA), player1Handler);

        assertEquals(2, engine.getActivePlayerId());
        assertEquals(2, engine.getCurrentState().activePlayerId());
    }

    @Test
    void afterPlayer2Moves_itsPlayer1TurnAgain() {
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA), player1Handler);
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA), player2Handler);

        assertEquals(1, engine.getActivePlayerId());
        assertEquals(1, engine.getCurrentState().activePlayerId());
    }

    // === Turn Validation Tests ===

    @Test
    void player2CannotMoveOnPlayer1Turn() {
        MoveRequestDTO move = new MoveRequestDTO(2, 0, Island.ISABELA);
        engine.processMove(move, player2Handler);

        ErrorDTO error = player2Handler.getLastError();
        assertNotNull(error, "Should send error when wrong player moves");
        assertEquals("It is not your turn!", error.errorMessage());
        assertEquals(0, engine.getCurrentState().player1().botany(), "State should not change");
    }

    @Test
    void player1CannotMoveOnPlayer2Turn() {
        // Player 1 moves first
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA), player1Handler);

        // Player 1 tries to move again (should fail)
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA), player1Handler);

        ErrorDTO error = player1Handler.getLastError();
        assertNotNull(error, "Should send error on wrong turn");
        assertEquals("It is not your turn!", error.errorMessage());
    }

    // === Worker Level Validation Tests ===

    @Test
    void level1WorkerCannotGoToSanCristobal() {
        // San Cristobal requires Level 3
        MoveRequestDTO move = new MoveRequestDTO(1, 0, Island.SAN_CRISTOBAL);
        engine.processMove(move, player1Handler);

        ErrorDTO error = player1Handler.getLastError();
        assertNotNull(error, "Should reject level 1 worker going to level 3 island");
        assertTrue(error.errorMessage().contains("too low"));
    }

    @Test
    void level1WorkerCanGoToIsabela() {
        MoveRequestDTO move = new MoveRequestDTO(1, 0, Island.ISABELA);
        engine.processMove(move, player1Handler);

        assertNull(player1Handler.getLastError(), "Should accept valid move");
    }

    @Test
    void level2WorkerCanGoToSantaCruz() {
        // First, level up worker 0 to level 2 by going to Isabela
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA), player1Handler);
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA), player2Handler);

        // Now worker 0 is level 2 — can go to Santa Cruz (requires level 2)
        engine.processMove(new MoveRequestDTO(1, 0, Island.SANTA_CRUZ), player1Handler);

        assertNull(player1Handler.getLastError(), "Should accept level 2 worker at Santa Cruz");
    }

    // === State Update Tests ===

    @Test
    void validMoveIncreasesPlayerTrack() {
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA), player1Handler);

        // Isabela rewards +1 Botany
        assertEquals(1, engine.getCurrentState().player1().botany(),
                "Player 1 should have 1 Botany after Isabela");
        assertEquals(0, engine.getCurrentState().player1().zoology(),
                "Player 1 should have 0 Zoology");
    }

    @Test
    void validMoveLevelsUpWorker() {
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA), player1Handler);

        // Worker 0 started at level 1, should now be level 2
        assertEquals(2, engine.getCurrentState().player1().worker0().level(),
                "Worker 0 should be level 2 after one move");
    }

    @Test
    void workerLevelCapsAt3() {
        // Level up worker 0 twice: 1 -> 2 -> 3
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA), player1Handler); // P1: worker0 = lvl 2
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA), player2Handler); // P2
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA), player1Handler); // P1: worker0 = lvl 3
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA), player2Handler); // P2
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA), player1Handler); // P1: worker0 = still 3 (capped)

        assertEquals(3, engine.getCurrentState().player1().worker0().level(),
                "Worker should cap at level 3");
    }

    @Test
    void differentIslandsRewardCorrespondingTracks() {
        // Both players go to Isabela (Botany) first to level up
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA), player1Handler);
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA), player2Handler);

        // Both should have 1 Botany
        assertEquals(1, engine.getCurrentState().player1().botany(), "P1 Isabela = Botany");
        assertEquals(1, engine.getCurrentState().player2().botany(), "P2 Isabela = Botany");

        // Both workers leveled up to 2
        assertEquals(2, engine.getCurrentState().player1().worker0().level(), "P1 worker leveled up");
        assertEquals(2, engine.getCurrentState().player2().worker0().level(), "P2 worker leveled up");
    }

    @Test
    void workerLevelingUpUnlocksHigherIslands() {
        // Round 1: P1 and P2 both go Isabela (level up to 2)
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA), player1Handler);
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA), player2Handler);

        // Round 2: P1 goes Santa Cruz (worker now level 2, can access)
        engine.processMove(new MoveRequestDTO(1, 0, Island.SANTA_CRUZ), player1Handler);

        // P1 should have 1 Botany (Isabela) and 1 Zoology (Santa Cruz)
        assertEquals(1, engine.getCurrentState().player1().botany(), "Isabela = Botany");
        assertEquals(1, engine.getCurrentState().player1().zoology(), "Santa Cruz = Zoology");
    }

    // === Win Condition Tests ===

    @Test
    void reaching5OnBotanyWins() {
        // Player 1 gets to 5 Botany by repeatedly sending worker to Isabela
        // Each trip to Isabela = +1 Botany
        for (int i = 0; i < 5; i++) {
            engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA), player1Handler);
            engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA), player2Handler);
        }

        assertEquals(1, engine.getCurrentState().winnerId(),
                "Player 1 should win by reaching 5 Botany");
        assertEquals(1, engine.getCurrentState().activePlayerId(),
                "Active player should be 1 after P2's last move (it's P1's turn)");
    }

    @Test
    void noWinnerBeforeLevel5() {
        // Do a few moves but not enough to win
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA), player1Handler);
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA), player2Handler);
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA), player1Handler);

        assertEquals(0, engine.getCurrentState().winnerId(),
                "No winner until someone reaches 5");
    }

    // Note: Player 2 cannot win when Player 1 goes first in our turn-based game.
    // Since P1 always moves first and can reach 5 Botany before P2 can reach any 5,
    // P1 will always win in a race to 5. This is intentional game design.
}