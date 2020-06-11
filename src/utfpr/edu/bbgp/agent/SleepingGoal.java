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
        
        for(Term t : goalPredicate.getTerms()){
            if(t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)){
                goalTerm++;
            }
        }
        
        if(goalTerm != 1){
            throw new IllegalArgumentException("The predicate used for goals must have one and only one \"Goals\" Term.");
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
