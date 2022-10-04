package at.jku.dke.bigkgolap.engines.aixm.analyzer;

import at.jku.dke.bigkgolap.api.engines.Analyzer;
import at.jku.dke.bigkgolap.api.engines.AnalyzerResult;
import at.jku.dke.bigkgolap.api.model.CubeSchema;
import at.jku.dke.bigkgolap.api.model.Hierarchy;
import at.jku.dke.bigkgolap.api.model.HierarchyFactory;
import org.apache.jena.ext.com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

public class AixmAnalyzer extends DefaultHandler implements Analyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AixmAnalyzer.class);


    public AixmAnalyzer() {
    }

    public static void main(String[] args) throws IOException {
        File f = new File("D:\\workspace\\aixm-gen\\data\\20180101_EDMM_AircraftStand_1.xml");
        AixmAnalyzer aixmAnalyzer = new AixmAnalyzer();
        AnalyzerResult analyze = aixmAnalyzer.analyze(new FileInputStream(f));
        LOGGER.info("{}", analyze);
    }

    private List<Hierarchy> loadLocationHierarchies(AixmSAXContextExtractor sax) {
        String location = sax.getLocation();
        String affectedFir = sax.getAffectedFir();
        if (Strings.isNullOrEmpty(location) || Objects.equals(location, affectedFir)) {
            // neither LOCATION nor FIR specified, assuming ALL level
            if (Strings.isNullOrEmpty(affectedFir)) {
                return List.of(Hierarchy.all("location"));
            }
            return List.of(HierarchyFactory.get(CubeSchema.getInstance().locate("location", "fir"), affectedFir));
        }
        return List.of(HierarchyFactory.get(CubeSchema.getInstance().locate("location", "location"), location));
    }

    private List<Hierarchy> loadTimeHierarchies(AixmSAXContextExtractor sax) {
        LocalDate begin = sax.getBeginPositionDate();
        LocalDate end = sax.getEndPositionDate();

        if (begin == null) {
            if (end == null) {
                return List.of(Hierarchy.all("time"));
            }
            begin = end;
        } else if (end == null) {
            end = begin;
        }

        Set<LocalDate> dates = new HashSet<>();
        LocalDate next = begin;

        do {
            dates.add(next);
            next = next.plusDays(1);
        } while (!next.isAfter(end));

        List<Hierarchy> hierarchies = new ArrayList<>();
        for (LocalDate date : dates) {
            hierarchies.add(HierarchyFactory.get(CubeSchema.getInstance().locate("time", "day"), date));
        }
        return hierarchies;
    }

    @Override
    public AnalyzerResult analyze(InputStream fileData) {

        AixmSAXContextExtractor aixmSAXContextExtractor = new AixmSAXContextExtractor();
        try {
            aixmSAXContextExtractor.parse(fileData);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }


        Set<Hierarchy> dimensionHierarchies = new HashSet<>();

        for (String dimension : CubeSchema.getInstance().getDimensions()) {
            switch (dimension) {
                case "time":
                    dimensionHierarchies.addAll(loadTimeHierarchies(aixmSAXContextExtractor));
                    break;
                case "location":
                    dimensionHierarchies.addAll(loadLocationHierarchies(aixmSAXContextExtractor));
                    break;
                case "topic":
                    dimensionHierarchies.add(HierarchyFactory.get(CubeSchema.getInstance().locate("topic", "feature"), aixmSAXContextExtractor.getTopic()));
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + dimension);
            }
        }

        return new AnalyzerResult(dimensionHierarchies);
    }
}
