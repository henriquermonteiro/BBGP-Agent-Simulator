package utfpr.edu.bbgp.agent.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.tweety.arg.aspic.ruleformulagenerator.FolFormulaGenerator;
import net.sf.tweety.arg.aspic.syntax.AspicArgument;
import net.sf.tweety.arg.dung.reasoner.AbstractExtensionReasoner;
import net.sf.tweety.arg.dung.semantics.Extension;
import net.sf.tweety.arg.dung.semantics.Semantics;
import net.sf.tweety.arg.dung.syntax.Argument;
import net.sf.tweety.arg.dung.syntax.DungTheory;
import net.sf.tweety.commons.ParserException;
import net.sf.tweety.commons.util.MapTools;
import net.sf.tweety.logics.commons.syntax.Constant;
import net.sf.tweety.logics.commons.syntax.Sort;
import net.sf.tweety.logics.commons.syntax.Variable;
import net.sf.tweety.logics.commons.syntax.interfaces.Term;
import net.sf.tweety.logics.fol.parser.FolParser;
import net.sf.tweety.logics.fol.syntax.FolAtom;
import net.sf.tweety.logics.fol.syntax.FolFormula;
import net.sf.tweety.logics.fol.syntax.FolSignature;
import net.sf.tweety.logics.fol.syntax.Negation;
import utfpr.edu.bbgp.agent.Agent;
import utfpr.edu.bbgp.agent.Goal;
import utfpr.edu.bbgp.agent.GoalMemory;
import utfpr.edu.bbgp.agent.GoalStage;
import utfpr.edu.bbgp.agent.Intention;
import utfpr.edu.bbgp.agent.Plan;
import utfpr.edu.bbgp.agent.SleepingGoal;
import utfpr.edu.bbgp.extended.AspicArgumentationTheoryFol;
import utfpr.edu.bbgp.simul.utils.FolFormulaUtils;

/**
 *
 * @author henri
 */
public class GoalProcessingManager {

    private final Agent agent;
    private Boolean goalPreferenceFirst = false;
    private Boolean changed;

    public GoalProcessingManager(Agent agent) {
        this.agent = agent;
    }

    public void setGoalPreferenceFirst(Boolean goalPreferenceFirst) {
        this.goalPreferenceFirst = goalPreferenceFirst;
    }

    public boolean processGoals() {
        changed = false;

        activationStage();

        evaluationStage();

        AspicArgumentationTheoryFol competence = evaluateCompetency();
        AspicArgumentationTheoryFol incompatibility = evaluateIncompatibilities();

        deliberationStage(competence, incompatibility);

        AspicArgumentationTheoryFol context = evaluateContext();

        checkingStage(competence, context);

        return changed;
    }

    public Constant getNextGoalConstant() {
        FolSignature signature = agent.getSignature();

        Set<Term<?>> terms = signature.getSort(Agent.GOAL_SORT_TEXT).getTerms();

        String last = "g0000";

        for (Term t : terms) {
            if (t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)) {
                if (t instanceof Constant && !t.get().equals(Agent.GOAL_PLACE_HOLDER_CONST_STR)) {
                    if (((String) t.get()).compareTo(last) > 0) {
                        last = t.get().toString();
                    }
                }
            }
        }

        int nextNumber = Integer.parseInt(last.substring(1)) + 1;

        Constant newGoal = new Constant(String.format("g%04d", nextNumber), signature.getSort(Agent.GOAL_SORT_TEXT));
        signature.add(newGoal);

