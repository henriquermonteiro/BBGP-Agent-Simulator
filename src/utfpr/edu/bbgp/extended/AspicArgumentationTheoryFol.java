/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.extended;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.tweety.arg.aspic.ruleformulagenerator.RuleFormulaGenerator;
import net.sf.tweety.arg.aspic.syntax.AspicArgument;
import net.sf.tweety.arg.aspic.syntax.AspicArgumentationTheory;
import net.sf.tweety.arg.aspic.syntax.DefeasibleInferenceRule;
import net.sf.tweety.arg.aspic.syntax.InferenceRule;
import net.sf.tweety.arg.aspic.syntax.StrictInferenceRule;
import net.sf.tweety.commons.util.Triple;
import net.sf.tweety.logics.commons.syntax.Constant;
import net.sf.tweety.logics.commons.syntax.Variable;
import net.sf.tweety.logics.commons.syntax.interfaces.Term;
import net.sf.tweety.logics.fol.syntax.FolFormula;
import net.sf.tweety.logics.fol.syntax.Negation;

/**
 *
 * @author henri
 */
public class AspicArgumentationTheoryFol extends AspicArgumentationTheory<FolFormula> {

    public AspicArgumentationTheoryFol(RuleFormulaGenerator<FolFormula> rfgen) {
        super(rfgen);
    }
    
    public static AspicArgumentationTheoryFol castTo(AspicArgumentationTheory<FolFormula> theory){
        AspicArgumentationTheoryFol thisTheory = new AspicArgumentationTheoryFol(theory.getRuleFormulaGenerator());
        
        theory.forEach((infRule) -> {
            thisTheory.add(infRule);
        });
        
        return thisTheory;
    }
    
    protected FolFormula cloneFolFormula(FolFormula formula){
        FolFormula clone = formula.clone();
        
        if(formula instanceof Negation){
            clone = new Negation(((Negation)((Negation) clone).getFormula()).getFormula().clone());
        }
        
        return clone;
    }

    @Override
    public Collection<AspicArgument<FolFormula>> getArguments() {
        Collection<AspicArgument<FolFormula>> args = new HashSet<>();
        Collection<Collection<AspicArgument<FolFormula>>> subs = new HashSet<>();
        Collection<Collection<AspicArgument<FolFormula>>> new_subs = new HashSet<>();
        Collection<AspicArgument<FolFormula>> argsForPrem = new HashSet<>();

        HashSet<Constant> usedTerms = new HashSet<>();

        for (InferenceRule<FolFormula> rule : this) { // Adiciona os fatos/crenças como argumentos sem premissas
            if (rule.isFact()) {
                args.add(new AspicArgument<>(rule));
                usedTerms.addAll(rule.getConclusion().getTerms(Constant.class));
            }
        }
        boolean changed;
        do {
            changed = false;
            for (InferenceRule<FolFormula> rule : this) {
                Set<Map<Variable, Term<?>>> mapping = rule.getConclusion().allSubstitutions(usedTerms);

                for (Map<Variable, Term<?>> map : mapping) {
                    if(map.isEmpty()) continue;
                    
                    subs.clear();
                    
                    boolean continueWithNextSubstitution = false;

//                    FolFormula ruleConclusion = (FolFormula) rule.getConclusion().clone().substitute(map);
                    FolFormula ruleConclusion = cloneFolFormula(rule.getConclusion());
                    ruleConclusion = (FolFormula) ruleConclusion.substitute(map);

                    
                    for (FolFormula prem : rule.getPremise()) {
//                        prem = (FolFormula) prem.clone().substitute(map);
                        prem = (FolFormula) cloneFolFormula(prem).substitute(map);

                        argsForPrem.clear();
                        for (AspicArgument<FolFormula> arg : args) { // Verifica se nos argumentos já construídos existe algum que unifique com a premissa
                            if (arg.getConclusion().equals(prem) // Se um argumento X tem uma conclusão que unifica com uma premissa
                                    && !arg.getAllConclusions().contains(ruleConclusion)) { // E minha conclusão não é um subargumento de X
                                argsForPrem.add(arg); // Adiciono como um argumento para a premissa
                            }
                        }
                        if (argsForPrem.isEmpty()) { // Se alguma premissa da regra não puder ser verificada
                            continueWithNextSubstitution = true; // Próxima regra
                            break;
                        } else {
                            if (subs.isEmpty()) {
                                for (AspicArgument<FolFormula> subarg : argsForPrem) {
                                    Collection<AspicArgument<FolFormula>> subargset = new HashSet<>();
                                    subargset.add(subarg);
                                    subs.add(subargset);
                                }
                            } else {
                                new_subs.clear();
                                for (AspicArgument<FolFormula> subarg : argsForPrem) {
                                    for (Collection<AspicArgument<FolFormula>> s : subs) {
                                        Collection<AspicArgument<FolFormula>> newS = new HashSet<>(s);
                                        newS.add(subarg);
                                        new_subs.add(newS);
                                    }
                                }
                                subs.clear();
                                subs.addAll(new_subs);
                            }
                        }
                    }
                    if (continueWithNextSubstitution) {
                        continue;
                    }
                    if(rule.getPremise().isEmpty()){
                        InferenceRule<FolFormula> unifiedRule;

                        if (rule instanceof StrictInferenceRule) {
                            unifiedRule = new StrictInferenceRule<>();

                        } else {
                            unifiedRule = new DefeasibleInferenceRule<>();
                        }

                        unifiedRule.setConclusion(ruleConclusion);

                        changed = args.add(new AspicArgument<>(unifiedRule)) || changed;
                        usedTerms.addAll(ruleConclusion.getTerms(Constant.class));
                    }
                    for (Collection<AspicArgument<FolFormula>> subargset : subs) {
                        InferenceRule<FolFormula> unifiedRule;

                        if (rule instanceof StrictInferenceRule) {
                            unifiedRule = new StrictInferenceRule<>();

                        } else {
                            unifiedRule = new DefeasibleInferenceRule<>();
                        }

                        unifiedRule.setConclusion(ruleConclusion);
                        for (FolFormula prem : rule.getPremise()) {
//                            unifiedRule.addPremise((FolFormula) prem.clone().substitute(map));
                            unifiedRule.addPremise((FolFormula) cloneFolFormula(prem).substitute(map));
                        }

                        changed = args.add(new AspicArgument<>(unifiedRule, subargset)) || changed;
                        usedTerms.addAll(ruleConclusion.getTerms(Constant.class));
                    }
                }
            }
        } while (changed);
        return args;
    }
    
    private ArrayList< Triple<FolFormula, HashSet<FolFormula>, HashMap<String, Double>>> planTemplates = new ArrayList<>();

    public void addPlanTemplate(FolFormula goalFormula, HashSet<FolFormula> beliefContext, HashMap<String, Double> resourceContext) {
        planTemplates.add(new Triple(goalFormula, beliefContext, resourceContext));
    }

    public List<Triple<FolFormula, HashSet<FolFormula>, HashMap<String, Double>>> getPlanTemplates() {
        return planTemplates;
    }
    
    private HashMap< String, Double> startingResources = new HashMap<>();
    
    public void addResource(String resource, Double value) {
        startingResources.put(resource, value);
    }

    public Map<String, Double> getStartingResources() {
        return startingResources;
    }

}
