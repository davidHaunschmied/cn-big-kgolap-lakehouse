package at.jku.dke.bigkgolap.api.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RawCubeSchema {

    @JsonProperty
    private Map<String, Map<String, Map<String, String>>> dimensions;

    @JsonProperty
    private Map<String, List<Map<String, String>>> hierarchies;

}