        return newGoal;
    }

    protected AspicArgumentationTheoryFol evaluateCompetency() {
        FolSignature signature = agent.getSignature();
        GoalManager goals = agent.getGoalManager();
        Map<SleepingGoal, ArrayList<Plan>> planLib = agent.getPlanLibrary();

        AspicArgumentationTheoryFol theory = new AspicArgumentationTheoryFol(new FolFormulaGenerator()).setCurrentAgent(agent);

        FolParser fParser = new FolParser();
        fParser.setSignature(signature);

        for (Goal pursuableGoal : goals.getGoalAtLeastAtStage(GoalStage.Pursuable)) {
            if (!planLib.get(pursuableGoal.getGoalBase()).isEmpty()) {

                try {
                    theory.addAxiom(fParser.parseFormula(Agent.HAS_PLANS_FOR_STR + "(" + pursuableGoal.getGoalTerm().get() + ")"));
                } catch (IOException | ParserException ex) {
                    Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return theory;
    }

    protected double preference(Goal g) {
        if (g == null) {
            return 0.0;
        }

        return g.getGoalBase().getPreference();
    }

    protected boolean isBeliefCompatible(Set<FolFormula> set1, Set<FolFormula> set2) {
        for (FolFormula f1 : set1) {
            for (FolFormula f2 : set2) {
                if (f1.equals(f2.complement())) {
                    return false;
                }
            }
        }

        return true;
    }

    protected Extension getCompatibleGoalsExtension(Collection<Extension> models, HashMap<Argument, Goal> argToGoalMap, Map<Argument, Map<Variable, Term<?>>> argToSubsttutionMap, Collection<Extension> possibleArguments) {
        if (argToGoalMap == null) {
            throw new NullPointerException("argToGoalMap is required.");
        }
        if (models == null) {
            throw new NullPointerException("models is required.");
        }

        if (models.size() <= 1) {
            return models.iterator().next();
        }

        double maxTotalPreference = 0.0;
        int maxGoalCount = 0;
        int maxValidContextCount = 0;
        Extension bestExt = null;

        for (Extension ext : models) {
            double extTotalPreference = 0.0;
            int extGoalCount = 0;
            int extValidContextCount = 0;

            for (Argument a : ext) {
                String[] aSplit = a.getName().split("_");
                Goal g = argToGoalMap.get(a);
                Plan pG = agent.getPlanLibrary().get(g.getGoalBase()).get(Integer.parseInt(aSplit[aSplit.length - 2]));

                extTotalPreference += g.getGoalBase().getPreference();
                extGoalCount++;

                if (isPlanContextValid(pG, g.getFullPredicate(), argToSubsttutionMap.get(a), possibleArguments)) {
                    extValidContextCount++;
                }
            }

            if (goalPreferenceFirst) {
                if (maxTotalPreference < extTotalPreference) {
                    maxTotalPreference = extTotalPreference;
                    maxGoalCount = extGoalCount;
                    maxValidContextCount = extValidContextCount;

                    bestExt = ext;
                } else if (maxTotalPreference == extTotalPreference) {
                    if (maxValidContextCount < extValidContextCount) {
                        maxTotalPreference = extTotalPreference;
                        maxGoalCount = extGoalCount;
                        maxValidContextCount = extValidContextCount;

                        bestExt = ext;
                    } else if (maxValidContextCount == extValidContextCount) {
                        if (maxGoalCount < extGoalCount) {
                            maxTotalPreference = extTotalPreference;
                            maxGoalCount = extGoalCount;
                            maxValidContextCount = extValidContextCount;

                            bestExt = ext;
                        }
                    }
                }
            } else {
                if (maxValidContextCount < extValidContextCount) {
                    maxTotalPreference = extTotalPreference;
                    maxGoalCount = extGoalCount;
                    maxValidContextCount = extValidContextCount;

                    bestExt = ext;
                } else if (maxValidContextCount == extValidContextCount) {
                    if (maxGoalCount < extGoalCount) {
                        maxTotalPreference = extTotalPreference;
                        maxGoalCount = extGoalCount;
                        maxValidContextCount = extValidContextCount;

                        bestExt = ext;
                    } else if (maxGoalCount == extGoalCount) {
                        if (maxTotalPreference < extTotalPreference) {
                            maxTotalPreference = extTotalPreference;
                            maxGoalCount = extGoalCount;
                            maxValidContextCount = extValidContextCount;

                            bestExt = ext;
                        }
                    }
                }
            }
        }

        return bestExt;
    }

    protected AspicArgumentationTheoryFol evaluateIncompatibilities() {
        FolSignature signature = agent.getSignature();
        GoalManager goals = agent.getGoalManager();
        ResourceManager resources = agent.getResources();
        Map<SleepingGoal, ArrayList<Plan>> planLib = agent.getPlanLibrary();

        AspicArgumentationTheoryFol theory = new AspicArgumentationTheoryFol(new FolFormulaGenerator()).setCurrentAgent(agent);

        Collection<Extension> possibleArguments = getPossibleBeliefsModels();

        // Identify terminal, resource and superfluidity attacks
        // terminal : conflict in the belief (be_operative and ¬be_operative)
        // resource : not enough resource
        // superfluidity : same concuclusion or sub arguments of an argument with same conclusion (as long as there isn't the same subargument among them)
        HashMap<Argument, Goal> argToGoalMap = new HashMap<>();
        HashMap<Argument, Map<Variable, Term<?>>> argToSubstitutionMap = new HashMap<>();
        DungTheory attackTheory = new DungTheory();

        FolParser fParser = new FolParser();
        fParser.setSignature(signature);

        // for each goal
        for (Goal g : goals.getGoalByStage(GoalStage.Pursuable)) {
            // for each plan
            int pos = 0;
            for (Plan p : planLib.get(g.getGoalBase())) {
                Set<Map<Variable, Term<?>>> substitutions = getSubstitutionsForPlan(p, g.getFullPredicate(), possibleArguments);

                int sub = 0;
                if (substitutions.isEmpty()) {
                    boolean skip = false;
                    for (FolFormula ctxt : p.getUnifiedSet(g.getFullPredicate())) {
                        if (!ctxt.isGround()) {
                            skip = true;
                            break;
                        }
                    }
                    if (!skip) {

                        // create an argument
                        Argument arg = new Argument(g.getFullPredicate().toString() + "_" + (pos) + "_" + (sub));

                        sub++;

                        argToGoalMap.put(arg, g);
                        argToSubstitutionMap.put(arg, new HashMap<>());

                        attackTheory.add(arg);
                    }
                }
                for (Map<Variable, Term<?>> mapping : substitutions) {
                    // create an argument
                    Argument arg = new Argument(g.getFullPredicate().toString() + "_" + (pos) + "_" + (sub));

                    sub++;

                    argToGoalMap.put(arg, g);
                    argToSubstitutionMap.put(arg, mapping);

                    attackTheory.add(arg);
                }

                pos++;
            }
        }

        for (Goal g : goals.getGoalByStage(GoalStage.Chosen)) {
            Argument arg = new Argument(g.getFullPredicate().toString() + "_" + (g.getSugestedPlanIndex()) + "_s");

            argToGoalMap.put(arg, g);
            argToSubstitutionMap.put(arg, g.getSugestedSubstitution());

            attackTheory.add(arg);
        }

        for (Goal g : goals.getGoalByStage(GoalStage.Executive)) {
            Argument arg = new Argument(g.getFullPredicate().toString() + "_" + (g.getSugestedPlanIndex()) + "_s");

            argToGoalMap.put(arg, g);
            argToSubstitutionMap.put(arg, g.getSugestedSubstitution());

            attackTheory.add(arg);
        }

        Argument[] args = argToGoalMap.keySet().toArray(new Argument[]{});
        // for each pair of arguments
        for (int k = 0; k < args.length - 1; k++) {
            for (int j = k + 1; j < args.length; j++) {
                Goal gK = argToGoalMap.get(args[k]);
                Goal gJ = argToGoalMap.get(args[j]);

                String[] ax = args[k].getName().split("_");
                Plan pGK = planLib.get(gK.getGoalBase()).get(Integer.parseInt(ax[ax.length - 2]));
                ax = args[j].getName().split("_");
                Plan pGJ = planLib.get(gJ.getGoalBase()).get(Integer.parseInt(ax[ax.length - 2]));

                boolean addAttack = false;
                String attackType = "";
                // if their context is incompatible add an attack
                if (!isBeliefCompatible(pGK.getUnifiedSet(gK.getFullPredicate()), pGJ.getUnifiedSet(gJ.getFullPredicate()))) {
                    addAttack = true;
                    attackType += "t";
                }
                // if there is not enough resource for both add an attack
                if (!resources.checkCompatibility(pGK.getResourceContext(), pGJ.getResourceContext())) {
                    addAttack = true;
                    attackType += "r";
                }
                // if the goal is the same add an attack
                if (gK == gJ) {
                    addAttack = true;
                    attackType += "s";
                }

                if (addAttack) {
                    double gKPref = (gK.getStage() == GoalStage.Pursuable ? gK.getGoalBase().getPreference() : 1.1);
                    gKPref -= 0.000001 * gK.getSugestedPlanIndex();
                    double gJPref = (gJ.getStage() == GoalStage.Pursuable ? gJ.getGoalBase().getPreference() : 1.1);
                    gJPref -= 0.000001 * gJ.getSugestedPlanIndex();

                    if (gJPref > gKPref) {
                        attackTheory.addAttack(args[j], args[k]);

                        try {
                            theory.addAxiom(fParser.parseFormula(Agent.PREFERRED_STR + "(" + gJ.getGoalTerm().get() + ", " + gK.getGoalTerm().get() + ")"));
                            theory.addAxiom(new Negation(fParser.parseFormula(Agent.PREFERRED_STR + "(" + gK.getGoalTerm().get() + ", " + gJ.getGoalTerm().get() + ")")));
                        } catch (IOException | ParserException ex) {
                            Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else if (gJPref < gKPref) {
                        attackTheory.addAttack(args[k], args[j]);

                        try {
                            theory.addAxiom(fParser.parseFormula(Agent.PREFERRED_STR + "(" + gK.getGoalTerm().get() + ", " + gJ.getGoalTerm().get() + ")"));
                            theory.addAxiom(new Negation(fParser.parseFormula(Agent.PREFERRED_STR + "(" + gJ.getGoalTerm().get() + ", " + gK.getGoalTerm().get() + ")")));
                        } catch (IOException | ParserException ex) {
                            Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else if (gKPref <= 1.0) {
                        attackTheory.addAttack(args[j], args[k]);
                        attackTheory.addAttack(args[k], args[j]);

                        try {
                            if (gK.getGoalTerm() != gJ.getGoalTerm()) {
                                theory.addAxiom(fParser.parseFormula(Agent.EQ_PREFERRED_STR + "(" + gK.getGoalTerm().get() + ", " + gJ.getGoalTerm().get() + ")"));
                                theory.addAxiom(fParser.parseFormula(Agent.EQ_PREFERRED_STR + "(" + gJ.getGoalTerm().get() + ", " + gK.getGoalTerm().get() + ")"));
                            }
                        } catch (IOException | ParserException ex) {
                            Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    if (gK.getGoalTerm() != gJ.getGoalTerm()) {
                        try {
                            theory.addAxiom(fParser.parseFormula(Agent.INCOMPATIBLE_STR + "(" + gK.getGoalTerm().get() + ", " + gJ.getGoalTerm().get() + "," + attackType + ")"));
                            theory.addAxiom(fParser.parseFormula(Agent.INCOMPATIBLE_STR + "(" + gJ.getGoalTerm().get() + ", " + gK.getGoalTerm().get() + "," + attackType + ")"));
                        } catch (IOException | ParserException ex) {
                            Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }

        // Run preferred semantic
        AbstractExtensionReasoner reasoner = AbstractExtensionReasoner.getSimpleReasonerForSemantics(Semantics.CONFLICTFREE_SEMANTICS);

        Collection<Extension> models = reasoner.getModels(attackTheory);

        // choose extension
        Extension compatibleGoals = getCompatibleGoalsExtension(models, argToGoalMap, argToSubstitutionMap, possibleArguments);

        for (Argument arg : attackTheory) {
            try {
                if (compatibleGoals.contains(arg)) {
                    theory.addAxiom(fParser.parseFormula(Agent.MAX_UTIL_STR + "(" + argToGoalMap.get(arg).getGoalTerm().get() + ")"));

                    String[] ax = arg.getName().split("_");

                    argToGoalMap.get(arg).setSugestedPlanIndex(Integer.parseInt(ax[ax.length - 2]));
                    argToGoalMap.get(arg).setSugestedSubstitution(argToSubstitutionMap.get(arg));

                    if (attackTheory.getAttackers(arg).isEmpty() && attackTheory.getAttacked(arg).isEmpty()) {
                        theory.addAxiom(fParser.parseFormula(Agent.NOT_HAS_INCOMPATIBILITY_STR + "(" + argToGoalMap.get(arg).getGoalTerm().get() + ")"));
                    }

                } else {
                    theory.addAxiom(new Negation(fParser.parseFormula(Agent.MAX_UTIL_STR + "(" + argToGoalMap.get(arg).getGoalTerm().get() + ")")));
                }
            } catch (IOException | ParserException ex) {
                Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return theory;
    }

    protected Set<Map<Variable, Term<?>>> getSubstitutionsForPlan(Plan p, FolAtom conclusionGroundPredicate, Collection<Extension> models) {
        HashSet<Constant> usedTerms = new HashSet<>();
        HashSet<Map<Variable, Term<?>>> returnMappings = new HashSet<>();

        for (Extension ext : models) {
            for (Argument posArg : ext) {
                AspicArgument<FolFormula> posAArg = (AspicArgument<FolFormula>) posArg;
                usedTerms.addAll(posAArg.getConclusion().getTerms(Constant.class));
            }

            Set<FolFormula> semiUnifiedPremisses = p.getUnifiedSet(conclusionGroundPredicate);

            HashSet<Variable> variables = new HashSet<>();
            for (FolFormula prem : semiUnifiedPremisses) {
                if (prem instanceof Negation) {
                    continue;
                }

                variables.addAll(prem.getUnboundVariables());
            }

            Map<Sort, Set<Variable>> sorts_variables = new HashMap<>();
            for (Variable v : variables) {
                if (!sorts_variables.containsKey(v.getSort())) {
                    sorts_variables.put(v.getSort(), new HashSet<>());
                }
                sorts_variables.get(v.getSort()).add(v);
            }
            //partition terms by sorts
            Map<Sort, Set<Term<?>>> sorts_terms = Sort.sortTerms(usedTerms);
            //combine the partitions
            Map<Set<Variable>, Set<Term<?>>> mappings = new HashMap<>();
            for (Sort s : sorts_variables.keySet()) {
                if (!sorts_terms.containsKey(s)) {
                    throw new IllegalArgumentException("There is no term of sort " + s + " to substitute.");
                }
                mappings.put(sorts_variables.get(s), sorts_terms.get(s));
            }

            Set<Map<Variable, Term<?>>> allMappings = new MapTools<Variable, Term<?>>().allMaps(mappings);

            allMappings.forEach((arg0) -> {
                if (isPlanContextValid(p, conclusionGroundPredicate, arg0, models)) {
                    returnMappings.add(arg0);
                }
            });
        }

        return returnMappings;
    }

    protected Collection<Extension> getPossibleBeliefsModels() {
        AspicArgumentationTheoryFol evalTheory = new AspicArgumentationTheoryFol(new FolFormulaGenerator()).setCurrentAgent(agent);
        evalTheory.addAll(agent.getBeliefs());
        evalTheory.addAll(agent.getStandardRules());

        AbstractExtensionReasoner simpleReasoner = AbstractExtensionReasoner.getSimpleReasonerForSemantics(Semantics.PREFERRED_SEMANTICS);
        return simpleReasoner.getModels(evalTheory.asDungTheory());
    }

    protected boolean isPlanContextValid(Plan p, FolAtom unifiedBelief, Map<Variable, Term<?>> auxiliarSubstitution, Collection<Extension> models) {
        for (Extension ext : models) {
            HashSet<FolFormula> extensionConclusions = new HashSet<>(ext.size());

            for (Argument arg : ext) {
                if (arg instanceof AspicArgument) {
                    AspicArgument<FolFormula> aarg = (AspicArgument) arg;

                    extensionConclusions.add(aarg.getConclusion());
                }
            }

            HashSet<FolFormula> positiveContextElements = new HashSet<>();
            HashSet<FolFormula> negativeContextElements = new HashSet<>();

            for (FolFormula ff : p.getUnifiedSet(unifiedBelief)) {
                FolFormula grounded = ff;
                if (auxiliarSubstitution != null) {
                    grounded = (FolFormula) ff.substitute(auxiliarSubstitution);
                }
                if (!grounded.isGround()) {
                    return false;
                }

                if (grounded instanceof Negation) {
                    negativeContextElements.add(((Negation) grounded).getFormula());
                } else {
                    positiveContextElements.add(grounded);
                }
            }

            if (extensionConclusions.containsAll(positiveContextElements)) {
                for (FolFormula negative : negativeContextElements) {
                    if (extensionConclusions.contains(negative)) {
                        return false;
                    }
                }

                if (agent.getResources().isAvaliable(p.getResourceContext())) {
                    return true;
                }
            }
        }

        return false;
    }

    protected AspicArgumentationTheoryFol evaluateContext() {
        FolSignature signature = agent.getSignature();
        GoalManager goals = agent.getGoalManager();
        Map<SleepingGoal, ArrayList<Plan>> planLib = agent.getPlanLibrary();

        AspicArgumentationTheoryFol theory = new AspicArgumentationTheoryFol(new FolFormulaGenerator()).setCurrentAgent(agent);

        FolParser fParser = new FolParser();
        fParser.setSignature(signature);

        Collection<Extension> models = getPossibleBeliefsModels();

        for (Goal choosenGoal : goals.getGoalByStage(GoalStage.Chosen)) {
            if (!planLib.get(choosenGoal.getGoalBase()).isEmpty()) {
                boolean validContext = false;

                for (Plan p : planLib.get(choosenGoal.getGoalBase())) {
                    validContext = validContext | isPlanContextValid(p, choosenGoal.getFullPredicate(), choosenGoal.getSugestedSubstitution(), models);
                }
                try {
                    theory.addAxiom(fParser.parseFormula((validContext ? "" : "!") + Agent.SATISFIED_CONTEXT_STR + "(" + choosenGoal.getGoalTerm().get() + ")"));
                } catch (IOException | ParserException ex) {
                    Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return theory;
    }

    protected Extension selectExtension_ActivationStage(Collection<Extension> extensions) {
        Extension ret = null;
        int goals = -1;

        for (Extension e : extensions) {
            int extGoals = 0;

            for (Argument arg : e) {
                if (arg instanceof AspicArgument) {
                    AspicArgument<FolFormula> aarg = (AspicArgument) arg;

                    FolFormula conc = aarg.getConclusion();

                    for (Term t : conc.getTerms()) {
                        if (t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)) {
                            extGoals++;
                            break;
                        }
                    }
                }
            }

            if (goals < extGoals) {
                goals = extGoals;
                ret = e;
            }
        }

        return ret;
    }

    protected void activationStage() {
        GoalManager goals = agent.getGoalManager();

        AspicArgumentationTheoryFol theory = new AspicArgumentationTheoryFol(new FolFormulaGenerator()).setCurrentAgent(agent);
        theory.addAll(agent.getBeliefs());
        theory.addAll(agent.getStandardRules());
        theory.addAll(agent.getActivationRules());

        AbstractExtensionReasoner prefSemantic = AbstractExtensionReasoner.getSimpleReasonerForSemantics(Semantics.PREFERRED_SEMANTICS);

        DungTheory dTheory = theory.asDungTheory();
        Extension selected = selectExtension_ActivationStage(prefSemantic.getModels(dTheory));

        HashMap<Goal, ArrayList<Argument>> argumentsForGoal = new HashMap<>();

        for (Argument arg : dTheory) {
            if (arg instanceof AspicArgument) {
                AspicArgument<FolFormula> aarg = (AspicArgument) arg;

                FolFormula conc = aarg.getConclusion();

                if (conc instanceof FolAtom) {
                    if (((FolAtom) conc).getPredicate().getName().equals(Agent.GOAL_PLACE_HOLDER_PRED_STR)) {
                        continue;
                    }
                }

                for (Term t : conc.getTerms()) {
                    if (t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)) {
                        boolean accepted = selected.contains(arg);

                        if (accepted) {
                            Goal g = goals.createGoal(((FolAtom) conc).getPredicate(), (FolAtom) conc);

                            if (g != null) {
                                ArrayList<Argument> list = argumentsForGoal.get(g);
                                if (list == null) {
                                    list = new ArrayList<>();
                                    argumentsForGoal.put(g, list);
                                }

                                list.add(arg);
                                changed = true;
                            }

                        } else {
                            Goal g = goals.createGoal(((FolAtom) conc).getPredicate(), (FolAtom) conc, false);

                            ArrayList<Argument> list = argumentsForGoal.get(g);
                            if (list == null) {
                                list = new ArrayList<>();
                                argumentsForGoal.put(g, list);
                            }

                            list.add(arg);
                        }

                        break;
                    }
                }
            }
        }

        for (Goal g : argumentsForGoal.keySet()) {
            List<Argument> targets = argumentsForGoal.get(g);

            if (g.getStage() == GoalStage.Active) {
                agent.addGoalMemoryEntry(new GoalMemory(agent, agent.getCycle(), g, true, targets, dTheory, selected));
            }
        }
    }

    protected Extension selectExtension_EvaluationStage(Collection<Extension> extensions) {
        Extension ret = null;
        int goals = Integer.MAX_VALUE;

        for (Extension e : extensions) {
            int extGoals = 0;

            for (Argument arg : e) {
                if (arg instanceof AspicArgument) {
                    AspicArgument<FolFormula> aarg = (AspicArgument) arg;

                    FolFormula conc = aarg.getConclusion();

                    for (Term t : conc.getTerms()) {
                        if (t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)) {
                            extGoals++;
                            break;
                        }
                    }
                }
            }

            if (goals > extGoals) {
                goals = extGoals;
                ret = e;
            }
        }

        return ret;
    }

    protected void evaluationStage() {
        GoalManager goals = agent.getGoalManager();

        AspicArgumentationTheoryFol theory = new AspicArgumentationTheoryFol(new FolFormulaGenerator()).setCurrentAgent(agent);
        theory.addAll(agent.getBeliefs());
        theory.addAll(agent.getStandardRules());
        theory.addAll(agent.getEvaluationRules());

        AbstractExtensionReasoner prefSemantic = AbstractExtensionReasoner.getSimpleReasonerForSemantics(Semantics.PREFERRED_SEMANTICS);

        DungTheory dTheory = theory.asDungTheory();
        Extension selected = selectExtension_EvaluationStage(prefSemantic.getModels(dTheory));

        HashSet<Goal> activeGoals = (HashSet) goals.getGoalByStage(GoalStage.Active);

        Set<Goal> active = goals.getGoalByStage(GoalStage.Active);
        HashMap<Goal, ArrayList<Argument>> argumentsForGoal = new HashMap<>();

        for (Argument arg : dTheory) {
            if (arg instanceof AspicArgument) {
                AspicArgument<FolFormula> aarg = (AspicArgument) arg;

                FolFormula conc = aarg.getConclusion();

                for (Term t : conc.getTerms()) {
                    if (t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)) {
                        ArrayList<Goal> rem = new ArrayList<>();

                        for (Goal gAx : activeGoals) {
                            if (FolFormulaUtils.equalsWithSubstitution(gAx.getFullPredicate(), (FolFormula) conc.complement(), agent.getgHolder())) {
                                boolean accepted = selected.contains(arg);

                                ArrayList<Argument> list = argumentsForGoal.get(gAx);
                                if (list == null) {
                                    list = new ArrayList<>();
                                    argumentsForGoal.put(gAx, list);
                                }

                                list.add(arg);

                                if (accepted) {
                                    rem.add(gAx);
                                }

                                break;
                            }
                        }

                        for (Goal g : rem) {
                            activeGoals.remove(g);
                        }

                        break;
                    }
                }
            }
        }

        for (Goal g : activeGoals) {
            g.setStage(GoalStage.Pursuable);
            changed = true;
        }

        for (Goal g : active) {
            List<Argument> targets = null;
            if (argumentsForGoal.containsKey(g)) {
                targets = argumentsForGoal.get(g);
                if (g.getStage() == GoalStage.Pursuable) {
                    agent.addGoalMemoryEntry(new GoalMemory(agent, agent.getCycle(), g, true, targets, dTheory, selected));
                    continue;
                }
            }

            Goal gClone = g.clone();
            gClone.setStage(GoalStage.Pursuable);
            agent.addGoalMemoryEntry(new GoalMemory(agent, agent.getCycle(), gClone, true, targets, dTheory, selected));
        }
    }

    protected Extension selectExtension_DeliberationStage(Collection<Extension> extensions) {
        Extension ret = null;
        int goals = -1;

        for (Extension e : extensions) {
            int extGoals = 0;

            for (Argument arg : e) {
                if (arg instanceof AspicArgument) {
                    AspicArgument<FolFormula> aarg = (AspicArgument) arg;

                    FolFormula conc = aarg.getConclusion();

                    for (Term t : conc.getTerms()) {
                        if (t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)) {
                            extGoals++;
                            break;
                        }
                    }
                }
            }

            if (goals < extGoals) {
                goals = extGoals;
                ret = e;
            }
        }

        return ret;
    }

    protected void deliberationStage(AspicArgumentationTheoryFol competence, AspicArgumentationTheoryFol incompetence) {
        GoalManager goals = agent.getGoalManager();

        AspicArgumentationTheoryFol theory = new AspicArgumentationTheoryFol(new FolFormulaGenerator()).setCurrentAgent(agent);
        theory.addAll(agent.getBeliefs());
//        theory.addAll(agent.getStandardRules());
        theory.addAll(agent.getDeliberationRules());
        theory.addAll(competence);
        theory.addAll(incompetence);

        AbstractExtensionReasoner prefSemantic = AbstractExtensionReasoner.getSimpleReasonerForSemantics(Semantics.PREFERRED_SEMANTICS);

        DungTheory dTheory = theory.asDungTheory();
        Extension selected = selectExtension_DeliberationStage(prefSemantic.getModels(dTheory));

        Set<Goal> pursuable = goals.getGoalByStage(GoalStage.Pursuable);
        HashMap<Goal, ArrayList<Argument>> argumentsForGoal = new HashMap<>();

        for (Argument arg : dTheory) {
            if (arg instanceof AspicArgument) {
                AspicArgument<FolFormula> aarg = (AspicArgument) arg;

                FolFormula conc = aarg.getConclusion();

                if (conc instanceof FolAtom) {
                    if (((FolAtom) conc).getPredicate().getName().equals(Agent.CHOOSEN_STR)) {
                        for (Term t : conc.getTerms()) {
                            if (t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)) {
                                for (Goal gAx : pursuable) {
                                    if (gAx.getGoalTerm().equals(t)) {
                                        boolean accepted = selected.contains(arg);

                                        ArrayList<Argument> list = argumentsForGoal.get(gAx);
                                        if (list == null) {
                                            list = new ArrayList<>();
                                            argumentsForGoal.put(gAx, list);
                                        }

                                        list.add(arg);

                                        if (accepted) {
                                            gAx.setStage(GoalStage.Chosen);
                                            changed = true;
                                        }
                                    }
                                }

                                break;
                            }
                        }
                    }
                }
            }
        }

        for (Goal g : pursuable) {
            List<Argument> targets = null;
            if (argumentsForGoal.containsKey(g)) {
                targets = argumentsForGoal.get(g);
                if (g.getStage() == GoalStage.Chosen) {
                    agent.addGoalMemoryEntry(new GoalMemory(agent, agent.getCycle(), g, true, targets, dTheory, selected));
                    continue;
                }
            }

            Goal gClone = g.clone();
            gClone.setStage(GoalStage.Chosen);
            agent.addGoalMemoryEntry(new GoalMemory(agent, agent.getCycle(), gClone, false, targets, dTheory, selected));
        }
    }

    protected Extension selectExtension_CheckingStage(Collection<Extension> extensions) {
        Extension ret = null;
        int goals = -1;

        for (Extension e : extensions) {
            int extGoals = 0;

            for (Argument arg : e) {
                if (arg instanceof AspicArgument) {
                    AspicArgument<FolFormula> aarg = (AspicArgument) arg;

                    FolFormula conc = aarg.getConclusion();

                    for (Term t : conc.getTerms()) {
                        if (t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)) {
                            extGoals++;
                            break;
                        }
                    }
                }
            }

            if (goals < extGoals) {
                goals = extGoals;
                ret = e;
            }
        }

        return ret;
    }

    protected void checkingStage(AspicArgumentationTheoryFol competence, AspicArgumentationTheoryFol context) {
        GoalManager goals = agent.getGoalManager();
        Map<SleepingGoal, ArrayList<Plan>> planLib = agent.getPlanLibrary();

        AspicArgumentationTheoryFol theory = new AspicArgumentationTheoryFol(new FolFormulaGenerator()).setCurrentAgent(agent);
        theory.addAll(agent.getBeliefs());
//        theory.addAll(agent.getStandardRules());
        theory.addAll(agent.getCheckingRules());
        theory.addAll(competence);
        theory.addAll(context);

        AbstractExtensionReasoner prefSemantic = AbstractExtensionReasoner.getSimpleReasonerForSemantics(Semantics.PREFERRED_SEMANTICS);

        DungTheory dTheory = theory.asDungTheory();
        Extension selected = selectExtension_CheckingStage(prefSemantic.getModels(dTheory));

        HashSet<Goal> executiveGoals = new HashSet<>();

        Set<Goal> choosen = goals.getGoalByStage(GoalStage.Chosen);
        HashMap<Goal, ArrayList<Argument>> argumentsForGoal = new HashMap<>();

        for (Argument arg : dTheory) {
            if (arg instanceof AspicArgument) {
                AspicArgument<FolFormula> aarg = (AspicArgument) arg;

                FolFormula conc = aarg.getConclusion();

                if (conc instanceof FolAtom) {
                    if (((FolAtom) conc).getPredicate().getName().equals(Agent.EXECUTIVE_STR)) {
                        for (Term t : conc.getTerms()) {
                            if (t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)) {
                                for (Goal gAx : choosen) {
                                    if (gAx.getGoalTerm().equals(t)) {
                                        boolean accepted = selected.contains(arg);

                                        ArrayList<Argument> list = argumentsForGoal.get(gAx);
                                        if (list == null) {
                                            list = new ArrayList<>();
                                            argumentsForGoal.put(gAx, list);
                                        }

                                        list.add(arg);

                                        if (accepted) {
                                            executiveGoals.add(gAx);

                                            gAx.setStage(GoalStage.Executive);
                                            changed = true;
                                        }
                                    }
                                }

                                break;
                            }
                        }
                    }
                }
            }
        }

        for (Goal g : choosen) {
            List<Argument> targets = null;
            if (argumentsForGoal.containsKey(g)) {
                targets = argumentsForGoal.get(g);
                if (g.getStage() == GoalStage.Executive) {
                    agent.addGoalMemoryEntry(new GoalMemory(agent, agent.getCycle(), g, true, targets, dTheory, selected));
                    continue;
                }
            }

            Goal gClone = g.clone();
            gClone.setStage(GoalStage.Executive);
            agent.addGoalMemoryEntry(new GoalMemory(agent, agent.getCycle(), gClone, false, targets, dTheory, selected));
        }

        for (Goal g : executiveGoals) {
            // save as intention
            Plan p = planLib.get(g.getGoalBase()).get(g.getSugestedPlanIndex());

            Intention newInt = new Intention(agent, g, p, g.getSugestedSubstitution());

            agent.addIntention(newInt);
        }
    }
}
