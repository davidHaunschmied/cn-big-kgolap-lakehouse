package at.jku.dke.bigkgolap.api.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.ext.com.google.common.annotations.VisibleForTesting;

import java.io.IOException;

@Slf4j
public class CubeSchemaReader {

    private static final String DEFAULT_CUBE_SCHEMA_FILE = "cube-schema.yaml";

    private static CubeSchemaReader INSTANCE;

    public synchronized static CubeSchemaReader getInstance() {
        if (INSTANCE == null) {
            try {
                INSTANCE = fromDefault();
            } catch (IOException e) {
                log.error("Could not read cube schema from file {}.", DEFAULT_CUBE_SCHEMA_FILE, e);
            }
        }
        return INSTANCE;
    }

    @JsonProperty
    @VisibleForTesting
    RawCubeSchema schema;

    public static RawCubeSchema getSchema() {
        return getInstance().schema;
    }

    public static CubeSchemaReader fromDefault() throws IOException {
        return from(DEFAULT_CUBE_SCHEMA_FILE);
    }

    public static CubeSchemaReader from(String filename) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(Thread.currentThread().getContextClassLoader().getResource(filename), CubeSchemaReader.class);
    }
}
