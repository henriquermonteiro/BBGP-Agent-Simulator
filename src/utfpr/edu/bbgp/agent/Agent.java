/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.agent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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

/**
 *
 * @author henri
 */
public class Agent {

    public final static String GOAL_SORT_TEXT = "Goals";
    
    protected final static String HAS_INCOMPATIBILITY_STR = "_has_incompatibility";
    protected final static String MOST_VALUABLE_STR = "_most_valuable";
    protected final static String CHOOSEN_STR = "_choosen";
    protected final static String HAS_PLANS_FOR_STR = "_has_plans_for";
    protected final static String SATISFIED_CONTEXT_STR = "_satisfied_context";
    protected final static String EXECUTIVE_STR = "_executive";
    protected final static String GOAL_PLACE_HOLDER_PRED_STR = "_goalPlaceHolder";
    protected final static String GOAL_PLACE_HOLDER_CONST_STR = "gHolder";
    protected final static String GOAL_PLACE_HOLDER_VAR_STR = "G";

    private Boolean goalPreferenceFirst = false;

    // Valid predicates, variables and constants
    private FolSignature signature;
    private Sort goalSort;

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
    private LinkedList<FolFormula> perceptionQueue;
    
    private ArrayList<GoalMemory> goalMemory;

    public Agent() {
        signature = new FolSignature();
        goalSort = new Sort(GOAL_SORT_TEXT);
        goals = new GoalManager(this);

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
        perceptionQueue = new LinkedList<>();
        
        goalMemory = new ArrayList<>();
        
        initializeBases();
    }

    public Agent(FolSignature folSignature, AspicArgumentationTheoryFol beliefs, AspicArgumentationTheoryFol standardRules, AspicArgumentationTheoryFol activationRules, AspicArgumentationTheoryFol evaluationRules) {
        this();
        this.signature.addSignature(folSignature);
        this.beliefs.addAll(beliefs);
        this.standardRules.addAll(standardRules);
        this.activationRules.addAll(activationRules);
        this.evaluationRules.addAll(evaluationRules);
    }
    
