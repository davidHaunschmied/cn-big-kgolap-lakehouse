package at.jku.dke.bigkgolap.api.model;

import lombok.Data;

@Data
public class LakehouseFile {
    private final String storedName;
    private final String fileType;
}
