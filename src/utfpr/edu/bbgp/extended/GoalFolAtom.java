/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.extended;

import java.util.List;
import java.util.Set;
import net.sf.tweety.logics.commons.syntax.Predicate;
import net.sf.tweety.logics.commons.syntax.interfaces.Term;
import net.sf.tweety.logics.fol.syntax.FolAtom;
import utfpr.edu.bbgp.agent.Agent;

/**
 *
 * @author henri
 */
public class GoalFolAtom extends FolAtom{

    public GoalFolAtom(FolAtom other) {
        super(other);
    }

    @Override
    public boolean equals(Object obj) {
        boolean equals = super.equals(obj);
        
        if(obj instanceof GoalFolAtom){
            if(!equals){
                if(hashCode() == obj.hashCode()){
                    equals = true;
                }
            }
        }
        
        return equals;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        
        List<Term<?>> arguments = getArguments();
        
        result = prime * result;
        if(arguments != null){
            int hash = 1; 
            for(Term t : arguments){
                hash = 31 * hash;
                
                if(t != null){
                    if(!t.getSort().getName().equals(Agent.GOAL_SORT_TEXT)){
                        hash += t.hashCode();
                    }
                }
            }
            
            result += hash;
        }
        
        Set<Predicate> predicates = getPredicates();
        
        result = prime * result + ((predicates == null) ? 0 : predicates.hashCode());
        
        return result;
    }
    
}