    protected void initializeBases(){
        Constant gHolder = null;
        Variable gVar = null;
        
        for(Constant c : goalSort.getTerms(Constant.class)){
            if(c.getSort().equals(goalSort)){
                gHolder = c;
                break;
            }
        }
        
        for(Variable v : goalSort.getTerms(Variable.class)){
            if(v.getSort().equals(goalSort)){
                gVar = v;
                break;
            }
        }
        
        if(gHolder == null){
            gHolder = new Constant(GOAL_PLACE_HOLDER_CONST_STR, goalSort);
            goalSort.add(gHolder);
        }
        
        if(gVar == null){
            gVar = new Variable(GOAL_PLACE_HOLDER_VAR_STR, goalSort);
            goalSort.add(gVar);
        }
        
        Predicate goalPlaceHolder = new Predicate(GOAL_PLACE_HOLDER_PRED_STR, Arrays.asList(goalSort));
        
        signature.add(goalPlaceHolder);
        
        beliefs.add(new DefeasibleInferenceRule<>(new FolAtom(goalPlaceHolder, gHolder), new ArrayList<>()));
        
        Predicate hasIncompatibility = new Predicate(HAS_INCOMPATIBILITY_STR, Arrays.asList(goalSort));
        Predicate mostValuable = new Predicate(MOST_VALUABLE_STR, Arrays.asList(goalSort));
        Predicate choosen = new Predicate(CHOOSEN_STR, Arrays.asList(goalSort));
        Predicate hasPlansFor = new Predicate(HAS_PLANS_FOR_STR, Arrays.asList(goalSort));
        Predicate satisfiedContext = new Predicate(SATISFIED_CONTEXT_STR, Arrays.asList(goalSort));
        Predicate executive = new Predicate(EXECUTIVE_STR, Arrays.asList(goalSort));
        
        signature.add(hasIncompatibility);
        signature.add(mostValuable);
        signature.add(choosen);
        signature.add(hasPlansFor);
        signature.add(satisfiedContext);
        signature.add(executive);
        
        deliberationRules.add(new StrictInferenceRule<>(new FolAtom(choosen, gVar), Arrays.asList((FolFormula)new FolAtom(mostValuable, gVar))));
        deliberationRules.add(new StrictInferenceRule<>(new FolAtom(choosen, gVar), Arrays.asList((FolFormula)new Negation(new FolAtom(hasIncompatibility, gVar)))));
        
        checkingRules.add(new StrictInferenceRule<>(new FolAtom(executive, gVar), Arrays.asList(new FolAtom(hasPlansFor, gVar), new FolAtom(satisfiedContext, gVar))));
        
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

    protected AspicArgumentationTheoryFol evaluateCompetency() {
        AspicArgumentationTheoryFol theory = new AspicArgumentationTheoryFol(new FolFormulaGenerator());

        FolParser fParser = new FolParser();
        fParser.setSignature(signature);

        for (Goal pursuableGoal : goals.getGoalByStage(GoalStage.Pursuable)) {
            if (!planLib.get(pursuableGoal.getGoalBase()).isEmpty()) {

                try {
                    theory.addAxiom(fParser.parseFormula(HAS_PLANS_FOR_STR+"(" + pursuableGoal.getGoalTerm().get() + ")"));
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
        
        for(Goal g : goals.getGoalByStage(GoalStage.Choosen)) {
            Argument arg = new Argument(g.getFullPredicate().toString() + "_" + (g.getSugestedPlanIndex()));
            
            argToGoalMap.put(arg, g);

            attackTheory.add(arg);
        }
        
        for(Goal g : goals.getGoalByStage(GoalStage.Executive)) {
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
                // if their context is incompatible add an attack
                if (!isBeliefCompatible(pGK.getBeliefContext(), pGJ.getBeliefContext())) {
                    addAttack = true;
                }
                // if there is not enough resource for both add an attack
                if (!resources.checkCompatibility(pGK.getResourceContext(), pGJ.getResourceContext())) {
                    addAttack = true;
                }
                // if the goal is the same add an attack
                if (gK == gJ) {
                    addAttack = true;
                }

                if (addAttack) {
                    double gKPref = (gK.getStage() == GoalStage.Pursuable ? gK.getGoalBase().getPreference() : 1.1);
                    double gJPref = (gJ.getStage() == GoalStage.Pursuable ? gJ.getGoalBase().getPreference() : 1.1);
                    
                    if (gJPref > gKPref) {
                        attackTheory.addAttack(args[j], args[k]);
                    } else if(gJPref < gKPref){
                        attackTheory.addAttack(args[k], args[j]);
                    }else if(gKPref <= 1.0){
                        attackTheory.addAttack(args[j], args[k]);
                        attackTheory.addAttack(args[k], args[j]);
                    }
                }
            }
        }

        // Run preferred semantic
        AbstractExtensionReasoner reasoner = AbstractExtensionReasoner.getSimpleReasonerForSemantics(Semantics.CONFLICTFREE_SEMANTICS);

        Collection<Extension> models = reasoner.getModels(attackTheory);

        // choose extension
        Extension compatibleGoals = getCompatibleGoalsExtension(models, argToGoalMap);

        FolParser fParser = new FolParser();
        fParser.setSignature(signature);

        for (Argument arg : compatibleGoals) {
            try {
                String[] ax = arg.getName().split("_");
                argToGoalMap.get(arg).setSugestedPlanIndex(Integer.parseInt(ax[ax.length - 1]));
                theory.addAxiom(fParser.parseFormula("!"+HAS_INCOMPATIBILITY_STR+"(" + argToGoalMap.get(arg).getGoalTerm().get() + ")"));
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
                    theory.addAxiom(fParser.parseFormula((validContext ? "" : "!") + SATISFIED_CONTEXT_STR +"(" + choosenGoal.getGoalTerm().get() + ")"));
                } catch (IOException | ParserException ex) {
                    Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return theory;
    }

    public void perceive() {
        while(!perceptionQueue.isEmpty()){
            FolFormula formula = perceptionQueue.pop();
            
            beliefs.addOrdinaryPremise(formula);
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

        for (Argument arg : selected) {
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
                        Goal g = goals.createGoal(((FolAtom) conc).getPredicate(), (FolAtom) conc);
                        
                        if(g != null)
                            goalMemory.add(new GoalMemory(agentCycle, g, arg, dTheory, selected));

                        break;
                    }
                }
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

        Extension selected = selectExtension_EvaluationStage(prefSemantic.getModels(theory.asDungTheory()));

        HashSet<Goal> activeGoals = (HashSet) goals.getGoalByStage(GoalStage.Active);

        for (Argument arg : selected) {
            if (arg instanceof AspicArgument) {
                AspicArgument<FolFormula> aarg = (AspicArgument) arg;

                FolFormula conc = aarg.getConclusion();

                if (conc instanceof Negation) {
                    for (Term t : conc.getTerms()) {
                        if (t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)) {
                            ArrayList<Goal> rem = new ArrayList<>();

                            for (Goal gAx : activeGoals) {
                                if (gAx.getFullPredicate().equals(conc.complement())) {
                                    rem.add(gAx);
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
        }

        for (Goal g : activeGoals) {
            g.setStage(GoalStage.Pursuable);
            
            goalMemory.add(new GoalMemory(agentCycle, g, null, null, selected));
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

        for (Argument arg : selected) {
            if (arg instanceof AspicArgument) {
                AspicArgument<FolFormula> aarg = (AspicArgument) arg;

                FolFormula conc = aarg.getConclusion();

                if (conc instanceof FolAtom) {
                    if (((FolAtom) conc).getPredicate().getName().equals(CHOOSEN_STR)) {
                        for (Term t : conc.getTerms()) {
                            if (t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)) {
                                for (Goal gAx : goals.getGoalByStage(GoalStage.Pursuable)) {
                                    if (gAx.getGoalTerm().equals(t)) {
//                                        choosenGoals.add(gAx);
                                        
                                        gAx.setStage(GoalStage.Choosen);
                                        goalMemory.add(new GoalMemory(agentCycle, gAx, arg, dTheory, selected));
                                    }
                                }

                                break;
                            }
                        }
                    }
                }
            }
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

        for (Argument arg : selected) {
            if (arg instanceof AspicArgument) {
                AspicArgument<FolFormula> aarg = (AspicArgument) arg;

                FolFormula conc = aarg.getConclusion();

                if (conc instanceof FolAtom) {
                    if (((FolAtom) conc).getPredicate().getName().equals(EXECUTIVE_STR)) {
                        for (Term t : conc.getTerms()) {
                            if (t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)) {
                                for (Goal gAx : goals.getGoalByStage(GoalStage.Choosen)) {
                                    if (gAx.getGoalTerm().equals(t)) {
                                        executiveGoals.add(gAx);
                                        
                                        gAx.setStage(GoalStage.Executive);
                                        goalMemory.add(new GoalMemory(agentCycle, gAx, arg, dTheory, selected));
                                    }
                                }

                                break;
                            }
                        }
                    }
                }
            }
        }

        for (Goal g : executiveGoals) {
//            g.setStage(GoalStage.Executive);
            // save as intention
            Plan p = planLib.get(g.getGoalBase()).get(g.getSugestedPlanIndex());

            Intention newInt = new Intention(g, p, p.getUnifiedSet(g.getFullPredicate()));

            activeIntentions.add(newInt);
        }

    }

    public void processGoals() {
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
            intentionListPointer++;
            intentionListPointer %= activeIntentions.size();
        }

    }

    public void doAction() {

    }

    protected void removeBelief(FolFormula formula){
        InferenceRule toRemove = null;
        
        for(InferenceRule<FolFormula> rule : beliefs){
            if(rule.getConclusion().equals(formula)){
                toRemove = rule;
                break;
            }
        }
        
        if(toRemove != null){
            beliefs.remove(toRemove);
        }
    }
    
    public void updateBeliefs() {
        while(!beliefDeletionQueue.isEmpty()){
            FolFormula formula = beliefDeletionQueue.pop();
            
            removeBelief(formula);
        }
        
        while(!beliefAdditionQueue.isEmpty()){
            FolFormula formula = beliefAdditionQueue.pop();
            
            if(beliefs.contains(new DefeasibleInferenceRule<>((FolFormula)formula.complement(), null))){
                removeBelief((FolFormula)formula.complement());
            }else{
                beliefs.addOrdinaryPremise(formula);
            }
        }
    }

    public void singleCycle() {
        this.agentCycle++;
        
        perceive();
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
    
    public boolean addBelief(FolFormula formula){
        return beliefAdditionQueue.add(formula);
    }
    
    public boolean addPerception(FolFormula formula){
        return perceptionQueue.add(formula);
    }
    
    public boolean addPlanTemplate(Plan plan){
        if(plan == null) return false;
        
        if(goals.contaisSleepingGoal(plan.getGoal())){
            if(!planLib.get(plan.getGoal()).contains(plan)){
                planLib.get(plan.getGoal()).add(plan);
                return true;
            }
        }
        
        return false;
    }
    
    public void addResource(String resource, Double amount){
        resources.addResource(resource, amount);
    }
    
    public void cancelAllExecutiveGoals(){
        for(Goal g : goals.getGoalByStage(GoalStage.Executive)){
            g.setStage(GoalStage.Cancelled);
            goalMemory.add(new GoalMemory(agentCycle, g, null, null, null));
        }
    }

    public static void main(String... args) throws IOException {
        Agent a = modelA();

        a.processGoals();

        System.out.println("");
    }

    protected static Agent modelA() throws IOException {
        Agent a = new Agent();

        FolParser fParser = new FolParser();
        fParser.setSignature(a.signature);

        Constant goalHolder = new Constant("gHolder", a.goalSort);
        a.signature.add(goalHolder);

        Predicate takeHospital = new Predicate("take_hospital", Arrays.asList(a.goalSort, Sort.THING));
        Predicate go = new Predicate("go", Arrays.asList(a.goalSort, Sort.THING, Sort.THING));
        Predicate sendShelter = new Predicate("send_shelter", Arrays.asList(a.goalSort, Sort.THING));

        a.signature.add(takeHospital);
        a.signature.add(go);
        a.signature.add(sendShelter);

        SleepingGoal sgTakeHosp = new SleepingGoal((FolAtom) fParser.parseFormula("take_hospital(G,X)"));
        sgTakeHosp.setPreference(1.0);
        a.addSleepingGoal(sgTakeHosp);

        SleepingGoal sgGo = new SleepingGoal((FolAtom) fParser.parseFormula("go(G,X,Y)"));
        sgGo.setPreference(0.1);
        a.addSleepingGoal(sgGo);

        SleepingGoal sgSendSh = new SleepingGoal((FolAtom) fParser.parseFormula("send_shelter(G,X)"));
        sgSendSh.setPreference(0.5);
        a.addSleepingGoal(sgSendSh);

        Constant arm = new Constant("arm");
        a.signature.add(arm);

        Predicate available = new Predicate("available", Arrays.asList(Sort.THING, Sort.THING));
        Predicate newSupply = new Predicate("new_supply", Arrays.asList(Sort.THING));
        Predicate hasFractBone = new Predicate("has_fract_bone", Arrays.asList(Sort.THING));
        Predicate fractBone = new Predicate("fract_bone", Arrays.asList(Sort.THING, Sort.THING));
        Predicate injuredSevere = new Predicate("injured_severe", Arrays.asList(Sort.THING));
        Predicate open_Fracture = new Predicate("open_fracture", Arrays.asList(Sort.THING));

        a.signature.add(available);
        a.signature.add(newSupply);
        a.signature.add(hasFractBone);
        a.signature.add(fractBone);
        a.signature.add(injuredSevere);
        a.signature.add(open_Fracture);

        a.standardRules.add(new StrictInferenceRule<>(fParser.parseFormula("available(X,Y)"), Arrays.asList(fParser.parseFormula("new_supply(X)"))));
        a.standardRules.add(new DefeasibleInferenceRule<>(fParser.parseFormula("has_fract_bone(X)"), Arrays.asList(fParser.parseFormula("injured_severe(X)"))));
        a.standardRules.add(new DefeasibleInferenceRule<>(fParser.parseFormula("fract_bone(X, arm)"), Arrays.asList(fParser.parseFormula("!injured_severe(X)"))));
        a.standardRules.add(new StrictInferenceRule<>(fParser.parseFormula("injured_severe(X)"), Arrays.asList(fParser.parseFormula("open_fracture(X)"))));

        Predicate askedForHelp = new Predicate("asked_for_help", Arrays.asList(Sort.THING, Sort.THING));

        a.signature.add(askedForHelp);

        a.activationRules.add(new StrictInferenceRule<>(fParser.parseFormula("take_hospital(G,X)"), Arrays.asList(fParser.parseFormula("injured_severe(X)"))));
        a.activationRules.add(new StrictInferenceRule<>(fParser.parseFormula("send_shelter(G,X)"), Arrays.asList(fParser.parseFormula("!injured_severe(X)"))));
        a.activationRules.add(new DefeasibleInferenceRule<>(fParser.parseFormula("go(G,X,Y)"), Arrays.asList(fParser.parseFormula("asked_for_help(X,Y)"))));

        Constant bed = new Constant("bed");
        a.signature.add(bed);

        Predicate supportWeigth = new Predicate("support_weight", Arrays.asList(Sort.THING));

        a.signature.add(supportWeigth);

        a.evaluationRules.add(new StrictInferenceRule<>(fParser.parseFormula("!take_hospital(G,X)"), Arrays.asList(fParser.parseFormula("!support_weight(X)"))));
        a.evaluationRules.add(new StrictInferenceRule<>(fParser.parseFormula("!take_hospital(G,X)"), Arrays.asList(fParser.parseFormula("!available(bed, X)"))));

        Predicate hasIncompatibility = new Predicate("has_incompatibility", Arrays.asList(a.goalSort));
        Predicate mostValuable = new Predicate("most_valuable", Arrays.asList(a.goalSort));
        Predicate choosen = new Predicate("choosen", Arrays.asList(a.goalSort));

        a.signature.add(hasIncompatibility);
        a.signature.add(mostValuable);
        a.signature.add(choosen);

        a.deliberationRules.add(new StrictInferenceRule<>(fParser.parseFormula("choosen(G)"), Arrays.asList(fParser.parseFormula("!has_incompatibility(G)"))));
        a.deliberationRules.add(new StrictInferenceRule<>(fParser.parseFormula("choosen(G)"), Arrays.asList(fParser.parseFormula("most_valuable(G)"))));

        Predicate hasPlansFor = new Predicate("has_plans_for", Arrays.asList(a.goalSort));
        Predicate satisfiedContext = new Predicate("satisfied_context", Arrays.asList(a.goalSort));
        Predicate executive = new Predicate("executive", Arrays.asList(a.goalSort));

        a.signature.add(hasPlansFor);
        a.signature.add(satisfiedContext);
        a.signature.add(executive);

        a.checkingRules.add(new StrictInferenceRule<>(fParser.parseFormula("executive(G)"), Arrays.asList(fParser.parseFormula("has_plans_for(G)"), fParser.parseFormula("satisfied_context(G)"))));

        Constant me = new Constant("me");
        Constant man32 = new Constant("man_32");
        Constant pos2 = new Constant("p2");
        Constant pos6 = new Constant("p6");

        a.signature.add(me);
        a.signature.add(man32);
        a.signature.add(pos2);
        a.signature.add(pos6);

        Predicate beOperative = new Predicate("be_operative", Arrays.asList(Sort.THING));

        a.signature.add(beOperative);

        a.beliefs.addOrdinaryPremise(fParser.parseFormula("be_operative(me)"));
        a.beliefs.addOrdinaryPremise(fParser.parseFormula("has_fract_bone(man_32)"));
        a.beliefs.addOrdinaryPremise(fParser.parseFormula("fract_bone(man_32, arm)"));
        a.beliefs.addOrdinaryPremise(fParser.parseFormula("asked_for_help(p2, p6)"));
        a.beliefs.addOrdinaryPremise(fParser.parseFormula("open_fracture(man_32)"));
        a.beliefs.addOrdinaryPremise(fParser.parseFormula("!available(bed,Y)"));
        a.beliefs.addOrdinaryPremise(fParser.parseFormula("new_supply(bed)"));
        a.beliefs.addOrdinaryPremise(fParser.parseFormula("support_weight(man_32)"));

        Predicate goalPlaceHolder = new Predicate("goal_place_holder", Arrays.asList(a.goalSort));

        a.signature.add(goalPlaceHolder);

        a.beliefs.addAxiom(fParser.parseFormula("goal_place_holder(gHolder)"));

        HashSet<FolFormula> beliefContext = new HashSet<>();
        beliefContext.add(fParser.parseFormula("be_operative(me)"));

        HashMap<String, Double> resourceContext = new HashMap<>();
        resourceContext.put("battery", 40d);
        Plan p1 = new Plan(sgTakeHosp, null, beliefContext, resourceContext);

        beliefContext = new HashSet<>();
        beliefContext.add(fParser.parseFormula("be_operative(me)"));

        resourceContext = new HashMap<>();
        resourceContext.put("battery", 50d);
        Plan p2 = new Plan(sgGo, null, beliefContext, resourceContext);

        beliefContext = new HashSet<>();
        beliefContext.add(fParser.parseFormula("be_operative(me)"));

        resourceContext = new HashMap<>();
        resourceContext.put("battery", 5d);
        Plan p3 = new Plan(sgSendSh, null, beliefContext, resourceContext);

        a.planLib.get(sgTakeHosp).add(p1);
        a.planLib.get(sgGo).add(p2);
        a.planLib.get(sgSendSh).add(p3);

        a.resources.addResource("battery", 80d);
        a.resources.addResource("oil", 70d);

        return a;
    }

    public List<GoalMemory> getGoalMemory() {
        return goalMemory.subList(0, goalMemory.size());
    }
}
