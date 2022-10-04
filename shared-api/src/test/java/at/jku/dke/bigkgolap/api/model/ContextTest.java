package at.jku.dke.bigkgolap.api.model;

import at.jku.dke.bigkgolap.api.model.exceptions.InvalidContextException;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextTest {

    private static final Level TIME_YEAR = CubeSchema.getInstance().locate("time", "year");

    private static final Level LOCATION_TERRITORY = CubeSchema.getInstance().locate("location", "territory");
    private static final Level LOCATION_FIR = CubeSchema.getInstance().locate("location", "fir");

    private static final Level TOPIC_CATEGORY = CubeSchema.getInstance().locate("topic", "category");
    private static final Level TOPIC_FAMILY = CubeSchema.getInstance().locate("topic", "family");
    private static final Level TOPIC_FEATURE = CubeSchema.getInstance().locate("topic", "feature");

    Hierarchy time = Hierarchy.of(Member.of(TIME_YEAR, Year.of(2018)));
    Hierarchy location = Hierarchy.of(Member.of(LOCATION_TERRITORY, "Austria"), Member.of(LOCATION_FIR, "LOVV"));
    Hierarchy topic = Hierarchy.of(Member.of(TOPIC_CATEGORY, "AirportHeliport"), Member.of(TOPIC_FAMILY, "AirportHeliport"), Member.of(TOPIC_FEATURE, "AirportHeliport"));

    @Test
    void testValidContexts() {
        assertThat(new Context()).isNotNull();
        assertThat(Context.of()).isNotNull();
        assertThat(Context.of(Map.of("time", Hierarchy.all("time"),
                "location", Hierarchy.all("location"),
                "topic", Hierarchy.all("topic")))).isNotNull();

        assertThat(Context.of(Map.of("time", time,
                "location", location,
                "topic", topic))).isNotNull();
    }

    @Test
    void testInvalidContexts() {
        assertThatThrownBy(() -> Context.of(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Context.of(Map.of("time", time,
                "location", time,
                "topic", topic))).isInstanceOf(InvalidContextException.class);
    }

    @Test
    void testToString() {
        assertThat(Context.of(Map.of("time", Hierarchy.all("time"),
                "location", Hierarchy.all("location"),
                "topic", Hierarchy.all("topic"))).toString())
                .isEqualTo("location:ALL_time:ALL_topic:ALL");

        assertThat(Context.of(Map.of("time", time,
                "location", location,
                "topic", topic)).toString())
                .isEqualTo("location:Austria>LOVV_time:2018_topic:AirportHeliport>AirportHeliport>AirportHeliport");
    }
}
