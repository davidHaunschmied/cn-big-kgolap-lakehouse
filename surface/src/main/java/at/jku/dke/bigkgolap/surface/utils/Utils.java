package at.jku.dke.bigkgolap.surface.utils;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;

public class Utils {
    private Utils() {
        // hide
    }

    public static Object parseMemberValue(String value, Class<?> clazz) {
        switch (clazz.getSimpleName()) {
            case "Integer":
                return Integer.parseInt(value);
            case "LocalDate":
                return LocalDate.parse(value);
            case "Year":
                return Year.parse(value);
            case "YearMonth":
                return YearMonth.parse(value);
            case "String":
                return value;
            default:
                throw new IllegalArgumentException("Unsupported data type: " + clazz.getSimpleName());
        }
    }
}
