package at.jku.dke.bigkgolap.shared.context;

import at.jku.dke.bigkgolap.api.model.*;
import at.jku.dke.bigkgolap.shared.model.LakehouseStats;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.Statement;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.DataAccessException;
import org.springframework.data.cassandra.core.cql.AsyncCqlTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static at.jku.dke.bigkgolap.shared.context.DbUtils.*;
import static java.lang.String.format;

@Repository
@Primary
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Slf4j
class CassandraContextRepository implements LakehouseRepository {

    // by dimension
    private final Map<String, PreparedStatement> upsertHierarchyStmts;
    private static final String UPSERT_HIERARCHY_BASE = "UPDATE lakehouse.%s SET hash = ?, " +
            "context_hashes = context_hashes + ? WHERE %s";

    // by dimension
    private final Map<String, PreparedStatement> selectSpecialHierarchiesStmts;
    private final Map<String, String> selectSpecialHierarchiesStmtsBase;
    private static final String SELECT_SPECIAL_HIERARCHIES_BASE = "SELECT hash, context_hashes, %s FROM lakehouse.%s";

    // by dimension
    private final Map<String, PreparedStatement> selectGeneralHierarchiesStmts;
    private static final String SELECT_GENERAL_HIERARCHIES_BASE = "SELECT hash, context_hashes, %s FROM lakehouse.%s WHERE %s";

    // by query
    private final Map<String, PreparedStatement> prepStmts;
    private static final String UPSERT_CONTEXT_BASE = "INSERT INTO lakehouse.contexts (hash, %s) VALUES (?, %s)";
    private static final String UPSERT_FILE = "INSERT INTO lakehouse.files (context_hash, stored_name, engine_type) VALUES (?, ?, ?)";
    private static final String UPSERT_FILE_DETAILS = "INSERT INTO lakehouse.file_details (stored_name, engine_type, original_name, size_bytes) VALUES (?, ?, ?, ?)";
    private static final String SELECT_FILES = "SELECT stored_name, engine_type FROM lakehouse.files WHERE context_hash = ?";
    private static final String SELECT_TOTAL_INDICES = "SELECT context_hash, stored_name FROM lakehouse.files";
    private static final String SELECT_TOTAL_CONTEXTS = "SELECT hash FROM lakehouse.contexts";
    private static final String SELECT_TOTAL_FILES = "SELECT stored_name FROM lakehouse.file_details";
    private static final String SELECT_TOTAL_SIZE_BYTES = "SELECT size_bytes FROM lakehouse.file_details";

    private final AsyncCqlTemplate cassie;
    private final CqlSession session;

    public CassandraContextRepository(AsyncCqlTemplate cassie, CqlSession session) {
        this.cassie = cassie;
        this.session = session;
        upsertHierarchyStmts = new HashMap<>();
        selectSpecialHierarchiesStmts = new HashMap<>();
        selectSpecialHierarchiesStmtsBase = new HashMap<>();
        selectGeneralHierarchiesStmts = new HashMap<>();
        prepStmts = new HashMap<>();

        prepareStmtsPerDimension(session);
        prepareOtherStmts(session);

        this.cassie.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
    }

    private void prepareStmtsPerDimension(CqlSession session) {
        for (String dimension : CubeSchema.getInstance().getDimensions()) {
            List<String> levels = CubeSchema.getInstance().idsByDimension(dimension);

            String upsertStmt = format(UPSERT_HIERARCHY_BASE, dimension, toWhereClause(levels));
            log.info("Preparing statement to upsert into dimension table {}: {}", dimension, upsertStmt);
            upsertHierarchyStmts.put(dimension, session.prepare(upsertStmt));

            String selectStmtBase = format(SELECT_SPECIAL_HIERARCHIES_BASE, csv(levels), dimension);
            selectSpecialHierarchiesStmtsBase.put(dimension, selectStmtBase);

            String selectGenStmtBase = format(SELECT_GENERAL_HIERARCHIES_BASE, csv(levels), dimension, toWhereClause(levels));
            log.info("Preparing statement to select general hierarchies from dimension table {}: {}", dimension, selectGenStmtBase);
            selectGeneralHierarchiesStmts.put(dimension, session.prepare(selectGenStmtBase));
        }
    }

