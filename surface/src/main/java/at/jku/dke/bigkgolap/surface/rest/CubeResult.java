package at.jku.dke.bigkgolap.surface.rest;

import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
public class CubeResult {
    private final boolean success;
    private final int consideredContexts;
    private final int nrOfContexts;
    private final Set<String> quads;
    private final long nrOfQuads;
    private final Instant ts1QueryRelevant;
    private final Instant ts2QueryGeneral;
    private final Instant ts3PrepareQuery;
}
