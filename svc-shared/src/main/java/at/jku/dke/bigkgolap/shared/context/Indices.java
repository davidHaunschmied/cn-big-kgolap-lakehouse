package at.jku.dke.bigkgolap.shared.context;

import at.jku.dke.bigkgolap.api.model.Level;
import lombok.Data;

import java.util.Map;
import java.util.Set;

/**
 * The difference to contexts is that an index contains multiple members per dimension
 */
@Data
public class Indices {
    private final Set<Map<Level, Object>> indexMaps;
}
