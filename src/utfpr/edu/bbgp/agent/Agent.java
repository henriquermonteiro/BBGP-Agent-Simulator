/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.agent;

import utfpr.edu.bbgp.agent.manager.ResourceManager;
import utfpr.edu.bbgp.agent.manager.GoalManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.tweety.arg.aspic.ruleformulagenerator.FolFormulaGenerator;
import net.sf.tweety.arg.aspic.syntax.AspicArgument;
import net.sf.tweety.arg.aspic.syntax.DefeasibleInferenceRule;
import net.sf.tweety.arg.aspic.syntax.InferenceRule;
import net.sf.tweety.arg.aspic.syntax.StrictInferenceRule;
import net.sf.tweety.arg.dung.reasoner.AbstractExtensionReasoner;
import net.sf.tweety.arg.dung.semantics.Extension;
import net.sf.tweety.arg.dung.semantics.Semantics;
import net.sf.tweety.arg.dung.syntax.Argument;
import net.sf.tweety.arg.dung.syntax.DungTheory;
import net.sf.tweety.commons.ParserException;
import net.sf.tweety.logics.commons.syntax.Constant;
import net.sf.tweety.logics.commons.syntax.Predicate;
import net.sf.tweety.logics.commons.syntax.Sort;
import net.sf.tweety.logics.commons.syntax.Variable;
import net.sf.tweety.logics.commons.syntax.interfaces.Term;
import net.sf.tweety.logics.fol.parser.FolParser;
import net.sf.tweety.logics.fol.syntax.FolAtom;
import net.sf.tweety.logics.fol.syntax.FolFormula;
import net.sf.tweety.logics.fol.syntax.FolSignature;
import net.sf.tweety.logics.fol.syntax.Negation;
import utfpr.edu.bbgp.extended.AspicArgumentationTheoryFol;
import utfpr.edu.bbgp.extended.DefeasibleInferenceRuleWithId;
import utfpr.edu.bbgp.extended.StrictInferenceRuleWithId;
import utfpr.edu.bbgp.simul.utils.FolFormulaUtils;

/**
 *
 * @author henri
 */
public class Agent {

    public final static String GOAL_SORT_TEXT = "Goals";

    public final static String NOT_HAS_INCOMPATIBILITY_STR = "_not_has_incompatibility";
    public final static String MOST_VALUABLE_STR = "_most_valuable";
    public final static String CHOOSEN_STR = "_choosen";
    public final static String HAS_PLANS_FOR_STR = "_has_plans_for";
    public final static String SATISFIED_CONTEXT_STR = "_satisfied_context";
    public final static String EXECUTIVE_STR = "_executive";
    public final static String GOAL_PLACE_HOLDER_PRED_STR = "_goalPlaceHolder";
    public final static String GOAL_PLACE_HOLDER_CONST_STR = "gHolder";
    public final static String GOAL_PLACE_HOLDER_VAR_STR = "G";
    public final static String TYPE_PLACE_HOLDER_PRED_STR = "_typeHolder";

    public final static String TYPE_SORT_TEXT = "Type";

    public final static String INCOMPATIBLE_STR = "_incompatible";
    public final static String PREFERRED_STR = "_preferred";
    public final static String EQ_PREFERRED_STR = "_eq_preferred";
    public final static String DEEFNDS_STR = "_defends";

    private Boolean goalPreferenceFirst = false;

    // Valid predicates, variables and constants
    private FolSignature signature;
    private Sort goalSort;
    private Sort typeSort;

    private Long agentCycle;

    // Beliefs
    private AspicArgumentationTheoryFol beliefs;

    //Rules
    private AspicArgumentationTheoryFol standardRules;
    private AspicArgumentationTheoryFol activationRules;
    private AspicArgumentationTheoryFol evaluationRules;
    private AspicArgumentationTheoryFol deliberationRules;
    private AspicArgumentationTheoryFol checkingRules;

    // Resources
    private final ResourceManager resources;
    // Goals
    private final GoalManager goals;
    // Plans
    private HashMap<SleepingGoal, ArrayList<Plan>> planLib;
    private ArrayList<Intention> activeIntentions;
    private Integer intentionListPointer;

    private LinkedList<FolFormula> beliefAdditionQueue;
    private LinkedList<FolFormula> beliefDeletionQueue;
//    private LinkedList<FolFormula> perceptionQueue;

    private ArrayList<GoalMemory> goalMemory;

    private HashMap<String, String> argumentToIdMap;
    private HashMap<String, String> conclusionToIdMap;

    private Integer idle = 0;
    private boolean changed;

    public Agent() {
        signature = new FolSignature();
        goalSort = new Sort(GOAL_SORT_TEXT);
        goals = new GoalManager(this);

        typeSort = new Sort(TYPE_SORT_TEXT);

        resources = new ResourceManager();

        planLib = new HashMap<>();
        activeIntentions = new ArrayList<>();
        intentionListPointer = 0;

        agentCycle = 0l;

        FolFormulaGenerator rfgen = new FolFormulaGenerator();
        beliefs = new AspicArgumentationTheoryFol(rfgen);
        standardRules = new AspicArgumentationTheoryFol(rfgen);
        activationRules = new AspicArgumentationTheoryFol(rfgen);
        evaluationRules = new AspicArgumentationTheoryFol(rfgen);
        deliberationRules = new AspicArgumentationTheoryFol(rfgen);
        checkingRules = new AspicArgumentationTheoryFol(rfgen);

        beliefAdditionQueue = new LinkedList<>();
        beliefDeletionQueue = new LinkedList<>();
//        perceptionQueue = new LinkedList<>();

        goalMemory = new ArrayList<>();

        argumentToIdMap = new HashMap<>();
        conclusionToIdMap = new HashMap<>();

        initializeBases();
    }

