package at.jku.dke.bigkgolap.api.schema;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DimensionHierarchy {
    private List<Map<String, String>> hierarchies;
}
