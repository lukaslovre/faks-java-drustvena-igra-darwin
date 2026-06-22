package hr.tvz.darwin.server;

import hr.tvz.darwin.shared.XsdValidator;
import hr.tvz.darwin.shared.dto.MoveRequestDTO;

import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.time.Instant;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Builds a replay as a DOM tree, validates it, then writes it to disk. */
public class DomXmlWriter {

    private static final String REPLAYS_DIR = "replays";

    /** Errors are logged rather than stopping the TCP server. */
    public void saveGame(List<MoveRequestDTO> moves, int winnerId) {
        try {
            // ── Step 1: Build the in-memory DOM tree ──────────────────────────
            // DocumentBuilderFactory.newDocumentBuilder() gives us a factory that
            // can parse OR create XML docs. Calling newDocument() gives us an
            // empty document to build from scratch (no file I/O yet).
            Document doc = DocumentBuilderFactory
                    .newInstance().newDocumentBuilder().newDocument();

            // <DarwinReplay> — root element wrapping the whole file
            Element root = doc.createElement("DarwinReplay");
            doc.appendChild(root);

            // ISO 8601 timestamp: e.g. "2026-06-15T14:30:00Z"
            String timestamp = Instant.now().toString();

            // <MatchInfo timestamp="..." winnerId="..." />
            // Metadata about the game: when it was played and who won.
            Element matchInfo = doc.createElement("MatchInfo");
            matchInfo.setAttribute("timestamp", timestamp);
            matchInfo.setAttribute("winnerId", String.valueOf(winnerId));
            root.appendChild(matchInfo);

            // <Moves> — container for all the individual <Move> elements
            Element movesElement = doc.createElement("Moves");
            int turn = 1;
            for (MoveRequestDTO move : moves) {
                Element moveElement = doc.createElement("Move");
                moveElement.setAttribute("turn", String.valueOf(turn));
                moveElement.setAttribute("playerId", String.valueOf(move.playerId()));
                moveElement.setAttribute("workerId", String.valueOf(move.workerId()));
                // Island.name() returns the enum constant name, e.g. "ISABELA"
                // This is the natural toString() of a Java enum — no custom mapping needed.
                moveElement.setAttribute("targetIsland", move.targetIsland().name());
                movesElement.appendChild(moveElement);
                turn++;
            }
            root.appendChild(movesElement);

            // ── Step 2: Validate the DOM tree against replay.xsd ──────────────
            // Three possible outcomes:
            //   - SAXException → the XML we built doesn't match the schema (bug)
            //   - RuntimeException → the .xsd isn't on the classpath (misconfiguration)
            //   - Success → everything is fine, continue writing
            try {
                XsdValidator.validate(new DOMSource(doc));
            } catch (SAXException e) {
                System.err.println("XML does not match replay.xsd (wrote potentially invalid file): " + e.getMessage());
            } catch (RuntimeException e) {
                System.err.println("replay.xsd not found on classpath (skipping validation): " + e.getMessage());
            }

            // ── Step 3: Write the DOM tree to disk ────────────────────────────
            // Ensure the replays/ directory exists. mkdirs() creates all missing
            // parent directories (unlike mkdir() which only creates the last segment).
            File replaysDir = new File(REPLAYS_DIR);
            if (!replaysDir.exists()) {
                replaysDir.mkdirs();
            }

            // Colons in ISO timestamps are not valid in Windows filenames.
            String sanitizedTimestamp = timestamp.replace(":", "-");
            File outputFile = new File(replaysDir, "replay_" + sanitizedTimestamp + ".xml");

            // Transformer writes the DOM tree to a file.
            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(new DOMSource(doc), new StreamResult(outputFile));

            System.out.println("Replay saved: " + outputFile.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Failed to write replay XML: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
