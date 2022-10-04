package at.jku.dke.bigkgolap.shared.context;

import at.jku.dke.bigkgolap.api.model.Context;
import at.jku.dke.bigkgolap.api.model.LakehouseFile;
import at.jku.dke.bigkgolap.api.model.SliceDiceContext;
import at.jku.dke.bigkgolap.shared.model.LakehouseStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class DatabaseService {

    private final LakehouseRepository repo;

    public DatabaseService(LakehouseRepository repo) {
        this.repo = repo;
    }

    public boolean upsertFileDetails(String fileId, String fileType, String originalName, long bytes) {
        return repo.upsertFileDetails(fileId, fileType, originalName, bytes);
    }

    public List<LakehouseFile> getFiles(String contextId) {
        return repo.getLakehouseFiles(contextId);
    }

    /**
     * Specific contexts
     *
     * @param context search context to slice'n'dice
     * @return slice'n'dice'd stored contexts
     */
    public Set<Context> getSpecificContexts(SliceDiceContext context) {
        return repo.getSpecificContexts(context);
    }


    public Set<String> getGeneralContexts(SliceDiceContext context) {
        return repo.getGeneralContextIds(context);
    }

    public LakehouseStats getLakehouseStats() {
        return repo.getLakehouseStats();
    }

    // Quick test
    //@EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() {
        log.info("Querying Lakehouse stats");
        LakehouseStats lakehouseStats = getLakehouseStats();
        log.info("----- LAKEHOUSE STATS -----");
        log.info("Total contexts: {}\n" +
                        "Total files: {}\n" +
                        "Total indices: {}\n" +
                        "Total file size MiB: {}", lakehouseStats.getTotalContexts(), lakehouseStats.getTotalFiles(),
                lakehouseStats.getTotalIndices(), (lakehouseStats.getTotalStoredFileSizeBytes() / 1024 / 1024));
        log.info("----- LAKEHOUSE STATS -----");
        log.info("End querying Lakehouse stats");
    }

    public boolean upsertFile(Context context, String storedName, String fileType) {
        return repo.upsertFile(context, storedName, fileType);
    }

    public boolean upsertContext(Context context) {
        return repo.upsertContext(context);
    }
}
