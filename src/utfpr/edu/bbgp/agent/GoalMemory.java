package utfpr.edu.bbgp.agent;

import utfpr.edu.argumentation.diagram.ArgumentionFramework;
import utfpr.edu.argumentation.diagram.Atom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import net.sf.tweety.arg.aspic.syntax.AspicArgument;
import net.sf.tweety.arg.aspic.syntax.StrictInferenceRule;
import net.sf.tweety.arg.dung.semantics.Extension;
import net.sf.tweety.arg.dung.syntax.Argument;
import net.sf.tweety.arg.dung.syntax.DungTheory;
import net.sf.tweety.logics.commons.syntax.interfaces.Term;
import net.sf.tweety.logics.fol.syntax.FolAtom;
import net.sf.tweety.logics.fol.syntax.FolFormula;
import net.sf.tweety.logics.fol.syntax.Negation;
import utfpr.edu.bbgp.extended.StrictInferenceRuleWithId;
import utfpr.edu.bbgp.simul.utils.Tuple;

/**
 *
 * @author henri
 */
public class GoalMemory {

    private final Long agentCycle;
    private final Goal goal;
    private boolean success;
    private final List<Argument> support;
    private final Collection<Argument> relevantArguments;
    private final HashMap<Argument, Boolean> acceptedArguments;
    private final Agent agent;
    private final DungTheory completeAF;
    
    private final HashSet<Tuple<Argument, Argument>> attackSet;

    public GoalMemory(Agent agent, Long agentCycle, Goal goal, boolean testAcceptance, List<Argument> support, DungTheory argFramework, Extension selectedExt) {
        this.agent = agent;
        this.agentCycle = agentCycle;
        this.goal = (Goal) goal.clone();
        this.completeAF = argFramework;

        this.success = false;
        if (testAcceptance) {
            boolean contains = false;
            if (support != null) {
                for (Argument a : support) {
                    if (selectedExt.contains(a)) {
                        contains = true;
                        break;
                    }
                }
            }

            if (goal.getStage() == GoalStage.Pursuable) {
                this.success = !contains;
            } else {
                this.success = contains;
            }
        }
        
        if(goal.getStage() == GoalStage.Completed) this.success = true;

        this.support = support;
        this.acceptedArguments = new HashMap<>();
        this.attackSet = new HashSet<>();
        this.relevantArguments = getRelevantArguments(support, argFramework, selectedExt);
    }

    @Override
    public String toString() {
        String ret = (agentCycle < 1000 ? "000".substring(3 - agentCycle.toString().length()) : "").concat(agentCycle.toString());
        ret = ret.concat(" : ").concat(goal.getFullPredicate().toString()).concat((this.success ? " became " : " did not became ")).concat(goal.getStage().name());

        return ret;
    }

    public String toStringExtended() {
        String ret = "Goal " + goal.getFullPredicate() + (this.success ? "" : " did not ") + " changed to state " + goal.getStage().name() + " at cycle " + agentCycle + " because <" + support + "> with the following relevant arguments:";

        for (Argument a : relevantArguments) {
            ret = ret.concat("\n\t[" + a + "] " + (acceptedArguments.get(a) ? "accepted" : "rejected"));
        }

        return ret;
    }
    
    public String getGoalFullPredicate(){
        return goal.getFullPredicate().toString();
    }
    
    public Long getCycle(){
        return agentCycle;
    }
    
