package at.jku.dke.bigkgolap.shared;

import at.jku.dke.bigkgolap.shared.config.LakehouseConfig;
import at.jku.dke.bigkgolap.shared.storage.StorageService;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.data.cassandra.CassandraQuerySyntaxException;
import org.springframework.data.cassandra.core.cql.AsyncCqlTemplate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.util.Objects.requireNonNull;

@Slf4j
public class LakehouseInitializer {
    private final AsyncCqlTemplate cassie;

    private final LakehouseConfig lakehouseConfig;

    private final StorageService storageService;


    public LakehouseInitializer(AsyncCqlTemplate cassie, LakehouseConfig lakehouseConfig, StorageService storageService) {
        this.cassie = cassie;
        this.lakehouseConfig = lakehouseConfig;
        this.storageService = storageService;
    }

    public void init() {
        if (lakehouseConfig.isReset()) {
            log.warn("Reset was true, applying {}, removing all stored files!", lakehouseConfig.getCassCleanFile());
            try {
                throwIfFalse(execute(lakehouseConfig.getCassCleanFile()));
                if (lakehouseConfig.isClearStorageDirOnReset()) {
                    storageService.clearAll();
                }
            } catch (IOException | ExecutionException | InterruptedException | RuntimeException e) {
                log.error("Could not clean the lakehouse!", e);
                throw new RuntimeException(e);
            }
            log.warn("Successfully reset the lakehouse!");
        }
        try {
            log.info("Applying {}!", lakehouseConfig.getCassSetupFile());
            throwIfFalse(execute(lakehouseConfig.getCassSetupFile()));
        } catch (IOException | ExecutionException | InterruptedException e) {
            log.error("Could not clean lakehouse!", e);
            throw new RuntimeException(e);
        }

    }

    private void throwIfFalse(boolean success) {
        if (!success) {
            throw new IllegalStateException("Statement was not successful!");
        }
    }

    private boolean execute(String cqlFile) throws IOException, ExecutionException, InterruptedException {
        String content = IOUtils.toString(requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(cqlFile)), Charset.defaultCharset());
        StringBuilder strippedContent = new StringBuilder();
        List<Boolean> booleans = new ArrayList<>();
        for (String line : content.split("\n|(\r\n)")) {
            int commentIndex = line.indexOf("--");
            strippedContent.append(line, 0, commentIndex > -1 ? commentIndex : line.length());
        }
        for (String stmt : strippedContent.toString().split(";")) {
            String trimmedStmt = stmt.trim();
            if (trimmedStmt.isEmpty()) {
                continue;
            }
            try {
                booleans.add(cassie.execute(SimpleStatement.builder(trimmedStmt + ";").setTimeout(Duration.ofSeconds(10)).build()).get());
            } catch (ExecutionException e) {
                if (e.getCause() instanceof CassandraQuerySyntaxException) {
                    log.error("CassandraQuerySyntaxException: {}", trimmedStmt);
                }
                throw e;
            }
        }
        return booleans.stream().filter(aBoolean -> !aBoolean).findAny().orElse(true);
    }
}
