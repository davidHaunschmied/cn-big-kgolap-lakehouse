package at.jku.dke.bigkgolap.api.model;

import at.jku.dke.bigkgolap.api.model.exceptions.HierarchyNotAvailableException;
import at.jku.dke.bigkgolap.api.model.exceptions.InvalidHierarchyException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HierarchyTest {

    private static final Level TIME_YEAR = CubeSchema.getInstance().locate("time", "year");
    private static final Level TIME_MONTH = CubeSchema.getInstance().locate("time", "month");
    private static final Level TIME_DAY = CubeSchema.getInstance().locate("time", "day");

    private static final Level LOCATION_TERRITORY = CubeSchema.getInstance().locate("location", "territory");
    private static final Level LOCATION_FIR = CubeSchema.getInstance().locate("location", "fir");
    private static final Level LOCATION_LOCATION = CubeSchema.getInstance().locate("location", "location");

    private static final Level TOPIC_CATEGORY = CubeSchema.getInstance().locate("topic", "category");
    private static final Level TOPIC_FAMILY = CubeSchema.getInstance().locate("topic", "family");
    private static final Level TOPIC_FEATURE = CubeSchema.getInstance().locate("topic", "feature");

    @Test
    void testInvalidConstructorUsage() {
        assertThatThrownBy(() -> Hierarchy.of()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testValidTimeHierarchies() {
        assertThat(Hierarchy.of(Member.of(TIME_YEAR, Year.of(2012)),
                Member.of(TIME_MONTH, YearMonth.of(2012, 12)),
                Member.of(TIME_DAY, LocalDate.of(2012, 12, 12))))
                .isNotNull();

        assertThat(Hierarchy.of(Member.of(TIME_YEAR, Year.of(2012)),
                Member.of(TIME_MONTH, YearMonth.of(2012, 12))))
                .isNotNull();

        assertThat(Hierarchy.of(Member.of(TIME_YEAR, Year.of(2012))))
                .isNotNull();

        assertThat(Hierarchy.all("time")).isNotNull();
    }

    @Test
    void testInvalidTimeHierarchies() {
        assertThatThrownBy(() ->
                Hierarchy.of(Member.of(TIME_YEAR, Year.of(2013)),
                        Member.of(TIME_MONTH, YearMonth.of(2012, 12)),
                        Member.of(TIME_DAY, LocalDate.of(2012, 12, 12))))
                .isInstanceOf(InvalidHierarchyException.class);

        assertThatThrownBy(() ->
                Hierarchy.of(Member.of(TIME_YEAR, Year.of(2012)),
                        Member.of(TIME_MONTH, YearMonth.of(2013, 12)),
                        Member.of(TIME_DAY, LocalDate.of(2012, 12, 12))))
                .isInstanceOf(InvalidHierarchyException.class);

        assertThatThrownBy(() ->
                Hierarchy.of(Member.of(TIME_YEAR, Year.of(2012)),
                        Member.of(TIME_MONTH, YearMonth.of(2012, 12)),
                        Member.of(TIME_DAY, LocalDate.of(2013, 12, 12))))
                .isInstanceOf(InvalidHierarchyException.class);


        assertThatThrownBy(() ->
                Hierarchy.of(Member.of(TIME_YEAR, Year.of(2012)),
                        Member.of(TIME_DAY, LocalDate.of(2012, 12, 12))))
                .isInstanceOf(InvalidHierarchyException.class);

        assertThatThrownBy(() ->
                Hierarchy.of(Member.of(TIME_MONTH, YearMonth.of(2012, 12)),
                        Member.of(TIME_DAY, LocalDate.of(2012, 12, 12))))
                .isInstanceOf(InvalidHierarchyException.class);
    }

    @Test
    void testValidLocationHierarchies() {
        assertThat(Hierarchy.of(Member.of(LOCATION_TERRITORY, "Austria"),
                Member.of(LOCATION_FIR, "LOVV"),
                Member.of(LOCATION_LOCATION, "LOWW")))
                .isNotNull();

        assertThat(Hierarchy.of(Member.of(LOCATION_TERRITORY, "Austria"),
                Member.of(LOCATION_FIR, "LOVV")))
                .isNotNull();

        assertThat(Hierarchy.of(Member.of(LOCATION_TERRITORY, "Austria")))
                .isNotNull();

        assertThat(Hierarchy.all("location")).isNotNull();
    }

    @Test
    void testInvalidLocationHierarchies() {
        assertThatThrownBy(() ->
                Hierarchy.of(Member.of(LOCATION_TERRITORY, "Invalid"),
                        Member.of(LOCATION_FIR, "LOVV"),
                        Member.of(LOCATION_LOCATION, "LOWW")))
                .isInstanceOf(InvalidHierarchyException.class);

        assertThatThrownBy(() ->
                Hierarchy.of(Member.of(LOCATION_TERRITORY, "Austria"),
                        Member.of(LOCATION_FIR, "Invalid"),
                        Member.of(LOCATION_LOCATION, "LOWW")))
                .isInstanceOf(HierarchyNotAvailableException.class);

        assertThatThrownBy(() ->
                Hierarchy.of(Member.of(LOCATION_TERRITORY, "Austria"),
                        Member.of(LOCATION_FIR, "LOVV"),
                        Member.of(LOCATION_LOCATION, "Invalid")))
                .isInstanceOf(HierarchyNotAvailableException.class);

        assertThatThrownBy(() ->
                Hierarchy.of(Member.of(LOCATION_TERRITORY, "Austria"),
                        Member.of(LOCATION_LOCATION, "LOWW")))
                .isInstanceOf(InvalidHierarchyException.class);

        assertThatThrownBy(() ->
                Hierarchy.of(Member.of(LOCATION_FIR, "LOVV"),
                        Member.of(LOCATION_LOCATION, "LOWW")))
                .isInstanceOf(InvalidHierarchyException.class);
    }

    @Test
    void testValidTopicHierarchies() {
        assertThat(Hierarchy.of(Member.of(TOPIC_CATEGORY, "Routes"),
                Member.of(TOPIC_FAMILY, "EnRoute"),
                Member.of(TOPIC_FEATURE, "RouteSegment")))
                .isNotNull();

        assertThat(Hierarchy.of(Member.of(TOPIC_CATEGORY, "Routes"),
                Member.of(TOPIC_FAMILY, "EnRoute")))
                .isNotNull();

        assertThat(Hierarchy.of(Member.of(TOPIC_CATEGORY, "Routes"))).isNotNull();

        assertThat(Hierarchy.all("topic")).isNotNull();
    }

    @Test
    void testInvalidTopicHierarchies() {
        assertThatThrownBy(() ->
                Hierarchy.of(Member.of(TOPIC_CATEGORY, "Invalid"),
                        Member.of(TOPIC_FAMILY, "EnRoute"),
                        Member.of(TOPIC_FEATURE, "RouteSegment")))
                .isInstanceOf(InvalidHierarchyException.class);

        assertThatThrownBy(() ->
                Hierarchy.of(Member.of(TOPIC_CATEGORY, "Routes"),
                        Member.of(TOPIC_FAMILY, "Invalid"),
                        Member.of(TOPIC_FEATURE, "RouteSegment")))
                .isInstanceOf(HierarchyNotAvailableException.class);

        assertThatThrownBy(() ->
                Hierarchy.of(Member.of(TOPIC_CATEGORY, "Routes"),
                        Member.of(TOPIC_FAMILY, "EnRoute"),
                        Member.of(TOPIC_FEATURE, "Invalid")))
                .isInstanceOf(HierarchyNotAvailableException.class);

        assertThatThrownBy(() ->
                Hierarchy.of(Member.of(TOPIC_CATEGORY, "Routes"),
                        Member.of(TOPIC_FEATURE, "RouteSegment")))
                .isInstanceOf(InvalidHierarchyException.class);

        assertThatThrownBy(() ->
                Hierarchy.of(Member.of(TOPIC_FAMILY, "EnRoute"),
                        Member.of(TOPIC_FEATURE, "RouteSegment")))
                .isInstanceOf(InvalidHierarchyException.class);
    }
}
