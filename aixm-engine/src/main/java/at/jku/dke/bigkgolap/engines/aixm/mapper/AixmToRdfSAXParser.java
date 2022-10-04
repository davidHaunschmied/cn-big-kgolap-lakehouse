package at.jku.dke.bigkgolap.engines.aixm.mapper;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.Stack;

import static at.jku.dke.bigkgolap.api.engines.Mapper.cp;
import static at.jku.dke.bigkgolap.api.engines.Mapper.cu;

public class AixmToRdfSAXParser extends DefaultHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AixmToRdfSAXParser.class);

    static final String BASE_URI = "http://example.org/bigkgolap/atm/aixm#";

    private static final String AIXM_PREFIX = "aixm:";
    private static final String MESSAGE_HAS_MEMBER = "message:hasMember";

    private static final String GML_ID_ATT = "gml:id";

    private static final Set<String> SUPPORTED_CONCEPTS = Set.of("aixm:availability", "aixm:activation", "aixm:annotation");

    private final Model model;
    private final Stack<Resource> resourceStack;
    private final SAXParser saxParser;
    private String previousTag;

    private boolean rootElemFound = false;
    private int capturingModeCount = 0;

    public AixmToRdfSAXParser(Model model) {
        this.model = model;
        this.resourceStack = new Stack<>();

        try {
            this.saxParser = SAXParserFactory.newInstance().newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException("Could not create SAX parser!", e);
        }
    }

    /*
    2022-02-10 05:18:55.810 ERROR 13236 --- [lt-executor-947] io.grpc.internal.SerializingExecutor     : Exception while executing runnable io.grpc.internal.ServerImpl$JumpToApplicationThreadServerStreamListener$1HalfClosed@6443adbb

java.lang.RuntimeException: org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 1; Premature end of file.
	at at.jku.dke.bigkgolap.engines.aixm.mapper.AixmMapper.map(AixmMapper.java:24) ~[main/:na]
	at at.jku.dke.bigkgolap.bed.file.FileLoaderService.loadFromFiles(FileLoaderService.java:39) ~[main/:na]
	at at.jku.dke.bigkgolap.bed.service.GraphService.loadGraph(GraphService.java:45) ~[main/:na]
	at at.jku.dke.bigkgolap.bed.service.GraphService$$FastClassBySpringCGLIB$$ae92d738.invoke(<generated>) ~[main/:na]
	at org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:218) ~[spring-core-5.3.12.jar:5.3.12]
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:689) ~[spring-aop-5.3.12.jar:5.3.12]
	at at.jku.dke.bigkgolap.bed.service.GraphService$$EnhancerBySpringCGLIB$$4be51ed0.loadGraph(<generated>) ~[main/:na]
	at at.jku.dke.bigkgolap.bed.grpc.GraphQueryServiceImpl.queryGraph(GraphQueryServiceImpl.java:42) ~[main/:na]
	at at.jku.dke.bigkgolap.shared.grpc.GraphQueryServiceGrpc$MethodHandlers.invoke(GraphQueryServiceGrpc.java:180) ~[main/:na]
	at io.grpc.stub.ServerCalls$UnaryServerCallHandler$UnaryServerCallListener.onHalfClose(ServerCalls.java:182) ~[grpc-stub-1.43.1.jar:1.43.1]
	at io.grpc.PartialForwardingServerCallListener.onHalfClose(PartialForwardingServerCallListener.java:35) ~[grpc-api-1.43.1.jar:1.43.1]
	at io.grpc.ForwardingServerCallListener.onHalfClose(ForwardingServerCallListener.java:23) ~[grpc-api-1.43.1.jar:1.43.1]
	at io.grpc.ForwardingServerCallListener$SimpleForwardingServerCallListener.onHalfClose(ForwardingServerCallListener.java:40) ~[grpc-api-1.43.1.jar:1.43.1]
	at io.grpc.PartialForwardingServerCallListener.onHalfClose(PartialForwardingServerCallListener.java:35) ~[grpc-api-1.43.1.jar:1.43.1]
	at io.grpc.ForwardingServerCallListener.onHalfClose(ForwardingServerCallListener.java:23) ~[grpc-api-1.43.1.jar:1.43.1]
	at io.grpc.ForwardingServerCallListener$SimpleForwardingServerCallListener.onHalfClose(ForwardingServerCallListener.java:40) ~[grpc-api-1.43.1.jar:1.43.1]
	at io.grpc.Contexts$ContextualizedServerCallListener.onHalfClose(Contexts.java:86) ~[grpc-api-1.43.1.jar:1.43.1]
	at io.grpc.internal.ServerCallImpl$ServerStreamListenerImpl.halfClosed(ServerCallImpl.java:340) ~[grpc-core-1.43.1.jar:1.43.1]
	at io.grpc.internal.ServerImpl$JumpToApplicationThreadServerStreamListener$1HalfClosed.runInContext(ServerImpl.java:866) ~[grpc-core-1.43.1.jar:1.43.1]
	at io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37) ~[grpc-core-1.43.1.jar:1.43.1]
	at io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133) ~[grpc-core-1.43.1.jar:1.43.1]
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128) ~[na:na]
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628) ~[na:na]
	at java.base/java.lang.Thread.run(Thread.java:835) ~[na:na]
Caused by: org.xml.sax.SAXParseException: Premature end of file.
	at java.xml/com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.createSAXParseException(ErrorHandlerWrapper.java:204) ~[na:na]
	at java.xml/com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper.fatalError(ErrorHandlerWrapper.java:178) ~[na:na]
	at java.xml/com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(XMLErrorReporter.java:400) ~[na:na]
	at java.xml/com.sun.org.apache.xerces.internal.impl.XMLErrorReporter.reportError(XMLErrorReporter.java:327) ~[na:na]
	at java.xml/com.sun.org.apache.xerces.internal.impl.XMLScanner.reportFatalError(XMLScanner.java:1471) ~[na:na]
	at java.xml/com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl$PrologDriver.next(XMLDocumentScannerImpl.java:1013) ~[na:na]
	at java.xml/com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:605) ~[na:na]
	at java.xml/com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:534) ~[na:na]
	at java.xml/com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:888) ~[na:na]
	at java.xml/com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:824) ~[na:na]
	at java.xml/com.sun.org.apache.xerces.internal.parsers.XMLParser.parse(XMLParser.java:141) ~[na:na]
	at java.xml/com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1216) ~[na:na]
	at java.xml/com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse(SAXParserImpl.java:635) ~[na:na]
	at java.xml/com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl.parse(SAXParserImpl.java:324) ~[na:na]
	at java.xml/javax.xml.parsers.SAXParser.parse(SAXParser.java:197) ~[na:na]
	at at.jku.dke.bigkgolap.engines.aixm.mapper.AixmSAXModelBuilder.parse(AixmSAXModelBuilder.java:55) ~[main/:na]
	at at.jku.dke.bigkgolap.engines.aixm.mapper.AixmMapper.map(AixmMapper.java:22) ~[main/:na]
	... 23 common frames omitted
     */
    public void parse(String fileData) throws IOException, SAXException {
        this.saxParser.parse(new ByteArrayInputStream(fileData.getBytes()), this);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (qName == null) {
            return;
        }

        if (!qName.startsWith(AIXM_PREFIX)) {
            previousTag = qName;
            return;
        }

        if (!rootElemFound && previousTag.equals(MESSAGE_HAS_MEMBER)) {
            // aixm root tag found -> Add it as root resource
            Resource newResource = model.createResource(cu(BASE_URI, attributes.getValue(GML_ID_ATT)))
                    .addProperty(RDF.type, aixmPrefix(qName));
            // customModel.newTriple(cu(BASE_URI, attributes.getValue(GML_ID_ATT)), RDF.type, aixmPrefix(qName))

            resourceStack.push(newResource);
            rootElemFound = true;
            return;
        }

        if (capturingModeCount > 0) {
            // Inside a supported concept

            String gmlId = attributes.getValue(GML_ID_ATT);

            if (gmlId != null) {
                // gml:id found -> New subject
                Resource newResource = model.createResource(cu(BASE_URI, gmlId))
                        .addProperty(RDF.type, aixmPrefix(qName));
                resourceStack.peek().addProperty(cp(BASE_URI, aixmPrefix(previousTag)), newResource);
                resourceStack.push(newResource);
            }
        }

        if (SUPPORTED_CONCEPTS.contains(qName)) {
            capturingModeCount++;
        }

        previousTag = qName;
    }

    private String aixmPrefix(String localName) {
        return localName.substring(AIXM_PREFIX.length());
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (capturingModeCount > 0) {
            String chars = new String(ch, start, length);
            if (!chars.isBlank()) {
                resourceStack.peek().addLiteral(cp(BASE_URI, aixmPrefix(previousTag)), chars);
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (SUPPORTED_CONCEPTS.contains(qName)) {
            capturingModeCount--;
        }

        if (resourceStack.size() > 1 && resourceStack.peek().getProperty(RDF.type).getObject().toString().equals(aixmPrefix(qName))) {
            // tag representing the current subject ended
            resourceStack.pop();
        }
    }

}
