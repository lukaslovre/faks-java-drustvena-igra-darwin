# 4. XML / Replay System Spec
**Project:** Darwin's Journey (Simplified Vertical Slice)
**Technologies:** DOM (Writing), SAX (Reading), XSD (Validation)

## 1. XML Structure & XSD Schema
To satisfy the XSD validation requirement, the XML file must follow a strict schema. The file acts as an "Event Ledger" of moves, rather than saving the entire board state.

### 1.1 The XML Output (`replay_match_123.xml`)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<DarwinReplay xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="replay.xsd">
    <MatchInfo timestamp="2025-10-15T14:30:00" winnerId="1"/>
    <Moves>
        <Move turn="1" playerId="1" workerId="0" targetIsland="ISABELA" />
        <Move turn="2" playerId="2" workerId="1" targetIsland="SANTA_CRUZ" />
        <Move turn="3" playerId="1" workerId="0" targetIsland="SAN_CRISTOBAL" />
    </Moves>
</DarwinReplay>
```

### 1.2 The XSD Schema (`replay.xsd`)
This file will be stored in the `src/main/resources` folder.
```xml
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xs:element name="DarwinReplay">
        <xs:complexType>
            <xs:sequence>
                <xs:element name="MatchInfo">
                    <xs:complexType>
                        <xs:attribute name="timestamp" type="xs:string" use="required"/>
                        <xs:attribute name="winnerId" type="xs:integer" use="required"/>
                    </xs:complexType>
                </xs:element>
                <xs:element name="Moves">
                    <xs:complexType>
                        <xs:sequence>
                            <xs:element name="Move" maxOccurs="unbounded">
                                <xs:complexType>
                                    <xs:attribute name="turn" type="xs:integer" use="required"/>
                                    <xs:attribute name="playerId" type="xs:integer" use="required"/>
                                    <xs:attribute name="workerId" type="xs:integer" use="required"/>
                                    <xs:attribute name="targetIsland" type="xs:string" use="required"/>
                                </xs:complexType>
                            </xs:element>
                        </xs:sequence>
                    </xs:complexType>
                </xs:element>
            </xs:sequence>
        </xs:complexType>
    </xs:element>
</xs:schema>
```

## 2. DOM Implementation (Writing the File)
**When does it happen?** The moment a player reaches Level 5 on a research track, the `GameEngine` detects a win.
**How does it work?** 
1.  The Server passes its `List<MoveRequestDTO> moveHistory` to the `DomXmlWriter` utility class.
2.  The `DomXmlWriter` uses `DocumentBuilderFactory` to create a new XML Document in memory.
3.  It iterates through the `moveHistory` list, appending `<Move>` elements to the DOM tree.
4.  It uses a `Transformer` to write the DOM tree to a file in the `replays/` directory.

## 3. SAX Implementation (Reading & Replaying)
**When does it happen?** The user clicks the "Load Replay" button on the Client UI.
**How does it work?** 
Because SAX is an event-driven parser (it fires methods like `startElement` as it scans the file top-to-bottom), it reads the file *instantly*. If we tied the UI animations directly to the SAX parser, the whole replay would finish in 2 milliseconds. 

To solve this, we use a **Producer-Consumer** pattern:

1.  **Validation:** The Client first validates the selected XML file against `replay.xsd`.
2.  **The Producer (SAX):** The `SaxReplayParser` reads the XML file and converts every `<Move>` tag back into a `MoveRequestDTO`. It pushes these DTOs into a `Queue<MoveRequestDTO>`.
3.  **The Consumer (Virtual Thread):** The Client spawns a Virtual Thread. This thread runs a `while(!queue.isEmpty())` loop:
    *   It pops the next move from the queue.
    *   It uses `Platform.runLater()` to tell the `AnimationHelper` to visually move the worker.
    *   It calls `Thread.sleep(1500)` to pause for 1.5 seconds, giving the user time to watch the animation before the next move occurs.

## 4. Class Responsibility Map (Respecting the <200 Line Limit)
*   **`DomXmlWriter.java` (Server-side):** Contains exactly one method: `public void saveGame(List<MoveRequestDTO> moves, int winnerId)`. (~70 lines).
*   **`SaxReplayParser.java` (Client-side):** Extends `DefaultHandler`. Overrides `startElement` to populate a Queue. (~80 lines).
*   **`ReplayEngine.java` (Client-side):** Manages the Virtual Thread that consumes the Queue and triggers UI updates. (~50 lines).