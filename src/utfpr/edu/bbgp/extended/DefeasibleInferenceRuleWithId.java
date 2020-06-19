/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.extended;

import java.util.Collection;
import net.sf.tweety.arg.aspic.syntax.DefeasibleInferenceRule;
import net.sf.tweety.logics.commons.syntax.interfaces.Invertable;

/**
 *
 * @author henri
 */
public class DefeasibleInferenceRuleWithId<T extends Invertable> extends DefeasibleInferenceRule<T> implements RuleWithIdInterface{
    private String ruleId;
    private String explanationSchema = null;

    public DefeasibleInferenceRuleWithId() {
    }

    public DefeasibleInferenceRuleWithId(T conclusion, Collection<T> premise) {
        super(conclusion, premise);
    }
    
    public DefeasibleInferenceRuleWithId(DefeasibleInferenceRule<T> rule) {
        super();
        
        this.setConclusion(rule.getConclusion());
        rule.getPremise().forEach((item) -> {this.addPremise(item);});
        
        if(rule instanceof DefeasibleInferenceRuleWithId){
            this.explanationSchema = ((DefeasibleInferenceRuleWithId)rule).explanationSchema;
        }
    }

    @Override
    public String getExplanationSchema() {
        return explanationSchema;
    }

    public DefeasibleInferenceRuleWithId setExplanationSchema(String explanationSchema) {
        this.explanationSchema = explanationSchema;
        return this;
    }

    @Override
    public String getRuleId() {
        return ruleId;
    }

    public DefeasibleInferenceRuleWithId setRuleId(String ruleId) {
        this.ruleId = ruleId;
        return this;
    }
    
}
