package at.jku.dke.bigkgolap.api.model;

import java.util.Map;

/*
Does not fill missing hierarchies -> otherwise slice/dice for all contexts and the context on the ALL-level would be equal
E.g. 'time_year=2018' would be equal to 'time_year=2018 AND location_all AND topic_all'
 */
public class SliceDiceContext extends Context {

    public SliceDiceContext(Map<String, Hierarchy> hierarchies) {
        super(hierarchies);
    }

    public SliceDiceContext() {
        this(Map.of());
    }

    public static SliceDiceContext of() {
        return new SliceDiceContext();
    }

    public static SliceDiceContext of(String d1, Hierarchy h1, String d2, Hierarchy h2, String d3, Hierarchy h3) {
        return new SliceDiceContext(Map.of(d1, h1, d2, h2, d3, h3));
    }

    public static SliceDiceContext of(Map<String, Hierarchy> hierarchies) {
        return new SliceDiceContext(hierarchies);
    }

    @Override
    protected boolean fillMissingHierarchies() {
        return false;
    }
}
