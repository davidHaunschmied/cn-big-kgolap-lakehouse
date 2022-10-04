package at.jku.dke.bigkgolap.shared.ingestionlog;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class FileIngestionLog {
    private final String storedName;
    private final ZonedDateTime start;
    private final ZonedDateTime end;
    private final long durationMillis;
}
