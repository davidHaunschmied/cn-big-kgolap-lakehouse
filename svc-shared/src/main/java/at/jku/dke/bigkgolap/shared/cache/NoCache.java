package at.jku.dke.bigkgolap.shared.cache;

import at.jku.dke.bigkgolap.api.model.Context;
import org.apache.jena.graph.Graph;

import java.util.Collection;

public class NoCache implements GraphCache {

    @Override
    public Graph loadGraph(String contextId) {
        return null;
    }

    @Override
    public void upsertGraph(String contextId, Graph graph) {

    }

    @Override
    public void deleteCachedGraphs(Collection<Context> contexts) {

    }

    @Override
    public String clear() {
        return "Cache not active";
    }
}
