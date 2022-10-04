package at.jku.dke.bigkgolap.bgprocessor.indexing;

import at.jku.dke.bigkgolap.api.model.Context;
import at.jku.dke.bigkgolap.shared.cache.GraphCache;
import at.jku.dke.bigkgolap.shared.context.DatabaseService;
import at.jku.dke.bigkgolap.shared.ingestionlog.FileIngestionLoggingRepository;
import at.jku.dke.bigkgolap.shared.messaging.FileIngestMessage;
import at.jku.dke.bigkgolap.shared.messaging.MessagingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class DataIngestionScheduler {

    private static final int BATCH_SIZE = 5;

    private final MessagingService messagingService;

    private final FileIngestionLoggingRepository ingestionLogging;

    private final ContextExtractionService contextExtractionService;

    private final GraphCache graphCache;

    private final DatabaseService databaseService;

    public DataIngestionScheduler(MessagingService messagingService, FileIngestionLoggingRepository ingestionLogging, ContextExtractionService contextExtractionService, GraphCache graphCache, DatabaseService databaseService) {
        this.messagingService = messagingService;
        this.ingestionLogging = ingestionLogging;
        this.contextExtractionService = contextExtractionService;
        this.graphCache = graphCache;
        this.databaseService = databaseService;
    }

    @Scheduled(fixedDelay = 100)
    public void ingest() {
        List<FileIngestMessage> fileIngestMessages = messagingService.pollNextFiles(BATCH_SIZE);

        fileIngestMessages.parallelStream().forEach(message -> {
            final String storedName = message.getStoredName();
            final String fileType = message.getFileType();
            log.info("Found new file {} of type {}!", storedName, fileType);

            Set<Context> contexts;
            try {
                contexts = contextExtractionService.analyze(storedName, fileType);
            } catch (Exception e) {
                log.error("Could not analyze file with name {} and type {}!", storedName, fileType, e);
                return;
            }

            Set<Boolean> insertionResults = new HashSet<>();
            for (Context context : contexts) {
                insertionResults.add(databaseService.upsertContext(context));
                insertionResults.add(databaseService.upsertFile(context, storedName, fileType));
            }

            insertionResults.add(ingestionLogging.ingestionFinished(storedName));

            // if every database insert went well, evict the graph cache and delete the message from queue
            if (!insertionResults.contains(false)) {
                graphCache.deleteCachedGraphs(contexts);
                messagingService.deleteMessage(message.getReceiptHandle());
                log.info("File {} of type {} processed successfully. Deleted it from queue!", storedName, fileType);
            } else {
                log.error("Something went wrong. Not deleting file {} of type {} from queue!", storedName, fileType);
            }
        });
        // highly likely that there are still messages in the queue, keep working
        if (fileIngestMessages.size() == BATCH_SIZE) {
            ingest();
        }
    }
}
