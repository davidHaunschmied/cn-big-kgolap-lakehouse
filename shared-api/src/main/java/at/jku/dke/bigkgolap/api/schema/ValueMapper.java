package at.jku.dke.bigkgolap.api.schema;

import at.jku.dke.bigkgolap.api.model.Level;
import at.jku.dke.bigkgolap.api.model.Member;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;

public class ValueMapper {
    private ValueMapper() {
        // utility
    }

    public static Object fromString(String value, Level level) {
        switch (level.getType().getSimpleName()) {
            case "Integer":
                return Integer.parseInt(value);
            case "String":
                return value;
            case "LocalDate":
                return LocalDate.parse(value);
            case "Year":
                return Year.parse(value);
            case "YearMonth":
                return YearMonth.parse(value);
            default:
                throw new IllegalArgumentException("Unsupported level type detected. This is an internal problem!");
        }
    }

    public static String toString(Member member) {
        return String.valueOf(member.getValue());
    }
}
