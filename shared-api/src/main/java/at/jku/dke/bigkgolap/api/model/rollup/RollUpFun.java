package at.jku.dke.bigkgolap.api.model.rollup;

import at.jku.dke.bigkgolap.api.model.CubeSchema;
import at.jku.dke.bigkgolap.api.model.Level;
import at.jku.dke.bigkgolap.api.model.Member;
import at.jku.dke.bigkgolap.api.model.exceptions.HierarchyNotAvailableException;
import at.jku.dke.bigkgolap.api.schema.CubeSchemaReader;
import at.jku.dke.bigkgolap.api.schema.ValueMapper;
import org.apache.jena.ext.com.google.common.base.Strings;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public class RollUpFun {

    public static Member rollUp(Member member) {
        if (member.getLevel().getRollsUpTo() == null) {
            // rolling up to all level
            return null;
        }

        if (member.getLevel().getDimension().equals("time")) {
            if (member.getLevel().getId().equals("day")) {
                return dayToMonthFun(member);
            } else if (member.getLevel().getId().equals("month")) {
                return monthToYearFun(member);
            } else {
                throw new HierarchyNotAvailableException("Unknown level for dimension time given: " + member.getLevel().getId());
            }
        }

        final Level currentLevel = member.getLevel();
        final Level rollUpLevel = rollUp(currentLevel);

        final String currentStringValue = ValueMapper.toString(member);

        String dimension = member.getLevel().getDimension();
        List<Map<String, String>> dimHierarchies = CubeSchemaReader.getSchema().getHierarchies().get(dimension);

        if (dimHierarchies == null || dimHierarchies.isEmpty()) {
            throw new RuntimeException("No hierarchies for dimension " + dimension + " available!");
        }

        for (Map<String, String> hierarchy : dimHierarchies) {

            String value = hierarchy.getOrDefault(currentLevel.getId(), null);

            if (value != null && value.equals(currentStringValue)) {
                String rollUpValue = hierarchy.getOrDefault(rollUpLevel.getId(), null);

                if (Strings.isNullOrEmpty(rollUpValue)) {
                    throw new HierarchyNotAvailableException("Found hierarchy for member " + member + " but the value for " + rollUpLevel + " was null or empty!");
                }

                return new Member(rollUpLevel, ValueMapper.fromString(rollUpValue, rollUpLevel));
            }
        }

        throw new HierarchyNotAvailableException("Could not roll up member " + member + " to level " + rollUpLevel + " as no matching hierarchy was found!");
    }

    public static Member dayToMonthFun(Member member) {
        LocalDate value = (LocalDate) member.getValue();
        return new Member(rollUp(member.getLevel()), YearMonth.of(value.getYear(), value.getMonth()));
    }

    public static Member monthToYearFun(Member member) {
        return new Member(rollUp(member.getLevel()), Year.of(((YearMonth) member.getValue()).getYear()));
    }

    private static Level rollUp(Level level) {
        return CubeSchema.getInstance().locate(level.getDimension(), level.getRollsUpTo());
    }
}
