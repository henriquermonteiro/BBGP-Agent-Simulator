package utfpr.edu.bbgp.agent;

import utfpr.edu.bbgp.agent.manager.ResourceManager;
import utfpr.edu.bbgp.agent.manager.GoalManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.tweety.arg.aspic.ruleformulagenerator.FolFormulaGenerator;
import net.sf.tweety.arg.aspic.syntax.AspicArgument;
import net.sf.tweety.arg.aspic.syntax.DefeasibleInferenceRule;
import net.sf.tweety.arg.aspic.syntax.InferenceRule;
import net.sf.tweety.arg.aspic.syntax.StrictInferenceRule;
import net.sf.tweety.commons.ParserException;
import net.sf.tweety.logics.commons.syntax.Constant;
import net.sf.tweety.logics.commons.syntax.Predicate;
import net.sf.tweety.logics.commons.syntax.Sort;
import net.sf.tweety.logics.commons.syntax.Variable;
import net.sf.tweety.logics.fol.parser.FolParser;
import net.sf.tweety.logics.fol.syntax.FolAtom;
import net.sf.tweety.logics.fol.syntax.FolFormula;
import net.sf.tweety.logics.fol.syntax.FolSignature;
import net.sf.tweety.logics.fol.syntax.Negation;
import utfpr.edu.bbgp.agent.manager.GoalProcessingManager;
import utfpr.edu.bbgp.extended.AspicArgumentationTheoryFol;
import utfpr.edu.bbgp.extended.DefeasibleInferenceRuleWithId;
import utfpr.edu.bbgp.extended.StrictInferenceRuleWithId;

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
    public final static String TYPE_PLACE_HOLDER_CONST_STR = "none";
    public final static String TYPE_PLACE_HOLDER_VAR_STR = "T";

    public final static String TYPE_SORT_TEXT = "Type";

    public final static String INCOMPATIBLE_STR = "_incompatible";
    public final static String PREFERRED_STR = "_preferred";
    public final static String EQ_PREFERRED_STR = "_eq_preferred";
    public final static String DEFENDS_STR = "_defends";
    public final static String PURSUED_STR = "_pursued";
    public final static String MAX_UTIL_STR = "_max_util";

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

    // Goal Processing Execution
    private final GoalProcessingManager goalProcessing;
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

    private ArrayList<GoalMemory> goalMemory;

    private HashMap<String, String> argumentToIdMap;
    private HashMap<String, String> conclusionToIdMap;

    private Integer idle = 0;

    public Agent() {
        signature = new FolSignature();
        goalSort = new Sort(GOAL_SORT_TEXT);
        goals = new GoalManager(this);

        typeSort = new Sort(TYPE_SORT_TEXT);

        goalProcessing = new GoalProcessingManager(this);

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

    private Constant gHolder = null;

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

        signature.add(new Constant(Agent.TYPE_PLACE_HOLDER_CONST_STR, typeSort));
        
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

            signature.add(new Constant(s, typeSort));
        }

        Variable tVar = new Variable(TYPE_PLACE_HOLDER_VAR_STR, typeSort);
        typeSort.add(tVar);

        if (gHolder == null) {
            gHolder = new Constant(GOAL_PLACE_HOLDER_CONST_STR, goalSort);
            signature.add(gHolder);
        }

        if (gVar1 == null) {
            gVar1 = new Variable(GOAL_PLACE_HOLDER_VAR_STR, goalSort);
            goalSort.add(gVar1);
        }

        if (gVar2 == null) {
            gVar2 = new Variable(GOAL_PLACE_HOLDER_VAR_STR + "1", goalSort);
            goalSort.add(gVar2);
        }

        if (gVar3 == null) {
            gVar3 = new Variable(GOAL_PLACE_HOLDER_VAR_STR + "2", goalSort);
            goalSort.add(gVar3);
        }

        Constant tHolder = null;
        for (Constant c : typeSort.getTerms(Constant.class)) {
            if (c.getSort().equals(typeSort) && (tHolder == null || c.get().equals(TYPE_PLACE_HOLDER_CONST_STR))) {
                tHolder = c;

                if (c.get().equals(TYPE_PLACE_HOLDER_CONST_STR)) {
                    break;
                }
            }
        }
        if (tHolder == null) {
            tHolder = new Constant(TYPE_PLACE_HOLDER_CONST_STR, typeSort);
            signature.add(tHolder);
        }

        beliefs.add(new DefeasibleInferenceRule<>(parseFolFormulaSafeFromForm(GOAL_PLACE_HOLDER_PRED_STR, gHolder.get()), new ArrayList<>()));
        beliefs.add(new DefeasibleInferenceRule<>(parseFolFormulaSafeFromForm(TYPE_PLACE_HOLDER_PRED_STR, tHolder.get()), new ArrayList<>()));

        StrictInferenceRuleWithId<FolFormula> rule;

