package at.jku.dke.bigkgolap.shared.context;

import at.jku.dke.bigkgolap.api.model.*;
import at.jku.dke.bigkgolap.shared.config.LakehouseConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.data.cassandra.core.cql.AsyncCqlTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CassandraContextRepository.class, LakehouseConfig.class, CassandraAutoConfiguration.class})
class CassandraContextRepositoryTest {

    private static final Level TIME_YEAR = CubeSchema.getInstance().locate("time", "year");
    private static final Level TIME_MONTH = CubeSchema.getInstance().locate("time", "month");
    private static final Level TIME_DAY = CubeSchema.getInstance().locate("time", "day");

    private static final Level LOCATION_TERRITORY = CubeSchema.getInstance().locate("location", "territory");
    private static final Level LOCATION_FIR = CubeSchema.getInstance().locate("location", "fir");
    private static final Level LOCATION_LOCATION = CubeSchema.getInstance().locate("location", "location");

    private static final Level TOPIC_CATEGORY = CubeSchema.getInstance().locate("topic", "category");
    private static final Level TOPIC_FAMILY = CubeSchema.getInstance().locate("topic", "family");
    private static final Level TOPIC_FEATURE = CubeSchema.getInstance().locate("topic", "feature");

    @Autowired
    CassandraContextRepository repository;

    @Autowired
    AsyncCqlTemplate cassie;

    private List<String> expectedContextIds;

    @Test
    void testUpsertContexts() {
        Context global = Context.of(Map.of());

        Context yearFirFeature = Context.of(Map.of("time", Hierarchy.of(Member.of(TIME_YEAR, Year.of(2018))),
                "location", Hierarchy.of(Member.of(LOCATION_TERRITORY, "Austria"), Member.of(LOCATION_FIR, "LOVV")),
                "topic", Hierarchy.of(Member.of(TOPIC_CATEGORY, "AirportHeliport"), Member.of(TOPIC_FAMILY, "AirportHeliport"), Member.of(TOPIC_FEATURE, "AirportHeliport"))));

        Context month = Context.of(Map.of("time", Hierarchy.of(Member.of(TIME_YEAR, Year.of(2019)), Member.of(TIME_MONTH, YearMonth.of(2019, 12)))));

        Context location = Context.of(Map.of("location", Hierarchy.of(Member.of(LOCATION_TERRITORY, "Austria"), Member.of(LOCATION_FIR, "LOVV"), Member.of(LOCATION_LOCATION, "LOWS"))));

        assertThat(repository.upsertContext(global)).isTrue();
        assertThat(repository.upsertContext(yearFirFeature)).isTrue();
        assertThat(repository.upsertContext(month)).isTrue();
        assertThat(repository.upsertContext(location)).isTrue();

        expectedContextIds = List.of(global.getId(), yearFirFeature.getId(), month.getId(), location.getId());
    }

    private Set<String> idsOf(Set<Context> specificContexts) {
        return specificContexts.stream().map(Context::getId).collect(Collectors.toSet());
    }

    @Test
    void testGetSpecificContexts() {
        testUpsertContexts();

        assertThat(idsOf(repository.getSpecificContexts(new SliceDiceContext())))
                .containsExactlyInAnyOrderElementsOf(expectedContextIds);
        assertThat(idsOf(repository.getSpecificContexts(new SliceDiceContext(Map.of(
                "time", HierarchyFactory.get(TIME_YEAR, Year.of(2018)),
                "location", HierarchyFactory.get(LOCATION_FIR, "LOVV"))))))
                .containsExactlyInAnyOrder(expectedContextIds.get(1));
        assertThat(idsOf(repository.getSpecificContexts(new SliceDiceContext(Map.of(
                "time", Hierarchy.of(Member.of(TIME_YEAR, Year.of(2019)), Member.of(TIME_MONTH, YearMonth.of(2019, 12))))))))
                .containsExactlyInAnyOrder(expectedContextIds.get(2));
        assertThat(idsOf(repository.getSpecificContexts(new SliceDiceContext(Map.of(
                "location", Hierarchy.of(Member.of(LOCATION_TERRITORY, "Austria"), Member.of(LOCATION_FIR, "LOVV"), Member.of(LOCATION_LOCATION, "LOWS")))))))
                .containsExactlyInAnyOrder(expectedContextIds.get(3));
        assertThat(idsOf(repository.getSpecificContexts(new SliceDiceContext(Map.of("location", HierarchyFactory.get(LOCATION_FIR, "LOVV"))))))
                .containsExactlyInAnyOrder(expectedContextIds.get(1), expectedContextIds.get(3));
    }

