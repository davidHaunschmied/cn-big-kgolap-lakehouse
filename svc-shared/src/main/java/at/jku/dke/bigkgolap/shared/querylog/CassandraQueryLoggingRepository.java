package at.jku.dke.bigkgolap.shared.querylog;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.cql.AsyncCqlTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Repository
@Slf4j
public class CassandraQueryLoggingRepository implements QueryLoggingRepository {

    private static final String UPSERT_QUERY_START = "UPDATE lakehouse.query_log " +
            "SET start = toTimeStamp(now()), query = ? WHERE id = ?";
    private static final String UPSERT_QUERY_FAILED = "UPDATE lakehouse.query_log " +
            "SET end = toTimeStamp(now()), success = false, exception_message = ? WHERE id = ?";
    private static final String UPSERT_QUERY_FAILED_TIMINGS = "UPDATE lakehouse.query_log " +
            "SET end = toTimeStamp(now()), success = false, exception_message = ?, ts_1_query_relevant = ?, " +
            "ts_2_query_general = ?, ts_3_prepare_query = ? WHERE id = ?";
    private static final String UPSERT_QUERY_SUCCEEDED = "UPDATE lakehouse.query_log " +
            "SET end = toTimeStamp(now()), success = true, considered_contexts = ?, contexts = ?, quads = ?, ts_1_query_relevant = ?, " +
            "ts_2_query_general = ?, ts_3_prepare_query = ? WHERE id = ?";
    private static final String UPSERT_CONTEXT_REQUEST = "UPDATE lakehouse.query_log " +
            "SET ctx_rq_in_mem_cache_hits = ctx_rq_in_mem_cache_hits + ?, " +
            "ctx_rq_local_file_cache_hit_pct = ctx_rq_local_file_cache_hit_pct + ? WHERE id = ?";
    private static final String SELECT_ALL = "SELECT * FROM lakehouse.query_log";
    private static final String SELECT_SINGLE = "SELECT * FROM lakehouse.query_log WHERE id = ?";

    private final Map<String, PreparedStatement> prepStmts;
    private final AsyncCqlTemplate cassie;

    public CassandraQueryLoggingRepository(AsyncCqlTemplate cassie) {
        this.prepStmts = new HashMap<>();
        this.cassie = cassie;

        CqlSession session = Objects.requireNonNull(cassie.getSessionFactory()).getSession();
        prepareStmts(session);


        this.cassie.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
    }

    private void prepareStmts(CqlSession session) {
        prepStmts.put(UPSERT_QUERY_START, session.prepare(UPSERT_QUERY_START));
        prepStmts.put(UPSERT_QUERY_FAILED, session.prepare(UPSERT_QUERY_FAILED));
        prepStmts.put(UPSERT_QUERY_FAILED_TIMINGS, session.prepare(UPSERT_QUERY_FAILED_TIMINGS));
        prepStmts.put(UPSERT_QUERY_SUCCEEDED, session.prepare(UPSERT_QUERY_SUCCEEDED));
        prepStmts.put(UPSERT_CONTEXT_REQUEST, session.prepare(UPSERT_CONTEXT_REQUEST));
        prepStmts.put(SELECT_ALL, session.prepare(SELECT_ALL));
        prepStmts.put(SELECT_SINGLE, session.prepare(SELECT_SINGLE));
    }

    @Override
    public void registerQueryStart(String uuid, String query) {
        forget(cassie.execute(prepStmts.get(UPSERT_QUERY_START).bind(query, UUID.fromString(uuid))));

    }

    @Override
    public void registerQueryFailed(String uuid, String exceptionMessage) {
        forget(cassie.execute(prepStmts.get(UPSERT_QUERY_FAILED).bind(exceptionMessage, UUID.fromString(uuid))));

    }

