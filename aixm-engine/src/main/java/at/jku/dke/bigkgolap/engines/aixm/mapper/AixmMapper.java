package at.jku.dke.bigkgolap.engines.aixm.mapper;

import at.jku.dke.bigkgolap.api.engines.Mapper;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class AixmMapper implements Mapper {

    @Override
    public void map(String data, Model model) {
        AixmToRdfSAXParser aixmToRdfSAXParser = new AixmToRdfSAXParser(model);
        try {
            aixmToRdfSAXParser.parse(data);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        File f = new File("C:\\workspace\\master-thesis\\notams-with-ph\\ph_DN_ACG.UNS_EADD.xml");
        AixmMapper aixmMapper = new AixmMapper();
        Model defaultModel = ModelFactory.createDefaultModel();
        aixmMapper.map(IOUtils.toString(new FileInputStream(f), Charset.defaultCharset()), defaultModel);
        defaultModel.write(System.out, "N-TRIPLE");
    }
}
