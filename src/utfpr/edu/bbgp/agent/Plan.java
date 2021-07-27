/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.tweety.logics.commons.syntax.interfaces.Term;
import net.sf.tweety.logics.fol.syntax.FolAtom;
import net.sf.tweety.logics.fol.syntax.FolFormula;
import net.sf.tweety.logics.fol.syntax.Negation;
import utfpr.edu.bbgp.extended.ResourceFolFormula;

/**
 *
 * @author henri
 */
public class Plan {

    private final SleepingGoal goal;
    private final Object actions;
    private final Set<FolFormula> beliefContext;
    private final HashSet<ResourceFolFormula> resourceContext;
    private final List<PerceptionEntry> postConditions;

    public Plan(SleepingGoal goal, Object actions, Set<FolFormula> beliefContext, HashSet<ResourceFolFormula> resourceContext, List<PerceptionEntry> postConditions) {
        this.goal = goal;
        this.actions = actions;
        this.beliefContext = beliefContext;
        this.resourceContext = resourceContext;

        if (postConditions == null) {
            this.postConditions = new ArrayList<>();
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

    public HashSet<ResourceFolFormula> getResourceContext() {
        return resourceContext;
    }

    public List<PerceptionEntry> getPostConditions() {
        return postConditions;
    }
    
    public Set<FolFormula> getTerminalCheckSet(FolAtom fullPredicate){
        Set<FolFormula> set = getUnifiedSet(fullPredicate);
        Map<FolFormula, Boolean> postCond = getUnifiedBeliefPostConditionsSet(fullPredicate);
        
        for(FolFormula postC : postCond.keySet()){
            set.add((postCond.get(postC) ? postC : new Negation(postC)));
        }
        
        return set;
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
    
    public List<PerceptionEntry> getResourcePostConditionsSet(){
        ArrayList<PerceptionEntry> list = new ArrayList<>();
        
        for(PerceptionEntry pC : postConditions){
            if(pC.isResourceType()){
                list.add(pC);
            }
        }
        
        return list;
    }

    @Override
    public String toString() {
        String string = goal.toString() + " : ";

        for (FolFormula folF : beliefContext) {
            string += folF.toString() + ", ";
        }

        for (ResourceFolFormula res : resourceContext) {
            string += res.toString() + ", ";
        }

        if (!beliefContext.isEmpty() || !resourceContext.isEmpty()) {
            string = string.substring(0, string.length() - 2);
        }
        
        string += " <- ";
        
        for(PerceptionEntry perEntry : postConditions){
            string += perEntry.toString().replace("[0-9]+\\s:\\s", "") + "; ";
        }

        if (!postConditions.isEmpty()) {
            string = string.substring(0, string.length() - 2);
        }
        
        string += ".";

        return string;
    }
}
