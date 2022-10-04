package at.jku.dke.bigkgolap.bed.service;

import lombok.Data;
import org.apache.jena.graph.Graph;

@Data
public class GraphResult {
    private final Graph graph;
    private final boolean fromInMemCache;
    private final Float fileCachePercentage;

    public static GraphResult fromInMemCache(Graph graph) {
        return new GraphResult(graph, true, null);
    }

    public static GraphResult fromFiles(Graph graph, float fileCachePercentage) {
        return new GraphResult(graph, false, fileCachePercentage);
    }
}
