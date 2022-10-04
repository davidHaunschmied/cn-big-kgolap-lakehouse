package at.jku.dke.bigkgolap.engines.aixm.analyzer;

import lombok.extern.slf4j.Slf4j;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;

@Slf4j
public class AixmSAXContextExtractor extends DefaultHandler {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .appendOptional(DateTimeFormatter.ISO_DATE_TIME)
            .toFormatter();

    private static final String HAS_MEMBER_TAG = "message:hasMember";
    private static final String LOCATION_TAG = "event:location";
    private static final String AFFECTED_FIR_TAG = "event:affectedFIR";
    private static final String BEGIN_POSITION_TAG = "gml:beginPosition";
    private static final String END_POSITION_TAG = "gml:endPosition";

    private static final String AIXM_PREFIX = "aixm:";
    private static final List<String> VALID_TIME_TAGS = List.of("gml:validTime", "gml:TimePeriod");
    private final SAXParser saxParser;

    private String state = "";
    private boolean nextIsTopElementOfMember = false;
    private int aixmStateCount = 0;
    private int validTimeStateCount = 0;

    private String topic;
    private String location;
    private String affectedFir;
    private LocalDate beginPositionDate;
    private LocalDate endPositionDate;

    public AixmSAXContextExtractor() {
        try {
            this.saxParser = SAXParserFactory.newInstance().newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException("Could not create SAX parser!", e);
        }
    }

    public void parse(InputStream is) throws IOException, SAXException {

        try {
            saxParser.parse(is, this);
        } catch (Exception e) {
            log.error("Exception occurred during parsing!", e);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName == null) {
            return;
        }

        state = qName;

        if (qName.startsWith(AIXM_PREFIX)) {
            // First AIXM TAG determines the topic
            if (nextIsTopElementOfMember && topic == null) {
                topic = qName.substring(AIXM_PREFIX.length());
            }
            aixmStateCount++;
        }
        if (VALID_TIME_TAGS.contains(qName)) {
            validTimeStateCount++;
        }

        nextIsTopElementOfMember = qName.equals(HAS_MEMBER_TAG);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        switch (state) {
            case LOCATION_TAG:
                location = new String(ch, start, length);
                break;
            case AFFECTED_FIR_TAG:
                affectedFir = new String(ch, start, length);
                break;
            case BEGIN_POSITION_TAG:
                if (aixmStateCount > 0 && validTimeStateCount > 0) {
                    String tmp = new String(ch, start, length);
                    beginPositionDate = LocalDate.parse(tmp, DATE_TIME_FORMATTER);
                }
                break;
            case END_POSITION_TAG:
                if (aixmStateCount > 0 && validTimeStateCount > 0) {
                    String tmp = new String(ch, start, length);
                    endPositionDate = LocalDate.parse(tmp, DATE_TIME_FORMATTER);
                }
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        state = "";

        if (qName.startsWith(AIXM_PREFIX)) {
            aixmStateCount--;
        }
        if (VALID_TIME_TAGS.contains(qName)) {
            validTimeStateCount--;
        }
    }

    public String getTopic() {
        return topic;
    }

    public String getLocation() {
        return location;
    }

    public String getAffectedFir() {
        return affectedFir;
    }

    public LocalDate getBeginPositionDate() {
        return beginPositionDate;
    }

    public LocalDate getEndPositionDate() {
        return endPositionDate;
    }
}
