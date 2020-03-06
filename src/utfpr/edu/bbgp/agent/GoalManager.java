/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.agent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import net.sf.tweety.logics.commons.syntax.Constant;
import net.sf.tweety.logics.commons.syntax.Predicate;
import net.sf.tweety.logics.fol.syntax.FolAtom;

/**
 *
 * @author henri
 */
public class GoalManager{
    private final Agent agent;
    private final HashMap<Predicate, SleepingGoal> sleepingGoals;
    private final HashSet<Goal> goals;

    public GoalManager(Agent agent) {
        this.agent = agent;
        sleepingGoals = new HashMap<>();
        goals = new HashSet<>();
    }
    
    public boolean addSleepingGoal(SleepingGoal sGoal){
        if(sleepingGoals.containsKey(sGoal.getGoalPredicate().getPredicate())){
            return true;
        }
        
        return sleepingGoals.put(sGoal.getGoalPredicate().getPredicate(), sGoal) == null;
    }
    
    public boolean addGoal(Goal goal){
        return goals.add(goal);
    }
    
    public Goal createGoal(Predicate pred, FolAtom fullPredicate){
        SleepingGoal sG = sleepingGoals.get(pred);
        
        if(sG == null || fullPredicate == null)
            return null;
        
        if(!fullPredicate.getName().equals(pred.getName()))
            return null;
        
        if(fullPredicate.getUnboundVariables().size() > 1){
            return null;
        }
        
        if(!fullPredicate.getUnboundVariables().isEmpty()){
            if(!fullPredicate.getUnboundVariables().iterator().next().getSort().getName().equals(Agent.GOAL_SORT_TEXT))
                return null;
        }
        
        Goal g = new Goal(agent, sG, fullPredicate, GoalStage.Active);
        boolean isNewGoal = goals.add(g);
        
        if(isNewGoal){
            Constant goalTerm = agent.getNextGoalConstant();
            
            g.setGoalTerm(goalTerm);
        }else{
//            for(Goal goal : goals){
//                if(g.equals(goal)){
//                    return goal;
//                }
//            }
            g = null;
        }
        
        return g;
    }
    
    public Set<Goal> getGoalByStage(GoalStage stage){
        HashSet<Goal> filteredGoals = new HashSet<>();
        
        for(Goal g : goals){
            if(g.getStage() == stage){
                filteredGoals.add(g);
            }
        }
        
        return filteredGoals;
    }

    public Set<Goal> getGoals() {
        return goals;
    }
    
    public SleepingGoal getGoalBase(Predicate pred){
        return sleepingGoals.get(pred);
    }

    boolean contaisSleepingGoal(SleepingGoal goal) {
        return sleepingGoals.containsValue(goal);
    }
}
