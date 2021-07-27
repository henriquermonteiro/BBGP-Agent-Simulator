package utfpr.edu.bbgp.extended;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.sf.tweety.logics.commons.syntax.Functor;
import net.sf.tweety.logics.commons.syntax.Predicate;
import net.sf.tweety.logics.commons.syntax.RelationalFormula;
import net.sf.tweety.logics.commons.syntax.Variable;
import net.sf.tweety.logics.commons.syntax.interfaces.Atom;
import net.sf.tweety.logics.commons.syntax.interfaces.Term;
import net.sf.tweety.logics.fol.syntax.FolFormula;

/**
 *
 * @author henri
 */
public class ResourceFolFormula extends FolFormula{
    private static final Predicate RESOURCE_PREDICATE = new Predicate("_RESOURCE_FORMULA", 0);
    private final String resourceName;
    private final double amount;

    public ResourceFolFormula(String resourceName, double amount) {
        super();
        this.resourceName = resourceName;
        this.amount = amount;
    }

    public String getResourceName() {
        return resourceName;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public FolFormula toNnf() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RelationalFormula collapseAssociativeFormulas() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isDnf() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public FolFormula substitute(Term<?> arg0, Term<?> arg1) throws IllegalArgumentException {
        return this.clone();
    }

    @Override
    public FolFormula clone() {
        return new ResourceFolFormula(resourceName, amount);
    }

    @Override
    public Set<? extends Atom> getAtoms() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<Functor> getFunctors() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String toString() {
        return "res : " + resourceName + "," + amount;
    }

    @Override
    public Set<? extends Predicate> getPredicates() {
        HashSet<Predicate> set = new HashSet<>(1);
        set.add(RESOURCE_PREDICATE);
        return new HashSet<>(set);
    }

    @Override
    public boolean isLiteral() {
        return true;
    }

    @Override
    public Set<Variable> getUnboundVariables() {
        return new HashSet<>();
    }

    @Override
    public boolean containsQuantifier() {
        return false;
    }

    @Override
    public boolean isWellBound() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isWellBound(Set<Variable> arg0) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isClosed() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isClosed(Set<Variable> arg0) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<Term<?>> getTerms() {
        return new HashSet<>();
    }

    @Override
    public <C extends Term<?>> Set<C> getTerms(Class<C> arg0) {
        return new HashSet<>();
    }

    public Map<String, Double> getResourceMap() {
        HashMap<String, Double> map = new HashMap<>();
        map.put(resourceName, amount);
        return map;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.resourceName);
        hash = 17 * hash + (int) (Double.doubleToLongBits(this.amount) ^ (Double.doubleToLongBits(this.amount) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ResourceFolFormula other = (ResourceFolFormula) obj;
        if (Double.doubleToLongBits(this.amount) != Double.doubleToLongBits(other.amount)) {
            return false;
        }
        if (!Objects.equals(this.resourceName, other.resourceName)) {
            return false;
        }
        return true;
    }
    
}
