package at.jku.dke.bigkgolap.api.model;

import lombok.Data;

import java.util.Set;

@Data
public final class StoredHierarchy {
    private final Hierarchy hierarchy;
    private final Set<String> associatedContexts;

    public StoredHierarchy(Hierarchy hierarchy, Set<String> associatedContexts) {
        this.hierarchy = hierarchy;
        this.associatedContexts = associatedContexts;
    }
}
