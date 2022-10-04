package at.jku.dke.bigkgolap.api.engines;

import at.jku.dke.bigkgolap.api.model.Hierarchy;
import lombok.Data;

import java.util.Set;

@Data
public class AnalyzerResult {
    private final Set<Hierarchy> hierarchies;
}