    @Override
    public void registerQueryFailed(String uuid, String exceptionMessage, Instant ts1QueryRelevant, Instant ts2QueryGeneral, Instant ts3PrepareQuery) {
        forget(cassie.execute(prepStmts.get(UPSERT_QUERY_FAILED_TIMINGS).bind(exceptionMessage, ts1QueryRelevant,
                ts2QueryGeneral, ts3PrepareQuery, UUID.fromString(uuid))));

    }

    @Override
    public void registerQuerySucceeded(String uuid, int consideredContexts, int nrOfContexts, long nrOfQuads, Instant ts1QueryRelevant, Instant ts2QueryGeneral, Instant ts3PrepareQuery) {
        forget(cassie.execute(prepStmts.get(UPSERT_QUERY_SUCCEEDED).bind(consideredContexts,
                nrOfContexts, nrOfQuads, ts1QueryRelevant, ts2QueryGeneral, ts3PrepareQuery, UUID.fromString(uuid))));
    }

    @Override
    public void registerContextRequest(String uuid, String contextId, boolean fromInMemCache, Float fileCachePercentage) throws ExecutionException, InterruptedException, TimeoutException {
        cassie.execute(prepStmts.get(UPSERT_CONTEXT_REQUEST).bind(Map.of(contextId, fromInMemCache),
                        Map.of(contextId, fileCachePercentage != null ? fileCachePercentage : -1), UUID.fromString(uuid)))
                .get(4, TimeUnit.SECONDS);
    }

    @Override
    public List<QueryLog> getLogs() {
        try {
            return cassie.query(prepStmts.get(SELECT_ALL).bind(), (Row row, int rowNum) -> mapQueryLog(row)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public QueryLog getLog(String uuid) {
        try {
            return cassie.queryForObject(prepStmts.get(SELECT_SINGLE).bind(uuid), (row, rowNum) -> mapQueryLog(row)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private QueryLog mapQueryLog(Row row) {
        try {
            QueryLog.QueryLogBuilder builder = QueryLog.builder();
            Instant start = Optional.ofNullable(row.getInstant("start")).orElse(Instant.ofEpochMilli(0));
            Instant ts1QueryRelevant = Optional.ofNullable(row.getInstant("ts_1_query_relevant")).orElse(Instant.ofEpochMilli(0));
            Instant ts2QueryGeneral = Optional.ofNullable(row.getInstant("ts_2_query_general")).orElse(Instant.ofEpochMilli(0));
            Instant ts3PrepareQuery = Optional.ofNullable(row.getInstant("ts_3_prepare_query")).orElse(Instant.ofEpochMilli(0));
            Instant end = Optional.ofNullable(row.getInstant("end")).orElse(start);

            int nrOfRq = row.getInt("considered_contexts");
            return builder.query(row.getString("query"))
                    .start(start.atZone(ZoneId.systemDefault()))
                    .dur1QueryRelevantMillis(ts1QueryRelevant.toEpochMilli() - start.toEpochMilli())
                    .dur2QueryGeneralMillis(ts2QueryGeneral.toEpochMilli() - ts1QueryRelevant.toEpochMilli())
                    .dur3PrepareQueryMillis(ts3PrepareQuery.toEpochMilli() - ts2QueryGeneral.toEpochMilli())
                    .dur4BuildGraphMillis(end.toEpochMilli() - ts3PrepareQuery.toEpochMilli())
                    .end(end.atZone(ZoneId.systemDefault()))
                    .durationMillis(end.toEpochMilli() - start.toEpochMilli())
                    .success(row.getBoolean("success"))
                    .exceptionMessage(row.getString("exception_message"))
                    .contextRequests(nrOfRq)
                    .contexts(row.getInt("contexts"))
                    .quads(row.getLong("quads"))
                    .build();
        } catch (Exception e) {
            log.error("Corrupt query_log entry found!", e);
            return null;
        }
    }

    private void forget(ListenableFuture<Boolean> query) {
        query.addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onFailure(Throwable ex) {
                log.error("Async query failed!", ex);
            }

            @Override
            public void onSuccess(Boolean result) {
                // ignore
            }
        });
    }
}