    public Agent(FolSignature folSignature, AspicArgumentationTheoryFol beliefs, AspicArgumentationTheoryFol standardRules, AspicArgumentationTheoryFol activationRules, AspicArgumentationTheoryFol evaluationRules) {
        this();
        this.signature.addSignature(folSignature);

        Integer count = 1;
        for (InferenceRule rule : beliefs) {
            String id = "" + rule.getIdentifier();
            String ruleId = "r_" + id;
            if (id.matches("(-)?[0-9]+") || id.isBlank()) {
                id = "bel_".concat("000".substring(3 - count.toString().length()).concat(count.toString()));
                ruleId = "r_be^".concat("000".substring(3 - count.toString().length()).concat(count.toString()));
                count++;
            }

            InferenceRule rule2;

            if (rule instanceof StrictInferenceRule) {
                rule2 = new StrictInferenceRuleWithId((StrictInferenceRule) rule).setRuleId(ruleId);
            } else if (rule instanceof DefeasibleInferenceRule) {
                rule2 = new DefeasibleInferenceRuleWithId((DefeasibleInferenceRule) rule).setRuleId(ruleId);
            } else {
                rule2 = rule;
            }

            conclusionToIdMap.put(rule2.getConclusion().toString(), id);

            this.beliefs.add(rule2);
        }

        count = 1;
        for (InferenceRule rule : standardRules) {
            String id = "" + rule.getIdentifier();
            if (id.matches("(-)?[0-9]+") || id.isBlank()) {
                id = "R_st^".concat("000".substring(3 - count.toString().length()).concat(count.toString()));
                count++;
            }

            InferenceRule rule2;

            if (rule instanceof StrictInferenceRule) {
                rule2 = new StrictInferenceRuleWithId((StrictInferenceRule) rule).setRuleId(id);
            } else if (rule instanceof DefeasibleInferenceRule) {
                rule2 = new DefeasibleInferenceRuleWithId((DefeasibleInferenceRule) rule).setRuleId(id);
            } else {
                rule2 = rule;
            }

            this.standardRules.add(rule2);
        }

        count = 1;
        for (InferenceRule rule : activationRules) {
            String id = "" + rule.getIdentifier();
            if (id.matches("(-)?[0-9]+") || id.isBlank()) {
                id = "R_ac^".concat("000".substring(3 - count.toString().length()).concat(count.toString()));
                count++;
            }

            InferenceRule rule2;

            if (rule instanceof StrictInferenceRule) {
                rule2 = new StrictInferenceRuleWithId((StrictInferenceRule) rule).setRuleId(id);
            } else if (rule instanceof DefeasibleInferenceRule) {
                rule2 = new DefeasibleInferenceRuleWithId((DefeasibleInferenceRule) rule).setRuleId(id);
            } else {
                rule2 = rule;
            }

            this.activationRules.add(rule2);
        }

        count = 1;
        for (InferenceRule rule : evaluationRules) {
            String id = "" + rule.getIdentifier();
            if (id.matches("(-)?[0-9]+") || id.isBlank()) {
                id = "R_ev^".concat("000".substring(3 - count.toString().length()).concat(count.toString()));
                count++;
            }

            InferenceRule rule2;

            if (rule instanceof StrictInferenceRule) {
                rule2 = new StrictInferenceRuleWithId((StrictInferenceRule) rule).setRuleId(id);
            } else if (rule instanceof DefeasibleInferenceRule) {
                rule2 = new DefeasibleInferenceRuleWithId((DefeasibleInferenceRule) rule).setRuleId(id);
            } else {
                rule2 = rule;
            }

            this.evaluationRules.add(rule2);
        }
    }

