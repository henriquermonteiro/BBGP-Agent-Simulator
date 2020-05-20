/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.simul.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.tweety.commons.util.MapTools;
import net.sf.tweety.logics.commons.syntax.Constant;
import net.sf.tweety.logics.commons.syntax.Predicate;
import net.sf.tweety.logics.commons.syntax.Sort;
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
