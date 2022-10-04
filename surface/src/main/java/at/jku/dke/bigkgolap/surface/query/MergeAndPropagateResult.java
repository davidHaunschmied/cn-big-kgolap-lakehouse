package at.jku.dke.bigkgolap.surface.query;

import at.jku.dke.bigkgolap.api.model.Context;
import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
public class MergeAndPropagateResult {
    private final Map<String, Set<Context>> contextMap;
    private final Set<Context> finalContexts;
}
