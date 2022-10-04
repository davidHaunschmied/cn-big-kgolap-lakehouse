package at.jku.dke.bigkgolap.shared.ingestionlog;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.cassandra.core.cql.AsyncCqlTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Repository
@Slf4j
public class CassandraFileIngestionLoggingRepository implements FileIngestionLoggingRepository {

    private static final String UPSERT_INGESTION_START = "UPDATE lakehouse.file_ingestion_log " +
            "SET start = ? WHERE stored_name = ?";
    private static final String UPSERT_INGESTION_END = "UPDATE lakehouse.file_ingestion_log " +
            "SET end = ? WHERE stored_name = ?";

    private static final String SELECT_ALL = "SELECT * FROM lakehouse.file_ingestion_log";

    private final Map<String, PreparedStatement> prepStmts;
    private final AsyncCqlTemplate cassie;

    public CassandraFileIngestionLoggingRepository(AsyncCqlTemplate cassie) {
        this.prepStmts = new HashMap<>();
        this.cassie = cassie;

        CqlSession session = Objects.requireNonNull(cassie.getSessionFactory()).getSession();
        prepareStmts(session);


        this.cassie.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
    }

    private void prepareStmts(CqlSession session) {
        prepStmts.put(UPSERT_INGESTION_START, session.prepare(UPSERT_INGESTION_START));
        prepStmts.put(UPSERT_INGESTION_END, session.prepare(UPSERT_INGESTION_END));
        prepStmts.put(SELECT_ALL, session.prepare(SELECT_ALL));
    }

    @Override
    public void ingestionStarted(String storedName) {
        forget(cassie.execute(prepStmts.get(UPSERT_INGESTION_START).bind(ZonedDateTime.now().toInstant(), storedName)));
    }

    @Override
    public boolean ingestionFinished(String storedName) {
        try {
            return cassie.execute(prepStmts.get(UPSERT_INGESTION_END).bind(ZonedDateTime.now().toInstant(), storedName)).get();
        } catch (DataAccessException | InterruptedException | ExecutionException e) {
            log.error("Could not upsert file {}: {}", storedName, e.getMessage());
            return false;
        }
    }

    @Override
    public List<FileIngestionLog> getLogs() {
        try {
            return cassie.query(prepStmts.get(SELECT_ALL).bind(), (Row row, int rowNum) -> {

                Instant start = Optional.ofNullable(row.getInstant("start")).orElse(Instant.ofEpochMilli(0));
                Instant end = Optional.ofNullable(row.getInstant("end")).orElse(Instant.ofEpochMilli(0));
                return new FileIngestionLog(row.getString("stored_name"), start.atZone(ZoneId.systemDefault()),
                        end.atZone(ZoneId.systemDefault()), end.toEpochMilli() - start.toEpochMilli());
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
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
