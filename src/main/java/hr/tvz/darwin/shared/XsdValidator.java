package hr.tvz.darwin.shared;

import org.xml.sax.SAXException;

import javax.xml.transform.Source;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loads and caches replay.xsd from classpath for validation.
 * <p>
 * Shared between DomXmlWriter (DOM write-time validation) and
 * SaxReplayParser (SAX read-time validation). Loading the Schema
 * once and reusing it avoids re-parsing the .xsd file every time.
 */
public class XsdValidator {

    private static final String XSD_RESOURCE = "replay.xsd";
    private static volatile Schema cachedSchema;

    private XsdValidator() {
        // Utility class: all behavior is exposed through static methods.
    }

    /**
     * Returns a Validator for the replay.xsd schema.
     * <p>
     * The Schema is loaded once from classpath and cached (lazily).
     * Each call gets a new Validator instance because Validator is
     * not thread-safe (it carries mutable validation state).
     *
     * @return a fresh Validator for replay.xsd
     * @throws SAXException if the .xsd file is malformed or cannot be compiled into a Schema
     * @throws IOException  if reading the .xsd resource from classpath fails
     * @throws RuntimeException if replay.xsd is not found on the classpath (misconfiguration)
     */
    public static Validator getValidator() throws SAXException, IOException {
        if (cachedSchema == null) {
            synchronized (XsdValidator.class) {
                if (cachedSchema == null) {
                    SchemaFactory sf = SchemaFactory.newInstance(
                            XMLConstants.W3C_XML_SCHEMA_NS_URI);
                    sf.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                    sf.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                    try (InputStream xsdStream = XsdValidator.class
                            .getClassLoader().getResourceAsStream(XSD_RESOURCE)) {
                        if (xsdStream == null) {
                            throw new RuntimeException("Cannot find " + XSD_RESOURCE + " on classpath");
                        }
                        cachedSchema = sf.newSchema(
                                new javax.xml.transform.stream.StreamSource(xsdStream));
                    }
                }
            }
        }
        Validator validator = cachedSchema.newValidator();
        validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return validator;
    }

    /**
     * Validates an XML Source against replay.xsd in one call.
     *
     * @param source the XML Source to validate (e.g. DOMSource, StreamSource)
     * @throws SAXException     if the XML does not conform to replay.xsd
     * @throws IOException      if reading the XML source or .xsd resource fails
     * @throws RuntimeException if replay.xsd is not found on the classpath
     */
    public static void validate(Source source) throws SAXException, IOException {
        getValidator().validate(source);
    }
}
