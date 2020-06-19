/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.extended;

import java.util.Collection;
import net.sf.tweety.arg.aspic.syntax.StrictInferenceRule;
import net.sf.tweety.logics.commons.syntax.interfaces.Invertable;

/**
 *
 * @author henri
 */
public class StrictInferenceRuleWithId<T extends Invertable> extends StrictInferenceRule<T> implements RuleWithIdInterface{
    private String ruleId = "";
    private String explanationSchema = null;

    public StrictInferenceRuleWithId() {
    }

    public StrictInferenceRuleWithId(T conclusion, Collection<T> premise) {
        super(conclusion, premise);
    }
    
    public StrictInferenceRuleWithId(StrictInferenceRule<T> rule) {
        super();
        
        this.setConclusion(rule.getConclusion());
        rule.getPremise().forEach((item) -> {this.addPremise(item);});
        
        if(rule instanceof StrictInferenceRuleWithId){
            this.explanationSchema = ((StrictInferenceRuleWithId)rule).explanationSchema;
        }
    }

    @Override
    public String getExplanationSchema() {
        return explanationSchema;
    }

    public StrictInferenceRuleWithId setExplanationSchema(String explanationSchema) {
        this.explanationSchema = explanationSchema;
        return this;
    }

    @Override
    public String getRuleId() {
        return ruleId;
    }

    public StrictInferenceRuleWithId setRuleId(String ruleId) {
        this.ruleId = ruleId;
        return this;
    }
    
}
