/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.agent;

import net.sf.tweety.arg.dung.syntax.Argument;

/**
 *
 * @author henri
 */
public class GoalMemory {
    private final Long agentCycle;
    private final Goal goal;
    private final Argument support;

    public GoalMemory(Long agentCycle, Goal goal, Argument support) {
        this.agentCycle = agentCycle;
        this.goal = (Goal) goal.clone();
        this.support = support;
    }

    @Override
    public String toString() {
        return "Goal " + goal.getFullPredicate() + " changed to state " + goal.getStage().name() + " at cycle " + agentCycle + " because [" + support + "]";
    }
}