    @Test
    void testUpsertFile() {
        Hierarchy time = Hierarchy.of(Member.of(TIME_YEAR, Year.of(2018)));
        Hierarchy location = Hierarchy.of(Member.of(LOCATION_TERRITORY, "Austria"), Member.of(LOCATION_FIR, "LOVV"));
        Hierarchy topic = Hierarchy.of(Member.of(TOPIC_CATEGORY, "AirportHeliport"), Member.of(TOPIC_FAMILY, "AirportHeliport"), Member.of(TOPIC_FEATURE, "AirportHeliport"));

        Context context = Context.of(Map.of("time", time,
                "location", location,
                "topic", topic));

        assertThat(repository.upsertFile(context, "stored-file.xml", "AIXM")).isTrue();

        assertThat(repository.getLakehouseFiles(context.getId())).containsExactly(new LakehouseFile("stored-file.xml", "AIXM"));
    }

    @Test
    void testUpsertFileDetails() {
        assertThat(repository.upsertFileDetails("stored-file.xml", "AIXM", "orignal-name.xml", 100L)).isTrue();
        assertThat(repository.upsertFileDetails("stored-file2.xml", "AIXM", "orignal-name2.xml", 100L)).isTrue();

        assertThat(repository.getLakehouseStats().getTotalFiles()).isEqualTo(2);
    }

    @Test
    void testGetGeneralContexts() {
        testUpsertContexts();

        assertThat(repository.getGeneralContextIds(new SliceDiceContext())).isEmpty();
        assertThat(repository.getGeneralContextIds(new SliceDiceContext(Map.of(
                "time", HierarchyFactory.get(TIME_YEAR, Year.of(2018)),
                "location", HierarchyFactory.get(LOCATION_FIR, "LOVV")))))
                .containsExactlyInAnyOrder(expectedContextIds.get(0));
        assertThat(repository.getGeneralContextIds(new SliceDiceContext(Map.of(
                "time", HierarchyFactory.get(TIME_DAY, LocalDate.of(2019, 12, 10))))))
                .containsExactlyInAnyOrder(expectedContextIds.get(0), expectedContextIds.get(2), expectedContextIds.get(3));
        assertThat(repository.getGeneralContextIds(new SliceDiceContext(Map.of(
                "location", Hierarchy.of(Member.of(LOCATION_TERRITORY, "Austria"), Member.of(LOCATION_FIR, "LOVV"), Member.of(LOCATION_LOCATION, "LOWS"))))))
                .containsExactlyInAnyOrder(expectedContextIds.get(0), expectedContextIds.get(1), expectedContextIds.get(2));
        assertThat(repository.getGeneralContextIds(new SliceDiceContext(Map.of("topic", HierarchyFactory.get(TOPIC_FAMILY, "AirportHeliport")))))
                .containsExactlyInAnyOrder(expectedContextIds.get(0), expectedContextIds.get(2), expectedContextIds.get(3));
        assertThat(repository.getGeneralContextIds(new SliceDiceContext(Map.of("location", Hierarchy.of(Member.of(LOCATION_TERRITORY, "Austria"), Member.of(LOCATION_FIR, "LOVV"), Member.of(LOCATION_LOCATION, "LOWS")),
                "topic", Hierarchy.of(Member.of(TOPIC_CATEGORY, "AirportHeliport"), Member.of(TOPIC_FAMILY, "AirportHeliport"), Member.of(TOPIC_FEATURE, "AirportHeliport"))))))
                .containsExactlyInAnyOrder(expectedContextIds.get(0), expectedContextIds.get(1), expectedContextIds.get(2), expectedContextIds.get(3));
    }

}
