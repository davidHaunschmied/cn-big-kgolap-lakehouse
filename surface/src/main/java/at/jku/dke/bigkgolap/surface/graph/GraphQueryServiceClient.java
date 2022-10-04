package at.jku.dke.bigkgolap.surface.graph;

import at.jku.dke.bigkgolap.shared.config.LakehouseConfig;
import at.jku.dke.bigkgolap.shared.grpc.GraphQueryRequest;
import at.jku.dke.bigkgolap.shared.grpc.GraphQueryResponse;
import at.jku.dke.bigkgolap.shared.grpc.GraphQueryServiceGrpc;
import at.jku.dke.bigkgolap.shared.service.NQuadWriter;
import io.grpc.CompressorRegistry;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Service
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Slf4j
public class GraphQueryServiceClient {

    private static final int DEFAULT_RETRY_COUNT = 3;

    @Autowired
    private final NQuadWriter rdfConverter;

    private final GraphQueryServiceGrpc.GraphQueryServiceStub stub;

    public GraphQueryServiceClient(NQuadWriter NQuadWriter, LakehouseConfig config) {
        this.rdfConverter = NQuadWriter;
        log.info("Bed target: {}:{}", config.getBedHost(), config.getBedGrpcPort());
        stub = GraphQueryServiceGrpc.newStub(ManagedChannelBuilder
                .forAddress(config.getBedHost(), config.getBedGrpcPort())
                .usePlaintext()
                .executor(Executors.newCachedThreadPool())
                .compressorRegistry(CompressorRegistry.getDefaultInstance()).build());
    }

    /**
     * IDEA: Pass a set of graphs -> The service returns the quads for all graphs for a given context id
     *
     * @param contextId
     * @param graphs
     * @param responseConsumer
     * @return
     */
    public CountDownLatch queryQuads(String requestUuid, String contextId, Set<String> graphs, Consumer<GraphQueryResponse> responseConsumer,
                                     Consumer<Void> completeConsumer) {
        GraphQueryRequest request = GraphQueryRequest.newBuilder()
                .setQueryUuid(requestUuid)
                .setContextId(contextId)
                .addAllGraphs(graphs).build();

        final CountDownLatch finishLatch = new CountDownLatch(1);
        stub.queryGraph(request, new StreamObserver<>() {
            @Override
            public void onNext(GraphQueryResponse value) {
                responseConsumer.accept(value);
            }

            @Override
            public void onError(Throwable t) {
                log.error("Querying data for context id {} threw an error", contextId, t);
                responseConsumer.accept(null);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                completeConsumer.accept(null);
                finishLatch.countDown();
            }
        });
        return finishLatch;
    }
}
