package hr.tvz.darwin.client.replay;

import hr.tvz.darwin.shared.Island;
import hr.tvz.darwin.shared.dto.MoveRequestDTO;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaxReplayParserTest {

    private final SaxReplayParser parser = new SaxReplayParser();

    @Test
    void parsesMovesInXmlElementOrder() throws Exception {
        List<MoveRequestDTO> moves = List.copyOf(parser.parse(fixture("valid.xml")));

        assertEquals(List.of(
                new MoveRequestDTO(1, 0, Island.ISABELA),
                new MoveRequestDTO(2, 1, Island.SANTA_CRUZ),
                new MoveRequestDTO(1, 0, Island.SAN_CRISTOBAL)), moves);
    }

    @Test
    void xsdRejectsOutOfRangePlayerAndWorkerIds() {
        assertThrows(SAXException.class, () -> parser.parse(fixture("invalid-player.xml")));
        assertThrows(SAXException.class, () -> parser.parse(fixture("invalid-worker.xml")));
    }

    @Test
    void saxRejectsDuplicateSkippedAndOutOfOrderTurns() {
        for (String name : List.of(
                "duplicate-turn.xml", "skipped-turn.xml", "out-of-order-turn.xml")) {
            SAXException error = assertThrows(
                    SAXException.class, () -> parser.parse(fixture(name)));
            assertTrue(error.getMessage().startsWith("Invalid move order:"));
        }
    }

    @Test
    void saxRejectsDoctypeDeclarations() {
        assertThrows(SAXException.class, () -> parser.parse(fixture("doctype.xml")));
    }

    private File fixture(String name) throws URISyntaxException {
        return new File(getClass().getResource("/replays/" + name).toURI());
    }
}
