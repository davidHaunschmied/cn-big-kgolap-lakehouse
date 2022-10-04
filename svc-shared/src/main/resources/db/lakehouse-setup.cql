-- Create a keyspace
CREATE KEYSPACE IF NOT EXISTS lakehouse WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : '1' };

CREATE TABLE IF NOT EXISTS lakehouse.time
(
    year           INT,
    month          VARCHAR,        -- uuuu-MM
    day            BIGINT,         -- EPOCH DAY

    hash           VARCHAR,        -- hashcode of hierarchy -> must be equal for equal hierarchies to avoid race conditions (two concurrent upserts will use the same hash)
    context_hashes SET<VARCHAR>,   -- set of context hashes this hierarchy is associated to

    PRIMARY KEY (year, month, day) -- year is PARTITION KEY, month and day are CLUSTERING KEYS
) WITH CLUSTERING ORDER BY (month DESC, day DESC); -- favor more recent times over past ones

CREATE TABLE IF NOT EXISTS lakehouse.location
(
    territory      VARCHAR,
    fir            VARCHAR,
    location       VARCHAR,

    hash           VARCHAR,                -- hashcode of hierarchy -> must be equal for equal hierarchies to avoid race conditions (two concurrent upserts will use the same hash)
    context_hashes SET<VARCHAR>,           -- set of context hashes this hierarchy is associated to

    PRIMARY KEY (territory, fir, location) -- territory is PARTITION KEY, fir and location are CLUSTERING KEYS
) WITH CLUSTERING ORDER BY (fir ASC, location ASC); -- makes no difference for performance if ASC or DESC since values are not known

CREATE TABLE IF NOT EXISTS lakehouse.topic
(
    category       VARCHAR,
    family         VARCHAR,
    feature        VARCHAR,

    hash           VARCHAR,                 -- hashcode of hierarchy -> must be equal for equal hierarchies to avoid race conditions (two concurrent upserts will use the same hash)
    context_hashes SET<VARCHAR>,            -- set of context hashes this hierarchy is associated to

    PRIMARY KEY (category, family, feature) -- category is PARTITION KEY, family and feature are CLUSTERING KEYS
) WITH CLUSTERING ORDER BY (family ASC, feature ASC); -- makes no difference for performance if ASC or DESC since values are not known

CREATE TABLE IF NOT EXISTS lakehouse.contexts
(
    hash          VARCHAR, -- hashcode of context -> must be equal for equal contexts to avoid race conditions (two concurrent upserts will use the same hash)

    time_hash     VARCHAR,
    location_hash VARCHAR,
    topic_hash    VARCHAR,
    PRIMARY KEY (hash)     -- dimension hashes form the PARTITION KEY
);
CREATE TABLE IF NOT EXISTS lakehouse.files
(
    context_hash VARCHAR,
    stored_name  VARCHAR,

    engine_type  VARCHAR,                     -- required here for query by context hash

    PRIMARY KEY ((context_hash), stored_name) -- dimension hashes form the PARTITION KEY, stored_name is CLUSTERING KEY
) WITH CLUSTERING ORDER BY (stored_name DESC); -- stored_name starts with the timestamp of the insertion (e.g 20181210-153012-199_file.xml). Queries containing the stored_name likely target newer files

CREATE TABLE IF NOT EXISTS lakehouse.file_details
(
    stored_name   VARCHAR,

    engine_type   VARCHAR,
    original_name VARCHAR,
    size_bytes    bigint,
    PRIMARY KEY (stored_name) -- dimension hashes form the PARTITION KEY, stored_name is CLUSTERING KEY
);

CREATE TABLE IF NOT EXISTS lakehouse.query_log
(
    id                              UUID,
    query                           TEXT,
    start                           TIMESTAMP,
    ts_1_query_relevant             TIMESTAMP,          -- ALTER TABLE lakehouse.query_log ADD ts_1_query_relevant TIMESTAMP;
    ts_2_query_general              TIMESTAMP,          -- ALTER TABLE lakehouse.query_log ADD ts_2_query_general TIMESTAMP;
    ts_3_prepare_query              TIMESTAMP,          -- ALTER TABLE lakehouse.query_log ADD ts_3_prepare_query TIMESTAMP;
    end                             TIMESTAMP,
    success                         BOOLEAN,
    exception_message               TEXT,
    ctx_rq_in_mem_cache_hits        MAP<TEXT, BOOLEAN>, -- request map from context id to in memory graph cache hit
    ctx_rq_local_file_cache_hit_pct MAP<TEXT, FLOAT>,   -- request map from context id to local file cache hit percentage
    considered_contexts             INT,
    contexts                        INT,
    quads                           BIGINT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS lakehouse.file_ingestion_log
(
    stored_name VARCHAR,
    start       TIMESTAMP,
    end         TIMESTAMP,
    PRIMARY KEY (stored_name)
);
