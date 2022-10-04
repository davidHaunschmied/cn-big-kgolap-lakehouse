package at.jku.dke.bigkgolap.bed.service;

import at.jku.dke.bigkgolap.api.model.LakehouseFile;
import at.jku.dke.bigkgolap.bed.file.FileLoaderService;
import at.jku.dke.bigkgolap.shared.cache.GraphCache;
import at.jku.dke.bigkgolap.shared.context.DatabaseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.graph.Graph;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class GraphService {
    private final GraphCache graphCache;

    private final FileLoaderService fileLoaderService;

    private final DatabaseService databaseService;

    public GraphService(GraphCache graphCache, FileLoaderService fileLoaderService, DatabaseService databaseService) {
        this.graphCache = graphCache;
        this.fileLoaderService = fileLoaderService;
        this.databaseService = databaseService;
    }

    public GraphResult loadGraph(String contextId) {
        log.info("enter load graph [{}]", contextId);
        final Graph cached = graphCache.loadGraph(contextId);
        // graph is available in cache, nice
        if (cached != null) {
            return GraphResult.fromInMemCache(cached);
        }
        log.info("after checking cache [{}]", contextId);

        // graph is not available in cache, need to construct it from the files
        List<LakehouseFile> files = databaseService.getFiles(contextId);
        if (files == null || files.isEmpty()) {
            log.error("No files available for context with id {}", contextId);
            return null;
        }

        log.info("after loading relevant files metadata [{}]", contextId);

        final GraphResult graphResult = fileLoaderService.loadFromFiles(files);

        log.info("after loading graph from {} files [{}]", files.size(), contextId);

        // store graph in cache asynchronously
        addGraphToCache(contextId, graphResult.getGraph());

        return graphResult;
    }

    @Async
    protected void addGraphToCache(String contextId, Graph fresh) {
        graphCache.upsertGraph(contextId, fresh);
    }
}
