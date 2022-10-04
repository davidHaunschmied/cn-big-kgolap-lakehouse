package at.jku.dke.bigkgolap.api.model;

import at.jku.dke.bigkgolap.api.Utils;
import at.jku.dke.bigkgolap.api.model.exceptions.InvalidHierarchyException;
import lombok.Data;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

@Data
public final class Hierarchy implements Comparable<Hierarchy> {
    private final String dimension;
    private final List<Member> members;
    private final String id;

    public static Hierarchy of(List<Member> members) {
        return new Hierarchy(members);
    }

    public static Hierarchy all(String dimension) {
        return new Hierarchy(dimension);
    }

    public static Hierarchy of(Member... members) {
        return new Hierarchy(List.of(members));
    }

    public Hierarchy(String dimension) {
        this.dimension = dimension;
        this.members = List.of();
        this.id = calculateId();
    }

    public Hierarchy(List<Member> members) {
        validateHierarchy(members);
        this.dimension = members.get(0).getLevel().getDimension();
        this.members = List.copyOf(members);
        this.id = calculateId();
    }

    private String calculateId() {
        StringBuilder sb = new StringBuilder();
        sb.append(dimension);
        for (Member member : members) {
            sb.append(member.getLevel()).append(member.getValue());
        }
        return Utils.sha1(sb.toString());
    }

    private void validateHierarchy(List<Member> members) {
        if (members == null || members.isEmpty()) {
            throw new IllegalArgumentException("Can not read dimension from empty members list. " +
                    "Use another constructor to specify the dimension of the ALL hierarchy explicitly!");
        }

        Member above = null;
        for (Member member : members) {
            Member rolledUp = member.rollUp();
            if (!Objects.equals(rolledUp, above)) {
                throw new InvalidHierarchyException(String.format("Expected %s to roll up to %s but was %s",
                        member, above, rolledUp));
            }
            above = member;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hierarchy hierarchy = (Hierarchy) o;
        return Objects.equals(dimension, hierarchy.dimension) && Objects.equals(members, hierarchy.members);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, members);
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(">");
        joiner.setEmptyValue("ALL");
        for (Member member : members) {
            joiner.add(member.getValue().toString());
        }
        return dimension + ":" + joiner;
    }

    public String getId() {
        return id;
    }

    @Override
    public int compareTo(Hierarchy o) {
        int dim = dimension.compareTo(o.dimension);
        return dim != 0 ? dim : id.compareTo(o.id);
    }

    public Hierarchy rollUp() {
        switch (members.size()) {
            case 0:
                return null; // can not roll up hierarchy on ALL level
            case 1:
                return new Hierarchy(dimension);
            default:
                return new Hierarchy(members.subList(0, members.size() - 1));
        }
    }
}
