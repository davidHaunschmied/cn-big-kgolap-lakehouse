package at.jku.dke.bigkgolap.surface.graph;

import at.jku.dke.bigkgolap.api.model.Context;
import at.jku.dke.bigkgolap.surface.exceptions.GraphQueryTimeoutException;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class GraphService {

    private static final int GRAPH_QUERY_TIMEOUT_SECONDS = 60;

    public Map<Context, Model> queryGraphMap(Set<Context> contexts) {
        final Map<Context, Model> triplesMap = new ConcurrentHashMap<>(contexts.size());
        final List<CountDownLatch> cdls = new ArrayList<>(contexts.size());
        for (Context context : contexts) {
            //cdls.add(graphQueryServiceClient.queryGraph(context, triplesMap));
        }

        for (CountDownLatch cdl : cdls) {
            try {
                if (!cdl.await(GRAPH_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    throw new GraphQueryTimeoutException("Did not receive graph within " + GRAPH_QUERY_TIMEOUT_SECONDS + " seconds!");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting for qraph queries to return! ", e);
            }
        }

        return triplesMap;
    }
}
