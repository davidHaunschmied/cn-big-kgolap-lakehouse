package at.jku.dke.bigkgolap.shared.service;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.writer.WriterStreamRDFPlain;
import org.apache.jena.sparql.core.Quad;
import org.springframework.stereotype.Service;

@Service
public class NQuadWriter {

    private final ReuseableStringWriter stringWriter;

    private final WriterStreamRDFPlain writer;

    public NQuadWriter() {
        stringWriter = new ReuseableStringWriter();
        writer = new WriterStreamRDFPlain(IO.wrap(stringWriter));
    }

    public String writeQuad(Triple triple, Node graphNode) {
        return writeQuad(Quad.create(graphNode, triple));
    }

    public String writeQuad(Quad quad) {
        stringWriter.reset();
        writer.quad(quad);
        writer.finish();
        return stringWriter.toString();
    }

    public String writeTriple(Triple triple) {
        stringWriter.reset();
        writer.triple(triple);
        writer.finish();
        return stringWriter.toString();
    }
}
