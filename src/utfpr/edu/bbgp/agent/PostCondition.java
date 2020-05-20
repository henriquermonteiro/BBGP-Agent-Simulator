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
public class PostCondition {
    public static char ADDITION_OPERATION = '+';
    public static char DELETION_OPERATION = '-';
    
    private FolFormula belief;
    private String resource;
    private Double resourceAmount;
    private char operation;

    public PostCondition(FolFormula belief, char operation) {
        if(belief == null) throw new NullPointerException("Belief must not be null.");
        if(operation != ADDITION_OPERATION && operation != DELETION_OPERATION) throw new IllegalArgumentException("Operation must be PostCondition.ADDITION_OPERATION or PostCondition.DELETION_OPERATION");
        
        this.belief = belief;
        this.operation = operation;
        resource = null;
        resourceAmount = null;
    }

    public PostCondition(String resource, Double resourceAmount, char operation) {
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
}