    Constant gHolder = null;
    protected void initializeBases() {
        Variable gVar1 = null;
        Variable gVar2 = null;
        Variable gVar3 = null;

        for (Constant c : goalSort.getTerms(Constant.class)) {
            if (c.getSort().equals(goalSort)) {
                gHolder = c;
                break;
            }
        }

        for (Variable v : goalSort.getTerms(Variable.class)) {
            if (v.getSort().equals(goalSort)) {
                if (gVar1 == null) {
                    gVar1 = v;
                } else if (gVar2 == null) {
                    gVar2 = v;
                } else if (gVar3 == null) {
                    gVar3 = v;
                    break;
                }
            }
        }

        typeSort.add(new Constant("none", typeSort));
        for (byte i = 1; i < 8; i++) {
            String s = "";
            if ((i & 4) == 4) {
                s += "t";
            }
            if ((i & 2) == 2) {
                s += "r";
            }
            if ((i & (byte) 1) == 1) {
                s += "s";
            }
            if (s.equals("")) {
                System.err.println("Empty constant wrongly instantiate. [Agent.initializeBases()]");
            }

            typeSort.add(new Constant(s, typeSort));
        }

        Variable tVar = new Variable("T", typeSort);
        typeSort.add(tVar);

        if (gHolder == null) {
            gHolder = new Constant(GOAL_PLACE_HOLDER_CONST_STR, goalSort);
            goalSort.add(gHolder);
        }

        if (gVar1 == null) {
            gVar1 = new Variable(GOAL_PLACE_HOLDER_VAR_STR, goalSort);
            goalSort.add(gVar1);
        }

        if (gVar2 == null) {
            gVar2 = new Variable(GOAL_PLACE_HOLDER_VAR_STR + "*", goalSort);
            goalSort.add(gVar2);
        }

        if (gVar3 == null) {
            gVar3 = new Variable(GOAL_PLACE_HOLDER_VAR_STR + "**", goalSort);
            goalSort.add(gVar3);
        }

        Constant tHolder = null;
        for (Constant c : typeSort.getTerms(Constant.class)) {
            if (c.getSort().equals(typeSort) && (tHolder == null || c.get().equals("none"))) {
                tHolder = c;

                if (c.get().equals("none")) {
                    break;
                }
            }
        }

        Predicate goalPlaceHolder = new Predicate(GOAL_PLACE_HOLDER_PRED_STR, Arrays.asList(goalSort));

        signature.add(goalPlaceHolder);

        Predicate typePlaceHolder = new Predicate(TYPE_PLACE_HOLDER_PRED_STR, Arrays.asList(typeSort));

        signature.add(typePlaceHolder);

        beliefs.add(new DefeasibleInferenceRule<>(new FolAtom(goalPlaceHolder, gHolder), new ArrayList<>()));
        beliefs.add(new DefeasibleInferenceRule<>(new FolAtom(typePlaceHolder, tHolder), new ArrayList<>()));

        Predicate notHasIncompatibility = new Predicate(NOT_HAS_INCOMPATIBILITY_STR, Arrays.asList(goalSort));
        Predicate mostValuable = new Predicate(MOST_VALUABLE_STR, Arrays.asList(goalSort));
        Predicate choosen = new Predicate(CHOOSEN_STR, Arrays.asList(goalSort));
        Predicate hasPlansFor = new Predicate(HAS_PLANS_FOR_STR, Arrays.asList(goalSort));
        Predicate satisfiedContext = new Predicate(SATISFIED_CONTEXT_STR, Arrays.asList(goalSort));
        Predicate executive = new Predicate(EXECUTIVE_STR, Arrays.asList(goalSort));

        Predicate incompatible = new Predicate(INCOMPATIBLE_STR, Arrays.asList(goalSort, goalSort, typeSort));
        Predicate preferred = new Predicate(PREFERRED_STR, Arrays.asList(goalSort, goalSort));
        Predicate eqPreferred = new Predicate(EQ_PREFERRED_STR, Arrays.asList(goalSort, goalSort));
        Predicate defends = new Predicate(DEEFNDS_STR, Arrays.asList(goalSort, goalSort, goalSort));

        signature.add(notHasIncompatibility);
        signature.add(mostValuable);
        signature.add(choosen);
        signature.add(hasPlansFor);
        signature.add(satisfiedContext);
        signature.add(executive);

        signature.add(incompatible);
        signature.add(preferred);
        signature.add(eqPreferred);
        signature.add(defends);

        StrictInferenceRuleWithId<FolFormula> rule = new StrictInferenceRuleWithId<>(new FolAtom(choosen, gVar1), Arrays.asList((FolFormula) new FolAtom(mostValuable, gVar1)));
        deliberationRules.add(rule);
        rule.setRuleId("R_de^009");

        rule = new StrictInferenceRuleWithId<>(new FolAtom(choosen, gVar1), Arrays.asList((FolFormula) new FolAtom(new FolAtom(notHasIncompatibility, gVar1))));
        deliberationRules.add(rule);
        rule.setRuleId("R_de^001");

        rule = new StrictInferenceRuleWithId<>(new FolAtom(choosen, gVar1), Arrays.asList((FolFormula) new FolAtom(incompatible, gVar1, gVar2, tVar), (FolFormula) new FolAtom(preferred, gVar1, gVar2)));
        deliberationRules.add(rule);
        rule.setRuleId("R_de^002");

        rule = new StrictInferenceRuleWithId<>(new FolAtom(choosen, gVar1), Arrays.asList((FolFormula) new FolAtom(incompatible, gVar1, gVar2, tVar), (FolFormula) new FolAtom(eqPreferred, gVar1, gVar2)));
        deliberationRules.add(rule);
        rule.setRuleId("R_de^003");

        rule = new StrictInferenceRuleWithId<>(new FolAtom(choosen, gVar1), Arrays.asList((FolFormula) new FolAtom(incompatible, gVar1, gVar2, tVar), (FolFormula) new FolAtom(defends, gVar3, gVar1, gVar2)));
        deliberationRules.add(rule);
        rule.setRuleId("R_de^004");

        rule = new StrictInferenceRuleWithId<>(new FolAtom(executive, gVar1), Arrays.asList(new FolAtom(hasPlansFor, gVar1), new FolAtom(satisfiedContext, gVar1)));
        checkingRules.add(rule);
        rule.setRuleId("R_ch^001");

    }

