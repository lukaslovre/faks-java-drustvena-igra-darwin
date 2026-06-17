package hr.tvz.darwin.server.rmi;

import hr.tvz.darwin.shared.dto.GameStateDTO;
import hr.tvz.darwin.shared.rmi.IDarwinArchive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RMI service that aggregates match statistics across ALL games on this server.
 * <p>
 * Think of this like a global leaderboard database. Every time a game ends,
 * {@link #onGameEnded(GameStateDTO)} is called to tally up the research points
 * earned by both players and increment the games-played counter. The totals
 * are persisted to {@code stats/global_stats.txt} so they survive server restarts.
 * <p>
 * Designed as a Singleton — there should only ever be one archive instance
 * per JVM, bound in JNDI for remote clients to look up.
 */
public class DarwinArchiveImpl extends UnicastRemoteObject implements IDarwinArchive {

    private static DarwinArchiveImpl instance;
    private static final Path STATS_FILE = Path.of("stats", "global_stats.txt");

    private final AtomicInteger totalResearchPoints = new AtomicInteger(0);
    private final AtomicInteger totalGamesPlayed = new AtomicInteger(0);

    private DarwinArchiveImpl() throws RemoteException {
        super();
        ensureDirectoryExists();
        loadStats();
    }

    /**
     * Returns the singleton, creating it on first call.
     * Thread-safe via {@code synchronized} — ensures only one thread
     * initialises the instance and loads persisted stats from disk.
     */
    public static synchronized DarwinArchiveImpl getInstance() {
        if (instance == null) {
            try {
                instance = new DarwinArchiveImpl();
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to initialize DarwinArchiveImpl", e);
            }
        }
        return instance;
    }

    /**
     * Creates the {@code stats/} directory if it doesn't exist.
     * Called once during construction so {@link #saveStats()} has a place to write.
     */
    private void ensureDirectoryExists() {
        var directory = STATS_FILE.getParent().toFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * Reads {@code research_points} and {@code games_played} from the
     * Properties file at {@link #STATS_FILE}. If the file doesn't exist yet
     * (fresh server), creates it with zero values.
     */
    private synchronized void loadStats() {
        if (!Files.exists(STATS_FILE)) {
            saveStats();
            return;
        }
        try (var reader = Files.newBufferedReader(STATS_FILE)) {
            Properties props = new Properties();
            props.load(reader);
            totalResearchPoints.set(Integer.parseInt(props.getProperty("research_points", "0")));
            totalGamesPlayed.set(Integer.parseInt(props.getProperty("games_played", "0")));
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading stats, starting with zero values: " + e.getMessage());
        }
    }

    /**
     * Writes the current counter values to {@code stats/global_stats.txt}
     * using {@link java.util.Properties#store}. Called after every game ends
     * so the file is always up-to-date even if the server crashes.
     */
    private synchronized void saveStats() {
        try (var writer = Files.newBufferedWriter(STATS_FILE)) {
            Properties props = new Properties();
            props.setProperty("research_points", String.valueOf(totalResearchPoints.get()));
            props.setProperty("games_played", String.valueOf(totalGamesPlayed.get()));
            props.store(writer, "Darwin Archive Global Statistics");
        } catch (IOException e) {
            System.err.println("Failed to write stats file: " + e.getMessage());
        }
    }

    /**
     * Called by the TCP server callback when a game ends (winnerId != 0).
     * Adds the combined research points from BOTH players to the global
     * tally and increments the games-played counter. Then persists to disk.
     */
    public synchronized void onGameEnded(GameStateDTO finalState) {
        int gamePoints = calculatePoints(finalState);
        totalResearchPoints.addAndGet(gamePoints);
        totalGamesPlayed.incrementAndGet();
        saveStats();
    }

    /**
     * Sums all three tracks (botany, zoology, geology) for both players.
     * This is the "research depth" contributed by a single match.
     */
    private int calculatePoints(GameStateDTO state) {
        return state.player1().botany() + state.player1().zoology() + state.player1().geology()
                + state.player2().botany() + state.player2().zoology() + state.player2().geology();
    }

    @Override
    public int getTotalGlobalResearchPoints() throws RemoteException {
        return totalResearchPoints.get();
    }

    @Override
    public int getTotalGamesPlayed() throws RemoteException {
        return totalGamesPlayed.get();
    }
}