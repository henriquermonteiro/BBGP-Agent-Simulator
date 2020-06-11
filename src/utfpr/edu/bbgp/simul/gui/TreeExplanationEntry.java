package utfpr.edu.bbgp.simul.gui;

import utfpr.edu.bbgp.agent.GoalMemory;

/**
 *
 * @author henri
 */
public class TreeExplanationEntry {
    private final String displayText;
    private final GoalMemory entry;
    private final boolean isPartial;

    public TreeExplanationEntry(GoalMemory entry, boolean isPartial) {
        this.entry = entry;
        this.isPartial = isPartial;
        displayText = (isPartial ? entry.getGoalFullPredicate() : String.format("Cycle %03d:", entry.getCycle()));
    }

    @Override
    public String toString() {
        return displayText;
    }
    
    public String getExplanation(){
        return entry.explain(!isPartial);
    }
}
