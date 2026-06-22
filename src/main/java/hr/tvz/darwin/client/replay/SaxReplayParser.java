package hr.tvz.darwin.client.replay;

import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.XsdValidator;
import hr.tvz.darwin.shared.dto.MoveRequestDTO;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Event-driven XML parser that reads replay files and converts {@code <Move>}
 * elements into {@link MoveRequestDTO} objects.
 *
 * <h3>SAX vs DOM (JS analogy)</h3>
 * DOM ({@link hr.tvz.darwin.server.DomXmlWriter}) builds a full in-memory tree
 * — like {@code document.createElement} in the browser. SAX fires callback
 * events as it streams through the file — like an {@code EventEmitter} or
 * {@code ReadableStream} pumping {@code 'data'} chunks. SAX is the right tool
 * here because we only care about {@code <Move>} tags; the rest of the
 * document is noise.
 */
public class SaxReplayParser extends DefaultHandler {
    private final Queue<MoveRequestDTO> moves = new ArrayDeque<>();
    private int expectedTurn;

    /**
     * Validates the file against {@code replay.xsd}, then streams it through
     * the SAX parser.
     *
     * @param xmlFile the replay {@code .xml} file to read
     * @return the populated queue of moves
     * @throws IOException if the replay or schema cannot be read
     * @throws SAXException if validation or parsing fails
     * @throws ParserConfigurationException if the platform cannot create a secure SAX parser
     */
    public Queue<MoveRequestDTO> parse(File xmlFile)
            throws IOException, SAXException, ParserConfigurationException {
        moves.clear();
        expectedTurn = 1;
        XsdValidator.validate(new StreamSource(xmlFile));
        SAXParserFactory factory = SAXParserFactory.newInstance();
        // Replay files are data, never XML programs allowed to load other files.
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        var parser = factory.newSAXParser();
        parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        parser.parse(xmlFile, this);

        return moves;
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
        // SAX reports EVERY element, not just <Move>. We filter early so the
        // queue doesn't pick up <DarwinReplay>, <MatchInfo>, or <Moves>.
        if (!"Move".equals(qName)) return;

        int turn = Integer.parseInt(attributes.getValue("turn"));
        if (turn != expectedTurn) {
            throw new SAXException("Invalid move order: expected turn "
                    + expectedTurn + " but found " + turn + ".");
        }
        expectedTurn++;
        int playerId = Integer.parseInt(attributes.getValue("playerId"));
        int workerId = Integer.parseInt(attributes.getValue("workerId"));
        Island targetIsland = Island.valueOf(attributes.getValue("targetIsland"));

        moves.add(new MoveRequestDTO(playerId, workerId, targetIsland));
    }
}
