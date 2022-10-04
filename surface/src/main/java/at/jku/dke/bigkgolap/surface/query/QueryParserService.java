package at.jku.dke.bigkgolap.surface.query;

import at.jku.dke.bigkgolap.api.model.*;
import at.jku.dke.bigkgolap.surface.exceptions.InvalidQueryException;
import at.jku.dke.bigkgolap.surface.utils.Utils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QueryParserService {

    private static final String SELECT = "SELECT ";
    private static final String AND_SPLIT = " AND ";
    private static final String ROLLUP_ON = " ROLLUP ON ";
    private static final String COMMA = ",";

    public SliceDiceContext parseSelectClauseOrThrow(String kgOlapQuery) {
        if (kgOlapQuery == null || kgOlapQuery.isBlank()) {
            return new SliceDiceContext();
        }

        final String contextQuery;
        String strippedQuery = kgOlapQuery.trim().replaceAll("\n|\r\n", " ")
                .replaceAll("(?i)" + SELECT, SELECT)
                .replaceAll("(?i)" + AND_SPLIT, AND_SPLIT)
                .replaceAll("(?i)" + ROLLUP_ON, ROLLUP_ON);

        if (!strippedQuery.startsWith(SELECT)) {
            throw new InvalidQueryException("Query must start with " + SELECT);
        }

        String selectLessQuery = strippedQuery.substring(SELECT.length());

        if (selectLessQuery.contains(ROLLUP_ON)) {
            contextQuery = selectLessQuery.substring(0, selectLessQuery.indexOf(ROLLUP_ON));
        } else {
            contextQuery = selectLessQuery;
        }

        String[] memberParts = contextQuery.split(AND_SPLIT);

        if (memberParts.length < 1) {
            throw new InvalidQueryException("Query is invalid: " + contextQuery);
        }

        if (memberParts.length == 1 && memberParts[0].trim().equals("*")) {
            return new SliceDiceContext();
        }

        SortedMap<String, Hierarchy> hierarchies = new TreeMap<>();

        for (String memberPart : memberParts) {

            String[] levelAndValue = memberPart.split("=");

            if (levelAndValue.length != 2) {
                throw new InvalidQueryException("Query contains invalid expression: " + memberPart);
            }

            String dimensionLevelStr = levelAndValue[0].strip();
            String[] dimensionLevel = dimensionLevelStr.split("_");

            if (dimensionLevel.length != 2) {
                throw new InvalidQueryException("Query contains invalid dimension_level argument: " + dimensionLevelStr);
            }

            String valueStr = levelAndValue[1].strip();

            Level nextLevel = CubeSchema.getInstance().locate(dimensionLevel[0], dimensionLevel[1]);

            if (nextLevel == null) {
                throw new InvalidQueryException("Query contains invalid level: " + dimensionLevelStr);
            }

            if (hierarchies.containsKey(nextLevel.getDimension())) {
                for (Member member : hierarchies.get(nextLevel.getDimension()).getMembers()) {
                    if (member.getLevel() == nextLevel) {
                        throw new InvalidQueryException("Query contains duplicate levels: " + dimensionLevelStr);
                    }
                }
                throw new InvalidQueryException("Query contains multiple members for dimension: " + nextLevel.getDimension());
            }

            try {
                Object value = Utils.parseMemberValue(valueStr, nextLevel.getType());
                hierarchies.put(nextLevel.getDimension(), HierarchyFactory.get(new Member(nextLevel, value)));
            } catch (Exception e) {
                throw new InvalidQueryException("Query contains invalid value: " + valueStr, e);
            }
        }

        return new SliceDiceContext(hierarchies);
    }

    /**
     * @param kgOlapQuery
     * @return Mapping from dimension to roll up on level. Level can be null which means roll up to the all level
     */
    public MergeLevels parseRollupOnOrThrow(String kgOlapQuery) {
        if (kgOlapQuery == null || !kgOlapQuery.contains(ROLLUP_ON)) {
            return null;
        }

        String groupByQuery = kgOlapQuery.substring(kgOlapQuery.indexOf(ROLLUP_ON) + ROLLUP_ON.length());

        String[] dimensionLevelParts = groupByQuery.split(COMMA);

        Map<String, Level> groupByMap = new HashMap<>();
        List<String> dimensions = CubeSchema.getInstance().getDimensions();

        for (String dimensionLevelPart : dimensionLevelParts) {
            String dimensionLevelStr = dimensionLevelPart.strip();
            String[] dimensionLevel = dimensionLevelStr.split("_");

            if (dimensionLevel.length != 2) {
                throw new InvalidQueryException("Query contains invalid dimension_level argument: " + dimensionLevelStr);
            }

            Level nextLevel;

            if ("ALL".equalsIgnoreCase(dimensionLevel[1])) {
                if (!dimensions.contains(dimensionLevel[0])) {
                    throw new InvalidQueryException("Invalid dimension found in the " + ROLLUP_ON + " clause: " + dimensionLevelStr);
                }
                nextLevel = null;
            } else {
                nextLevel = CubeSchema.getInstance().locate(dimensionLevel[0], dimensionLevel[1]);

                if (nextLevel == null) {
                    throw new InvalidQueryException("Query contains invalid dimension_level in the " + ROLLUP_ON + " clause: " + dimensionLevelStr);
                }

                if (groupByMap.containsKey(nextLevel.getDimension())) {
                    throw new InvalidQueryException("Query contains multiple levels in " + ROLLUP_ON + " clause for dimension: " + nextLevel.getDimension());
                }
            }

            groupByMap.put(dimensionLevel[0], nextLevel);
        }

        return new MergeLevels(groupByMap);
    }
}