    public String explain(boolean complete){
        if(this.goal.getStage() != GoalStage.Choosen){
            return toString();
        }
        
        String composition = String.format("Cycle: %03d", this.agentCycle) + (complete ? "" : " (partial for " + goal.getFullPredicate().toString() + ")") + "\n      » ";
        
        Collection<Argument> target = (complete ? completeAF : relevantArguments);
        
        for(Argument arg : target){
            if(arg instanceof AspicArgument){
                AspicArgument<FolFormula> aArg = (AspicArgument<FolFormula>) arg;
                
                if(!(((AspicArgument) arg).getTopRule() instanceof StrictInferenceRuleWithId)) continue;
                
                StrictInferenceRuleWithId topRule = (StrictInferenceRuleWithId) aArg.getTopRule();
                String pattern;
                String xId = "";
                String yId = "";
                String conflict = "";
                Goal g1, g2;
                
                for(AspicArgument<FolFormula> subA : aArg.getDirectSubs()) {
                    FolFormula conclusion = subA.getConclusion();
                    if(conclusion instanceof Negation) conclusion = ((Negation)conclusion).getFormula();

                    if(conclusion instanceof FolAtom){
                        if(((FolAtom)conclusion).getPredicate().getName().equals(Agent.INCOMPATIBLE_STR)){
                            Iterator<Term<?>> iterator = ((FolAtom)conclusion).getArguments().iterator();
                            xId = (String) iterator.next().get();
                            yId = (String) iterator.next().get();
                            conflict = (String) iterator.next().get();
                        } else if(((FolAtom)conclusion).getPredicate().getName().equals(Agent.MAX_UTIL_STR)){
                            Iterator<Term<?>> iterator = ((FolAtom)conclusion).getArguments().iterator();
                            xId = (String) iterator.next().get();
                        }  else if(((FolAtom)conclusion).getPredicate().getName().equals(Agent.NOT_HAS_INCOMPATIBILITY_STR)){
                            Iterator<Term<?>> iterator = ((FolAtom)conclusion).getArguments().iterator();
                            xId = (String) iterator.next().get();
                        } 
                    }
                }
                
                if(!composition.isBlank() && !composition.endsWith("\n      » ") && topRule.getRuleId().startsWith("R_de")){
                    composition = composition.concat("\n      » ");
                }
                
                switch(topRule.getRuleId()){
                    case "R_de^001":
                        pattern = "<name_x> has no incompatibility, so it became pursued.";
                        g1 = agent.getGoalForID(xId);
                        composition = composition.concat(pattern.replaceAll("<name_x>", g1.getFullPredicate().toString()));
                        break;
                    case "R_de^002":
                        pattern = "<name_x> and <name_y> have the following conflicts: <conflict>. Since <name_x> is more preferable than <name_y>, <name_x> became pursued.";
                        g1 = agent.getGoalForID(xId);
                        g2 = agent.getGoalForID(yId);
                        composition = composition.concat(pattern.replaceAll("<name_x>", g1.getFullPredicate().toString()).replaceAll("<name_y>", g2.getFullPredicate().toString()).replaceAll("<conflict>", conflict));
                        break;
                    case "R_de^003":
                        pattern = "<name_x> and <name_y> have the following conflicts: <conflict>. Since <name_y> is less preferable than <name_x>, <name_y> did not become pursued.";
                        g1 = agent.getGoalForID(xId);
                        g2 = agent.getGoalForID(yId);
                        composition = composition.concat(pattern.replaceAll("<name_x>", g1.getFullPredicate().toString()).replaceAll("<name_y>", g2.getFullPredicate().toString()).replaceAll("<conflict>", conflict));
                        break;
                    case "R_de^004":
                        pattern = "<name_x> and <name_y> have the following conflicts: <conflict>. Since <name_x> and <name_y> have the same preference value, <name_x> became pursued.";
                        g1 = agent.getGoalForID(xId);
                        g2 = agent.getGoalForID(yId);
                        composition = composition.concat(pattern.replaceAll("<name_x>", g1.getFullPredicate().toString()).replaceAll("<name_y>", g2.getFullPredicate().toString()).replaceAll("<conflict>", conflict));
                        break;
                    case "R_de^005":
                        pattern = "Since <name_x> belonged to the set of goals that maximize the utility, it became pursued.";
                        g1 = agent.getGoalForID(xId);
                        composition = composition.concat(pattern.replaceAll("<name_x>", g1.getFullPredicate().toString()));
                        break;
                    case "R_de^006":
                        pattern = "Since <name_x> did not belong to the set of goals that maximizes the utility, it did not become pursued.";
                        g1 = agent.getGoalForID(xId);
                        composition = composition.concat(pattern.replaceAll("<name_x>", g1.getFullPredicate().toString()));
                        break;
                }
            }
        }
        
        return composition;
    }

