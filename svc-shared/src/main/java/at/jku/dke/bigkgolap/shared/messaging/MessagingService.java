package at.jku.dke.bigkgolap.shared.messaging;

import java.util.List;

public interface MessagingService {
    void registerNewFile(String storedName, String fileType);

    List<FileIngestMessage> pollNextFiles(int max);

    void deleteMessage(String receiptHandle);
}
