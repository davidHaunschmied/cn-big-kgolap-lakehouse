package at.jku.dke.bigkgolap.bed.service;

public enum CacheLevel {
    GRAPH_IN_MEMORY(1),
    FILE_STORED_LOCALLY(2),
    NONE(-1);

    private final int level;

    CacheLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
