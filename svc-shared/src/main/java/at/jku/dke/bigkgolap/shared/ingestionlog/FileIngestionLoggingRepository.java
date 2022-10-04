package at.jku.dke.bigkgolap.shared.ingestionlog;

import java.util.List;

public interface FileIngestionLoggingRepository {
    void ingestionStarted(String storedName);

    boolean ingestionFinished(String storedName);

    List<FileIngestionLog> getLogs();
}
