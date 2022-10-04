
package at.jku.dke.bigkgolap.surface.query;

import at.jku.dke.bigkgolap.api.model.Context;
import at.jku.dke.bigkgolap.api.model.Member;
import at.jku.dke.bigkgolap.api.model.MergeLevels;
import at.jku.dke.bigkgolap.api.model.SliceDiceContext;
import at.jku.dke.bigkgolap.shared.config.LakehouseConfig;
import at.jku.dke.bigkgolap.shared.context.DatabaseService;
import at.jku.dke.bigkgolap.shared.exceptions.GraphNotAvailableException;
import at.jku.dke.bigkgolap.shared.service.NQuadWriter;
import at.jku.dke.bigkgolap.surface.exceptions.GraphQueryTimeoutException;
import at.jku.dke.bigkgolap.surface.graph.GraphQueryServiceClient;
import at.jku.dke.bigkgolap.surface.rest.CubeResult;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class QueryService {

    private static final int GRAPH_QUERY_TIMEOUT_SECONDS = 5 * 60 * 1000; // TODO change

    private final LakehouseConfig config;

    private final DatabaseService databaseService;

    private final MergeAndPropagateService mapService;

    private final QueryParserService queryParserService;

    private final GraphQueryServiceClient graphQueryServiceClient;

    public QueryService(LakehouseConfig config, DatabaseService databaseService, MergeAndPropagateService mapService, QueryParserService queryParserService, GraphQueryServiceClient graphQueryServiceClient) {
        this.config = config;
        this.databaseService = databaseService;
        this.mapService = mapService;
        this.queryParserService = queryParserService;
        this.graphQueryServiceClient = graphQueryServiceClient;
    }

    public CubeResult getCube(String kgOlapQuery, String requestUuid) {
        SliceDiceContext sliceDiceContext = queryParserService.parseSelectClauseOrThrow(kgOlapQuery);
        MergeLevels mergeLevels = queryParserService.parseRollupOnOrThrow(kgOlapQuery);

        if (mergeLevels != null && !mergeLevels.getMergeLevels().isEmpty()) {
            log.info("Merging cube at following levels: [{}]", mergeLevels.getMergeLevels().entrySet());
        }

        // 1.
        Set<Context> specificContexts = this.databaseService.getSpecificContexts(sliceDiceContext);
        log.info("Stored contexts to slice and dice: {}", specificContexts.size());


        final Instant ts1QueryRelevant = ZonedDateTime.now().toInstant();

        // 2
        Set<String> generalContextIds = this.databaseService.getGeneralContexts(sliceDiceContext);
        log.info("General contexts to build KG from: {}", generalContextIds.size());


        final Instant ts2QueryGeneral = ZonedDateTime.now().toInstant();

        // 3
        final MergeAndPropagateResult result = this.mapService.mergeAndPropagate(specificContexts, mergeLevels);

        final Map<String, Set<Context>> knowledgePropMap = result.getContextMap();
        final Set<Context> finalContexts = result.getFinalContexts();
        for (String generalContextId : generalContextIds) {
            knowledgePropMap.put(generalContextId, finalContexts);
        }

        final Instant ts3PrepareQuery = ZonedDateTime.now().toInstant();

        int consideredContexts = knowledgePropMap.size();
        log.info("Built cube with {} contexts.", consideredContexts);
        try {
            final Set<String> quads = buildCube(knowledgePropMap, requestUuid);
            return new CubeResult(true, consideredContexts, finalContexts.size(), quads, quads.size(), ts1QueryRelevant, ts2QueryGeneral, ts3PrepareQuery);
        } catch (GraphNotAvailableException e) {
            return new CubeResult(false, consideredContexts, 0, Set.of(), 0L, ts1QueryRelevant, ts2QueryGeneral, ts3PrepareQuery);
        }

    }

    private Set<String> buildCube(Map<String, Set<Context>> knowledgePropMap, String requestUuid) {

        final Set<String> quads = Sets.newConcurrentHashSet();
        final List<CountDownLatch> parallelCdls = new ArrayList<>(knowledgePropMap.size());
        final ConcurrentLinkedQueue<String> addedData = new ConcurrentLinkedQueue<>();

        AtomicInteger sentRequests = new AtomicInteger();

        knowledgePropMap.entrySet().parallelStream().forEach(entry -> {
            final String contextId = entry.getKey();
            final Set<Context> finalContexts = entry.getValue();
            final Set<String> graphs = new HashSet<>();

            for (Context finalContext : finalContexts) {
                Node graph = NodeFactory.createURI(config.getGraphBaseUri() + "context/" + finalContext.getUniqueContextName());
                addContextMembers(quads, graph, finalContext);
                graphs.add(graph.toString());
            }

            /* LINZ-DEZ.9, LINZ-DEZ,  Austria-2021, AUSTRIA-DEZ --> LINZ-DEZ.9 */
            queryQuads(requestUuid, contextId, graphs, quads, parallelCdls, addedData);
            sentRequests.getAndIncrement();
        });

        for (CountDownLatch cdl : parallelCdls) {
            try {
                if (!cdl.await(GRAPH_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    throw new GraphQueryTimeoutException("Did not receive graph within " + GRAPH_QUERY_TIMEOUT_SECONDS + " seconds!");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting for graph queries to return! ", e);
            }
        }

        log.info("SENT REQUESTS: {}, ANSWERS: {}, MISSING: {}", sentRequests, addedData.size(), (sentRequests.get() - addedData.size()));

        if ((sentRequests.get() - addedData.size()) > 0) {
            throw new GraphNotAvailableException("Some contexts could not be queried!");
        }
        return quads;
    }

    private void queryQuads(String requestUuid, String contextId, Set<String> graphs, Set<String> quads, List<CountDownLatch> parallelCdls,
                            ConcurrentLinkedQueue<String> addedData) {
        parallelCdls.add(graphQueryServiceClient.queryQuads(requestUuid, contextId, graphs, graphQueryResponse -> {
            if (graphQueryResponse == null) {
                log.error("Did not receive data!");
                return;
            }
            quads.addAll(graphQueryResponse.getQuadsList());
        }, unused -> addedData.add(contextId)));
    }

    private void addContextMembers(Set<String> quads, Node contextName, Context context) {
        final NQuadWriter writer = new NQuadWriter();
        for (Member member : context.getFlatMembers()) {
            quads.add(writer.writeTriple(Triple.create(contextName,
                    NodeFactory.createURI(config.getGraphBaseUri() + member.getLevel().toString().toLowerCase(Locale.ROOT)),
                    NodeFactory.createLiteral(member.getValue() != null ? member.getValue().toString() : "NULL"))));
        }
    }
}