//        rule = new StrictInferenceRuleWithId<>(new FolAtom(choosen, gVar1), Arrays.asList((FolFormula) new FolAtom(new FolAtom(notHasIncompatibility, gVar1))));
        rule = new StrictInferenceRuleWithId<>(parseFolFormulaSafeFromForm(CHOOSEN_STR, gVar1.get()),
                Arrays.asList(parseFolFormulaSafeFromForm(NOT_HAS_INCOMPATIBILITY_STR, gVar1.get())));
        deliberationRules.add(rule);
        rule.setRuleId("R_de^001");

//        rule = new StrictInferenceRuleWithId<>(new FolAtom(choosen, gVar1), Arrays.asList((FolFormula) new FolAtom(incompatible, gVar1, gVar2, tVar), (FolFormula) new FolAtom(preferred, gVar1, gVar2)));
        rule = new StrictInferenceRuleWithId<>(parseFolFormulaSafeFromForm(CHOOSEN_STR, gVar1.get()),
                Arrays.asList(parseFolFormulaSafeFromForm(INCOMPATIBLE_STR, gVar1.get(), gVar2.get(), tVar.get()),
                        parseFolFormulaSafeFromForm(PREFERRED_STR, gVar1.get(), gVar2.get())));
        deliberationRules.add(rule);
        rule.setRuleId("R_de^002");

        rule = new StrictInferenceRuleWithId<>(new Negation(parseFolFormulaSafeFromForm(CHOOSEN_STR, gVar2.get())),
                Arrays.asList(parseFolFormulaSafeFromForm(INCOMPATIBLE_STR, gVar1.get(), gVar2.get(), tVar.get()),
                        new Negation(parseFolFormulaSafeFromForm(PREFERRED_STR, gVar2.get(), gVar1.get()))));
        deliberationRules.add(rule);
        rule.setRuleId("R_de^003");

//        rule = new StrictInferenceRuleWithId<>(new FolAtom(choosen, gVar1), Arrays.asList((FolFormula) new FolAtom(incompatible, gVar1, gVar2, tVar), (FolFormula) new FolAtom(eqPreferred, gVar1, gVar2)));
        rule = new StrictInferenceRuleWithId<>(parseFolFormulaSafeFromForm(CHOOSEN_STR, gVar1.get()),
                Arrays.asList(parseFolFormulaSafeFromForm(INCOMPATIBLE_STR, gVar1.get(), gVar2.get(), tVar.get()),
                        parseFolFormulaSafeFromForm(EQ_PREFERRED_STR, gVar1.get(), gVar2.get())));
        deliberationRules.add(rule);
        rule.setRuleId("R_de^004");

//        rule = new StrictInferenceRuleWithId<>(new FolAtom(choosen, gVar1), Arrays.asList((FolFormula) new FolAtom(incompatible, gVar1, gVar2, tVar), (FolFormula) new FolAtom(defends, gVar3, gVar1, gVar2)));
        rule = new StrictInferenceRuleWithId<>(parseFolFormulaSafeFromForm(CHOOSEN_STR, gVar1.get()),
                Arrays.asList(parseFolFormulaSafeFromForm(MAX_UTIL_STR, gVar1.get())));
        deliberationRules.add(rule);
        rule.setRuleId("R_de^005");

        rule = new StrictInferenceRuleWithId<>(new Negation(parseFolFormulaSafeFromForm(CHOOSEN_STR, gVar1.get())),
                Arrays.asList(new Negation(parseFolFormulaSafeFromForm(MAX_UTIL_STR, gVar1.get()))));
        deliberationRules.add(rule);
        rule.setRuleId("R_de^006");

