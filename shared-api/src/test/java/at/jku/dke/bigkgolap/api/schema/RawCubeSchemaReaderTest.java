package at.jku.dke.bigkgolap.api.schema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RawCubeSchemaReaderTest {

    private static final String CUBE_SCHEMA_FILE = "cube-schema-test.yaml";

    @Test
    void testDimensions() throws IOException {
        RawCubeSchema schema = CubeSchemaReader.from(CUBE_SCHEMA_FILE).schema;
        assertThat(schema).isNotNull();

        Map<String, Map<String, Map<String, String>>> dimensions = schema.getDimensions();
        assertThat(dimensions).containsOnlyKeys("location", "topic", "time");
        assertThat(dimensions.get("location")).containsOnlyKeys("territory", "fir", "location");
        assertThat(dimensions.get("topic")).containsOnlyKeys("category", "family", "feature");
        assertThat(dimensions.get("time")).containsOnlyKeys("year", "month", "day");
    }

    @Test
    void testHierarchies() throws IOException {
        RawCubeSchema schema = CubeSchemaReader.from(CUBE_SCHEMA_FILE).schema;
        assertThat(schema).isNotNull();

        Map<String, List<Map<String, String>>> hierarchies = schema.getHierarchies();
        assertThat(hierarchies).containsOnlyKeys("location", "topic");
        assertThat(hierarchies.get("location")).hasSize(4);
        assertThat(hierarchies.get("topic")).hasSize(5);

        assertThat(hierarchies.get("location").get(0)).containsExactly(Map.entry("territory", "Austria"), Map.entry("fir", "LOVV"), Map.entry("location", "LOWW"));
    }
}