    private void prepareOtherStmts(CqlSession session) {
        List<String> dimHashes = CubeSchema.getInstance().getDimensions().stream().map(dim -> dim + "_hash").collect(Collectors.toList());

        String upsertContextStmt = format(UPSERT_CONTEXT_BASE, csv(dimHashes), csQuestionMarks(dimHashes.size()));
        log.info("Preparing statement to upsert context: {}", upsertContextStmt);
        prepStmts.put(UPSERT_CONTEXT_BASE, session.prepare(upsertContextStmt));

        prepStmts.put(UPSERT_FILE, session.prepare(UPSERT_FILE));
        prepStmts.put(UPSERT_FILE_DETAILS, session.prepare(UPSERT_FILE_DETAILS));
        prepStmts.put(SELECT_FILES, session.prepare(SELECT_FILES));
        prepStmts.put(SELECT_TOTAL_INDICES, session.prepare(SELECT_TOTAL_INDICES));
        prepStmts.put(SELECT_TOTAL_CONTEXTS, session.prepare(SELECT_TOTAL_CONTEXTS));
        prepStmts.put(SELECT_TOTAL_FILES, session.prepare(SELECT_TOTAL_FILES));
        prepStmts.put(SELECT_TOTAL_SIZE_BYTES, session.prepare(SELECT_TOTAL_SIZE_BYTES));
    }

    @Override
    public boolean upsertFileDetails(String storedName, String fileType, String originalName, long sizeBytes) {
        try {
            return cassie.execute(prepStmts.get(UPSERT_FILE_DETAILS).bind(storedName, fileType, originalName, sizeBytes)).get();
        } catch (DataAccessException | InterruptedException | ExecutionException e) {
            log.error("Could not upsert file!", e);
            return false;
        }
    }

    @Override
    public boolean upsertFile(Context context, String storedName, String fileType) {
        try {
            return cassie.execute(prepStmts.get(UPSERT_FILE).bind(context.getId(), storedName, fileType)).get();
        } catch (DataAccessException | InterruptedException | ExecutionException e) {
            log.error("Could not upsert file!", e);
            return false;
        }
    }

    @Override
    public boolean upsertContext(Context context) {
        String contextId = context.getId();
        List<String> params = new ArrayList<>(context.getHierarchies().size() + 1);
        params.add(contextId);

        for (Map.Entry<String, Hierarchy> entry : context.getHierarchies().entrySet()) {
            if (!upsertHierarchy(entry.getKey(), entry.getValue(), contextId)) {
                return false;
            }
            params.add(entry.getValue().getId());
        }

        try {
            return cassie.execute(prepStmts.get(UPSERT_CONTEXT_BASE).bind(params.toArray())).get();
        } catch (DataAccessException | InterruptedException | ExecutionException e) {
            log.error("Could not upsert context {}: {}", context, e.getMessage());
            return false;
        }
    }

    @Override
    public LakehouseStats getLakehouseStats() {
        try {
            long totalIndices = queryAndGetRowCount(SELECT_TOTAL_INDICES);
            log.info("totalIndices: {}", totalIndices);
            long totalContexts = queryAndGetRowCount(SELECT_TOTAL_CONTEXTS);
            log.info("totalContexts: {}", totalContexts);
            long totalFiles = queryAndGetRowCount(SELECT_TOTAL_FILES);
            log.info("totalFiles: {}", totalFiles);
            long totalSizeBytes = queryAndSum(SELECT_TOTAL_SIZE_BYTES, "size_bytes");
            log.info("totalSizeBytes (in MiB): {}", (totalSizeBytes / 1024 / 1024));
            return LakehouseStats.builder()
                    .totalIndices(totalIndices)
                    .totalContexts(totalContexts)
                    .totalFiles(totalFiles)
                    .totalStoredFileSizeBytes(totalSizeBytes)
                    .build();
        } catch (RuntimeException e) {
            log.error("Could not query lakehouse stats", e);
            return null;
        }
    }

