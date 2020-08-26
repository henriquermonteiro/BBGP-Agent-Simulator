package utfpr.edu.bbgp.simul.gui;

import utfpr.edu.argumentation.diagram.ArgumentionFramework;
import utfpr.edu.bbgp.agent.GoalMemory;

/**
 *
 * @author henri
 */
public class TreeExplanationEntry {
    private final String displayText;
    private final GoalMemory entry;
    private final char type;
    
    public final static char CYCLE_TYPE = 'c';
    public final static char STAGE_TYPE = 's';
    public final static char GOAL_TYPE = 'g';

    public TreeExplanationEntry(GoalMemory entry, char type) {
        this.entry = entry;
        this.type = type;
        if(type != CYCLE_TYPE && type != STAGE_TYPE && type != GOAL_TYPE) throw new IllegalArgumentException("Invalid type. Must be one of: TreeExplanationEntry.CYCLE_TYPE, TreeExplanationEntry.STAGE_TYPE or TreeExplanationEntry.GOAL_TYPE.");
        displayText = (type == GOAL_TYPE ? entry.toStringSimplified(): (type == CYCLE_TYPE ? String.format("Cycle %03d:", entry.getCycle()) : entry.getGoalStage().getStage_name()));
    }

    @Override
    public String toString() {
        return displayText;
    }
    
    public String getExplanation(){
        if(type == CYCLE_TYPE) return "Select a stage or goal.";
        return entry.explain(type == STAGE_TYPE);
    }

    public char getType() {
        return type;
    }
    
    public void showInCluster(ArgumentionFramework cluster) {
        if(type == CYCLE_TYPE) return;
        if(type == GOAL_TYPE){
            entry.showInCluster(cluster);
            return;
        }
        if(type == STAGE_TYPE){
            entry.showCompleteAFInCluster(cluster);
        }
    }
}
