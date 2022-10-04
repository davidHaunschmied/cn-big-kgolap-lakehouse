package at.jku.dke.bigkgolap.api.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HierarchyFactory {

    private HierarchyFactory() {

    }

    public static Hierarchy get(Level level, Object value) {
        return get(Member.of(level, value));
    }

    public static Hierarchy get(Member member) {
        List<Member> fromSpecToGen = new ArrayList<>();
        Member current = member;
        while (current != null) {
            fromSpecToGen.add(current);
            current = current.rollUp();
        }
        Collections.reverse(fromSpecToGen);
        return Hierarchy.of(fromSpecToGen);
    }
}