    private long queryAndSum(String query, String column) {
        Statement<?> statement = prepStmts.get(query).bind();

        long sum = 0;

        ByteBuffer pagingState;

        do {
            ResultSet resultSet = session.execute(statement);
            for (Row row : resultSet) {
                sum += row.get(column, Long.class);
            }
            pagingState = resultSet.getExecutionInfo().getPagingState();
            statement = statement.copy(pagingState);
        } while (pagingState != null);

        return sum;
    }

    private long queryAndGetRowCount(String query) {
        Statement<?> statement = prepStmts.get(query).bind();
        ResultSet resultSet = session.execute(statement);

        long count = resultSet.getAvailableWithoutFetching();
        ByteBuffer pagingState = resultSet.getExecutionInfo().getPagingState();

        while (pagingState != null) {
            statement = statement.copy(pagingState);
            resultSet = session.execute(statement);
            pagingState = resultSet.getExecutionInfo().getPagingState();
            count += resultSet.getAvailableWithoutFetching();
        }

        return count;
    }

    @Override
    public Set<Context> getSpecificContexts(SliceDiceContext context) {
        List<List<StoredHierarchy>> storedHierarchies = selectStoredHierarchies(context);

        Map<List<StoredHierarchy>, Set<String>> existingCombinations = toAllExistingHierarchyCombinations(storedHierarchies);

        Set<Context> specificContexts = new HashSet<>();
        for (Map.Entry<List<StoredHierarchy>, Set<String>> existingCombination : existingCombinations.entrySet()) {
            Context existingContext = Context.of(existingCombination.getKey().stream()
                    .collect(Collectors.toMap(storedHierarchy -> storedHierarchy.getHierarchy().getDimension(), StoredHierarchy::getHierarchy)));

            if (existingCombination.getValue().size() > 1) {
                throw new IllegalStateException(format("Context %s can not be associated to multiple context ids!", context));
            }

            specificContexts.add(existingContext);
        }
        return specificContexts;
    }

    @Override
    public Set<String> getGeneralContextIds(SliceDiceContext context) {
        List<List<StoredHierarchy>> storedHierarchies = selectGeneralStoredHierarchies(context);
        Map<List<StoredHierarchy>, Set<String>> existingCombinations = toAllExistingHierarchyCombinations(storedHierarchies);

        Set<String> generalContextIds = new HashSet<>();

        for (Map.Entry<List<StoredHierarchy>, Set<String>> existingCombination : existingCombinations.entrySet()) {
            SliceDiceContext existingContext = SliceDiceContext.of(existingCombination.getKey().stream()
                    .collect(Collectors.toMap(storedHierarchy -> storedHierarchy.getHierarchy().getDimension(),
                            StoredHierarchy::getHierarchy)));

            // the given context must not be part of the general contexts
            if (context.getHierarchies().equals(existingContext.getHierarchies())) {
                continue;
            }

            generalContextIds.addAll(existingCombination.getValue());
        }

        return generalContextIds;
    }

    private boolean upsertHierarchy(String dimension, Hierarchy hierarchy, String contextId) {
        List<Object> params = new ArrayList<>();
        params.add(hierarchy.getId());
        params.add(Set.of(contextId));

        addParams(params, dimension, hierarchy, true);

        try {
            return cassie.execute(upsertHierarchyStmts.get(dimension).bind(params.toArray())).get();
        } catch (DataAccessException | InterruptedException | ExecutionException e) {
            log.error("Could not upsert hierarchy {}", hierarchy, e);
            return false;
        }
    }

