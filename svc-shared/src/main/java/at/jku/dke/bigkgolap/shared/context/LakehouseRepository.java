package at.jku.dke.bigkgolap.shared.context;

import at.jku.dke.bigkgolap.api.model.Context;
import at.jku.dke.bigkgolap.api.model.LakehouseFile;
import at.jku.dke.bigkgolap.api.model.SliceDiceContext;
import at.jku.dke.bigkgolap.shared.model.LakehouseStats;

import java.util.List;
import java.util.Set;

/*
TODO: Split it up into SurfaceRepository and BedRepository
 */
public interface LakehouseRepository {
    boolean upsertFileDetails(String storedName, String fileType, String originalName, long sizeBytes);

    boolean upsertContext(Context context);

    boolean upsertFile(Context context, String storedName, String fileType);

    Set<String> getGeneralContextIds(SliceDiceContext context);

    Set<Context> getSpecificContexts(SliceDiceContext context);

    List<LakehouseFile> getLakehouseFiles(String contextId);

    LakehouseStats getLakehouseStats();
}
