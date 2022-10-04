package at.jku.dke.bigkgolap.shared.messaging;

import lombok.Data;

@Data
public class FileIngestMessage {
    private final String storedName;
    private final String fileType;
    private final String receiptHandle;
}
