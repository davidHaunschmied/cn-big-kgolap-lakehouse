package at.jku.dke.bigkgolap.shared.cache;

import at.jku.dke.bigkgolap.api.model.Context;
import org.apache.jena.graph.Graph;

import java.util.Collection;

public interface GraphCache {

    Graph loadGraph(String contextId);

    void upsertGraph(String contextId, Graph graph);

    void deleteCachedGraphs(Collection<Context> contexts);

    String clear();
}
