package at.jku.dke.bigkgolap.api.model;

import java.util.Locale;

/**
 * All levels except "ALL" levels. Must be aligned to the hierarchy schema for now. Order is important!
 */
public class Level {

    private final String dimension;
    private final String id;
    private final Class<?> type;
    private final String rollsUpTo;
    private final int depth;

    public Level(String dimension, String id, Class<?> type, String rollsUpTo, int depth) {
        this.dimension = dimension;
        this.id = id;
        this.type = type;
        this.depth = depth;
        this.rollsUpTo = rollsUpTo;
    }

    public String getDimension() {
        return dimension;
    }

    public String getId() {
        return id;
    }

    public Class<?> getType() {
        return type;
    }

    public String getRollsUpTo() {
        return rollsUpTo;
    }

    public int getDepth() {
        return depth;
    }

    @Override
    public String toString() {
        return dimension + "_" + id.toUpperCase(Locale.ROOT);
    }

}
