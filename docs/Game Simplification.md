# Game Simplification

## Purpose

This project implements a small, playable interpretation of _Darwin's Journey_ rather than a complete reproduction of the board game. The simplified version is a vertical slice: it preserves the central loop of placing workers, meeting level requirements, gaining research progress, and improving workers while limiting the amount of content and state involved.

A complete match is designed to last approximately three to five minutes. This keeps the multiplayer session easy to understand and allows the full game lifecycle—from connecting two players to completing and replaying a match—to be demonstrated in one short session.

## Scope Comparison

| Feature       | Original Board Game                                  | Simplified Version                                                               |
| :------------ | :--------------------------------------------------- | :------------------------------------------------------------------------------- |
| Players       | One to four players                                  | Exactly two players: Player 1 (Red) and Player 2 (Blue)                          |
| Map           | Five Galapagos islands                               | Three islands: Isabela, Santa Cruz, and San Cristobal                            |
| Workers       | Several workers with more extensive progression      | Two workers per player, with levels from 1 to 3                                  |
| Action spaces | Multiple spaces with varied requirements and rewards | One action per island, with a fixed worker-level requirement and research reward |
| Resources     | Samples, letters, coins, and other resources         | Three research tracks used as the only form of progress                          |
| Research      | More complex research and scoring systems            | Three linear tracks with values from 0 to 5                                      |
| Victory       | Points gained from multiple systems                  | The first player to reach 5 on any research track wins immediately               |

## Design Rationale

### Two-player matches

The game always starts with two players. This avoids lobby configuration and variable player-count rules while preserving direct competition, alternating turns, and synchronized views of a shared match.

### Three islands

Each island represents one worker-level threshold and one research discipline. The fixed relationship makes the board readable and keeps move validation deterministic:

| Island        | Required worker level | Research reward |
| :------------ | :-------------------: | :-------------- |
| Isabela       |      1 or higher      | +1 Botany       |
| Santa Cruz    |      2 or higher      | +1 Zoology      |
| San Cristobal |      3 or higher      | +1 Geology      |

### Linear research tracks

Botany, Zoology, and Geology are independent tracks ranging from 0 to 5. Research points replace the original game's resources and broader scoring systems, so every accepted move has one clear and visible result.

### Automatic worker progression

After completing an action, the selected worker gains one level, up to the maximum level of 3. Progression is therefore part of the main placement loop rather than a separate action. A worker must first visit Isabela before it can access Santa Cruz, and must reach level 3 before it can access San Cristobal.

### Immediate victory

A player wins as soon as any one of their research tracks reaches 5. No additional end-game scoring is performed. This produces a short race while still allowing players to choose between advancing an accessible track and developing workers toward higher-level islands.

## Game Flow

### Setup

1. The server starts and waits for players.
2. Two clients connect and are assigned Player 1 (Red) and Player 2 (Blue).
3. Each player begins with two level-1 workers.
4. All Botany, Zoology, and Geology tracks begin at 0.
5. Player 1 takes the first turn.

### Turn sequence

1. The active player selects one of their workers and an island.
2. The move is accepted only when it is that player's turn and the worker meets the island's level requirement.
3. The selected worker travels to the island.
4. The island increases its associated research track by 1.
5. The worker gains one level, unless it is already level 3, and returns to the player's base.
6. If the updated research track has reached 5, the match ends. Otherwise, the other player becomes active.

An invalid move does not change the game state or end the player's turn.

## End of Match and Replay

When a player reaches 5 on any research track, both clients receive the final state and display the winner. No further moves are accepted for that match.

The completed move history is saved as an XML replay. A replay reconstructs the accepted turns in their original order, including worker movement, worker levels, and research progress. Chat messages and rejected moves are not part of the replay.

## Features Outside the Simplified Scope

The simplified version does not include the full commercial game's five-island map, variable player counts, samples, letters, coins, expeditions, branching research, complex action spaces, or multi-source end-game scoring. These omissions are intentional boundaries of this implementation.
