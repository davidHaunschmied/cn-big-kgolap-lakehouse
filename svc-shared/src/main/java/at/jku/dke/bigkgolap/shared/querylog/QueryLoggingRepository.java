package at.jku.dke.bigkgolap.shared.querylog;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface QueryLoggingRepository {

    void registerQueryStart(String uuid, String query);

    void registerQueryFailed(String uuid, String exceptionMessage);

    void registerQueryFailed(String uuid, String exceptionMessage, Instant ts1QueryRelevant, Instant ts2QueryGeneral, Instant ts3PrepareQuery);

    void registerQuerySucceeded(String uuid, int consideredContexts, int nrOfContexts, long nrOfQuads, Instant ts1QueryRelevant, Instant ts2QueryGeneral, Instant ts3PrepareQuery);

    void registerContextRequest(String uuid, String contextId, boolean fromInMemCache, Float fileCachePercentage) throws ExecutionException, InterruptedException, TimeoutException;

    List<QueryLog> getLogs();

    QueryLog getLog(String uuid);
}
