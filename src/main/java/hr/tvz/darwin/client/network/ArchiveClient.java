package hr.tvz.darwin.client.network;

import hr.tvz.darwin.shared.rmi.IDarwinArchive;

import javax.naming.InitialContext;
import java.util.function.Consumer;

/** Performs the blocking RMI archive lookup away from the JavaFX thread. */
public class ArchiveClient {

    public record ArchiveStats(int totalGames, int totalPoints) {
    }

    /**
     * Starts the lookup on a virtual thread. Both callbacks run on that thread,
     * so JavaFX callers must use {@code Platform.runLater()} before updating UI.
     */
    public void fetchStats(Consumer<ArchiveStats> onSuccess, Consumer<Exception> onError) {
        Thread.ofVirtual().start(() -> {
            try {
                var context = new InitialContext();
                var archive = (IDarwinArchive) context.lookup(
                        "rmi://localhost:1099/DarwinArchive");
                onSuccess.accept(new ArchiveStats(
                        archive.getTotalGamesPlayed(),
                        archive.getTotalGlobalResearchPoints()));
            } catch (Exception exception) {
                onError.accept(exception);
            }
        });
    }
}
