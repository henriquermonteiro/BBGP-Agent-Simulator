/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.agent;

import java.util.Objects;
import net.sf.tweety.logics.commons.syntax.Constant;
import net.sf.tweety.logics.commons.syntax.interfaces.Term;
import net.sf.tweety.logics.fol.syntax.FolAtom;
import utfpr.edu.bbgp.extended.GoalFolAtom;

/**
 *
 * @author henri
 */
public class Goal implements Comparable<Goal>{
    private final Agent agent;
    private final SleepingGoal goalBase;
    private GoalFolAtom goalPredicateUnified;
    private GoalStage stage;
    private Constant goalTerm;
    private int sugestedPlanIndex;

    public Goal(Agent agent, SleepingGoal goalBase, FolAtom fullPredicate, GoalStage stage) {
        this.agent = agent;
        this.goalBase = goalBase;
        this.stage = stage;
        
        if(!fullPredicate.getName().equals(goalBase.getGoalPredicate().getName())){
            throw new IllegalArgumentException("The fullPredicate must be the same as the goalBase predicate.");
        }
        
        
        int goals = 0;
        for(Term t : fullPredicate.getTerms()){
            if(t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)){
                goals++;
            }else{
                if(! (t instanceof Constant)){
                    throw new IllegalArgumentException("The fullPredicate must have all terms unified. Only a single term os the Sort \"Goals\" can be unbound.");
                }
            }

            if(goals > 1){
                throw new IllegalArgumentException("There can be only a single term of the Sort \"Goals\".");
            }
        }
        
        this.goalPredicateUnified = new GoalFolAtom(fullPredicate);
    }

    public SleepingGoal getGoalBase() {
        return goalBase;
    }

    public FolAtom getFullPredicate() {
        return goalPredicateUnified;
    }

    public Constant getGoalTerm() {
        return goalTerm;
    }

    public void setGoalTerm(Constant goalTerm) {
        if(goalTerm == null){
            throw new NullPointerException("Goal term can not be set to null.");
        }
        
        if(this.goalTerm != null){
            throw new IllegalStateException("The goal term can be set only once.");
        }
        
        for(Constant c : goalPredicateUnified.getTerms(Constant.class)){
            if(c.getSort().getName().equals(Agent.GOAL_SORT_TEXT)){
                goalPredicateUnified = new GoalFolAtom( (FolAtom) goalPredicateUnified.exchange(c, goalTerm));
                
                this.goalTerm = goalTerm;
            }
        }
    }

    public GoalStage getStage() {
        return stage;
    }

    public void setStage(GoalStage stage) {
        this.stage = stage;
    }

    @Override
    public Goal clone(){
        Goal clone = new Goal(agent, goalBase, goalPredicateUnified.clone(), stage);
        return clone;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.agent);
        hash = 97 * hash + Objects.hashCode(this.goalBase);
        hash = 97 * hash + Objects.hashCode(this.goalPredicateUnified);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Goal other = (Goal) obj;
        if (!Objects.equals(this.agent, other.agent)) {
            return false;
        }
        if (!this.goalBase.getGoalPredicate().getName().equals(other.goalBase.getGoalPredicate().getName())) {
            return false;
        }
        if (!Objects.equals(this.goalPredicateUnified, other.goalPredicateUnified)) {
            return false;
        }
        
        if(this.stage == GoalStage.Cancelled || other.stage == GoalStage.Cancelled){
            return false;
        }
        
        if(this.stage == GoalStage.Completed || other.stage == GoalStage.Completed){
            return false;
        }
        
        return true;
    }

    @Override
    public int compareTo(Goal arg0) {
        return (int) Math.round((this.goalBase.getPreference() - arg0.goalBase.getPreference()) * 1000);
    }

    public void setSugestedPlanIndex(int index) {
        this.sugestedPlanIndex = index;
    }

    public int getSugestedPlanIndex() {
        return sugestedPlanIndex;
    }
}
