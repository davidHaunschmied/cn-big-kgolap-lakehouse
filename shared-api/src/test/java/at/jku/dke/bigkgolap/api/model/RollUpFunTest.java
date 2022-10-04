package at.jku.dke.bigkgolap.api.model;

import at.jku.dke.bigkgolap.api.model.rollup.RollUpFun;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class RollUpFunTest {

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
    void testRollUpLocation() {
        Member location = Member.of(LOCATION_LOCATION, "LFMB");

        Member fir = RollUpFun.rollUp(location);
        assertThat(fir).isNotNull();
        assertThat(fir.getLevel()).isEqualTo(LOCATION_FIR);
        assertThat(fir.getValue()).isEqualTo("LFMM");

        Member territory = RollUpFun.rollUp(fir);
        assertThat(territory).isNotNull();
        assertThat(territory.getLevel()).isEqualTo(LOCATION_TERRITORY);
        assertThat(territory.getValue()).isEqualTo("France");

        Member all = RollUpFun.rollUp(territory);
        assertThat(all).isNull();
    }


    @Test
    void testRollUpTopic() {
        Member feature = Member.of(TOPIC_FEATURE, "RouteSegment");

        Member family = RollUpFun.rollUp(feature);
        assertThat(family).isNotNull();
        assertThat(family.getLevel()).isEqualTo(TOPIC_FAMILY);
        assertThat(family.getValue()).isEqualTo("EnRoute");

        Member category = RollUpFun.rollUp(family);
        assertThat(category).isNotNull();
        assertThat(category.getLevel()).isEqualTo(TOPIC_CATEGORY);
        assertThat(category.getValue()).isEqualTo("Routes");

        Member all = RollUpFun.rollUp(category);
        assertThat(all).isNull();
    }

    @Test
    void testRollUpTime() {
        Member day = Member.of(TIME_DAY, LocalDate.of(2021, 12, 10));

        Member month = RollUpFun.rollUp(day);
        assertThat(month).isNotNull();
        assertThat(month.getLevel()).isEqualTo(TIME_MONTH);
        assertThat(month.getValue()).isEqualTo(YearMonth.of(2021, 12));

        Member year = RollUpFun.rollUp(month);
        assertThat(year).isNotNull();
        assertThat(year.getLevel()).isEqualTo(TIME_YEAR);
        assertThat(year.getValue()).isEqualTo(Year.of(2021));

        Member all = RollUpFun.rollUp(year);
        assertThat(all).isNull();
    }

}
