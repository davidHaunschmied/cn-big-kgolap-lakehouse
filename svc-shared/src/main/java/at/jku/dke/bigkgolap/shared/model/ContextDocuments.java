package at.jku.dke.bigkgolap.shared.model;

import lombok.Data;

import java.util.Set;

@Data
public class ContextDocuments {
    private final int contextId;
    private final Set<String> fileIds;
}