    private void addParams(List<Object> params, String dimension, Hierarchy hierarchy, boolean includeMissingValues) {
        Map<Level, Object> memberMap = hierarchy.getMembers().stream().collect(Collectors.toMap(Member::getLevel, Member::getValue));
        for (Level level : CubeSchema.getInstance().byDimension(dimension)) {
            if (!memberMap.containsKey(level) && !includeMissingValues) {
                return; // if higher level is not present, lower levels can not be set
            }
            Object value = memberMap.getOrDefault(level, null);
            params.add(toParam(level, value));
        }
    }

    /**
     * Takes a list of stored hierarchies of a dimension (e.g. List<time1, time2>, List<location1, location2>) and returns a cartesian product of all possible
     * combinations between the dimensions (e.g. time1 - location1, time2 - location1, time1 - location2, time2 - location2) provided that all stored hierarchies
     * part of the combination are associated to the same context id(s). The key of the returned map is the possible combination and the value is the set of associated
     * context ids.
     * If a combination contains a hierarchy from each dimension a concrete context can be identified, hence the set of context ids must be exactly 1.
     *
     * @param storedHierarchies hierarchies stored in the database
     * @return mapping from a possible hierarchy combination (~ Context) to a set of common contextIds associated to them (= intersection of context ids of all hierarchies in the combination).
     * If a possible hierarchy combination (~ Context) consists of one hierarchy for every dimension then the size of the set of context IDs will always be 1, otherwise an exception is thrown
     */
    private Map<List<StoredHierarchy>, Set<String>> toAllExistingHierarchyCombinations(List<List<StoredHierarchy>> storedHierarchies) {
        final int dimensions = CubeSchema.getInstance().getDimensions().size();

        List<List<StoredHierarchy>> allPossibleContextCombinations = Lists.cartesianProduct(storedHierarchies);
        Map<List<StoredHierarchy>, Set<String>> existingCombinations = new HashMap<>();

        for (List<StoredHierarchy> possibleContextCombination : allPossibleContextCombinations) {
            if (possibleContextCombination.isEmpty()) {
                continue;
            }
            // get contexts of any hierarchy and check if it is present in the others
            Set<String> associatedContexts = new HashSet<>();
            for (StoredHierarchy hierarchy : possibleContextCombination) {
                if (associatedContexts.isEmpty()) {
                    associatedContexts.addAll(hierarchy.getAssociatedContexts());
                    continue;
                }

                associatedContexts.retainAll(hierarchy.getAssociatedContexts());
                if (associatedContexts.isEmpty()) {
                    break;
                }
            }

            if (associatedContexts.size() > 0) {
                if (possibleContextCombination.size() == dimensions
                        && associatedContexts.size() > 1) {
                    throw new IllegalStateException(format("Found multiple context ids (%s) associated to complete hierarchy combination (= unique context) %s", associatedContexts, possibleContextCombination));
                }
                existingCombinations.put(possibleContextCombination, associatedContexts);
            }
        }

        return existingCombinations;
    }

    private List<List<StoredHierarchy>> selectStoredHierarchies(SliceDiceContext context) {
        SortedMap<String, Hierarchy> hierarchies = context.getHierarchies();
        List<List<StoredHierarchy>> storedHierarchies = new ArrayList<>();
        for (String dimension : CubeSchema.getInstance().getDimensions()) {
            // Use ALL-Hierarchy for absent hierarchies as this will return all stored hierarchies
            Hierarchy hierarchy = hierarchies.getOrDefault(dimension, Hierarchy.all(dimension));
            storedHierarchies.add(selectSpecificStoredHierarchies(hierarchy));
        }
        return storedHierarchies;
    }

