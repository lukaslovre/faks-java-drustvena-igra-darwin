package hr.tvz.darwin.server.core;

import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Covers game rules, turn management, state transitions, and win detection. */
class GameEngineTest {

    private GameEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GameEngine();
    }

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

    @Test
    void player2CannotMoveOnPlayer1Turn() {
        InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                () -> engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA)));

        assertEquals("It is not your turn!", ex.getMessage());
        assertEquals(0, engine.getCurrentState().player1().botany(), "State should not change");
    }

    @Test
    void player1CannotMoveOnPlayer2Turn() throws InvalidMoveException {
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));
        InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                () -> engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA)));

        assertEquals("It is not your turn!", ex.getMessage());
    }

    @Test
    void level1WorkerCannotGoToSanCristobal() {
        InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                () -> engine.processMove(new MoveRequestDTO(1, 0, Island.SAN_CRISTOBAL)));

        assertTrue(ex.getMessage().contains("too low"));
    }

    @Test
    void level1WorkerCanGoToIsabela() throws InvalidMoveException {
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));

    }

    @Test
    void level2WorkerCanGoToSantaCruz() throws InvalidMoveException {
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA));
        engine.processMove(new MoveRequestDTO(1, 0, Island.SANTA_CRUZ));
    }

    @Test
    void validMoveIncreasesPlayerTrack() throws InvalidMoveException {
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));

        assertEquals(1, engine.getCurrentState().player1().botany(),
                "Player 1 should have 1 Botany after Isabela");
        assertEquals(0, engine.getCurrentState().player1().zoology(),
                "Player 1 should have 0 Zoology");
    }

    @Test
    void validMoveLevelsUpWorker() throws InvalidMoveException {
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));

        assertEquals(2, engine.getCurrentState().player1().worker0().level(),
                "Worker 0 should be level 2 after one move");
    }

    @Test
    void workerLevelCapsAt3() throws InvalidMoveException {
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
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA));
        assertEquals(1, engine.getCurrentState().player1().botany(), "P1 Isabela = Botany");
        assertEquals(1, engine.getCurrentState().player2().botany(), "P2 Isabela = Botany");
        assertEquals(2, engine.getCurrentState().player1().worker0().level(), "P1 worker leveled up");
        assertEquals(2, engine.getCurrentState().player2().worker0().level(), "P2 worker leveled up");
    }

    @Test
    void workerLevelingUpUnlocksHigherIslands() throws InvalidMoveException {
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA));
        engine.processMove(new MoveRequestDTO(1, 0, Island.SANTA_CRUZ));
        assertEquals(1, engine.getCurrentState().player1().botany(), "Isabela = Botany");
        assertEquals(1, engine.getCurrentState().player1().zoology(), "Santa Cruz = Zoology");
    }

    @Test
    void reaching5OnBotanyWins() throws InvalidMoveException {
        for (int i = 0; i < 5; i++) {
            engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));
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
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));
        engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA));
        engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));

        assertEquals(0, engine.getCurrentState().winnerId(),
                "No winner until someone reaches 5");
    }

    @Test
    void gameOverRejectsFurtherMoves() throws InvalidMoveException {
        for (int i = 0; i < 5; i++) {
            engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA));
            if (engine.getCurrentState().winnerId() != 0) {
                break;
            }
            engine.processMove(new MoveRequestDTO(2, 0, Island.ISABELA));
        }

        InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                () -> engine.processMove(new MoveRequestDTO(1, 0, Island.ISABELA)));

        assertEquals("The game is already over!", ex.getMessage());
    }
}
