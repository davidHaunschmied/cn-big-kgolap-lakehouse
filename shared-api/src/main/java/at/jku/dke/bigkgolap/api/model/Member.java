package at.jku.dke.bigkgolap.api.model;

import at.jku.dke.bigkgolap.api.model.rollup.RollUpFun;

import java.util.Objects;

public class Member implements Comparable<Member> {
    private final Level level;
    private final Object value;

    public Member(Level level, Object value) {
        this.level = Objects.requireNonNull(level);
        if (value != null && !this.level.getType().isInstance(value)) {
            throw new IllegalArgumentException("Value is no instance of type " + this.level.getType().getSimpleName());
        }
        this.value = value;
    }

    public static Member of(Level level, Object value) {
        return new Member(level, value);
    }

    public Level getLevel() {
        return level;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Member) {
            return Objects.equals(level, ((Member) obj).level)
                    && (Objects.equals(value, ((Member) obj).value));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, value);
    }

    @Override
    public String toString() {
        return String.format("Member{level=%s, value=%s}", level.toString(), value.toString());
    }

    public Member rollUp() {
        return RollUpFun.rollUp(this);
    }

    @Override
    public int compareTo(Member o) {
        return CubeSchema.getInstance().getComparator().compare(this.level, o.level);
    }
}
