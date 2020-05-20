/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.agent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.tweety.logics.commons.syntax.interfaces.Term;
import net.sf.tweety.logics.fol.syntax.FolAtom;
import net.sf.tweety.logics.fol.syntax.FolFormula;

/**
 *
 * @author henri
 */
public class Plan {

    private final SleepingGoal goal;
    private final Object actions;
    private final Set<FolFormula> beliefContext;
    private final HashMap<String, Double> resourceContext;
    private final Set<PerceptionEntry> postConditions;

    public Plan(SleepingGoal goal, Object actions, Set<FolFormula> beliefContext, HashMap<String, Double> resourceContext, Set<PerceptionEntry> postConditions) {
        this.goal = goal;
        this.actions = actions;
        this.beliefContext = beliefContext;
        this.resourceContext = resourceContext;

        if (postConditions == null) {
            this.postConditions = new HashSet<>();
        } else {
            this.postConditions = postConditions;
        }
    }

    public SleepingGoal getGoal() {
        return goal;
    }

    public Object getActions() {
        return actions;
    }

    public Set<FolFormula> getBeliefContext() {
        return beliefContext;
    }

    public HashMap<String, Double> getResourceContext() {
        return resourceContext;
    }

    public Set<PerceptionEntry> getPostConditions() {
        return postConditions;
    }

    public Set<FolFormula> getUnifiedSet(FolAtom fullPredicate) {
        if (!goal.getGoalPredicate().getName().equals(fullPredicate.getName())) {
            throw new IllegalArgumentException("fullPredicate must be compatible with the goal from the plan.");
        }

        List<Term<?>> goalTerms = goal.getGoalPredicate().getArguments();
        List<Term<?>> fullTerms = fullPredicate.getArguments();

        HashMap<Term<?>, Term<?>> mapping = new HashMap<>();

        for (int k = 0; k < goalTerms.size(); k++) {
            if (!goalTerms.get(k).equals(fullTerms.get(k))) {
                mapping.put(goalTerms.get(k), fullTerms.get(k));
            }
        }

        HashSet<FolFormula> set = new HashSet<>();

        for (FolFormula f : beliefContext) {
            set.add((FolFormula) f.substitute(mapping));
        }

        return set;
    }
    
    public HashMap<FolFormula, Boolean> getUnifiedBeliefPostConditionsSet(FolAtom fullPredicate) {
        if (!goal.getGoalPredicate().getName().equals(fullPredicate.getName())) {
            throw new IllegalArgumentException("fullPredicate must be compatible with the goal from the plan.");
        }

        List<Term<?>> goalTerms = goal.getGoalPredicate().getArguments();
        List<Term<?>> fullTerms = fullPredicate.getArguments();

        HashMap<Term<?>, Term<?>> mapping = new HashMap<>();

        for (int k = 0; k < goalTerms.size(); k++) {
            if (!goalTerms.get(k).equals(fullTerms.get(k))) {
                mapping.put(goalTerms.get(k), fullTerms.get(k));
            }
        }

        HashMap<FolFormula, Boolean> set = new HashMap<>();

        for(PerceptionEntry pC : postConditions){
            if(pC.isBeliefType()){
                FolFormula f = pC.getBelief();
                set.put((FolFormula) f.substitute(mapping), pC.getOperation() == PerceptionEntry.ADDITION_OPERATION);
            }
        }

        return set;
    }
    
    public HashMap<String, Double> getResourcePostConditionsSet(){
        HashMap<String, Double> set = new HashMap<>();
        
        for(PerceptionEntry pC : postConditions){
            if(pC.isResourceType()){
                set.put(pC.getResource(), pC.getResourceAmount() * (pC.getOperation() == PerceptionEntry.DELETION_OPERATION ? -1.0 : 1.0));
            }
        }
        
        return set;
    }
}
