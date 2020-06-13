package utfpr.edu.bbgp.agent.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import net.sf.tweety.logics.commons.syntax.Constant;
import net.sf.tweety.logics.commons.syntax.Predicate;
import net.sf.tweety.logics.fol.syntax.FolAtom;
import utfpr.edu.bbgp.agent.Agent;
import utfpr.edu.bbgp.agent.Goal;
import utfpr.edu.bbgp.agent.GoalStage;
import utfpr.edu.bbgp.agent.SleepingGoal;

/**
 *
 * @author henri
 */
public class GoalManager{
    private final Agent agent;
    private final HashMap<Predicate, SleepingGoal> sleepingGoals;
    private final HashSet<Goal> goals;
    private final HashMap<String, Goal> idToGoalMap;

    public GoalManager(Agent agent) {
        this.agent = agent;
        sleepingGoals = new HashMap<>();
        goals = new HashSet<>();
        idToGoalMap = new HashMap<>();
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
    
    public Goal getGoalbyId(String id){
        return idToGoalMap.get(id);
    }
    
    public Goal createGoal(Predicate pred, FolAtom fullPredicate){
        return this.createGoal(pred, fullPredicate, true);
    }
    
    public Goal createGoal(Predicate pred, FolAtom fullPredicate, boolean createIdentifier){
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
        if(createIdentifier){
            boolean isNewGoal = goals.add(g);

            if(isNewGoal){
                Constant goalTerm = agent.getNextGoalConstant();
                
                idToGoalMap.put(goalTerm.get(), g);

                g.setGoalTerm(goalTerm);
            }else{
                g = null;
            }
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
    
    public Set<Goal> getGoalAtLeastAtStage(GoalStage stage){
        HashSet<Goal> filteredGoals = new HashSet<>();
        
        filteredGoals.addAll(getGoalByStage(stage));
        
        switch(stage){
            case Active:
                filteredGoals.addAll(getGoalAtLeastAtStage(GoalStage.Pursuable));
                return filteredGoals;
            case Pursuable:
                filteredGoals.addAll(getGoalAtLeastAtStage(GoalStage.Choosen));
                return filteredGoals;
            case Choosen:
                filteredGoals.addAll(getGoalAtLeastAtStage(GoalStage.Executive));
                return filteredGoals;
        }
        
        return filteredGoals;
    }

    public Set<Goal> getGoals() {
        return goals;
    }
    
    public SleepingGoal getGoalBase(Predicate pred){
        return sleepingGoals.get(pred);
    }
    
    public Collection<SleepingGoal> getSleepingGoal() {
        return sleepingGoals.values();
    }

    public boolean contaisSleepingGoal(SleepingGoal goal) {
        return sleepingGoals.containsValue(goal);
    }
}
