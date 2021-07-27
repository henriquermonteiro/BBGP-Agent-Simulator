/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.agent;

import net.sf.tweety.logics.fol.syntax.FolFormula;

/**
 *
 * @author henri
 */
public class PerceptionEntry implements Comparable<PerceptionEntry> {
    public static char ADDITION_OPERATION = '+';
    public static char DELETION_OPERATION = '-';

    private long cycle = -1;
    private Integer order;
    private FolFormula belief;
    private char operation;
    private String resource;
    private Double resourceAmount;

    public PerceptionEntry(FolFormula belief, char operation) {
        if(belief == null) throw new NullPointerException("Belief must not be null.");
        if(operation != ADDITION_OPERATION && operation != DELETION_OPERATION) throw new IllegalArgumentException("Operation must be PostCondition.ADDITION_OPERATION or PostCondition.DELETION_OPERATION");
        
        this.belief = belief;
        this.operation = operation;
        resource = null;
        resourceAmount = null;
    }

    public PerceptionEntry(String resource, Double resourceAmount, char operation) {
        if(resource == null) throw new NullPointerException("Resource must not be null.");
        if(resourceAmount == null) throw new NullPointerException("Resource amount must not be null.");
        if(resource.isBlank()) throw new IllegalArgumentException("Resource must not be blank.");
        if(resourceAmount < 0) throw new IllegalArgumentException("Resource amount must be positive.");
        if(operation != ADDITION_OPERATION && operation != DELETION_OPERATION) throw new IllegalArgumentException("Operation must be PostCondition.ADDITION_OPERATION or PostCondition.DELETION_OPERATION");
        this.resource = resource;
        this.resourceAmount = resourceAmount;
        this.operation = operation;
        this.belief = null;
    }

    public PerceptionEntry(Long cycle, Integer order, char operation, FolFormula perception) {
        this.cycle = cycle;
        this.order = order;
        this.operation = operation;
        this.belief = perception;
    }

    public PerceptionEntry(Long cycle, Integer order, char operation, String resource, Double resourceAmount) {
        this.cycle = cycle;
        this.order = order;
        this.resource = resource;
        this.resourceAmount = resourceAmount;
        this.operation = operation;
        this.belief = null;
    }

    public long getCycle() {
        return cycle;
    }

    public FolFormula getBelief() {
        return belief;
    }

    public String getResource() {
        return resource;
    }

    public Double getResourceAmount() {
        return resourceAmount;
    }

    public char getOperation() {
        return operation;
    }
    
    public boolean isBeliefType(){
        return belief != null;
    }
    
    public boolean isResourceType(){
        return resource != null;
    }

    @Override
    public int compareTo(PerceptionEntry arg0) {
        if (arg0 == null) {
            return 1;
        }

        if (cycle != arg0.cycle) {
            return (int) (cycle - arg0.cycle);
        }
        return order - arg0.order;
    }

    @Override
    public String toString() {
        String cycleS = ((Long)this.cycle).toString();
        if (cycleS.length() < 4) {
            cycleS = "000".substring(0, 3 - cycleS.length()).concat(cycleS);
        }
        String body = "" + operation + " ";
        if(isBeliefType()){
            body = body.concat(this.belief.toString());
        }else{
            body = body.concat("res : " + resource + ", " + resourceAmount.toString());
        }
        return (cycle > -1 ? cycleS.concat(" : ").concat(body) : body);
    }
}
