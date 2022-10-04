package at.jku.dke.bigkgolap.shared.querylog;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class QueryLog {
    private final String query;
    private final ZonedDateTime start;
    private final ZonedDateTime end;
    private final long dur1QueryRelevantMillis;
    private final long dur2QueryGeneralMillis;
    private final long dur3PrepareQueryMillis;
    private final long dur4BuildGraphMillis;
    private final long durationMillis;
    private final boolean success;
    private final String exceptionMessage;
    private final int contextRequests;
    //    private final double graphInMemCacheHitRatio;
//    private final double localFileCacheHitRatio;
    private final int contexts;
    private final long quads;
}