    protected Set<Argument> getRelevantArguments(List<Argument> target, DungTheory argFramework, Extension selectedExt) {
        HashSet<Argument> set = new HashSet<>();
        

        if (argFramework == null && target == null && selectedExt == null) {
            return set;
        }
        
        for(Argument a : argFramework){
            if(a instanceof AspicArgument){
                AspicArgument<FolFormula> a2 = (AspicArgument) a;
                
                if(a2.getConclusion().isGround()){
                    for(Term t : a2.getConclusion().getTerms()){
                        if(t.equals(goal.getGoalTerm())){
                            set.add(a);
                            acceptedArguments.put(a, selectedExt.contains(a));
                        }
                    }
                }
            }
        }

        if(target != null){
            set.addAll(target);
            for (Argument t : target) {
                acceptedArguments.put(t, selectedExt.contains(t));

                for (Argument a : ((AspicArgument<FolFormula>) t).getAllSubs()) {
                    set.add(a);
                    acceptedArguments.put(a, selectedExt.contains(a));
                }
            }
        }

        if (argFramework == null) {
            return set;
        }

        boolean changed;
        HashSet<Argument> queue = set;

        do {
            changed = false;
            HashSet<Argument> auxSet = new HashSet<>();
            for (Argument inSet : queue) {
                AspicArgument<FolFormula> argInSet = (AspicArgument<FolFormula>) inSet;
                for (Argument attacker : argFramework.getAttacked(inSet)) {
                    AspicArgument<FolFormula> argAtt = (AspicArgument<FolFormula>) attacker;
                    
                    if(argAtt.getConclusion().equals((FolFormula)argInSet.getConclusion().complement())){
                        attackSet.add(new Tuple<>(inSet, attacker));
                    }
                    
                    if (!set.contains(attacker)) {
                        auxSet.add(attacker);
                        acceptedArguments.put(attacker, selectedExt.contains(attacker));

                        for (Argument a : argAtt.getAllSubs()) {
                            auxSet.add(a);
                            acceptedArguments.put(a, selectedExt.contains(a));
                        }
                    }
                }
            }

            changed = changed || set.addAll(auxSet);
            queue = auxSet;
        } while (changed);

        return set;
    }

    private HashMap<Object, JComponent> elementsToComponentMap = new HashMap<>();
    public void showInCluster(ArgumentionFramework cluster) {
        if(cluster == null) return;
        
        cluster.clear();
        
        elementsToComponentMap.clear();
        
        for(Argument arg : relevantArguments){
            boolean skip = false;
            
            AspicArgument<FolFormula> argumentFol = (AspicArgument<FolFormula>) arg;
            
            for(Argument arg2 : relevantArguments){
                AspicArgument<FolFormula> argumentFol2 = (AspicArgument<FolFormula>) arg2;
                
                if(argumentFol.isSubArgumentOf(argumentFol2) && argumentFol != argumentFol2){
                    skip = true;
                    break;
                }
            }
            
            if(skip) continue;
            
            boolean isFocus = false;
            if(support != null){
                isFocus = support.contains(arg);
            }
            
            utfpr.edu.argumentation.diagram.Argument argument = showArgumentInCluster(argumentFol, cluster, isFocus, acceptedArguments.get(arg));
            elementsToComponentMap.put(arg, argument);
            cluster.addArgument(argument);
        }
        
        for(Tuple<Argument, Argument> attck : attackSet){
            JComponent attacker = elementsToComponentMap.get(attck.getT());
            JComponent attacked = elementsToComponentMap.get(attck.getU());
            
            if(attacker == null) continue;
            if(attacked == null) continue;
            
            cluster.addAttack((utfpr.edu.argumentation.diagram.Argument)attacker, (utfpr.edu.argumentation.diagram.Argument)attacked, false);
        }
        
        cluster.repositionComponents();
        cluster.revalidate();
        cluster.repaint();
    }

    protected utfpr.edu.argumentation.diagram.Argument showArgumentInCluster(AspicArgument<FolFormula> arg, ArgumentionFramework cluster, Boolean isFocus, Boolean accepted) {
        int type = (accepted? 0 : 2) + (isFocus ? 1 : 0);
        boolean isStrict = arg.getTopRule() instanceof StrictInferenceRule;
        String label = agent.getArgumentConclusionID(arg);
        String tooltip = arg.getConclusion().toString();
        String ruleTooltip = arg.getTopRule().toString();
        
        Atom conclusion = new Atom(label, tooltip, isStrict, type, cluster);
        
        ArrayList<utfpr.edu.argumentation.diagram.Argument> subArgs = new ArrayList<>();
        for(AspicArgument<FolFormula> subA : arg.getDirectSubs()){
            utfpr.edu.argumentation.diagram.Argument subArg = showArgumentInCluster(subA, cluster, isFocus, acceptedArguments.get(subA));
            elementsToComponentMap.put(subA, subArg);
            subArgs.add(subArg);
        }
        return new utfpr.edu.argumentation.diagram.Argument(conclusion, type, cluster, agent.getArgumentID(arg), agent.getRuleID(arg), ruleTooltip, agent.isRuleStrict(arg), subArgs.toArray(new utfpr.edu.argumentation.diagram.Argument[]{}));
    }

    public GoalStage getGoalStage() {
        return goal.getStage();
    }
}
