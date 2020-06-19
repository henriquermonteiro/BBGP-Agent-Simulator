/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.agent;

/**
 *
 * @author henri
 */
public enum GoalStage {
    Active("Active"), Pursuable("Pursuable"), Chosen("Chosen"), Executive("Executive"), Cancelled("Cancelled"), Completed("Completed");
    
    private final String stage_name;

    private GoalStage(String stage_name) {
        this.stage_name = stage_name;
    }

    public String getStage_name() {
        return stage_name;
    }
}
