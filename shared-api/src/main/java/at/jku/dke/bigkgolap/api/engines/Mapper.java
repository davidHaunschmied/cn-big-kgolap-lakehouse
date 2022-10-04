package at.jku.dke.bigkgolap.api.engines;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public interface Mapper {

    static Property cp(String baseUri, String localName) {
        return ResourceFactory.createProperty(cu(baseUri, localName));
    }

    // uri
    static String cu(String baseUri, String localName) {
        return baseUri + localName;
    }

    void map(String data, Model model);
}
