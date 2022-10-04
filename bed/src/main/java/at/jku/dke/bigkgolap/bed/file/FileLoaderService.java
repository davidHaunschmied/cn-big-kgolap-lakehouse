package at.jku.dke.bigkgolap.bed.file;

import at.jku.dke.bigkgolap.api.engines.Engine;
import at.jku.dke.bigkgolap.api.model.LakehouseFile;
import at.jku.dke.bigkgolap.bed.service.GraphResult;
import at.jku.dke.bigkgolap.shared.storage.StorageService;
import at.jku.dke.bigkgolap.shared.storage.TieredStorageInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class FileLoaderService {

    private final StorageService storageService;

    private final Map<String, Engine> engines;

    public FileLoaderService(StorageService storageService, Map<String, Engine> engines) {
        this.storageService = storageService;
        this.engines = engines;
    }

    public GraphResult loadFromFiles(List<LakehouseFile> files) {
        Model model = ModelFactory.createDefaultModel();

        int cacheHitCount = 0;
        for (LakehouseFile file : files) {
            String content;
            try (TieredStorageInputStream tsis = storageService.getInputStream(file.getStoredName())) {
                cacheHitCount += tsis.isCachedLocally() ? 1 : 0;
                content = IOUtils.toString(tsis, Charset.defaultCharset());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Engine engine = engines.get(file.getFileType().toUpperCase(Locale.ROOT));
            if (engine == null) {
                log.error("No engine provided for file type '{}'. Skipping file {}", file.getFileType(), file.getStoredName());
            } else {
                engine.getMapper().map(content, model);
            }
        }

        return GraphResult.fromFiles(model.getGraph(), (float) cacheHitCount / files.size());

    }
}
