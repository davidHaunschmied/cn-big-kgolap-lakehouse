package at.jku.dke.bigkgolap.shared.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LakehouseStats {
    private final long totalIndices;
    private final long totalContexts;
    private final long totalFiles;
    private final long totalStoredFileSizeBytes;
}
