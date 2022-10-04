package at.jku.dke.bigkgolap.bgprocessor.indexing;

import at.jku.dke.bigkgolap.api.engines.AnalyzerResult;
import at.jku.dke.bigkgolap.api.engines.Engine;
import at.jku.dke.bigkgolap.api.model.Context;
import at.jku.dke.bigkgolap.api.model.CubeSchema;
import at.jku.dke.bigkgolap.api.model.Hierarchy;
import at.jku.dke.bigkgolap.shared.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class ContextExtractionService {

    private final StorageService storage;

    private final Map<String, Engine> engines;

    public ContextExtractionService(StorageService storage, Map<String, Engine> engines) {
        this.storage = storage;
        this.engines = engines;
    }

    public Set<Context> analyze(String storedName, String fileType) {
        try {
            Engine engineForFile = getEngineOrThrow(fileType);
            AnalyzerResult analyzerResult = engineForFile.getAnalyzer().analyze(storage.getInputStream(storedName));
            Set<Hierarchy> hierarchies = analyzerResult.getHierarchies();
            return toCartesianProduct(hierarchies);
        } catch (Exception e) {
            log.error("Could not analyze file due to exception", e);
            throw new RuntimeException(e);
        }
    }

    private Engine getEngineOrThrow(String fileType) {
        Engine engineForFile = this.engines.getOrDefault(fileType.toUpperCase(Locale.ROOT), null);
        if (engineForFile == null) {
            throw new IllegalArgumentException("No engine registered for file type: " + fileType);
        }
        return engineForFile;
    }

    private Set<Context> toCartesianProduct(Set<Hierarchy> hierarchies) {
        List<List<Hierarchy>> byDimension = split(hierarchies);

        // generate cartesian product of hierarchies from different dimensions
        List<List<Hierarchy>> lists = Lists.cartesianProduct(byDimension);

        Set<Context> contexts = new HashSet<>();

        for (List<Hierarchy> index : lists) {
            Map<String, Hierarchy> indexMap = new HashMap<>();
            for (Hierarchy hierarchy : index) {
                indexMap.put(hierarchy.getDimension(), hierarchy);
            }
            contexts.add(new Context(indexMap));
        }
        return contexts;
    }

    private static List<List<Hierarchy>> split(Set<Hierarchy> hierarchies) {
        List<List<Hierarchy>> byDimension = new ArrayList<>();
        for (String dimension : CubeSchema.getInstance().getDimensions()) {
            List<Hierarchy> dimensionList = new ArrayList<>();
            for (Hierarchy hierarchy : hierarchies) {
                if (hierarchy.getDimension().equals(dimension)) {
                    dimensionList.add(hierarchy);
                }
            }
            byDimension.add(dimensionList);
        }
        return byDimension;
    }
}