//        rule = new StrictInferenceRuleWithId<>(new FolAtom(executive, gVar1), Arrays.asList(new FolAtom(hasPlansFor, gVar1), new FolAtom(satisfiedContext, gVar1)));
        rule = new StrictInferenceRuleWithId<>(parseFolFormulaSafeFromForm(EXECUTIVE_STR, gVar1.get()),
                Arrays.asList(parseFolFormulaSafeFromForm(HAS_PLANS_FOR_STR, gVar1.get()),
                        parseFolFormulaSafeFromForm(SATISFIED_CONTEXT_STR, gVar1.get())));
        checkingRules.add(rule);
        rule.setRuleId("R_ch^001");

    }

    public Constant getNextGoalConstant() {
        return goalProcessing.getNextGoalConstant();
    }

    public Integer idleCyclesCount() {
        return idle;
    }

    public void selectIntention(boolean goalProcessingHadChanges) {
        Intention intent = null;

        if (!activeIntentions.isEmpty()) {
            intent = activeIntentions.get(intentionListPointer % activeIntentions.size());
        }

        if (intent != null) {
            intent.executeNextStep();
            idle = 0;
        } else {
            if (!goalProcessingHadChanges) {
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
        
        System.out.println("Completed " + intention.getGoal().getFullPredicate() + " using plan: " + intention.getUnifiedContext() + " - " + intention.getPlan().getResourceContext() + 
                " $ " + intention.getPlan().getUnifiedBeliefPostConditionsSet(intention.getGoal().getFullPredicate()));

        FolAtom fullF = intention.getGoal().getFullPredicate();
        Plan plan = intention.getPlan();

        Map<FolFormula, Boolean> beliefChanges = plan.getUnifiedBeliefPostConditionsSet(fullF);

        for (FolFormula bel : beliefChanges.keySet()) {
            FolFormula f = bel;
            if(intention.getAuxiliaryMapping() != null){
                f = (FolFormula) bel.substitute(intention.getAuxiliaryMapping());
            }
            if (beliefChanges.get(bel)) {
                beliefAdditionQueue.add(f);
            } else {
                beliefDeletionQueue.add(f);
            }
        }

        List<PerceptionEntry> resourceChanges = plan.getResourcePostConditionsSet();
        for (PerceptionEntry res : resourceChanges) {
            addResource(res.getResource(), res.getResourceAmount() * (res.getOperation() == '-' ? -1 : 1));
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

        updateBeliefs();
        boolean changed = goalProcessing.processGoals();
        selectIntention(changed);
//        doAction();
//        updateBeliefs();

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

    public void addGoalMemoryEntry(GoalMemory entry) {
        goalMemory.add(entry);
    }

    public void addIntention(Intention intention) {
        activeIntentions.add(intention);
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
    
    public String getOrderingToString() {
        String toString = "";
        
        ArrayList<SleepingGoal> sleepingGoals = new ArrayList<>(goals.getSleepingGoal());
        
        sleepingGoals.sort(((arg0, arg1) -> {
            return (int)((arg0.getPreference() - arg1.getPreference()) * 1000000);
        }));
        
        Iterator<SleepingGoal> iterator = sleepingGoals.iterator();
        SleepingGoal sG = iterator.next();
        
        if(sG != null) toString = sG.getGoalPredicate().toString();
        
        while(iterator.hasNext()){
            sG = iterator.next();
            toString = toString.concat(" < ").concat(sG.getGoalPredicate().toString());
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

    public static String formPredicate(String predicate, String... terms) {
        if (terms == null) {
            return predicate;
        }
        if (terms.length < 1) {
            return predicate;
        }
        String ret = predicate.concat("(").concat(terms[0]);
        for (int k = 1; k < terms.length; k++) {
            ret = ret.concat(", ").concat(terms[k]);
        }
        return ret.concat(")");
    }

    public FolFormula parseFolFormulaSafeFromForm(String predicate, String... terms) {
        return parseFolFormulaSafe(formPredicate(predicate, terms), signature);
    }

    public FolFormula parseFolFormulaSafe(String toParse) {
        return parseFolFormulaSafe(toParse, signature);
    }

    public static FolFormula parseFolFormulaSafe(String toParse, FolSignature sign) {
        try {
            FolParser parser = new FolParser();
            parser.setSignature(sign);

            Pattern PREDICATE_PATTERN = Pattern.compile("\\s*(!\\s*)?\\s*(\\w+(\\(\\s*\\w+(\\s*,\\s*\\w+)*\\s*\\))?)\\s*");

            Sort goalSort = null;
            for (Sort sort : sign.getSorts()) {
                if (sort.getName().equals(Agent.GOAL_SORT_TEXT)) {
                    goalSort = sort;
                }
            }
            if (goalSort == null) {
                goalSort = new Sort(Agent.GOAL_SORT_TEXT);
                sign.add(goalSort);
            }

            Sort typeSort = null;
            for (Sort sort : sign.getSorts()) {
                if (sort.getName().equals(Agent.TYPE_SORT_TEXT)) {
                    typeSort = sort;
                }
            }
            if (typeSort == null) {
                typeSort = new Sort(Agent.TYPE_SORT_TEXT);
                sign.add(typeSort);
            }

            Matcher matcher = PREDICATE_PATTERN.matcher(toParse);
            if (matcher.matches()) {
                String termSet = matcher.group(3);

                ArrayList<Sort> termsSort = new ArrayList<>();

                if (termSet != null) {
                    String[] termList = termSet.replace("(", "").replace(")", "").split(",");

                    for (String term : termList) {
                        term = term.trim();
                        if (term.matches("^[a-z].*")) {
                            if (term.equals(Agent.GOAL_PLACE_HOLDER_CONST_STR)) {
                                termsSort.add(goalSort);
                                if (!sign.containsConstant(term)) {
                                    sign.add(new Constant(term, goalSort));
                                }
                            } else if (term.equals(Agent.TYPE_PLACE_HOLDER_CONST_STR)) {
                                termsSort.add(typeSort);
                                if (!sign.containsConstant(term)) {
                                    sign.add(new Constant(term, typeSort));
                                }
                            } else {
                                termsSort.add(Sort.THING);
                                if (!sign.containsConstant(term)) {
                                    sign.add(new Constant(term));
                                }
                            }
                        } else if (term.matches("^G[0-9]*$")) {
                            termsSort.add(goalSort);
                        } else if (term.equals(Agent.TYPE_PLACE_HOLDER_VAR_STR)) {
                            termsSort.add(typeSort);
                        } else {
                            termsSort.add(Sort.THING);
                        }
                    }
                }

                String predicate = matcher.group(2).trim();
                if (!sign.containsPredicate(predicate)) {
                    if (termSet == null) {
                        sign.add(new Predicate(predicate, 0));
                    } else {
                        predicate = predicate.replace(termSet, "");
                        sign.add(new Predicate(predicate, termsSort));
                    }
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

    public Goal getGoalForID(String id) {
        return goals.getGoalbyId(id);
    }

    public GoalManager getGoalManager() {
        return goals;
    }

    public ResourceManager getResources() {
        return resources;
    }

    public GoalManager getGoals() {
        return goals;
    }

    public AspicArgumentationTheoryFol getBeliefs() {
        return beliefs;
    }

    public AspicArgumentationTheoryFol getActivationRules() {
        return activationRules;
    }

    public AspicArgumentationTheoryFol getDeliberationRules() {
        return deliberationRules;
    }

    public AspicArgumentationTheoryFol getCheckingRules() {
        return checkingRules;
    }

    public AspicArgumentationTheoryFol getStandardRules() {
        return standardRules;
    }

    public AspicArgumentationTheoryFol getEvaluationRules() {
        return evaluationRules;
    }

    public Constant getgHolder() {
        return gHolder;
    }
}
