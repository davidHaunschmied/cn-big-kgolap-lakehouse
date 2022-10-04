package at.jku.dke.bigkgolap.surface.query;

import at.jku.dke.bigkgolap.api.model.Context;
import at.jku.dke.bigkgolap.api.model.CubeSchema;
import at.jku.dke.bigkgolap.api.model.HierarchyFactory;
import at.jku.dke.bigkgolap.api.model.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

import static at.jku.dke.bigkgolap.api.model.Member.of;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
class MergeAndPropagateServiceTest {

    @InjectMocks
    MergeAndPropagateService cos;

    private static final Level TIME_YEAR = CubeSchema.getInstance().locate("time", "year");
    private static final Level TIME_MONTH = CubeSchema.getInstance().locate("time", "month");
    private static final Level TIME_DAY = CubeSchema.getInstance().locate("time", "day");

    private static final Level LOCATION_TERRITORY = CubeSchema.getInstance().locate("location", "territory");
    private static final Level LOCATION_FIR = CubeSchema.getInstance().locate("location", "fir");
    private static final Level LOCATION_LOCATION = CubeSchema.getInstance().locate("location", "location");


    Context k1 = new Context();
    Context k2 = new Context(Map.of("time", HierarchyFactory.get(of(TIME_YEAR, Year.of(2020))), "location", HierarchyFactory.get(of(LOCATION_FIR, "LOVV"))));
    Context k3 = new Context(Map.of("time", HierarchyFactory.get(of(TIME_MONTH, YearMonth.of(2020, 2))), "location", HierarchyFactory.get(of(LOCATION_FIR, "LOVV"))));
    Context k4 = new Context(Map.of("time", HierarchyFactory.get(of(TIME_DAY, LocalDate.of(2020, 2, 10))), "location", HierarchyFactory.get(of(LOCATION_LOCATION, "LOWW"))));
    Context k5 = new Context(Map.of("time", HierarchyFactory.get(of(TIME_DAY, LocalDate.of(2020, 2, 10))), "location", HierarchyFactory.get(of(LOCATION_LOCATION, "LOWS"))));


    @Test
    void testRollsUpTo() {
        assertThat(cos.rollsUpTo(k1, k1)).isTrue();
        assertThat(cos.rollsUpTo(k2, k1)).isTrue();
        assertThat(cos.rollsUpTo(k3, k1)).isTrue();
        assertThat(cos.rollsUpTo(k4, k1)).isTrue();
        assertThat(cos.rollsUpTo(k5, k1)).isTrue();

        assertThat(cos.rollsUpTo(k2, k2)).isTrue();
        assertThat(cos.rollsUpTo(k3, k2)).isTrue();
        assertThat(cos.rollsUpTo(k4, k2)).isTrue();
        assertThat(cos.rollsUpTo(k5, k2)).isTrue();

        assertThat(cos.rollsUpTo(k3, k3)).isTrue();
        assertThat(cos.rollsUpTo(k4, k3)).isTrue();
        assertThat(cos.rollsUpTo(k5, k3)).isTrue();


        assertThat(cos.rollsUpTo(k1, k2)).isFalse();

        assertThat(cos.rollsUpTo(k1, k3)).isFalse();
        assertThat(cos.rollsUpTo(k2, k3)).isFalse();

        assertThat(cos.rollsUpTo(k1, k4)).isFalse();
        assertThat(cos.rollsUpTo(k2, k4)).isFalse();
        assertThat(cos.rollsUpTo(k3, k4)).isFalse();
        assertThat(cos.rollsUpTo(k5, k4)).isFalse();

        assertThat(cos.rollsUpTo(k1, k5)).isFalse();
        assertThat(cos.rollsUpTo(k2, k5)).isFalse();
        assertThat(cos.rollsUpTo(k3, k5)).isFalse();
        assertThat(cos.rollsUpTo(k4, k5)).isFalse();
    }

    @Test
    void testRollUpContextTo() {
        Map<String, Level> singleMergeLevel = Map.of(TIME_MONTH.getDimension(), TIME_MONTH);
        Map<String, Level> twoMergeLevels = Map.of(TIME_YEAR.getDimension(), TIME_YEAR, LOCATION_FIR.getDimension(), LOCATION_FIR);
        Map<String, Level> allLevels = new HashMap<>();
        allLevels.put(TIME_YEAR.getDimension(), null);
        allLevels.put(LOCATION_FIR.getDimension(), null);

        assertThat(cos.rollUpContextTo(k1, singleMergeLevel)).isEqualTo(k1);
        assertThat(cos.rollUpContextTo(k2, singleMergeLevel)).isEqualTo(k2);
        assertThat(cos.rollUpContextTo(k3, singleMergeLevel)).isEqualTo(new Context(Map.of("time", HierarchyFactory.get(TIME_MONTH, YearMonth.of(2020, 2)), "location", HierarchyFactory.get(LOCATION_FIR, "LOVV"))));
        assertThat(cos.rollUpContextTo(k4, singleMergeLevel)).isEqualTo(new Context(Map.of("time", HierarchyFactory.get(TIME_MONTH, YearMonth.of(2020, 2)), "location", HierarchyFactory.get(LOCATION_LOCATION, "LOWW"))));
        assertThat(cos.rollUpContextTo(k5, singleMergeLevel)).isEqualTo(new Context(Map.of("time", HierarchyFactory.get(TIME_MONTH, YearMonth.of(2020, 2)), "location", HierarchyFactory.get(LOCATION_LOCATION, "LOWS"))));


        assertThat(cos.rollUpContextTo(k1, twoMergeLevels)).isEqualTo(k1);
        assertThat(cos.rollUpContextTo(k2, twoMergeLevels)).isEqualTo(new Context(Map.of("time", HierarchyFactory.get(TIME_YEAR, Year.of(2020)), "location", HierarchyFactory.get(LOCATION_FIR, "LOVV"))));
        assertThat(cos.rollUpContextTo(k3, twoMergeLevels)).isEqualTo(new Context(Map.of("time", HierarchyFactory.get(TIME_YEAR, Year.of(2020)), "location", HierarchyFactory.get(LOCATION_FIR, "LOVV"))));
        assertThat(cos.rollUpContextTo(k4, twoMergeLevels)).isEqualTo(new Context(Map.of("time", HierarchyFactory.get(TIME_YEAR, Year.of(2020)), "location", HierarchyFactory.get(LOCATION_FIR, "LOVV"))));
        assertThat(cos.rollUpContextTo(k5, twoMergeLevels)).isEqualTo(new Context(Map.of("time", HierarchyFactory.get(TIME_YEAR, Year.of(2020)), "location", HierarchyFactory.get(LOCATION_FIR, "LOVV"))));

        assertThat(cos.rollUpContextTo(k1, allLevels)).isEqualTo(k1);
        assertThat(cos.rollUpContextTo(k2, allLevels)).isEqualTo(k1);
        assertThat(cos.rollUpContextTo(k3, allLevels)).isEqualTo(k1);
        assertThat(cos.rollUpContextTo(k4, allLevels)).isEqualTo(k1);
        assertThat(cos.rollUpContextTo(k5, allLevels)).isEqualTo(k1);
    }
}
