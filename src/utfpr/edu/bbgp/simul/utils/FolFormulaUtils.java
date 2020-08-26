/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.simul.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import net.sf.tweety.logics.commons.syntax.Constant;
import net.sf.tweety.logics.commons.syntax.Predicate;
import net.sf.tweety.logics.commons.syntax.Variable;
import net.sf.tweety.logics.commons.syntax.interfaces.Term;
import net.sf.tweety.logics.fol.syntax.FolAtom;
import net.sf.tweety.logics.fol.syntax.FolFormula;
import net.sf.tweety.logics.fol.syntax.Negation;

/**
 *
 * @author henri
 */
public class FolFormulaUtils {
    
    public static Set<Map<Variable, Term<?>>> getMappingsForTargets(FolFormula target, Queue<FolFormula> nextTargets, ArrayList<FolFormula> possibleValues, Set<Map<Variable, Term<?>>> mapping){
        if(possibleValues == null) return null;
        
        Set<Map<Variable, Term<?>>> mappings = new HashSet<>();
        
        if(target == null){
            FolFormula toAtom = target;
            Predicate targetPred = null;
            do{
                if(toAtom instanceof FolAtom){
                    targetPred = ((FolAtom) toAtom).getPredicate();
                    break;
                }
                if(toAtom instanceof Negation){
                    toAtom = ((Negation)toAtom).getFormula();
                    continue;
                }
                break;
            }while(!(toAtom instanceof FolAtom));

            if(targetPred == null) return null;
            FolAtom targetAtom = (FolAtom) toAtom;

            for(FolFormula poss : possibleValues){
                toAtom = poss;
                Predicate possPred = null;
                do{
                    if(toAtom instanceof FolAtom){
                        possPred = ((FolAtom) toAtom).getPredicate();
                        break;
                    }
                    if(toAtom instanceof Negation){
                        toAtom = ((Negation)toAtom).getFormula();
                        continue;
                    }
                    break;
                }while(!(toAtom instanceof FolAtom));

                if(possPred == null) return null;
                FolAtom possAtom = (FolAtom) toAtom;

                HashMap<Variable, Term<?>> map = new HashMap<>();

                boolean purge = false;
                for(int k = 0; k < targetPred.getArity(); k++){
                    Term t1 = targetAtom.getArguments().get(k);
                    Term t2 = possAtom.getArguments().get(k);

                    if(t2 instanceof Variable) continue;

                    if(t1 instanceof Constant && t2 instanceof Constant){
                        if(t1 != t2){
                            purge = true;
                            break;
                        }
                    }else {
                        if(t1 instanceof Variable){
                            map.put((Variable) t1, t2);
                        }
                    }
                }

                if(!purge){
                    mappings.add(map);
                }
            }
        }
        
        return mappings;
    }
    
    public static boolean compatibleEqualityFolFormula(FolFormula formula1, FolFormula formula2){
        if(formula1 instanceof FolAtom){
            if(formula2 instanceof FolAtom){
                FolAtom f1 = (FolAtom) formula1;
                FolAtom f2 = (FolAtom) formula2;
                
                if(f1.getPredicate().equals(f2.getPredicate())){
                    for(int k = 0; k < f1.getPredicate().getArity(); k++){
                        Term t1 = f1.getArguments().get(k);
                        Term t2 = f2.getArguments().get(k);

                        if(t1 instanceof Variable && t2 instanceof Variable){
                            continue;
                        }

                        if(t1 instanceof Constant && t2 instanceof Constant){
                            if(t1 != t2){
                                return false;
                            }
                        }else {
                            if(t1 instanceof Variable){
                                f1 = f1.substitute(t1, t2);
                            }else{
                                f2 = f2.substitute(t2, t1);
                            }
                        }
                    }
                    
                    boolean changed = true;
                    while(changed){
                        changed = false;
                        
                        for(int k = 0; k < f1.getPredicate().getArity(); k++){
                            Term t1 = f1.getArguments().get(k);
                            Term t2 = f2.getArguments().get(k);

                            if(t1 instanceof Variable && t2 instanceof Variable){
                                continue;
                            }
                            
                            if(t1 instanceof Constant && t2 instanceof Constant){
                                if(t1 != t2){
                                    return false;
                                }
                            }else {
                                if(t1 instanceof Variable){
                                    f1 = f1.substitute(t1, t2);
                                }else{
                                    f2 = f2.substitute(t2, t1);
                                }
                                changed = true;
                                break;
                            }
                        }
                    }
                    
                    for(int k = 0; k < f1.getPredicate().getArity(); k++){
                        Term t1 = f1.getArguments().get(k);
                        Term t2 = f2.getArguments().get(k);

                        if(t1 instanceof Variable && t2 instanceof Variable){
                            f1 = f1.substitute(t1, t2);
                        }
                    }
                    
                    return f1.equals(f2);
                }
            }
        }
        
        return false;
    }

    public static boolean equalsWithSubstitution(FolFormula formula1, FolFormula formula2, Constant wildcard) {
        if (formula1 == null || formula2 == null) {
            return false;
        }
        
        boolean formula1HasWildcard = formula1.getTerms().contains(wildcard);
        boolean formula2HasWildcard = formula2.getTerms().contains(wildcard);

        if ((!formula1HasWildcard && !formula2HasWildcard) || (formula1HasWildcard && formula2HasWildcard)) {
            return formula1.equals(formula2);
        }

        boolean hasAllPredicates = true;

        for (Predicate p : formula2.getPredicates()) {
            if (!formula1.getPredicates().contains(p)) {
                hasAllPredicates = false;
                break;
            }
        }

        for (Predicate p : formula1.getPredicates()) {
            if (!formula2.getPredicates().contains(p)) {
                hasAllPredicates = false;
                break;
            }
        }

        if (hasAllPredicates) {
            if(formula1 instanceof Negation && formula2 instanceof Negation){
                formula1 = ((Negation)formula1).getFormula();
                formula2 = ((Negation)formula2).getFormula();
            }
            
            if(formula1 instanceof FolAtom && formula2 instanceof FolAtom){
                FolAtom atomWith = (FolAtom) (formula1HasWildcard?formula1 : formula2);
                FolAtom atomWithout = (FolAtom) (formula1HasWildcard ? formula2: formula1);
                
                int size = atomWith.getArguments().size();
                if(atomWithout.getArguments().size() != size) return false;
                
                List<Term<?>> with = atomWith.getArguments();
                List<Term<?>> without = atomWithout.getArguments();
                for(int k = 0; k < size; k++){
                    if(with.get(k).equals(wildcard)) continue;
                    if(!without.get(k).equals(with.get(k))){
                        return false;
                    }
                }
                
                return true;
            }
            
//            FolFormula withW = (formula1HasWildcard? formula1 : formula2);
//            FolFormula withoutW = (formula1HasWildcard? formula2 : formula1);
//            
//            for(Term t : withoutW.getTerms()){
//                if(t.getSort().equals(wildcard.getSort())){
//                    FolFormula test = withW.substitute(wildcard, t);
//                    
//                    if(withoutW.equals(test)){
//                        return true;
//                    }
//                }
//            }
        }

        return false;
    }
}