    public Constant getNextGoalConstant() {
        Set<Term<?>> terms = signature.getSort(GOAL_SORT_TEXT).getTerms();

        String last = "g0000";

        for (Term t : terms) {
            if (t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)) {
                if (t instanceof Constant && !t.get().equals(GOAL_PLACE_HOLDER_CONST_STR)) {
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

    public Integer idleCyclesCount() {
        return idle;
    }

    protected AspicArgumentationTheoryFol evaluateCompetency() {
        AspicArgumentationTheoryFol theory = new AspicArgumentationTheoryFol(new FolFormulaGenerator());

        FolParser fParser = new FolParser();
        fParser.setSignature(signature);

        for (Goal pursuableGoal : goals.getGoalAtLeastAtStage(GoalStage.Pursuable)) {
            if (!planLib.get(pursuableGoal.getGoalBase()).isEmpty()) {

                try {
                    theory.addAxiom(fParser.parseFormula(HAS_PLANS_FOR_STR + "(" + pursuableGoal.getGoalTerm().get() + ")"));
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

    protected Extension getCompatibleGoalsExtension(Collection<Extension> models, HashMap<Argument, Goal> argToGoalMap) {
        if (argToGoalMap == null) {
            throw new NullPointerException("argToGoalMap is required.");
        }
        if (models == null) {
            throw new NullPointerException("models is required.");
        }
        Extension ret = null;

        if (models.size() <= 1) {
            return models.iterator().next();
        }

        Collection<Extension> col1 = new ArrayList<>();

        int ax = 0;
        double dAx = 0.0;

        for (Extension ext : models) {
            if (goalPreferenceFirst) {
                if (ext.size() > ax) {
                    col1.clear();
                    col1.add(ext);
                    ax = ext.size();
                } else if (ext.size() == ax) {
                    col1.add(ext);
                }
            } else {
                double prefCount = 0.0;
                for (Argument arg : ext) {
                    Goal g = argToGoalMap.get(arg);

                    if (g != null) {
                        prefCount += g.getGoalBase().getPreference();
                    }
                }

                if (prefCount > dAx) {
                    col1.clear();
                    col1.add(ext);
                    dAx = prefCount;
                } else if (prefCount == dAx) {
                    col1.add(ext);
                }
            }
        }

        if (col1.size() <= 1) {
            return col1.iterator().next();
        }

        Collection<Extension> col2 = new ArrayList<>();

        ax = 0;
        dAx = 0.0;

        for (Extension ext : col1) {
            if (!goalPreferenceFirst) {
                if (ext.size() > ax) {
                    col2.clear();
                    col2.add(ext);
                    ax = ext.size();
                } else if (ext.size() == ax) {
                    col2.add(ext);
                }
            } else {
                double prefCount = 0.0;
                for (Argument arg : ext) {
                    Goal g = argToGoalMap.get(arg);

                    if (g != null) {
                        prefCount += g.getGoalBase().getPreference();
                    }
                }

                if (prefCount > dAx) {
                    col2.clear();
                    col2.add(ext);
                    dAx = prefCount;
                } else if (prefCount == dAx) {
                    col2.add(ext);
                }
            }
        }

        ret = col2.iterator().next();

        return ret;
    }

    protected AspicArgumentationTheoryFol evaluateIncompatibilities() {
        AspicArgumentationTheoryFol theory = new AspicArgumentationTheoryFol(new FolFormulaGenerator());

        // Identify terminal, resource and superfluidity attacks
        // terminal : conflict in the belief (be_operative and ¬be_operative)
        // resource : not enough resource
        // superfluidity : same concuclusion or sub arguments of an argument with same conclusion (as long as there isn't the same subargument among them)
        HashMap<Argument, Goal> argToGoalMap = new HashMap<>();
        DungTheory attackTheory = new DungTheory();

        FolParser fParser = new FolParser();
        fParser.setSignature(signature);

        // for each goal
        for (Goal g : goals.getGoalByStage(GoalStage.Pursuable)) {
            // for each plan
            int pos = 0;
            for (Plan p : planLib.get(g.getGoalBase())) {
                // create an argument
                Argument arg = new Argument(g.getFullPredicate().toString() + "_" + (pos));

                pos++;

                argToGoalMap.put(arg, g);

                attackTheory.add(arg);
            }
        }

        for (Goal g : goals.getGoalByStage(GoalStage.Choosen)) {
            Argument arg = new Argument(g.getFullPredicate().toString() + "_" + (g.getSugestedPlanIndex()));

            argToGoalMap.put(arg, g);

            attackTheory.add(arg);
        }

        for (Goal g : goals.getGoalByStage(GoalStage.Executive)) {
            Argument arg = new Argument(g.getFullPredicate().toString() + "_" + (g.getSugestedPlanIndex()));

            argToGoalMap.put(arg, g);

            attackTheory.add(arg);
        }

        Argument[] args = argToGoalMap.keySet().toArray(new Argument[]{});
        // for each pair of arguments
        for (int k = 0; k < args.length - 1; k++) {
            for (int j = k + 1; j < args.length; j++) {
                Goal gK = argToGoalMap.get(args[k]);
                Goal gJ = argToGoalMap.get(args[j]);

                String[] ax = args[k].getName().split("_");
                Plan pGK = planLib.get(gK.getGoalBase()).get(Integer.parseInt(ax[ax.length - 1]));
                ax = args[j].getName().split("_");
                Plan pGJ = planLib.get(gJ.getGoalBase()).get(Integer.parseInt(ax[ax.length - 1]));

                boolean addAttack = false;
                String attackType = "";
                // if their context is incompatible add an attack
                if (!isBeliefCompatible(pGK.getBeliefContext(), pGJ.getBeliefContext())) {
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
                    double gJPref = (gJ.getStage() == GoalStage.Pursuable ? gJ.getGoalBase().getPreference() : 1.1);

                    if (gJPref > gKPref) {
                        attackTheory.addAttack(args[j], args[k]);

                        try {
                            theory.addAxiom(fParser.parseFormula(PREFERRED_STR + "(" + argToGoalMap.get(args[j]).getGoalTerm().get() + ", " + argToGoalMap.get(args[k]).getGoalTerm().get() + ")"));
                        } catch (IOException | ParserException ex) {
                            Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else if (gJPref < gKPref) {
                        attackTheory.addAttack(args[k], args[j]);

                        try {
                            theory.addAxiom(fParser.parseFormula(PREFERRED_STR + "(" + argToGoalMap.get(args[k]).getGoalTerm().get() + ", " + argToGoalMap.get(args[j]).getGoalTerm().get() + ")"));
                        } catch (IOException | ParserException ex) {
                            Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else if (gKPref <= 1.0) {
                        attackTheory.addAttack(args[j], args[k]);
                        attackTheory.addAttack(args[k], args[j]);

                        try {
                            theory.addAxiom(fParser.parseFormula(EQ_PREFERRED_STR + "(" + argToGoalMap.get(args[k]).getGoalTerm().get() + ", " + argToGoalMap.get(args[j]).getGoalTerm().get() + ")"));
                            theory.addAxiom(fParser.parseFormula(EQ_PREFERRED_STR + "(" + argToGoalMap.get(args[j]).getGoalTerm().get() + ", " + argToGoalMap.get(args[k]).getGoalTerm().get() + ")"));
                        } catch (IOException | ParserException ex) {
                            Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    try {
                        theory.addAxiom(fParser.parseFormula(INCOMPATIBLE_STR + "(" + argToGoalMap.get(args[k]).getGoalTerm().get() + ", " + argToGoalMap.get(args[j]).getGoalTerm().get() + "," + attackType + ")"));
                        theory.addAxiom(fParser.parseFormula(INCOMPATIBLE_STR + "(" + argToGoalMap.get(args[j]).getGoalTerm().get() + ", " + argToGoalMap.get(args[k]).getGoalTerm().get() + "," + attackType + ")"));
                    } catch (IOException | ParserException ex) {
                        Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

        // Run preferred semantic
        AbstractExtensionReasoner reasoner = AbstractExtensionReasoner.getSimpleReasonerForSemantics(Semantics.CONFLICTFREE_SEMANTICS);

        Collection<Extension> models = reasoner.getModels(attackTheory);

        // choose extension
        Extension compatibleGoals = getCompatibleGoalsExtension(models, argToGoalMap);

        for (Argument arg : compatibleGoals) {
            try {
                String[] ax = arg.getName().split("_");

                if (attackTheory.getAttackers(arg).isEmpty() && attackTheory.getAttacked(arg).isEmpty()) {
                    argToGoalMap.get(arg).setSugestedPlanIndex(Integer.parseInt(ax[ax.length - 1]));
                    theory.addAxiom(fParser.parseFormula(NOT_HAS_INCOMPATIBILITY_STR + "(" + argToGoalMap.get(arg).getGoalTerm().get() + ")"));
                    continue;
                }

                for (Argument attacker : attackTheory.getAttackers(arg)) {
                    for (Argument defender : attackTheory.getAttackers(attacker)) {
                        theory.addAxiom(fParser.parseFormula(DEEFNDS_STR + "(" + argToGoalMap.get(defender).getGoalTerm().get() + ", " + argToGoalMap.get(arg).getGoalTerm().get() + ", " + argToGoalMap.get(attacker).getGoalTerm().get() + ")"));
                    }
                }

//                argToGoalMap.get(arg).setSugestedPlanIndex(Integer.parseInt(ax[ax.length - 1]));
//                theory.addAxiom(fParser.parseFormula(NOT_HAS_INCOMPATIBILITY_STR+"(" + argToGoalMap.get(arg).getGoalTerm().get() + ")"));
            } catch (IOException | ParserException ex) {
                Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return theory;
    }

    protected AspicArgumentationTheoryFol evaluateValue() {
        AspicArgumentationTheoryFol theory = new AspicArgumentationTheoryFol(new FolFormulaGenerator());

        // para cada grupo de objetivos incompatíveis
        // adiciona a crença de maior valor ao objetivo devido
        // was incorporate on evaluateIncompatibilities() as by the article.
        return theory;
    }

    protected AspicArgumentationTheoryFol evaluateContext() {
        AspicArgumentationTheoryFol theory = new AspicArgumentationTheoryFol(new FolFormulaGenerator());

        FolParser fParser = new FolParser();
        fParser.setSignature(signature);

        AspicArgumentationTheoryFol evalTheory = new AspicArgumentationTheoryFol(new FolFormulaGenerator());
        evalTheory.addAll(beliefs);
        evalTheory.addAll(standardRules);

        AbstractExtensionReasoner simpleReasoner = AbstractExtensionReasoner.getSimpleReasonerForSemantics(Semantics.PREFERRED_SEMANTICS);
        Collection<Extension> models = simpleReasoner.getModels(evalTheory.asDungTheory());

        for (Goal choosenGoal : goals.getGoalByStage(GoalStage.Choosen)) {
            if (!planLib.get(choosenGoal.getGoalBase()).isEmpty()) {
                boolean validContext = false;

                for (Plan p : planLib.get(choosenGoal.getGoalBase())) {
                    for (Extension ext : models) {
                        HashSet<FolFormula> extensionConclusions = new HashSet<>(ext.size());

                        for (Argument arg : ext) {
                            if (arg instanceof AspicArgument) {
                                AspicArgument<FolFormula> aarg = (AspicArgument) arg;

                                extensionConclusions.add(aarg.getConclusion());
                            }
                        }

                        if (extensionConclusions.containsAll(p.getBeliefContext())) {
                            if (resources.isAvaliable(p.getResourceContext())) {
                                validContext = true;
                            }
                        }
                    }
                }
                try {
                    theory.addAxiom(fParser.parseFormula((validContext ? "" : "!") + SATISFIED_CONTEXT_STR + "(" + choosenGoal.getGoalTerm().get() + ")"));
                } catch (IOException | ParserException ex) {
                    Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return theory;
    }

    public void perceive() {
//        while (!perceptionQueue.isEmpty()) {
//            FolFormula formula = perceptionQueue.pop();
//
//            beliefs.addOrdinaryPremise(formula);
//        }
        while (!beliefAdditionQueue.isEmpty()) {
            FolFormula formula = beliefAdditionQueue.pop();
            beliefs.addOrdinaryPremise(formula);
        }

        while (!beliefDeletionQueue.isEmpty()) {
            FolFormula formula = beliefDeletionQueue.pop();
            beliefs.removeIf((InferenceRule<FolFormula> arg0) -> {
                if (arg0.getPremise().isEmpty()) {
                    if (formula.toString().equals(arg0.getConclusion().toString())) {
                        return true;
                    }
                }

                return false;
            });
        }
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
        AspicArgumentationTheoryFol theory = new AspicArgumentationTheoryFol(new FolFormulaGenerator());
        theory.addAll(beliefs);
        theory.addAll(standardRules);
        theory.addAll(activationRules);

        AbstractExtensionReasoner prefSemantic = AbstractExtensionReasoner.getSimpleReasonerForSemantics(Semantics.PREFERRED_SEMANTICS);

        DungTheory dTheory = theory.asDungTheory();
        Extension selected = selectExtension_ActivationStage(prefSemantic.getModels(dTheory));

        HashMap<Goal, ArrayList<Argument>> argumentsForGoal = new HashMap<>();

//        for (Argument arg : selected) {
        for (Argument arg : dTheory) {
            if (arg instanceof AspicArgument) {
                AspicArgument<FolFormula> aarg = (AspicArgument) arg;

                FolFormula conc = aarg.getConclusion();

                if (conc instanceof FolAtom) {
                    if (((FolAtom) conc).getPredicate().getName().equals(GOAL_PLACE_HOLDER_PRED_STR)) {
                        continue;
                    }
                }

                for (Term t : conc.getTerms()) {
                    if (t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)) {
                        boolean accepted = selected.contains(arg);

                        if (accepted) {
                            Goal g = goals.createGoal(((FolAtom) conc).getPredicate(), (FolAtom) conc);

                            if (g != null) {
//                                goalMemory.add(new GoalMemory(agentCycle, g, arg, dTheory, selected));

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
                goalMemory.add(new GoalMemory(this, agentCycle, g, true, targets, dTheory, selected));
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
        AspicArgumentationTheoryFol theory = new AspicArgumentationTheoryFol(new FolFormulaGenerator());
        theory.addAll(beliefs);
        theory.addAll(standardRules);
        theory.addAll(evaluationRules);

        AbstractExtensionReasoner prefSemantic = AbstractExtensionReasoner.getSimpleReasonerForSemantics(Semantics.PREFERRED_SEMANTICS);

        DungTheory dTheory = theory.asDungTheory();
        Extension selected = selectExtension_EvaluationStage(prefSemantic.getModels(dTheory));

        HashSet<Goal> activeGoals = (HashSet) goals.getGoalByStage(GoalStage.Active);

        Set<Goal> active = goals.getGoalByStage(GoalStage.Active);
        HashMap<Goal, ArrayList<Argument>> argumentsForGoal = new HashMap<>();

//        for (Argument arg : selected) {
        for (Argument arg : dTheory) {
            if (arg instanceof AspicArgument) {
                AspicArgument<FolFormula> aarg = (AspicArgument) arg;

                FolFormula conc = aarg.getConclusion();

                for (Term t : conc.getTerms()) {
                    if (t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)) {
                        ArrayList<Goal> rem = new ArrayList<>();

                        for (Goal gAx : activeGoals) {
                            if (FolFormulaUtils.equalsWithSubstitution(gAx.getFullPredicate(), (FolFormula) conc.complement(), gHolder)) {
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
//            goalMemory.add(new GoalMemory(agentCycle, g, null, null, selected));
        }

        for (Goal g : active) {
            List<Argument> targets = null;
            if (argumentsForGoal.containsKey(g)) {
                targets = argumentsForGoal.get(g);
                if (g.getStage() == GoalStage.Pursuable) {
                    goalMemory.add(new GoalMemory(this, agentCycle, g, true, targets, dTheory, selected));
                    continue;
                }
            }

            Goal gClone = g.clone();
            gClone.setStage(GoalStage.Pursuable);
            goalMemory.add(new GoalMemory(this, agentCycle, gClone, true, targets, dTheory, selected));
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

    protected void deliberationStage(AspicArgumentationTheoryFol competence, AspicArgumentationTheoryFol incompetence, AspicArgumentationTheoryFol value) {
        AspicArgumentationTheoryFol theory = new AspicArgumentationTheoryFol(new FolFormulaGenerator());
        theory.addAll(beliefs);
        theory.addAll(standardRules);
        theory.addAll(deliberationRules);
        theory.addAll(competence);
        theory.addAll(incompetence);
        theory.addAll(value);

        AbstractExtensionReasoner prefSemantic = AbstractExtensionReasoner.getSimpleReasonerForSemantics(Semantics.PREFERRED_SEMANTICS);

        DungTheory dTheory = theory.asDungTheory();
        Extension selected = selectExtension_DeliberationStage(prefSemantic.getModels(dTheory));

//        HashSet<Goal> choosenGoals = new HashSet<>();
        Set<Goal> pursuable = goals.getGoalByStage(GoalStage.Pursuable);
        HashMap<Goal, ArrayList<Argument>> argumentsForGoal = new HashMap<>();

//        for (Argument arg : selected) {
        for (Argument arg : dTheory) {
            if (arg instanceof AspicArgument) {
                AspicArgument<FolFormula> aarg = (AspicArgument) arg;

                FolFormula conc = aarg.getConclusion();

                if (conc instanceof FolAtom) {
                    if (((FolAtom) conc).getPredicate().getName().equals(CHOOSEN_STR)) {
                        for (Term t : conc.getTerms()) {
                            if (t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)) {
                                for (Goal gAx : pursuable) {
                                    if (gAx.getGoalTerm().equals(t)) {
//                                        choosenGoals.add(gAx);
                                        boolean accepted = selected.contains(arg);

                                        ArrayList<Argument> list = argumentsForGoal.get(gAx);
                                        if (list == null) {
                                            list = new ArrayList<>();
                                            argumentsForGoal.put(gAx, list);
                                        }

                                        list.add(arg);

                                        if (accepted) {
                                            gAx.setStage(GoalStage.Choosen);
                                            changed = true;
//                                            goalMemory.add(new GoalMemory(agentCycle, gAx, arg, dTheory, selected));
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
                if (g.getStage() == GoalStage.Choosen) {
                    goalMemory.add(new GoalMemory(this, agentCycle, g, true, targets, dTheory, selected));
                    continue;
                }
            }

            Goal gClone = g.clone();
            gClone.setStage(GoalStage.Choosen);
            goalMemory.add(new GoalMemory(this, agentCycle, gClone, false, targets, dTheory, selected));
        }

//        for (Goal g : choosenGoals) {
//            g.setStage(GoalStage.Chosen);
//
//        }
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
        AspicArgumentationTheoryFol theory = new AspicArgumentationTheoryFol(new FolFormulaGenerator());
        theory.addAll(beliefs);
        theory.addAll(standardRules);
        theory.addAll(checkingRules);
        theory.addAll(competence);
        theory.addAll(context);

        AbstractExtensionReasoner prefSemantic = AbstractExtensionReasoner.getSimpleReasonerForSemantics(Semantics.PREFERRED_SEMANTICS);

        DungTheory dTheory = theory.asDungTheory();
        Extension selected = selectExtension_CheckingStage(prefSemantic.getModels(dTheory));

        HashSet<Goal> executiveGoals = new HashSet<>();

        Set<Goal> choosen = goals.getGoalByStage(GoalStage.Choosen);
        HashMap<Goal, ArrayList<Argument>> argumentsForGoal = new HashMap<>();

//        for (Argument arg : selected) {
        for (Argument arg : dTheory) {
            if (arg instanceof AspicArgument) {
                AspicArgument<FolFormula> aarg = (AspicArgument) arg;

                FolFormula conc = aarg.getConclusion();

                if (conc instanceof FolAtom) {
                    if (((FolAtom) conc).getPredicate().getName().equals(EXECUTIVE_STR)) {
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

//                                        goalMemory.add(new GoalMemory(agentCycle, gAx, arg, dTheory, selected));
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
                    goalMemory.add(new GoalMemory(this, agentCycle, g, true, targets, dTheory, selected));
                    continue;
                }
            }

            Goal gClone = g.clone();
            gClone.setStage(GoalStage.Executive);
            goalMemory.add(new GoalMemory(this, agentCycle, gClone, false, targets, dTheory, selected));
        }

        for (Goal g : executiveGoals) {
//            g.setStage(GoalStage.Executive);
            // save as intention
            Plan p = planLib.get(g.getGoalBase()).get(g.getSugestedPlanIndex());

            Intention newInt = new Intention(this, g, p, p.getUnifiedSet(g.getFullPredicate()));

            activeIntentions.add(newInt);
        }

    }

    public void processGoals() {
        changed = false;

        activationStage();

        evaluationStage();

        AspicArgumentationTheoryFol competence = evaluateCompetency();
        AspicArgumentationTheoryFol incompatibility = evaluateIncompatibilities();
        AspicArgumentationTheoryFol value = evaluateValue();

        deliberationStage(competence, incompatibility, value);

        AspicArgumentationTheoryFol context = evaluateContext();

        checkingStage(competence, context);
    }

    public void selectIntention() {
        Intention intent = null;

        if (!activeIntentions.isEmpty()) {
            intent = activeIntentions.get(intentionListPointer % activeIntentions.size());
        }

        if (intent != null) {
            intent.executeNextStep();
            idle = 0;
        } else {
            if (!changed) {
                idle++;
            }
        }

        if (!activeIntentions.isEmpty()) {
            intentionListPointer++;
            intentionListPointer %= activeIntentions.size();
        }
    }

    public void completeIntention(Intention intention) {
        if (!activeIntentions.contains(intention)) {
            return;
        }

        FolAtom fullF = intention.getGoal().getFullPredicate();
        Plan plan = intention.getPlan();

        Map<FolFormula, Boolean> beliefChanges = plan.getUnifiedBeliefPostConditionsSet(fullF);

        for (FolFormula f : beliefChanges.keySet()) {
            if (beliefChanges.get(f)) {
                beliefAdditionQueue.add(f);
            } else {
                beliefDeletionQueue.add(f);
            }
        }

        Map<String, Double> resourceChanges = plan.getResourcePostConditionsSet();
        for (String res : resourceChanges.keySet()) {
            addResource(res, resourceChanges.get(res));
        }

        intention.getGoal().setStage(GoalStage.Completed);
        goalMemory.add(new GoalMemory(this, agentCycle, intention.getGoal(), true, null, null, null));
        activeIntentions.remove(intention);
    }

    public void doAction() {

    }

    public boolean removeBelief(FolFormula formula) {
        return beliefDeletionQueue.add(formula);
    }

    public void updateBeliefs() {
        while (!beliefDeletionQueue.isEmpty()) {
            FolFormula formula = beliefDeletionQueue.pop();

            InferenceRule toRemove = null;

            for (InferenceRule<FolFormula> rule : beliefs) {
                if (rule.getConclusion().equals(formula)) {
                    toRemove = rule;
                    break;
                }
            }

            if (toRemove != null) {
                beliefs.remove(toRemove);
            }
        }

        while (!beliefAdditionQueue.isEmpty()) {
            FolFormula formula = beliefAdditionQueue.pop();

            beliefs.addOrdinaryPremise(formula);
        }
    }

    public void singleCycle() {
        this.agentCycle++;

//        perceive();
        updateBeliefs();
        processGoals();
        selectIntention();
//        doAction();
        updateBeliefs();

    }

    public FolSignature getSignature() {
        return signature;
    }

    public Sort getGoalSort() {
        return goalSort;
    }

    public boolean addSleepingGoal(SleepingGoal sleepingGoal) {
        boolean added = goals.addSleepingGoal(sleepingGoal);

        if (added) {
            planLib.put(sleepingGoal, new ArrayList<>());
        }

        return added;
    }

    public boolean addBelief(FolFormula formula) {
        return beliefAdditionQueue.add(formula);
    }
//
//    public boolean addPerception(FolFormula formula) {
//        return perceptionQueue.add(formula);
//    }

    public boolean addPlanTemplate(Plan plan) {
        if (plan == null) {
            return false;
        }

        if (goals.contaisSleepingGoal(plan.getGoal())) {
            if (!planLib.get(plan.getGoal()).contains(plan)) {
                planLib.get(plan.getGoal()).add(plan);
                return true;
            }
        }

        return false;
    }

    public Map<SleepingGoal, ArrayList<Plan>> getPlanLibrary() {
        return planLib;
    }

    public void addResource(String resource, Double amount) {
        resources.addResource(resource, amount);
    }

    public void cancelAllExecutiveGoals() {
        for (Goal g : goals.getGoalByStage(GoalStage.Executive)) {
            g.setStage(GoalStage.Cancelled);
            goalMemory.add(new GoalMemory(this, agentCycle, g, true, null, null, null));
        }
    }

    public List<GoalMemory> getGoalMemory() {
        return goalMemory.subList(0, goalMemory.size());
    }

    public String getBeliefBaseToString() {
        String toString = "";

        for (InferenceRule<FolFormula> belief : beliefs) {
            if (belief.getConclusion() instanceof FolAtom) {
                if (((FolAtom) belief.getConclusion()).getPredicate().getName().equals(GOAL_PLACE_HOLDER_PRED_STR)) {
                    continue;
                }
                if (((FolAtom) belief.getConclusion()).getPredicate().getName().equals(TYPE_PLACE_HOLDER_PRED_STR)) {
                    continue;
                }
            }
            toString += belief.toString().trim();
            toString += "\r\n";
        }

        return toString;
    }

    public String getResourceBaseToString() {
        String toString = "";

        for (String res : resources.getAvailableResourcesNames()) {
            toString += res.trim() + " : " + resources.getAvaliability(res);
            toString += "\r\n";
        }

        return toString;
    }

    public String getStandardRulesToString() {
        String toString = "";

        for (InferenceRule<FolFormula> rule : standardRules) {
            toString += rule.toString();
            toString += "\r\n";
        }

        return toString;
    }

    public String getActivationRulesToString() {
        String toString = "";

        for (InferenceRule<FolFormula> rule : activationRules) {
            toString += rule.toString();
            toString += "\r\n";
        }

        return toString;
    }

    public String getEvaluationRulesToString() {
        String toString = "";

        for (InferenceRule<FolFormula> rule : evaluationRules) {
            toString += rule.toString();
            toString += "\r\n";
        }

        return toString;
    }

    public String getDeliberationRulesToString() {
        String toString = "";

        for (InferenceRule<FolFormula> rule : deliberationRules) {
            toString += rule.toString();
            toString += "\r\n";
        }

        return toString;
    }

    public String getCheckingRulesToString() {
        String toString = "";

        for (InferenceRule<FolFormula> rule : checkingRules) {
            toString += rule.toString();
            toString += "\r\n";
        }

        return toString;
    }

    public Long getCycle() {
        return agentCycle;
    }

    public boolean noAvailableAction() {
        if (!goals.getGoalByStage(GoalStage.Active).isEmpty()) {
            return false;
        }
        if (!goals.getGoalByStage(GoalStage.Pursuable).isEmpty()) {
            return false;
        }
        if (!goals.getGoalByStage(GoalStage.Choosen).isEmpty()) {
            return false;
        }
        if (!goals.getGoalByStage(GoalStage.Executive).isEmpty()) {
            return false;
        }

        return true;
    }

    public synchronized String getArgumentID(AspicArgument<FolFormula> arg) {
        String id = argumentToIdMap.get(arg.toString());

        if (id == null) {
            String rId = getRuleID(arg);

            if (!rId.isBlank()) {
                String size = (argumentToIdMap.size() + 1) + "";
                String count = "000".substring(3 - size.length()).concat(size);
                String[] splitUnderscore = rId.split("_");
                String type = "un";
                if (splitUnderscore.length > 1) {
                    String[] splitCirc = splitUnderscore[1].split("\\^");
                    type = splitCirc[0];
                } else if (!rId.matches("(-)?([0-9]+)")) {
                    type = rId;
                }
                id = "A_" + type + "^" + count;
                argumentToIdMap.put(arg.toString(), id);
            } else {
                String size = (argumentToIdMap.size() + 1) + "";
                String count = "000".substring(3 - size.length()).concat(size);
                id = "A_ins^" + count;;
                argumentToIdMap.put(arg.toString(), id);
            }
        }

        return id;
    }

    public synchronized String getArgumentConclusionID(AspicArgument<FolFormula> arg) {
        String id = conclusionToIdMap.get(arg.getConclusion().toString());

        if (id == null) {
            String size = (conclusionToIdMap.size() + 1) + "";
            String count = "000".substring(3 - size.length()).concat(size);
            id = "bel_" + count;
            conclusionToIdMap.put(arg.getConclusion().toString(), id);
        }

        return id;
    }

    public synchronized String getRuleID(AspicArgument<FolFormula> arg) {
        String id = "";
        if (arg.getTopRule() instanceof StrictInferenceRuleWithId) {
            id = ((StrictInferenceRuleWithId) arg.getTopRule()).getRuleId();
        } else if (arg.getTopRule() instanceof DefeasibleInferenceRuleWithId) {
            id = ((DefeasibleInferenceRuleWithId) arg.getTopRule()).getRuleId();
        }
        if (id == null) {
            return "";
        }
        return id;
    }

    public boolean isRuleStrict(AspicArgument<FolFormula> arg) {
        return !arg.getTopRule().isDefeasible();
    }

    public static FolFormula parseFolFormulaSafe(String toParse, FolSignature sign) {
        try {
            FolParser parser = new FolParser();
            parser.setSignature(sign);

            Pattern PREDICATE_PATTERN = Pattern.compile("\\s*(!\\s*)?\\s*(\\w+(\\(\\s*\\w+(\\s*,\\s*\\w+)*\\s*\\))?)\\s*");

            Matcher matcher = PREDICATE_PATTERN.matcher(toParse);
            if (matcher.matches()) {
                String termSet = matcher.group(3);
                int arity = 0;
                if (termSet != null) {
                    String[] termList = termSet.replace("(", "").replace(")", "").split(",");
                    arity = termList.length;

                    for (String term : termList) {
                        term = term.trim();
                        if (term.matches("^[a-z].*")) {
                            if (!sign.containsConstant(term)) {
                                sign.add(new Constant(term));
                            }
                        }
                    }
                }

                String predicate = matcher.group(2).replace(termSet, "").trim();
                if (!sign.containsPredicate(predicate)) {
                    sign.add(new Predicate(predicate, arity));
                }

                FolFormula formula = parser.parseFormula(matcher.group(2));

                Boolean isNegative = "!".equals(matcher.group(1));

                if (isNegative) {
                    formula = new Negation(formula);
                }

                return formula;
            }
        } catch (IOException | ParserException ex) {
            Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public void resetIdleCycleCount() {
        idle = 0;
    }
}
