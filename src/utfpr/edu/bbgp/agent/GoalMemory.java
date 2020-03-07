/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.agent;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import net.sf.tweety.arg.aspic.syntax.AspicArgument;
import net.sf.tweety.arg.dung.semantics.Extension;
import net.sf.tweety.arg.dung.syntax.Argument;
import net.sf.tweety.arg.dung.syntax.DungTheory;
import net.sf.tweety.logics.fol.syntax.FolFormula;

/**
 *
 * @author henri
 */
public class GoalMemory {
    private final Long agentCycle;
    private final Goal goal;
    private final Argument support;
    private final Collection<Argument> relevantArguments;
    private final HashMap<Argument, Boolean> acceptedArguments;

    public GoalMemory(Long agentCycle, Goal goal, Argument support, DungTheory argFramework, Extension selectedExt) {
        this.agentCycle = agentCycle;
        this.goal = (Goal) goal.clone();
        this.support = support;
        this.acceptedArguments = new HashMap<>();
        this.relevantArguments = getRelevantArguments(support, argFramework, selectedExt);
    }

    @Override
    public String toString() {
        String ret = "Goal " + goal.getFullPredicate() + " changed to state " + goal.getStage().name() + " at cycle " + agentCycle + " because [" + support + "] with the following relevant arguments:";
        
        for(Argument a : relevantArguments){
            ret = ret.concat("\n\t[" + a + "] " + (acceptedArguments.get(a)?"accepted":"rejected"));
        }
        
        return ret;
    }
    
    protected Set<Argument> getRelevantArguments(Argument target, DungTheory argFramework, Extension selectedExt){
        HashSet<Argument> set = new HashSet<>();
        
        if(target == null){
            return set;
        }
        
        set.add(target);
        acceptedArguments.put(target, Boolean.TRUE);
        
        for(Argument a : ((AspicArgument<FolFormula>)target).getAllSubs()){
            set.add(a);
            acceptedArguments.put(a, Boolean.TRUE);
        }
        
        
        if(argFramework == null){
            return set;
        }
        
        boolean changed;
        HashSet<Argument> queue = set;
        
        do{
            changed = false;
            HashSet<Argument> auxSet = new HashSet<>();
            for(Argument inSet : queue){
                for(Argument attacker : argFramework.getAttacked(inSet)){
                    AspicArgument<FolFormula> argAtt = (AspicArgument<FolFormula>) attacker;
                    if(!set.contains(attacker)){
                        auxSet.add(attacker);
                        acceptedArguments.put(attacker, selectedExt.contains(attacker));
                        
                        auxSet.addAll(argAtt.getAllSubs());
                        for(Argument a : argAtt.getAllSubs()){
                            set.add(a);
                            acceptedArguments.put(a, selectedExt.contains(a));
                        }
                    }
                }
            }
            
            changed = changed || set.addAll(auxSet);
            queue = auxSet;
        }while(changed);
        
        return set;
    }
}
