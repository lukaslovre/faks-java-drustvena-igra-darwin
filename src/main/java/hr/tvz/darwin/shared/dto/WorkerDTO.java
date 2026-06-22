package hr.tvz.darwin.shared.dto;

import hr.tvz.darwin.shared.Island;

import java.io.Serial;
import java.io.Serializable;

/** A worker token; {@code currentIsland == null} means it is at base. */
public record WorkerDTO(int id, int level, Island currentIsland) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
