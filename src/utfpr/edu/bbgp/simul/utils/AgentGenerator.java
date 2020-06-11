package utfpr.edu.bbgp.simul.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import net.sf.tweety.arg.aspic.ruleformulagenerator.FolFormulaGenerator;
import net.sf.tweety.arg.aspic.syntax.AspicArgument;
import net.sf.tweety.arg.aspic.syntax.AspicArgumentationTheory;
import net.sf.tweety.arg.aspic.syntax.InferenceRule;
import net.sf.tweety.logics.commons.syntax.interfaces.Term;
import net.sf.tweety.logics.fol.parser.FolParser;
import net.sf.tweety.logics.fol.syntax.FolAtom;
import net.sf.tweety.logics.fol.syntax.FolFormula;
import net.sf.tweety.logics.fol.syntax.FolSignature;
import net.sf.tweety.logics.fol.syntax.Negation;
import utfpr.edu.bbgp.agent.Agent;
import utfpr.edu.bbgp.agent.PerceptionEntry;
import utfpr.edu.bbgp.agent.Plan;
import utfpr.edu.bbgp.agent.SleepingGoal;
import utfpr.edu.bbgp.extended.AspicArgumentationTheoryFol;
import utfpr.edu.bbgp.agent.parser.AspicFolParser;

/**
 *
 * @author henri
 */
public class AgentGenerator {

    protected static boolean hasAGoalTerm(FolFormula formula) {
        for (Term t : formula.getTerms()) {
            if (t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)) {
                return true;
            }
        }

        return false;
    }

    public static Agent getAgentFromFolBase(Reader agentBeliefAndRuleSet) throws IOException {
        AspicFolParser aspicParser = new AspicFolParser(new FolParser(), new FolFormulaGenerator());
        
        FolSignature signature = aspicParser.getFolParser().getSignature();

        AspicArgumentationTheory<FolFormula> theory = aspicParser.parseBeliefBase(agentBeliefAndRuleSet);

        AspicArgumentationTheoryFol beliefs = new AspicArgumentationTheoryFol(new FolFormulaGenerator());
        AspicArgumentationTheoryFol stdRules = new AspicArgumentationTheoryFol(new FolFormulaGenerator());
        AspicArgumentationTheoryFol actRules = new AspicArgumentationTheoryFol(new FolFormulaGenerator());
        AspicArgumentationTheoryFol evlRules = new AspicArgumentationTheoryFol(new FolFormulaGenerator());

        HashMap<AspicArgument, SleepingGoal> sleepingGoals = new HashMap<>();
        HashMap<FolFormula, SleepingGoal> formulaTosleepingGoalsMap = new HashMap<>();

        for (InferenceRule<FolFormula> rule : theory) {
            if (rule.getPremise().isEmpty()) {
                beliefs.add(rule);
                continue;
            }

            FolFormula conclusion = rule.getConclusion();
            if (!hasAGoalTerm(conclusion)) {
                stdRules.add(rule);
            } else {
                if (conclusion instanceof Negation) {
                    evlRules.add(rule);
                } else {
                    actRules.add(rule);

                    AspicArgument<FolFormula> arg = new AspicArgument<>(rule);
                    SleepingGoal g = new SleepingGoal((FolAtom) conclusion);
                    sleepingGoals.put(arg, g);
                    formulaTosleepingGoalsMap.put(conclusion, g);
                }
            }
        }

        Agent a = new Agent(signature, beliefs, stdRules, actRules, evlRules);

        if (theory.getOrder() != null) {
            AspicArgument<FolFormula>[] toOrder = sleepingGoals.keySet().toArray(new AspicArgument[]{});
            Arrays.sort(toOrder, theory.getOrder());

            Double part = 1.0 / toOrder.length;

            for (int k = 0; k < toOrder.length; k++) {
                double pref = (k + 1) * part;

                SleepingGoal g = sleepingGoals.get(toOrder[k]);
                g.setPreference(pref);

                a.addSleepingGoal(g);
            }
        } else {
            for (SleepingGoal g : sleepingGoals.values()) {
                a.addSleepingGoal(g);
            }
        }
        
        if(theory instanceof AspicArgumentationTheoryFol){
            AspicArgumentationTheoryFol theoryFol = (AspicArgumentationTheoryFol) theory;
            
            for(Quadruplet<FolFormula, HashSet<FolFormula>, HashMap<String, Double>, HashSet<PerceptionEntry>> t : theoryFol.getPlanTemplates()){
                if(formulaTosleepingGoalsMap.containsKey(t.getT())){
                    a.addPlanTemplate(new Plan(formulaTosleepingGoalsMap.get(t.getT()), null, t.getU(), t.getV(), t.getW()));
                }
            }
            
            for(String res : theoryFol.getStartingResources().keySet()){
                a.addResource(res, theoryFol.getStartingResources().get(res));
            }
            
        }
        

        return a;
    }

    public static void main(String... args) throws FileNotFoundException, IOException {
        Agent a = getAgentFromFolBase(new FileReader("modelA.bbgpagent"));

        a.singleCycle();

        System.out.println("");
    }
}
