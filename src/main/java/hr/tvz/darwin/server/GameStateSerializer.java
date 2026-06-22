package hr.tvz.darwin.server;

import hr.tvz.darwin.shared.dto.GameStateDTO;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Stores the latest authoritative game state using Java object serialization. */
public final class GameStateSerializer {

    private static final Path LATEST_GAME_PATH = Path.of("saves", "latest-game.ser");

    private final Path savePath;

    public GameStateSerializer() {
        this(LATEST_GAME_PATH);
    }

    /* Package-private path injection keeps tests isolated from the real save file. */
    GameStateSerializer(Path savePath) {
        this.savePath = Objects.requireNonNull(savePath);
    }

    public void save(GameStateDTO state) throws IOException {
        Objects.requireNonNull(state);
        Files.createDirectories(savePath.getParent());

        // Unlike JSON.stringify(), ObjectOutputStream preserves the complete typed DTO graph.
        try (var output = new ObjectOutputStream(Files.newOutputStream(savePath))) {
            output.writeObject(state);
        }
    }

    /** Reads a saved snapshot for verification; it does not restore a live match. */
    public GameStateDTO load() throws IOException, ClassNotFoundException {
        try (var input = new ObjectInputStream(Files.newInputStream(savePath))) {
            Object savedObject = input.readObject();
            if (savedObject instanceof GameStateDTO state) {
                return state;
            }
            throw new IOException("Save file does not contain a GameStateDTO.");
        }
    }
}
