package utfpr.edu.bbgp.agent;

import utfpr.edu.argumentation.diagram.ArgumentionFramework;
import utfpr.edu.argumentation.diagram.Atom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import net.sf.tweety.arg.aspic.syntax.AspicArgument;
import net.sf.tweety.arg.aspic.syntax.InferenceRule;
import net.sf.tweety.arg.aspic.syntax.StrictInferenceRule;
import net.sf.tweety.arg.dung.semantics.Extension;
import net.sf.tweety.arg.dung.syntax.Argument;
import net.sf.tweety.arg.dung.syntax.DungTheory;
import net.sf.tweety.logics.commons.syntax.interfaces.Term;
import net.sf.tweety.logics.fol.syntax.FolAtom;
import net.sf.tweety.logics.fol.syntax.FolFormula;
import net.sf.tweety.logics.fol.syntax.Negation;
import utfpr.edu.bbgp.extended.RuleWithIdInterface;
import utfpr.edu.bbgp.simul.utils.FolFormulaUtils;
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

        if (agentCycle == null) {
            throw new NullPointerException("Agent cycle cannot be null.");
        }

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

        if (goal.getStage() == GoalStage.Completed) {
            this.success = true;
        }

        this.support = support;
        this.acceptedArguments = new HashMap<>();
        this.attackSet = new HashSet<>();
        this.relevantArguments = getRelevantArguments(support, argFramework, selectedExt);
    }

    @Override
    public String toString() {
        String ret = (agentCycle < 1000 ? "000".substring(3 - agentCycle.toString().length()) : "").concat(agentCycle.toString());
        ret = ret.concat(" : ").concat(toStringSimplified());

        return ret;
    }

    public String toStringSimplified() {
        return goal.getFullPredicate().toString().concat((this.success ? " became " : " did not became ")).concat(goal.getStage().name());
    }

    public String toStringExtended() {
        String ret = "Goal " + goal.getFullPredicate() + (this.success ? "" : " did not ") + " changed to state " + goal.getStage().name() + " at cycle " + agentCycle + " because <" + support + "> with the following relevant arguments:";

        for (Argument a : relevantArguments) {
            ret = ret.concat("\n\t[" + a + "] " + (acceptedArguments.get(a) ? "accepted" : "rejected"));
        }

        return ret;
    }

    public String getGoalFullPredicate() {
        return goal.getFullPredicate().toString();
    }

    public Long getCycle() {
        return agentCycle;
    }

    protected void recursiveAddArgument(Deque<AspicArgument> stack, AspicArgument argument) {
        if (stack != null && argument != null) {
            String topRuleStr = argument.getTopRule().toString();

            boolean add = true;
            for (AspicArgument stackElm : stack) {
                if (stackElm.getTopRule().toString().equals(topRuleStr)) {
                    add = false;
                    break;
                }
            }

            if (add) {
                stack.push(argument);
            }

            if (argument instanceof AspicArgument) {
                AspicArgument<FolFormula> aspicArgument = (AspicArgument) argument;

                for (AspicArgument subArg : aspicArgument.getDirectSubs()) {
                    recursiveAddArgument(stack, subArg);
                }
            }
        }
    }

    protected Collection<AspicArgument> getArgumentsStackForExplaining(boolean complete) {
        Deque<AspicArgument> stack = new LinkedList<>();
        Queue<AspicArgument> queueOfAttackers = new LinkedList<>();
        Queue<AspicArgument> subQueueOfAttackers = new LinkedList<>();

        Collection<Argument> target1 = (support != null ? support : relevantArguments);
//        Collection<Argument> target1 = relevantArguments;

        for (Argument arg : target1) {
            recursiveAddArgument(stack, (AspicArgument) arg);

            Deque<AspicArgument> argQueue = new LinkedList<>();
            recursiveAddArgument(argQueue, (AspicArgument) arg);

            while (!argQueue.isEmpty()) {
                Argument fromQueue = argQueue.pop();
                HashSet<Argument> neighbours = new HashSet<>(completeAF.getAttackers(fromQueue));
                neighbours.addAll(completeAF.getAttacked(fromQueue));
//                for (Argument attacker : completeAF.getAttackers(fromQueue)) {
                for (Argument attacker : neighbours) {
                    if (!queueOfAttackers.contains(attacker)) {
                        queueOfAttackers.offer((AspicArgument) attacker);
                    }
                }
            }
        }

        while (!queueOfAttackers.isEmpty()) {
            Argument arg = queueOfAttackers.poll();
            recursiveAddArgument(stack, (AspicArgument) arg);

            HashSet<Argument> neighbours = new HashSet<>(completeAF.getAttackers(arg));
            neighbours.addAll(completeAF.getAttacked(arg));
//            for (Argument attacker : completeAF.getAttackers(arg)) {
            for (Argument attacker : neighbours) {
                if (!stack.contains(attacker)) {
                    if (!subQueueOfAttackers.contains(attacker)) {
                        subQueueOfAttackers.offer((AspicArgument) attacker);
                    }
                }
            }

            if (queueOfAttackers.isEmpty()) {
                queueOfAttackers = subQueueOfAttackers;
            }
        }

        if (complete) {
            for (Argument arg : completeAF) {
                if (stack.contains(arg)) {
                    continue;
                }

                recursiveAddArgument(stack, (AspicArgument) arg);

                HashSet<Argument> neighbours = new HashSet<>(completeAF.getAttackers(arg));
                neighbours.addAll(completeAF.getAttacked(arg));
//                for (Argument attacker : completeAF.getAttackers(arg)) {
                for (Argument attacker : neighbours) {
                    queueOfAttackers.offer((AspicArgument) attacker);
                }
            }
        }

        return stack;
    }

    protected List<AspicArgument<FolFormula>> orderSubArgumentsByRule(AspicArgument<FolFormula> argument) {
        ArrayList<AspicArgument<FolFormula>> retList = new ArrayList<>();

        InferenceRule<FolFormula> topRule = argument.getTopRule();

        for (FolFormula prem : topRule.getPremise()) {
            boolean found = false;
            for (AspicArgument<FolFormula> subArg : argument.getDirectSubs()) {
                if (!prem.equals(subArg.getConclusion())) {
                    continue;
                }
                if (retList.contains(subArg)) {
                    continue;
                }

                retList.add(subArg);
                found = true;
                break;
            }
            if (!found) {
                return new ArrayList<>();
            }
        }

        return retList;
    }

    public String explain(boolean complete) {
//        if(this.goal.getStage() != GoalStage.Chosen){
//            return toString();
//        }

        String composition = String.format("Cycle: %03d", this.agentCycle) + (complete ? "" : " (partial for " + goal.getFullPredicate().toString() + ")") + "\n      » ";

//        Collection<Argument> target = (complete ? completeAF : relevantArguments);
        Collection<AspicArgument> target = getArgumentsStackForExplaining(complete);

        if (target.isEmpty() && !complete && goal.getStage() == GoalStage.Pursuable) {
            String goal_name = goal.getFullPredicate().toString().replaceAll(goal.getGoalTerm().get(), "").replaceAll(",\\s*,", "").replaceAll("\\(\\s*,", "(").replaceAll(",\\s\\)", ")").replaceAll("\\s+", " ");
            composition = composition.concat("Since " + goal_name + " had no argument against it, " + goal_name + " became pursuable.");
            return composition;
        }

        Pattern SCHEMA = Pattern.compile("\\<\\s*((conclusion(_name)?)|((belief(_name)?|term_conc|goal_name)_[0-9]+)|(term_[0-9]+_[0-9]+))\\s*\\>");
        Pattern SCHEMA_IFACCEPTED = Pattern.compile("\\[\\s*((if_accepted)\\s*\\?\\s*([^:]*)\\s*:\\s*([^\\]]*)\\s*)\\s*\\]");
        Matcher m;

        for (Argument arg : target) {
//            if (arg instanceof AspicArgument) {
                AspicArgument<FolFormula> aArg = (AspicArgument<FolFormula>) arg;

                if (!(aArg.getTopRule() instanceof RuleWithIdInterface)) {
                    continue;
                }

                RuleWithIdInterface topRule = (RuleWithIdInterface) aArg.getTopRule();
                String pattern = topRule.getExplanationSchema();
                if (pattern == null) {
                    continue;
                }
                String ruleId = topRule.getRuleId();

                ArrayList<String> beliefs = new ArrayList<>(aArg.getDirectSubs().size());
                ArrayList<String> beliefsNames = new ArrayList<>(aArg.getDirectSubs().size());
                ArrayList<ArrayList<String>> terms = new ArrayList<>(beliefsNames.size());

                boolean isAccepted = acceptedArguments.get(arg);

                ArrayList<Term> goalTerms = new ArrayList<>();

                for (AspicArgument<FolFormula> subA : orderSubArgumentsByRule(aArg)) {
                    FolFormula formula = subA.getConclusion();
                    FolAtom atom = null;
                    while (atom == null) {
                        if (formula instanceof FolAtom) {
                            atom = (FolAtom) formula;
                        } else if (formula instanceof Negation) {
                            formula = ((Negation) formula).getFormula();
                        } else {
                            break;
                        }
                    }

                    if (atom == null) {
                        continue;
                    }

                    beliefs.add(formula.toString());
                    beliefsNames.add(atom.getPredicate().getName());
                    ArrayList<String> termsList = new ArrayList<>();
                    for (Term t : atom.getArguments()) {
                        termsList.add((String) t.get());

                        if (t.getSort().equals(agent.getGoalSort())) {
                            if (!goalTerms.contains(t)) {
                                goalTerms.add(t);
                            }
                        }
                    }

                    terms.add(termsList);
                }

                FolFormula formula = aArg.getConclusion();
                FolAtom atom = null;
                while (atom == null) {
                    if (formula instanceof FolAtom) {
                        atom = (FolAtom) formula;
                    } else if (formula instanceof Negation) {
                        formula = ((Negation) formula).getFormula();
                    } else {
                        break;
                    }
                }

                if (atom == null) {
                    continue;
                }

                String conclusion = formula.toString();
                String conclusionName = atom.getPredicate().getName();
                ArrayList<String> conclusionTerms = new ArrayList<>();
                for (Term t : atom.getArguments()) {
                    conclusionTerms.add((String) t.get());

                    if (t.getSort().equals(agent.getGoalSort())) {
                        if (!goalTerms.contains(t)) {
                            goalTerms.add(t);
                        }
                    }
                }

                ArrayList<String> goals = new ArrayList<>(goalTerms.size());
                boolean skip = false;

                for (Term t : goalTerms) {
                    if (t == agent.getgHolder()) {
                        if (goal.getStage() == GoalStage.Active) {
                            String fullPredicate = atom.toString();
                            fullPredicate = fullPredicate.replaceAll(agent.getgHolder().get(), "").replaceAll(",\\s*,", "").replaceAll("\\(\\s*,", "(").replaceAll(",\\s\\)", ")").replaceAll("\\s+", " ");

                            goals.add(fullPredicate);
                            continue;
                        } else {
                            skip = true;
                            for (GoalMemory gM : agent.getGoalmemoryPacketForCycle(agentCycle).getAllGoalMemory()) {
                                if (gM.agentCycle == agentCycle) {
                                    if (FolFormulaUtils.equalsWithSubstitution(gM.goal.getFullPredicate(), atom, agent.getgHolder())) {
                                        skip = false;
                                        String fullPredicate = atom.toString();
                                        fullPredicate = fullPredicate.replaceAll(agent.getgHolder().get(), "").replaceAll(",\\s*,", "").replaceAll("\\(\\s*,", "(").replaceAll(",\\s\\)", ")").replaceAll("\\s+", " ");

                                        goals.add(fullPredicate);
                                        break;
                                    }
                                }
                            }

                            if (skip) {
                                break;
                            }
                        }

                        continue;
                    }

                    Goal g = agent.getGoalForID((String) t.get());

                    String fullPredicate = g.getFullPredicate().toString();
                    fullPredicate = fullPredicate.replaceAll(g.getGoalTerm().get(), "").replaceAll(",\\s*,", ",").replaceAll("\\(\\s*,", "(").replaceAll(",\\s\\)", ")").replaceAll("\\s+", " ");

                    goals.add(fullPredicate);
                }

                if (skip) {
                    continue;
                }

                if (!composition.isBlank() && !composition.endsWith("\n      » ")) {
                    composition = composition.concat("\n      » ");
                }

                m = SCHEMA_IFACCEPTED.matcher(pattern);
                String pattern2 = pattern;

                while (m.find()) {
                    String replace = pattern.substring(m.start(), m.end());
                    String if_accepted = m.group(3);
                    String if_not_accepted = m.group(4);

                    pattern2 = pattern2.replaceFirst("\\Q".concat(replace).concat("\\E"), (isAccepted ? if_accepted : if_not_accepted));
                }

                pattern = pattern2;
                m = SCHEMA.matcher(pattern);

                boolean usesGoal = false;

                while (m.find()) {
                    String replace = pattern.substring(m.start(), m.end());
                    String found = m.group(1);
                    String[] split = found.split("_");

                    try {
                        if (found.startsWith("belief_name")) {
                            pattern2 = pattern2.replaceFirst(replace, beliefsNames.get(Integer.parseInt(split[2])));
                        } else if (found.startsWith("belief")) {
                            pattern2 = pattern2.replaceFirst(replace, beliefs.get(Integer.parseInt(split[1])));
                        }
                        if (found.startsWith("term_conc")) {
                            pattern2 = pattern2.replaceFirst(replace, conclusionTerms.get(Integer.parseInt(split[2])));
                        } else if (found.startsWith("term")) {
                            pattern2 = pattern2.replaceFirst(replace, terms.get(Integer.parseInt(split[1])).get(Integer.parseInt(split[2])));
                        }
                        if (found.startsWith("conclusion_name")) {
                            pattern2 = pattern2.replaceFirst(replace, conclusionName);
                        } else if (found.startsWith("conclusion")) {
                            pattern2 = pattern2.replaceFirst(replace, conclusion);
                        } else if (found.startsWith("goal_name")) {
                            usesGoal = true;
                            pattern2 = pattern2.replaceFirst(replace, goals.get(Integer.parseInt(split[2])));
                        }
                    } catch (Exception ex) {
                    }
                }

                pattern2 = pattern2.trim();
                if (pattern2.startsWith("{")) {
                    pattern2 = pattern2.substring(1).trim();
                }
                if (pattern2.endsWith("}")) {
                    pattern2 = pattern2.substring(0, pattern2.length() - 1).trim();
                }

                if (!composition.contains(pattern2) || usesGoal) {
                    composition = composition.concat(pattern2);
                }
//            }
        }

        return composition;
    }

    protected Set<Argument> getRelevantArguments(List<Argument> target, DungTheory argFramework, Extension selectedExt) {
        HashSet<Argument> set = new HashSet<>();

        if (argFramework == null && target == null && selectedExt == null) {
            return set;
        }

        for (Argument a : argFramework) {
            if (a instanceof AspicArgument) {
                AspicArgument<FolFormula> a2 = (AspicArgument) a;

                acceptedArguments.put(a, selectedExt.contains(a));

                if (a2.getConclusion().isGround()) {
                    for (Term t : a2.getConclusion().getTerms()) {
                        if (t.equals(goal.getGoalTerm())) {
                            set.add(a);
//                            acceptedArguments.put(a, selectedExt.contains(a));
                        }
                    }
                }
            }
        }

        if (target != null) {
            set.addAll(target);
            for (Argument t : target) {
//                acceptedArguments.put(t, selectedExt.contains(t));

                for (Argument a : ((AspicArgument<FolFormula>) t).getAllSubs()) {
                    set.add(a);
//                    acceptedArguments.put(a, selectedExt.contains(a));
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
//                HashSet<Argument> conflictSet = new HashSet<>(argFramework.getAttacked(inSet));
//                conflictSet.addAll(argFramework.getAttackers(inSet));
                for (Argument attacker : argFramework.getAttacked(inSet)) {
                    AspicArgument<FolFormula> argAtt = (AspicArgument<FolFormula>) attacker;

                    if (argAtt.getConclusion().equals((FolFormula) argInSet.getConclusion().complement())) {
                        attackSet.add(new Tuple<>(inSet, attacker));
                    }

                    if (!set.contains(attacker)) {
                        auxSet.add(attacker);
//                        acceptedArguments.put(attacker, selectedExt.contains(attacker));

                        for (Argument a : argAtt.getAllSubs()) {
                            auxSet.add(a);
//                            acceptedArguments.put(a, selectedExt.contains(a));
                        }
                    }
                }
                for (Argument attacker : argFramework.getAttackers(inSet)) {
                    AspicArgument<FolFormula> argAtt = (AspicArgument<FolFormula>) attacker;

                    if (argAtt.getConclusion().equals((FolFormula) argInSet.getConclusion().complement())) {
                        attackSet.add(new Tuple<>(attacker, inSet));
                    }

                    if (!set.contains(attacker)) {
                        auxSet.add(attacker);
//                        acceptedArguments.put(attacker, selectedExt.contains(attacker));

                        for (Argument a : argAtt.getAllSubs()) {
                            auxSet.add(a);
//                            acceptedArguments.put(a, selectedExt.contains(a));
                        }
                    }
                }
            }

            changed = changed || set.addAll(auxSet);
            queue = auxSet;
        } while (changed);

        return set;
    }

    private HashMap<Object, utfpr.edu.argumentation.diagram.Argument> elementsToComponentMap = new HashMap<>();

    public void showCompleteAFInCluster(ArgumentionFramework cluster) {
        if (cluster == null) {
            return;
        }

        cluster.clear();

        elementsToComponentMap.clear();

        HashSet<Tuple<Argument, Argument>> attacks = new HashSet<>();

        for (Argument arg : completeAF) {
            boolean skip = false;

            AspicArgument<FolFormula> argumentFol = (AspicArgument<FolFormula>) arg;

            if (argumentFol.getConclusion() instanceof FolAtom) {
                FolAtom conc = (FolAtom) argumentFol.getConclusion();

                if (conc.getPredicate().getName().equals(Agent.GOAL_PLACE_HOLDER_PRED_STR)) {
                    continue;
                }
                if (conc.getPredicate().getName().equals(Agent.TYPE_PLACE_HOLDER_PRED_STR)) {
                    continue;
                }
            }

            for (Argument attacked : completeAF.getAttacked(arg)) {
                AspicArgument<FolFormula> argAtt = (AspicArgument<FolFormula>) attacked;

                if (argAtt.getConclusion().equals((FolFormula) argumentFol.getConclusion().complement())) {
                    attacks.add(new Tuple<>(arg, attacked));
                }
            }

            for (Argument attacker : completeAF.getAttackers(arg)) {
                AspicArgument<FolFormula> argAtt = (AspicArgument<FolFormula>) attacker;

                if (argAtt.getConclusion().equals((FolFormula) argumentFol.getConclusion().complement())) {
                    attacks.add(new Tuple<>(attacker, arg));
                }
            }

            for (Argument arg2 : completeAF) {
                AspicArgument<FolFormula> argumentFol2 = (AspicArgument<FolFormula>) arg2;

                if (argumentFol.isSubArgumentOf(argumentFol2) && argumentFol != argumentFol2) {
                    skip = true;
                    break;
                }
            }

            if (skip) {
                continue;
            }

            utfpr.edu.argumentation.diagram.Argument argument = showArgumentInCluster(argumentFol, cluster, false, acceptedArguments.get(arg));
            elementsToComponentMap.put(arg, argument);
            cluster.addArgument(argument);
        }

        for (Tuple<Argument, Argument> attck : attacks) {
            JComponent attacker = elementsToComponentMap.get(attck.getT());
            JComponent attacked = elementsToComponentMap.get(attck.getU());

            if (attacker == null) {
                continue;
            }
            if (attacked == null) {
                continue;
            }

            cluster.addAttack((utfpr.edu.argumentation.diagram.Argument) attacker, (utfpr.edu.argumentation.diagram.Argument) attacked, false);
        }

        cluster.repositionComponents();
        cluster.revalidate();
        cluster.repaint();
    }

    public void showInCluster(ArgumentionFramework cluster) {
        showInCluster(cluster, true);
    }

    public void showInCluster(ArgumentionFramework cluster, boolean checkFocus) {
        if (cluster == null) {
            return;
        }

        cluster.clear();

        elementsToComponentMap.clear();

        for (Argument arg : relevantArguments) {
            boolean skip = false;

            AspicArgument<FolFormula> argumentFol = (AspicArgument<FolFormula>) arg;

            for (Argument arg2 : relevantArguments) {
                AspicArgument<FolFormula> argumentFol2 = (AspicArgument<FolFormula>) arg2;

                if (argumentFol.isSubArgumentOf(argumentFol2) && argumentFol != argumentFol2) {
                    skip = true;
                    break;
                }
            }

            if (skip) {
                continue;
            }

            boolean isFocus = false;
            if (support != null && checkFocus) {
                isFocus = support.contains(arg);
            }

            utfpr.edu.argumentation.diagram.Argument argument = showArgumentInCluster(argumentFol, cluster, isFocus, acceptedArguments.get(arg));
            elementsToComponentMap.put(arg, argument);
            cluster.addArgument(argument);
        }

        for (Tuple<Argument, Argument> attck : attackSet) {
            JComponent attacker = elementsToComponentMap.get(attck.getT());
            JComponent attacked = elementsToComponentMap.get(attck.getU());

            if (attacker == null) {
                continue;
            }
            if (attacked == null) {
                continue;
            }

            cluster.addAttack((utfpr.edu.argumentation.diagram.Argument) attacker, (utfpr.edu.argumentation.diagram.Argument) attacked, false);
        }

        cluster.repositionComponents();
        cluster.revalidate();
        cluster.repaint();
    }

    protected utfpr.edu.argumentation.diagram.Argument showArgumentInCluster(AspicArgument<FolFormula> arg, ArgumentionFramework cluster, Boolean isFocus, Boolean accepted) {
        int type = (accepted ? 0 : 2) + (isFocus ? 1 : 0);
        boolean isStrict = arg.getTopRule() instanceof StrictInferenceRule;
        String label = agent.getArgumentConclusionID(arg);
        String tooltip = arg.getConclusion().toString().replaceAll("\\s*" + Agent.GOAL_PLACE_HOLDER_CONST_STR + "\\s*", "").replaceAll(",,", ",").replaceAll("\\(\\s*,", "(").replaceAll(",\\s\\)", ")").replaceAll("\\s+", " ");
        String ruleTooltip = arg.getTopRule().toString().replaceAll("\\s*" + Agent.GOAL_PLACE_HOLDER_CONST_STR + "\\s*", "").replaceAll(",,", ",").replaceAll("\\(\\s*,", "(").replaceAll(",\\s\\)", ")").replaceAll("\\s+", " ");

        label = "<html><p style=\"font-size:85%\"><font color=#000000 >" + tooltip + "&nbsp;&nbsp;</font></p></html>";
//        ruleTooltip = "<html><p style=\"font-size:85%\"><font color=#000000 >" + ruleTooltip.replaceAll("^([a-zA-Z0-9]*)(_([a-zA-Z0-9]*))?(\\^([a-zA-Z0-9]*))?", "$1<sub>$3</sub><sup>$5</sup>") + "&nbsp;&nbsp;</font></p></html>";
        tooltip = null;

        Atom conclusion = new Atom(label, tooltip, isStrict, type, cluster);

        ArrayList<utfpr.edu.argumentation.diagram.Argument> subArgs = new ArrayList<>();
        for (AspicArgument<FolFormula> subA : arg.getDirectSubs()) {
            utfpr.edu.argumentation.diagram.Argument subArg = showArgumentInCluster(subA, cluster, isFocus, acceptedArguments.get(subA));

            if (elementsToComponentMap.get(subA) != null) {
                cluster.removeArgument(elementsToComponentMap.get(subA));
            }

            elementsToComponentMap.put(subA, subArg);
            subArgs.add(subArg);
        }

        String argId = agent.getArgumentID(arg);
        argId = "<html><p style=\"font-size:95%\"><font color=#000000 >" + argId.replaceAll("^([a-zA-Z0-9]*)(_([a-zA-Z0-9]*))?(\\^([a-zA-Z0-9]*))?", "$1<sub>$3</sub><sup>$5</sup>") + "&nbsp;&nbsp;</font></p></html>";
        String ruleId = agent.getRuleID(arg);
        ruleId = "<html><p style=\"font-size:95%\"><font color=#000000 >" + ruleId.replaceAll("^([a-zA-Z0-9]*)(_([a-zA-Z0-9]*))?(\\^([a-zA-Z0-9]*))?", "$1<sub>$3</sub><sup>$5</sup>") + "&nbsp;&nbsp;</font></p></html>";

        if (subArgs.isEmpty()) {
            ruleId = null;
        }

        return new utfpr.edu.argumentation.diagram.Argument(conclusion, type, cluster, argId, ruleId, ruleTooltip, agent.isRuleStrict(arg), subArgs.toArray(new utfpr.edu.argumentation.diagram.Argument[]{}));
    }

    public GoalStage getGoalStage() {
        return goal.getStage();
    }

    public double getGoalPreference() {
        return goal.getGoalBase().getPreference();
    }
}
