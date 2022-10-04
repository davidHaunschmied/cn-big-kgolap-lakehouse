package at.jku.dke.bigkgolap.shared.cache;

import at.jku.dke.bigkgolap.api.model.Context;
import at.jku.dke.bigkgolap.shared.config.LakehouseConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.graph.GraphFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Collection;

@Slf4j
public class RedisCache implements GraphCache {

    private final LakehouseConfig config;
    private final JedisCluster jedis;

    public RedisCache(LakehouseConfig config) {
        this.config = config;
        this.jedis = new JedisCluster(new HostAndPort(config.getRedisHost(), config.getRedisPort()));
    }

    @Override
    public Graph loadGraph(String contextId) {
        String plainGraph;

        try {
            plainGraph = jedis.get(contextId);
        } catch (JedisConnectionException e) {
            log.error("Could not connect to Redis instance!", e);
            return null;
        }

        if (plainGraph == null || plainGraph.isBlank() || plainGraph.equals("nil")) {
            // Graph not cached
            return null;
        }
        Graph graph = GraphFactory.createDefaultGraph();
        RDFDataMgr.read(graph, IOUtils.toInputStream(plainGraph, Charset.defaultCharset()), Lang.RDFTHRIFT);
        return graph;
    }

    @Override
    public void upsertGraph(String contextId, Graph graph) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            RDFDataMgr.write(baos, graph, Lang.RDFTHRIFT);
            try {
                jedis.setex(contextId.getBytes(), config.getGraphCacheTimeoutMinutes() * 60L, baos.toByteArray());
            } catch (JedisConnectionException e) {
                log.error("Could not connect to Redis instance!", e);
            }
        } catch (Exception e) {
            log.error("Could not upsert graph for context {} into Redis cache!", contextId);
        }
    }

    @Override
    public void deleteCachedGraphs(Collection<Context> contexts) {
        jedis.del(contexts.stream().map(Context::getId).toArray(String[]::new));
    }

    @Override
    public String clear() {
        String messages = "";
        for (ConnectionPool pool : jedis.getClusterNodes().values()) {
            try (Connection jedis = pool.getResource()) {
                jedis.executeCommand(Protocol.Command.FLUSHDB);
            } catch (Exception ex) {
                messages += ex.getMessage() + "\n";
            }
        }
        if (messages.length() > 0) {
            return messages;
        }
        return "SUCCESS";
    }
}
