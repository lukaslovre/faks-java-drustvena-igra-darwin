package hr.tvz.darwin.shared.dto;

import hr.tvz.darwin.shared.Island;

import java.io.Serial;
import java.io.Serializable;

/** Client → server: a requested worker destination. */
public record MoveRequestDTO(int playerId, int workerId, Island targetIsland) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
