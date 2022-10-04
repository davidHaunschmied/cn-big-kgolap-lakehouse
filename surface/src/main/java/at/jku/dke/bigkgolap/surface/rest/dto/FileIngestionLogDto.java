package at.jku.dke.bigkgolap.surface.rest.dto;

import at.jku.dke.bigkgolap.shared.ingestionlog.FileIngestionLog;
import lombok.Data;

@Data
public class FileIngestionLogDto {
    private final String storedName;
    private final long startMinute;
    private final long endMinute;
    private final long durationMillis;

    public FileIngestionLogDto(FileIngestionLog log) {
        this.storedName = log.getStoredName();
        this.durationMillis = log.getDurationMillis();

        this.startMinute = (log.getStart() != null && log.getStart().toEpochSecond() != 0) ? log.getStart().toEpochSecond() / 60 : -1;
        this.endMinute = (log.getEnd() != null && log.getEnd() != log.getStart() && log.getEnd().toEpochSecond() != 0) ? log.getEnd().toEpochSecond() / 60 : -1;
    }
}
