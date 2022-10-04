package at.jku.dke.bigkgolap.api.model;

import at.jku.dke.bigkgolap.api.Utils;
import at.jku.dke.bigkgolap.api.model.exceptions.InvalidContextException;

import java.util.*;
import java.util.stream.Collectors;

public class Context {
    private static final String EMPTY_CONTEXT_ID = "EMPTY";

    private static final String CONTEXT_STRING_JOINER = "_";

    private final SortedMap<String, Hierarchy> hierarchies;
    private final String id;

    public Context() {
        this(Map.of());
    }

    public Context(Map<String, Hierarchy> hierarchies) {
        this.hierarchies = Collections.unmodifiableSortedMap(validate(hierarchies));
        this.id = calculateId();
    }

    public static Context of() {
        return new Context();
    }

    public static Context of(String d1, Hierarchy h1, String d2, Hierarchy h2, String d3, Hierarchy h3) {
        return new Context(Map.of(d1, h1, d2, h2, d3, h3));
    }

    private String calculateId() {
        StringBuilder sb = new StringBuilder();
        for (Hierarchy hierarchy : hierarchies.values()) {
            sb.append(hierarchy.getId());
        }
        return Utils.sha1(sb.length() > 0 ? sb.toString() : EMPTY_CONTEXT_ID);
    }

    public static Context of(Map<String, Hierarchy> hierarchies) {
        return new Context(hierarchies);
    }

    private SortedMap<String, Hierarchy> validate(Map<String, Hierarchy> hierarchies) {
        Objects.requireNonNull(hierarchies);

        SortedMap<String, Hierarchy> result = new TreeMap<>();
        // Insert hierarchy per dimension into the context map or the all hierarchy if it is absent
        for (String dimension : CubeSchema.getInstance().getDimensions()) {
            Hierarchy hierarchy = hierarchies.getOrDefault(dimension, fillMissingHierarchies() ? Hierarchy.all(dimension) : null);

            if (hierarchy != null) {
                if (!dimension.equals(hierarchy.getDimension())) {
                    throw new InvalidContextException("Invalid hierarchy given for dimension " + dimension + ": " + hierarchy);
                }
                result.put(dimension, hierarchy);
            }
        }

        return result;
    }

    protected boolean fillMissingHierarchies() {
        return true;
    }

    @Override
    public final String toString() {
        return getUniqueContextName();
    }

    public final String getUniqueContextName() {
        StringJoiner stringJoiner = new StringJoiner(CONTEXT_STRING_JOINER);
        stringJoiner.setEmptyValue(EMPTY_CONTEXT_ID);
        for (Map.Entry<String, Hierarchy> entry : this.hierarchies.entrySet()) {
            stringJoiner.add(entry.getValue().toString());
        }
        return stringJoiner.toString();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Context context = (Context) o;
        return id.equals(context.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }

    public final List<Member> getFlatMembers() {
        return hierarchies.values().stream().flatMap(hierarchy -> hierarchy.getMembers().stream()).collect(Collectors.toList());
    }

    public final SortedMap<String, Hierarchy> getHierarchies() {
        return hierarchies;
    }

    public final String getId() {
        return id;
    }
}
