package hr.tvz.darwin.server.core;

import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
 * ERROR HANDLING:
 * GameEngine throws InvalidMoveException when a business rule is violated,
 * instead of sending network DTOs directly. This keeps the domain layer pure.
 */
class GameEngineTest {

    private GameEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GameEngine();
    }

    // === Turn Management Tests ===

    @Test
    void gameStartsWithPlayer1Turn() {
        assertEquals(1, engine.getActivePlayerId());
        assertEquals(1, engine.getCurrentState().activePlayerId());
    }

    @Test
    void afterPlayer1Moves_itsPlayer2Turn() throws InvalidMoveException {
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));

        assertEquals(2, engine.getActivePlayerId());
        assertEquals(2, engine.getCurrentState().activePlayerId());
    }

    @Test
    void afterPlayer2Moves_itsPlayer1TurnAgain() throws InvalidMoveException {
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA));

        assertEquals(1, engine.getActivePlayerId());
        assertEquals(1, engine.getCurrentState().activePlayerId());
    }

    // === Turn Validation Tests ===

    @Test
    void player2CannotMoveOnPlayer1Turn() {
        InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                () -> engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA)));

        assertEquals("It is not your turn!", ex.getMessage());
        assertEquals(0, engine.getCurrentState().player1().botany(), "State should not change");
    }

    @Test
    void player1CannotMoveOnPlayer2Turn() throws InvalidMoveException {
        // Player 1 moves first
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));

        // Player 1 tries to move again (should fail)
        InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                () -> engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA)));

        assertEquals("It is not your turn!", ex.getMessage());
    }

    // === Worker Level Validation Tests ===

    @Test
    void level1WorkerCannotGoToSanCristobal() {
        // San Cristobal requires Level 3
        InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                () -> engine.processMove(new MoveRequestDTO(1, 0, Island.SAN_CRISTOBAL)));

        assertTrue(ex.getMessage().contains("too low"));
    }

    @Test
    void level1WorkerCanGoToIsabela() throws InvalidMoveException {
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));

        // No exception thrown — move was accepted
    }

    @Test
    void level2WorkerCanGoToSantaCruz() throws InvalidMoveException {
        // First, level up worker 0 to level 2 by going to Isabela
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA));

        // Now worker 0 is level 2 — can go to Santa Cruz (requires level 2)
        engine.processMove(new MoveRequestDTO(1, 0, Island.SANTA_CRUZ));

        // No exception thrown — move was accepted
    }

    // === State Update Tests ===

    @Test
    void validMoveIncreasesPlayerTrack() throws InvalidMoveException {
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));

        // Isabela rewards +1 Botany
        assertEquals(1, engine.getCurrentState().player1().botany(),
                "Player 1 should have 1 Botany after Isabela");
        assertEquals(0, engine.getCurrentState().player1().zoology(),
                "Player 1 should have 0 Zoology");
    }

    @Test
    void validMoveLevelsUpWorker() throws InvalidMoveException {
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));

        // Worker 0 started at level 1, should now be level 2
        assertEquals(2, engine.getCurrentState().player1().worker0().level(),
                "Worker 0 should be level 2 after one move");
    }

    @Test
    void workerLevelCapsAt3() throws InvalidMoveException {
        // Level up worker 0 twice: 1 -> 2 -> 3
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA)); // P1: worker0 = lvl 2
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA)); // P2
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA)); // P1: worker0 = lvl 3
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA)); // P2
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA)); // P1: worker0 = still 3 (capped)

        assertEquals(3, engine.getCurrentState().player1().worker0().level(),
                "Worker should cap at level 3");
    }

    @Test
    void differentIslandsRewardCorrespondingTracks() throws InvalidMoveException {
        // Both players go to Isabela (Botany) first to level up
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA));

        // Both should have 1 Botany
        assertEquals(1, engine.getCurrentState().player1().botany(), "P1 Isabela = Botany");
        assertEquals(1, engine.getCurrentState().player2().botany(), "P2 Isabela = Botany");

        // Both workers leveled up to 2
        assertEquals(2, engine.getCurrentState().player1().worker0().level(), "P1 worker leveled up");
        assertEquals(2, engine.getCurrentState().player2().worker0().level(), "P2 worker leveled up");
    }

    @Test
    void workerLevelingUpUnlocksHigherIslands() throws InvalidMoveException {
        // Round 1: P1 and P2 both go Isabela (level up to 2)
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA));

        // Round 2: P1 goes Santa Cruz (worker now level 2, can access)
        engine.processMove(new MoveRequestDTO(1, 0, Island.SANTA_CRUZ));

        // P1 should have 1 Botany (Isabela) and 1 Zoology (Santa Cruz)
        assertEquals(1, engine.getCurrentState().player1().botany(), "Isabela = Botany");
        assertEquals(1, engine.getCurrentState().player1().zoology(), "Santa Cruz = Zoology");
    }

    // === Win Condition Tests ===

    @Test
    void reaching5OnBotanyWins() throws InvalidMoveException {
        // Player 1 gets to 5 Botany by repeatedly sending worker to Isabela
        // Each trip to Isabela = +1 Botany
        for (int i = 0; i < 5; i++) {
            engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));
            // After P1's 5th move, the game is won — skip P2's move
            if (engine.getCurrentState().winnerId() != 0) {
                break;
            }
            engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA));
        }

        assertEquals(1, engine.getCurrentState().winnerId(),
                "Player 1 should win by reaching 5 Botany");
        assertEquals(2, engine.getCurrentState().activePlayerId(),
                "Active player should be 2 after P1's winning move");
    }

    @Test
    void noWinnerBeforeLevel5() throws InvalidMoveException {
        // Do a few moves but not enough to win
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA));
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));

        assertEquals(0, engine.getCurrentState().winnerId(),
                "No winner until someone reaches 5");
    }

    @Test
    void gameOverRejectsFurtherMoves() throws InvalidMoveException {
        // Play until Player 1 wins
        for (int i = 0; i < 5; i++) {
            engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));
            if (engine.getCurrentState().winnerId() != 0) {
                break;
            }
            engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA));
        }

        // Any further move should throw
        InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                () -> engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA)));

        assertEquals("The game is already over!", ex.getMessage());
    }

    // Note: Player 2 cannot win when Player 1 goes first in our turn-based game.
    // Since P1 always moves first and can reach 5 Botany before P2 can reach any 5,
    // P1 will always win in a race to 5. This is intentional game design.
}