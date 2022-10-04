package at.jku.dke.bigkgolap.api.model;

import lombok.Data;

import java.util.Map;
import java.util.Objects;

@Data
public class MergeLevels {
    private final Map<String, Level> mergeLevels;

    public MergeLevels(Map<String, Level> mergeLevels) {
        this.mergeLevels = Objects.requireNonNull(mergeLevels, "MergeLevels must not be null!");
    }
}
