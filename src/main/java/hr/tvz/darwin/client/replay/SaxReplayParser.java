package hr.tvz.darwin.client.replay;

import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.XsdValidator;
import hr.tvz.darwin.shared.dto.MoveRequestDTO;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
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

    /**
     * Validates the file against {@code replay.xsd}, then streams it through
     * the SAX parser.
     *
     * @param xmlFile the replay {@code .xml} file to read
     * @return the populated queue of moves
     * @throws Exception propagated from either XSD validation or SAX parsing
     */
    public Queue<MoveRequestDTO> parse(File xmlFile) throws Exception {
        moves.clear();
        XsdValidator.validate(new StreamSource(xmlFile));
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.newSAXParser().parse(xmlFile, this);

        return moves;
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
        // SAX reports EVERY element, not just <Move>. We filter early so the
        // queue doesn't pick up <DarwinReplay>, <MatchInfo>, or <Moves>.
        if (!"Move".equals(qName)) return;

        int playerId = Integer.parseInt(attributes.getValue("playerId"));
        int workerId = Integer.parseInt(attributes.getValue("workerId"));
        Island targetIsland = Island.valueOf(attributes.getValue("targetIsland"));

        moves.add(new MoveRequestDTO(playerId, workerId, targetIsland));
    }
}
