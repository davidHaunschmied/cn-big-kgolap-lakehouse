package at.jku.dke.bigkgolap.shared.context;

import at.jku.dke.bigkgolap.api.model.Level;
import at.jku.dke.bigkgolap.api.model.Member;
import com.datastax.oss.driver.api.core.cql.Row;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.Collection;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;

import static java.lang.String.join;

public class DbUtils {

    private static final String NULL_STR = "UNDEFINED";
    private static final int NULL_INT = 0; // can not be -1 as row.getInt() returns 0 for null values

    private DbUtils() {
        // hide
    }

    public static String toWhereClause(Collection<String> values) {
        return join(" = ? AND ", values) + " = ?";
    }

    public static String csv(Collection<String> values) {
        return join(", ", values);
    }

    public static String csQuestionMarks(int length) {
        StringJoiner csQms = new StringJoiner(", ");
        for (int i = 0; i < length; i++) {
            csQms.add("?");
        }
        return csQms.toString();
    }

    public static SortedSet<Member> loadHierarchy(Member member) {
        SortedSet<Member> hierarchy = new TreeSet<>();

        Member rolledUp = member;
        do {
            hierarchy.add(rolledUp);
        } while ((rolledUp = rolledUp.rollUp()) != null);

        return hierarchy;
    }

    public static Object toParam(Member member) {
        return toParam(member.getLevel(), member.getValue());
    }

    public static Object toParam(Level level, Object value) {
        switch (level.getType().getSimpleName()) {
            case "Integer":
                return value == null ? NULL_INT : value;
            case "String":
                return value == null ? NULL_STR : value;
            case "LocalDate":
                return value == null ? NULL_INT : ((LocalDate) value).toEpochDay();
            case "Year":
                return value == null ? NULL_INT : ((Year) value).getValue();
            case "YearMonth":
                return value == null ? NULL_STR : ((YearMonth) value).toString();
            default:
                throw new IllegalArgumentException("Unsupported level type detected. This is an internal problem!");
        }
    }

    public static Object getObject(Row row, Level level) {
        switch (level.getType().getSimpleName()) {
            case "Integer":
                int anInt = row.getInt(level.getId());
                return anInt > NULL_INT ? anInt : null;
            case "String":
                String str = row.getString(level.getId());
                return !(str == null || str.equals(NULL_STR)) ? str : null;
            case "LocalDate":
                long day = row.getLong(level.getId());
                return day > NULL_INT ? LocalDate.ofEpochDay(day) : null;
            case "Year":
                int year = row.getInt(level.getId());
                return year > NULL_INT ? Year.of(year) : null;
            case "YearMonth":
                String yearMonth = row.getString(level.getId());
                return !(yearMonth == null || yearMonth.equals(NULL_STR)) ? YearMonth.parse(yearMonth) : null;
            default:
                throw new IllegalArgumentException("Unsupported level type detected. This is an internal problem!");
        }
    }
}
