package at.jku.dke.bigkgolap.surface.rest.dto;

import at.jku.dke.bigkgolap.shared.model.LakehouseStats;
import lombok.Data;

@Data
public class LakehouseStatsDto {
    private final long totalIndices;
    private final long totalContexts;
    private final long totalFiles;
    private final long totalStoredFileSizeBytes;

    public static LakehouseStatsDto from(LakehouseStats lakehouseStats) {
        return new LakehouseStatsDto(lakehouseStats.getTotalIndices(),
                lakehouseStats.getTotalContexts(),
                lakehouseStats.getTotalFiles(),
                lakehouseStats.getTotalStoredFileSizeBytes());
    }
}
