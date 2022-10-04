package at.jku.dke.bigkgolap.surface.rest;

import at.jku.dke.bigkgolap.api.model.Level;
import lombok.Data;

@Data
public class LevelDto {
    private final String name;
    private final String dimension;
    private final String id;
    private final String type;

    LevelDto(Level level) {
        this.name = level.toString();
        this.dimension = level.getDimension();
        this.id = level.getId();
        this.type = level.getType().getSimpleName();
    }
}
