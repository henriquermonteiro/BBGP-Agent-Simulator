/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.agent;

import java.util.Objects;
import net.sf.tweety.logics.commons.syntax.interfaces.Term;
import net.sf.tweety.logics.fol.syntax.FolAtom;

/**
 *
 * @author henri
 */
public class SleepingGoal {
    // Predicate of the goal
    private final FolAtom goalPredicate;
    private double preference;

    public SleepingGoal(FolAtom goalPredicate) throws IllegalArgumentException{
        this.preference = 0.0;
        
        int goalTerm = 0;
        int otherTerm = 0;
        
        for(Term t : goalPredicate.getTerms()){
            if(t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)){
                goalTerm++;
            }else{
                otherTerm++;
            }
        }
        
        if(goalTerm != 1 || otherTerm < 1){
            throw new IllegalArgumentException("The predicate used for goals must have one and only one \"Goals\" Term and some (one or more) other Terms.");
        }
        
        this.goalPredicate = goalPredicate;
    }

    public FolAtom getGoalPredicate() {
        return goalPredicate;
    }

    public double getPreference() {
        return preference;
    }

    public void setPreference(double preference) {
        this.preference = preference;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj != null){
            if(obj.getClass().equals(this.getClass())){
                return (this.goalPredicate.getName().equals(((SleepingGoal) obj).goalPredicate.getName()));
            }
        }
        
        return false;
    }
    
    public boolean instanceOf(Goal g){
        return this.equals(g.getGoalBase());
    }

    @Override
    public String toString() {
        return goalPredicate.toString();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.goalPredicate);
        return hash;
    }
}
