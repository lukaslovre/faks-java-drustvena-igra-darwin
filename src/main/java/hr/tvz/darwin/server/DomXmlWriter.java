package hr.tvz.darwin.server;

import hr.tvz.darwin.shared.dto.MoveRequestDTO;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Writes game move history to an XML file using DOM and validates against replay.xsd.
 */
public class DomXmlWriter {

    private static final String XSD_RESOURCE = "replay.xsd";
    private static final String REPLAYS_DIR = "replays";

    public void saveGame(List<MoveRequestDTO> moves, int winnerId) {
        try {
            Document doc = DocumentBuilderFactory
                    .newInstance().newDocumentBuilder().newDocument();

            Element root = doc.createElement("DarwinReplay");
            doc.appendChild(root);

            String timestamp = Instant.now().toString();

            Element matchInfo = doc.createElement("MatchInfo");
            matchInfo.setAttribute("timestamp", timestamp);
            matchInfo.setAttribute("winnerId", String.valueOf(winnerId));
            root.appendChild(matchInfo);

            Element movesElement = doc.createElement("Moves");
            int turn = 1;
            for (MoveRequestDTO move : moves) {
                Element moveElement = doc.createElement("Move");
                moveElement.setAttribute("turn", String.valueOf(turn));
                moveElement.setAttribute("playerId", String.valueOf(move.playerId()));
                moveElement.setAttribute("workerId", String.valueOf(move.workerId()));
                moveElement.setAttribute("targetIsland", move.targetIsland().name());
                movesElement.appendChild(moveElement);
                turn++;
            }
            root.appendChild(movesElement);

            SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try (InputStream xsdStream = getClass().getClassLoader().getResourceAsStream(XSD_RESOURCE)) {
                if (xsdStream != null) {
                    sf.newSchema(new javax.xml.transform.stream.StreamSource(xsdStream))
                            .newValidator()
                            .validate(new DOMSource(doc));
                } else {
                    System.err.println("Warning: " + XSD_RESOURCE + " not found on classpath — skipping validation");
                }
            }

            File replaysDir = new File(REPLAYS_DIR);
            if (!replaysDir.exists()) {
                replaysDir.mkdirs();
            }

            String sanitized = timestamp.replace(":", "-");
            File outputFile = new File(replaysDir, "replay_" + sanitized + ".xml");

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