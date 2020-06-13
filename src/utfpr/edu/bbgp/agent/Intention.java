/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.agent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.sf.tweety.logics.commons.syntax.Variable;
import net.sf.tweety.logics.commons.syntax.interfaces.Term;
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
    // mapping
    private Map<Variable, Term<?>> auxiliaryMapping;
    // Goal
    private final Goal goal;
    // Plan
    private final Plan plan;

    public Intention(Agent agent, Goal goal, Plan plan, Map<Variable, Term<?>> auxiliaryMapping) {
        if(!goal.getGoalBase().equals(plan.getGoal())){
            throw new IllegalArgumentException("The plan incompatible with goal: The goal of the plan must be the same as the base goal.");
        }
        
        this.agent = agent;
        this.goal = goal;
        this.plan = plan;
        this.auxiliaryMapping = auxiliaryMapping;
        
        Set<FolFormula> unification = plan.getUnifiedSet(goal.getFullPredicate());
        
        this.unifiedContext = new HashSet<>(unification.size());
        
        for(FolFormula formula : unification){
            FolFormula unified = (FolFormula) formula.substitute(auxiliaryMapping);
            
            if(!unified.isGround()){
                throw new IllegalArgumentException("All formulas must be grounded.");
            }
            
            unifiedContext.add(unified);
        }
        
        this.currentStep = plan.getActions();
    }

    public Goal getGoal() {
        return goal;
    }

    public Plan getPlan() {
        return plan;
    }

    public Set<FolFormula> getUnifiedContext() {
        return unifiedContext;
    }
    
    public boolean executeNextStep(){
        System.out.println("Executing action ...");
        
        agent.completeIntention(this);
        
        return true;
    }

    public Map<Variable, Term<?>> getAuxiliaryMapping() {
        return auxiliaryMapping;
    }
}
