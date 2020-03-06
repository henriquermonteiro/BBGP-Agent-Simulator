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

    public Plan(SleepingGoal goal, Object actions, Set<FolFormula> beliefContext, HashMap<String, Double> resourceContext) {
        this.goal = goal;
        this.actions = actions;
        this.beliefContext = beliefContext;
        this.resourceContext = resourceContext;
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

    public Set<FolFormula> getUnifiedSet(FolAtom fullPredicate) {
        if(!goal.getGoalPredicate().getName().equals(fullPredicate.getName())){
            throw new IllegalArgumentException("fullPredicate must be compatible with the goal from the plan.");
        }
        
        List<Term<?>> goalTerms = goal.getGoalPredicate().getArguments();
        List<Term<?>> fullTerms = fullPredicate.getArguments();
        
        HashMap<Term<?>, Term<?>> mapping = new HashMap<>();
        
        for(int k = 0; k < goalTerms.size(); k++){
            if(!goalTerms.get(k).equals(fullTerms.get(k))){
                mapping.put(goalTerms.get(k), fullTerms.get(k));
            }
        }
        
        HashSet<FolFormula> set = new HashSet<>();
        
        for(FolFormula f : beliefContext){
            set.add((FolFormula)f.substitute(mapping));
        }
        
        return set;
    }
}
