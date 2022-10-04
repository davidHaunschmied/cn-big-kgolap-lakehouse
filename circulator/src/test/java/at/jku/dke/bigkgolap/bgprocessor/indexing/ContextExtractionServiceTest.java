package at.jku.dke.bigkgolap.bgprocessor.indexing;

import at.jku.dke.bigkgolap.api.engines.Analyzer;
import at.jku.dke.bigkgolap.api.engines.AnalyzerResult;
import at.jku.dke.bigkgolap.api.engines.Engine;
import at.jku.dke.bigkgolap.api.model.*;
import at.jku.dke.bigkgolap.shared.context.LakehouseRepository;
import at.jku.dke.bigkgolap.shared.storage.StorageService;
import at.jku.dke.bigkgolap.shared.storage.TieredStorageInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.Map;
import java.util.Set;

import static at.jku.dke.bigkgolap.api.model.Member.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class ContextExtractionServiceTest {

    private static final Level TIME_YEAR = CubeSchema.getInstance().locate("time", "year");
    private static final Level TIME_MONTH = CubeSchema.getInstance().locate("time", "month");
    private static final Level TIME_DAY = CubeSchema.getInstance().locate("time", "day");

    private static final Level LOCATION_LOCATION = CubeSchema.getInstance().locate("location", "location");

    private static final Level TOPIC_CATEGORY = CubeSchema.getInstance().locate("topic", "category");

    @Mock
    Map<String, Engine> engines;

    @Mock
    LakehouseRepository repo;

    @Mock
    StorageService storageService;

    @InjectMocks
    ContextExtractionService contextExtractionService;

    Analyzer analyzer;

    @BeforeEach
    void before() {
        Engine engine = mock(Engine.class);
        analyzer = mock(Analyzer.class);
        when(engines.get("AIXM")).thenReturn(engine);
        when(engines.getOrDefault("AIXM", null)).thenReturn(engine);
        when(engine.getAnalyzer()).thenReturn(analyzer);
    }

    @Test
    void testAdd() {
        TieredStorageInputStream fileIs = mock(TieredStorageInputStream.class);
        String storedName = "storedName";
        String fileType = "AIXM";

        Hierarchy febTen = Hierarchy.of(of(TIME_YEAR, Year.of(2020)),
                of(TIME_MONTH, YearMonth.of(2020, 2)),
                of(TIME_DAY, LocalDate.of(2020, 2, 10)));
        Hierarchy marchEleven = Hierarchy.of(of(TIME_YEAR, Year.of(2020)),
                of(TIME_MONTH, YearMonth.of(2020, 3)),
                of(TIME_DAY, LocalDate.of(2020, 3, 11)));

        Hierarchy loww = HierarchyFactory.get(of(LOCATION_LOCATION, "LOWW"));
        Hierarchy lows = HierarchyFactory.get(of(LOCATION_LOCATION, "LOWS"));

        Hierarchy airportHeliport = Hierarchy.of(of(TOPIC_CATEGORY, "AirportHeliport"));

        Set<Hierarchy> dimensionHierarchies = Set.of(
                febTen,
                marchEleven,

                loww,
                lows,

                airportHeliport);

        when(analyzer.analyze(any())).thenReturn(new AnalyzerResult(dimensionHierarchies));
        when(storageService.getInputStream(storedName)).thenReturn(fileIs);

        Set<Context> contexts = Set.of(
                Context.of(Map.of("time", febTen
                        , "location", loww, "topic", airportHeliport)),
                Context.of(Map.of("time", marchEleven,
                        "location", loww, "topic", airportHeliport)),
                Context.of(Map.of("time", febTen,
                        "location", lows, "topic", airportHeliport)),
                Context.of(Map.of("time", marchEleven,
                        "location", lows, "topic", airportHeliport))
        );

        assertThat(contextExtractionService.analyze(storedName, fileType))
                .hasSameElementsAs(contexts);
    }
}
