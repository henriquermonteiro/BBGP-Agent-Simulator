/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.agent;

import java.util.HashSet;
import java.util.Set;
import net.sf.tweety.logics.fol.syntax.FolFormula;

/**
 *
 * @author henri
 */
public class Intention {
    private Agent agent;
    // plan execution "pointer".
    private Object currentStep;
    // mapping
    private Set<FolFormula> unifiedContext;
    // Goal
    private final Goal goal;
    // Plan
    private final Plan plan;

    public Intention(Agent agent, Goal goal, Plan plan, Set<FolFormula> unification) {
        if(!goal.getGoalBase().equals(plan.getGoal())){
            throw new IllegalArgumentException("The plan incompatible with goal: The goal of the plan must be the same as the base goal.");
        }
        
        this.agent = agent;
        this.goal = goal;
        this.plan = plan;
        
        for(FolFormula formula : unification){
            if(!formula.isGround()){
                throw new IllegalArgumentException("All formulas must be grounded.");
            }
        }
        
        this.unifiedContext = new HashSet<>(unification);
        
        this.currentStep = plan.getActions(); // TODO: get root action
    }

    public Goal getGoal() {
        return goal;
    }

    public Plan getPlan() {
        return plan;
    }
    
    public boolean executeNextStep(){
        System.out.println("Executing action ...");
        
        agent.completeIntention(this);
        
        return true;
    }
}