    private List<StoredHierarchy> selectSpecificStoredHierarchies(Hierarchy hierarchy) {
        final String dimension = hierarchy.getDimension();

        List<Object> params = new ArrayList<>();
        addParams(params, dimension, hierarchy, false);

        StringJoiner whereClauseBuilder = new StringJoiner(" AND ");

        for (Level level : hierarchy.getMembers().stream().map(Member::getLevel).collect(Collectors.toList())) {
            whereClauseBuilder.add(level.getId() + " = ?");
        }

        String stmt = selectSpecialHierarchiesStmtsBase.get(dimension);
        if (whereClauseBuilder.length() > 0) {
            stmt += " WHERE " + whereClauseBuilder;
        }

        PreparedStatement prepStmt = selectSpecialHierarchiesStmts.computeIfAbsent(stmt, s -> {
            log.info("Preparing statement to select special hierarchies from dimension table {}: {}", dimension, s);
            return Objects.requireNonNull(cassie.getSessionFactory()).getSession().prepare(s);
        });

        try {
            return queryStoredHierarchies(prepStmt, params, dimension).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Could not query hierarchy from dimension table " + dimension, e);
            throw new RuntimeException(e);
        }
    }

    private Hierarchy toHierarchy(String dimension, Row row) {
        List<Level> levels = CubeSchema.getInstance().byDimension(dimension);
        List<Member> members = new ArrayList<>();

        for (Level level : levels) {
            Object value = getObject(row, level);
            if (value != null) {
                members.add(Member.of(level, value));
            }
        }
        return members.isEmpty() ? Hierarchy.all(dimension) : new Hierarchy(members);
    }

    private List<List<StoredHierarchy>> selectGeneralStoredHierarchies(SliceDiceContext context) {
        SortedMap<String, Hierarchy> hierarchies = context.getHierarchies();
        List<List<StoredHierarchy>> storedHierarchies = new ArrayList<>();
        for (Map.Entry<String, Hierarchy> entry : hierarchies.entrySet()) {
            storedHierarchies.add(selectGeneralStoredHierarchies(entry.getValue()));
        }
        return storedHierarchies;
    }

    private List<StoredHierarchy> selectGeneralStoredHierarchies(Hierarchy hierarchy) {
        final String dimension = hierarchy.getDimension();

        List<StoredHierarchy> result = new ArrayList<>();
        List<ListenableFuture<List<StoredHierarchy>>> futures = new ArrayList<>();
        PreparedStatement stmt = selectGeneralHierarchiesStmts.get(dimension);

        // need to start on current hierarchy, because a general context B of a context A can have the same hierarchy for a given dimension if another is on a higher level
        Hierarchy parentHierarchy = hierarchy;

        do {
            Map<Level, Object> memberMap = parentHierarchy.getMembers().stream().collect(Collectors.toMap(Member::getLevel, Member::getValue));
            List<Level> dimLevels = CubeSchema.getInstance().byDimension(dimension);

            List<Object> params = new ArrayList<>(dimLevels.size());
            for (Level level : dimLevels) {
                params.add(toParam(level, memberMap.getOrDefault(level, null)));
            }
            futures.add(queryStoredHierarchies(stmt, params, dimension));
        } while ((parentHierarchy = parentHierarchy.rollUp()) != null);

        for (ListenableFuture<List<StoredHierarchy>> future : futures) {
            try {
                result.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                log.error("Could not get future", e);
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    public List<LakehouseFile> getLakehouseFiles(String contextId) {
        return getLakehouseFiles(contextId, 1);
    }

    private List<LakehouseFile> getLakehouseFiles(String contextId, int retryCount) {
        try {
            return cassie.query(prepStmts.get(SELECT_FILES).bind(contextId),
                    (row, rowNum) -> new LakehouseFile(row.getString("stored_name"), row.getString("engine_type")))
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Could not query lakehouse files for context with id {}; Message: {}", contextId, e.getMessage());
            if (retryCount < 3) {
                int waitMs = new Random().nextInt(100);
                log.error("Retrying in {}ms... (retry count was {})", waitMs, retryCount);
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                return getLakehouseFiles(contextId, retryCount + 1);
            }

            throw new RuntimeException(e);
        }
    }

    private ListenableFuture<List<StoredHierarchy>> queryStoredHierarchies(PreparedStatement stmt, List<Object> params, String dimension) {
        return cassie.query(stmt.bind(params.toArray()), (row, rowNum) ->
                new StoredHierarchy(toHierarchy(dimension, row), row.getSet("context_hashes", String.class)));
    }
}
