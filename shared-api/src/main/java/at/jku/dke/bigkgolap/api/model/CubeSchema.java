package at.jku.dke.bigkgolap.api.model;

import at.jku.dke.bigkgolap.api.schema.CubeSchemaReader;
import at.jku.dke.bigkgolap.api.schema.RawCubeSchema;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class CubeSchema {
    private static CubeSchema INSTANCE = null;
    private final List<Level> levels; // ordered by depth. Important for database queries

    private CubeSchema() throws ClassNotFoundException {
        List<Level> levels = new ArrayList<>();
        RawCubeSchema raw = CubeSchemaReader.getSchema();

        for (Map.Entry<String, Map<String, Map<String, String>>> dim : raw.getDimensions().entrySet()) {
            String dimension = dim.getKey();

            Set<String> levelsTemp = new HashSet<>();
            Set<String> rollUpLevelsTemp = new HashSet<>();

            for (Map.Entry<String, Map<String, String>> lev : dim.getValue().entrySet()) {
                String level = lev.getKey();

                String rollUpLevel = null;
                Class<?> javaDataType = null;
                int depth = -1;

                for (Map.Entry<String, String> property : lev.getValue().entrySet()) {

                    switch (property.getKey()) {
                        case "rollUpLevel":
                            rollUpLevel = property.getValue();
                            break;
                        case "javaDataType":
                            javaDataType = Class.forName(property.getValue());
                            break;
                        case "depth":
                            depth = Integer.parseInt(property.getValue());
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown property for level " + level + " detected!");
                    }
                }

                if (rollUpLevel == null || javaDataType == null || depth == -1) {
                    throw new IllegalArgumentException("Cube schema misses properties for level " + level);
                }

                levelsTemp.add(level);

                if (!"ALL".equals(rollUpLevel)) {
                    rollUpLevelsTemp.add(rollUpLevel);
                } else {
                    rollUpLevel = null;
                }


                levels.add(new Level(dimension, level, javaDataType, rollUpLevel, depth));
            }

            if (!levelsTemp.containsAll(rollUpLevelsTemp)) {
                throw new IllegalArgumentException("Some level of dimension " + dimension + " rolls up to an unknown level!");
            }
        }

        levels.sort(this.getComparator());

        this.levels = Collections.unmodifiableList(levels);
    }

    public synchronized static CubeSchema getInstance() {
        if (INSTANCE == null) {
            try {
                INSTANCE = new CubeSchema();
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Cube schema is invalid!");
            }
        }
        return INSTANCE;
    }

    /**
     * @param other level
     * @return true if the current levels rolls up to "other" recursively. Will always roll up to "null", which is equal to the "ALL" level.
     */
    public boolean rollsUpTo(Level level, Level other) {
        String dimension = level.getDimension();
        Level rollUpLevel = locate(dimension, level.getRollsUpTo());
        return level.getRollsUpTo().equals(other.getId()) || (rollUpLevel != null && rollsUpTo(rollUpLevel, other));
    }

    /**
     * @param dimension The dimension ignoring the case (e.g. year or FIR)
     * @param id        Textual identifier withn a dimension ignoring the case (e.g. year or FIR)
     * @return The Level associated with the given id
     */
    public Level locate(String dimension, String id) {
        for (Level level : this.levels) {
            if (level.getDimension().equalsIgnoreCase(dimension) && level.getId().equalsIgnoreCase(id)) {
                return level;
            }
        }
        return null;
    }

    public List<String> idsByDimension(String dimension) {
        return this.levels.stream().filter(level -> level.getDimension().equals(dimension)).map(Level::getId).collect(Collectors.toList());
    }

    public List<Level> byDimension(String dimension) {
        return this.levels.stream().filter(level -> level.getDimension().equals(dimension)).collect(Collectors.toList());
    }

    public Comparator<Level> getComparator() {
        return (level1, level2) -> {
            int dimDiff = level1.getDimension().compareTo(level2.getDimension());
            if (dimDiff == 0) {
                int depthDiff = level1.getDepth() - level2.getDepth();
                return depthDiff != 0 ? depthDiff : level1.getId().compareTo(level2.getId());
            }
            return dimDiff;
        };
    }

    public List<String> getDimensions() {
        List<String> sortedDimensions = new ArrayList<>();
        for (Level level : this.levels) {
            if (!sortedDimensions.contains(level.getDimension())) {
                sortedDimensions.add(level.getDimension());
            }
        }
        return sortedDimensions;
    }
}
