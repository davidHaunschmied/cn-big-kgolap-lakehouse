package at.jku.dke.bigkgolap.bed.grpc;

import at.jku.dke.bigkgolap.bed.service.GraphResult;
import at.jku.dke.bigkgolap.bed.service.GraphService;
import at.jku.dke.bigkgolap.shared.exceptions.GraphNotAvailableException;
import at.jku.dke.bigkgolap.shared.grpc.GraphQueryRequest;
import at.jku.dke.bigkgolap.shared.grpc.GraphQueryResponse;
import at.jku.dke.bigkgolap.shared.grpc.GraphQueryServiceGrpc;
import at.jku.dke.bigkgolap.shared.querylog.QueryLoggingRepository;
import at.jku.dke.bigkgolap.shared.service.NQuadWriter;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.springframework.scheduling.annotation.Async;

import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

@GrpcService
@Slf4j
public class GraphQueryServiceImpl extends GraphQueryServiceGrpc.GraphQueryServiceImplBase {

    // 40 KiB (divided by 2 as a string takes 2 bytes per character)
    // calculating with 300 chars per line
    private static final int CHUNK_SIZE_LINES = (40 * 1024 / 2) / 300;

    private final GraphService graphService;

    private final QueryLoggingRepository queryLoggingRepo;

    public GraphQueryServiceImpl(GraphService graphService, QueryLoggingRepository queryLoggingRepo) {
        this.graphService = graphService;
        this.queryLoggingRepo = queryLoggingRepo;
    }

    @Override
    public void queryGraph(GraphQueryRequest request, StreamObserver<GraphQueryResponse> responseObserver) {
        log.info("Incoming request for context {}", request.getContextId());
        GraphResult graphResult = graphService.loadGraph(request.getContextId());
        log.info("Loaded graph for context {}", request.getContextId());
        if (graphResult == null || graphResult.getGraph() == null) {
            responseObserver.onError(new GraphNotAvailableException("Could not load RDF data for context " + request.getContextId() + " because no files were found."));
            return;
        }

        Set<Node> contextNodes = new HashSet<>();
        for (int i = 0; i < request.getGraphsCount(); i++) {
            contextNodes.add(NodeFactory.createURI(request.getGraphs(i)));
        }

        if (contextNodes.isEmpty()) {
            responseObserver.onError(new IllegalArgumentException(format("No graphs given; This needs to be fixed. DEBUG data: graphs = %s", request.getGraphsList())));
            return;
        }

        writeQuads(graphResult.getGraph(), contextNodes, responseObserver);

        responseObserver.onCompleted();
        log.info("Completed request for context {}", request.getContextId());
        registerRequestAsync(request, graphResult);
    }

    @Async
    protected void registerRequestAsync(GraphQueryRequest request, GraphResult graphResult) {
        try {
            this.queryLoggingRepo.registerContextRequest(request.getQueryUuid(), request.getContextId(), graphResult.isFromInMemCache(), graphResult.getFileCachePercentage());
        } catch (Exception e) {
            log.error("Could not register request!", e);
        }
    }

    private void writeQuads(Graph graph, Set<Node> contextNodes, StreamObserver<GraphQueryResponse> responseObserver) {
        final NQuadWriter nQuadWriter = new NQuadWriter();
        final GraphQueryResponse.Builder builder = GraphQueryResponse.newBuilder();
        final ExtendedIterator<Triple> tripleIterator = graph.find();

        int currentLines = 0;

        while (tripleIterator.hasNext()) {
            Triple triple = tripleIterator.next();

            for (Node graphNode : contextNodes) {
                builder.addQuads(nQuadWriter.writeQuad(triple, graphNode));
                currentLines++;
            }

            if (currentLines > CHUNK_SIZE_LINES) {
                responseObserver.onNext(builder.build());

                // reset
                builder.clear();
                currentLines = 0;
            }
        }

        responseObserver.onNext(builder.build());
    }
}
