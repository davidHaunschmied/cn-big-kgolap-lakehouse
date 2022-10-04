package at.jku.dke.bigkgolap.surface.query;

import at.jku.dke.bigkgolap.api.model.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MergeAndPropagateService {

    public MergeAndPropagateResult mergeAndPropagate(Set<Context> storedContexts, MergeLevels mergeLevels) {
        final Map<String, Set<Context>> contextMap = new ConcurrentHashMap<>(storedContexts.size());
        final Set<Context> finalContexts = Sets.newConcurrentHashSet();

        storedContexts.parallelStream().forEach(storedContext -> {
            final String contextId = storedContext.getId();
            final Set<Context> relevantContexts = new HashSet<>();

            for (Context relevantCandidate : storedContexts) {
                if (contextId.equals(relevantCandidate.getId()) || rollsUpTo(relevantCandidate, storedContext)) {
                    final Context finalContext = mergeLevels == null ? relevantCandidate :
                            rollUpContextTo(relevantCandidate, mergeLevels.getMergeLevels());

                    relevantContexts.add(finalContext);
                    finalContexts.add(finalContext);
                }
            }

            contextMap.put(contextId, relevantContexts);
        });

        return new MergeAndPropagateResult(contextMap, finalContexts);
    }

    /**
     * @param special
     * @param general
     * @return true if the special context rolls up to the general context or if both are equal
     */
    @VisibleForTesting
    boolean rollsUpTo(Context special, Context general) {
        SortedMap<String, Hierarchy> specialHierarchies = special.getHierarchies();
        SortedMap<String, Hierarchy> generalHierarchies = general.getHierarchies();

        for (String dimension : CubeSchema.getInstance().getDimensions()) {
            List<Member> specialMembers = specialHierarchies.get(dimension).getMembers(); // e.g. 2018>2018-12
            List<Member> generalMembers = generalHierarchies.get(dimension).getMembers(); // e.g. 2018 or empty

            if (specialMembers.size() < generalMembers.size()) {
                // special hierarchy is on a higher level than general hierarchy
                return false;
            }

            for (int i = 0; i < generalMembers.size(); i++) {
                try {
                    Member specialMember = specialMembers.get(i);
                    Member generalMember = generalMembers.get(i);

                    if (!generalMember.equals(specialMember)) {
                        return false;
                    }
                } catch (IndexOutOfBoundsException e) {
                    log.error("Error when trying to roll up special context {} to general context {}", special, general, e);
                    return false;
                }
            }
        }

        return true;
    }

    private Member rollUpToLevel(Member member, Level level) {
        if (!CubeSchema.getInstance().rollsUpTo(member.getLevel(), level)) {
            throw new IllegalArgumentException("Member level " + member.getLevel().getId() + " does not roll up to " + member.getLevel().getId());
        }

        Member rolledUp = member;

        do {
            rolledUp = rolledUp.rollUp();
        } while (rolledUp.getLevel() != level);

        return rolledUp;
    }

    private boolean rollsUpTo(@NonNull Member specialMember, Member generalMember) {
        if (generalMember == null) {
            return true;
        }

        if (!CubeSchema.getInstance().rollsUpTo(specialMember.getLevel(), generalMember.getLevel())) {
            return false;
        }

        return generalMember.equals(rollUpToLevel(specialMember, generalMember.getLevel()));
    }

    /**
     * Attempts to roll up the context to the given merge levels. If one or more dimensions can not be rolled up, the
     * level for the respective dimension remains the same. If no dimension can be rolled up to the given merge levels
     * (= the merge levels are on a lower granularity than the levels of the given context), the same context is returned.
     * For instance, the context 2018-02;LOWW together with merge levels "time_day" and "location_fir" can only
     * be partly rolled up: 2018-02 can not be rolled up to the "time_day" day level, hence it stays on the
     * "time_month" level. "LOWW" can be rolled up to the "location_fir" level, resulting in "LOVV". In this case,
     * the resulting context would be 2018-02:LOVV.
     *
     * @param context     The context to roll up
     * @param mergeLevels The merge levels on which to roll up
     * @return The rolled up context
     */
    @VisibleForTesting
    Context rollUpContextTo(Context context, Map<String, Level> mergeLevels) {
        if (context == null) {
            return null;
        }

        SortedMap<String, Hierarchy> hierarchies = context.getHierarchies();
        SortedMap<String, Hierarchy> rolledUpHierarchies = new TreeMap<>();

        for (String dimension : CubeSchema.getInstance().getDimensions()) {
            Hierarchy currentHierarchy = hierarchies.get(dimension);
            Level mergeLevel = mergeLevels.get(dimension);

            if (mergeLevels.containsKey(dimension) && mergeLevel == null) {
                rolledUpHierarchies.put(dimension, Hierarchy.all(dimension));
                continue;
            }

            List<Member> members = new ArrayList<>();
            for (Member member : currentHierarchy.getMembers()) {
                members.add(member);

                if (member.getLevel() == mergeLevel) {
                    break;
                }
            }

            Hierarchy hierarchy;
            if (members.isEmpty()) {
                hierarchy = Hierarchy.all(dimension);
            } else {
                hierarchy = Hierarchy.of(members);
            }

            rolledUpHierarchies.put(dimension, hierarchy);
        }

        return new Context(rolledUpHierarchies);
    }
}
