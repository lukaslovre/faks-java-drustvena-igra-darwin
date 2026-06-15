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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
 *
 * <h3>Thread-safety of the Queue</h3>
 * The SAX parser runs on the calling thread synchronously and populates the
 * queue before returning. However, the queue is declared as a
 * {@link ConcurrentLinkedQueue} so the consuming {@link ReplayEngine} (on a
 * separate Virtual Thread) never sees a stale view, even if the two phases
 * overlap in future refactors. In JS terms this is like a shared
 * {@code Array} that has built-in {@code push} / {@code shift} without
 * needing a {@code Mutex} / {@code Lock}.
 */
public class SaxReplayParser extends DefaultHandler {

    // ConcurrentLinkedQueue is a non-blocking, thread-safe FIFO queue.
    // Equivalent in JS: a plain [] with a library like 'fastq' that handles
    // concurrent push/pop natively without external synchronization.
    private final Queue<MoveRequestDTO> moves = new ConcurrentLinkedQueue<>();

    /**
     * Validates the file against {@code replay.xsd}, then streams it through
     * the SAX parser.
     *
     * @param xmlFile the replay {@code .xml} file to read
     * @return the populated queue of moves (shared reference — do not modify
     *         during consumption)
     * @throws Exception propagated from either XSD validation or SAX parsing
     */
    public Queue<MoveRequestDTO> parse(File xmlFile) throws Exception {
        moves.clear(); // allow re-use of the same parser instance across files

        // Step 1 — structural validation against the schema.
        // StreamSource wraps a File into the javax.xml.transform abstraction
        // that XsdValidator's SchemaFactory expects.
        XsdValidator.validate(new StreamSource(xmlFile));

        // Step 2 — event-driven parse.
        // SAXParserFactory follows the "Factory" design pattern: callers never
        // instantiate SAXParser directly. In JS this is like a module that
        // exports a createParser() function instead of exposing the constructor.
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.newSAXParser().parse(xmlFile, this);

        return moves;
    }

    /**
     * SAX callback — fired every time the parser encounters an opening tag.
     *
     * <h3>Java note: @Override</h3>
     * {@code @Override} is a compile-time safety net. If the parent class
     * ({@link DefaultHandler}) changes its signature or we typo the method
     * name, the compiler refuses to build. In TypeScript the closest analogue
     * is the {@code override} keyword (TS 4.3+).
     *
     * @param qName     qualified name of the element (we ignore namespace-aware
     *                  variants because the replay XML uses no namespaces)
     * @param attributes key-value pairs from the opening tag
     */
    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
        // SAX reports EVERY element, not just <Move>. We filter early so the
        // queue doesn't pick up <DarwinReplay>, <MatchInfo>, or <Moves>.
        if (!"Move".equals(qName)) return;

        // Attributes are string-keyed string values, so we parse into the
        // strongly-typed DTO.  Integer.parseInt is Java's equivalent of
        // Number.parseInt() in JS — it throws NumberFormatException on
        // bad input, which surfaces as a replay error to the user.
        int playerId = Integer.parseInt(attributes.getValue("playerId"));
        int workerId = Integer.parseInt(attributes.getValue("workerId"));

        // Island.valueOf() is the enum equivalent of TS's enum reverse lookup.
        // It throws IllegalArgumentException if the string doesn't match any
        // enum constant — which only happens if the XML is corrupt or the XSD
        // validation was skipped.
        Island targetIsland = Island.valueOf(attributes.getValue("targetIsland"));

        moves.add(new MoveRequestDTO(playerId, workerId, targetIsland));
    }
}